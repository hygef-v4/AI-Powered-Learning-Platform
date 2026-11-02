# Council scope — source synchronization 2026-10-30

Original scope decision 13–15/10 retained; ownership updated against staging@794a4053. No survey or runtime certification.

## Backup

U03 performs encrypted PostgreSQL-only backups to private Drive, retention 7 days, schedules via Settings; U15 exposes status/history, U02 audit, U14 receives failure events. User file storage is not backed up. Isolated restore and measured RPO/RTO require evidence; no production restore UI/MCP.

## Work activity

U07/U10 own assessment policy, U08 stores activity via IActivityWrite; U11/U12 integrate events; U09 and scoped Quiz review show them. Retention is fixed at one day in current source, replacing configurable retention from the old design. Monitoring is an untrusted signal, no automatic cheating decision or score penalty; no webcam/screenshots/app blocking.

## Important email

U14 handles learning/important announcement/backup failure recipients, preference/dedup/finite retry after commit. U01 owns OTP and shared SMTP adapter. SENT means provider acceptance, not delivery or exactly-once. No secret/raw grade/backup archive in mail.

## AI transparency

U06 owns API/MCP/task/quota/usage. Measured/estimated/externally reported/unavailable provenance kept separate; unknown is not zero. Shared educator quota never auto-falls back to personal. Student personal API/MCP supports submitted-own Practice; Admin MCP removed. Admin institutional-key reveal and owner MCP-token reveal are explicit code-derived exceptions recorded in the decision register.

## Decisions and mapping

D01 Quiz Practice, D02 GROUP Graded, D03 no Student AI for GROUP, D04 daily educator/global quota were decided 15/10; D05 council additions retained. U03 Settings UC14/15 delegates AI configuration to U06, U15 owns UC66 dashboard, U09 owns grading UC42–45. Detailed current behavior: [decision-register.md](../reverse-engineering/decision-register.md) and [doc-code-traceability.md](../reverse-engineering/doc-code-traceability.md).
