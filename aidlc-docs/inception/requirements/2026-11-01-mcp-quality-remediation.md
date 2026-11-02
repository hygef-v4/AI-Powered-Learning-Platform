# MCP authoring and grading remediation — requirements

Status: implementation authorized and delivered for deterministic safeguards and setup guidance. Teacher-scored model pilot and new authenticated acceptance in each external client remain pending.

## Intent and baseline

The user requested an MCP/AI-DLC audit, asked whether skills/hooks would help, and now requests a repair plan. Improve connection guidance, assignment authoring, rubric grading and the evidence used to judge grading quality. The user authorized implementation and subsequently requested Codex, Claude Code, Antigravity and OpenCode support, with application/file-based setup instead of terminal instructions.

Observed baseline: Java 17/Spring Boot, Next.js/React/TypeScript; MCP Streamable HTTP at /mcp; external agents use their own models. Student/Teacher/Subject Manager may connect; Admin may not. Student is restricted to existing submitted-own-Practice feedback. Teacher/Manager create proposals, never official grades through MCP. Assume approximately 200 students per class and concurrent teachers; latency targets require measurement.

Source references: [client guide](../../../docs/guides/mcp-clients.md), [U09 rules](../../construction/u09-assignment-grading/functional-design/business-rules.md), [latest Student/Admin policy](2026-10-30-test-remediation-student-mcp.md). The 2026-10-26 staff expansion is historical where superseded by the 2026-10-30 policy.

## Proposed approach

Keep the existing tools and services. First repair server validation and common grading context, then connection guidance and discovery cost, then ship two optional agent skills and thin client hooks. Backend validation remains mandatory without any installed client package. Do not add a second grading framework or a provider call to MCP tools.

Alternative: documentation/skills only would be faster but leaves the validator hole. A new orchestrator with per-section grading, MCP Prompts and multiple reviewing agents adds contracts and operational cost; defer until the repaired flow is measured.

## Requirements and acceptance

| ID | Requirement | Acceptance |
|---|---|---|
| R01 | New AI proposals from MCP and API must have explicit scores and structured feedback for every locked rubric item. | Reject missing/null/duplicate/unknown items, negative/over-max/>2-decimal scores, incorrect totals, unknown evidence anchors and evidence-free items without a review reason. No READY proposal is inserted on rejection. |
| R02 | Preserve reading of existing checklist-only records and manual grading. | Existing proposal/history display, manual save, group propagation and teacher review/finalize/publish still work; no migration or stored-data rewrite. Old requestRef replay is tested separately from accepting a new incomplete proposal. |
| R03 | MCP and API share grading policy and assignment context. | MCP supplies assignment ID/version, instructions, skeleton and required diagrams with locked rubric, schema and paginated submission; all pages agree on fingerprint. Missing or changed context fails explicitly. Never include account names/emails or group contribution scores in the grading payload added by this work. |
| R04 | Connection setup must match the selected client. | Separate Codex, Claude Code, OpenCode and Antigravity instructions/config; Gemini retained for compatibility. Show personal config file, direct Bearer header, restart, capabilities check and common errors without requiring terminal commands. Format verification is distinct from authenticated client acceptance. Do not log/copy a real token into tracked files. |
| R05 | Current documentation has one policy entry point. | Mark superseded policy sections rather than rewriting approval history; reconcile contradictory verification and token-reveal statements. State client/version/date/scope of real verification. |
| R06 | Submission discovery must not load every submitted document. | A 200-submission metadata listing does not call detail/getSubmission/activity.summary per evaluation. Filter before pagination; preserve scopes, root-group behavior, ordering, counts and current-snapshot semantics. |
| R07 | Surface the existing 30-item AI grading limit before use. | Creating/editing an assignment with 31 items remains valid for manual grading but shows an explicit AI limitation. Exactly 30 works. Server exposes the same constant; no silent point/criterion reduction or automatic splitting. |
| R08 | Provide two optional skills, author-assignment and grade-with-rubric. | Skills call current schemas/capabilities, follow continuation, use bounded batching/retry and return review links/IDs. No auto-publish, invented criteria, fake confidence or claim that a valid schema proves a correct grade. |
| R09 | Optional hooks assist supported clients without becoming a trust boundary. | BeforeTool gives local structural guidance; AfterTool reports tool/item errors without leaking data. No network/model calls or writes from hooks. Client compatibility and packaging are checked before supplying runnable adapters; unsupported clients use skills plus backend guards. |
| R10 | Evaluate semantic grading quality separately from protocol validity. | Proposed pilot: 20 teacher-scored, de-identified submissions with per-item expected scores, evidence and notes on valid alternatives. Report score error, unsupported evidence, missed requirements and review flags by model/version/policy. Teachers must approve expected answers and acceptable error before quality acceptance. |

## User scenarios

- A teacher selects OpenCode, copies its configuration, verifies capabilities, creates an assignment proposal and follows its review URL.
- A teacher grades a class through MCP; an incomplete or stale proposal fails, a complete proposal remains subject to teacher review, and only failed batch items are retried.
- A teacher creates a 32-item rubric, sees the AI limit before requesting grading, and can continue manual grading.
- A student can still request private feedback for their submitted Practice work; neither Admin nor an out-of-scope teacher gains access through these changes.

## Limits and decisions requiring later evidence

No automatic installation into user-level agent settings, no real-token creation, no production/student-data export, no deployment and no commit/push are part of this planning request. Do not force a 10-point scale when the teacher selected another scale. Keep 64 KiB MCP arguments, 192 KiB content/256 KiB envelope, existing read pagination and the maximum 20 batch items; batch size must also fit bytes. Skills cannot prove that an external model read all pages or cited true evidence.

Security/resiliency extensions retain the existing enabled scope; PBT remains disabled. Plan covers authorization, privacy, validation, bounded retry and historical compatibility. Runtime security, throughput and grading quality remain unverified.
