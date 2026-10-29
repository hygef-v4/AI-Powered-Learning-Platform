import fs from "node:fs";
import { Session, env } from "./lib.mjs";
const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const T = new Session(); await T.login(seed.accounts.teacher.email, PW);
const R = [];
const log = (id, ok, d) => { R.push({ id, ok }); console.log(`${ok ? "PASS" : "FAIL"} ${id} ${d ?? ""}`); };
const t0 = Date.now();
const test = await T.post("/api/v1/me/ai-connections/GEMINI:test");
log("U06-02 Kiểm tra khóa thật: Đạt", test.status === 200 && !test.data?.lastTestResult?.match(/không|lỗi|từ chối|HTTP/i), `${test.status} result=${test.data?.lastTestResult} (${Date.now() - t0} ms)`);
const quotaBefore = ((await T.get("/api/v1/ai/sources")).data || []).find(s => s.source === "SHARED")?.quota;
const asg = await T.post(`/api/v1/classes/${seed.classId}/assignments`, { submissionMode: "INDIVIDUAL", gradingMode: "GRADED", diagramEnabled: false, title: "Bài thử AI thật" });
const AID = asg.data.id;
const before = await T.get(`/api/v1/assignments/${AID}`);
const draft = (i) => T.post(`/api/v1/assignments/${AID}/ai-drafts`, { request: "Tạo đề bài ngắn: phân tích yêu cầu cho hệ thống quản lý thư viện, 2 câu hỏi.", materialIds: [], sourceFileRefs: [], source: "PERSONAL", provider: "GEMINI", requestRef: `real-${Date.now()}-${i}`, options: null });
const p = await draft(0);
const taskId = p.data?.taskId;
const states = []; let final = null;
for (let i = 0; i < 240 && taskId; i++) {
  const t = await T.get(`/api/v1/ai/tasks/${taskId}`);
  if (t.data?.state && states.at(-1) !== t.data.state) states.push(t.data.state);
  if (["READY", "FAILED", "CANCELLED", "UNKNOWN"].includes(t.data?.state)) { final = t.data; break; }
  await new Promise(x => setTimeout(x, 1000));
}
log("U06-12 tác vụ AI thật: chờ → đang chạy → xong", final?.state === "READY", `states=${states.join("→")} apply=${final?.applyResult} err=${final?.safeError ?? ""} usage=${JSON.stringify(final?.usage)}`);
const dr = await T.get(`/api/v1/assignments/${AID}/ai-drafts/${p.data?.draftId}`);
log("U07+U06 bản nháp AI có nội dung xem trước", dr.status === 200 && JSON.stringify(dr.data).length > 300, `${dr.status} status=${dr.data?.status} size=${JSON.stringify(dr.data ?? {}).length}`);
const after = await T.get(`/api/v1/assignments/${AID}`);
log("Bản nháp AI chưa tự áp vào bài (phải bấm áp dụng)", JSON.stringify(after.data.skeleton) === JSON.stringify(before.data.skeleton), "");
const hist = await T.get("/api/v1/me/ai-usage?source=PERSONAL&size=5");
const row = (hist.data?.items || []).find(i => i.taskType === "ASSIGNMENT_DRAFT" && (i.outcome === "READY" || i.outcome === "OK"));
log("U06-06/20 lịch sử ghi token thật, nguồn API cá nhân", !!row && row.inputTokens > 0 && row.outputTokens > 0, JSON.stringify(row ?? (hist.data?.items || [])[0] ?? {}).slice(0, 260));
const quotaAfter = ((await T.get("/api/v1/ai/sources")).data || []).find(s => s.source === "SHARED")?.quota;
log("U06-20 API cá nhân không trừ hạn mức trường", quotaBefore?.used === quotaAfter?.used, `${quotaBefore?.used}→${quotaAfter?.used}`);
// U06-18 với khóa thật: tác vụ chạy vài giây nên 4 yêu cầu liên tiếp phải có 1 bị từ chối
const asgs = []; for (let i = 0; i < 4; i++) asgs.push((await T.post(`/api/v1/classes/${seed.classId}/assignments`, { submissionMode: "INDIVIDUAL", gradingMode: "GRADED", diagramEnabled: false, title: "AI song song " + i })).data.id);
const seq = [];
for (let i = 0; i < 4; i++) { const r = await T.post(`/api/v1/assignments/${asgs[i]}/ai-drafts`, { request: "Tạo đề bài ngắn về sơ đồ use case cho hệ thống thư viện.", materialIds: [], sourceFileRefs: [], source: "PERSONAL", provider: "GEMINI", requestRef: `seq-real-${Date.now()}-${i}`, options: null }); seq.push(r.status + (r.data?.code ? ":" + r.data.code : "")); }
log("U06-18 gửi lần lượt 4 tác vụ: tác vụ thứ 4 bị từ chối", seq.filter(s => s.startsWith("2")).length === 3 && seq[3].includes("AI_TOO_MANY_TASKS"), seq.join(" "));
console.log(`\nAI thật: ${R.filter(r => r.ok).length}/${R.length} đạt`);
