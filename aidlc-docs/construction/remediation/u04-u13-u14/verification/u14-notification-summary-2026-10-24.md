# U14 notification summary verification — 2026-10-24

Branch: `fix-u04-u13-u14`. User approved the concrete recommendation with “ok sửa cho tôi đi”. Plan: `../plans/u14-notification-summary-plan.md`. Code summary and complete copy catalog: `../implementation/notification-summary-refinement.md`.

## Scope

New event notifications use fixed action text for each of the12 notification types. Legacy rows containing the exact generic sign-in summary get the corresponding display text on feed read. Specific summaries, stored entities/read state/links/timestamps, recipient routing, SSE behavior and email's separate minimal sign-in template are preserved. New reminders share the properly spaced “24 giờ” wording; reminder selection/timing are unchanged. No frontend or schema change and no database rewrite.

## Automated verification

RED command from backend: `mvn -B -Dtest=NotificationEventServiceTest,NotificationServiceTest,EmailDeliveryServiceTest test`.39 tests ran,15 expected assertion failures,zero errors/skips before the production change:12 legacy-summary cases and3 event-copy/role/privacy cases. Email and custom-summary preservation checks already passed.

GREEN command from backend: `mvn -B -Dtest=Notification*Test,RecipientResolverTest,EmailDeliveryServiceTest,DeadlineReminderScannerTest test`. **75 passed,zero failures/errors/skips**, BUILD SUCCESS, finished10:29:45+07 on24October2026.

| Suite | Passed |
| --- | ---: |
| NotificationAccountLookupTest | 3 |
| DeadlineReminderScannerTest | 9 |
| EmailDeliveryServiceTest | 14 |
| NotificationEventServiceTest | 3 |
| NotificationServiceTest | 22 |
| RecipientResolverTest | 10 |
| NotificationEventListenerTest | 5 |
| NotificationTopologyContextTest | 2 |
| NotificationControllerTest | 7 |
| Total | 75 |

New feed regressions cover all12 legacy types, fixed text bounds, unchanged identity/link/createdAt/readAt and source entity, no mail/SSE side effects and preservation of specific summaries. New event tests verify the teaching decision action, Admin backup handling, grade-payload privacy and unchanged resolved delivery metadata. Email privacy test now supplies private score/comment text and verifies it stays out of the independent sign-in email. No full-suite rerun is claimed; the earlier full verify is historical evidence for the previous remediation.

Supporting local logs: ignored `.tmp/u14-summary-red.log`, `u14-summary-green.log`, `u14-summary-docker-build.log`. Results are recorded here so handoff does not depend on ignored logs.

## Local build and UI verification

Production Docker image build succeeded with the existing backend Dockerfile. Recreated only local backend/worker with the existing Compose files and `.env.local`; both reported healthy after Java startup (worker briefly reported unhealthy before startup completed and recovered). No configuration/volume reset, frontend rebuild or migration.

Authenticated UI before/after confirmed the same historical records changed display copy:

| Audience / existing record | After | Preserved evidence |
| --- | --- | --- |
| Teacher leader-change request,23October22:41:31 | Xem yêu cầu để phê duyệt hoặc từ chối. | Unread count1 and original timestamp unchanged |
| Student grade notification,23October22:37:42 | Xem kết quả chấm bài của bạn. | Unread count5 and original timestamp unchanged; no score/comments |
| Student assignment notification,24October10:08:36 | Xem nội dung và hạn nộp bài. | Same existing event/time |
| Student class announcement,24October10:04:28 | Xem nội dung thông báo từ giảng viên. | Same existing event/time |
| Student Quiz notification,23October18:34:01 | Mở Quiz để xem và bắt đầu luyện tập. | Same existing event/time |

Evidence directory: `E:/FPTUni/sem9_fa26/test_report/reports/u14-notification-summary-2026-10-24` in the external report directory. `teacher-before.png`, `teacher-after.png`, `student-after.png` and `ui-readback.json` record visible UI, not hidden application state. Existing items were not clicked/marked read, and no new notification was created for this check. New event creation and Admin-specific copy are covered by focused backend regressions; no runtime backup failure or separate subject-manager-teaching fixture was induced.

Artifact U14-13 note was supplemented with the new grade-summary/privacy evidence and verified after reload. `checklist-results.json` and `artifact-after.png` in the evidence directory preserve the unchanged49 passed /0 failed /1 skipped dispositions; this is a readback of the existing checklist, not a claim of repeating all50 workflows.

## Boundaries and remaining acceptance

No extra QA events, real backup failure or live clock/DB modification is needed for this check. Admin copy is verified by focused tests; a real backup failure will not be induced. Existing user-owned Compose overlay and all prior dirty changes are retained. No commit/push, external deployment or Operations acceptance. The existing U04/U13/U14 artifact remains49 passed /0 failed /1 skipped, with U14-15 real rescheduled/stopped-assignment reminder observation pending; summary refinement does not satisfy that time-dependent gate.

Applicable scoped extension checks: privacy maintained with fixed enum copy and unchanged authorized targets; email remains minimal; recipient/self-feed/security regressions passed. Resiliency routing/retries/SSE/scheduler remain unchanged. Production deployment hardening/load acceptance are N/A; PBT remains disabled per project state.

Later manual follow-up at16:40+07 closed U14-15 expected behavior using the user's +5minute rescheduling variant and stopped-assignment confirmation. The exact +2day manual wait was not performed. Latest scoped checklist50/0/0 and final snapshot/proof are documented in `u04-u13-u14-remediation-2026-10-24.md`; the49/0/1 above is the earlier summary-refinement handoff.
