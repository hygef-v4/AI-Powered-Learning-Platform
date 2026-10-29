// Kiểm thử end-to-end U02 Audit (UC 13) qua API trên stack local, đối chiếu BR-U02.
import fs from "node:fs";
import { Session, check, env } from "./lib.mjs";

const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const R = [];
const admin = new Session("admin");
await admin.login(env("U01_BOOTSTRAP_ADMIN_EMAIL"), PW);
const teacher = new Session("teacher");
await teacher.login(seed.accounts.teacher.email, PW);
const manager = new Session("manager");
await manager.login(seed.accounts.manager.email, PW);
const L = "/api/v1/admin/audit-logs";

const all = await admin.get(`${L}?size=100`);
const items = all.data?.items ?? [];
check(R, "UC13 admin xem nhật ký", all.status === 200 && items.length > 0, `${all.status} total=${all.data?.total} page=${items.length}`);
const times = items.map((i) => new Date(i.occurredAt ?? i.at ?? i.createdAt).getTime());
check(R, "BR-U02-08 sắp xếp mới nhất trước", times.every((t, i) => i === 0 || t <= times[i - 1]), `first=${items[0]?.occurredAt ?? JSON.stringify(items[0]).slice(0, 80)}`);
const big = await admin.get(`${L}?size=500`);
check(R, "BR-U02-08 mỗi trang tối đa 100", big.status === 400 || (big.data?.items ?? []).length <= 100, `${big.status} n=${(big.data?.items ?? []).length}`);
for (const [who, s] of [["giảng viên", teacher], ["chủ nhiệm môn", manager]]) {
  const r = await s.get(L);
  check(R, `BR-U02-07 ${who} không xem được audit`, r.status === 403 || r.status === 404, r.status);
}
const anon = await new Session("anon").get(L);
check(R, "Chưa đăng nhập bị từ chối", anon.status === 401, anon.status);

const byActor = await admin.get(`${L}?actorId=${seed.accounts.teacher.id}&size=100`);
check(R, "BR-U02-08 lọc theo người thực hiện", byActor.status === 200 && (byActor.data?.items ?? []).length > 0 && byActor.data.items.every((i) => i.actorId === seed.accounts.teacher.id), `n=${byActor.data?.items?.length}`);
check(R, "BR-U02-09 hiện email người thực hiện", (byActor.data?.items ?? []).some((i) => i.actorEmail === seed.accounts.teacher.email), JSON.stringify(byActor.data?.items?.[0] ?? {}).slice(0, 200));
const actions = [...new Set(items.map((i) => i.action))];
const act = actions.find((a) => a === "MCP_CREDENTIAL_CREATED") ?? actions[0];
const byAction = await admin.get(`${L}?action=${act}&size=100`);
check(R, "BR-U02-08 lọc theo action", byAction.status === 200 && (byAction.data?.items ?? []).every((i) => i.action === act), `${act} n=${byAction.data?.items?.length}`);
const denied = await admin.get(`${L}?result=DENIED&size=100`);
check(R, "BR-U02-02 lần bị từ chối cũng được ghi (DENIED)", denied.status === 200 && (denied.data?.items ?? []).length > 0, `n=${denied.data?.items?.length} actions=${[...new Set((denied.data?.items ?? []).map((i) => i.action))].slice(0, 5)}`);
const range = await admin.get(`${L}?from=2026-01-01T00:00:00Z&to=2026-01-02T00:00:00Z`);
check(R, "BR-U02-08 lọc khoảng thời gian (khoảng trống)", range.status === 200 && (range.data?.items ?? []).length === 0, `n=${range.data?.items?.length}`);
const badRange = await admin.get(`${L}?from=2026-10-10T00:00:00Z&to=2026-10-01T00:00:00Z`);
check(R, "Khoảng thời gian ngược bị báo lỗi", badRange.status === 400, badRange.status);
const viewed = await admin.get(`${L}?action=AUDIT_QUERIED&size=5`);
const anyView = (viewed.data?.items ?? []).length > 0 || items.some((i) => /AUDIT/.test(i.action));
check(R, "BR-U02-07 mọi lần tra cứu cũng được audit", anyView, `actions có AUDIT: ${actions.filter((a) => /AUDIT/.test(a))}`);
const text = JSON.stringify((await admin.get(`${L}?size=100`)).data);
check(R, "BR-U02-05 details không chứa mật khẩu/OTP/token", !/"(password|otp|token|secret|phone)"\s*:/i.test(text) && !text.includes(PW), "");
const del = await admin.call("DELETE", `${L}/${items[0]?.id}`);
const put = await admin.call("PUT", `${L}/${items[0]?.id}`, {});
check(R, "BR-U02-01 không có API sửa/xóa audit", [404, 405].includes(del.status) && [404, 405].includes(put.status), `DELETE=${del.status} PUT=${put.status}`);
const mcp = items.filter((i) => /^MCP_|^AI_|^QUOTA|^GROUP|^LEADER/.test(i.action)).map((i) => i.action);
check(R, "U06/U12 ghi audit qua U02", mcp.length > 0, [...new Set(mcp)].join(", "));

fs.writeFileSync(new URL("./u02-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU02: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
