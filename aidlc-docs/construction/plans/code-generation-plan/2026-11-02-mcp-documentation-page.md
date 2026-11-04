# Dedicated MCP documentation page

User request: move the inline MCP instructions into a separate documentation page, reachable by a link/button, covering connection, usage, features and each tool. This is an extension of the existing AI Connections feature, not a new design system.

Design: authenticated /ai/mcp-guide under the existing Student/Teacher/Subject Manager layout. Read mode; incumbent white/slate surfaces, muted blue controls, current type and house components. Compact help link remains visible on Agent MCP even if credential loading fails. The document has a narrow sticky contents rail on desktop, a flowing contents list on mobile, readable prose and grouped tool definitions with wrapping identifiers. No new dependencies, DB/API changes or token fetching on the docs page.

- [x] Step 1: Add a route/page and a small source-aligned catalog of all non-Admin MCP tools, with permission/effect notes. Explain agent conversation vs platform review/apply, role/token differences, rubric limitations and safe retries.
- [x] Step 2: Reuse McpClientSetup for Codex, Claude Code, OpenCode and Antigravity. Retain the English copy prompt, dynamic endpoint, placeholder-only public examples and explicit token-dialog behavior. Provide app-first steps and links to official documentation; no terminal command tutorial or Gemini CLI option.
- [x] Step 3: Replace inline guide with a compact persistent documentation link; preserve token management and links back to the MCP tab.
- [x] Step 4: Test navigation, docs contents/anchors, setup prompt/clipboard behavior, credential-error access and tool-catalog coverage against actual provider names. TypeScript/ESLint/final production build passed; 39 focused AI/MCP tests passed after the localized correction found by the full 727-case execution. Desktop/mobile light/dark renders inspected, zero horizontal overflow. [Evidence and limits](../../build-and-test/2026-11-02-mcp-documentation-results.md).

Content sources: current backend MCP provider registry/workflow resources and official OpenAI, Claude Code, OpenCode and Google Antigravity documentation checked 2026-11-02. Document actual scopes: Admin has no MCP access; student has own submitted Practice feedback only; teacher/manager author proposals; question bank requires subject manager. Discovery remains authoritative for currently available tools.

Authorization: implement the page/link/content requested; no additional product choices needed. No commit, push, real token/grade write, backend restart or migration. Security/resiliency scope unchanged; PBT disabled.
