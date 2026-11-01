# Server-managed MCP workflows implementation plan

> Use superpowers:executing-plans inline. User authorized planning and implementation; no second approval checkpoint or automatic commit.

**Goal:** Let an external agent fetch complete authoring/grading instructions from the platform without local skill installation.

**Architecture:** One canonical full SKILL.md per workflow in backend classpath resources; a small role-guarded read-only provider maps two fixed enum values to those resources and returns a content hash. Existing optional local skills become wrappers. No filesystem lookup outside the packaged backend at runtime.

**Tech stack:** Java 17/Spring resource loading, existing MCP registry/schema/hash helpers and JUnit/Mockito. No new dependencies or build-context changes.

**Spec:** [Requirements/stories](../../../inception/requirements/2026-11-01-server-agent-workflows.md).

## Review focus

Unauthorized discovery/direct calls, path traversal/unknown workflow names, missing packaged resources, stale content versions, accidental QUIZ/BANK routing and resource availability outside the repository working directory. Existing writes must retain their guards; reading a workflow is not evidence of model compliance.

### Task 1 — serve and discover complete workflows

Files: backend/src/main/resources/agent-workflows/{author-assignment,grade-with-rubric}/SKILL.md; ai/service/McpAgentWorkflowTools.java (new); McpCapabilitiesTools.java, McpAuthoringRequirementsTools.java; assignment/grading tool descriptions; backend MCP tests.

- [x] Add catalog regression for read_agent_workflow and observe RED before implementation.
- [x] Package the existing full instructions as canonical classpath resources. Implement fixed enum mapping, teaching-role guard, complete UTF-8 content and deterministic version. Fail closed when resource loading fails.
- [x] Direct capabilities and appropriate domain descriptions to the workflow; keep Student guidance and QUIZ/BANK routes separate.
- [x] Test both workflows, full instruction coverage, stable/change-sensitive version, invalid arguments, roles and read-only catalog/protocol access.

### Task 2 — optional wrappers and documentation

Files: agent-skills/*/SKILL.md; docs/guides/mcp-agent-workflows.md, mcp-clients.md; current U06/U07/U09 business rules; AI-DLC state/audit/results.

- [x] Replace local full copies with short wrappers that call read_agent_workflow and follow returned instructions; fail clearly if the server/tool is unavailable. No automatic download/install step.
- [x] Document server-first discovery, exact tool calls, content version semantics and optional legacy hooks. Synchronize current AI-DLC rules and preserve historical dated evidence.
- [x] Review wrapper instructions against canonical resources to prevent recursion or fallback to stale copied workflows.

### Task 3 — verify and reload local backend

- [x] Run focused MCP/role/protocol tests and architecture checks; run wrapper/hook checks where affected.
- [x] Stop only the local backend after tests, package the standard jar, inspect both resource entries, and verify that default application/migration resources remain present (see execution ruling).
- [x] Reload only the existing local backend terminal after a successful package. Test initialize/capabilities, tools/list and both workflow reads with the existing configured credential, without exposing it or calling write tools/providers.
- [x] Record evidence, source review and remaining all-client/model-quality acceptance limits. No commit or push.

Ruling: full source lives under backend/src/main/resources because Docker's context is backend. Avoid Maven resources pointing outside that context or maintaining two full copies.

Execution ruling: keep build configuration unchanged. Run tests first, then briefly stop only the local backend before standard packaging/restart, so Maven never overwrites the running jar. A new classifier/build profile solely to avoid a few seconds of local downtime is unnecessary. No Docker image build is claimed from inspecting Docker COPY src and packaged resource entries.

Completion: implemented and verified; [results and remaining acceptance](../../build-and-test/2026-11-01-server-agent-workflows-results.md). No commit/push or private configuration changes.
