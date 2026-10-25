// Kiểm thử end-to-end U12 qua API trên stack local. Ca có mã U12-xx theo kế hoạch kiểm thử của nhóm.
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
const T = await login("teacher");
const S = {};
for (const k of ["sv1", "sv2", "sv3", "sv4", "sv5"]) S[k] = await login(k);
const brief = (r) => `${r.status} ${r.data?.code ?? ""} ${(r.data?.detail ?? "").slice(0, 90)}`;

let set = await T.get(`/api/v1/classes/${C}/groups`);
const save = (groups, version = set.data.version) => T.put(`/api/v1/classes/${C}/groups`, { version, groups });
const g = (key, id, name, leader, members) => ({ key, id, name, leaderId: leader ? A[leader].id : null, memberIds: members.map((m) => A[m].id) });
if (!set.data.groups.some((x) => x.name === "Nhóm A")) {
  const B0 = set.data.groups.find((x) => x.name === "Nhóm B");
  const fix = await save([g("a", null, "Nhóm A", "sv2", ["sv1", "sv2"]), g("b", B0.id, "Nhóm B", "sv4", ["sv3", "sv4"])]);
  console.log("dựng lại Nhóm A", fix.status, JSON.stringify(fix.data?.issues ?? ""));
  set = await T.get(`/api/v1/classes/${C}/groups`);
}
const G1 = set.data.groups.find((x) => x.name === "Nhóm A");
const G2 = set.data.groups.find((x) => x.name === "Nhóm B");
let r, ok;
// ---------- Bài nhóm: tạo, kiểm sẵn sàng, phát hành (U12-22, 23, 05)
const created = await T.post(`/api/v1/classes/${C}/assignments`, { submissionMode: "GROUP", gradingMode: "PRACTICE", diagramEnabled: true, title: "Bài nhóm thử – practice" });
check(R, "U12-22 bài nhóm chỉ có chế độ Graded", created.status >= 400, brief(created));
const asg = await T.post(`/api/v1/classes/${C}/assignments`, { submissionMode: "GROUP", gradingMode: "GRADED", diagramEnabled: true, title: "Bài nhóm thử E2E " + new Date().toISOString().slice(11,19) });
check(R, "U12-22 tạo bài nhóm", asg.status === 201 || asg.status === 200, brief(asg));
const AID = asg.data.id;
const H = (t) => ({ id: crypto.randomUUID(), origin: "TEACHER", type: "HEADING", content: { style: "H1", text: t } });
const P = (t) => ({ id: crypto.randomUUID(), origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: t }] } });
let detail = await T.get(`/api/v1/assignments/${AID}`);
const sk = await T.put(`/api/v1/assignments/${AID}/skeleton`, { version: detail.data.version, skeleton: { blocks: [P("Vẽ sơ đồ và giải thích."), H("1. Phân tích"), H("2. Thiết kế"), H("3. Kết luận")] }, instructions: "Làm theo nhóm", fromDocx: false });
check(R, "U12-22 soạn 3 phần lá", sk.status === 200, brief(sk));
detail = await T.get(`/api/v1/assignments/${AID}`);
for (const part of detail.data.parts || []) {
  const cur = await T.get(`/api/v1/assignments/${AID}`);
  const put = await T.put(`/api/v1/assignments/${AID}/parts/${part.partId ?? part.id}/rubric`, { version: cur.data.version, criteria: [{ criterionId: crypto.randomUUID(), title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: crypto.randomUUID(), text: "Đúng yêu cầu", points: 5 }] }] });
  if (put.status >= 300) console.log("rubric", part.title, brief(put));
}
detail = await T.get(`/api/v1/assignments/${AID}`);
const closesAt = new Date(Date.now() + 3 * 60 * 1000).toISOString();
let pub = await T.post(`/api/v1/assignments/${AID}/publish`, { version: detail.data.version, openNow: true, closesAt, maxAttempts: 3, confirmUngroupedStudents: false });
check(R, "U12-05 sinh viên chưa có nhóm là cảnh báo cần xác nhận", pub.status === 409 && pub.data?.code === "GROUP_CONFIRMATION_REQUIRED", brief(pub));
pub = await T.post(`/api/v1/assignments/${AID}/publish`, { version: detail.data.version, openNow: true, closesAt, maxAttempts: 3, confirmUngroupedStudents: true });
check(R, "U12-22 phát hành bài nhóm (hạn 3 phút)", pub.status === 200, brief(pub));

// ---------- Tài liệu nhóm (U12-08..12, 24, 25, 26, 27)
const docOf = async (s) => {
  for (let i = 0; i < 20; i++) {
    const d = await s.get(`/api/v1/assignments/${AID}/my-group-doc`);
    if (d.status === 200 && (d.data?.documentId || d.data?.id)) return d.data.documentId ?? d.data.id;
    await new Promise((x) => setTimeout(x, 500));
  }
  return null;
};
const D1 = await docOf(S.sv1);
const D2 = await docOf(S.sv3);
let w = await S.sv1.get(`/api/v1/group-docs/${D1}`);
check(R, "U12-22 mỗi nhóm có tài liệu với 3 mục đúng 3 phần lá", D1 && D2 && D1 !== D2 && w.data?.sections?.length === 3, `D1=${!!D1} D2=${!!D2} sections=${w.data?.sections?.map((s) => s.title)}`);
const outsider = await S.sv3.get(`/api/v1/group-docs/${D1}`);
check(R, "U12-25 sinh viên nhóm khác nhận không tìm thấy", outsider.status === 404, outsider.status);
const tv = await T.get(`/api/v1/group-docs/${D1}`);
const tClaim = await T.post(`/api/v1/group-docs/${D1}/sections/${w.data.sections[2].id}:claim`);
check(R, "U12-25 giảng viên chỉ xem, không nhận/sửa", tv.status === 200 && tv.data?.role?.teacher && tClaim.status >= 400, `view=${tv.status} claim=${tClaim.status}`);
const [s1, s2, s3] = w.data.sections;

// SSE cho B (sv2) – đo thời gian nhận sự kiện khi A bấm Xong (U12-24)
const events = [];
const ctl = new AbortController();
(async () => {
  try {
    const res = await fetch(`${BASE}/api/v1/group-docs/${D1}/events`, { headers: { Cookie: S.sv2.cookieHeader(), Accept: "text/event-stream" }, signal: ctl.signal });
    const reader = res.body.getReader();
    let buf = "";
    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      buf += new TextDecoder().decode(value);
      let i;
      while ((i = buf.indexOf("\n\n")) >= 0) {
        const chunk = buf.slice(0, i);
        buf = buf.slice(i + 2);
        const data = chunk.split("\n").filter((l) => l.startsWith("data:")).map((l) => l.slice(5).trim()).join("");
        if (data) events.push({ at: Date.now(), data });
      }
    }
  } catch {}
})();
await new Promise((x) => setTimeout(x, 1000));

let c = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s1.id}:claim`);
check(R, "U12-08 nhận mục", c.status === 200 && c.data?.holderId === A.sv1.id, brief(c));
const otherDraft = await S.sv2.put(`/api/v1/group-docs/${D1}/sections/${s1.id}/draft`, { version: c.data.version, blocks: [] });
check(R, "U12-09 thành viên khác không sửa được mục người khác giữ", otherDraft.status >= 400, brief(otherDraft));
const blk = [{ id: crypto.randomUUID(), origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Phân tích của Sinh viên 1" }] } }];
const dr = await S.sv1.put(`/api/v1/group-docs/${D1}/sections/${s1.id}/draft`, { version: c.data.version, blocks: blk });
check(R, "U12-08 tự lưu bản nháp", dr.status === 200, brief(dr));
const before = events.length;
const doneAt = Date.now();
const dn = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s1.id}:done`, { version: dr.data.version });
check(R, "U12-08 bấm Xong: mục chuyển Xong", dn.status === 200 && dn.data?.status === "DONE", brief(dn));
await new Promise((x) => setTimeout(x, 2500));
const ev = events.slice(before)[0];
check(R, "U12-24 B nhận cập nhật trong 2 giây sau khi A bấm Xong", ev && ev.at - doneAt <= 2000, ev ? `${ev.at - doneAt} ms ${ev.data.slice(0, 80)}` : "không có sự kiện");
check(R, "U12-24 lúc A gõ nháp B không nhận nội dung", !events.slice(0, before).some((e) => e.data.includes("Phân tích của")), `events trước Xong=${before}`);

c = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:claim`);
let as = await S.sv2.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:assign`, { assigneeId: A.sv2.id, replaceHolder: false });
check(R, "U12-10 giao mục đang có người giữ: cần nhả trước", as.status === 409, brief(as));
as = await S.sv2.post(`/api/v1/group-docs/${D1}/sections/${s3.id}:assign`, { assigneeId: A.sv1.id, replaceHolder: false });
check(R, "U12-10 giao mục trống thành công", as.status === 200 && as.data?.holderId === A.sv1.id, brief(as));
const asMember = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:assign`, { assigneeId: A.sv2.id, replaceHolder: true });
check(R, "Thành viên không phải trưởng không giao được", asMember.status >= 400, brief(asMember));
const rel1 = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s3.id}:release`);
check(R, "U12-27 người giữ tự nhả: về Mở", rel1.status === 200 && rel1.data?.status === "OPEN", brief(rel1));
const rel2 = await S.sv2.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:release`);
check(R, "U12-27 trưởng nhóm nhả mục của thành viên", rel2.status === 200 && rel2.data?.status === "OPEN", brief(rel2));
c = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s1.id}:claim`);
const relDone = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s1.id}:release`);
check(R, "U12-27 nhả mục đã có nội dung Xong: về Xong", relDone.status === 200 && relDone.data?.status === "DONE", brief(relDone));

w = await S.sv1.get(`/api/v1/group-docs/${D1}`);
const subMember = await S.sv1.post(`/api/v1/group-docs/${D1}/submit`, { version: w.data.version });
check(R, "U12-11 thành viên không nộp được", subMember.status === 403 || subMember.status === 404 || subMember.status === 409, brief(subMember));
const sub1 = await S.sv2.post(`/api/v1/group-docs/${D1}/submit`, { version: w.data.version });
check(R, "U12-11 trưởng nhóm nộp khi còn mục chưa Xong, có mã biên nhận", sub1.status === 200 && sub1.data?.receiptHash && (sub1.data?.warnings ?? []).length > 0, `${brief(sub1)} warnings=${JSON.stringify(sub1.data?.warnings)}`);
const docx = await S.sv2.call("GET", `/api/v1/group-docs/${D1}/export.docx?submitted=true`, undefined, { raw: true });
const buf = Buffer.from(await docx.arrayBuffer());
check(R, "U12-12 tải DOCX bản nộp", docx.status === 200 && buf.slice(0, 2).toString() === "PK", `${docx.status} ${docx.headers.get("content-type")} ${buf.length}B`);
c = await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:claim`);
// Mỗi block cần mã riêng: dùng lại mã của mục khác thì bản ghép có hai block trùng mã và bị từ chối.
const d2 = await S.sv1.put(`/api/v1/group-docs/${D1}/sections/${s2.id}/draft`, { version: c.data.version, blocks: [{ ...blk[0], id: crypto.randomUUID(), content: { runs: [{ text: "Thiết kế của Sinh viên 1" }] } }] });
await S.sv1.post(`/api/v1/group-docs/${D1}/sections/${s2.id}:done`, { version: d2.data.version });
w = await S.sv1.get(`/api/v1/group-docs/${D1}`);
const sub2 = await S.sv2.post(`/api/v1/group-docs/${D1}/submit`, { version: w.data.version });
check(R, "U12-26 sửa tiếp và nộp lại: mã biên nhận mới", sub2.status === 200 && sub2.data?.receiptHash && sub2.data.receiptHash !== sub1.data?.receiptHash, brief(sub2));

// ---------- Theo dõi (U12-14, 15) và xóa nhóm đã dùng (U12-17)
const prog = await T.get(`/api/v1/classes/${C}/group-assignments/${AID}/progress`);
check(R, "U12-14 bảng tiến độ có từng nhóm và trạng thái", prog.status === 200 && JSON.stringify(prog.data).includes("Nhóm A"), `${prog.status} ${JSON.stringify(prog.data).slice(0, 200)}`);
const cl = await S.sv4.post(`/api/v1/group-docs/${D2}/sections/${(await S.sv4.get(`/api/v1/group-docs/${D2}`)).data.sections[0].id}:claim`);
const tRel = await T.post(`/api/v1/group-docs/${D2}/sections/${cl.data?.id}:release`);
check(R, "U12-15 giảng viên nhả mục đang giữ", tRel.status === 200 && tRel.data?.status === "OPEN", brief(tRel));
set = await T.get(`/api/v1/classes/${C}/groups`);
const delUsed = await save([g("g2", G2.id, "Nhóm B", "sv4", ["sv3", "sv4", "sv1", "sv2"])], set.data.version);
check(R, "U12-17 nhóm đã dùng không xóa được", delUsed.status === 400 || delUsed.status === 409, brief(delUsed) + " " + JSON.stringify(delUsed.data?.issues ?? ""));
const noAI = JSON.stringify((await S.sv1.get(`/api/v1/group-docs/${D1}`)).data);
check(R, "U12-28 tài liệu nhóm không có chức năng AI", !/aiTask|ai-feedback|AI_FEEDBACK/i.test(noAI), "");

// ---------- Tự nộp khi hết hạn (U12-13)
console.log("chờ hạn nộp", closesAt);
const autoBefore = events.length;
const wait = Math.max(0, new Date(closesAt).getTime() - Date.now()) + 100_000;
let auto = null;
for (const end = Date.now() + wait; Date.now() < end; ) {
  await new Promise((x) => setTimeout(x, 5000));
  const d = await S.sv3.get(`/api/v1/group-docs/${D2}`);
  if (d.data?.lastSubmission?.mode?.startsWith("AUTO")) { auto = d.data; break; }
}
check(R, "U12-13 hết hạn: nhóm chưa nộp được tự nộp", auto && auto.lastSubmission.receiptHash, auto ? `${auto.lastSubmission.mode} ${auto.lastSubmission.submittedAt}` : "chưa tự nộp");
check(R, "U12-13 sự kiện realtime khi tự nộp/đóng", events.slice(autoBefore).some((e) => /auto-submitted|closed/.test(e.data)), events.slice(autoBefore).map((e) => e.data.slice(0, 60)).join(" | "));
ctl.abort();

fs.writeFileSync(new URL("./u12b-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU12: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
process.exit(0);
