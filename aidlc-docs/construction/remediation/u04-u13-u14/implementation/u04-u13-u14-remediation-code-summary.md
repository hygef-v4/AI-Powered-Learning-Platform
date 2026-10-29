# U04/U13/U14 remediation code summary

Branch: `fix-u04-u13-u14`. Implementation and retest: 23–24 October 2026. Scope follows approved plan A in `../plans/u04-u13-u14-blackbox-remediation-plan.md`. This is a brownfield correction to existing units, including the narrow U09 dependency required to test U14 grade notifications.

## Behavior changes and traceability

| Area | Problem | Result | Traceability |
| --- | --- | --- | --- |
| U04 | Admin's read-only class table lacked visible ownership guidance | Displays “Chỉ xem. Lớp do Chủ nhiệm môn quản lý.”; columns, counts and read-only behavior retained | UC18, BR-U04-05, U04-09 |
| U13 upload | Multipart XHR omitted the CSRF header sent by the fetch client | Both transports use the same decoded XSRF cookie helper; upload sends X-XSRF-TOKEN with credentials and FormData | UC59/61, BR-U13-10/40, U03 file policy, U01 CSRF |
| U13 extraction | Worker completed extraction but list could remain PENDING until another user action | Refresh every five seconds while ACTIVE/PENDING; stop at terminal status, load error or unmount; pause during add/reorder operations | BR-U13-30, U13-01/04 |
| U14 announcements | Mixed Markdown and raw script was rejected entirely | Approved policy A strips HTML/attributes/script/style data before storage, retains tested Markdown/newlines, rejects empty/oversized sanitized content; title remains plain text | UC63/64, BR-U14-02, U14-02 |
| U09 grading dependency | Reading mutable JSON snapshots could cause an extra Hibernate update and version increment, blocking subsequent grading commands | Immutable JSON text mapping avoids false dirty checking while retaining JSONB object storage and optimistic locking | BR-U09-40/71, U14-12/13 |

## Modified production files

- `frontend/src/features/subjects/SubjectClassesTab.tsx`: visible read-only guidance.
- `frontend/src/features/files/api.ts` and `frontend/src/shared/api/client.ts`: shared CSRF contract. New `frontend/src/shared/api/csrf.ts` safely handles absent/malformed cookies without fabricating a token.
- `frontend/src/features/materials/MaterialList.tsx` and `useMaterialResource.ts`: pending-extraction polling and stable reload callback. Each response precedes the next timer; no overlapping polling requests.
- `backend/src/main/java/edu/aiplatform/announcements/domain/Announcement.java`: common create/edit sanitization, raw and stored length bounds, validation before edit assignments. `backend/pom.xml` pins new direct dependency `org.jsoup:jsoup:1.23.2`.
- `backend/src/main/java/edu/aiplatform/grading/infrastructure/EvaluationJpaEntity.java`: serialize aggregate snapshots to immutable JSON text and deserialize independent views. No schema migration is required; PostgreSQL regression confirms JSONB values remain objects.
- `aidlc-docs/construction/u14-announcements-and-notifications/functional-design/business-rules.md`: aligns BR-U14-02 with the explicitly approved policy A.

## Regression evidence

- New `features/files/api.test.ts`: decoded CSRF header, credentials, FormData boundary, progress, abort, network/error responses and no automatic upload replay. Existing client/FileUploader tests also pass.
- New `AnnouncementSanitizationTest`: mixed HTML create/edit, script/style-only empty content, length boundaries, nested payload/entities and no partial edit mutation on validation failure. Existing Markdown renderer protections/tests remain in place.
- New `EvaluationVersionIntegrationTest`: real Hibernate/PostgreSQL in disposable Testcontainers; reading does not change version, save/finalize/publish each increment once, returned versions match committed state, stale writer still conflicts, JSONB object compatibility.
- `EvaluationMappingTest`: retains independent aggregate/view checks after the mapping change.
- `GradingServiceTest`: added stale-version regression asserting 409 with no repository save, audit write or event publication.
- `MaterialList.test.tsx`: extraction transition without a user action and timer cleanup on unmount. The transition test failed before polling and passed afterward.
- `Notifications.test.tsx`: reconnect backoff 1/2/4/8/16/30 seconds, unread-count recovery and cleanup.
- `DeadlineReminderScannerTest`: stopped assignment after candidate selection and reschedule by two days, old deadline ignored and one new reminder.

## Preserved boundaries

CSRF/authentication, server authorization, optimistic concurrency, grading lifecycle, Markdown renderer sanitization/URL restrictions and CSP remain enforced. No upload retry after an ambiguous result, test endpoint, live clock adjustment, schema change or external AI call was introduced. The user's pre-existing `infra/docker-compose.local.yml` modifications were retained. No commit, push or merge was performed.

Verification results and unresolved E2E conditions are documented in `../verification/u04-u13-u14-remediation-2026-10-24.md`; they do not constitute acceptance of all units or Operations readiness.
