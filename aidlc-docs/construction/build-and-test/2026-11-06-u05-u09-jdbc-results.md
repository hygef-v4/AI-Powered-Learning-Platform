# U05/U09 JDBC — execution ledger and evidence

[Approved scope and plan](../plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md).

Status: complete, code/documentation verified; no commit/push or runtime restart. User discarded the preceding JPA migration; clean checkout verified before starting this JDBC task. No prior JPA migration evidence counts toward this task.

Pre-flight: U05 already JDBC; U09 mapping tests and PostgreSQL version tests directly referenced two JPA entities and interfaces and moved with both adapters. Other unit JPA infrastructure remains to preserve shared transactions until migrated separately. No worktree/commit/push or app restart.

## Baseline and first migration gate

- `baseline-red.log`: 209 behavior/other architecture cases passed, zero errors/skips; the new JDBC architecture rule failed against 25 JPA/Hibernate dependencies as expected. Seven PostgreSQL version/lock/rollback cases ran using all 32 owner migrations, app grants and Hibernate validate before adapter changes. Two initial setup/compile attempts failed (duplicated test insertion, then incorrect ArchUnit API); fixed and reran before using results.
- `jdbc-first.log`: the same 210 cases passed, zero failures/errors/skips after switching both adapters and the fixture to JDBC/DataSourceTransactionManager. JSONB stays object-shaped; reads/polling do not write; save/finalize/publish advance versions once, stale/concurrent writers cannot both commit, row/advisory locks survive to caller commit and rollback restores document/version.
- Expanded focused verification: `jdbc-focused.log`, 446 passed, zero failures/errors/skips, including nine PostgreSQL persistence cases and JDBC rollback joining the application's JpaTransactionManager on the same DataSource.

## Review and execution decisions

- Fresh independent review found no Critical/Important production regression. One Important documentation defect (U04 target still prescribed @Version under JDBC) was fixed with explicit SQL CAS and a separately labeled current JPA gap.
- Review identified missing no-op characterization. Added `explicitNoOpSavesAdvanceVersionsButReadsDoNot`: an explicit save of an existing evaluation/proposal increments storage version even with unchanged payload; reads do not. JPA could suppress a same-context no-op. Caller review found no meaningful user regression: grading commands change state/timestamps; duplicate submissions return before saving; proposal saves hold the parent evaluation lock and their storage version is not public. No ORM cache emulation added; this internal difference is explicit in current U09 docs.
- Standard `full-verify.log`: 1,829 tests passed, no failures/errors/skips, but repackage failed because the target JAR could not be renamed while the local backend held it. Did not stop/restart the application. Standard target currently contains a thin JAR; use the separate executable artifact after successful isolated packaging, not that thin JAR for a new launch.
- Prepared an ignored scratch POM with identical dependencies/source/test/resource inputs and a separate output directory; Surefire working directory stays backend for existing fixtures. First isolated verify hit `AiRepositoriesIntegrationTest` migration SQLState 08006/EOF from PostgreSQL; its isolated rerun passed all five cases without source/config changes. Full rerun subsequently passed. No live DB/app modification performed to address the build lock.

## Final verification — 2026-11-06T09:27:28Z

| Check | Actual result |
|---|---|
| Full isolated Maven verify | BUILD SUCCESS, 1,830 tests in 236 suites; zero failures/errors/skips; `full-isolated-verify-final.log` |
| Required PostgreSQL guard | 31 U04 + 2 notifications + 10 U09 version/transaction cases + 1 visibility case; all ran, none skipped |
| Artifact | `.tmp/jdbc-transition-20261106/target/platform-backend-0.1.0-SNAPSHOT.jar`; executable Boot manifest verified; obsolete U09 JPA entity/interface classes absent |
| Source/config verification | 1,008 backend source/test/resource/POM/guard hashes unchanged after final run; scratch dependency block identical to backend/pom.xml |
| Architecture | Production U05/U09 cannot depend on JPA/Hibernate; all three architecture rules passed |
| Documentation | 49 changed/new Markdown documents checked, balanced fences, no newly broken local links; historical broken links not silently rewritten |
| Whitespace and formatting | git diff --check passed; existing JDT formatter checked token preservation/idempotence on changed Java files; no new dependency |

Guard command: `powershell -NoProfile -File scripts/assert-required-backend-tests.ps1 -ReportsDirectory .tmp/jdbc-transition-20261106/target/surefire-reports`.

Full verify launcher/logs/scratch POM and hashes are ignored under `.tmp/jdbc-transition-20261106`. Source POM, dependencies, frontend, schema and migrations unchanged. Standard backend/target JAR remains a thin artifact after the locked repackage attempt: use the executable path above for a future launch, or rebuild the normal target when the old process is stopped. The existing application was not restarted into this code.

## Limits and compliance

JDBC is the project-wide documented target; U04/U13/U14 still use JPA and require separate code migrations. JPA dependencies/manager remain for those owners; U09 JDBC rollback participation was exercised directly. Explicit no-op saves increase storage version, unlike JPA's possible same-context suppression; caller review and the new characterization record that internal distinction. No blanket claim of exact ORM internal equivalence or runtime/provider/load acceptance.

Existing Security/Resiliency scope retained. SQL input binding, API/authorization/privacy regression, version/locking and rollback checks passed within this change; no new endpoint/deployment/logging policy introduced. Infrastructure/provider/load controls are N/A for this migration verification, not newly certified. PBT stays disabled. Frontend tests, authenticated UI/provider acceptance and app restart were not performed. No commit, push, merge or live migration.
