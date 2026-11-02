# Dedicated MCP documentation — results

Implemented on fix/u05-u09-u15 after merge commit 047c2e5c. User requested a separate MCP documentation page linked from AI Connections, explaining connection, usage, features and every tool.

## Delivered

- Authenticated /ai/mcp-guide inherits Student/Teacher/Subject Manager access. The Agent MCP tab now has compact help and a documentation link, still available during credential loading/errors. Token management and explicit token dialogs remain.
- Six navigable sections: overview/permissions, four client connections, workflows, 29 grouped tool descriptions, troubleshooting/retries and official references. Includes proposal/apply distinction, rubric evidence/review limits, Practice feedback, no Admin MCP, disconnect/revoke and server-managed workflows without mandatory skill downloads.
- Reuses native config formats and English setup prompt, with dynamic origin and no real token on the docs page. Antigravity instructions include current app menus and older-version fallback. Non-Codex clients are marked not practically verified in this environment (BR-U06-50).
- SSR uses a stable relative MCP endpoint before hydrating to the browser origin; same shared setup component remains usable in explicit-token dialogs.
- U06 business rules/components/as-built and execution plan updated. No backend/API/DB/schema changes, new dependencies or MCP business-data writes.

## Verification

- Red/green: old inline guide failed the new separate-link assertion; SSR endpoint test failed before the useSyncExternalStore correction, then passed.
- Fresh AI/MCP suite: 39 tests / 7 files passed. Tests cover doc navigation/anchors, context/role/proposal guidance, native config formats, safe prompt/clipboard handling, public token absence, SSR and link visibility when credential loading fails.
- Full frontend execution: 727 tests, 726 passed and one failed because a newly added verification note was duplicated. Removed the duplicate and reran all 39 AI/MCP tests successfully. No complete full-suite rerun after that localized correction; unrelated cases passed in the full execution.
- TypeScript and focused ESLint on every changed source/test passed. Final production build passed with installed Next 16.4.3, isolated .next-build-mcp-docs, CIRCLE_NODE_TOTAL=2 and bounded Node heap. Restored Next-generated tsconfig/AGENTS changes afterward.
- Source cross-check found 29 documented names, 29 non-Admin provider names, no missing/unknown/duplicate tools. Grading context continuation, write restrictions and roles reviewed against backend code.
- Independent reviewer: no Critical/Important findings in auth, SSR, setup prompts, tool semantics, proposal/apply or retries. External client installations not independently exercised.
- Real browser: connection-tab link opens docs and returns to the MCP tab; long setup form is absent from the management page. Headless Chrome with the supplied test teacher inspected desktop 1440x1000 and mobile 390x844, light/dark, 29 tools and OpenCode configuration. All measured scroll widths equal viewport widths; four final screenshots inspected. Connected-browser screenshot calls timed out, so cached Playwright/installed Chrome were used without Computer Use.
- Synced node_modules with the already-merged package-lock (no manifest/lock changes). Initial npm ci hit Windows native-file locks; stopped only the confirmed project frontend/CSS workers, completed npm ci, and restarted frontend on port 3000 in a hidden process. Backend and open terminal windows retained. Existing Node 25.8.1 produced dependency engine warnings; checks above nevertheless passed.

Ignored local logs/screenshots: .tmp/mcp-docs-tests-final.log, .tmp/mcp-docs-full-tests.log, .tmp/mcp-docs-types-final.log, .tmp/mcp-docs-lint.log, .tmp/mcp-docs-build-final.log, .tmp/mcp-docs-browser-final.log, and .tmp/mcp-docs-*.png. Browser login used a local test account; no token, assignment, proposal or grade created by this change.

## Sources and limits

Current backend registry/provider/workflow source is authoritative for tool behavior. Client setup checked against [OpenAI](https://developers.openai.com/codex/mcp), [Claude Code](https://code.claude.com/docs/en/mcp), [OpenCode MCP](https://opencode.ai/docs/mcp-servers/), [OpenCode config](https://opencode.ai/docs/config/) and [Google Antigravity](https://codelabs.developers.google.com/getting-started-google-antigravity#6) on 2026-11-02.

No live credential/assignment/grade write, backend restart, migration, commit, push or deployment. Backend tests/load/real third-party client connections not rerun for this frontend-only documentation feature. No claim that schema/context checks guarantee AI grading quality. Existing security/resiliency scope retained; PBT disabled.
