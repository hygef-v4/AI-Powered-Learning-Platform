// Kiểm thử end-to-end góp ý 2026-11-02 trên stack local: chốt, lịch sử, khôi phục, nhập DOCX/Draw.io,
// đánh giá chéo (U12) và nguồn AI mặc định (U06). Tự chọn một nhóm của lớp trong seed.json có trưởng nhóm và thành viên.
import fs from "node:fs";
import { BASE, Session, check, env } from "./lib.mjs";

const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const C = seed.classId;
const A = seed.accounts;
const R = [];
const login = async (key) => {
  const s = new Session(key);
  await s.login(A[key].email, PW);
  return s;
};
const brief = (r) => `${r.status} ${r.data?.code ?? ""} ${(r.data?.detail ?? "").slice(0, 90)}`;
const T = await login("teacher");
const S = {};
for (const k of ["sv1", "sv2", "sv3", "sv4", "sv5"]) S[k] = await login(k);

/** Gửi multipart kèm cookie và CSRF của phiên. */
async function multipart(session, path, files) {
  if (!session.jar.has("XSRF-TOKEN")) await session.get("/api/v1/me/profile");
  const form = new FormData();
  for (const [name, [bytes, filename, type]] of Object.entries(files)) form.append(name, new Blob([bytes], { type }), filename);
  const res = await fetch(BASE + path, {
    method: "POST",
    headers: { Accept: "application/json", Cookie: session.cookieHeader(), "X-XSRF-TOKEN": decodeURIComponent(session.jar.get("XSRF-TOKEN")) },
    body: form,
  });
  session.absorb(res);
  const text = await res.text();
  return { status: res.status, data: text ? JSON.parse(text) : null };
}

// ---------- U06: nguồn AI mặc định (giữ lại mặc định cũ để trả về cuối bài)
const oldPref = (await T.get("/api/v1/me/ai-source-preference")).data;
let pref = await T.put("/api/v1/me/ai-source-preference", { source: "PERSONAL", provider: "GEMINI" });
let sources = (await T.get("/api/v1/ai/sources")).data ?? [];
check(R, "FB-U06-01 lưu nguồn mặc định API cá nhân Gemini, nguồn được đánh dấu", pref.status === 200 &&
  sources.filter((s) => s.preferred).length === 1 && sources.find((s) => s.preferred)?.provider === "GEMINI", brief(pref));
const bad = await T.put("/api/v1/me/ai-source-preference", { source: "PERSONAL", provider: null });
check(R, "FB-U06-01 API cá nhân thiếu nhà cung cấp bị từ chối", bad.status === 400 && bad.data?.code === "AI_PREFERENCE_INVALID", brief(bad));
const stuShared = await S.sv1.put("/api/v1/me/ai-source-preference", { source: "SHARED", provider: null });
check(R, "FB-U06-01 sinh viên không đặt được API của trường", stuShared.status >= 400, brief(stuShared));
pref = await T.put("/api/v1/me/ai-source-preference", { source: null, provider: null });
sources = (await T.get("/api/v1/ai/sources")).data ?? [];
check(R, "FB-U06-01 về Hỏi mỗi lần: không nguồn nào được đánh dấu", pref.status === 200 && pref.data?.source === null && !sources.some((s) => s.preferred), brief(pref));
await T.put("/api/v1/me/ai-source-preference", { source: oldPref?.source ?? null, provider: oldPref?.provider ?? null });

// ---------- Bài nhóm 3 phần, mở 15 phút
const set = (await T.get(`/api/v1/classes/${C}/groups`)).data;
// Nhóm có trưởng nhóm và ít nhất một thành viên khác; người ngoài là sinh viên của nhóm khác.
const keyOf = (id) => Object.keys(S).find((k) => A[k].id === id);
const GA = set.groups.find((x) => x.leaderId && keyOf(x.leaderId) && x.members.some((m) => m.active && m.accountId !== x.leaderId && keyOf(m.accountId)));
const LK = keyOf(GA?.leaderId);
const MK = keyOf(GA?.members.find((m) => m.active && m.accountId !== GA.leaderId && keyOf(m.accountId))?.accountId);
const OK = Object.keys(S).find((k) => set.groups.some((x) => x.id !== GA?.id && x.members.some((m) => m.active && m.accountId === A[k].id)));
check(R, "Chuẩn bị: có nhóm với trưởng nhóm, thành viên và người nhóm khác", GA && LK && MK && OK, `${GA?.name} leader=${LK} member=${MK} outsider=${OK}`);
const L = S[LK], M = S[MK], O = S[OK];
const LID = A[LK].id, MID = A[MK].id;
const asg = await T.post(`/api/v1/classes/${C}/assignments`, { submissionMode: "GROUP", gradingMode: "GRADED", diagramEnabled: true, title: "Góp ý 11/2 " + new Date().toISOString().slice(11, 19) });
const AID = asg.data.id;
const H = (t) => ({ id: crypto.randomUUID(), origin: "TEACHER", type: "HEADING", content: { style: "H1", text: t } });
let detail = await T.get(`/api/v1/assignments/${AID}`);
await T.put(`/api/v1/assignments/${AID}/skeleton`, { version: detail.data.version, skeleton: { blocks: [H("1. Phân tích"), H("2. Thiết kế"), H("3. Kết luận")] }, instructions: "Làm theo nhóm", fromDocx: false });
detail = await T.get(`/api/v1/assignments/${AID}`);
for (const part of detail.data.parts || []) {
  const cur = await T.get(`/api/v1/assignments/${AID}`);
  await T.put(`/api/v1/assignments/${AID}/parts/${part.partId ?? part.id}/rubric`, { version: cur.data.version, criteria: [{ criterionId: crypto.randomUUID(), title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: crypto.randomUUID(), text: "Đúng", points: 5 }] }] });
}
detail = await T.get(`/api/v1/assignments/${AID}`);
const pub = await T.post(`/api/v1/assignments/${AID}/publish`, { version: detail.data.version, openNow: true, closesAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(), maxAttempts: 3, confirmUngroupedStudents: true });
check(R, "Chuẩn bị: phát hành bài nhóm 3 phần", pub.status === 200, brief(pub));
let D = null;
for (let i = 0; i < 20 && !D; i++) {
  const d = await M.get(`/api/v1/assignments/${AID}/my-group-doc`);
  D = d.data?.documentId ?? null;
  if (!D) await new Promise((x) => setTimeout(x, 500));
}
let w = (await M.get(`/api/v1/group-docs/${D}`)).data;
const [s1, s2, s3] = w.sections;
const para = (t) => [{ id: crypto.randomUUID(), origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: t }] } }];

// ---------- Phân công rồi chốt (FB-U12-01)
const base = `/api/v1/group-docs/${D}`;
let c = await M.post(`${base}/sections/${s1.id}:claim`);
check(R, "FB-U12-01 phân công: thành viên nhận mục làm người phụ trách", c.status === 200 && c.data?.assigneeId === MID && !c.data?.holderId, brief(c));
const leaderClaim = await L.post(`${base}/sections/${s1.id}:claim`);
check(R, "U12-33 mục đã có người nhận: trưởng nhóm cũng không nhận được", leaderClaim.status === 409 && leaderClaim.data?.code === "SECTION_CLAIMED_BY_OTHER", brief(leaderClaim));
const takeGone = await L.post(`${base}/sections/${s1.id}:take-over`);
check(R, "U12-33 không còn chức năng tiếp quản", takeGone.status === 404 || takeGone.status === 405, `${takeGone.status}`);
const tooEarly = await M.put(`${base}/sections/${s1.id}/draft`, { version: c.data.version, blocks: para("Sớm") });
check(R, "FB-U12-01 chưa chốt phân công thì chưa soạn được", tooEarly.status === 409 && tooEarly.data?.code === "ASSIGNMENT_NOT_LOCKED", brief(tooEarly));
const memberLock = await M.post(`${base}/assignment:lock`);
check(R, "FB-U12-01 thành viên không chốt phân công được", memberLock.status === 403, brief(memberLock));
const incomplete = await L.post(`${base}/assignment:lock`);
check(R, "FB-U12-01 còn mục trống thì chưa chốt được", incomplete.status === 409 && incomplete.data?.code === "ASSIGNMENT_INCOMPLETE", brief(incomplete));
await L.post(`${base}/sections/${s2.id}:assign`, { assigneeId: LID, replaceHolder: false });
await L.post(`${base}/sections/${s3.id}:assign`, { assigneeId: MID, replaceHolder: false });
const lock = await L.post(`${base}/assignment:lock`);
const ws = (await M.get(base)).data;
check(R, "FB-U12-01 trưởng nhóm chốt phân công, workspace hiện trạng thái và người phụ trách", lock.status === 200 && ws.assignmentLockedBy === LID &&
  ws.sections.find((x) => x.id === s1.id)?.assigneeId === MID, brief(lock));
const assignLocked = await L.post(`${base}/sections/${s1.id}:assign`, { assigneeId: LID, replaceHolder: false });
check(R, "FB-U12-01 đã chốt thì không giao lại được", assignLocked.data?.code === "ASSIGNMENT_LOCKED", brief(assignLocked));
c = await M.post(`${base}/sections/${s1.id}:claim`);
const d1 = await M.put(`${base}/sections/${s1.id}/draft`, { version: c.data.version, blocks: para("Bản của thành viên") });
const done = await M.post(`${base}/sections/${s1.id}:done`);
check(R, "U12-35 người phụ trách soạn và bấm Xong mục của mình", c.data?.holderId === MID && d1.status === 200 && done.data?.status === "DONE", brief(done));
const notMine = await L.post(`${base}/sections/${s1.id}:claim`);
check(R, "FB-U12-01 trưởng nhóm không soạn mục người khác phụ trách", notMine.status === 409 && notMine.data?.code === "SECTION_ASSIGNED_TO_OTHER", brief(notMine));
const unlock = await L.post(`${base}/assignment:unlock`);
const wsU = (await M.get(base)).data;
check(R, "FB-U12-01 mở chốt để phân lại, nội dung đã Xong giữ nguyên", unlock.status === 204 && !wsU.assignmentLockedAt &&
  wsU.sections.find((x) => x.id === s1.id)?.status === "DONE", `${unlock.status}`);
await L.post(`${base}/assignment:lock`);

// ---------- Lịch sử và khôi phục (FB-U12-02)
let revs = await O.get(`/api/v1/group-docs/${D}/sections/${s1.id}/revisions`);
check(R, "FB-U12-02 sinh viên nhóm khác không xem được lịch sử", revs.status === 404, `${revs.status}`);
revs = await L.get(`/api/v1/group-docs/${D}/sections/${s1.id}/revisions`);
check(R, "FB-U12-02 lịch sử có bản Xong của thành viên", revs.status === 200 && revs.data?.[0]?.kind === "DONE" && revs.data[0].authorId === MID, brief(revs));
c = await M.post(`/api/v1/group-docs/${D}/sections/${s1.id}:claim`);
const d2 = await M.put(`/api/v1/group-docs/${D}/sections/${s1.id}/draft`, { version: c.data.version, blocks: para("Bản sửa sau") });
const restore = await M.post(`/api/v1/group-docs/${D}/sections/${s1.id}:restore`, { revisionId: revs.data[0].id, version: d2.data.version });
check(R, "FB-U12-02 khôi phục bản lịch sử vào bản nháp", restore.status === 200 && JSON.stringify(restore.data?.draftBlocks).includes("Bản của thành viên"), brief(restore));
await M.post(`/api/v1/group-docs/${D}/sections/${s1.id}:release`);

// ---------- Nhập DOCX vào mục và cả tài liệu (FB-U12-03)
const exported = await L.call("GET", `/api/v1/group-docs/${D}/export.docx?submitted=false`, undefined, { raw: true });
const docxBytes = Buffer.from(await exported.arrayBuffer());
const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
c = await M.post(`/api/v1/group-docs/${D}/sections/${s3.id}:claim`);
const secPrev = await multipart(M, `/api/v1/group-docs/${D}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "FB-U12-03 người giữ xem trước DOCX vào mục, không còn heading", secPrev.status === 200 && secPrev.data?.document?.blocks?.length > 0 &&
  !secPrev.data.document.blocks.some((b) => b.type === "HEADING"), `${secPrev.status} blocks=${secPrev.data?.document?.blocks?.length}`);
const notHolder = await multipart(L, `/api/v1/group-docs/${D}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "FB-U12-03 không giữ mục thì không nhập DOCX vào mục", notHolder.status === 409, `${notHolder.status} ${notHolder.data?.code}`);
const drawio = `<mxfile><diagram name="2. Thiết kế" id="p1"><mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="2" value="Lớp A" vertex="1" parent="1"><mxGeometry x="20" y="20" width="120" height="60" as="geometry"/></mxCell></root></mxGraphModel></diagram><diagram name="Trang lạ" id="p2">x</diagram></mxfile>`;
const memberImport = await multipart(M, `/api/v1/group-docs/${D}/import:preview`, { docx: [docxBytes, "nhom.docx", DOCX] });
check(R, "FB-U12-03 thành viên không nhập cả tài liệu", memberImport.status === 403, `${memberImport.status}`);
const prev = await multipart(L, `/api/v1/group-docs/${D}/import:preview`, { docx: [docxBytes, "nhom.docx", DOCX], drawio: [Buffer.from(drawio), "nhom.drawio", "application/xml"] });
const plan = prev.data?.sections ?? [];
check(R, "FB-U12-03 xem trước chia theo tên mục, sơ đồ vào mục 2, mục đang có người soạn bị bỏ qua", prev.status === 200 &&
  plan.some((p) => p.sectionId === s1.id && !p.skipReason) && plan.some((p) => p.sectionId === s2.id && p.blocks.some((b) => b.type === "DIAGRAM")) &&
  (plan.find((p) => p.sectionId === s3.id)?.skipReason ?? "").includes("người soạn") && prev.data.unmatched.some((u) => u.includes("Trang lạ")),
  `${prev.status} ${JSON.stringify(plan.map((p) => [p.title, p.blocks.length, p.skipReason]))} unmatched=${JSON.stringify(prev.data?.unmatched)}`);
const apply = await L.post(`/api/v1/group-docs/${D}/import:apply`, { sections: plan.filter((p) => !p.skipReason).map((p) => ({ sectionId: p.sectionId, version: p.version, blocks: p.blocks })) });
w = (await L.get(`/api/v1/group-docs/${D}`)).data;
const s2now = w.sections.find((s) => s.id === s2.id);
check(R, "FB-U12-03 áp dụng: mục 2 Xong có sơ đồ, lịch sử IMPORT", apply.status === 200 && apply.data?.applied >= 2 && s2now.status === "DONE" &&
  s2now.publishedBlocks.some((b) => b.type === "DIAGRAM"), `${brief(apply)} applied=${apply.data?.applied}`);
const rev2 = await L.get(`/api/v1/group-docs/${D}/sections/${s2.id}/revisions`);
check(R, "FB-U12-03 mục được nhập có bản lịch sử IMPORT", rev2.data?.[0]?.kind === "IMPORT", brief(rev2));
await M.post(`/api/v1/group-docs/${D}/sections/${s3.id}:release`);

// ---------- Đánh giá chéo (FB-U12-04)
const early = await M.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: [{ revieweeId: LID, score: 4, comment: "Tốt" }] });
check(R, "FB-U12-04 chưa nộp thì chưa đánh giá được", early.status === 409 && early.data?.code === "PEER_REVIEW_CLOSED", brief(early));
w = (await L.get(`/api/v1/group-docs/${D}`)).data;
const sub = await L.post(`/api/v1/group-docs/${D}/submit`, { version: w.version });
if (sub.status !== 200) console.log("submit issues", JSON.stringify(sub.data?.issues ?? sub.data));
const self = await M.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: [{ revieweeId: MID, score: 5, comment: null }] });
check(R, "FB-U12-04 không tự chấm mình", sub.status === 200 && self.status === 400, `${brief(sub)} | ${brief(self)}`);
const mates = GA.members.filter((m) => m.active && m.accountId !== MID).map((m) => m.accountId);
const save = await M.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: mates.map((id) => ({ revieweeId: id, score: id === LID ? 4 : 3, comment: "Đóng góp tốt" })) });
check(R, "FB-U12-04 thành viên lưu đánh giá cho các thành viên khác", save.status === 200 && save.data?.mine?.length === mates.length, brief(save));
const leaderView = await L.get(`/api/v1/group-docs/${D}/peer-reviews`);
check(R, "FB-U12-04 thành viên khác không thấy đánh giá của người khác", leaderView.status === 200 && leaderView.data?.mine?.length === 0 && leaderView.data?.summary?.length === 0, brief(leaderView));
const tView = await T.get(`/api/v1/group-docs/${D}/peer-reviews`);
const lead = tView.data?.summary?.find((s) => s.accountId === LID);
check(R, "FB-U12-04 giảng viên thấy trung bình và thang 10", tView.status === 200 && lead?.average === 4 && lead?.outOfTen === 8 && tView.data.reviewers === 1,
  `${brief(tView)} lead=${JSON.stringify(lead && { a: lead.average, t: lead.outOfTen })} reviewers=${tView.data?.reviewers}`);

// ---------- Nhật ký (U02) ghi các thao tác mới
const admin = new Session("admin");
await admin.login(env("U01_BOOTSTRAP_ADMIN_EMAIL"), PW);
const found = new Set();
for (const action of ["GROUP_ASSIGNMENT_LOCKED", "GROUP_ASSIGNMENT_UNLOCKED", "GROUP_SECTION_RESTORED", "GROUP_DOC_IMPORTED", "GROUP_PEER_REVIEW_SAVED"]) {
  const r = await admin.get(`/api/v1/admin/audit-logs?action=${action}&size=5`);
  if ((r.data?.items ?? []).length > 0) found.add(action);
}
check(R, "U02 nhật ký ghi chốt phân công, mở chốt, khôi phục, nhập file, đánh giá chéo", found.size === 5, [...found].join(","));

fs.writeFileSync(new URL("./u12c-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nGóp ý 11/2: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
process.exit(0);
