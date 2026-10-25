// Tạo dữ liệu thử: admin (bootstrap), chủ nhiệm môn, giảng viên, 5 sinh viên, 1 môn, 1 lớp OPEN có 5 sinh viên.
import fs from "node:fs";
import { Session, activate, env } from "./lib.mjs";

const PW = env("TEST_ACCOUNT_PASSWORD");
const ADMIN = env("U01_BOOTSTRAP_ADMIN_EMAIL");
const people = [
  { key: "manager", email: "cn.test@fpt.edu.vn", displayName: "Chủ nhiệm Thử", role: "SUBJECT_MANAGER" },
  { key: "teacher", email: "gv.test@fpt.edu.vn", displayName: "Giảng viên Thử", role: "TEACHER" },
  ...[1, 2, 3, 4, 5].map((i) => ({ key: `sv${i}`, email: `sv${i}.test@fpt.edu.vn`, displayName: `Sinh viên ${i}`, role: "STUDENT" })),
];

const out = { accounts: {} };
const admin = new Session("admin");
const loginAdmin = await admin.post("/api/v1/auth/login", { email: ADMIN, password: PW });
if (loginAdmin.status >= 300) {
  await activate(ADMIN, PW);
  await admin.login(ADMIN, PW);
}
console.log("admin ok");

for (const p of people) {
  const list = await admin.get(`/api/v1/admin/accounts?q=${encodeURIComponent(p.email)}`);
  let acc = (list.data?.items || list.data?.content || []).find((a) => a.email === p.email);
  if (!acc) {
    const r = await admin.post("/api/v1/admin/accounts", { email: p.email, displayName: p.displayName, role: p.role });
    if (r.status >= 300) throw new Error(`create ${p.email}: ${r.status} ${JSON.stringify(r.data)}`);
    acc = r.data;
  }
  if (acc.status !== "ACTIVE") await activate(p.email, PW);
  out.accounts[p.key] = { id: acc.id ?? acc.accountId, email: p.email };
  console.log("account", p.key, out.accounts[p.key].id);
}

let subjects = await admin.get("/api/v1/admin/subjects?q=SWT301");
let subject = (subjects.data?.items || subjects.data?.content || []).find((s) => s.code === "SWT301");
if (!subject) {
  const r = await admin.post("/api/v1/admin/subjects", { code: "SWT301", name: "Kiểm thử phần mềm", description: "Môn chạy thử local", managerId: out.accounts.manager.id });
  if (r.status >= 300) throw new Error(`subject: ${r.status} ${JSON.stringify(r.data)}`);
  subject = r.data;
}
out.subjectId = subject.id;
console.log("subject", subject.id);

const manager = new Session("manager");
await manager.login(out.accounts.manager.email, PW);
const mine = await manager.get("/api/v1/manager/classes");
let cls = (mine.data?.items || mine.data?.content || mine.data || []).find?.((c) => c.code === "SE-TEST1");
if (!cls) {
  const r = await manager.post("/api/v1/manager/classes", { subjectId: subject.id, code: "SE-TEST1", name: "Lớp chạy thử U06 U12", description: "Lớp thử", term: "2026-1", teacherId: out.accounts.teacher.id });
  if (r.status >= 300) throw new Error(`class: ${r.status} ${JSON.stringify(r.data)}`);
  cls = r.data;
}
if (cls.status !== "OPEN") {
  const r = await manager.put(`/api/v1/manager/classes/${cls.id}/status`, { status: "OPEN", version: cls.version });
  console.log("class status", r.status, r.data?.status ?? JSON.stringify(r.data));
}
for (const i of [1, 2, 3, 4, 5]) {
  const r = await manager.post(`/api/v1/manager/classes/${cls.id}/enrollments`, { accountId: out.accounts[`sv${i}`].id });
  console.log("enroll", i, r.status, r.data?.result ?? JSON.stringify(r.data));
}
out.classId = cls.id;
fs.writeFileSync(new URL("./seed.json", import.meta.url), JSON.stringify(out, null, 2));
console.log("seed done", out.classId);
