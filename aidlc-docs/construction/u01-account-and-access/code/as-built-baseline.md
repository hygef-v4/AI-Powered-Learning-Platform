# U01 Account & Access — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 01–12; FR-001, FR-002, FR-015.

Tài khoản trường cấp, OTP/kích hoạt/khôi phục, JWT/session, hồ sơ và Admin CRUD/status có controller/service. Quyền current actor/session không thay bằng hard-code UI; xem identity contract và tests. SMTP thực tế vẫn cần cấu hình, không kết luận email đã đến inbox.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/identity/application/AccountAdminService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/AccountAdminService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AccountImportService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/AccountImportService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AccountLookupService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/AccountLookupService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/ActivationService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/ActivationService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AuthorizationService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/AuthorizationService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AuthService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/AuthService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/OtpService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/OtpService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/PasswordChangeService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/PasswordChangeService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/PasswordResetService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/PasswordResetService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/ProfileService.java](../../../../backend/src/main/java/edu/aiplatform/identity/application/ProfileService.java)
- [backend/src/main/java/edu/aiplatform/identity/web/AdminAccountController.java](../../../../backend/src/main/java/edu/aiplatform/identity/web/AdminAccountController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/AuthController.java](../../../../backend/src/main/java/edu/aiplatform/identity/web/AuthController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/MeController.java](../../../../backend/src/main/java/edu/aiplatform/identity/web/MeController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/OtpFlowController.java](../../../../backend/src/main/java/edu/aiplatform/identity/web/OtpFlowController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
