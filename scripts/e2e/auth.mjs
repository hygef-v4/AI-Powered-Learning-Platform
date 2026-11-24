// System test U01: login, session cookies, CSRF, role check, refresh rotation, logout, refresh-token reuse and password change.
// Calls the running app like a browser. Never prints passwords, cookies or tokens.
//   E2E_BASE_URL        app origin, e.g. http://localhost:3000 (default http://localhost:8088)
//   E2E_TEACHER_EMAIL   an ACTIVE teacher account
//   E2E_STUDENT_EMAIL   an ACTIVE student account; ALL its sessions are revoked by the reuse check, and its password is changed then restored
//   E2E_AUTH_PASSWORD   password of both accounts (falls back to TEST_ACCOUNT_PASSWORD in infra/.env.local)
import { randomBytes } from "node:crypto";
import { Session, BASE, env } from "./lib.mjs";

const TEACHER = process.env.E2E_TEACHER_EMAIL;
const STUDENT = process.env.E2E_STUDENT_EMAIL;
const PASSWORD = process.env.E2E_AUTH_PASSWORD ?? env("TEST_ACCOUNT_PASSWORD");
if (!TEACHER || !STUDENT || !PASSWORD) {
  console.error("Set E2E_TEACHER_EMAIL, E2E_STUDENT_EMAIL and E2E_AUTH_PASSWORD (see scripts/e2e/README.md).");
  process.exit(2);
}

const results = [];
const check = (id, name, ok, detail = "") => {
  results.push({ id, ok });
  console.log(`${ok ? "PASS" : "FAIL"} ${id} ${name}${detail ? ` (${detail})` : ""}`);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const cookieFlags = (res) => (res.headers.getSetCookie?.() ?? []).map((c) => {
  const [pair, ...attrs] = c.split(";");
  return { name: pair.split("=")[0].trim(), cleared: /max-age=0/i.test(c), attrs: attrs.map((a) => a.trim().toLowerCase()) };
});

// A1 anonymous access is refused
const a1 = await new Session("anon").get("/api/v1/me/profile");
check("A1", "chưa đăng nhập thì /me/profile trả 401", a1.status === 401, `status ${a1.status}`);

// A2 wrong password: refused with a generic code (one attempt, far below the per-IP limit)
const bad = await new Session("bad").post("/api/v1/auth/login", { email: TEACHER, password: PASSWORD + "x" });
check("A2", "sai mật khẩu bị từ chối với mã chung", bad.status === 401 && bad.data?.code === "AUTH_FAILED",
  `status ${bad.status}, code ${bad.data?.code ?? "-"}`);
check("A2b", "phản hồi sai mật khẩu không chứa mật khẩu", !JSON.stringify(bad.data ?? "").includes(PASSWORD));

// A3 login sets HttpOnly SameSite session cookies and returns no token in the body
const teacher = new Session("teacher");
const loginRes = await teacher.call("POST", "/api/v1/auth/login", { email: TEACHER, password: PASSWORD }, { raw: true });
const sessionCookies = cookieFlags(loginRes).filter((c) => !/xsrf/i.test(c.name) && !c.cleared);
check("A3", "đăng nhập đúng trả 200", loginRes.status === 200, `status ${loginRes.status}`);
check("A3b", "cookie phiên có HttpOnly và SameSite",
  sessionCookies.length >= 2 && sessionCookies.every((c) => c.attrs.includes("httponly")
    && c.attrs.some((a) => a.startsWith("samesite"))),
  sessionCookies.map((c) => `${c.name}:${c.attrs.filter((a) => a.startsWith("samesite")).join("")}`).join(", "));
const loginBody = await loginRes.text();
check("A3c", "thân phản hồi đăng nhập không chứa token hay mật khẩu",
  !/eyJ[A-Za-z0-9_-]{10,}/.test(loginBody) && !loginBody.includes(PASSWORD));

// A4 the session works, shows the right role and no password hash
const me = await teacher.get("/api/v1/me/profile");
check("A4", "sau đăng nhập đọc được hồ sơ, đúng vai trò", me.status === 200 && JSON.stringify(me.data).includes("TEACHER"),
  `status ${me.status}`);
check("A4b", "hồ sơ không lộ mã băm mật khẩu", !/password_?hash|\$2[aby]\$/i.test(JSON.stringify(me.data)));

// A5 a protected state-changing call without the CSRF header is refused. Login, refresh, logout and OTP routes are
// exempt by design and rely on SameSite cookies; the empty body here cannot change anything.
const noCsrf = await fetch(BASE + "/api/v1/me/password", {
  method: "POST", body: "{}",
  headers: { Cookie: teacher.cookieHeader("/api/v1/me/password"), "Content-Type": "application/json" },
});
check("A5", "POST thiếu header CSRF bị từ chối", noCsrf.status === 403 && (await noCsrf.text()).includes("CSRF_INVALID"),
  `status ${noCsrf.status}`);

// A6 role check: a teacher cannot call an admin-only endpoint
const admin = await teacher.get("/api/v1/admin/accounts");
check("A6", "giảng viên gọi API admin bị từ chối", admin.status === 403, `status ${admin.status}`);

// A7 refresh rotates the session cookies and the session keeps working
const before = new Map(teacher.cookies);
const refreshed = await teacher.post("/api/v1/auth/refresh");
const rotated = [...teacher.cookies].some(([k, c]) => !/xsrf/i.test(k) && before.get(k)?.value !== c.value);
check("A7", "refresh xoay cookie phiên", refreshed.status === 204 && rotated, `status ${refreshed.status}`);
check("A7b", "phiên vẫn dùng được sau refresh", (await teacher.get("/api/v1/me/profile")).status === 200);

// A8 logout ends the session and the old cookies stop working
const old = new Session("old");
old.copyCookies(teacher);
const out = await teacher.post("/api/v1/auth/logout");
check("A8", "đăng xuất trả 204", out.status === 204, `status ${out.status}`);
check("A8b", "cookie cũ sau đăng xuất không dùng được", (await old.get("/api/v1/me/profile")).status === 401);

// A9 refresh-token reuse (BR-U01-46): within 20 s it is a concurrent refresh (409, nothing issued); later it is
// treated as a stolen token and the real session is revoked too.
const student = new Session("student");
await student.login(STUDENT, PASSWORD);
const thief = new Session("thief");
thief.copyCookies(student);
await student.post("/api/v1/auth/refresh");
const concurrent = await thief.call("POST", "/api/v1/auth/refresh", {}, { raw: true });
const issued = (concurrent.headers.getSetCookie?.() ?? []).filter((c) => /^(access|refresh)_token=[^;]+/.test(c));
check("A9", "dùng lại token cũ trong 20 giây: 409, không cấp cookie mới", concurrent.status === 409 && issued.length === 0,
  `status ${concurrent.status}, cookie mới ${issued.length}`);
check("A9b", "phiên thật vẫn dùng được sau va chạm", (await student.get("/api/v1/me/profile")).status === 200);
await sleep(21_000);
check("A9c", "dùng lại token cũ sau 20 giây bị từ chối", (await thief.post("/api/v1/auth/refresh")).status === 401);
check("A9d", "phát hiện dùng lại thì thu hồi luôn phiên thật", (await student.post("/api/v1/auth/refresh")).status === 401);

// A10 change password while logged in (BR-U01-33/34): needs the current password, keeps this session, revokes the
// others. The student's password is changed and then restored, so later runs still work.
const NEW_PASSWORD = randomBytes(24).toString("base64url") + "Aa7!";
const main = new Session("main");
await main.login(STUDENT, PASSWORD);
const other = new Session("other");
await other.login(STUDENT, PASSWORD);
const wrongCurrent = await main.post("/api/v1/me/password", { currentPassword: PASSWORD + "x", newPassword: NEW_PASSWORD });
check("A10", "sai mật khẩu hiện tại bị từ chối", wrongCurrent.status === 400 && wrongCurrent.data?.code === "CURRENT_PASSWORD_INVALID",
  `status ${wrongCurrent.status}, code ${wrongCurrent.data?.code ?? "-"}`);
const weakNew = await main.post("/api/v1/me/password", { currentPassword: PASSWORD, newPassword: "123" });
check("A10b", "mật khẩu mới yếu bị từ chối", weakNew.status === 400, `status ${weakNew.status}`);
const changed = await main.post("/api/v1/me/password", { currentPassword: PASSWORD, newPassword: NEW_PASSWORD });
check("A10c", "đổi mật khẩu đúng trả 204", changed.status === 204, `status ${changed.status}`);
check("A10d", "phiên hiện tại vẫn dùng được", (await main.get("/api/v1/me/profile")).status === 200);
check("A10e", "phiên khác bị thu hồi", (await other.get("/api/v1/me/profile")).status === 401);
const oldPw = await new Session("old-pw").post("/api/v1/auth/login", { email: STUDENT, password: PASSWORD });
const newPw = new Session("new-pw");
const newPwLogin = await newPw.post("/api/v1/auth/login", { email: STUDENT, password: NEW_PASSWORD });
check("A10f", "mật khẩu cũ hết dùng được, mật khẩu mới đăng nhập được", oldPw.status === 401 && newPwLogin.status === 200,
  `cũ ${oldPw.status}, mới ${newPwLogin.status}`);
const restored = await newPw.post("/api/v1/me/password", { currentPassword: NEW_PASSWORD, newPassword: PASSWORD });
const back = await new Session("restored").post("/api/v1/auth/login", { email: STUDENT, password: PASSWORD });
check("A10g", "đổi lại mật khẩu ban đầu", restored.status === 204 && back.status === 200,
  `đổi ${restored.status}, đăng nhập ${back.status}`);

const failed = results.filter((r) => !r.ok);
console.log(`\n${results.length - failed.length}/${results.length} PASS`);
process.exit(failed.length ? 1 : 0);
