# Integration instructions — current source

Source snapshot `f68a6ec5`, 2026-11-07. The [current source manifest](../../inception/reverse-engineering/current-code-baseline.md) lists migrations per owner. U07/U08 owner migrations are present; the “missing assignments/assignment_attempts schema” statement from the U14 21/10 checkout is historical. Docker Engine/current bean/provider availability is not checked by this doc sync.

## Isolated prerequisites

Use isolated PostgreSQL/Redis/RabbitMQ and authorized local configuration. Flyway scans db/migration recursively (out-of-order enabled); JPA validates the remaining U04/U13/U14 mappings while Spring JDBC is the project persistence policy. Never point test JDBC variables at live data. Check conditional fallback beans instead of assuming all providers are stubs. Local SMTP testing uses Mailpit; Brevo HTTPS API (`MAIL_TRANSPORT=brevo-api`) and SMTP/Drive/AI require separate provider configuration and acceptance. Do not put secret values in reports.

## Current schema deltas

- November: AI source preference, personal-key enabled/disabled state and multiple keys/provider; assignment-class grade-distribution visibility; group collaboration/assignment phase; Quiz rework and attempt activity penalty. Exact migration names/paths are in the current source manifest. Do not infer live upgrade acceptance from their presence.

- AI27/10: multiple institutional keys/provider and nullable MCP ciphertext/IV/key version. Check legacy credentials and wrong-owner reveal.
- Classes27/10: OPEN teacher constraint removed; create domain still DRAFT.
- Materials29/10: module table, backfill Module1, required materials.module_id and unique Quiz module link. Check delete with active/archived materials and cross-subject attach.
- Groups29/10: leader request reason check removed; service max1,000 remains.

## Verification

Run Maven verify and the required-suite guard from [build instructions](build-instructions.md). Check transaction/CAS/rollback/audit, notification event recipients and retries, permission changes at next request, group locks/submission, quota/tasks and provider fallback. U06 AiRepositoriesIntegrationTest and U12 GroupRepositoriesIntegrationTest now start disposable PostgreSQL/Testcontainers, load actual roles/Flyway and use restricted app credentials; no external JDBC test database is needed. Docker must be available and skipped tests must be reported explicitly. Existing [E2E scripts](../../../scripts/e2e/README.md) can seed/mutate fixtures and are not run automatically by this documentation update.

Keep runtime evidence separate from source review, mock tests and old reports. [Historical scoped evidence](build-and-test-summary.md).

Quiz V20261030_1810 permits never-published RETIRED rows with null publication and reconciles ambiguous legacy timestamps against successful exact-object publication audit. Verify upgrade with QuizPublicationMigrationIntegrationTest before applying to a target environment.
