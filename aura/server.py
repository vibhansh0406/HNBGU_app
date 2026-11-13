"""Aura — Personal Energy & Circadian Rhythm Companion.

Zero-dependency backend built on the Python standard library:
http.server + sqlite3 + hashlib (pbkdf2) + secrets.

Endpoints live under /api/*; everything else serves the SPA.
"""

import http.server
import json
import os
import sqlite3
import threading
import time
import hashlib
from zoneinfo import ZoneInfo
import secrets
import urllib.parse
from datetime import datetime, date, timedelta, timezone

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "aura.db")
SESSION_TTL_SECONDS = 30 * 24 * 60 * 60  # 30 days
PORT = int(os.environ.get("PORT", "8765"))

# ---------------------------------------------------------------------------
# Database
# ---------------------------------------------------------------------------

_db_lock = threading.RLock()


def get_conn():
    conn = sqlite3.connect(DB_PATH, timeout=30, isolation_level=None)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    conn.execute("PRAGMA busy_timeout=30000")
    return conn


def init_db():
    with _db_lock:
        conn = get_conn()
        try:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    name TEXT,
                    wake_time TEXT,      -- 'HH:MM'
                    sleep_time TEXT,     -- 'HH:MM'
                    work_start TEXT,     -- 'HH:MM'
                    work_end TEXT,       -- 'HH:MM'
                    tz TEXT DEFAULT 'UTC',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS checkins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    ts TEXT NOT NULL,           -- ISO8601 local
                    energy INTEGER NOT NULL,    -- 1..10
                    focus INTEGER NOT NULL,     -- 1..10
                    mood TEXT NOT NULL,         -- emoji / label
                    tags TEXT NOT NULL DEFAULT '[]', -- JSON array
                    note TEXT DEFAULT '',
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS settings (
                    user_id INTEGER PRIMARY KEY,
                    theme TEXT DEFAULT 'dark',
                    reminders INTEGER DEFAULT 1,
                    export_count INTEGER DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS sessions (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    expires_at TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );

                CREATE INDEX IF NOT EXISTS idx_checkins_user_ts ON checkins(user_id, ts);
                CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);
                """
            )
        finally:
            conn.close()


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def parse_ts(ts):
    """Accept ISO8601; return a date-local ISO string (date portion)."""
    if not ts:
        return date.today().isoformat()
    try:
        return ts[:10]
    except Exception:
        return date.today().isoformat()


# ---------------------------------------------------------------------------
# Auth helpers
# ---------------------------------------------------------------------------

def hash_password(password, salt=None):
    if salt is None:
        salt = secrets.token_bytes(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 100_000)
    return salt.hex() + ":" + dk.hex()


def verify_password(password, stored):
    try:
        salt_hex, dk_hex = stored.split(":")
        salt = bytes.fromhex(salt_hex)
        dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 100_000)
        return secrets.compare_digest(dk.hex(), dk_hex)
    except Exception:
        return False


def create_session(user_id):
    token = secrets.token_urlsafe(32)
    expires = (datetime.now(timezone.utc) + timedelta(seconds=SESSION_TTL_SECONDS)).isoformat()
    with _db_lock:
        conn = get_conn()
        try:
            conn.execute(
                "INSERT INTO sessions(token, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)",
                (token, user_id, expires, now_iso()),
            )
            conn.execute("DELETE FROM sessions WHERE expires_at < ?", (now_iso(),))
        finally:
            conn.close()
    return token


def get_current_user(headers):
    cookie = headers.get("Cookie") or ""
    token = None
    for part in cookie.split(";"):
        part = part.strip()
        if part.startswith("aura_session="):
            token = part.split("=", 1)[1]
            break
    if not token:
        return None
    with _db_lock:
        conn = get_conn()
        try:
            row = conn.execute(
                "SELECT u.*, s.expires_at FROM users u JOIN sessions s ON s.user_id = u.id "
                "WHERE s.token = ? AND s.expires_at > ?",
                (token, now_iso()),
            ).fetchone()
            if row:
                # extend session
                new_exp = (datetime.now(timezone.utc) + timedelta(seconds=SESSION_TTL_SECONDS)).isoformat()
                conn.execute(
                    "UPDATE sessions SET expires_at = ? WHERE token = ?",
                    (new_exp, token),
                )
                return sanitize_user(dict(row))
            return None
        finally:
            conn.close()


def sanitize_user(row):
    if not row:
        return None
    return {k: v for k, v in row.items() if k not in ("password_hash", "expires_at")}


def destroy_session(headers):
    cookie = headers.get("Cookie") or ""
    token = None
    for part in cookie.split(";"):
        part = part.strip()
        if part.startswith("aura_session="):
            token = part.split("=", 1)[1]
            break
    if token:
        with _db_lock:
            conn = get_conn()
            try:
                conn.execute("DELETE FROM sessions WHERE token = ?", (token,))
            finally:
                conn.close()


# ---------------------------------------------------------------------------
# Request handler
# ---------------------------------------------------------------------------

class Handler(http.server.BaseHTTPRequestHandler):
    server_version = "Aura/1.0"

    def log_message(self, format, *args):  # noqa: ARG002
        # keep logs quiet but present
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), format % args))

    # ---- small helpers ----------------------------------------------------

    def _send_json(self, status, payload, extra_headers=None):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        if extra_headers:
            for k, v in extra_headers.items():
                self.send_header(k, v)
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            return b""
        return self.rfile.read(length)

    def _parse_json(self, raw):
        try:
            return json.loads(raw.decode("utf-8"))
        except Exception:
            return None

    def _set_session_cookie(self, token):
        self.send_header(
            "Set-Cookie",
            f"aura_session={token}; Path=/; HttpOnly; SameSite=Lax; Max-Age={SESSION_TTL_SECONDS}",
        )

    def _clear_session_cookie(self):
        self.send_header("Set-Cookie", "aura_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0")

    # ---- routing ----------------------------------------------------------

    def do_GET(self):  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        query = urllib.parse.parse_qs(parsed.query)

        # API routes
        if path == "/api/health":
            self._send_json(200, {"ok": True, "service": "aura"})
            return
        if path == "/api/auth/me":
            user = get_current_user(self.headers)
            self._send_json(200, {"user": user or None})
            return
        if path == "/api/checkins":
            self._api_list_checkins(query)
            return
        if path == "/api/insights":
            self._api_insights()
            return
        if path == "/api/settings":
            self._api_settings_get()
            return
        if path == "/api/export":
            self._api_export()
            return
        m = self._match(path, r"/api/checkins/(\d+)$")
        if m:
            self._api_checkin_detail(int(m.group(1)))
            return

        # Static assets
        if path.endswith(".css") or path.endswith(".js") or path.endswith(".png") or path.endswith(".jpg") or path.endswith(".ico") or path.endswith(".svg") or path.endswith(".woff2") or path.endswith(".woff"):
            self._serve_file("static" + path)
            return

        # SPA fallback: serve index.html for any non-api route
        self._serve_file("static/index.html")

    def do_POST(self):  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        raw = self._read_body()
        data = self._parse_json(raw)

        if path == "/api/auth/signup":
            self._api_signup(data)
            return
        if path == "/api/auth/login":
            self._api_login(data)
            return
        if path == "/api/auth/logout":
            self._api_logout()
            return
        if path == "/api/checkins":
            self._api_create_checkin(data)
            return
        if path == "/api/onboarding":
            self._api_onboarding(data)
            return
        self._send_json(404, {"error": "not found"})

    def do_PUT(self):  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        raw = self._read_body()
        data = self._parse_json(raw)
        m = self._match(path, r"/api/checkins/(\d+)$")
        if m:
            self._api_update_checkin(int(m.group(1)), data)
            return
        if path == "/api/onboarding":
            self._api_onboarding(data)
            return
        if path == "/api/settings":
            self._api_settings_put(data)
            return
        self._send_json(404, {"error": "not found"})

    def do_DELETE(self):  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        if path == "/api/auth/logout":
            self._api_logout()
            return
        m = self._match(path, r"/api/checkins/(\d+)$")
        if m:
            self._api_delete_checkin(int(m.group(1)))
            return
        if path == "/api/account":
            self._api_delete_account()
            return
        self._send_json(404, {"error": "not found"})

    # ---- route matching ---------------------------------------------------

    @staticmethod
    def _match(path, pattern):
        import re

        return re.match(pattern, path)

    # ---- static file serving ----------------------------------------------

    def _serve_file(self, rel_path):
        full = os.path.join(os.path.dirname(os.path.abspath(__file__)), rel_path)
        if not os.path.isfile(full):
            self._send_json(404, {"error": "not found"})
            return
        try:
            with open(full, "rb") as f:
                body = f.read()
        except OSError:
            self._send_json(500, {"error": "read error"})
            return
        ctype = "text/html; charset=utf-8"
        if rel_path.endswith(".css"):
            ctype = "text/css; charset=utf-8"
        elif rel_path.endswith(".js"):
            ctype = "application/javascript; charset=utf-8"
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "public, max-age=31536000, immutable")
        self.end_headers()
        self.wfile.write(body)

    # ---- auth API ---------------------------------------------------------

    def _api_signup(self, data):
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        email = (data.get("email") or "").strip().lower()
        password = data.get("password") or ""
        name = (data.get("name") or "").strip() or email.split("@")[0]
        if not email or "@" not in email:
            self._send_json(400, {"error": "valid email required"})
            return
        if len(password) < 6:
            self._send_json(400, {"error": "password must be at least 6 characters"})
            return
        with _db_lock:
            conn = get_conn()
            try:
                existing = conn.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()
                if existing:
                    self._send_json(409, {"error": "email already registered"})
                    return
                now = now_iso()
                cur = conn.execute(
                    "INSERT INTO users(email, password_hash, name, created_at, updated_at) "
                    "VALUES (?, ?, ?, ?, ?)",
                    (email, hash_password(password), name, now, now),
                )
                user_id = cur.lastrowid
                conn.execute(
                    "INSERT INTO settings(user_id) VALUES (?)",
                    (user_id,),
                )
                token = create_session(user_id)
            finally:
                conn.close()
        self._send_json(
            201,
            {"user": {"id": user_id, "email": email, "name": name}, "token": token},
            extra_headers={"Set-Cookie": f"aura_session={token}; Path=/; HttpOnly; SameSite=Lax; Max-Age={SESSION_TTL_SECONDS}"},
        )

    def _api_login(self, data):
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        email = (data.get("email") or "").strip().lower()
        password = data.get("password") or ""
        with _db_lock:
            conn = get_conn()
            try:
                row = conn.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
            finally:
                conn.close()
        if not row or not verify_password(password, row["password_hash"]):
            self._send_json(401, {"error": "invalid email or password"})
            return
        token = create_session(row["id"])
        self._send_json(
            200,
            {"user": {"id": row["id"], "email": row["email"], "name": row["name"]}, "token": token},
            extra_headers={"Set-Cookie": f"aura_session={token}; Path=/; HttpOnly; SameSite=Lax; Max-Age={SESSION_TTL_SECONDS}"},
        )

    def _api_logout(self):
        destroy_session(self.headers)
        self._send_json(200, {"ok": True}, extra_headers={"Set-Cookie": "aura_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"})

    # ---- checkins API -----------------------------------------------------

    def _require_user(self):
        user = get_current_user(self.headers)
        if not user:
            self._send_json(401, {"error": "authentication required"})
            return None
        return user

    def _api_create_checkin(self, data):
        user = self._require_user()
        if not user:
            return
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        try:
            energy = int(data.get("energy", 5))
            focus = int(data.get("focus", 5))
        except (TypeError, ValueError):
            self._send_json(400, {"error": "energy and focus must be numbers"})
            return
        if not (1 <= energy <= 10 and 1 <= focus <= 10):
            self._send_json(400, {"error": "energy and focus must be between 1 and 10"})
            return
        mood = str(data.get("mood") or "neutral")
        tags = data.get("tags") or []
        if not isinstance(tags, list):
            tags = [tags]
        note = str(data.get("note") or "")
        ts = data.get("ts") or now_iso()
        day = parse_ts(ts)

        with _db_lock:
            conn = get_conn()
            try:
                cur = conn.execute(
                    "INSERT INTO checkins(user_id, ts, energy, focus, mood, tags, note, created_at) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (user["id"], ts, energy, focus, mood, json.dumps(tags), note, now_iso()),
                )
                cid = cur.lastrowid
                row = conn.execute(
                    "SELECT * FROM checkins WHERE id = ?", (cid,)
                ).fetchone()
            finally:
                conn.close()
        self._send_json(201, {"checkin": dict(row)})

    def _api_list_checkins(self, query):
        user = self._require_user()
        if not user:
            return
        limit = min(int(query.get("limit", ["50"])[0]) or 50, 200)
        since = query.get("since", [None])[0]
        until = query.get("until", [None])[0]
        with _db_lock:
            conn = get_conn()
            try:
                sql = "SELECT * FROM checkins WHERE user_id = ?"
                params = [user["id"]]
                if since:
                    sql += " AND ts >= ?"
                    params.append(since)
                if until:
                    sql += " AND ts <= ?"
                    params.append(until)
                sql += " ORDER BY ts DESC LIMIT ?"
                params.append(limit)
                rows = conn.execute(sql, params).fetchall()
            finally:
                conn.close()
        items = []
        for r in rows:
            d = dict(r)
            try:
                d["tags"] = json.loads(d["tags"] or "[]")
            except Exception:
                d["tags"] = []
            items.append(d)
        self._send_json(200, {"checkins": items})

    def _api_checkin_detail(self, cid):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                row = conn.execute(
                    "SELECT * FROM checkins WHERE id = ? AND user_id = ?",
                    (cid, user["id"]),
                ).fetchone()
            finally:
                conn.close()
        if not row:
            self._send_json(404, {"error": "checkin not found"})
            return
        d = dict(row)
        try:
            d["tags"] = json.loads(d["tags"] or "[]")
        except Exception:
            d["tags"] = []
        self._send_json(200, {"checkin": d})

    def _api_update_checkin(self, cid, data):
        user = self._require_user()
        if not user:
            return
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        fields = []
        params = []
        for key in ("energy", "focus", "mood", "tags", "note", "ts"):
            if key in data:
                if key == "tags":
                    fields.append("tags = ?")
                    params.append(json.dumps(data[key]))
                else:
                    fields.append(f"{key} = ?")
                    params.append(data[key])
        if not fields:
            self._send_json(400, {"error": "nothing to update"})
            return
        params.append(cid)
        params.append(user["id"])
        with _db_lock:
            conn = get_conn()
            try:
                conn.execute(
                    f"UPDATE checkins SET {', '.join(fields)}, created_at = ? WHERE id = ? AND user_id = ?",
                    params + [now_iso()],
                )
                row = conn.execute(
                    "SELECT * FROM checkins WHERE id = ? AND user_id = ?",
                    (cid, user["id"]),
                ).fetchone()
            finally:
                conn.close()
        if not row:
            self._send_json(404, {"error": "checkin not found"})
            return
        d = dict(row)
        try:
            d["tags"] = json.loads(d["tags"] or "[]")
        except Exception:
            d["tags"] = []
        self._send_json(200, {"checkin": d})

    def _api_delete_checkin(self, cid):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                cur = conn.execute(
                    "DELETE FROM checkins WHERE id = ? AND user_id = ?",
                    (cid, user["id"]),
                )
            finally:
                conn.close()
        self._send_json(200, {"deleted": bool(cur.rowcount)})

    # ---- onboarding / profile ---------------------------------------------

    def _api_onboarding(self, data):
        user = self._require_user()
        if not user:
            return
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        updates = {}
        for key in ("name", "wake_time", "sleep_time", "work_start", "work_end", "tz"):
            if key in data:
                updates[key] = data[key]
        if not updates:
            self._send_json(400, {"error": "nothing to update"})
            return
        updates["updated_at"] = now_iso()
        with _db_lock:
            conn = get_conn()
            try:
                cols = ", ".join(f"{k} = ?" for k in updates)
                params = list(updates.values()) + [user["id"]]
                conn.execute(f"UPDATE users SET {cols} WHERE id = ?", params)
                row = conn.execute("SELECT * FROM users WHERE id = ?", (user["id"],)).fetchone()
            finally:
                conn.close()
        self._send_json(200, {"user": sanitize_user(dict(row))})

    # ---- settings -----------------------------------------------------------

    def _api_settings_get(self):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                s = conn.execute(
                    "SELECT * FROM settings WHERE user_id = ?",
                    (user["id"],),
                ).fetchone()
            finally:
                conn.close()
        self._send_json(200, {
            "theme": s["theme"] if s else "dark",
            "reminders": bool(s["reminders"]) if s else False,
        })

    def _api_settings_put(self, data):
        user = self._require_user()
        if not user:
            return
        if not data:
            self._send_json(400, {"error": "invalid body"})
            return
        updates = {}
        for key in ("theme", "reminders"):
            if key in data:
                updates[key] = data[key]
        if not updates:
            self._send_json(400, {"error": "nothing to update"})
            return
        updates["theme"] = updates.get("theme", "dark")
        updates["reminders"] = 1 if updates.get("reminders") else 0
        with _db_lock:
            conn = get_conn()
            try:
                conn.execute(
                    "INSERT OR REPLACE INTO settings(user_id, theme, reminders) VALUES (?, ?, ?)",
                    (user["id"], updates["theme"], updates["reminders"]),
                )
            finally:
                conn.close()
        self._send_json(200, {
            "theme": updates["theme"],
            "reminders": bool(updates["reminders"]),
        })

    # ---- insights ---------------------------------------------------------

    def _api_insights(self):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                rows = conn.execute(
                    "SELECT ts, energy, focus, mood FROM checkins WHERE user_id = ? ORDER BY ts ASC",
                    (user["id"],),
                ).fetchall()
                tz_name = conn.execute(
                    "SELECT tz FROM users WHERE id = ?",
                    (user["id"],),
                ).fetchone()
            finally:
                conn.close()
        checkins = [dict(r) for r in rows]
        tz = tz_name["tz"] if tz_name and tz_name["tz"] else None
        insights = self._compute_insights(checkins, tz)
        self._send_json(200, insights)

    @staticmethod
    def _compute_insights(checkins, tz=None):
        if not checkins:
            return {
                "count": 0,
                "avg_energy": None,
                "avg_focus": None,
                "peak_hour": None,
                "best_day": None,
                "streak": 0,
                "hourly": [],
                "weekday": [],
                "best_window": None,
                "week_comparison": None,
                "moods": {},
                "insights": ["Log your first check-in to see your rhythm emerge."],
            }
        n = len(checkins)
        avg_energy = round(sum(c["energy"] for c in checkins) / n, 1)
        avg_focus = round(sum(c["focus"] for c in checkins) / n, 1)

        # energy/focus by hour of day (local wall-clock from ISO offset)
        hour_energy = {}
        hour_focus = {}
        day_energy = {}
        weekday_order = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
        weekday_map = {}
        moods = {}
        samples = []
        for c in checkins:
            try:
                dt = datetime.fromisoformat(c["ts"])
            except Exception:
                continue
            h = dt.hour
            dname = dt.strftime("%A")
            weekday_map[dname] = (dt.weekday() + 1) % 7  # Mon=0..Sun=6
            hour_energy.setdefault(h, []).append(c["energy"])
            hour_focus.setdefault(h, []).append(c["focus"])
            day_energy.setdefault(dname, []).append(c["energy"])
            moods[c["mood"]] = moods.get(c["mood"], 0) + 1
            samples.append((dt, c["energy"], c["focus"]))

        hourly = []
        for h in range(24):
            e = hour_energy.get(h, [])
            f = hour_focus.get(h, [])
            hourly.append({
                "hour": h,
                "energy": round(sum(e) / len(e), 1) if e else None,
                "focus": round(sum(f) / len(f), 1) if f else None,
                "n": len(e),
            })

        peak_hour = None
        best_hour_avg = -1
        for h, vals in hour_energy.items():
            a = sum(vals) / len(vals)
            if a > best_hour_avg:
                best_hour_avg = a
                peak_hour = h

        best_day = None
        best_day_avg = -1
        for d, vals in day_energy.items():
            a = sum(vals) / len(vals)
            if a > best_day_avg:
                best_day_avg = a
                best_day = d

        # streak: consecutive days with a checkin (user-local dates, up to today)
        days = sorted({parse_ts(c["ts"]) for c in checkins})
        streak = 0
        try:
            tzobj = ZoneInfo(tz) if tz else None
        except Exception:
            tzobj = None
        today = datetime.now(tzobj).date() if tzobj else date.today()
        check_day = today
        day_set = set(days)
        while check_day.isoformat() in day_set:
            streak += 1
            check_day -= timedelta(days=1)

        # best 2-hour focus window among hours that have data
        best_window = None
        best_window_avg = -1
        for h in range(23):
            ea = hour_energy.get(h, [])
            eb = hour_energy.get(h + 1, [])
            if not ea or not eb:
                continue
            a = (sum(ea) + sum(eb)) / (len(ea) + len(eb))
            if a > best_window_avg:
                best_window_avg = a
                best_window = {
                    "start_hour": h,
                    "end_hour": h + 2,
                    "avg": round(a, 1),
                }

        # weekly comparison: last 7 days vs previous 7 days (user-local dates)
        week_comparison = None
        now_anchor = samples[-1][0]  # latest checkin, tz-aware if offset stored
        try:
            tzobj = ZoneInfo(tz) if tz else None
        except Exception:
            tzobj = None
        if tzobj:
            now_naive = now_anchor.astimezone(tzobj).replace(tzinfo=None)
        else:
            now_naive = now_anchor.replace(tzinfo=None) if now_anchor.tzinfo else now_anchor
        samples_local = [(s[0].replace(tzinfo=None) if s[0].tzinfo else s[0], s[1], s[2]) for s in samples]
        last7_start = now_naive - timedelta(days=7)
        prev7_start = last7_start - timedelta(days=7)
        last7 = [s[1] for s in samples_local if last7_start <= s[0] <= now_naive]
        prev7 = [s[1] for s in samples_local if prev7_start <= s[0] < last7_start]
        if last7:
            a = sum(last7) / len(last7)
            b = sum(prev7) / len(prev7) if prev7 else None
            week_comparison = {
                "avg": round(a, 1),
                "prev_avg": round(b, 1) if b is not None else None,
                "change": round(a - b, 1) if b is not None else None,
            }

        # weekday averages ordered Mon..Sun
        weekday = []
        for dname in weekday_order:
            vals = day_energy.get(dname, [])
            weekday.append({
                "day": dname,
                "energy": round(sum(vals) / len(vals), 1) if vals else None,
                "n": len(vals),
            })

        msgs = []
        if peak_hour is not None:
            msgs.append(f"Your energy peaks around {peak_hour}:00 — guard that window for deep work.")
        if best_day:
            msgs.append(f"Your strongest day tends to be {best_day}.")
        if best_window:
            msgs.append(f"Your best focus window is {best_window['start_hour']}:00–{best_window['end_hour']}:00.")
        if avg_energy >= 7:
            msgs.append("You're running high energy overall — sustainable, but watch for burnout.")
        elif avg_energy <= 4:
            msgs.append("Your energy has been low lately — prioritize rest and recovery.")
        if week_comparison and week_comparison["change"] is not None:
            if week_comparison["change"] >= 1:
                msgs.append("Your energy is trending up versus last week — keep it going.")
            elif week_comparison["change"] <= -1:
                msgs.append("Your energy is trending down versus last week — consider more rest.")
        if streak >= 3:
            msgs.append(f"{streak}-day logging streak — keep it going to see your full rhythm.")
        if not msgs:
            msgs.append("Keep logging daily to uncover your personal energy pattern.")

        return {
            "count": n,
            "avg_energy": avg_energy,
            "avg_focus": avg_focus,
            "peak_hour": peak_hour,
            "best_hour_avg": round(best_hour_avg, 1) if peak_hour is not None else None,
            "best_day": best_day,
            "streak": streak,
            "hourly": hourly,
            "weekday": weekday,
            "best_window": best_window,
            "week_comparison": week_comparison,
            "moods": moods,
            "insights": msgs,
        }

    # ---- export / delete account ------------------------------------------

    def _api_export(self):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                user_row = conn.execute("SELECT * FROM users WHERE id = ?", (user["id"],)).fetchone()
                checkins = conn.execute(
                    "SELECT * FROM checkins WHERE user_id = ? ORDER BY ts ASC",
                    (user["id"],),
                ).fetchall()
                settings = conn.execute("SELECT * FROM settings WHERE user_id = ?", (user["id"],)).fetchone()
            finally:
                conn.close()
        payload = {
            "user": sanitize_user(dict(user_row)) if user_row else None,
            "settings": {k: settings[k] for k in settings.keys()} if settings else {},
            "checkins": [dict(c) for c in checkins],
            "exported_at": now_iso(),
        }
        self._send_json(200, payload)

    def _api_delete_account(self):
        user = self._require_user()
        if not user:
            return
        with _db_lock:
            conn = get_conn()
            try:
                conn.execute("DELETE FROM sessions WHERE user_id = ?", (user["id"],))
                conn.execute("DELETE FROM checkins WHERE user_id = ?", (user["id"],))
                conn.execute("DELETE FROM settings WHERE user_id = ?", (user["id"],))
                conn.execute("DELETE FROM users WHERE id = ?", (user["id"],))
            finally:
                conn.close()
        self._send_json(200, {"ok": True}, extra_headers={"Set-Cookie": "aura_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"})


import sys  # noqa: E402


def run():
    init_db()
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Aura server running on http://0.0.0.0:{PORT}  (DB: {DB_PATH})", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down...", flush=True)
    finally:
        server.server_close()


if __name__ == "__main__":
    run()
