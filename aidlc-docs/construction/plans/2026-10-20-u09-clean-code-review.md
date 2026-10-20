# U09 behavior-preserving clean-code review

Scope: continue existing U09 Construction with the user's explicit refactor authorization. Retain current API/DTOs, schema, validation order and messages, access checks, transactions, callbacks and side effects. Existing dirty work is the baseline, not disposable changes.

- [x] Read Clean Code skill, AI-DLC state/design handoff and relevant source/tests; identify existing refactor and remaining friction.
- [x] Establish backend/frontend baseline and pass retained MCP characterization tests on unchanged production code.
- [x] Simplify MCP argument validation and submission lookup within the existing class; rename ambiguous local variables in batch/proposal and gradebook UI code.
- [x] Verify focused and broader checks, compare against baseline, inspect the actual session diff and record limitations.

Review findings: existing GradebookService helpers and workspace/form/polling separation are useful; additional layers would add navigation cost. GradingMcpTools.call still mixes JSON shape validation, submission lookup and proposal recording, with opaque variable names. Extract only cohesive blocks into private methods in the same file. Preserve the exact advertised schema string. Names in proposal dispatch/batches and gradebook rows can clarify intent without changing flow.

Protection: current U09 domain/service/access/export/persistence tests and frontend workflow tests; add MCP tests at the public call seam for both submission kinds, default agent label, output shape, validation precedence, malformed UUID error mapping and write suppression. Run those before production edits.

Verification uses local Maven cache/JDK and the existing ByteBuddy startup agent. Raw test logs and pre-edit snapshots live in ignored scratch. No changes to tooling/dependencies. SQL/provider/browser/NFR and executable packaging gates from the implementation handoff remain separate and unverified by this refactor.

Extensions: Security 03/04/05/08/09/12/15 and Resiliency 04/06/10 retain the existing reduced project scope. This refactor does not change logging, error handling, validation, authorization or external interactions. Deployment/configuration checks are N/A to these internal edits; disabled PBT skipped.

## Evidence

| Check | Result |
|---|---|
| Baseline Maven U09 + GradingPolicyTest + ArchitectureTest | 45 total: 42 passed, 3 PostgreSQL cases skipped; no failures/errors |
| MCP characterization before production changes | 8 passed (3 existing + 5 new cases); both source kinds, agent default, result DTO, validation ordering and malformed UUID mapping |
| Final full backend `mvn -o -f backend/pom.xml test` with startup ByteBuddy agent | 724 total: 662 passed, 62 skipped; no failures/errors |
| Baseline and final full frontend `npm --prefix frontend test -- --maxWorkers=4` | 223 passed / 45 files in both runs |
| Frontend lint/typecheck | Both passed |
| Frontend `npm --prefix frontend run build` | Failed with TurbopackInternalError in unchanged `academics.module.css`: PostCSS child node process exited before connection. Production build is unverified. This differs from the earlier handoff's EPERM failure and was not reproduced on the session baseline; no claim of equivalent build failure is made. |
| Backend `mvn -o -f backend/pom.xml -DskipTests verify` | Packaging blocked by missing offline plugin dependencies, consistent with existing implementation handoff; not a source compilation/test failure |
| Session diff review | Four production files only: MCP private-method extraction/constants, batch/proposal local naming, gradebook local naming. All string literals unchanged; advertised MCP schema unchanged. Public signatures except parameter names, transaction annotations and external call order retained. No new production files/dependencies. |

Initial test launcher attempt failed before running tests because a spaced javaagent path was not preserved by the Windows launcher. Copying the same agent JAR to ignored scratch resolved this; no production or build configuration changes were needed.

For a future change to submission lookup, the two real source branches are now together in `findEvaluation`; JSON shape guards remain together in `readSubmissionReference`, in the same class. Existing tests protect default output and error precedence without testing private methods. Existing GradebookService selection rules and workspace conflict/polling code were reviewed and deliberately retained.
