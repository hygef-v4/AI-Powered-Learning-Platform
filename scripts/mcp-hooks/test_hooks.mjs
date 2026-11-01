import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const tool = "mcp_nen_tang_hoc_tap_record_grading_proposal";
const id = "00000000-0000-4000-8000-000000000001";
const proposal = { checks: {}, score: "0", itemScoresByRubric: { [id]: { [id]: 0 } },
  structuredFeedback: { schemaVersion: 1, summary: { strengths: [], improvements: [], nextSteps: [] },
    items: [{ rubricId: id, itemId: id, evidence: "No answer", reason: "Required work is absent", requiresReview: false }] } };
const args = { submissionRef: { kind: "ATTEMPT", id, classId: id, assignmentId: id },
  submissionHash: "hash", snapshotFingerprint: "a".repeat(64), expectedVersion: 0, requestRef: "retry-same", proposal };
function run(script, input) {
  const result = spawnSync(process.execPath, [fileURLToPath(new URL(script, import.meta.url))],
    { input: typeof input === "string" ? input : JSON.stringify(input), encoding: "utf8" });
  assert.equal(result.status, 0, result.stderr);
  return JSON.parse(result.stdout);
}
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: args }).decision, "allow");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, proposal: { checks: {}, score: "0" } } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, proposal: { ...proposal, score: "1" } } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, requestRef: "" } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, snapshotFingerprint: "" } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, proposal: { ...proposal, structuredFeedback: null } } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool, tool_input: { ...args, submissionHash: "x".repeat(65536) } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: tool + "s_batch", tool_input: { items: [args, args] } }).decision, "deny");
assert.equal(run("validate-proposal.mjs", { tool_name: "another_server_record_grading_proposal" }).decision, "allow");
assert.equal(run("validate-proposal.mjs", "{bad").decision, "deny");
const output = run("summarize-result.mjs", { tool_name: tool, tool_response: { llmContent: [{ text: JSON.stringify({ items: [
  { index: 0, outcome: "OK", result: { proposalId: id } },
  { index: 1, outcome: "ERROR", code: "SECRET_TOKEN_DO_NOT_ECHO", message: "private student text" }], partial: true }) }] } });
assert.match(output.hookSpecificOutput.additionalContext, /1 OK.*1 ERROR/);
assert.doesNotMatch(JSON.stringify(output), /SECRET_TOKEN|private student/);
assert.match(run("summarize-result.mjs", { tool_name: tool, tool_response: { error: "secret" } })
  .hookSpecificOutput.additionalContext, /ERROR/);
assert.deepEqual(run("summarize-result.mjs", "{bad"), {});
console.log("MCP hook checks passed (14 scenarios).");
