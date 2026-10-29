// Trợ giúp gọi API local (http://localhost:8088) với cookie + CSRF, và đọc OTP từ Mailpit.
import fs from "node:fs";

export const BASE = "http://localhost:8088";
export const MAILPIT = "http://localhost:8025";
const ENV = new URL("../../infra/.env.local", import.meta.url);

export function env(key) {
  const line = fs.readFileSync(ENV, "utf8").split(/\r?\n/).find((l) => l.startsWith(key + "="));
  return line ? line.slice(key.length + 1) : undefined;
}

export class Session {
  constructor(name) {
    this.name = name;
    this.jar = new Map();
  }
  cookieHeader() {
    return [...this.jar].map(([k, v]) => `${k}=${v}`).join("; ");
  }
  absorb(res) {
    for (const c of res.headers.getSetCookie?.() ?? []) {
      const [pair] = c.split(";");
      const i = pair.indexOf("=");
      const k = pair.slice(0, i).trim();
      const v = pair.slice(i + 1).trim();
      if (v === "" || /max-age=0/i.test(c)) this.jar.delete(k);
      else this.jar.set(k, v);
    }
  }
  async call(method, path, body, { raw = false, headers = {} } = {}) {
    if (method !== "GET" && !this.jar.has("XSRF-TOKEN")) await this.call("GET", "/api/v1/me/profile").catch(() => {});
    const h = { Accept: "application/json", Cookie: this.cookieHeader(), ...headers };
    if (body !== undefined) h["Content-Type"] = "application/json";
    if (this.jar.has("XSRF-TOKEN")) h["X-XSRF-TOKEN"] = decodeURIComponent(this.jar.get("XSRF-TOKEN"));
    const res = await fetch(BASE + path, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body), redirect: "manual" });
    this.absorb(res);
    if (raw) return res;
    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = text;
    }
    return { status: res.status, data };
  }
  get(p) { return this.call("GET", p); }
  post(p, b) { return this.call("POST", p, b ?? {}); }
  put(p, b) { return this.call("PUT", p, b ?? {}); }
  patch(p, b) { return this.call("PATCH", p, b ?? {}); }
  del(p) { return this.call("DELETE", p); }
  async login(email, password) {
    const r = await this.post("/api/v1/auth/login", { email, password });
    if (r.status >= 300) throw new Error(`login ${email}: ${r.status} ${JSON.stringify(r.data)}`);
    return r;
  }
}

export async function latestOtp(email, after) {
  for (let i = 0; i < 40; i++) {
    const list = await (await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent("to:" + email)}&limit=5`)).json();
    const msg = (list.messages || []).find((m) => new Date(m.Created).getTime() >= after - 2000);
    if (msg) {
      const full = await (await fetch(`${MAILPIT}/api/v1/message/${msg.ID}`)).json();
      const m = (full.Text || full.HTML || "").match(/\b(\d{6})\b/);
      if (m) return m[1];
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error("no OTP for " + email);
}

export async function activate(email, password) {
  const s = new Session(email);
  const t = Date.now();
  const r1 = await s.post("/api/v1/auth/activation/request", { email });
  if (r1.status >= 300) throw new Error(`activation/request ${email}: ${r1.status} ${JSON.stringify(r1.data)}`);
  const otp = await latestOtp(email, t);
  const r2 = await s.post("/api/v1/auth/activation/verify", { email, otp });
  if (r2.status >= 300) throw new Error(`verify ${email}: ${r2.status} ${JSON.stringify(r2.data)}`);
  const r3 = await s.post("/api/v1/auth/activation/complete", { otpTicket: r2.data.otpTicket, newPassword: password });
  if (r3.status >= 300) throw new Error(`complete ${email}: ${r3.status} ${JSON.stringify(r3.data)}`);
  return s;
}

export function check(results, id, ok, detail) {
  results.push({ id, ok: !!ok, detail });
  console.log(`${ok ? "PASS" : "FAIL"} ${id} ${detail ?? ""}`);
}
