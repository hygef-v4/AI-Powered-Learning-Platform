# Scoped commits, staging merge and isolated E2E

User explicitly requested: split commits, push the current branch, merge staging into it, then run E2E. Continue on `fix/remove-term`; never merge this branch into staging or force-push. Previous no-commit/no-push limits are superseded for this delivery. Unit-test report drafting remains paused; existing dated drafts keep their original contents/status.

## Execution

- [x] Fresh complete backend verification before delivery; preserve existing frontend evidence and source hashes.
- [ ] Six scoped commits: semester removal; U04 archive safety/UI; U13 provenance/denial audit/tests; U14 dependency/reminder fixes/tests; cross-unit API/transaction/protocol tests; current AI-DLC requirements/decisions/evidence.
- [ ] Normal push to origin/fix/remove-term before staging integration.
- [ ] Merge verified origin/staging into fix/remove-term, preserving both sets of changes and resolving any conflicts explicitly.
- [ ] Verify the merged backend and frontend; run the existing U04/U13/U14 API E2E through Nginx on an isolated stack, then applicable broader regressions.
- [ ] Record actual failures/fixes/reruns and final state; publish successful integration to the same branch.

## Environment and limits

Starting HEAD c9b7ce96. Fetched origin/staging at528cc50e,11 commits ahead of the original branch point. Remote fix/remove-term matches the starting HEAD. E2E project `platform-remove-term-e2e-20261108` uses separate PostgreSQL/Rabbit volumes, Redis, Mailpit and local generated credentials under gitignored `.tmp/remove-term-e2e`. Application JAR must be rebuilt from merged source; existing cached backend image supplies the runtime only. Nginx loopback18088, Mailpit loopback18025, current frontend on separate loopback18030. Existing platform containers/data are not test fixtures.

Existing Node scripts execute API E2E with real authentication/cookies/CSRF/OTP and proxy; they do not establish full browser accessibility, deployment capacity or provider acceptance. No real AI-provider-key E2E or external email delivery is authorized/needed for this local regression check. Existing security/resiliency scope retained; PBT disabled.

## Precommit verification

First complete backend run:2,015 cases,0 assertion failures/1 AMQP handshake error at the first Rabbit exchange declaration. Preserved failed log and diagnosed the fixture's TCP-only startup wait. Added startup-complete log readiness, preserving all delivery deadlines and production behavior; focused1/1 passed. Independent review found no actionable issue.

Fresh complete retry: **2,015 passed, zero failures/errors/skips**, BUILD SUCCESS16:38:43+07, executable JAR packaged. Required PostgreSQL guard31/7/10/1 passed. Fresh complete frontend **805/805 passed**, zero failed/pending, exit0; earlier typecheck/scoped lint/build31pages remain valid on the unchanged frontend sources. This new result supersedes earlier2,010 plus targeted-run evidence for the premerge tree; historical run records remain unchanged.
