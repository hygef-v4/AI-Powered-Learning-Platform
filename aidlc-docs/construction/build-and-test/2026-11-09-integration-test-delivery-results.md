# Integration-test staging merge and E2E verification

Verified application head: `65f86a6c432c63e318737cef88195ad5ffd30ab1`. Latest integrated staging: `198b796f`. Branch `integration-test`; PR target `staging`. Publication follows the evidence commit; the PR is not merged by this task.

Commits15400ac2 and8d53a1c6 were pushed normally before merging staging. Merge a55f8426 integrated staging fb20c6bc. Only audit history conflicted; every line from both sides was preserved. Progress commit ef55fab9 preceded merge65f86a6c, integrating the later U12 lost-hold/submit-author fix. No source conflict or force-push.

| Verification snapshot | Actual result |
|---|---|
| Complete first merge: backend |2,028 passed; zero failures/errors/skips; JAR packaged;57-case guard passed |
| Complete first merge: frontend | Build/lint/typecheck passed;800/800 tests passed with two workers and original5000ms timeout |
| Latest U12 increment: backend |95/95 passed, including all Group/ClassGroup tests,24 real composed integration methods and architecture checks; all test sources compiled and current JAR packaged |
| Latest U12 increment: frontend | Current production build/typecheck and changed-file lint passed;15/15 workspace tests passed |
| U04/U13/U14 API E2E |40/40 |
| U12 API/proxy/SSE E2E |58/58 |
| U12b additional group E2E |35/35 |
| U12c current group editing/import E2E |19/19 |
| U02 audit E2E, after group fixtures |16/16 |

All168 E2E assertions ran against the latest rebuilt application. These are authenticated API/proxy/SSE checks with Mailpit OTP/local SMTP, not a complete browser journey. Real AI/provider-key suites were outside this local regression scope. Legacy scripts' JSON assertions and expected counts were checked independently of exit0.

The complete runs apply to the first staging snapshot; the95/15 results are the subsequent U12 increment. They are not added together or presented as a fresh complete run of the final tree. PR CI runs the complete suites on its merge candidate.

The first default-worker frontend run had seven5000ms timeouts (793/800 passed). No source assertion or timeout was changed. The same seven files passed40/40 in isolation, then all800 passed with maxWorkers=2 after backend completion. Both logs and machine-readable results are retained. The initial smoke-only stack was stopped to integrate staging's second update before business E2E.

The separate project used fresh generated credentials, loopback Nginx19088/Mailpit19025 and a copied immutable JAR. Backend and worker were healthy before seeding. U02 ran last to consume real group audit fixtures. Only task containers/frontend were stopped afterward; all7 original platform containers retained their IDs, and task volumes/logs were preserved. No real platform data or live-provider operation was used. Existing security/resiliency retained; PBT disabled.

The [machine-readable evidence](2026-11-09-integration-test-delivery-evidence.json) records the verified code head, Git tree IDs, JAR/result/log hashes and exact scopes. Publication and subsequent CI status belong to GitHub; these execution snapshots remain historical. The previously delivered57-case Excel workbook is unchanged.
