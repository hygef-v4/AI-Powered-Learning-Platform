import { readSync } from "node:fs";
import { fileURLToPath } from "node:url";

export function readEvent() {
  const chunks = [];
  const buffer = Buffer.alloc(65536);
  let size = 0;
  for (;;) {
    const length = readSync(0, buffer, 0, buffer.length, null);
    if (!length) break;
    size += length;
    if (size > 1024 * 1024) throw new Error("Input too large");
    chunks.push(Buffer.from(buffer.subarray(0, length)));
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

export const isPlatformTool = (name) => typeof name === "string"
  && /^mcp_nen[-_]tang[-_]hoc[-_]tap_+record_grading_proposal(s_batch)?$/.test(name);
const object = (value) => value !== null && typeof value === "object" && !Array.isArray(value);
const uuid = (value) => typeof value === "string" && /^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(value);
const text = (value) => typeof value === "string" && value.trim().length > 0;
function cents(value) {
  if (!/^(0|[1-9]\d*)(\.\d{1,2})?$/.test(String(value))) throw new Error();
  const [whole, fraction = ""] = String(value).split(".");
  const result = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  if (!Number.isSafeInteger(result)) throw new Error();
  return result;
}

export function validate(event) {
  if (!isPlatformTool(event?.tool_name)) return { decision: "allow" };
  try {
    const args = event.tool_input;
    if (!object(args) || Buffer.byteLength(JSON.stringify(args), "utf8") > 65536) throw new Error();
    const items = event.tool_name.endsWith("_batch") ? args.items : [args];
    if (!Array.isArray(items) || items.length < 1 || items.length > 20) throw new Error();
    const refs = new Set();
    for (const input of items) {
      if (!object(input) || !text(input.requestRef) || input.requestRef.length > 200 || refs.has(input.requestRef)
          || !Number.isSafeInteger(input.expectedVersion) || input.expectedVersion < 0 || !text(input.submissionHash)
          || typeof input.snapshotFingerprint !== "string" || !/^[0-9a-f]{64}$/.test(input.snapshotFingerprint)) throw new Error();
      refs.add(input.requestRef);
      const ref = input.submissionRef;
      if (!object(ref) || !["ATTEMPT", "GROUP_DOCUMENT"].includes(ref.kind)
          || ![ref.id, ref.classId, ref.assignmentId].every(uuid)) throw new Error();
      const p = input.proposal;
      if (!object(p) || !object(p.checks) || !object(p.itemScoresByRubric) || !object(p.structuredFeedback)) throw new Error();
      const expected = new Set();
      let total = 0;
      for (const [rubricId, scores] of Object.entries(p.itemScoresByRubric)) {
        if (!uuid(rubricId) || !object(scores)) throw new Error();
        for (const [itemId, score] of Object.entries(scores)) {
          if (!uuid(itemId) || typeof score !== "number") throw new Error();
          total += cents(score);
          expected.add(`${rubricId}/${itemId}`);
        }
      }
      if (cents(p.score) !== total) throw new Error();
      for (const [rubricId, checks] of Object.entries(p.checks)) {
        if (!uuid(rubricId) || !Array.isArray(checks) || new Set(checks).size !== checks.length
            || checks.some(id => !expected.has(`${rubricId}/${id}`))) throw new Error();
      }
      const feedback = p.structuredFeedback;
      if (feedback.schemaVersion !== 1 || !object(feedback.summary) || !Array.isArray(feedback.items)) throw new Error();
      for (const field of ["strengths", "improvements", "nextSteps"])
        if (!Array.isArray(feedback.summary[field])) throw new Error();
      const seen = new Set();
      for (const item of feedback.items) {
        const key = `${item?.rubricId}/${item?.itemId}`;
        if (!expected.has(key) || seen.has(key) || !text(item.reason) || typeof item.requiresReview !== "boolean"
            || (item.requiresReview && !text(item.reviewReason))
            || (!text(item.evidence) && !(item.requiresReview && text(item.reviewReason)))) throw new Error();
        seen.add(key);
      }
      if (seen.size !== expected.size) throw new Error();
    }
    return { decision: "allow" };
  } catch {
    return { decision: "deny", reason: "MCP proposal is incomplete or exceeds a limit. Check requestRef, snapshot reference, every item score, exact total and per-item evidence/review reason against read_grading_context.outputSchema. Backend validation remains authoritative." };
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  let result;
  try { result = validate(readEvent()); }
  catch { result = { decision: "deny", reason: "Invalid or oversized hook input; no proposal was sent." }; }
  process.stdout.write(JSON.stringify(result));
}
