# Scoped commits, staging merge and isolated E2E

User explicitly requested: split commits, push the current branch, merge staging into it, then run E2E. Continue on `fix/remove-term`; never merge this branch into staging or force-push. Previous no-commit/no-push limits are superseded for this delivery. Unit-test report drafting remains paused; existing dated drafts keep their original contents/status.

## Execution

- [x] Fresh complete backend verification before delivery; preserve existing frontend evidence and source hashes.
- [x] Six scoped commits: semester removal; U04 archive safety/UI; U13 provenance/denial audit/tests; U14 dependency/reminder fixes/tests; cross-unit API/transaction/protocol tests; current AI-DLC requirements/decisions/evidence.
- [x] Normal push to origin/fix/remove-term before staging integration.
- [x] Merge verified origin/staging into fix/remove-term, preserving both sets of changes and resolving any conflicts explicitly.
- [x] Verify the merged backend and frontend; run the existing U04/U13/U14 API E2E through Nginx on an isolated stack, then applicable broader regressions.
- [x] Record actual failures/fixes/reruns and final state; publish successful integration to the same branch.

## Environment and limits

Starting HEAD c9b7ce96. Fetched origin/staging at528cc50e,11 commits ahead of the original branch point. Remote fix/remove-term matches the starting HEAD. E2E project `platform-remove-term-e2e-20261108` uses separate PostgreSQL/Rabbit volumes, Redis, Mailpit and local generated credentials under gitignored `.tmp/remove-term-e2e`. Application JAR must be rebuilt from merged source; existing cached backend image supplies the runtime only. Nginx loopback18088, Mailpit loopback18025, current frontend on separate loopback18030. Existing platform containers/data are not test fixtures.

Existing Node scripts execute API E2E with real authentication/cookies/CSRF/OTP and proxy; they do not establish full browser accessibility, deployment capacity or provider acceptance. No real AI-provider-key E2E or external email delivery is authorized/needed for this local regression check. Existing security/resiliency scope retained; PBT disabled.

## Precommit verification

First complete backend run:2,015 cases,0 assertion failures/1 AMQP handshake error at the first Rabbit exchange declaration. Preserved failed log and diagnosed the fixture's TCP-only startup wait. Added startup-complete log readiness, preserving all delivery deadlines and production behavior; focused1/1 passed. Independent review found no actionable issue.

Fresh complete retry: **2,015 passed, zero failures/errors/skips**, BUILD SUCCESS16:38:43+07, executable JAR packaged. Required PostgreSQL guard31/7/10/1 passed. Fresh complete frontend **805/805 passed**, zero failed/pending, exit0; earlier typecheck/scoped lint/build31pages remain valid on the unchanged frontend sources. This new result supersedes earlier2,010 plus targeted-run evidence for the premerge tree; historical run records remain unchanged.

## Git delivery and merge

Six commits pushed normally to origin/fix/remove-term, ending atfc31721b: b49bcc38 semester removal; ed7e27f1 U04; bcc9dfee U13;635f8b68 U14;722c8f68 cross-unit tests;fc31721b AI-DLC documentation. Refetched staging at528cc50e immediately before integration. Merge6007c5fb has both parents and preserves all staging changes. The only conflict was append-only audit history; removed conflict markers and verified every line from both original sides remains in order. No source conflict, force-push or merge into staging.

E2E used a copied merged JAR, so a later Maven verification could not replace the artifact while the isolated app was running. Postmerge test compile exposed three imports removed by staging's unused-import cleanup but needed by the branch's added tests: Mockito.never, ArgumentCaptor and AssertJ.assertThat. Restored exactly those imports. The U14 E2E still expected staff defaults OFF; BR-U14-21 and approved D-U14-09 explicitly require ON. Corrected that oracle, retaining exact role keys, opt-out/relogin and mandatory Admin checks. Independent review found no actionable issue. These four test-only repairs are commit44b5781d; no production behavior was changed after merge.

## Postmerge verification and E2E

| Suite | Final result | Evidence scope |
|---|---|---|
| U04/U13/U14 API E2E |40/40 | Real role accounts, OTP/Mailpit, cookies/CSRF, Nginx and isolated owner database |
| U02 API E2E |16/16 | Full original assertions, rerun after U12 created required cross-unit audit fixtures |
| U12 API E2E |58/58 | Group flows, submission/deadline and actual SSE through proxy |
| U12b API E2E |35/35 | Additional group work/deadline/realtime checks |
| U12c API E2E |19/19 | Current staging group editing/import/review flows |
| Frontend suites changed by staging |286/286 | Academics, admin dashboard, audit, grading, groupdocs and groups; zero failed/pending |
| Production frontend build |31 pages, exit0 | Internal TypeScript build check also passed |

Total E2E **168 checks passed**. These counts are API/script checks, not pure unit cases or a full browser journey. Legacy U12 scripts return exit0 even on failed assertions, so every result JSON was independently checked. U02 was initially run too early:15/16 because no U06/U12 audit fixture existed yet; preserved that result and reran the unchanged suite after U12,16/16. Initial U14 stale-default oracle result38/40 is also retained separately. No real AI key/provider email was used.

The initial E2E backend startup overlapped builds and missed its initial health window; it became healthy without a source/config/deadline change. Waited for both backend and worker health before OTP fixtures. Proxy smoke checks: login200, protected profile401, nginx-health200. Separate project containers and the verified frontend PID were stopped after E2E; volumes/ignored logs retained, original platform stack preserved.

The first complete postmerge backend execution compiled after the import repair and ran **2,004 tests:2,003 passed, one p95 assertion failed**, zero errors/skips. U14 announcements p95 **368.3049ms >300ms** while E2E ran concurrently. This is retained as an observed load-sensitive result; no threshold was changed. After stopping the E2E project/frontend, the identical benchmark passed1/1 with announcements p95 **120.9493ms**, same fixture/warmup/50 samples/threshold. This does not certify concurrent-load performance.

Final complete backend retry after stopping E2E: **2,004/2,004 passed, zero failures/errors/skips**, BUILD SUCCESS17:21:26+07, executable JAR packaged. All seven declared p95 checks passed, including announcements **83.9347ms ≤300ms**. Required PostgreSQL guard31/7/10/1 passed again. Postmerge scoped frontend lint exited0. All1,625 source/config hashes match the tested tree; documentation relative-link and whitespace checks passed. No production source changed between the copied E2E artifact and final verification; the later repairs changed test imports/oracles only.

Verified merge/test integration through44b5781d was normally pushed to origin/fix/remove-term after these results. The final documentation-only commit records this completed publication. No force-push, staging write, production deployment or complete browser/load/provider acceptance claim. Report drafting remains paused.
