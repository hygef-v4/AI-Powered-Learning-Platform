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

// ---------- CP1: chia nhóm
let set = await T.get(`/api/v1/classes/${C}/groups`);
if ((set.data?.groups ?? []).length > 0) {
  console.log("lớp đã có nhóm từ lần chạy trước; dùng lại");
}
check(R, "U12-01 xem nhóm của lớp", set.status === 200, `groups=${set.data?.groups?.length} ungrouped=${set.data?.ungrouped?.length}`);
const p1 = await T.post(`/api/v1/classes/${C}/groups:random-preview`, { maxSize: 1 });
const p21 = await T.post(`/api/v1/classes/${C}/groups:random-preview`, { maxSize: 21 });
check(R, "U12-02 cỡ nhóm ngoài 2–20 bị báo lỗi", p1.status === 400 && p21.status === 400, `${brief(p1)} | ${brief(p21)}`);
const p2 = await T.post(`/api/v1/classes/${C}/groups:random-preview`, { maxSize: 2 });
const sizes = (p2.data || []).filter((g) => g.generated).map((g) => g.memberIds.length);
check(R, "U12-02 xem trước chia ngẫu nhiên, cỡ lệch ≤ 1, có trưởng", p2.status === 200 && sizes.length > 0 && Math.max(...sizes) - Math.min(...sizes) <= 1 && (p2.data || []).every((g) => g.leaderId), `sizes=${sizes}`);

const save = (groups, version = set.data.version) => T.put(`/api/v1/classes/${C}/groups`, { version, groups });
const g = (key, id, name, leader, members) => ({ key, id, name, leaderId: leader ? A[leader].id : null, memberIds: members.map((m) => A[m].id) });
let bad = await save([g("n1", null, "Nhóm A", null, ["sv1", "sv2"])]);
check(R, "U12-03 nhóm không có trưởng bị báo lỗi theo nhóm", bad.status === 400 && bad.data?.code === "GROUP_SET_INVALID" && (bad.data?.issues || []).some((i) => i.groupKey === "n1"), brief(bad) + " " + JSON.stringify(bad.data?.issues));
bad = await save([g("n1", null, "x".repeat(101), "sv1", ["sv1"])]);
check(R, "U12-16 tên 101 ký tự bị báo lỗi", bad.status === 400, JSON.stringify(bad.data?.issues));
bad = await save([g("n1", null, "Trùng", "sv1", ["sv1"]), g("n2", null, "trùng", "sv3", ["sv3"])]);
check(R, "U12-16 tên trùng trong lớp bị báo lỗi", bad.status === 400, JSON.stringify(bad.data?.issues));
bad = await save([g("n1", null, "Nhóm A", "sv1", ["sv1", "sv2"]), g("n2", null, "Nhóm B", "sv3", ["sv2", "sv3"])]);
check(R, "Một sinh viên chỉ ở một nhóm", bad.status === 400, JSON.stringify(bad.data?.issues));
const name100 = "N".repeat(100);
let ok = await save([g("n1", null, "Nhóm A", "sv1", ["sv1", "sv2"]), g("n2", null, name100, "sv3", ["sv3", "sv4"])]);
check(R, "U12-16 tên 100 ký tự được nhận; lưu cả lớp", ok.status === 200, brief(ok));
const stale = await save([g("n1", null, "Nhóm A", "sv1", ["sv1", "sv2"])], set.data.version);
check(R, "Lưu với version cũ bị 409, không ghi đè", stale.status === 409, brief(stale));
set = await T.get(`/api/v1/classes/${C}/groups`);
const G1 = set.data.groups.find((x) => x.name === "Nhóm A");
const G2 = set.data.groups.find((x) => x.name === name100);
ok = await save([{ ...g("g1", G1.id, "Nhóm A", "sv1", ["sv1", "sv2"]) }, { ...g("g2", G2.id, "Nhóm B", "sv3", ["sv3", "sv4"]) }]);
set = await T.get(`/api/v1/classes/${C}/groups`);
check(R, "U12-03 đổi tên nhóm, sv5 chưa có nhóm", ok.status === 200 && set.data.ungrouped.some((p) => p.accountId === A.sv5.id), brief(ok));

// ---------- Nhóm của tôi (U12-06, U12-29)
const my1 = await S.sv1.get(`/api/v1/me/classes/${C}/group`);
check(R, "U12-06 Nhóm của tôi: thành viên và trưởng nhóm", my1.status === 200 && my1.data?.leaderId === A.sv1.id && my1.data?.members?.length === 2, `${my1.status} leader=${my1.data?.leaderId === A.sv1.id}`);
const my5 = await S.sv5.get(`/api/v1/me/classes/${C}/group`);
check(R, "U12-29 sinh viên chưa có nhóm: báo chưa có, không thấy nhóm khác", (my5.status === 200 && !my5.data?.groupId) || my5.status === 404, `${my5.status} ${JSON.stringify(my5.data).slice(0, 120)}`);
const peek = await S.sv5.get(`/api/v1/classes/${C}/groups`);
check(R, "Sinh viên không xem được bộ nhóm của lớp (API giảng viên)", peek.status === 403 || peek.status === 404, peek.status);

// ---------- Đổi trưởng nhóm (U12-07, 04, 20, 21)
const lr = (s, body) => s.post(`/api/v1/groups/${G1.id}/leader-requests`, body);
let r = await lr(S.sv2, { reason: "ngắn", proposedLeaderId: A.sv2.id });
check(R, "U12-07 lý do dưới 10 ký tự bị chặn", r.status === 400, brief(r));
r = await lr(S.sv2, { reason: "Trưởng nhóm bận thực tập cả tháng", proposedLeaderId: A.sv2.id });
const req1 = r.data;
check(R, "U12-07 gửi yêu cầu", r.status === 201 || r.status === 200, brief(r));
r = await lr(S.sv1, { reason: "Một yêu cầu nữa cho cùng nhóm", proposedLeaderId: null });
check(R, "U12-07 mỗi nhóm chỉ một yêu cầu chờ", r.status === 409, brief(r));
r = await S.sv2.post(`/api/v1/leader-requests/${req1.id}:cancel`);
const myAfterCancel = await S.sv2.get(`/api/v1/me/classes/${C}/group`);
check(R, "U12-07 hủy yêu cầu: Đã hủy", (r.status === 200 || r.status === 204) && JSON.stringify(myAfterCancel.data?.myRequests ?? []).includes("CANCELLED"), brief(r));
r = await lr(S.sv2, { reason: "Trưởng nhóm bận thực tập cả tháng", proposedLeaderId: A.sv2.id });
const req2 = r.data;
let rj = await T.post(`/api/v1/leader-requests/${req2.id}:reject`, { reason: "" });
check(R, "U12-04 từ chối bắt buộc lý do", rj.status === 400, brief(rj));
rj = await T.post(`/api/v1/leader-requests/${req2.id}:reject`, { reason: "Nhóm đang ổn, giữ nguyên" });
check(R, "U12-04 từ chối có lý do", rj.status === 200 || rj.status === 204, brief(rj));
r = await lr(S.sv2, { reason: "Không đề cử ai, nhờ thầy chọn giúp", proposedLeaderId: null });
const req3 = r.data;
let ap = await T.post(`/api/v1/leader-requests/${req3.id}:approve`, { newLeaderId: null });
check(R, "U12-20 duyệt không có đề cử phải chọn trưởng mới", ap.status === 400, brief(ap));
ap = await T.post(`/api/v1/leader-requests/${req3.id}:approve`, { newLeaderId: A.sv2.id });
set = await T.get(`/api/v1/classes/${C}/groups`);
check(R, "U12-04 duyệt thì trưởng nhóm đổi", (ap.status === 200 || ap.status === 204) && set.data.groups.find((x) => x.id === G1.id).leaderId === A.sv2.id, brief(ap));
r = await S.sv3.post(`/api/v1/groups/${G2.id}/leader-requests`, { reason: "Muốn đổi trưởng để chia việc", proposedLeaderId: A.sv4.id });
const req4 = r.data;
ok = await save([g("g1", G1.id, "Nhóm A", "sv2", ["sv1", "sv2"]), g("g2", G2.id, "Nhóm B", "sv4", ["sv3", "sv4"])], set.data.version);
const reqs = await T.get(`/api/v1/classes/${C}/leader-requests`);
const my3 = await S.sv3.get(`/api/v1/me/classes/${C}/group`);
check(R, "U12-21 giảng viên đổi trưởng trực tiếp: yêu cầu chờ tự hủy", ok.status === 200 && JSON.stringify(my3.data?.myRequests ?? []).includes("CANCELLED") && !JSON.stringify(reqs.data ?? []).includes(req4?.id ?? "none"), `${brief(ok)} sv3=${JSON.stringify((my3.data?.myRequests ?? []).map((x) => x.status))}`);

// ---------- Bài nhóm: tạo, kiểm sẵn sàng, phát hành (U12-22, 23, 05)
const created = await T.post(`/api/v1/classes/${C}/assignments`, { submissionMode: "GROUP", gradingMode: "PRACTICE", diagramEnabled: true, title: "Bài nhóm thử – practice" });
check(R, "U12-22 bài nhóm chỉ có chế độ Graded", created.status >= 400, brief(created));
const asg = await T.post(`/api/v1/classes/${C}/assignments`, { submissionMode: "GROUP", gradingMode: "GRADED", diagramEnabled: true, title: "Bài nhóm thử E2E" });
check(R, "U12-22 tạo bài nhóm", asg.status === 201 || asg.status === 200, brief(asg));
const AID = asg.data.id;
const H = (t) => ({ id: crypto.randomUUID(), origin: "TEACHER", type: "HEADING", content: { style: "H1", text: t } });
const P = (t) => ({ id: crypto.randomUUID(), origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: t }] } });
let detail = await T.get(`/api/v1/assignments/${AID}`);
const sk = await T.put(`/api/v1/assignments/${AID}/skeleton`, { version: detail.data.version, skeleton: { blocks: [P("Vẽ sơ đồ và giải thích."), H("1. Phân tích"), H("2. Thiết kế"), H("3. Kết luận")] }, instructions: "Làm theo nhóm", fromDocx: false });
check(R, "U12-22 soạn 3 phần lá", sk.status === 200, brief(sk));
detail = await T.get(`/api/v1/assignments/${AID}`);
for (const part of detail.data.parts || []) {
  const rub = await T.get(`/api/v1/assignments/${AID}/parts/${part.partId ?? part.id}/rubric`);
  const put = await T.put(`/api/v1/assignments/${AID}/parts/${part.partId ?? part.id}/rubric`, { version: rub.data?.version ?? 0, criteria: [{ criterionId: crypto.randomUUID(), title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: crypto.randomUUID(), text: "Đúng yêu cầu", points: 5 }] }] });
  if (put.status >= 300) console.log("rubric", part.title, brief(put));
}
detail = await T.get(`/api/v1/assignments/${AID}`);
const closesAt = new Date(Date.now() + 3 * 60 * 1000).toISOString();
let pub = await T.post(`/api/v1/assignments/${AID}/publish`, { version: detail.data.version, openNow: true, closesAt, maxAttempts: null, confirmUngroupedStudents: false });
check(R, "U12-05 sinh viên chưa có nhóm là cảnh báo cần xác nhận", pub.status === 409 && pub.data?.code === "GROUP_CONFIRMATION_REQUIRED", brief(pub));
pub = await T.post(`/api/v1/assignments/${AID}/publish`, { version: detail.data.version, openNow: true, closesAt, maxAttempts: null, confirmUngroupedStudents: true });
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
const d2 = await S.sv1.put(`/api/v1/group-docs/${D1}/sections/${s2.id}/draft`, { version: c.data.version, blocks: blk });
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

fs.writeFileSync(new URL("./u12-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU12: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
process.exit(0);
