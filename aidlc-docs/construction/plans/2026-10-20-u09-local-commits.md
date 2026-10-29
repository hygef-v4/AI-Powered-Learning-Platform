# U09 local commit split

User request: `giúp tôi chia nhỏ commit chưa push`.

Branch: `grading-gradebook`. Starting HEAD: `6341aed2f66271dd7d112bf3e3565349071402d2`.

- [x] Inspect all tracked/untracked changes and confirm the index starts empty.
- [x] Review existing build/test evidence from the unchanged source state; diff whitespace check passed.
- [x] Define seven Conventional Commits with explicit file membership and validation context.
- [ ] Create local commits. Blocked: `git add` cannot create `.git/index.lock` because Git metadata is read-only in this sandbox. No files were staged and no commits created by this attempt.
- [ ] Verify resulting commit history and clean working tree. No push is authorized or performed.

| Order | Commit | Scope |
|---|---|---|
| 1 | `feat(grading): add evaluation domain and persistence` | Domain entities/repositories, JPA adapters, migration and associated domain/persistence tests/fixtures |
| 2 | `feat(grading): implement grading workflows and REST APIs` | Application services, AI/MCP proposals, controllers, access policy/actions, group contract stub behavior, backend tests and OpenAPI |
| 3 | `feat(api): support authenticated binary downloads` | Shared API download implementation and tests |
| 4 | `feat(grading): add grading workspace and gradebook UI` | U09 components/hooks/routes/slot registration and frontend workflow tests |
| 5 | `fix(build): stabilize Windows builds and self-host fonts` | Build launcher/config/ignore rules, local font assets/license, layout and Docker public assets |
| 6 | `build(maven): support existing offline cache origin` | Optional Maven Central cache-origin settings profile |
| 7 | `docs(grading): record U09 implementation and verification` | AI-DLC plans, state, audit and implementation handoff |

The new U09 files already contain the reviewed refactor. It is included with its feature responsibility rather than reconstructing an artificial older implementation solely to produce a refactor commit.

Verification already completed for this source state: full backend verify/executable JAR with cache profile, 724 tests total / 662 passed / 62 skipped; frontend 223 tests, lint/typecheck and standalone build passed. PostgreSQL/provider/NFR gates and HTTP smoke socket restrictions remain as documented. These are final-state results, not a claim of independently testing every intermediate commit.

An executable fallback is prepared in ignored `.tmp/commit-u09-local.ps1`. Run it from PowerShell outside this sandbox after reviewing `-Preview`. It validates the starting branch/HEAD, an empty index and exact file hashes, uses literal Git paths, checks each staged set and creates only the listed local commits. It stops on any Git error. It contains no push command. If interrupted after some commits, inspect those commits and the index before continuing; the starting-HEAD check deliberately prevents blind replay.

Existing security/resiliency configuration retained; no application changes in this commit-splitting task. Disabled PBT skipped. No test rerun is needed merely to reorganize unchanged files.
