// System test U01: OTP activation and password reset through Mailpit, on one fresh student account per run.
// Never prints passwords, OTPs or tokens. Leaves the test account in the database.
//   E2E_BASE_URL        app origin, e.g. http://localhost:3000 (default http://localhost:8088)
//   E2E_MAILPIT_URL     Mailpit origin (default http://localhost:8025)
//   E2E_ADMIN_EMAIL     an ACTIVE admin (falls back to U01_BOOTSTRAP_ADMIN_EMAIL in infra/.env.local)
//   E2E_AUTH_PASSWORD   the admin's password (falls back to TEST_ACCOUNT_PASSWORD in infra/.env.local)
//   E2E_ACCOUNT_DOMAIN  allowed school domain for the new account (falls back to U01_ALLOWED_EMAIL_DOMAINS)
// The backend must send mail to Mailpit with a sender address (SMTP_FROM), and its worker must be running.
import { randomBytes } from "node:crypto";
import { Session, MAILPIT, env } from "./lib.mjs";

const ADMIN = process.env.E2E_ADMIN_EMAIL ?? env("U01_BOOTSTRAP_ADMIN_EMAIL");
const ADMIN_PASSWORD = process.env.E2E_AUTH_PASSWORD ?? env("TEST_ACCOUNT_PASSWORD");
const DOMAIN = (process.env.E2E_ACCOUNT_DOMAIN ?? env("U01_ALLOWED_EMAIL_DOMAINS") ?? "").split(",")[0].trim();
if (!ADMIN || !ADMIN_PASSWORD || !DOMAIN) {
  console.error("Set E2E_ADMIN_EMAIL, E2E_AUTH_PASSWORD and E2E_ACCOUNT_DOMAIN (see scripts/e2e/README.md).");
  process.exit(2);
}

const stamp = Date.now().toString().slice(-6);
const EMAIL = `otp.${stamp}@${DOMAIN}`;
const UNKNOWN = `nobody.${stamp}@${DOMAIN}`;
const PASSWORD_1 = randomBytes(24).toString("base64url") + "Aa7!";
const PASSWORD_2 = randomBytes(24).toString("base64url") + "Aa7!";
const results = [];
const check = (id, name, ok, detail = "") => {
  results.push({ id, ok });
  console.log(`${ok ? "PASS" : "FAIL"} ${id} ${name}${detail ? ` (${detail})` : ""}`);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function listMails(email) {
  const res = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent("to:" + email)}&limit=20`);
  return (await res.json()).messages ?? [];
}

/** Waits until at least `count` mails reached `email`, then returns the newest one's OTP and subject. */
async function waitForOtp(email, count) {
  for (let i = 0; i < 60; i++) {
    const messages = await listMails(email);
    if (messages.length >= count) {
      const newest = messages.sort((a, b) => new Date(b.Created) - new Date(a.Created))[0];
      const full = await (await fetch(`${MAILPIT}/api/v1/message/${newest.ID}`)).json();
      const match = (full.Text || full.HTML || "").match(/\b(\d{6})\b/);
      if (match) return { otp: match[1], subject: newest.Subject };
    }
    await sleep(500);
  }
  return null;
}

function stopWhen(missing, reason) {
  if (!missing) return;
  console.error(`Stop: ${reason}`);
  process.exit(1);
}

// Setup: an admin creates a fresh student account
const admin = new Session("admin");
await admin.login(ADMIN, ADMIN_PASSWORD);
const created = await admin.post("/api/v1/admin/accounts",
  { email: EMAIL, displayName: `OTP test ${stamp}`, role: "STUDENT", studentCode: `OT${stamp}` });
check("O0", "Admin tạo tài khoản sinh viên mới, chưa ACTIVE", created.status === 201 && created.data?.status !== "ACTIVE",
  `status ${created.status}, trạng thái ${created.data?.status}`);

// O1 a not-yet-activated account cannot log in
const early = await new Session("early").post("/api/v1/auth/login", { email: EMAIL, password: PASSWORD_1 });
check("O1", "chưa kích hoạt thì không đăng nhập được", early.status === 401, `status ${early.status}`);

// O2 activation request sends an OTP mail
const guest = new Session("guest");
const request = await guest.post("/api/v1/auth/activation/request", { email: EMAIL });
const mail1 = await waitForOtp(EMAIL, 1);
check("O2", "yêu cầu kích hoạt gửi mail có mã 6 số", request.status === 202 && mail1 !== null,
  `status ${request.status}, tiêu đề "${mail1?.subject ?? "không có mail"}"`);
stopWhen(!mail1, "no activation mail; check SMTP_FROM, Mailpit and the worker (OTP_DELIVERY job)");

// O3 wrong OTP is refused; O4 the right one returns a ticket; O5 it cannot be used twice
const wrong = await guest.post("/api/v1/auth/activation/verify",
  { email: EMAIL, otp: mail1.otp === "000000" ? "111111" : "000000" });
check("O3", "nhập sai OTP bị từ chối", wrong.status === 400 && wrong.data?.code === "OTP_INVALID",
  `status ${wrong.status}, code ${wrong.data?.code ?? "-"}`);
const verified = await guest.post("/api/v1/auth/activation/verify", { email: EMAIL, otp: mail1.otp });
check("O4", "nhập đúng OTP nhận được ticket", verified.status === 200 && Boolean(verified.data?.otpTicket),
  `status ${verified.status}`);
const again = await new Session("again").post("/api/v1/auth/activation/verify", { email: EMAIL, otp: mail1.otp });
check("O5", "mã OTP đã dùng không dùng lại được", again.status === 400, `status ${again.status}`);

// O6 a weak password is refused, a valid one activates the account
const weak = await guest.post("/api/v1/auth/activation/complete", { otpTicket: verified.data?.otpTicket, newPassword: "123" });
check("O6", "mật khẩu yếu bị từ chối", weak.status === 400 && weak.data?.code === "PASSWORD_POLICY",
  `status ${weak.status}, code ${weak.data?.code ?? "-"}`);
const done = await guest.post("/api/v1/auth/activation/complete",
  { otpTicket: verified.data?.otpTicket, newPassword: PASSWORD_1 });
check("O6b", "đặt mật khẩu hợp lệ thì kích hoạt xong", done.status < 300, `status ${done.status}`);

// O7 the activated account can log in
const user = new Session("user");
const login1 = await user.post("/api/v1/auth/login", { email: EMAIL, password: PASSWORD_1 });
check("O7", "tài khoản vừa kích hoạt đăng nhập được", login1.status === 200, `status ${login1.status}`);

// O8 no account enumeration: an active account and an unknown address get the same answer, and no mail goes out
const forActive = await new Session("x").post("/api/v1/auth/activation/request", { email: EMAIL });
const forUnknown = await new Session("y").post("/api/v1/auth/activation/request", { email: UNKNOWN });
check("O8", "email đã ACTIVE và email không tồn tại nhận cùng phản hồi",
  forActive.status === forUnknown.status && JSON.stringify(forActive.data) === JSON.stringify(forUnknown.data),
  `${forActive.status} / ${forUnknown.status}`);
await sleep(3000);
check("O8b", "email không tồn tại không nhận được mail", (await listMails(UNKNOWN)).length === 0);

// P1 password reset sends a new OTP. OTP requests share a per-email cooldown (U01_RL_OTP_COOLDOWN, 60 s by default)
// and O8 just used it, so wait it out first.
await sleep(65_000);
const before = (await listMails(EMAIL)).length;
const reset = new Session("reset");
const resetRequest = await reset.post("/api/v1/auth/password-reset/request", { email: EMAIL });
const mail2 = await waitForOtp(EMAIL, before + 1);
check("P1", "yêu cầu quên mật khẩu gửi mail có mã mới", resetRequest.status === 202 && mail2 !== null,
  `status ${resetRequest.status}, tiêu đề "${mail2?.subject ?? "không có mail"}"`);
stopWhen(!mail2, "no password-reset mail");

// P2 an immediate second request is accepted but sends no extra mail
const second = await new Session("reset2").post("/api/v1/auth/password-reset/request", { email: EMAIL });
await sleep(3000);
const sent = (await listMails(EMAIL)).length - before;
check("P2", "gửi lại ngay thì bị giãn cách, không có mail thứ hai", second.status === 202 && sent === 1,
  `status ${second.status}, số mail ${sent}`);

// P3 verify the OTP and set a new password
const resetVerified = await reset.post("/api/v1/auth/password-reset/verify", { email: EMAIL, otp: mail2.otp });
const resetDone = await reset.post("/api/v1/auth/password-reset/complete",
  { otpTicket: resetVerified.data?.otpTicket, newPassword: PASSWORD_2 });
check("P3", "xác minh OTP và đặt mật khẩu mới thành công", resetVerified.status === 200 && resetDone.status < 300,
  `verify ${resetVerified.status}, complete ${resetDone.status}`);

// P4 the old password stops working and the new one works
const oldLogin = await new Session("old").post("/api/v1/auth/login", { email: EMAIL, password: PASSWORD_1 });
const newLogin = await new Session("new").post("/api/v1/auth/login", { email: EMAIL, password: PASSWORD_2 });
check("P4", "mật khẩu cũ hết dùng được, mật khẩu mới đăng nhập được", oldLogin.status === 401 && newLogin.status === 200,
  `cũ ${oldLogin.status}, mới ${newLogin.status}`);

// P5 sessions opened before the reset are revoked
const stale = await user.get("/api/v1/me/profile");
const staleRefresh = await user.post("/api/v1/auth/refresh");
check("P5", "phiên mở trước khi đặt lại mật khẩu bị thu hồi", stale.status === 401 && staleRefresh.status === 401,
  `profile ${stale.status}, refresh ${staleRefresh.status}`);

const failed = results.filter((r) => !r.ok);
console.log(`\n${results.length - failed.length}/${results.length} PASS · tài khoản test: ${EMAIL}`);
process.exit(failed.length ? 1 : 0);
