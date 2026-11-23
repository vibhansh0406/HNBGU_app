import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

// FCM configuration - these must be set as Edge Function secrets
const FCM_PROJECT_ID = Deno.env.get("FCM_PROJECT_ID")!;
const FCM_SERVICE_ACCOUNT_EMAIL = Deno.env.get("FCM_SERVICE_ACCOUNT_EMAIL")!;
const FCM_PRIVATE_KEY = Deno.env.get("FCM_PRIVATE_KEY") ?? ""; // PEM format private key

// Cache the OAuth2 access token
let accessToken: string | null = null;
let tokenExpiry = 0;

function base64Url(value: string): string {
  const bytes = new TextEncoder().encode(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

async function getFcmAccessToken(): Promise<string> {
  if (!FCM_PROJECT_ID || !FCM_SERVICE_ACCOUNT_EMAIL || !FCM_PRIVATE_KEY) {
    throw new Error("FCM server credentials are not configured");
  }
  const now = Date.now();
  if (accessToken && now < tokenExpiry - 60000) {
    return accessToken;
  }

  // Generate JWT for Google OAuth2
  const header = { alg: "RS256", typ: "JWT" };
  const nowSec = Math.floor(now / 1000);
  const payload = {
    iss: FCM_SERVICE_ACCOUNT_EMAIL,
    sub: FCM_SERVICE_ACCOUNT_EMAIL,
    aud: "https://oauth2.googleapis.com/token",
    iat: nowSec,
    exp: nowSec + 3600,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  };

  const encoder = new TextEncoder();
  const headerB64 = base64Url(JSON.stringify(header));
  const payloadB64 = base64Url(JSON.stringify(payload));
  const signingInput = `${headerB64}.${payloadB64}`;

  // Sign with the private key (PKCS#8 PKCS#8 PEM)
  const keyData = FCM_PRIVATE_KEY.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");
  const keyBytes = Uint8Array.from(atob(keyData), (c) => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, encoder.encode(signingInput));
  const signatureBytes = new Uint8Array(signature);
  let signatureBinary = "";
  for (const byte of signatureBytes) signatureBinary += String.fromCharCode(byte);
  const signatureB64 = btoa(signatureBinary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const jwt = `${signingInput}.${signatureB64}`;

  // Exchange JWT for access token
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  const tokenData = await tokenResponse.json();
  if (!tokenResponse.ok || !tokenData.access_token) {
    throw new Error("Google OAuth token exchange failed: " + (tokenData.error ?? tokenResponse.status));
  }
  accessToken = tokenData.access_token;
  tokenExpiry = now + (tokenData.expires_in || 3600) * 1000;

  return accessToken;
}

async function sendFcmMessage(
  accessToken: string,
  fcmToken: string,
  title: string,
  body: string,
  data: Record<string, string>
): Promise<boolean> {
  try {
    const message = {
      message: {
        token: fcmToken,
        // Data-only payload: Android service renders one notification and applies preferences.
        data: data,
        android: {
          priority: "HIGH",
        },
        apns: {
          payload: {
            aps: {
              sound: "default",
            },
          },
        },
      },
    };

    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(message),
      }
    );

    return response.ok;
  } catch (error) {
    console.error("FCM send error:", error);
    return false;
  }
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type" } });
  }

  try {
    // Get the authorization header
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ ok: false, error: "Missing authorization header" }), {
        status: 401,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    // Create a Supabase client with the service role key
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Verify the user's JWT and check admin permission
    const token = authHeader.replace("Bearer ", "");
    const { data: { user }, error: authError } = await supabase.auth.getUser(token);

    if (authError || !user) {
      return new Response(JSON.stringify({ ok: false, error: "Invalid or expired token" }), {
        status: 401,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    // Check if user has VIEW_USERS permission (admin)
    const { data: profile } = await supabase
      .from("profiles")
      .select("role")
      .eq("id", user.id)
      .single();

    const isAdmin = profile?.role === "ADMIN" || profile?.role === "SUPER_ADMIN";
    if (!isAdmin) {
      return new Response(JSON.stringify({ ok: false, error: "Admin access required" }), {
        status: 403,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    // Parse request body
    const body = await req.json();
    const { user_ids, section_key, title, body: notificationBody, category, target_type, target_id } = body;

    if (typeof title !== "string" || typeof notificationBody !== "string" || !title.trim() || !notificationBody.trim() || title.length > 120 || notificationBody.length > 1200) {
      return new Response(JSON.stringify({ ok: false, error: "Missing title or body" }), {
        status: 400,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    // Resolve section-scoped recipients from trusted profile columns.
    // sectionKey format: college_course_semN_secX (course may contain underscores).
    let targetUserIds: string[] | null = user_ids && Array.isArray(user_ids)
      ? [...new Set(user_ids.filter((id: unknown) => typeof id === "string"))]
      : null;

    if (typeof section_key === "string" && section_key !== "global") {
      const match = section_key.match(/^([^_]+)_(.+)_sem(\d+)_sec(.+)$/);
      if (!match) {
        return new Response(JSON.stringify({ ok: false, error: "Invalid section_key" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
      const [, collegeId, courseId, semesterText, section] = match;
      const { data: profiles, error: profileError } = await supabase
        .from("profiles")
        .select("id")
        .eq("college_id", collegeId)
        .eq("course_id", courseId)
        .eq("semester", Number(semesterText))
        .eq("section", section);

      if (profileError) {
        console.error("Recipient profile query failed:", profileError);
        return new Response(JSON.stringify({ ok: false, error: "Failed to resolve section recipients" }), {
          status: 500,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
      targetUserIds = (profiles ?? []).map((profile: { id: string }) => profile.id);
      if (targetUserIds.length === 0) {
        return new Response(JSON.stringify({ ok: true, message: "No users in target section", sent: 0, total: 0 }), {
          status: 200,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
    }

    // Get active device tokens for the resolved audience.
    let query = supabase.from("device_tokens").select("fcm_token, user_id").eq("is_active", true);
    if (targetUserIds !== null) {
      if (targetUserIds.length === 0) {
        return new Response(JSON.stringify({ ok: true, message: "No target users", sent: 0, total: 0 }), {
          status: 200,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
      query = query.in("user_id", targetUserIds);
    }

    const { data: tokens, error: tokensError } = await query;

    if (tokensError) {
      console.error("Token query error:", tokensError);
      return new Response(JSON.stringify({ ok: false, error: "Failed to query device tokens" }), {
        status: 500,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    if (!tokens || tokens.length === 0) {
      return new Response(JSON.stringify({ ok: true, message: "No active device tokens found", sent: 0 }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    // Get FCM access token
    const fcmToken = await getFcmAccessToken();

    // Send notifications to all device tokens
    const dataPayload: Record<string, string> = {
      title,
      body: notificationBody,
      category: category || "notice",
      target_type: target_type || "",
      target_id: target_id || "",
    };

    let sentCount = 0;
    let failedCount = 0;
    for (let offset = 0; offset < tokens.length; offset += 100) {
      const batch = tokens.slice(offset, offset + 100);
      const results = await Promise.allSettled(
        batch.map((t) => sendFcmMessage(fcmToken, t.fcm_token, title, notificationBody, dataPayload))
      );
      for (const result of results) {
        if (result.status === "fulfilled" && result.value === true) sentCount++;
        else failedCount++;
      }
    }

    return new Response(JSON.stringify({ ok: true, message: `Sent ${sentCount}/${tokens.length} notifications`, sent: sentCount, failed: failedCount, total: tokens.length }), {
      status: 200,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  } catch (error) {
    console.error("Edge function error:", error);
    return new Response(JSON.stringify({ ok: false, error: "Internal server error" }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  }
});
