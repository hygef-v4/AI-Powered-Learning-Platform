# Integration instructions — current source

Source-observed baseline 2026-10-30. U07/U08 owner migrations are present; the “missing assignments/assignment_attempts schema” statement from the U14 21/10 checkout is historical. Docker Engine/current bean/provider availability has not been checked in this doc sync.

## Isolated prerequisites

Use isolated PostgreSQL/Redis/RabbitMQ and authorized local configuration. Flyway scans db/migration recursively (out-of-order enabled), JPA validates schema. Never point test JDBC variables at live data. Check conditional fallback beans rather than assuming all providers are stubs from the old checkout. SMTP local testing uses Mailpit; real Brevo/Drive/AI requires separate credentials/acceptance. Do not put secret values in reports.

## Current schema deltas

- AI27/10: multiple institutional keys/provider and nullable MCP ciphertext/IV/key version. Check legacy credentials and wrong-owner reveal.
- Classes27/10: OPEN teacher constraint removed; create domain still DRAFT.
- Materials29/10: module table, backfill Module1, required materials.module_id and unique Quiz module link. Check delete with active/archived materials and cross-subject attach.
- Groups29/10: leader request reason check removed; service max1,000 remains.

## Verification

Run Maven verify and the required-suite guard from [build instructions](build-instructions.md). Check transaction/CAS/rollback/audit, notification event recipients and retries, permission changes at next request, group locks/submission, quota/tasks and provider fallback. U06 AiRepositoriesIntegrationTest and U12 GroupRepositoriesIntegrationTest now start disposable PostgreSQL/Testcontainers, load actual roles/Flyway and use restricted app credentials; no external JDBC test database is needed. Docker must be available and skipped tests must be reported explicitly. Existing [E2E scripts](../../../scripts/e2e/README.md) can seed/mutate fixtures and are not run automatically by this documentation update.

Keep runtime evidence separate from source review, mock tests and old reports. [Historical scoped evidence](build-and-test-summary.md).

Quiz V20261030_1810 permits never-published RETIRED rows with null publication and reconciles ambiguous legacy timestamps against successful exact-object publication audit. Verify upgrade with QuizPublicationMigrationIntegrationTest before applying to a target environment.
