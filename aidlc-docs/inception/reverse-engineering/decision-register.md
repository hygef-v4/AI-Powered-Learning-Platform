# Decision register — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`. User approved documentation execution: “Giúp tôi triển khai theo plan”. Source-observed behavior is documented without approving new application behavior.

| Delta | Previous requirement | Current implementation | Disposition |
|---|---|---|---|
| D01–D04 scope decisions 15/10 | Quiz Practice, GROUP Graded, no Student AI for GROUP, educator/day plus global quota | Retain scope decisions; details traced per unit | Checklist feedback 29/10 does not automatically supersede these decisions |
| D05 council scope | Backup/activity/email/provenance | Source exists; runtime acceptance is separate | No NFR certification |
| D03 audit | Student MCP, no Admin MCP | MCP_SELF for Teacher/Subject Manager/Admin; Student personal API remains | Merge staff expansion 26/10 |
| D04 audit | Single institutional key/provider | Multiple keys/provider | Document AS-BUILT cardinality |
| D05 audit | MCP token shown once | Owner can reveal ACTIVE credential if ciphertext exists | Document encryption, audit and legacy-key limits |
| D06 audit | OPEN requires teacher | Domain/service/DB permit no teacher; create remains DRAFT | Document source behavior, not migration comment alone |
| D07 audit | Configurable 1–365 day retention | Activity retention is fixed at 1 day | No automatic penalty inferred |
| D08 audit | Leader request/reject reason required | Optional, maximum 1,000 characters | Update validation and flow |
| D09 audit | Module only an extension | Basic module is implemented in U13 | Keep advanced Lesson/RAG separate |
| D10 audit | Only edit DRAFT, retire OPEN with reason, no reopen | OPEN edit stays OPEN; retire non-RETIRED; reopen DRAFT/OPEN | REVIEWED still exists; do not remove review from docs |
| D15 discovered during execution | FR-012/BR-U06-03 prohibit all API-key readback | AiConnectionService.revealInstitutional and AiAdminController permit Admin reveal for non-REVOKED institutional connections, including DISABLED; AI_CONNECTION_KEY_REVEALED audit | Document AS-BUILT exception; security design approval pending, no code/security change in this synchronization. Admin cannot thereby read personal keys |

## Execution rulings

- Execute directly in the existing checkout to preserve uncommitted plan/state/audit context; no worktree migration, commit or push.
- No production-code changes, so application TDD is not applicable. Verify document links/manifests, OpenAPI, diagram XML/renders and final diff.
- Preserve dated audit and test evidence; replace stale current descriptions rather than cover them with an ambiguous new note.
- Existing four OpenAPI files remain scoped specs. Controller inventory documents APIs without an OpenAPI spec instead of guessing their DTOs.

## User override and remediation — 2026-10-30

Previous D03 role findings describe the initial794a4053 audit. The user now explicitly removes Admin MCP and restores Student MCP for the same submitted-own PRACTICE feedback operation as personal APIs. No new AI Quiz-result explanation is introduced. Teacher/Manager tools stay scoped. Current spec: [remediation scope](../requirements/2026-10-30-test-remediation-student-mcp.md).
