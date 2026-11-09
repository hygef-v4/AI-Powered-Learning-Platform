# Commit, staging integration and E2E delivery

User explicitly authorizes committing/pushing the current integration-test branch, then merging staging into that branch and creating a PR to staging only after E2E succeeds. This supersedes the previous no-commit/no-push limit for delivery. Do not merge the PR or deploy.

- [ ] Verify that the reviewed source still matches the successful2,028-test/57-case snapshot; inspect scoped changes and commit them in reviewable groups.
- [ ] Push integration-test normally, preserving any existing remote work.
- [ ] Fetch current staging and merge origin/staging into integration-test; preserve both sides of any conflict.
- [ ] Assess the merge delta and verify applicable backend/frontend changes. Run Maven sequentially and before starting the E2E stack to avoid contention with local latency checks.
- [ ] Start a separate local E2E project with disposable database/Rabbit volumes, Redis and Mailpit, current merged JAR and loopback Nginx. Wait for backend and worker health. Preserve original platform data/containers.
- [ ] Run the existing U04/U13/U14, U12/U12b/U12c and U02 API E2E suites with the required fixture ordering; inspect result JSON as well as process exit codes. These are API/proxy/SSE checks, not complete browser journeys. No real AI key or external email provider calls.
- [ ] Record actual results and exclusions, stop only this task's stack, commit delivery evidence and push the merged branch normally.
- [ ] Verify the remote head matches the tested head and create/update one PR from integration-test to staging; report its URL. No PR merge.

Pre-delivery evidence: full backend2,028/2,028 passed, zero failures/errors/skips; required guard57 cases/62 mapped invocations and four negative guard checks passed. English workbook57 cases, preserving49 original rows/styles/dates. The workbook remains the separately delivered artifact; repository evidence includes its SHA256. Existing security/resiliency retained; PBT disabled.
