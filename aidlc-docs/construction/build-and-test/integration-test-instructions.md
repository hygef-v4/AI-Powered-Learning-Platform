# Integration test instructions — U14

Status2026-10-21: independent runtime acceptance not run. Docker CLI exists but Engine's `dockerDesktopLinuxEngine` pipe is unavailable. U09 grading references `assignments` and `assignment_attempts`, whose real U07/U08 migrations are absent. U07/U08/U12 query/hub providers are stubs. These prerequisites must be supplied by owner units; do not create replacement tables or treat empty stub responses as success.

## Environment setup

Use an isolated database, broker and test accounts. Confirm `docker version` returns a server version. After owner migrations/providers are available, review existing local configuration without exposing credentials. From `infra/`:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up --build
```

The local override routes email to Mailpit and disables SMTP auth/TLS for that local service. Preserve it; do not use a real recipient/provider for these scenarios. Configure a tester-owned public origin and test cap through existing `APP_PUBLIC_URL` and `U14_EMAIL_DAILY_CAP`. Confirm all migrations apply and Hibernate validation succeeds before testing U14.

## Execution

Run existing Testcontainers tests using `mvn -B verify` from `backend/` once Engine works. This does not constitute an independent U14 integration suite. No `npm run test:integration` script or separate integration Maven profile exists. An independent tester must execute and retain evidence for the complete [U14 matrix](u14-verification.md), including:

| Scenario | Action and expected evidence |
|---|---|
| Migration and transaction | Apply real migrations to empty PostgreSQL; validate five U14 tables/FKs/indexes/checks. Race two updates/deletes with one version: one success, one409. Inject audit/persistence failure: no partial mutation/event. |
| Real producers and dedup | Publish actual supported owner events with actual payloads; replay concurrently. Verify current authorized recipients,125+Admin pagination, one notification/email per unique key, SSE only after commit. |
| Broker faults | Stop broker/consumer connections and fail retry publication; observe confirm-before-ack, TTL30s/1m/2m/4m/8m and exhausted parking queue. Unknown schema must not fan out. |
| Email and cap | Use Mailpit with concurrent workers and API,300daily reservations, preference changes and Vietnam midnight. Verify priority and no cap overflow; failure finite retry, SENT terminal, expired reminders skipped. Explicitly record unresolved SMTP-accept-before-DB-commit ambiguity. |
| Reminder and retention | Real assignment/submission/group data: non-submitters only, changed deadline, late-open/retired assignments, subject classes and repeated scan. Verify atomic marker/fanout;180day cutoff and10000delete limit with child cascade. |
| SSE/session | Two actual app instances with real U12 hub; read from another tab, reconnect/logout/account disable and guessed foreign channel. Check account isolation and delivery latency. |

Inspect database state and captured mail, not only HTTP success. Record event correlation IDs and sanitized timings without bodies, tokens or addresses. Stop only the test stack you started using the same compose files with `stop`; preserve volumes/evidence for investigation. Reset only tester-owned data through the agreed fixture lifecycle.
