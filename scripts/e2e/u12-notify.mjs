// U12 → U14: đổi thành viên nhóm phải tạo thông báo cho người được chuyển (review R07). Chạy sau u12.mjs (cần Nhóm A, B).
import fs from "node:fs";
import { Session, check, env } from "./lib.mjs";

const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const A = seed.accounts;
const C = seed.classId;
const R = [];
const T = new Session("teacher");
await T.login(A.teacher.email, PW);
const sv5 = new Session("sv5");
await sv5.login(A.sv5.email, PW);

const unread = async () => ((await sv5.get("/api/v1/me/notifications?size=50")).data?.items ?? []).filter((n) => n.type === "GROUP_MEMBERSHIP_CHANGED");
const before = (await unread()).length;
const set = (await T.get(`/api/v1/classes/${C}/groups`)).data;
const target = set.groups.find((g) => g.name === "Nhóm B") ?? set.groups[0];
const drafts = set.groups.map((g) => ({
  key: g.id,
  id: g.id,
  name: g.name,
  leaderId: g.leaderId,
  memberIds: [...g.members.filter((m) => m.active).map((m) => m.accountId), ...(g.id === target.id ? [A.sv5.id] : [])],
}));
const saved = await T.put(`/api/v1/classes/${C}/groups`, { version: set.version, groups: drafts });
check(R, "Chuyển sinh viên chưa có nhóm vào nhóm", saved.status === 200, `${saved.status} ${saved.data?.code ?? ""}`);

let after = before;
let note = null;
for (let i = 0; i < 20 && after <= before; i++) {
  await new Promise((x) => setTimeout(x, 1000));
  const list = await unread();
  after = list.length;
  note = list[0];
}
check(R, "R07 người được chuyển nhóm nhận thông báo GROUP_MEMBERSHIP_CHANGED", after > before, `${before}→${after} ${note ? `"${note.title}" link=${note.link}` : ""}`);
check(R, "R07 link thông báo dẫn tới trang lớp của sinh viên", !note || String(note.link ?? "").startsWith(`/classes/${C}`), String(note?.link ?? ""));

console.log(`\nU12→U14 thông báo: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
