# U14 Announcements & Notifications — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 63–65; FR-011, FR-019, FR-023.

announcements/notifications có CRUD, recipients, preference, in-app/SSE/email/reminder/retention. OTP gửi qua U01; U14 xử lý email học tập/cảnh báo. Remediation 23–26/10 giữ historical evidence; SENT chỉ là SMTP chấp nhận, không bảo đảm exactly-once hoặc inbox. Scope/privacy/version vẫn do backend kiểm.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/announcements/application/AnnouncementService.java](../../../../backend/src/main/java/edu/aiplatform/announcements/application/AnnouncementService.java)
- [backend/src/main/java/edu/aiplatform/announcements/web/AnnouncementController.java](../../../../backend/src/main/java/edu/aiplatform/announcements/web/AnnouncementController.java)
- [backend/src/main/java/edu/aiplatform/announcements/web/MyAnnouncementController.java](../../../../backend/src/main/java/edu/aiplatform/announcements/web/MyAnnouncementController.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/EmailDeliveryService.java](../../../../backend/src/main/java/edu/aiplatform/notifications/application/EmailDeliveryService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationEventService.java](../../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationEventService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationPreferenceService.java](../../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationPreferenceService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationService.java](../../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationService.java)
- [backend/src/main/java/edu/aiplatform/notifications/web/NotificationController.java](../../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationController.java)
- [backend/src/main/java/edu/aiplatform/notifications/web/NotificationPreferenceController.java](../../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationPreferenceController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/u14-build-and-test-plan.md](../../build-and-test/u14-build-and-test-plan.md)
- [aidlc-docs/construction/build-and-test/u14-verification.md](../../build-and-test/u14-verification.md)
