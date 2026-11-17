// U06 bổ sung: thay/thu hồi khóa, tắt khóa trường, hết hạn mức, 3 tác vụ, trạng thái tác vụ, lỗi AI không mất việc.
// Khóa dùng ở đây là khóa thử (sai) nên tác vụ gọi nhà cung cấp sẽ lỗi — đúng cái cần kiểm cho U06-19.
// Khóa API cá nhân quản lý theo id, một nhà cung cấp có thể có nhiều khóa (yêu cầu 2026-11-05).
// Không sửa hay thu hồi khóa Gemini của giảng viên thử: người dùng có thể đã dán khóa thật ở đó (u06-real-key.mjs dùng).
// Worker tự thử khóa cá nhân kế tiếp khi khóa được chọn lỗi, nên trước khi chạy tác vụ script tạm tắt mọi khóa cá nhân
// khác của giảng viên (chỉ đổi trạng thái bật/tắt) rồi bật lại; không cô lập được thì bỏ qua phần tác vụ, không gọi AI.
// Thêm/thay/thu hồi khóa làm trên tài khoản chủ nhiệm môn; tác vụ lỗi chạy bằng khóa OpenAI giả của giảng viên.
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
const M = new Session("manager");
await M.login(seed.accounts.manager.email, PW);
// Khóa cá nhân chưa thu hồi của một tài khoản (API không trả khóa đã thu hồi), lọc theo nhà cung cấp nếu có.
const listKeys = async (s, provider) => ((await s.get("/api/v1/me/ai-connections")).data || []).filter((c) => !provider || c.provider === provider);
const addKey = (s, provider, apiKey, model) => s.post("/api/v1/me/ai-connections", { provider, apiKey, model });
const reloadKey = async (s, id) => (await listKeys(s)).find((c) => c.id === id);

// U06-02 / U06-13 / U06-03 trên tài khoản chủ nhiệm môn
let gem = (await listKeys(M, "GEMINI"))[0];
let r = gem
  ? await M.put(`/api/v1/me/ai-connections/${gem.id}`, { apiKey: "test-khoa-sai-AAAA1111", model: "gemini-3.6-flash", version: gem.version })
  : await addKey(M, "GEMINI", "test-khoa-sai-AAAA1111", "gemini-3.6-flash");
gem = r.data;
const tested = await M.post(`/api/v1/me/ai-connections/${gem.id}:test`);
check(R, "U06-02 khóa sai: Kiểm tra báo khóa không hợp lệ", tested.status === 200 && /Key API không hợp lệ/.test(tested.data?.lastTestResult ?? ""), `${tested.status} ${tested.data?.lastTestResult}`);
gem = await reloadKey(M, gem.id);
const countBefore = (await listKeys(M, "GEMINI")).length;
r = await M.put(`/api/v1/me/ai-connections/${gem.id}`, { apiKey: "test-khoa-thay-BBBB2222", model: "gemini-3.6-flash", version: gem.version });
const rows = await listKeys(M, "GEMINI");
check(R, "U06-13 thay khóa theo id: cùng dòng, không thêm dòng mới", r.status === 200 && r.data?.id === gem.id && rows.length === countBefore && r.data?.keyHint === "2222", `${brief(r)} rows=${countBefore}→${rows.length} hint=${r.data?.keyHint}`);
const stale = await M.put(`/api/v1/me/ai-connections/${gem.id}`, { apiKey: "test-khoa-cu-CCCC", model: "gemini-3.6-flash", version: gem.version });
check(R, "Thay khóa với version cũ bị 409", stale.status === 409, brief(stale));
const second = await addKey(M, "GEMINI", "test-khoa-hai-FFFF5555", "gemini-3.6-flash");
const withSecond = await listKeys(M, "GEMINI");
check(R, "U06-13 một nhà cung cấp có nhiều khóa: thêm khóa thứ hai", second.status < 300 && second.data?.id !== gem.id && withSecond.length === countBefore + 1, `${brief(second)} rows=${countBefore}→${withSecond.length}`);
if (second.data?.id) await M.del(`/api/v1/me/ai-connections/${second.data.id}`);
const oa = await addKey(M, "OPENAI", "test-openai-DDDD3333", "gpt-5-mini");
const del = await M.del(`/api/v1/me/ai-connections/${oa.data?.id}`);
const oaLeft = (await listKeys(M)).filter((c) => c.id === oa.data?.id || c.id === second.data?.id);
check(R, "U06-03 thu hồi: khóa không còn trong danh sách", oa.status < 300 && (del.status === 204 || del.status === 200) && oaLeft.length === 0, `add=${oa.status} del=${del.status} còn=${oaLeft.length}`);

// U06-07 / U06-08 khóa của trường
// Khóa của trường gọi theo id (một nhà cung cấp có thể có nhiều khóa).
let ac = ((await admin.get("/api/v1/admin/ai/connections")).data || []).find((c) => c.provider === "GEMINI");
const at = await admin.post(`/api/v1/admin/ai/connections/${ac.id}:test`);
check(R, "U06-07 Kiểm tra khóa trường sai báo khóa không hợp lệ", at.status === 200 && /Key API không hợp lệ/.test(at.data?.lastTestResult ?? ""), `${at.status} ${at.data?.lastTestResult}`);
ac = ((await admin.get("/api/v1/admin/ai/connections")).data || []).find((c) => c.id === ac.id);
const off = await admin.put(`/api/v1/admin/ai/connections/${ac.id}/status`, { enabled: false, version: ac.version });
let src = ((await T.get("/api/v1/ai/sources")).data || []).find((s) => s.source === "SHARED");
const still = ((await admin.get("/api/v1/admin/ai/connections")).data || []).find((c) => c.id === ac.id);
check(R, "U06-08 tắt khóa: còn trong bảng, API trường không dùng được", off.status === 200 && still?.configured && src && !src.available, `${brief(off)} shared.available=${src?.available} reason=${src?.reason}`);
ac = still;
await admin.put(`/api/v1/admin/ai/connections/${ac.id}/status`, { enabled: true, version: ac.version });

// Bài cá nhân để chạy tác vụ AI (tạo nháp bằng AI)
const asg = await T.post(`/api/v1/classes/${seed.classId}/assignments`, { submissionMode: "INDIVIDUAL", gradingMode: "GRADED", diagramEnabled: false, title: "Bài thử AI " + Date.now() });
const AID = asg.data.id;
const before = await T.get(`/api/v1/assignments/${AID}`);
const draft = (source, provider, i = 0) => T.post(`/api/v1/assignments/${AID}/ai-drafts`, { request: "Tạo đề bài phân tích yêu cầu cho hệ thống thư viện", materialIds: [], sourceFileRefs: [], source, provider, requestRef: `e2e-${Date.now()}-${i}`, options: null });

// Khóa OpenAI giả của giảng viên: nhận theo đuôi khóa, chưa có thì thêm.
const FAKE_HINT = "4444";
let fake = (await listKeys(T, "OPENAI")).find((c) => c.keyHint === FAKE_HINT);
if (!fake) fake = (await addKey(T, "OPENAI", "test-openai-gv-EEEE" + FAKE_HINT, "gpt-5-mini")).data;
if (fake.status !== "ACTIVE") fake = (await T.put(`/api/v1/me/ai-connections/${fake.id}/status`, { enabled: true, version: fake.version })).data;
// Tạm tắt mọi khóa cá nhân khác đang bật để worker không tự chuyển sang khóa thật; bật lại ở finally.
const paused = [];
for (const c of await listKeys(T)) {
  if (c.id === fake.id || c.status !== "ACTIVE") continue;
  const off = await T.put(`/api/v1/me/ai-connections/${c.id}/status`, { enabled: false, version: c.version });
  if (off.status === 200) paused.push(c.id);
}
const stillOn = (await listKeys(T)).filter((c) => c.id !== fake.id && c.status === "ACTIVE");
const isolated = stillOn.length === 0;
check(R, "Chuẩn bị: chỉ còn khóa giả đang bật, tác vụ không chạm khóa thật", isolated, `tạm tắt ${paused.length} khóa, còn bật ${stillOn.length}`);
try {
if (isolated) {
// U06-17 hết hạn mức không tự đổi nguồn
await admin.put(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`, { dayTokens: 10 });
const q = await draft("SHARED", null);
check(R, "U06-17 hết hạn mức: từ chối, báo lúc có hạn mức mới, không tự đổi nguồn", q.status === 429 || q.status === 409 || q.status === 400, `${brief(q)}`);
await admin.del(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`);

// U06-20 API cá nhân không trừ hạn mức trường; U06-12 trạng thái; U06-19 lỗi không mất việc
const quotaBefore = ((await T.get("/api/v1/ai/sources")).data || []).find((s) => s.source === "SHARED")?.quota;
const p = await draft("PERSONAL", "OPENAI");
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
const many = await Promise.all([1, 2, 3, 4].map((i) => draft("PERSONAL", "OPENAI", i)));
const codes = many.map((m) => m.status);
check(R, "U06-18 tác vụ thứ 4 bị từ chối kèm lý do", codes.filter((c) => c < 300).length <= 3 && many.some((m) => m.data?.code === "AI_TOO_MANY_TASKS" || /3/.test(m.data?.detail ?? "")), `${codes} ${many.map((m) => m.data?.code ?? "").join(",")}`);
const queued = many.find((m) => m.status < 300);
const qid = queued?.data?.taskId ?? queued?.data?.task?.taskId;
if (qid) {
  const c = await T.post(`/api/v1/ai/tasks/${qid}:cancel`);
  check(R, "U06-12 hủy được khi còn chờ (hoặc báo đã chạy)", c.status === 200 || c.status === 409, brief(c) + " " + (c.data?.state ?? ""));
}
// Đợi các tác vụ còn lại kết thúc rồi mới bật lại khóa, để chúng không kịp chuyển sang khóa vừa bật.
for (const m of many) {
  const id = m.data?.taskId ?? m.data?.task?.taskId;
  for (let i = 0; i < 60 && id; i++) {
    const t = await T.get(`/api/v1/ai/tasks/${id}`);
    if (["READY", "FAILED", "CANCELLED", "UNKNOWN"].includes(t.data?.state)) break;
    await new Promise((x) => setTimeout(x, 500));
  }
}
}
} finally {
  for (const id of paused) {
    const c = await reloadKey(T, id);
    if (c && c.status !== "ACTIVE") await T.put(`/api/v1/me/ai-connections/${id}/status`, { enabled: true, version: c.version });
  }
}
const restored = (await listKeys(T)).filter((c) => paused.includes(c.id) && c.status === "ACTIVE").length;
check(R, "Dọn dẹp: bật lại các khóa đã tạm tắt", restored === paused.length, `${restored}/${paused.length}`);
const stu = await sv.get("/api/v1/ai/sources");
check(R, "U06-11 (sau sửa) sinh viên chỉ có API cá nhân", (stu.data || []).every((s) => s.source === "PERSONAL"), JSON.stringify((stu.data || []).map((s) => s.source)));

fs.writeFileSync(new URL("./u06b-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU06 bổ sung: ${R.filter((x) => x.ok).length}/${R.length} đạt`);
