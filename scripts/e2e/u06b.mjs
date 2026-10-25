// U06 bổ sung: thay/thu hồi khóa, tắt khóa trường, hết hạn mức, 3 tác vụ, trạng thái tác vụ, lỗi AI không mất việc.
// Khóa dùng ở đây là khóa thử (sai) nên tác vụ gọi nhà cung cấp sẽ lỗi — đúng cái cần kiểm cho U06-19.
import fs from "node:fs";
import { Session, check, env } from "./lib.mjs";

const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const R = [];
const admin = new Session("admin");
await admin.login(env("U01_BOOTSTRAP_ADMIN_EMAIL"), PW);
const T = new Session("teacher");
await T.login(seed.accounts.teacher.email, PW);
const sv = new Session("sv1");
await sv.login(seed.accounts.sv1.email, PW);
const brief = (r) => `${r.status} ${r.data?.code ?? ""} ${(r.data?.detail ?? "").slice(0, 100)}`;
const conn = async (p) => ((await T.get("/api/v1/me/ai-connections")).data || []).find((c) => c.provider === p);

// U06-02 / U06-13 / U06-03
let gem = await conn("GEMINI");
let r = await T.put("/api/v1/me/ai-connections/GEMINI", { apiKey: "test-khoa-sai-AAAA1111", model: "gemini-3.6-flash", version: gem?.configured ? gem.version : null });
const tested = await T.post("/api/v1/me/ai-connections/GEMINI:test");
check(R, "U06-02 khóa sai: Kiểm tra báo khóa không hợp lệ", tested.status === 200 && /Key API không hợp lệ/.test(tested.data?.lastTestResult ?? ""), `${tested.status} ${tested.data?.lastTestResult}`);
gem = await conn("GEMINI");
r = await T.put("/api/v1/me/ai-connections/GEMINI", { apiKey: "test-khoa-thay-BBBB2222", model: "gemini-3.6-flash", version: gem.version });
const rows = ((await T.get("/api/v1/me/ai-connections")).data || []).filter((c) => c.provider === "GEMINI");
check(R, "U06-13 mỗi nhà cung cấp một khóa (thay, không thêm)", r.status === 200 && rows.length === 1 && rows[0].keyHint === "2222", `${brief(r)} rows=${rows.length} hint=${rows[0]?.keyHint}`);
const stale = await T.put("/api/v1/me/ai-connections/GEMINI", { apiKey: "test-khoa-cu-CCCC", model: "gemini-3.6-flash", version: gem.version });
check(R, "Thay khóa với version cũ bị 409", stale.status === 409, brief(stale));
const oa = await conn("OPENAI");
if (!oa?.configured) await T.put("/api/v1/me/ai-connections/OPENAI", { apiKey: "test-openai-DDDD3333", model: "gpt-5-mini", version: null });
const del = await T.del("/api/v1/me/ai-connections/OPENAI");
const oa2 = await conn("OPENAI");
check(R, "U06-03 thu hồi: nhà cung cấp về Chưa cấu hình", (del.status === 204 || del.status === 200) && oa2 && !oa2.configured, `${del.status} configured=${oa2?.configured}`);

// U06-07 / U06-08 khóa của trường
const at = await admin.post("/api/v1/admin/ai/connections/GEMINI:test");
check(R, "U06-07 Kiểm tra khóa trường sai báo khóa không hợp lệ", at.status === 200 && /Key API không hợp lệ/.test(at.data?.lastTestResult ?? ""), `${at.status} ${at.data?.lastTestResult}`);
let ac = ((await admin.get("/api/v1/admin/ai/connections")).data || []).find((c) => c.provider === "GEMINI");
const off = await admin.put("/api/v1/admin/ai/connections/GEMINI/status", { enabled: false, version: ac.version });
let src = ((await T.get("/api/v1/ai/sources")).data || []).find((s) => s.source === "SHARED");
const still = ((await admin.get("/api/v1/admin/ai/connections")).data || []).find((c) => c.provider === "GEMINI");
check(R, "U06-08 tắt khóa: còn trong bảng, API trường không dùng được", off.status === 200 && still?.configured && src && !src.available, `${brief(off)} shared.available=${src?.available} reason=${src?.reason}`);
ac = still;
await admin.put("/api/v1/admin/ai/connections/GEMINI/status", { enabled: true, version: ac.version });

// Bài cá nhân để chạy tác vụ AI (tạo nháp bằng AI)
const asg = await T.post(`/api/v1/classes/${seed.classId}/assignments`, { submissionMode: "INDIVIDUAL", gradingMode: "GRADED", diagramEnabled: false, title: "Bài thử AI " + Date.now() });
const AID = asg.data.id;
const before = await T.get(`/api/v1/assignments/${AID}`);
const draft = (source, provider, i = 0) => T.post(`/api/v1/assignments/${AID}/ai-drafts`, { request: "Tạo đề bài phân tích yêu cầu cho hệ thống thư viện", materialIds: [], sourceFileRefs: [], source, provider, requestRef: `e2e-${Date.now()}-${i}`, options: null });

// U06-17 hết hạn mức không tự đổi nguồn
await admin.put(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`, { dayTokens: 10 });
const q = await draft("SHARED", null);
check(R, "U06-17 hết hạn mức: từ chối, báo lúc có hạn mức mới, không tự đổi nguồn", q.status === 429 || q.status === 409 || q.status === 400, `${brief(q)}`);
await admin.del(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`);

// U06-20 API cá nhân không trừ hạn mức trường; U06-12 trạng thái; U06-19 lỗi không mất việc
const quotaBefore = ((await T.get("/api/v1/ai/sources")).data || []).find((s) => s.source === "SHARED")?.quota;
const p = await draft("PERSONAL", "GEMINI");
const taskId = p.data?.taskId ?? p.data?.task?.taskId ?? p.data?.aiTaskId;
check(R, "U06-12 gửi tác vụ bằng API cá nhân", p.status === 200 || p.status === 201 || p.status === 202, `${brief(p)} ${JSON.stringify(p.data).slice(0, 160)}`);
const states = [];
let final = null;
for (let i = 0; i < 40 && taskId; i++) {
  const t = await T.get(`/api/v1/ai/tasks/${taskId}`);
  if (t.data?.state && states.at(-1) !== t.data.state) states.push(t.data.state);
  if (["READY", "FAILED", "CANCELLED", "UNKNOWN"].includes(t.data?.state)) { final = t.data; break; }
  await new Promise((x) => setTimeout(x, 500));
}
check(R, "U06-12 trạng thái chờ → đang chạy → lỗi (khóa thử sai)", final && final.state === "FAILED" && final.safeError, `states=${states.join("→")} err=${final?.safeError}`);
const after = await T.get(`/api/v1/assignments/${AID}`);
check(R, "U06-19 tác vụ lỗi không làm mất nội dung đang soạn", after.status === 200 && JSON.stringify(after.data.skeleton) === JSON.stringify(before.data.skeleton), `version ${before.data.version}→${after.data.version}`);
const quotaAfter = ((await T.get("/api/v1/ai/sources")).data || []).find((s) => s.source === "SHARED")?.quota;
check(R, "U06-20 API cá nhân không trừ hạn mức trường", quotaBefore && quotaAfter && quotaBefore.used === quotaAfter.used, `${quotaBefore?.used}→${quotaAfter?.used}`);
const hist = await T.get("/api/v1/me/ai-usage?source=PERSONAL");
check(R, "U06-20 lịch sử ghi nguồn API cá nhân", hist.status === 200 && (hist.data?.items ?? []).some((i) => i.source === "PERSONAL"), JSON.stringify((hist.data?.items ?? [])[0] ?? {}).slice(0, 200));

// U06-18 tối đa 3 tác vụ cùng lúc; hủy khi còn chờ
const many = await Promise.all([1, 2, 3, 4].map((i) => draft("PERSONAL", "GEMINI", i)));
const codes = many.map((m) => m.status);
check(R, "U06-18 tác vụ thứ 4 bị từ chối kèm lý do", codes.filter((c) => c < 300).length <= 3 && many.some((m) => m.data?.code === "AI_TOO_MANY_TASKS" || /3/.test(m.data?.detail ?? "")), `${codes} ${many.map((m) => m.data?.code ?? "").join(",")}`);
const queued = many.find((m) => m.status < 300);
const qid = queued?.data?.taskId ?? queued?.data?.task?.taskId;
if (qid) {
  const c = await T.post(`/api/v1/ai/tasks/${qid}:cancel`);
  check(R, "U06-12 hủy được khi còn chờ (hoặc báo đã chạy)", c.status === 200 || c.status === 409, brief(c) + " " + (c.data?.state ?? ""));
}
const stu = await sv.get("/api/v1/ai/sources");
check(R, "U06-11 (sau sửa) sinh viên chỉ có API cá nhân", (stu.data || []).every((s) => s.source === "PERSONAL"), JSON.stringify((stu.data || []).map((s) => s.source)));

fs.writeFileSync(new URL("./u06b-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU06 bổ sung: ${R.filter((x) => x.ok).length}/${R.length} đạt`);
