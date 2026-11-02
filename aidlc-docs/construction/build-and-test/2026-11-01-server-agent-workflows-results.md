# Server-managed MCP workflows — results

Date: 2026-11-01. Baseline: mcp-test at e54359e4; working tree initially clean. [Requirements/stories](../../inception/requirements/2026-11-01-server-agent-workflows.md), [implementation plan](../plans/code-generation-plan/2026-11-01-server-agent-workflows.md).

## Implemented behavior

`read_agent_workflow` serves the complete AUTHOR_ASSIGNMENT or GRADE_WITH_RUBRIC instructions from packaged classpath resources, with workflow name and content-hash version. The full source is under backend/src/main/resources/agent-workflows. The old repository-local skill paths are optional short wrappers; no full duplicate or required client download remains.

Capabilities and relevant authoring/grading descriptions point to the reader. Assignment requirements route to AUTHOR_ASSIGNMENT, while QUIZ/BANK retain separate schema and clarification guidance. Existing gradingInstructions and all authorization, fingerprint/read-progress/rubric/idempotency guards remain unchanged. Teacher/Subject Manager can read with either token access level; Student/Admin cannot discover or call educator workflows. Reading does not grant write access.

Current user guides and U06/U07/U09 AI-DLC rules are synchronized. No frontend behavior change, new dependency, migration, user configuration installation or write-tool test.

## Evidence

| Verification | Result |
|---|---|
| Catalog regression RED | Staff catalog lacked read_agent_workflow before implementation; observed assertion failure. |
| Backend focused/architecture | 39 tests in 6 suites passed, zero failures/errors/skips: McpAgentWorkflowToolsTest, McpStaffCatalogTest, McpServerTest, AssignmentMcpAuthoringServiceTest, GradingMcpQueryServiceTest and ArchitectureTest. |
| New provider coverage | Complete canonical instructions, stable/change-sensitive versions, fixed allowlist/path rejection, missing arguments, teaching-role guards, read-only protocol access, ASSIGNMENT vs QUIZ/BANK routing and thin wrappers. |
| Optional hook self-check | Existing 14 dummy-data scenarios passed; no hook port or client installation added. |
| Packaged backend | Standard Maven package passed after stopping the old local backend. Inspected jar entries for both full workflows, application.yml and existing AI migration. No Maven/Docker build configuration change needed. |
| Authenticated MCP | Existing Codex connection get_my_capabilities succeeds and advertises the new reader. Direct JSON-RPC initialize/tools-list and both workflow reads succeed at localhost:3000/mcp using the existing configured credential. No local skill file was read during the runtime acceptance. |
| Served content | AUTHOR_ASSIGNMENT: 3,609 characters, version df1cc525095ee918a61789b6f79d9b2bf0363e8d8f043281b34e9973535ca905. GRADE_WITH_RUBRIC: 4,828 characters, version 566b9b8e22c6d5bc879081add5c06c8366d474ba746af4f2d4c1333a5f6f653f. |
| Source/skill review | Fresh read-only reviewer checked baseline full local copies, updated wrapper dry-runs, role guard, classpath resources, Docker context and routing. Generic QUIZ/BANK discovery was retained after a review note; no remaining blocking issue found. |

## Rollout and limits

### User-requested authoring trial after implementation

The user subsequently requested another test assignment for SE_JS. The external agent fetched AUTHOR_ASSIGNMENT from the server through authenticated MCP JSON-RPC, discovered the authorized class/current schema and empty class-material list, and created one new DRAFT with a READY proposal: meeting-room booking design, 4 questions, 10 points and 10 rubric items. The tool returned reviewUrl successfully. No local skill file was used; no automatic application/publication occurred. This separately authorized trial verifies draft creation through the server-workflow path, not educational quality or native acceptance in every client.

Backend reloaded in its existing Orca terminal. Clients holding the old tool catalog need a catalog refresh or a new agent session to expose read_agent_workflow natively. The existing session's capability call sees the tool, but its cached callable-tool metadata did not yet contain the new name; runtime workflow reads were verified through direct MCP JSON-RPC, not claimed as a native call from all clients.

Full repository/frontend suites and a Docker image build were not rerun: no frontend or build-context changes. Docker compatibility is supported by classpath packaging and existing COPY src, not by a fresh image execution. Implementation verification performed no provider invocation, assignment/grade mutation, real credential creation, private configuration change or global installation; the later explicitly requested draft trial is recorded above. No official grade write/publication, commit or push was part of implementation verification. Model obedience and grading accuracy still require separate live-client and teacher-reference evaluation. Backend safeguards remain authoritative regardless of workflow reading.

Existing security/resiliency scope retained; PBT disabled. Local diagnostic artifacts remain ignored under .tmp.
