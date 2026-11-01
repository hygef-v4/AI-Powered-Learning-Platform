// Kiểm thử end-to-end góp ý 2026-11-02 trên stack local: tiếp quản, chốt, lịch sử, khôi phục, nhập DOCX/Draw.io,
// đánh giá chéo (U12) và nguồn AI mặc định (U06). Dùng Nhóm A (sv1, trưởng nhóm sv2) của lớp trong seed.json.
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
const S = { sv1: await login("sv1"), sv2: await login("sv2"), sv3: await login("sv3") };

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
check(R, "U06-28 lưu nguồn mặc định API cá nhân Gemini, nguồn được đánh dấu", pref.status === 200 &&
  sources.filter((s) => s.preferred).length === 1 && sources.find((s) => s.preferred)?.provider === "GEMINI", brief(pref));
const bad = await T.put("/api/v1/me/ai-source-preference", { source: "PERSONAL", provider: null });
check(R, "U06-28 API cá nhân thiếu nhà cung cấp bị từ chối", bad.status === 400 && bad.data?.code === "AI_PREFERENCE_INVALID", brief(bad));
const stuShared = await S.sv1.put("/api/v1/me/ai-source-preference", { source: "SHARED", provider: null });
check(R, "U06-28 sinh viên không đặt được API của trường", stuShared.status >= 400, brief(stuShared));
pref = await T.put("/api/v1/me/ai-source-preference", { source: null, provider: null });
sources = (await T.get("/api/v1/ai/sources")).data ?? [];
check(R, "U06-28 về Hỏi mỗi lần: không nguồn nào được đánh dấu", pref.status === 200 && pref.data?.source === null && !sources.some((s) => s.preferred), brief(pref));
await T.put("/api/v1/me/ai-source-preference", { source: oldPref?.source ?? null, provider: oldPref?.provider ?? null });

// ---------- Bài nhóm 3 phần, mở 15 phút
const set = (await T.get(`/api/v1/classes/${C}/groups`)).data;
const GA = set.groups.find((x) => x.members.some((m) => m.accountId === A.sv1.id && m.active));
check(R, "Chuẩn bị: sv1 có nhóm, trưởng nhóm là sv2", GA && GA.leaderId === A.sv2.id, GA ? `${GA.name} leader=${GA.leaderId === A.sv2.id}` : "sv1 chưa có nhóm");
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
  const d = await S.sv1.get(`/api/v1/assignments/${AID}/my-group-doc`);
  D = d.data?.documentId ?? null;
  if (!D) await new Promise((x) => setTimeout(x, 500));
}
let w = (await S.sv1.get(`/api/v1/group-docs/${D}`)).data;
const [s1, s2, s3] = w.sections;
const para = (t) => [{ id: crypto.randomUUID(), origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: t }] } }];

// ---------- Tiếp quản (BR-U12-33)
let c = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:claim`);
const d1 = await S.sv1.put(`/api/v1/group-docs/${D}/sections/${s1.id}/draft`, { version: c.data.version, blocks: para("Nháp của sv1") });
const take = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s1.id}:take-over`);
check(R, "U12-33 trưởng nhóm tiếp quản mục sv1 đang giữ, làm tiếp từ bản nháp", take.status === 200 && take.data?.holderId === A.sv2.id &&
  JSON.stringify(take.data?.draftBlocks ?? []).includes("Nháp của sv1"), brief(take));
const late = await S.sv1.put(`/api/v1/group-docs/${D}/sections/${s1.id}/draft`, { version: d1.data.version, blocks: para("sv1 gõ tiếp") });
check(R, "U12-33 người bị tiếp quản lưu tiếp nhận SECTION_NOT_HELD", late.status === 409 && late.data?.code === "SECTION_NOT_HELD", brief(late));
const outsiderTake = await S.sv3.post(`/api/v1/group-docs/${D}/sections/${s1.id}:take-over`);
check(R, "U12-33 sinh viên nhóm khác không tiếp quản được", outsiderTake.status === 404, brief(outsiderTake));
const teacherTake = await T.post(`/api/v1/group-docs/${D}/sections/${s1.id}:take-over`);
check(R, "U12-33 giảng viên không tiếp quản được", teacherTake.status >= 400, brief(teacherTake));

// ---------- Lịch sử, Xong, chốt (BR-U12-35, 39)
let revs = await S.sv1.get(`/api/v1/group-docs/${D}/sections/${s1.id}/revisions`);
check(R, "U12-35 lịch sử có bản TAKEOVER của sv1", revs.status === 200 && revs.data?.[0]?.kind === "TAKEOVER" && revs.data[0].authorId === A.sv1.id, brief(revs));
const d2 = await S.sv2.put(`/api/v1/group-docs/${D}/sections/${s1.id}/draft`, { version: take.data.version, blocks: para("Bản trưởng nhóm") });
const done = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s1.id}:done`);
check(R, "U12-35 trưởng nhóm bấm Xong mục đã tiếp quản", d2.status === 200 && done.status === 200 && done.data?.status === "DONE", brief(done));
const memberLock = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:lock`);
check(R, "U12-39 thành viên không chốt được", memberLock.status === 403, brief(memberLock));
const notDone = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s3.id}:lock`);
check(R, "U12-39 mục chưa Xong không chốt được", notDone.status === 409 && notDone.data?.code === "SECTION_NOT_DONE", brief(notDone));
const lock = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s1.id}:lock`);
check(R, "U12-39 trưởng nhóm chốt mục đã Xong", lock.status === 200 && lock.data?.lockedBy === A.sv2.id, brief(lock));
const claimLocked = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:claim`);
const assignLocked = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s1.id}:assign`, { assigneeId: A.sv1.id, replaceHolder: false });
check(R, "U12-39 mục đã chốt không nhận, không giao được", claimLocked.data?.code === "SECTION_LOCKED" && assignLocked.data?.code === "SECTION_LOCKED", `${brief(claimLocked)} | ${brief(assignLocked)}`);
const unlock = await S.sv2.post(`/api/v1/group-docs/${D}/sections/${s1.id}:unlock`);
check(R, "U12-39 mở chốt", unlock.status === 200 && !unlock.data?.lockedBy, brief(unlock));

// ---------- Khôi phục (BR-U12-35)
c = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:claim`);
revs = await S.sv1.get(`/api/v1/group-docs/${D}/sections/${s1.id}/revisions`);
const takeoverRev = revs.data.find((r) => r.kind === "TAKEOVER");
const restore = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:restore`, { revisionId: takeoverRev.id, version: c.data.version });
check(R, "U12-35 khôi phục bản lịch sử vào bản nháp", restore.status === 200 && JSON.stringify(restore.data?.draftBlocks).includes("Nháp của sv1"), brief(restore));
await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s1.id}:release`);

// ---------- Nhập DOCX vào mục và cả tài liệu (BR-U12-46)
const exported = await S.sv2.call("GET", `/api/v1/group-docs/${D}/export.docx?submitted=false`, undefined, { raw: true });
const docxBytes = Buffer.from(await exported.arrayBuffer());
const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
c = await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s3.id}:claim`);
const secPrev = await multipart(S.sv1, `/api/v1/group-docs/${D}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "U12-46 người giữ xem trước DOCX vào mục, không còn heading", secPrev.status === 200 && secPrev.data?.document?.blocks?.length > 0 &&
  !secPrev.data.document.blocks.some((b) => b.type === "HEADING"), `${secPrev.status} blocks=${secPrev.data?.document?.blocks?.length}`);
const notHolder = await multipart(S.sv2, `/api/v1/group-docs/${D}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "U12-46 không giữ mục thì không nhập DOCX vào mục", notHolder.status === 409, `${notHolder.status} ${notHolder.data?.code}`);
const drawio = `<mxfile><diagram name="2. Thiết kế" id="p1"><mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/></root></mxGraphModel></diagram><diagram name="Trang lạ" id="p2">x</diagram></mxfile>`;
const memberImport = await multipart(S.sv1, `/api/v1/group-docs/${D}/import:preview`, { docx: [docxBytes, "nhom.docx", DOCX] });
check(R, "U12-46 thành viên không nhập cả tài liệu", memberImport.status === 403, `${memberImport.status}`);
const prev = await multipart(S.sv2, `/api/v1/group-docs/${D}/import:preview`, { docx: [docxBytes, "nhom.docx", DOCX], drawio: [Buffer.from(drawio), "nhom.drawio", "application/xml"] });
const plan = prev.data?.sections ?? [];
check(R, "U12-46 xem trước chia theo tên mục, sơ đồ vào mục 2, mục đang giữ bị bỏ qua", prev.status === 200 &&
  plan.some((p) => p.sectionId === s1.id && !p.skipReason) && plan.some((p) => p.sectionId === s2.id && p.blocks.some((b) => b.type === "DIAGRAM")) &&
  (plan.find((p) => p.sectionId === s3.id)?.skipReason ?? "").includes("người giữ") && prev.data.unmatched.some((u) => u.includes("Trang lạ")),
  `${prev.status} ${JSON.stringify(plan.map((p) => [p.title, p.blocks.length, p.skipReason]))} unmatched=${JSON.stringify(prev.data?.unmatched)}`);
const apply = await S.sv2.post(`/api/v1/group-docs/${D}/import:apply`, { sections: plan.filter((p) => !p.skipReason).map((p) => ({ sectionId: p.sectionId, version: p.version, blocks: p.blocks })) });
w = (await S.sv2.get(`/api/v1/group-docs/${D}`)).data;
const s2now = w.sections.find((s) => s.id === s2.id);
check(R, "U12-46 áp dụng: mục 2 Xong có sơ đồ, lịch sử IMPORT", apply.status === 200 && apply.data?.applied >= 2 && s2now.status === "DONE" &&
  s2now.publishedBlocks.some((b) => b.type === "DIAGRAM"), `${brief(apply)} applied=${apply.data?.applied}`);
const rev2 = await S.sv2.get(`/api/v1/group-docs/${D}/sections/${s2.id}/revisions`);
check(R, "U12-46 mục được nhập có bản lịch sử IMPORT", rev2.data?.[0]?.kind === "IMPORT", brief(rev2));
await S.sv1.post(`/api/v1/group-docs/${D}/sections/${s3.id}:release`);

// ---------- Đánh giá chéo (BR-U12-47)
const early = await S.sv1.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: [{ revieweeId: A.sv2.id, score: 4, comment: "Tốt" }] });
check(R, "U12-47 chưa nộp thì chưa đánh giá được", early.status === 409 && early.data?.code === "PEER_REVIEW_CLOSED", brief(early));
w = (await S.sv2.get(`/api/v1/group-docs/${D}`)).data;
const sub = await S.sv2.post(`/api/v1/group-docs/${D}/submit`, { version: w.version });
const self = await S.sv1.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: [{ revieweeId: A.sv1.id, score: 5, comment: null }] });
check(R, "U12-47 không tự chấm mình", sub.status === 200 && self.status === 400, `${brief(sub)} | ${brief(self)}`);
const mates = GA.members.filter((m) => m.active && m.accountId !== A.sv1.id).map((m) => m.accountId);
const save = await S.sv1.put(`/api/v1/group-docs/${D}/peer-reviews`, { reviews: mates.map((id) => ({ revieweeId: id, score: id === A.sv2.id ? 4 : 3, comment: "Đóng góp tốt" })) });
check(R, "U12-47 thành viên lưu đánh giá cho các thành viên khác", save.status === 200 && save.data?.mine?.length === mates.length, brief(save));
const sv2View = await S.sv2.get(`/api/v1/group-docs/${D}/peer-reviews`);
check(R, "U12-47 thành viên khác không thấy đánh giá của sv1", sv2View.status === 200 && sv2View.data?.mine?.length === 0 && sv2View.data?.summary?.length === 0, brief(sv2View));
const tView = await T.get(`/api/v1/group-docs/${D}/peer-reviews`);
const lead = tView.data?.summary?.find((s) => s.accountId === A.sv2.id);
check(R, "U12-47 giảng viên thấy trung bình và thang 10", tView.status === 200 && lead?.average === 4 && lead?.outOfTen === 8 && tView.data.reviewers === 1,
  `${brief(tView)} lead=${JSON.stringify(lead && { a: lead.average, t: lead.outOfTen })} reviewers=${tView.data?.reviewers}`);

// ---------- Nhật ký (U02) ghi các thao tác mới
const admin = new Session("admin");
await admin.login(env("U01_BOOTSTRAP_ADMIN_EMAIL"), PW);
const found = new Set();
for (const action of ["GROUP_SECTION_TAKEN_OVER", "GROUP_SECTION_LOCKED", "GROUP_SECTION_UNLOCKED", "GROUP_SECTION_RESTORED", "GROUP_DOC_IMPORTED", "GROUP_PEER_REVIEW_SAVED"]) {
  const r = await admin.get(`/api/v1/admin/audit-logs?action=${action}&size=5`);
  if ((r.data?.items ?? []).length > 0) found.add(action);
}
check(R, "U02 nhật ký ghi tiếp quản, chốt, mở chốt, khôi phục, nhập file, đánh giá chéo", found.size === 6, [...found].join(","));

fs.writeFileSync(new URL("./u12c-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nGóp ý 11/2: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
process.exit(0);
