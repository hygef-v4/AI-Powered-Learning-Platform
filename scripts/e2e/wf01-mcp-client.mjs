import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { Session } from "./lib.mjs";

const [command, directory, proposalPath] = process.argv.slice(2);
assert.ok(command === "read" || (command === "record" && proposalPath), "Use read or record DIRECTORY [PROPOSAL]");
const folder = path.resolve(directory);
assert.ok(folder.startsWith(path.resolve(".tmp/systemtest") + path.sep), "Use a local system test fixture");
const fixture = JSON.parse(fs.readFileSync(path.join(folder, "fixture.json"), "utf8"));
assert.equal(process.env.E2E_BASE_URL, fixture.base);
assert.ok(["localhost", "127.0.0.1"].includes(new URL(fixture.base).hostname));
const { password } = JSON.parse(fs.readFileSync(path.join(folder, "test-login.private.json"), "utf8"));
const contextPath = path.join(folder, "mcp-context.json");

async function runMcpClient() {
  const teacher = new Session("PE teacher");
  const login = await teacher.post("/api/v1/auth/login", { email: fixture.accounts.teacher.email, password });
  assert.equal(login.status, 200, "Test teacher sign-in failed");
  const created = await teacher.post("/api/v1/me/mcp-credentials", {
    label: `PE external agent ${command}`, access: "READ_WRITE", days: 7,
  });
  assert.equal(created.status, 201, "Could not create a test MCP credential");
  const { token } = created.data;
  assert.ok(token, "Test MCP token was unavailable");

  async function callRpc(method, params) {
    const response = await fetch(`${fixture.base}/mcp`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ jsonrpc: "2.0", id: randomUUID(), method, params }),
    });
    assert.ok(response.ok, `MCP transport HTTP ${response.status}`);
    const rpc = await response.json();
    assert.ok(!rpc.error && rpc.result, `MCP method ${method} failed`);
    return rpc.result;
  }

  async function callTool(name, args) {
    const result = await callRpc("tools/call", { name, arguments: args });
    const raw = result.content?.find((item) => item.type === "text")?.text;
    let body;
    try {
      body = JSON.parse(raw);
    } catch {
      throw new Error(`${name} did not return JSON`);
    }
    const code = typeof body.code === "string" && /^[A-Z0-9_]+$/.test(body.code) ? body.code : "TOOL_ERROR";
    const detail = typeof body.message === "string" && /^[\p{L}\p{N} .:'_-]{1,160}$/u.test(body.message)
      ? ` (${body.message})` : "";
    if (result.isError) throw new Error(`${name} failed: ${code}${detail}`);
    return body;
  }

  await callRpc("initialize", {
    protocolVersion: "2025-03-26", capabilities: {}, clientInfo: { name: "PE external system test", version: "1" },
  });
  if (command === "read") {
    const target = fixture.individual.MCP[0];
    let cursor = { evaluationId: target.evaluationId };
    let context;
    const documentBlocks = [];
    for (let page = 0; page < 15; page++) {
      const part = await callTool("read_grading_context", cursor);
      context ??= part;
      assert.equal(context.snapshotFingerprint, part.snapshotFingerprint);
      documentBlocks.push(...part.documentBlocks);
      if (part.complete) {
        assert.equal(documentBlocks.length, part.totalBlocks);
        fs.writeFileSync(contextPath, JSON.stringify({ ...part, documentBlocks }, null, 2));
        console.log(`MCP read complete: ${documentBlocks.length} blocks. Context: ${contextPath}`);
        return;
      }
      cursor = part.continuation;
    }
    throw new Error("Grading context did not complete within 15 test pages");
  }

  const { submissionRef, submissionHash, expectedVersion, snapshotFingerprint } = JSON.parse(fs.readFileSync(contextPath, "utf8"));
  const proposal = JSON.parse(fs.readFileSync(proposalPath, "utf8"));
  const recorded = await callTool("record_grading_proposal", {
    submissionRef, submissionHash, expectedVersion, snapshotFingerprint, proposal,
    requestRef: `pe-grading-${fixture.runId}`, agentLabel: "Orca GPT Luna system test",
  });
  assert.equal(recorded.status, "READY");
  assert.match(recorded.reviewUrl, /^\/teaching\/evaluations\//);
  fs.writeFileSync(path.join(folder, "mcp-recorded.json"), JSON.stringify(recorded, null, 2));
  console.log(`MCP proposal ready: ${recorded.reviewUrl}`);
}

runMcpClient().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
