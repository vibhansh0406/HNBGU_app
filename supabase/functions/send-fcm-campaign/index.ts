// ============================================================================
// CampusOne — Supabase Edge Function: send-fcm-campaign
// Validates caller JWT & admin scope, filters target device_tokens, and
// dispatches push notifications via Firebase Cloud Messaging HTTP v1 API.
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

interface CampaignRequest {
  campaignId: string;
}

interface FcmServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const fcmServiceAccountRaw = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";

    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceRoleKey) {
      return new Response(
        JSON.stringify({ error: "Supabase environment variables are not configured." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing Authorization bearer token." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Verify caller identity using caller JWT
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: authData, error: authErr } = await userClient.auth.getUser();
    if (authErr || !authData?.user) {
      return new Response(
        JSON.stringify({ error: "Invalid or expired user session." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Check caller role and active status via service client
    const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey);
    const { data: callerProfile, error: profileErr } = await adminClient
      .from("profiles")
      .select("id, email, role, department_id, account_status")
      .eq("id", authData.user.id)
      .single();

    if (
      profileErr ||
      !callerProfile ||
      callerProfile.account_status !== "active" ||
      !["super_admin", "department_admin", "notice_admin"].includes(callerProfile.role)
    ) {
      return new Response(
        JSON.stringify({ error: "Forbidden: caller is not an authorized active administrator." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = (await req.json()) as CampaignRequest;
    if (!body?.campaignId) {
      return new Response(
        JSON.stringify({ error: "campaignId is required." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { data: campaign, error: campErr } = await adminClient
      .from("notification_campaigns")
      .select("*")
      .eq("id", body.campaignId)
      .single();

    if (campErr || !campaign) {
      return new Response(
        JSON.stringify({ error: "Notification campaign not found." }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Enforce department scope for non-super-admins
    if (
      callerProfile.role !== "super_admin" &&
      campaign.target_department_id !== callerProfile.department_id
    ) {
      return new Response(
        JSON.stringify({ error: "Forbidden: cannot send campaigns outside your assigned department." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Avoid duplicate sends if already sent
    if (campaign.delivery_status === "sent") {
      return new Response(
        JSON.stringify({
          status: "already_sent",
          message: "Campaign was already delivered.",
          total_targeted: campaign.total_targeted,
          total_delivered: campaign.total_delivered,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Query active target device tokens
    let tokenQuery = adminClient
      .from("device_tokens")
      .select("fcm_token")
      .eq("is_active", true);

    if (campaign.target_department_id) {
      tokenQuery = tokenQuery.eq("department_id", campaign.target_department_id);
    }
    if (campaign.target_semester) {
      tokenQuery = tokenQuery.eq("semester", campaign.target_semester);
    }

    const { data: tokenRows, error: tokenErr } = await tokenQuery;
    if (tokenErr) {
      throw new Error(`Failed to query device_tokens: ${tokenErr.message}`);
    }

    const uniqueTokens = Array.from(
      new Set((tokenRows ?? []).map((r: { fcm_token: string }) => r.fcm_token).filter(Boolean))
    );

    if (!fcmServiceAccountRaw) {
      await adminClient
        .from("notification_campaigns")
        .update({
          delivery_status: "scheduled",
          total_targeted: uniqueTokens.length,
          last_error:
            "FCM_SERVICE_ACCOUNT_JSON secret is not configured in Supabase Edge Function secrets yet. Campaign saved & queued.",
        })
        .eq("id", campaign.id);

      return new Response(
        JSON.stringify({
          fcm_configured: false,
          status: "queued_pending_fcm_secret",
          total_targeted: uniqueTokens.length,
          message:
            "Campaign saved in Supabase. Set FCM_SERVICE_ACCOUNT_JSON in Supabase Edge Function secrets to enable live FCM push dispatch.",
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const serviceAccount = JSON.parse(fcmServiceAccountRaw) as FcmServiceAccount;
    const accessToken = await getGoogleAccessToken(serviceAccount);

    await adminClient
      .from("notification_campaigns")
      .update({ delivery_status: "sending", total_targeted: uniqueTokens.length })
      .eq("id", campaign.id);

    let deliveredCount = 0;
    let lastError: string | null = null;

    for (const token of uniqueTokens) {
      const fcmRes = await fetch(
        `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token,
              notification: {
                title: campaign.title,
                body: campaign.body,
              },
              data: {
                campaign_id: String(campaign.id),
                related_entity_type: String(campaign.related_entity_type ?? "notice"),
                related_entity_id: String(campaign.related_entity_id ?? ""),
              },
            },
          }),
        }
      );

      if (fcmRes.ok) {
        deliveredCount += 1;
      } else {
        const errBody = await fcmRes.text();
        lastError = errBody.slice(0, 300);
      }
    }

    const finalStatus = deliveredCount > 0 || uniqueTokens.length === 0 ? "sent" : "failed";
    await adminClient
      .from("notification_campaigns")
      .update({
        delivery_status: finalStatus,
        total_targeted: uniqueTokens.length,
        total_delivered: deliveredCount,
        last_error: lastError,
        sent_at: new Date().toISOString(),
      })
      .eq("id", campaign.id);

    await adminClient.from("audit_logs").insert({
      actor_id: callerProfile.id,
      actor_email: callerProfile.email,
      actor_role: callerProfile.role,
      action: "SEND_FCM_CAMPAIGN",
      entity_type: "notification_campaigns",
      entity_id: String(campaign.id),
      metadata: {
        total_targeted: uniqueTokens.length,
        total_delivered: deliveredCount,
        final_status: finalStatus,
      },
    });

    return new Response(
      JSON.stringify({
        fcm_configured: true,
        status: finalStatus,
        total_targeted: uniqueTokens.length,
        total_delivered: deliveredCount,
        last_error: lastError,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});

async function getGoogleAccessToken(sa: FcmServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claimSet = btoa(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })
  );
  const unsignedJwt = `${header}.${claimSet}`;
  const pemContents = sa.private_key
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s+/g, "");
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signatureBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(unsignedJwt)
  );
  const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)));
  const assertion = `${unsignedJwt}.${signature}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) {
    throw new Error(`OAuth token exchange failed: ${await res.text()}`);
  }
  const data = await res.json();
  return data.access_token as string;
}
