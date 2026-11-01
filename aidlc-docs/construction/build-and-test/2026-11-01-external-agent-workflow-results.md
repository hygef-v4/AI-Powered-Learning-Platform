# External-agent workflow — verification results

Date: 2026-11-01. Existing checkout: mcp-test, base a0e1ea4f. Prior authorized quality-remediation changes retained. [Requirements/stories](../../inception/requirements/2026-11-01-external-agent-workflow.md), [plan](../plans/code-generation-plan/2026-11-01-external-agent-workflow.md).

## Delivered

- External agent chat remains the starting point and place for clarifying requirements. No new workflow-session table, mandatory platform form or UI chat. Capabilities and authoring-requirements responses expose workflow guidance without requiring skill installation. Student guidance remains own submitted Practice only.
- Authoring skill asks grouped material questions, disambiguates authorized targets, inherits known settings and covers Practice/Graded mode. Grading skill clarifies assignment and pending/draft scope, reads every page and returns exact result links. Both skills remain repository-local and optional; model obedience is not guaranteed.
- MCP grading proposals now require snapshotFingerprint and verified contiguous context reads. Redis atomically tracks served block coverage per actor/evaluation/fingerprint for 30 minutes of inactivity. Missing/gapped/expired coverage or unavailable Redis fails closed. Current policy/assignment/document fingerprint is rechecked at ingress. Empty documents require an initial context read. API/manual/history behavior remains separate.
- Batch response budgeting includes the escaped MCP text envelope before progress is recorded. Results omitted by the budget remain uncredited. Batch context preserves REPEATABLE_READ and the existing timeout.
- Revised assignment drafts and single/batch/listed grading proposals return reviewUrl. Grading routes accept proposalId, open the flat review section and expand/scroll to the selected older proposal. Missing links report an error after loading. No URL selection applies or saves a score.
- Current U06/U07/U09 business rules, user guides, plan, audit and state updated. Existing historical reports preserved.

## Evidence

| Check | Result |
|---|---|
| Backend regression RED | New grading write without fingerprint was accepted by baseline. New test failed before guard implementation. |
| Batch regression RED | Escape-heavy JSON batch passed raw budget and would advance undelivered read progress. Reproduced before escaped-envelope fix. |
| Backend focused verification | 67 tests in 13 suites; zero failures, errors or skips. Includes GradingMcp*, GradingProposal*, AssignmentMcp*, McpStaffCatalogTest, McpServerTest and ArchitectureTest. |
| Redis runtime | Existing local Redis: initial page gap rejected, contiguous pages accepted, skipped offset rejected, overlapping retry did not regress progress, TTL bounded. Used one random temporary key, deleted afterwards. Credentials were not printed. |
| Frontend regression RED | Older proposal link did not open the requested feedback before implementation. |
| Frontend affected verification | 38 tests across 5 suites passed: workspace, route shell, AI source integration, structured feedback and setup guide. |
| Frontend checks | Typecheck and production build passed. Lint zero errors; two existing unused-variable warnings in AI register and AccountDetailPage. |
| Optional hook self-check | 14 dummy-data scenarios passed; adapter updated for required fingerprint. This remains a legacy optional Gemini adapter, not a port to the four current clients. |
| Fresh source review | Identified envelope coverage bug and lost transaction boundary; both corrected. Reviewer confirmed no remaining concrete blocker in reviewed scope. |
| Skill pressure checks | Ambiguous classes, ambiguous grade-all scope, incomplete context and exact older review handoff covered. Practice/Graded omission found and corrected. Dry-run policy checks do not measure educational quality. |
| Local package/runtime | Backend repackaged and restarted in existing Orca terminal; health UP. Old backend retained the jar during shutdown; stopped only the verified local backend process and then packaging succeeded. |
| Orca UI | Separate tab in existing Teacher profile opened an existing older proposal. Selected label and feedback visible, review section open, zero dialogs; at 627 px viewport document width 617 px. Screenshot inspected. No apply/save/request/provider action performed. |

## Limits and rollout

Clients must refresh/discover the new write schema and include snapshotFingerprint. Existing historical proposal records are untouched; a retry after progress expires must first reread current context. Progress is account-scoped across that account's MCP credentials and shared across backend instances through Redis. A disconnect can occur after a page was served; the server cannot prove client attention or semantic understanding.

No new provider/model calls, real MCP credentials, personal skill/config installation, database migration, official publication, commit or push. No fresh authenticated acceptance through all four external clients or teacher-scored model-quality pilot. Full repository suites were not repeated for this follow-up; targeted affected suites and architecture were run. Existing security/resiliency scope retained; PBT disabled. No dependencies added.
