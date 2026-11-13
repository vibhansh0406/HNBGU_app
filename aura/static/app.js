/* Aura — frontend application (zero-dependency vanilla JS) */

const API = "";
const esc = (s) =>
  String(s == null ? "" : s).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
  );

// Local wall-clock ISO-8601 with real offset, e.g. "2026-11-11T12:00:00.000+05:30".
// Storing this keeps server and client in agreement about what hour/date "now" is,
// regardless of where the server runs.
function localISO() {
  const now = new Date();
  const off = -now.getTimezoneOffset(); // minutes east of UTC
  const abs = Math.abs(off);
  const h = Math.floor(abs / 60);
  const m = abs % 60;
  const sign = off >= 0 ? "+" : "-";
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}.${String(
      now.getMilliseconds()
    ).padStart(3, "0")}${sign}${pad(h)}:${pad(m)}`
  );
}

// ---------------- shell refs ----------------
const VIEWS = $("#views");
const SHELL_BAR = $("#shell-bar");
const SHELL_NAV = $("#shell-nav");
const SHELL_BRAND = $("#shell-brand");
const SHELL_RIGHT = $("#shell-bar-right");

function shell(show) {
  if (show) {
    SHELL_BAR.style.display = "flex";
    SHELL_NAV.style.display = "flex";
  } else {
    SHELL_BAR.style.display = "none";
    SHELL_NAV.style.display = "none";
  }
}

function shellRight() {
  SHELL_RIGHT.innerHTML = "";
  if (!state.user) return;
  const gear = document.createElement("button");
  gear.className = "btn-icon";
  gear.title = "Settings";
  gear.textContent = "⚙️";
  gear.onclick = () => { location.hash = "#settings"; };
  const out = document.createElement("button");
  out.className = "btn-icon";
  out.title = "Sign out";
  out.textContent = "⏷";
  out.onclick = async () => {
    try { await api("/api/auth/logout", "POST"); } catch (e) {}
    state.user = null;
    shellRight();
    location.hash = "#login";
  };
  SHELL_RIGHT.appendChild(gear);
  SHELL_RIGHT.appendChild(out);
}

SHELL_BRAND.onclick = () => { location.hash = "#home"; };

// ---------------- state ----------------
const state = {
  user: null,
  theme: localStorage.getItem("aura_theme") || "dark",
  view: null,
  energy: 6,
  focus: 6,
  mood: "neutral",
  tags: [],
  note: "",
  editId: null,
  checkins: [],
  insights: null,
  todayCheckin: null,
  pointer: { x: 0, y: 0, active: false },
  settingsReminders: false,
  settingsTheme: "dark",
  historyFilter: { mood: "", q: "", sort: "date" },
};

function applyTheme(theme) {
  state.theme = theme;
  document.documentElement.classList.toggle("light", theme === "light");
  document.body.classList.toggle("light", theme === "light");
}

// mood -> color
const MOOD_COLORS = {
  great: "var(--mood-great)",
  good: "var(--mood-good)",
  neutral: "var(--mood-neutral)",
  low: "var(--mood-low)",
  tired: "var(--mood-tired)",
};
const MOOD_LABELS = {
  great: "Great",
  good: "Good",
  neutral: "Neutral",
  low: "Low",
  tired: "Tired",
};
const MOOD_EMOJIS = { great: "😊", good: "🙂", neutral: "😐", low: "😔", tired: "😴" };
const TAGS = [
  { id: "deep", label: "Deep work", emoji: "🧠" },
  { id: "meet", label: "Meeting", emoji: "🗣️" },
  { id: "exercise", label: "Exercise", emoji: "🏃" },
  { id: "meal", label: "Meal", emoji: "🍽️" },
  { id: "rest", label: "Rest", emoji: "🛌" },
  { id: "commute", label: "Commute", emoji: "🚇" },
];

// ---------------- api ----------------
async function api(path, method = "GET", body) {
  const opts = {
    method,
    headers: { Accept: "application/json" },
    credentials: "include",
  };
  if (body && (method === "POST" || method === "PUT")) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(API + path, opts);
  let data;
  try {
    data = await res.json();
  } catch (e) {
    data = {};
  }
  if (!res.ok) {
    const err = new Error(data.error || `request failed (${res.status})`);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

async function me() {
  try {
    const { user } = await api("/api/auth/me");
    state.user = user || null;
    return state.user;
  } catch (e) {
    state.user = null;
    return null;
  }
}

// ---------------- render helpers ----------------
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

function setView(name) {
  $$(".view").forEach((v) => v.classList.remove("active"));
  const el = $(`#view-${name}`);
  if (el) el.classList.add("active");
  state.view = name;
  if (name !== "home" && auraAnim) {
    cancelAnimationFrame(auraAnim);
    auraAnim = null;
  }
  const authViews = ["login", "signup", "onboarding"];
  shell(!authViews.includes(name));
  if (!authViews.includes(name)) shellRight();
  if (name === "home") renderHome();
  if (name === "rhythm") renderRhythm();
  if (name === "history") renderHistory();
  if (name === "settings") renderSettings();
  window.scrollTo(0, 0);
}

function nav() {
  const hash = (location.hash || "#home").replace(/#/, "");
  const allowed = ["home", "rhythm", "history", "settings", "login", "signup", "onboarding"];
  let name = allowed.includes(hash) ? hash : "home";
  if (name !== "login" && name !== "signup" && name !== "onboarding") {
    if (!state.user) { location.hash = "#login"; return; }
    if (!state.user.wake_time && name === "home") { location.hash = "#onboarding"; return; }
  }
  setView(name);
  updateBottomNav(name);
}

function updateBottomNav(name) {
  $$(".nav-tab").forEach((t) => {
    t.classList.toggle("active", t.dataset.tab === name);
  });
}

// ---------------- auth views ----------------
function renderLogin() {
  VIEWS.innerHTML = `
    <section class="view" id="view-login">
      <div class="auth-wrap">
        <div class="auth-card">
          <div class="auth-head">
            <div class="mark">A</div>
            <h1>Welcome back.</h1>
            <p>Step into your rhythm.</p>
          </div>
          <form id="form-login">
            <div class="field">
              <label>Email</label>
              <input type="email" name="email" placeholder="you@example.com" required autocomplete="email">
            </div>
            <div class="field">
              <label>Password</label>
              <input type="password" name="password" placeholder="••••••••" required minlength="6" autocomplete="current-password">
            </div>
            <button class="btn btn-primary" type="submit" id="btn-login">Sign in</button>
          </form>
          <div class="auth-footer">
            Don&rsquo;t have an account? <a href="#signup" data-link>Create one</a>
          </div>
        </div>
      </div>
    </section>
  `;
  $("#form-login").onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const btn = $("#btn-login");
    btn.disabled = true;
    try {
      await api("/api/auth/login", "POST", {
        email: fd.get("email"),
        password: fd.get("password"),
      });
      await me();
      location.hash = "#home";
    } catch (err) {
      alert(err.message || "Login failed");
    } finally {
      btn.disabled = false;
    }
  };
  $(`[data-link][href="#signup"]`).onclick = (e) => { e.preventDefault(); location.hash = "#signup"; };
}

function renderSignup() {
  VIEWS.innerHTML = `
    <section class="view" id="view-signup">
      <div class="auth-wrap">
        <div class="auth-card">
          <div class="auth-head">
            <div class="mark">A</div>
            <h1>Create your Aura.</h1>
            <p>Your energy, your rhythm, your artwork.</p>
          </div>
          <form id="form-signup">
            <div class="field">
              <label>Name</label>
              <input type="text" name="name" placeholder="Your name" required autocomplete="name">
            </div>
            <div class="field">
              <label>Email</label>
              <input type="email" name="email" placeholder="you@example.com" required autocomplete="email">
            </div>
            <div class="field">
              <label>Password</label>
              <input type="password" name="password" placeholder="••••••••" required minlength="6" autocomplete="new-password">
            </div>
            <button class="btn btn-primary" type="submit" id="btn-signup">Get started</button>
          </form>
          <div class="auth-footer">
            Already have an account? <a href="#login" data-link>Sign in</a>
          </div>
        </div>
      </div>
    </section>
  `;
  $("#form-signup").onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const btn = $("#btn-signup");
    btn.disabled = true;
    try {
      await api("/api/auth/signup", "POST", {
        name: fd.get("name"),
        email: fd.get("email"),
        password: fd.get("password"),
      });
      await me();
      location.hash = state.user.wake_time ? "#home" : "#onboarding";
    } catch (err) {
      alert(err.message || "Signup failed");
    } finally {
      btn.disabled = false;
    }
  };
  $(`[data-link][href="#login"]`).onclick = (e) => { e.preventDefault(); location.hash = "#login"; };
}

// ---------------- onboarding ----------------
function renderOnboarding() {
  const u = state.user;
  VIEWS.innerHTML = `
    <section class="view" id="view-onboarding">
      <div class="onboard-wrap">
        <div class="onboard-card">
          <div class="step-progress">
            <div class="step-dot current"></div>
            <div class="step-dot"></div>
            <div class="step-dot"></div>
          </div>
          <div class="step active" id="step-1">
            <h2>When does your day begin?</h2>
            <p class="small">We&rsquo;ll map your energy against your natural rhythm.</p>
            <div class="field" style="margin-bottom:6px">
              <label>Typical wake time</label>
              <input type="time" id="ob-wake" value="${esc(u.wake_time || "07:00")}">
            </div>
            <div class="field">
              <label>Typical sleep time</label>
              <input type="time" id="ob-sleep" value="${esc(u.sleep_time || "23:00")}">
            </div>
            <div class="row-btns" style="margin-top:18px">
              <button class="btn btn-ghost" onclick="location.hash='#login'">Back</button>
              <button class="btn btn-primary" id="ob-next-1">Next</button>
            </div>
          </div>
          <div class="step" id="step-2">
            <h2>Your work window</h2>
            <p class="small">When you&rsquo;re usually at your desk.</p>
            <div class="field" style="margin-bottom:6px">
              <label>Work starts</label>
              <input type="time" id="ob-wstart" value="${esc(u.work_start || "09:00")}">
            </div>
            <div class="field">
              <label>Work ends</label>
              <input type="time" id="ob-wend" value="${esc(u.work_end || "17:00")}">
            </div>
            <div class="row-btns" style="margin-top:18px">
              <button class="btn btn-ghost" id="ob-back-2">Back</button>
              <button class="btn btn-primary" id="ob-next-2">Next</button>
            </div>
          </div>
          <div class="step" id="step-3">
            <h2>Preferences</h2>
            <p class="small">Tailor the experience to you.</p>
            <label class="toggle-row">
              <input type="checkbox" id="ob-reminders">
              <span class="slider-thumb"></span>
              <span>Remind me to log my energy daily</span>
            </label>
            <div class="field" style="margin-top:6px">
              <label>Theme</label>
              <select id="ob-theme" style="width:100%;height:46px;border-radius:10px;border:1px solid var(--border);background:var(--bg-deep);color:var(--text);padding:0 14px;font-size:0.92rem;">
                <option value="dark">Dark (cinematic)</option>
                <option value="light">Light</option>
              </select>
            </div>
            <div class="row-btns" style="margin-top:22px">
              <button class="btn btn-ghost" id="ob-back-3">Back</button>
              <button class="btn btn-primary" id="ob-finish">Finish setup</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  `;

  let step = 1;
  const show = (n) => {
    $$("#view-onboarding .step").forEach((s) => s.classList.remove("active"));
    $(`#step-${n}`).classList.add("active");
    $$("#view-onboarding .step-dot").forEach((d, i) => {
      d.className = "step-dot" + (i < n - 1 ? " done" : i === n - 1 ? " current" : "");
    });
    step = n;
  };
  $("#ob-next-1").onclick = () => show(2);
  $("#ob-back-2").onclick = () => show(1);
  $("#ob-next-2").onclick = () => show(3);
  $("#ob-back-3").onclick = () => show(2);
  $("#ob-finish").onclick = async () => {
    const payload = {
      wake_time: $("#ob-wake").value,
      sleep_time: $("#ob-sleep").value,
      work_start: $("#ob-wstart").value,
      work_end: $("#ob-wend").value,
      theme: $("#ob-theme").value,
      tz: Intl.DateTimeFormat().resolvedOptions().timeZone,
    };
    try {
      await api("/api/onboarding", "PUT", payload);
      await me();
      applyTheme(payload.theme);
      localStorage.setItem("aura_theme", payload.theme);
      location.hash = "#home";
    } catch (err) {
      alert(err.message || "Setup failed");
    }
  };
}

// ---------------- home / aura ----------------
function energyColor(v) {
  const t = (v - 1) / 9;
  const c1 = [239, 68, 68]; // red
  const c2 = [245, 158, 11]; // amber
  const c3 = [34, 197, 94]; // green
  const k = t < 0.5 ? t * 2 : (t - 0.5) * 2;
  const a = t < 0.5 ? c1 : c2;
  const b = t < 0.5 ? c2 : c3;
  return `rgb(${Math.round(a[0] + (b[0] - a[0]) * k)},${Math.round(a[1] + (b[1] - a[1]) * k)},${Math.round(a[2] + (b[2] - a[2]) * k)})`;
}

async function loadHomeData() {
  try {
    const { checkins } = await api("/api/checkins?limit=200");
    state.checkins = checkins;
    const today = localDateStr();
    state.todayCheckin = checkins.find((c) => c.ts.slice(0, 10) === today) || null;
    const ins = await api("/api/insights");
    state.insights = ins;
  } catch (e) {
    state.checkins = [];
  }
}

function renderHome() {
  const ci = state.todayCheckin;
  const ins = state.insights || { count: 0, avg_energy: null, streak: 0 };
  const done = !!ci;
  const moodColor = MOOD_COLORS[state.mood] || MOOD_COLORS.neutral;

  let appHtml = `
    <section class="view active" id="view-home">
      <div class="panel">
        <div class="aura-header">
          <div class="aura-kicker">Your energy, today</div>
          <h1 class="aura-title">Good ${new Date().getHours() < 12 ? "morning" : new Date().getHours() < 18 ? "afternoon" : "evening"}, ${esc(state.user.name || "there")}.</h1>
          <p class="small">Log how you feel &mdash; your Aura paints it.</p>
        </div>

        <div class="card elev" style="margin-bottom:14px;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap">
            <div class="stat">
              <span class="label">Avg energy (7d)</span>
              <span class="value">${ins.avg_energy == null ? "\u2014" : ins.avg_energy}</span>
            </div>
            <div class="stat">
              <span class="label">Streak</span>
              <span class="value">${ins.streak}</span>
            </div>
            <div class="stat">
              <span class="label">Check-ins</span>
              <span class="value">${ins.count}</span>
            </div>
          </div>
        </div>

        ${done
          ? `
          <div class="card" style="margin-bottom:14px;">
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:10px;">
              <span style="font-size:1.6rem">${MOOD_EMOJIS[ci.mood] || "😐"}</span>
              <div>
                <div style="font-weight:800;color:var(--ink);font-size:0.95rem;">Logged today</div>
                <div style="font-size:0.75rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.08em">${esc(MOOD_LABELS[ci.mood] || ci.mood)}</div>
              </div>
            </div>
            <div style="display:flex;gap:6px;flex-wrap:wrap;margin-bottom:8px;">
              ${(ci.tags || []).map((t) => `<span class="tag" style="background:color-mix(in srgb, var(--accent) 15%, var(--surface));border-color:color-mix(in srgb, var(--accent) 30%, var(--border));color:var(--accent-glow)">${esc(t)}</span>`).join("") || "<span class='tag'>&mdash;</span>"}
            </div>
            ${ci.note ? `<div style="font-size:0.86rem;color:var(--text-dim);">“${esc(ci.note)}”</div>` : ""}
            <div class="row-btns" style="margin-top:10px">
              <button class="btn btn-ghost" id="btn-edit-today">Edit entry</button>
            </div>
          </div>
          `
          : `
          <div class="checkin-composer">
            <div class="range-wrap">
              <div class="range-track"><div class="range-fill" id="energy-fill"></div></div>
              <input type="range" min="1" max="10" value="${state.energy}" id="range-energy">
              <div class="range-thumb" id="energy-thumb"></div>
              <div class="range-val" id="energy-val">${state.energy}</div>
              <div class="range-hint">Energy</div>
            </div>
            <div class="range-wrap" style="margin-top:10px;">
              <div class="range-track"><div class="range-fill" id="focus-fill"></div></div>
              <input type="range" min="1" max="10" value="${state.focus}" id="range-focus">
              <div class="range-thumb" id="focus-thumb"></div>
              <div class="range-val" id="focus-val">${state.focus}</div>
              <div class="range-hint">Focus</div>
            </div>
            <div style="font-size:0.7rem;font-weight:800;letter-spacing:0.14em;text-transform:uppercase;color:var(--text-muted);margin:10px 0 4px;">Mood</div>
            <div class="mood-picker">
              ${Object.entries(MOOD_EMOJIS).map(([k, emoji]) =>
                `<button class="mood-btn ${k === state.mood ? "active" : ""}" data-mood="${k}" aria-pressed="${k === state.mood}" aria-label="${MOOD_LABELS[k]}">${emoji}</button>`
              ).join("")}
            </div>
            <div class="tags-wrap">
              ${TAGS.map((t) =>
                `<button class="tag-chip" data-tag="${t.id}">${t.emoji} ${esc(t.label)}</button>`
              ).join("")}
            </div>
            <textarea class="note-field" id="input-note" placeholder="A word about your day&hellip;"></textarea>
            <button class="btn btn-primary btn-full" id="btn-submit-checkin" style="margin-top:12px;width:100%;">
              Paint today&rsquo;s Aura
            </button>
          </div>
          `
        }

        <div class="aura-panel">
          <canvas id="aura-canvas"></canvas>
          <div class="aura-overlay"></div>
          ${!done ? `<div class="aura-empty"><div class="mark-big">✨</div><div style="margin-top:8px;font-size:0.85rem">Log your first check-in to bring your Aura to life.</div></div>` : ""}
        </div>
        <p class="small" style="margin-top:8px;text-align:center">${!done ? "Your check-in shapes the colors, motion, and intensity of your Aura." : "Your Aura evolves with every entry."}</p>
      </div>
    </section>
  `;
  VIEWS.innerHTML = appHtml;
  // wire composer
  const setupRange = (id, valId, fillId, thumbId, key) => {
    const input = $(id);
    const val = $(valId);
    const fill = $(fillId);
    const thumb = $(thumbId);
    input.setAttribute("role", "slider");
    input.setAttribute("aria-valuemin", input.min);
    input.setAttribute("aria-valuemax", input.max);
    input.setAttribute("aria-label", key === "energy" ? "Energy" : "Focus");
    const apply = () => {
      const p = (input.value - input.min) / (input.max - input.min) * 100;
      fill.style.width = p + "%";
      if (thumb) thumb.style.left = p + "%";
      val.textContent = input.value;
      state[key] = parseInt(input.value, 10);
      input.setAttribute("aria-valuenow", input.value);
    };
    apply();
    input.oninput = apply;
  };
  setupRange("#range-energy", "#energy-val", "#energy-fill", "#energy-thumb", "energy");
  setupRange("#range-focus", "#focus-val", "#focus-fill", "#focus-thumb", "focus");

  $$(".mood-btn").forEach((b) => {
    b.onclick = () => {
      state.mood = b.dataset.mood;
      $$(".mood-btn").forEach((x) => x.classList.toggle("active", x === b));
    };
  });
  $$(".tag-chip").forEach((b) => {
    b.onclick = () => {
      const id = b.dataset.tag;
      b.classList.toggle("active");
      if (state.tags.includes(id)) state.tags = state.tags.filter((t) => t !== id);
      else state.tags.push(id);
    };
  });
  $("#input-note").oninput = (e) => { state.note = e.target.value; };

  $("#btn-submit-checkin").onclick = async () => {
    const btn = $("#btn-submit-checkin");
    btn.disabled = true;
    btn.textContent = "Painting&hellip;";
    try {
      const created = await api("/api/checkins", "POST", {
        energy: state.energy,
        focus: state.focus,
        mood: state.mood,
        tags: state.tags,
        note: state.note,
        ts: localISO(),
      });
      await loadHomeData();
      renderHome();
          } catch (err) {
      alert(err.message || "Could not save check-in");
      btn.disabled = false;
      btn.textContent = "Paint today&rsquo;s Aura";
    }
  };

  if (done) {
    $("#btn-edit-today").onclick = async () => {
      const c = state.todayCheckin;
      state.energy = c.energy;
      state.focus = c.focus;
      state.mood = c.mood;
      state.tags = (c.tags || []).slice();
      state.note = c.note || "";
      state.editId = c.id;
      renderHomeEdit();
    };
  }

  auraInit();
}

function renderHomeEdit() {
  const ci = state.todayCheckin;
  const app = $("#app");
  VIEWS.innerHTML = `
    <section class="view active" id="view-home">
      <div class="panel">
        <div class="aura-header">
          <button class="btn-icon" id="btn-back-home" style="padding:0">&#8592;</button>
          <div class="aura-kicker">Edit entry</div>
          <h1 class="aura-title">Update today&rsquo;s Aura</h1>
        </div>
        <div class="checkin-composer">
          <div class="range-wrap">
            <div class="range-track"><div class="range-fill" id="energy-fill"></div></div>
            <input type="range" min="1" max="10" value="${ci.energy}" id="range-energy">
            <div class="range-thumb" id="energy-thumb"></div>
            <div class="range-val" id="energy-val">${ci.energy}</div>
            <div class="range-hint">Energy</div>
          </div>
          <div class="range-wrap" style="margin-top:10px;">
            <div class="range-track"><div class="range-fill" id="focus-fill"></div></div>
            <input type="range" min="1" max="10" value="${ci.focus}" id="range-focus">
            <div class="range-thumb" id="focus-thumb"></div>
            <div class="range-val" id="focus-val">${ci.focus}</div>
            <div class="range-hint">Focus</div>
          </div>
          <div style="font-size:0.7rem;font-weight:800;letter-spacing:0.14em;text-transform:uppercase;color:var(--text-muted);margin:10px 0 4px;">Mood</div>
          <div class="mood-picker">
            ${Object.entries(MOOD_EMOJIS).map(([k, emoji]) =>
              `<button class="mood-btn ${k === ci.mood ? "active" : ""}" data-mood="${k}" aria-pressed="${k === ci.mood}" aria-label="${MOOD_LABELS[k]}">${emoji}</button>`
            ).join("")}
          </div>
          <div class="tags-wrap">
            ${TAGS.map((t) =>
              `<button class="tag-chip ${(ci.tags || []).includes(t.id) ? "active" : ""}" data-tag="${t.id}">${t.emoji} ${esc(t.label)}</button>`
            ).join("")}
          </div>
          <textarea class="note-field" id="input-note">${esc(ci.note || "")}</textarea>
          <div class="row-btns" style="margin-top:12px">
            <button class="btn btn-ghost" id="btn-cancel-edit">Cancel</button>
            <button class="btn btn-primary" id="btn-save-edit">Save changes</button>
          </div>
        </div>
      </div>
    </section>
  `;

  const setupRange = (id, valId, fillId, thumbId, key) => {
    const input = $(id);
    const val = $(valId);
    const fill = $(fillId);
    const thumb = $(thumbId);
    input.setAttribute("role", "slider");
    input.setAttribute("aria-valuemin", input.min);
    input.setAttribute("aria-valuemax", input.max);
    input.setAttribute("aria-label", key === "energy" ? "Energy" : "Focus");
    const apply = () => {
      const p = (input.value - input.min) / (input.max - input.min) * 100;
      fill.style.width = p + "%";
      if (thumb) thumb.style.left = p + "%";
      val.textContent = input.value;
      state[key] = parseInt(input.value, 10);
      input.setAttribute("aria-valuenow", input.value);
    };
    apply();
    input.oninput = apply;
  };
  setupRange("#range-energy", "#energy-val", "#energy-fill", "#energy-thumb", "energy");
  setupRange("#range-focus", "#focus-val", "#focus-fill", "#focus-thumb", "focus");

  state.mood = ci.mood;
  $$(".mood-btn").forEach((b) => {
    b.onclick = () => { state.mood = b.dataset.mood; $$(".mood-btn").forEach((x) => x.classList.toggle("active", x === b)); };
  });
  state.tags = (ci.tags || []).slice();
  $$(".tag-chip").forEach((b) => {
    b.onclick = () => { const id = b.dataset.tag; b.classList.toggle("active"); if (state.tags.includes(id)) state.tags = state.tags.filter((t) => t !== id); else state.tags.push(id); };
  });
  $("#input-note").oninput = (e) => { state.note = e.target.value; };

  $("#btn-back-home").onclick = () => { state.editId = null; renderHome(); };
  $("#btn-cancel-edit").onclick = () => { state.editId = null; renderHome(); };
  $("#btn-save-edit").onclick = async () => {
    const btn = $("#btn-save-edit");
    btn.disabled = true;
    try {
      await api(`/api/checkins/${state.editId}`, "PUT", {
        energy: state.energy, focus: state.focus, mood: state.mood, tags: state.tags, note: state.note,
      });
      await loadHomeData();
      state.editId = null;
      renderHome();
          } catch (err) {
      alert(err.message || "Update failed");
      btn.disabled = false;
    }
  };
}

// ---------------- rhythm ----------------
function renderRhythm() {
  const ins = state.insights || {
    count: 0, avg_energy: null, avg_focus: null, peak_hour: null, best_day: null,
    streak: 0, hourly: [], weekday: [], best_window: null, week_comparison: null,
    moods: {}, insights: ["Log your first check-in to see your rhythm emerge."],
  };
  const u = state.user || {};
  const hasData = ins.count > 0;

  let weekdayBars = "";
  if (hasData) {
    weekdayBars = ins.weekday.map((d) => {
      const pct = d.energy ? Math.round((d.energy / 10) * 100) : 0;
      const hue = moodHue(d.mood || "neutral");
      return `
        <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:6px">
          <div style="font-size:0.68rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted)">${esc(d.day.slice(0,3))}</div>
          <div style="width:26px;height:26px;border-radius:50%;border:2px solid var(--border);display:grid;place-items:center;position:relative">
            <div style="width:14px;height:14px;border-radius:50%;background:linear-gradient(135deg,var(--accent),var(--accent-2));box-shadow:0 0 10px color-mix(in srgb, var(--accent) 50%, transparent);transform:scale(${d.energy ? 0.85 + (d.energy/10)*0.3 : 0.55});opacity:${d.energy ? 1 : 0.35}"></div>
            <span style="position:absolute;bottom:-14px;font-size:0.62rem;font-weight:800;color:var(--text-dim);white-space:nowrap">${d.energy == null ? "\u2014" : d.energy}</span>
          </div>
          <div style="font-size:0.7rem;color:var(--text-muted)">${d.n || 0}</span>
        </div>
      `;
    }).join("");
  }

  VIEWS.innerHTML = `
    <section class="view active" id="view-rhythm">
      <div class="panel">
        <div class="aura-header">
          <h1>Your rhythm</h1>
          <p class="small">A circadian view of your energy &mdash; your best focus window, strongest days, and weekly trend.</p>
        </div>

        ${!hasData ? `
        <div class="empty-state" style="padding:40px 16px">
          <div class="mark-big">🕰️</div>
          <div style="margin-top:10px;font-size:0.85rem">Log check-ins to see your circadian rhythm.</div>
        </div>
        ` : `
        <div style="display:flex;flex-direction:column;gap:10px;align-items:center">
          <canvas id="rhythm-canvas" width="340" height="340" style="width:340px;height:340px;border-radius:50%;border:1px solid var(--border);background:var(--surface);box-shadow:var(--shadow-sm)"></canvas>
          <div style="margin-top:10px;display:flex;gap:14px;flex-wrap:wrap;justify-content:center;align-items:center;font-size:0.8rem;color:var(--text-dim)">
            ${ins.peak_hour != null ? `<span>Peak: <b style="color:var(--ink)">${ins.peak_hour}:00</b></span>` : ""}
            ${ins.best_window ? `<span>Best focus: <b style="color:var(--ink)">${ins.best_window.start_hour}:00\u2013${ins.best_window.end_hour}:00</b></span>` : ""}
            ${ins.avg_energy != null ? `<span>Avg energy: <b style="color:var(--ink)">${ins.avg_energy}</b></span>` : ""}
          </div>
        </div>

        ${u.wake_time || u.sleep_time || u.work_start || u.work_end ? `
        <div class="card" style="margin-top:12px">
          <div style="font-size:0.7rem;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:var(--text-muted);margin-bottom:8px">Your day</div>
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            ${u.wake_time ? `<div class="stat" style="flex:1;min-width:90px"><span class="label">Wake</span><span class="value">${esc(u.wake_time)}</span></div>` : ""}
            ${u.sleep_time ? `<div class="stat" style="flex:1;min-width:90px"><span class="label">Sleep</span><span class="value">${esc(u.sleep_time)}</span></div>` : ""}
            ${u.work_start && u.work_end ? `<div class="stat" style="flex:1;min-width:90px"><span class="label">Work</span><span class="value">${esc(u.work_start)}\u2013${esc(u.work_end)}</span></div>` : ""}
          </div>
        </div>
        ` : ""}

        <div class="card" style="margin-top:12px">
          <div style="font-size:0.7rem;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:var(--text-muted);margin-bottom:10px">Energy by weekday</div>
          <div style="display:flex;gap:4px">${weekdayBars}</div>
        </div>

        <div style="margin-top:14px">
          <h2 style="font-size:1rem;">Insights</h2>
          ${ins.insights.map((m) => `<div class="insight"><div class="q">&#8226;</div><div class="p">${esc(m)}</div></div>`).join("")}
          ${ins.week_comparison && ins.week_comparison.change != null ? `
          <div class="insight" style="margin-top:8px">
            <div class="q" style="color:var(--accent)">↗</div>
            <div class="p">This week avg energy <b>${ins.week_comparison.avg}</b>${ins.week_comparison.change >= 0 ? " (+"+ins.week_comparison.change+") vs last week" : " ("+ins.week_comparison.change+") vs last week"}</div>
          </div>
          ` : ""}
        </div>
        `}
      </div>
    </section>
  `;
  if (hasData) setTimeout(() => drawRhythmClock(), 50);
}

function drawRhythmClock() {
  const canvas = $("#rhythm-canvas");
  if (!canvas) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const size = 340;
  canvas.width = size * dpr;
  canvas.height = size * dpr;
  canvas.style.width = size + "px";
  canvas.style.height = size + "px";
  const ctx = canvas.getContext("2d");
  ctx.scale(dpr, dpr);

  const ins = state.insights || {};
  const hourly = ins.hourly || [];
  const best_window = ins.best_window;
  const ctxW = size;
  const ctxH = size;
  const cx = ctxW / 2;
  const cy = ctxH / 2;
  const outerR = size * 0.44;
  const innerR = size * 0.06;

  const bg = parseCssColor(cssColor("--bg-deep", "#07080e")) || [7, 8, 14];
  ctx.fillStyle = `rgb(${bg[0]}, ${bg[1]}, ${bg[2]})`;
  ctx.fillRect(0, 0, ctxW, ctxH);

  // subtle hour ticks (every 3h)
  ctx.strokeStyle = parseCssColor(cssColor("--border", "#242a38"));
  if (ctx.strokeStyle) {
    ctx.globalAlpha = 0.15;
    ctx.lineWidth = 1;
    for (let h = 0; h < 24; h++) {
      if (h % 3 !== 0) continue;
      const a = (h / 24) * Math.PI * 2 - Math.PI / 2;
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * (outerR - 6), cy + Math.sin(a) * (outerR - 6));
      ctx.lineTo(cx + Math.cos(a) * outerR, cy + Math.sin(a) * outerR);
      ctx.stroke();
    }
    ctx.globalAlpha = 1;
  }

  // build points for hours that have data
  const pts = [];
  for (let h = 0; h < 24; h++) {
    const d = hourly[h];
    if (!d || d.n === 0 || d.energy == null) continue;
    const a = (h / 24) * Math.PI * 2 - Math.PI / 2;
    const r = innerR + (d.energy / 10) * (outerR - innerR);
    pts.push({ h, x: cx + Math.cos(a) * r, y: cy + Math.sin(a) * r, energy: d.energy, a });
  }

  if (pts.length >= 2) {
    // fill under curve (toward center)
    const grad = ctx.createLinearGradient(cx, cy - outerR, cx, cy + outerR);
    grad.addColorStop(0, `hsla(${moodHue(state.mood || 'neutral')}, 60%, 55%, 0.25)`);
    grad.addColorStop(1, `hsla(${moodHue(state.mood || 'neutral')}, 60%, 55%, 0.05)`);
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i].x, pts[i].y);
    // close toward center along the arc span
    ctx.lineTo(cx + Math.cos(pts[pts.length-1].a) * innerR, cy + Math.sin(pts[pts.length-1].a) * innerR);
    for (let i = pts.length - 2; i >= 0; i--) ctx.lineTo(cx + Math.cos(pts[i].a) * innerR, cy + Math.sin(pts[i].a) * innerR);
    ctx.closePath();
    ctx.fill();

    // curve stroke
    ctx.lineWidth = 3;
    ctx.lineJoin = "round";
    ctx.lineCap = "round";
    ctx.strokeStyle = `hsla(${moodHue(state.mood || 'neutral')}, 70%, 55%, 0.95)`;
    ctx.shadowColor = `hsla(${moodHue(state.mood || 'neutral')}, 70%, 55%, 0.5)`;
    ctx.shadowBlur = 10;
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i].x, pts[i].y);
    ctx.stroke();
    ctx.shadowBlur = 0;

    // dots per point
    for (const p of pts) {
      ctx.beginPath();
      ctx.arc(p.x, p.y, 3.5, 0, Math.PI * 2);
      ctx.fillStyle = energyColor(p.energy);
      ctx.fill();
    }
  }

  // best focus window arc
  if (best_window) {
    const startA = (best_window.start_hour / 24) * Math.PI * 2 - Math.PI / 2;
    const endA = (best_window.end_hour / 24) * Math.PI * 2 - Math.PI / 2;
    ctx.lineWidth = 6;
    ctx.lineCap = "round";
    ctx.strokeStyle = `hsla(${moodHue('great')}, 80%, 55%, 0.9)`;
    ctx.shadowColor = `hsla(${moodHue('great')}, 80%, 55%, 0.6)`;
    ctx.shadowBlur = 14;
    ctx.beginPath();
    ctx.arc(cx, cy, outerR + 10, startA, endA, false);
    ctx.stroke();
    ctx.shadowBlur = 0;
    // label
    const midA = (startA + endA) / 2;
    ctx.font = "600 10px system-ui";
    ctx.fillStyle = "var(--ink)";
    ctx.textAlign = "center";
    ctx.fillText("best focus", cx + Math.cos(midA) * (outerR + 24), cy + Math.sin(midA) * (outerR + 24) + 3);
  }

  // center dot
  ctx.beginPath();
  ctx.arc(cx, cy, 5, 0, Math.PI * 2);
  ctx.fillStyle = parseCssColor(cssColor("--border-focus", "#7c6ef0")) ? `rgb(${parseCssColor(cssColor("--border-focus","#7c6ef0")).join(",")})` : "#7c6ef0";
  ctx.fill();

  // cardinal hour labels
  ctx.fillStyle = "var(--text-muted)";
  ctx.font = "600 9px system-ui";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  const labels = { 0: "0", 6: "6", 12: "12", 18: "18" };
  for (const [h, lab] of Object.entries(labels)) {
    const hh = parseInt(h);
    const a = (hh / 24) * Math.PI * 2 - Math.PI / 2;
    ctx.fillText(lab, cx + Math.cos(a) * (outerR + 16), cy + Math.sin(a) * (outerR + 16));
  }
}

// ---------------- history ----------------
function renderHistory() {
  const f = state.historyFilter;
  let list = state.checkins.slice();
  // filter by mood
  if (f.mood) list = list.filter((c) => c.mood === f.mood);
  // search by note or tag
  if (f.q) {
    const q = f.q.toLowerCase();
    list = list.filter((c) =>
      (c.note || "").toLowerCase().includes(q) ||
      (c.tags || []).some((t) => t.toLowerCase().includes(q))
    );
  }
  // sort
  if (f.sort === "date") list.sort((a, b) => b.ts.localeCompare(a.ts));
  else if (f.sort === "energy") list.sort((a, b) => b.energy - a.energy);
  else if (f.sort === "focus") list.sort((a, b) => b.focus - a.focus);

  const moodOptions = [
    { id: "great", emoji: "😊", label: "Great" },
    { id: "good", emoji: "🙂", label: "Good" },
    { id: "neutral", emoji: "😐", label: "Neutral" },
    { id: "low", emoji: "😔", label: "Low" },
    { id: "tired", emoji: "😴", label: "Tired" },
  ];

  const moodChips = moodOptions.map((m) => {
    const active = f.mood === m.id;
    return `<button class="tag-chip ${active ? "active" : ""}" data-mood="${m.id}">${m.emoji} ${m.label}</button>`;
  }).join("");

  VIEWS.innerHTML = `
    <section class="view active" id="view-history">
      <div class="panel">
        <div class="aura-header">
          <h1>History</h1>
          <p class="small">Every check-in you&rsquo;ve painted.</p>
        </div>
        <div class="card" style="margin-bottom:12px">
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:10px">
            <div class="tag" style="background:var(--surface-elev);border-color:var(--border);color:var(--text-dim);font-size:0.68rem">${list.length} entr${list.length === 1 ? "y" : "ies"}</div>
            <input type="text" id="hist-search" placeholder="Search note or tag&hellip;" value="${esc(f.q)}" style="flex:1;min-width:140px;height:36px;border-radius:9px;border:1px solid var(--border);background:var(--bg-deep);color:var(--text);padding:0 10px;font-size:0.85rem">
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;align-items:center">
            <span style="font-size:0.68rem;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:var(--text-muted);margin-right:6px">Mood</span>
            ${moodChips}
            <span style="font-size:0.68rem;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:var(--text-muted);margin:0 6px 0 10px">Sort</span>
            <select id="hist-sort" style="height:34px;border-radius:8px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-size:0.8rem;padding:0 8px">
              <option value="date" ${f.sort === "date" ? "selected" : ""}>Date (newest)</option>
              <option value="energy" ${f.sort === "energy" ? "selected" : ""}>Energy (high)</option>
              <option value="focus" ${f.sort === "focus" ? "selected" : ""}>Focus (high)</option>
            </select>
          </div>
        </div>
        <div class="history-list">
          ${list.map((c) => {
            const dt = new Date(c.ts);
            const day = dt.toLocaleDateString(undefined, { weekday: "short" });
            const time = dt.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
            const moodKey = c.mood === "great" ? "great" : c.mood === "good" ? "good" : c.mood === "neutral" ? "neutral" : c.mood === "low" ? "low" : "tired";
            return `
              <div class="history-row" data-id="${c.id}">
                <div class="history-meta">
                  <div class="history-time">${esc(time)}</div>
                  <div class="history-day">${esc(day)} &bull; ${esc(c.ts.slice(0, 10))}</div>
                </div>
                <div class="history-vals">
                  <span class="history-val" style="color:var(--mood-${moodKey})">${c.energy}</span>
                  <span class="history-val" style="color:var(--mood-good)">${c.focus}</span>
                  <span style="font-size:1.2rem;color:var(--text-muted)">${MOOD_EMOJIS[c.mood] || "😐"}</span>
                </div>
                <div class="history-actions">
                  <button class="btn-tiny" data-id="${c.id}" title="Delete">&#215;</button>
                </div>
              </div>
            `;
          }).join("") || `
          <div class="empty-state">
            <div class="mark-big">&#8226;</div>
            <div style="margin-top:8px;font-size:0.85rem">No matching entries.</div>
          </div>
          `}
        </div>
      </div>
    </section>
  `;

  // wire filters
  $("#hist-search").oninput = (e) => { f.q = e.target.value; renderHistory(); };
  $("#hist-sort").onchange = (e) => { f.sort = e.target.value; renderHistory(); };
  $$(".tag-chip[data-mood]").forEach((b) => {
    b.onclick = () => {
      f.mood = b.dataset.mood === f.mood ? "" : b.dataset.mood;
      renderHistory();
    };
  });
  $$(".history-actions .btn-tiny").forEach((b) => {
    b.onclick = async () => {
      if (!confirm("Delete this entry?")) return;
      try {
        await api(`/api/checkins/${b.dataset.id}`, "DELETE");
        await loadHomeData();
        renderHistory();
      } catch (err) { alert(err.message || "Delete failed"); }
    };
  });
}

// ---------------- settings ----------------
async function loadSettings() {
  try {
    const s = await api("/api/settings");
    state.settingsTheme = s.theme || "dark";
    state.settingsReminders = s.reminders === true || s.reminders === 1;
    state.theme = state.settingsTheme;
    applyTheme(state.settingsTheme);
  } catch (e) {
    state.settingsReminders = false;
  }
}

function renderSettings() {
  const u = state.user || {};
  loadSettings();
  VIEWS.innerHTML = `
    <section class="view active" id="view-settings">
      <div class="panel">
        <div class="aura-header">
          <h1>Settings</h1>
          <p class="small">Account, appearance, and data.</p>
        </div>
        <div class="settings-group">
          <span class="t">Profile</span>
          <div class="field">
            <label>Name</label>
            <input type="text" id="set-name" value="${esc(u.name || "")}">
          </div>
          <div class="field">
            <label>Email</label>
            <input type="email" id="set-email" value="${esc(u.email || "")}" readonly>
          </div>
          <button class="btn btn-primary" id="btn-save-profile" style="margin-top:10px">Save profile</button>
        </div>
        <div class="settings-group">
          <span class="t">Appearance</span>
          <div class="theme-toggle" id="btn-theme">
            <span id="theme-label">${state.settingsTheme === "dark" ? "Dark" : "Light"}</span>
            <span class="slider-thumb"></span>
          </div>
          <label class="toggle-row">
            <input type="checkbox" id="set-reminders" ${state.settingsReminders ? "checked" : ""}>
            <span class="slider-thumb"></span>
            <span>Daily reminder to log my energy</span>
          </label>
        </div>
        <div class="settings-group">
          <span class="t">Data</span>
          <button class="btn btn-ghost" id="btn-export" style="width:100%;text-align:left">Export my data (JSON)</button>
          <button class="btn btn-ghost" id="btn-delete-account" style="width:100%;text-align:left;margin-top:8px;color:var(--energy-1)">Delete account &amp; all data</button>
        </div>
        <div class="danger-zone" style="margin-top:16px">
          <span class="t" style="margin-bottom:8px">Danger zone</span>
          <button class="btn btn-ghost" id="btn-delete-account2" style="width:100%;text-align:left;color:var(--energy-1);font-weight:700">Permanently delete my account</button>
        </div>
        <button class="btn btn-ghost" id="btn-logout" style="margin-top:16px;width:100%">Sign out</button>
      </div>
    </section>
  `;

  $("#btn-save-profile").onclick = async () => {
    const btn = $("#btn-save-profile");
    btn.disabled = true;
    try {
      await api("/api/onboarding", "PUT", {
        name: $("#set-name").value || state.user.name,
      });
      await me();
      renderSettings();
    } catch (err) { alert(err.message || "Update failed"); btn.disabled = false; }
  };

  $("#btn-theme").onclick = async () => {
    const next = state.theme === "dark" ? "light" : "dark";
    applyTheme(next);
    localStorage.setItem("aura_theme", next);
    $("#theme-label").textContent = next === "dark" ? "Dark" : "Light";
    const thumb = $("#btn-theme .slider-thumb");
    thumb.style.transform = next === "light" ? "translateX(20px)" : "";
    try {
      await api("/api/settings", "PUT", { theme: next, reminders: state.settingsReminders });
    } catch (e) {}
  };
  const thumb = $("#btn-theme .slider-thumb");
  thumb.style.transform = state.theme === "light" ? "translateX(20px)" : "";

  $("#set-reminders").onclick = async () => {
    const next = $("#set-reminders").checked;
    state.settingsReminders = next;
    try {
      await api("/api/settings", "PUT", { theme: state.theme, reminders: next });
    } catch (e) {}
  };

  $("#btn-export").onclick = async () => {
    try {
      const data = await api("/api/export");
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `aura-export-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) { alert(err.message || "Export failed"); }
  };

  const doDelete = async () => {
    if (!confirm("This permanently deletes your account and all check-ins. Cannot be undone.")) return;
    if (!confirm("Are you absolutely sure?")) return;
    try {
      await api("/api/account", "DELETE");
      state.user = null;
      location.hash = "#login";
    } catch (err) { alert(err.message || "Delete failed"); }
  };
  $("#btn-delete-account").onclick = doDelete;
  $("#btn-delete-account2").onclick = doDelete;
  $("#btn-logout").onclick = async () => {
    try { await api("/api/auth/logout", "POST"); } catch (e) {}
    state.user = null;
    location.hash = "#login";
  };
}

// ---------------- canvas generative artwork ----------------
let auraAnim;
let auraCtx;
let auraCanvas;
let auraParticles = [];
let auraField = [];
let auraTime = 0;
let auraInitialized = false;

function localDateStr(d = new Date()) {
  const tzOffMs = d.getTimezoneOffset() * 60000;
  const localMs = d.getTime() - tzOffMs;
  return new Date(localMs).toISOString().slice(0, 10);
}

function cssColor(varName, fallback) {
  try {
    return getComputedStyle(document.documentElement).getPropertyValue(varName).trim() || fallback;
  } catch (e) { return fallback; }
}

function parseCssColor(css) {
  const d = document.createElement("div");
  d.style.color = css;
  document.body.appendChild(d);
  const comp = getComputedStyle(d).color;
  document.body.removeChild(d);
  const m = comp.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/);
  return m ? [+m[1], +m[2], +m[3]] : null;
}

function moodHue(mood) {
  return { great: 140, good: 210, neutral: 260, low: 340, tired: 280 }[mood] || 260;
}

function auraInit() {
  auraCanvas = $("#aura-canvas");
  if (!auraCanvas) return;
  if (!auraInitialized) {
    auraCtx = auraCanvas.getContext("2d", { alpha: false });
    auraCanvas.addEventListener("pointermove", (e) => {
      const r = auraCanvas.getBoundingClientRect();
      state.pointer.x = e.clientX - r.left;
      state.pointer.y = e.clientY - r.top;
      state.pointer.active = true;
    });
    auraCanvas.addEventListener("pointerleave", () => { state.pointer.active = false; });
    auraCanvas.addEventListener("touchmove", (e) => {
      const r = auraCanvas.getBoundingClientRect();
      const t = e.touches[0];
      state.pointer.x = t.clientX - r.left;
      state.pointer.y = t.clientY - r.top;
      state.pointer.active = true;
    }, { passive: true });
    auraInitialized = true;
  }
  auraResize();
  if (!auraAnim) auraLoop();
}

// Rebuild vortices whenever today's check-ins change (called from renderHome).
function auraBuildVortices() {
  const today = localDateStr();
  const todays = state.checkins.filter((c) => c.ts.slice(0, 10) === today);
  const source = todays.length ? todays : state.checkins.slice(-3);
  auraVortices = source.map((c) => {
    const hour = parseInt(c.ts.slice(11, 13), 10) || 12;
    const angle = (hour / 24) * Math.PI * 2 - Math.PI / 2; // 0 at top, clockwise
    return {
      hour, energy: c.energy, mood: c.mood,
      angle, strength: c.energy / 10,
      today: c.ts.slice(0, 10) === today,
    };
  });
}

function auraResize() {
  if (!auraCanvas) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const r = auraCanvas.getBoundingClientRect();
  const w = Math.max(r.width, 1);
  const h = Math.max(r.height, 1);
  auraCanvas.width = w * dpr;
  auraCanvas.height = h * dpr;
  auraCanvas.style.width = w + "px";
  auraCanvas.style.height = h + "px";
  auraCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
  auraBuildField(w, h);
  auraSpawnParticles();
  auraBuildVortices();
}

function auraBuildField(w, h) {
  const cols = Math.ceil(w / 24) + 1;
  const rows = Math.ceil(h / 24) + 1;
  auraField = { cols, rows, w, h, data: new Float32Array(cols * rows * 2) };
}

function auraFieldAt(fx, fy) {
  const { cols, rows, w, h, data } = auraField;
  const cx = Math.min(Math.max(Math.floor(fx / w * cols), 0), cols - 1);
  const cy = Math.min(Math.max(Math.floor(fy / h * rows), 0), rows - 1);
  const i = (cy * cols + cx) * 2;
  return [data[i], data[i + 1]];
}

function auraSpawnParticles() {
  const count = window.innerWidth < 768 ? 140 : 300;
  auraParticles = [];
  const w = auraCanvas.width / (Math.min(window.devicePixelRatio || 1, 2));
  const h = auraCanvas.height / (Math.min(window.devicePixelRatio || 1, 2));
  for (let i = 0; i < count; i++) {
    auraParticles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: 0, vy: 0,
      life: Math.random(),
      size: 1.2 + Math.random() * 1.8,
      angle: Math.random() * Math.PI * 2,
    });
  }
}

function auraLoop() {
  if (!auraCanvas || !auraCtx) { requestAnimationFrame(auraLoop); return; }
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const w = auraCanvas.width / dpr;
  const h = auraCanvas.height / dpr;
  const cx = w / 2;
  const cy = h / 2;
  const R = Math.min(w, h) * 0.42;

  // base slow curl-noise field
  const { cols, rows, data } = auraField;
  const t = auraTime * 0.0004;
  for (let cyi = 0; cyi < rows; cyi++) {
    for (let cxi = 0; cxi < cols; cxi++) {
      const x = (cxi / cols) * w;
      const y = (cyi / rows) * h;
      const i = (cyi * cols + cxi) * 2;
      const nx = Math.sin(x * 0.0035 + t) * Math.cos(y * 0.0035 + t * 0.7);
      const ny = Math.cos(x * 0.0035 + t * 0.6) * Math.sin(y * 0.0035 + t * 0.5);
      const m = Math.hypot(nx, ny) || 1;
      data[i] = nx / m;
      data[i + 1] = ny / m;
    }
  }

  // trail clear (resolve CSS variable so canvas understands it)
  const bg = parseCssColor(cssColor("--bg-deep", "#07080e")) || [7, 8, 14];
  auraCtx.fillStyle = `rgb(${bg[0]}, ${bg[1]}, ${bg[2]})`;
  auraCtx.globalAlpha = 0.5;
  auraCtx.fillRect(0, 0, w, h);
  auraCtx.globalAlpha = 1;

  const energy = state.energy || 6;
  const focus = state.focus || 6;
  const speed = 0.25 + (energy / 10) * 1.0;
  const hue = moodHue(state.mood);

  // draw the day ring + vortex dots (data legibility within the art)
  if (auraVortices.length) {
    const border = parseCssColor(cssColor("--border", "#242a38")) || [36, 42, 56];
    auraCtx.strokeStyle = `rgb(${border[0]}, ${border[1]}, ${border[2]})`;
    auraCtx.beginPath();
    auraCtx.arc(cx, cy, R * 0.78, 0, Math.PI * 2);
    auraCtx.globalAlpha = 0.12;
    auraCtx.stroke();
    auraCtx.globalAlpha = 1;
    for (const v of auraVortices) {
      const vx = cx + Math.cos(v.angle) * R * (0.22 + 0.56 * v.strength);
      const vy = cy + Math.sin(v.angle) * R * (0.22 + 0.56 * v.strength);
      const vh = moodHue(v.mood);
      const vsize = 3 + v.energy * 0.7;
      const grad = auraCtx.createRadialGradient(vx, vy, 0, vx, vy, vsize * 2.2);
      grad.addColorStop(0, `hsla(${vh}, 75%, 60%, ${v.today ? 0.55 : 0.25})`);
      grad.addColorStop(1, `hsla(${vh}, 75%, 60%, 0)`);
      auraCtx.fillStyle = grad;
      auraCtx.beginPath();
      auraCtx.arc(vx, vy, vsize * 2.2, 0, Math.PI * 2);
      auraCtx.fill();
    }
  }

  for (const p of auraParticles) {
    // base flow field force
    const [bfx, bfy] = auraFieldAt(p.x, p.y);
    p.vx += bfx * 0.35;
    p.vy += bfy * 0.35;

    // vortices sculpt the flow (your check-ins)
    for (const v of auraVortices) {
      const vx = cx + Math.cos(v.angle) * R * (0.22 + 0.56 * v.strength);
      const vy = cy + Math.sin(v.angle) * R * (0.22 + 0.56 * v.strength);
      const dx = p.x - vx;
      const dy = p.y - vy;
      const d = Math.hypot(dx, dy) || 1;
      const sigma = R * 0.4;
      const influence = v.strength * Math.exp(-(d * d) / (2 * sigma * sigma));
      if (influence > 0.02) {
        p.vx += (-dy / d) * influence * 0.9;
        p.vy += (dx / d) * influence * 0.9;
      }
    }

    // pointer repulsion (interactive)
    if (state.pointer.active) {
      const dx = p.x - state.pointer.x;
      const dy = p.y - state.pointer.y;
      const d = Math.hypot(dx, dy) || 1;
      if (d < 150) {
        const force = (150 - d) / 150;
        p.vx += (dx / d) * force * 1.7;
        p.vy += (dy / d) * force * 1.7;
      }
    }

    p.vx *= 0.86;
    p.vy *= 0.86;
    p.x += p.vx * speed;
    p.y += p.vy * speed;

    if (p.x < 0) p.x = w;
    if (p.x > w) p.x = 0;
    if (p.y < 0) p.y = h;
    if (p.y > h) p.y = 0;

    p.life += 0.002;
    const pulse = 0.55 + 0.45 * Math.sin(p.life + auraTime * 0.0012 + p.angle);

    const sat = 40 + (energy / 10) * 45;
    const light = 50 + pulse * 14;
    const alpha = 0.45 + (focus / 10) * 0.3 + pulse * 0.2;

    auraCtx.beginPath();
    auraCtx.arc(p.x, p.y, p.size * (0.7 + 0.5 * pulse), 0, Math.PI * 2);
    auraCtx.fillStyle = `hsla(${hue}, ${sat}%, ${light}%, ${alpha})`;
    auraCtx.fill();
  }

  auraTime += 16;
  requestAnimationFrame(auraLoop);
}

// ---------------- router & init ----------------
window.addEventListener("hashchange", nav);
window.addEventListener("load", async () => {
  applyTheme(state.theme);
  document.body.classList.add("ready");
  await me();
  auraInit();
  nav();
});

// expose internals for inline handlers
window.renderHome = renderHome;
window.renderHomeEdit = renderHomeEdit;
window.loadHomeData = loadHomeData;
