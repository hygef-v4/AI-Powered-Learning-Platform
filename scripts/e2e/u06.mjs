// Kiểm thử end-to-end U06 qua API trên stack local. Ca có mã U06-xx theo kế hoạch kiểm thử của nhóm.
import fs from "node:fs";
import { BASE, Session, check, env } from "./lib.mjs";

const seed = JSON.parse(fs.readFileSync(new URL("./seed.json", import.meta.url)));
const PW = env("TEST_ACCOUNT_PASSWORD");
const R = [];
const admin = new Session("admin");
await admin.login(env("U01_BOOTSTRAP_ADMIN_EMAIL"), PW);
const teacher = new Session("teacher");
await teacher.login(seed.accounts.teacher.email, PW);
const student = new Session("sv5");
await student.login(seed.accounts.sv5.email, PW);

// --- Nguồn AI trước khi Admin đặt hạn mức chung (U06-16), sinh viên chỉ có API cá nhân (U06-11)
await admin.put("/api/v1/admin/ai/quota-policy", { defaultDayTokens: null, globalDayTokens: null, version: (await admin.get("/api/v1/admin/ai/quota-policy")).data?.version ?? 0 }).catch(() => {});
let src = await teacher.get("/api/v1/ai/sources");
const shared = (src.data || []).find((s) => s.source === "SHARED");
check(R, "U06-16 chưa đặt hạn mức chung", src.status === 200 && shared && shared.available === false && shared.reason, `available=${shared?.available} reason=${shared?.reason}`);
const sSrc = await student.get("/api/v1/ai/sources");
check(R, "U06-11 sinh viên chỉ có API cá nhân", sSrc.status === 200 && (sSrc.data || []).every((s) => s.source === "PERSONAL"), JSON.stringify((sSrc.data || []).map((s) => s.source + ":" + s.provider)));

// --- Khóa cá nhân sai (U06-02), mỗi nhà cung cấp một khóa (U06-13 – kiểm qua API: PUT thay, không thêm)
const bad = await teacher.put("/api/v1/me/ai-connections/GEMINI", { apiKey: "AIza-khoa-thu-sai-0000000000000000000", model: null, version: null });
check(R, "U06-02 khóa sai bị báo lỗi, không lưu", bad.status >= 400, `${bad.status} ${bad.data?.code ?? ""} ${bad.data?.detail ?? ""}`);
const conns = await teacher.get("/api/v1/me/ai-connections");
const gem = (conns.data || []).find?.((c) => c.provider === "GEMINI");
check(R, "U06-02 không lộ khóa trong danh sách", conns.status === 200 && !JSON.stringify(conns.data).includes("khoa-thu-sai"), `GEMINI=${JSON.stringify(gem ?? null)}`);
const providers = await teacher.get("/api/v1/ai/providers");
check(R, "U06-15 có Gemini, OpenAI, Claude", ["GEMINI", "OPENAI", "CLAUDE"].every((p) => JSON.stringify(providers.data).includes(p)), providers.status);

// --- Admin: hạn mức chung và mức riêng 0 (U06-09), không phải admin bị chặn
const pol = await admin.get("/api/v1/admin/ai/quota-policy");
const setPol = await admin.put("/api/v1/admin/ai/quota-policy", { defaultDayTokens: 50000, globalDayTokens: 1000000, version: pol.data?.version ?? 0 });
check(R, "U06-09 đặt hạn mức chung", setPol.status === 200, `${setPol.status} ${JSON.stringify(setPol.data).slice(0, 120)}`);
const zero = await admin.put(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`, { dayTokens: 0 });
check(R, "U06-09 mức riêng 0 cho giảng viên", zero.status === 200 || zero.status === 204, `${zero.status} ${JSON.stringify(zero.data).slice(0, 120)}`);
src = await teacher.get("/api/v1/ai/sources");
const shared0 = (src.data || []).find((s) => s.source === "SHARED");
check(R, "U06-09 API trường mờ kèm lý do với mức 0", shared0 && shared0.available === false && shared0.reason, `reason=${shared0?.reason}`);
await admin.del(`/api/v1/admin/ai/teacher-quotas/${seed.accounts.teacher.id}`);
src = await teacher.get("/api/v1/ai/sources");
const shared1 = (src.data || []).find((s) => s.source === "SHARED");
check(R, "U06-11 giảng viên thấy hạn mức còn x / y", shared1 && shared1.quota && shared1.quota.limit === 50000, `quota=${JSON.stringify(shared1?.quota)} available=${shared1?.available} reason=${shared1?.reason}`);
const deny = await teacher.get("/api/v1/admin/ai/quota-policy");
check(R, "Phân quyền: giảng viên không vào API admin", deny.status === 403 || deny.status === 404, deny.status);
const summary = await admin.get("/api/v1/admin/ai/usage-summary?days=7");
check(R, "U06-10 tab Usage của admin", summary.status === 200, `${summary.status} ${JSON.stringify(summary.data).slice(0, 160)}`);
const adminBad = await admin.put("/api/v1/admin/ai/connections/GEMINI", { apiKey: "AIza-khoa-truong-sai-000000000000000000", model: null, version: null });
check(R, "U06-07 khóa trường sai bị báo lỗi", adminBad.status >= 400, `${adminBad.status} ${adminBad.data?.code ?? ""}`);

// --- Lịch sử dùng AI rỗng (U06-21)
const hist = await teacher.get(`/api/v1/me/ai-usage?from=2026-01-01T00:00:00Z&to=2026-01-08T00:00:00Z`);
check(R, "U06-21 lịch sử khoảng chưa dùng: rỗng, không lỗi", hist.status === 200 && (hist.data?.items ?? []).length === 0, `${hist.status} ${JSON.stringify(hist.data).slice(0, 160)}`);
const histSrc = await teacher.get(`/api/v1/me/ai-usage?source=MCP`);
check(R, "U06-06 lọc theo nguồn", histSrc.status === 200, histSrc.status);

// --- Agent MCP: tạo 5, chặn thứ 6, token chỉ hiện một lần (U06-04, 05)
for (const c of (await teacher.get("/api/v1/me/mcp-credentials")).data || []) if (c.status === "ACTIVE") await teacher.del(`/api/v1/me/mcp-credentials/${c.id}`);
const created = [];
for (let i = 1; i <= 5; i++) {
  const r = await teacher.post("/api/v1/me/mcp-credentials", { label: `agent thử ${i}`, access: i === 1 ? "READ_ONLY" : "READ_WRITE", days: 30 });
  created.push(r);
}
check(R, "U06-04 tạo token trả token một lần", created.every((r) => r.status === 201 && r.data?.token), created.map((r) => r.status).join(","));
const sixth = await teacher.post("/api/v1/me/mcp-credentials", { label: "agent thứ 6", access: "READ_ONLY", days: 30 });
check(R, "U06-05 token thứ 6 bị chặn kèm lý do", sixth.status >= 400, `${sixth.status} ${sixth.data?.code} ${sixth.data?.detail ?? ""}`);
const list = await teacher.get("/api/v1/me/mcp-credentials");
const tokenRO = created[0].data.token;
check(R, "U06-04 danh sách chỉ có 4 ký tự cuối", !JSON.stringify(list.data).includes(tokenRO) && (list.data || []).every((c) => (c.tokenHint || "").length === 4), JSON.stringify((list.data || []).slice(0, 1)));

// --- MCP: quyền chỉ đọc, không có công cụ nộp hay công bố điểm (U06-14)
let rpcId = 1;
const mcp = async (token, method, params = {}) => {
  const res = await fetch(BASE + "/mcp", { method: "POST", headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json", Accept: "application/json, text/event-stream" }, body: JSON.stringify({ jsonrpc: "2.0", id: rpcId++, method, params }) });
  const text = await res.text();
  let data = null;
  try { data = JSON.parse(text); } catch { data = text; }
  return { status: res.status, data };
};
const init = await mcp(tokenRO, "initialize", { protocolVersion: "2025-06-18", capabilities: {}, clientInfo: { name: "e2e", version: "1" } });
check(R, "U06-14 initialize với token", init.status === 200 && init.data?.result, `${init.status} ${JSON.stringify(init.data).slice(0, 120)}`);
const toolsRO = await mcp(tokenRO, "tools/list");
const namesRO = (toolsRO.data?.result?.tools || []).map((t) => t.name);
const toolsRW = await mcp(created[1].data.token, "tools/list");
const namesRW = (toolsRW.data?.result?.tools || []).map((t) => t.name);
check(R, "U06-14 không có công cụ nộp bài/công bố điểm", ![...namesRO, ...namesRW].some((n) => /submit|publish/.test(n)), `RO=[${namesRO}] RW=[${namesRW}]`);
const writeRO = await mcp(tokenRO, "tools/call", { name: "save_assignment_draft", arguments: {} });
const writeDenied = writeRO.data?.error || writeRO.data?.result?.isError;
check(R, "U06-14 công cụ ghi bị từ chối với token Chỉ đọc", writeDenied, JSON.stringify(writeRO.data).slice(0, 200));
const readCall = await mcp(tokenRO, "tools/call", { name: namesRO.find((n) => n.startsWith("read_")) ?? "read_assignment_context", arguments: {} });
check(R, "U06-14 công cụ đọc gọi được (lỗi tham số là của công cụ, không phải quyền)", readCall.status === 200 && !/permission|quyền|READ_ONLY/i.test(JSON.stringify(readCall.data?.error ?? "")), JSON.stringify(readCall.data).slice(0, 200));
const noToken = await mcp("sai-token", "tools/list");
check(R, "U06-14 token sai bị 401", noToken.status === 401, noToken.status);

// đổi mật khẩu không làm token mất hiệu lực
const PW2 = PW + "x";
const ch = await teacher.post("/api/v1/me/password", { currentPassword: PW, newPassword: PW2 });
const afterPw = await mcp(tokenRO, "tools/list");
const back = new Session("teacher2");
await back.login(seed.accounts.teacher.email, PW2);
await back.post("/api/v1/me/password", { currentPassword: PW2, newPassword: PW });
check(R, "U06-14 đổi mật khẩu không làm token mất hiệu lực", (ch.status === 200 || ch.status === 204) && afterPw.status === 200 && afterPw.data?.result, `change=${ch.status} after=${afterPw.status}`);

// thu hồi token: agent bị từ chối ngay (U06-05)
const t2 = await new Session("teacher3");
await t2.login(seed.accounts.teacher.email, PW);
const rev = await t2.del(`/api/v1/me/mcp-credentials/${created[2].data.credential.id}`);
const afterRev = await mcp(created[2].data.token, "tools/list");
const list2 = await t2.get("/api/v1/me/mcp-credentials");
const revoked = (list2.data || []).find((c) => c.id === created[2].data.credential.id);
check(R, "U06-05 thu hồi: Đã thu hồi, agent bị từ chối", (rev.status === 204 || rev.status === 200) && afterRev.status === 401 && revoked?.status === "REVOKED", `rev=${rev.status} mcp=${afterRev.status} status=${revoked?.status}`);
const studentMcp = await student.post("/api/v1/me/mcp-credentials", { label: "sv agent", access: "READ_ONLY", days: 7 });
check(R, "U06-01 sinh viên cũng tạo được agent MCP", studentMcp.status === 201, studentMcp.status);
if (studentMcp.data?.credential) await student.del(`/api/v1/me/mcp-credentials/${studentMcp.data.credential.id}`);
for (const c of created) if (c.data?.credential) await t2.del(`/api/v1/me/mcp-credentials/${c.data.credential.id}`);

fs.writeFileSync(new URL("./u06-result.json", import.meta.url), JSON.stringify(R, null, 2));
console.log(`\nU06: ${R.filter((r) => r.ok).length}/${R.length} đạt`);
