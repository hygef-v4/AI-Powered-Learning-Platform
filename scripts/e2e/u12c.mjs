// Kiểm thử end-to-end trên stack local: luồng mục tối giản của U12 theo BR-U12-33, 35, 36 (nhận là soạn, nhận lại mục
// Xong, lịch sử chỉ xem, nhập DOCX vào mục), các API đã bỏ ngày 2026-11-08 không còn, và nguồn AI mặc định (U06).
// Tự chọn một nhóm của lớp trong seed.json có trưởng nhóm và thành viên.
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

// ---------- Mục theo BR-U12-33, 36: nhận là soạn ngay
const base = `/api/v1/group-docs/${D}`;
let c = await M.post(`${base}/sections/${s1.id}:claim`);
check(R, "U12-33 thành viên nhận mục là soạn ngay", c.status === 200 && c.data?.status === "CLAIMED" && c.data?.holderId === MID, brief(c));
const leaderClaim = await L.post(`${base}/sections/${s1.id}:claim`);
check(R, "U12-33 mục người khác đang giữ: trưởng nhóm cũng không nhận được", leaderClaim.status === 409 && leaderClaim.data?.code === "SECTION_CLAIMED_BY_OTHER", brief(leaderClaim));
const takeGone = await L.post(`${base}/sections/${s1.id}:take-over`);
check(R, "U12-33 không có chức năng tiếp quản", takeGone.status === 404 || takeGone.status === 405, `${takeGone.status}`);
const d1 = await M.put(`${base}/sections/${s1.id}/draft`, { version: c.data.version, blocks: para("Bản của thành viên") });
const done = await M.post(`${base}/sections/${s1.id}:done`);
check(R, "U12-35 người giữ soạn và bấm Xong", d1.status === 200 && done.data?.status === "DONE" && done.data?.lastAuthorId === MID, brief(done));

// ---------- Lịch sử chỉ xem (BR-U12-35)
let revs = await O.get(`${base}/sections/${s1.id}/revisions`);
check(R, "U12-35 sinh viên nhóm khác không xem được lịch sử", revs.status === 404, `${revs.status}`);
revs = await L.get(`${base}/sections/${s1.id}/revisions`);
check(R, "U12-35 lịch sử có bản Xong kèm tác giả", revs.status === 200 && revs.data?.[0]?.kind === "DONE" && revs.data[0].authorId === MID, brief(revs));
const tRevs = await T.get(`${base}/sections/${s1.id}/revisions`);
check(R, "U12-35 giảng viên của lớp xem được lịch sử", tRevs.status === 200 && tRevs.data?.length >= 1, brief(tRevs));

// ---------- Nhận lại mục đã Xong (BR-U12-33)
c = await L.post(`${base}/sections/${s1.id}:claim`);
check(R, "U12-33 trưởng nhóm nhận lại mục đã Xong, bản nháp từ nội dung đã đăng", c.status === 200 && c.data?.holderId === LID && JSON.stringify(c.data?.draftBlocks).includes("Bản của thành viên"), brief(c));
const rel = await L.post(`${base}/sections/${s1.id}:release`);
check(R, "U12-36 nhả mục đã có nội dung: về Xong, nội dung giữ nguyên", rel.status === 200 && rel.data?.status === "DONE" && JSON.stringify(rel.data?.publishedBlocks).includes("Bản của thành viên"), brief(rel));

// ---------- Nhập DOCX vào mục đang soạn
const exported = await L.call("GET", `${base}/export.docx?submitted=false`, undefined, { raw: true });
const docxBytes = Buffer.from(await exported.arrayBuffer());
const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
c = await M.post(`${base}/sections/${s3.id}:claim`);
const secPrev = await multipart(M, `${base}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "Người giữ xem trước DOCX vào mục, không còn heading", secPrev.status === 200 && secPrev.data?.document?.blocks?.length > 0 &&
  !secPrev.data.document.blocks.some((b) => b.type === "HEADING"), `${secPrev.status} blocks=${secPrev.data?.document?.blocks?.length}`);
const notHolder = await multipart(L, `${base}/sections/${s3.id}/docx:preview`, { file: [docxBytes, "nhom.docx", DOCX] });
check(R, "Không giữ mục thì không nhập DOCX vào mục", notHolder.status === 409, `${notHolder.status} ${notHolder.data?.code}`);
await M.post(`${base}/sections/${s3.id}:release`);

// ---------- Các API đã bỏ ngày 2026-11-08 (chốt phân công, khôi phục, nhập cả tài liệu, đánh giá chéo)
const gone = [
  await L.post(`${base}/assignment:lock`),
  await L.post(`${base}/assignment:unlock`),
  await M.post(`${base}/sections/${s1.id}:restore`, { revisionId: revs.data?.[0]?.id, version: 1 }),
  await L.post(`${base}/import:apply`, { sections: [] }),
  await L.get(`${base}/peer-reviews`),
];
check(R, "Các API đã bỏ không còn (404/405)", gone.every((r) => r.status === 404 || r.status === 405), gone.map((r) => r.status).join(","));

// ---------- Giảng viên xem tiến độ từng nhóm (BR-U12-52)
const prog = await T.get(`/api/v1/classes/${C}/group-assignments/${AID}/progress`);
const mine = (prog.data ?? []).find((x) => x.groupId === GA.id);
check(R, "U12-52 tiến độ có nhóm, số mục Xong và tài liệu", prog.status === 200 && mine?.done >= 1 && mine?.documentId === D, `${prog.status} ${JSON.stringify(mine).slice(0, 160)}`);

fs.writeFileSync(new URL("./u12c-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU12c: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
process.exit(0);
