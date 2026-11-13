import { readEvent, isPlatformTool } from "./validate-proposal.mjs";

function summarize(event) {
  if (!isPlatformTool(event?.tool_name)) return {};
  let result;
  try {
    const content = event.tool_response?.llmContent;
    result = typeof content === "string" ? JSON.parse(content)
      : JSON.parse(content.find(part => typeof part.text === "string").text);
    if (result.content) result = JSON.parse(result.content.find(part => part.type === "text").text);
  } catch {
    return { hookSpecificOutput: { additionalContext: "MCP result ERROR or unavailable. Do not claim success; reconcile unchanged requestRef before retry." } };
  }
  const rows = Array.isArray(result.items) ? result.items : [result];
  let ok = 0;
  let errors = 0;
  for (const row of rows) {
    if (row.outcome === "OK" || (!row.outcome && typeof row.proposalId === "string" && row.status === "READY")) ok++;
    else errors++;
  }
  return { hookSpecificOutput: { additionalContext:
    `MCP proposals: ${ok} OK; ${errors} ERROR. Inspect original per-item outcomes. Keep successful items; retry only eligible failures with unchanged requestRef/payload. No official grades were published.` } };
}

let output;
try { output = summarize(readEvent()); }
catch { output = {}; }
process.stdout.write(JSON.stringify(output));
