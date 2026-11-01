# Assignment distribution visibility — verification 2026-11-01

## Delivered behavior

The manager class-wide toggle and mutation endpoint are removed. Assignment settings expose a grading-owned control for the assigned teacher, including a Subject Manager actually assigned to teach the class. Visibility is stored independently for each assignment/class pair and defaults off. A published SUBJECT assignment can therefore have different visibility in different classes, even while its content is read-only to the teacher. The control saves immediately and independently of assignment content.

Only GRADED assignments qualify. Unauthorized, mismatched-class and unpublished SUBJECT access fails closed. Archived classes and retired assignments cannot change the preference. Stale writes return 409 instead of overwriting another save. Failed frontend saves retain the confirmed value and require reloading before retrying.

The student endpoint retains active-enrollment checks, published-score filtering, one value per active learner, at least 20 usable published scores, and suppression of buckets below five. No learner identities or individual peer scores are returned. Quiz/Practice remains excluded. The historical class flag remains in storage and compatibility DTOs but has no effect on visibility; old true flags are not migrated.

## Fresh verification

- Backend focused Maven run: 173 passed, zero failures/errors/skips, including architecture (2) and actual PostgreSQL migration/concurrent-insert/concurrent-update/class-isolation coverage (1). Final AcademicsApiContractTest rerun: 35 passed, including the newly added old-route 404 regression; 174 distinct tests across both runs, not 208.
- Maven used offline cached dependencies with `-s ../scripts/maven-local-cache-settings.xml` and explicit local repository. Tests compiled the complete backend test source set. The complete backend suite was not executed.
- Frontend full suite: 615 passed across 95 files. The expanded visibility settings suite also passed 13/13. Typecheck and production build passed. Full lint: zero errors, two existing unused-import warnings in AI registration and account details.
- Grading OpenAPI YAML parsed; 21 unique operations, all path parameters and 169 local references validated. New GET/PUT document dependency failure responses. `git diff --check` passed.
- Current-document link scan covered 356 links; the existing `docs/usecase_table.md` link to `history/use-cases-73.md` is still broken and was not introduced by this change. Historical reports were preserved.
- Restarted the existing local backend terminal using the repository script. Application startup succeeded with the new migration. Orca browser confirmed an existing teacher-owned class assignment defaults off; switching on returned HTTP 200 with enabled=true/version=1; switching off returned enabled=false/version=2. Reloading the page and reopening settings preserved off. No assignment content or scores were changed by this check.
- Inspected the new settings at 390 px: dialog fit within the viewport, text remained readable and no page-level horizontal overflow was measured. The preference was left off after verification.

## Boundaries

No production deployment, commit or push. No full backend, provider, load or broad all-role browser acceptance run. Existing pending assignment/grading layout work was preserved. The local database received only the additive preference migration and the test preference row (off, version 2). Other classes and assignments remain disabled unless their assigned teacher explicitly enables them.

Security scope: applicable authorization, boundary validation and anonymous-aggregate privacy checks verified within this feature. Resiliency scope: optimistic conflicts and failed-load/save recovery verified. Infrastructure restore/load/provider controls are outside this delta; PBT remains disabled.
