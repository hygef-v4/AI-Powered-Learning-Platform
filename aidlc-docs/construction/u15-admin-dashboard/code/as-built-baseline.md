# U15 Admin Dashboard — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 66; FR-024, FR-038.

Admin Dashboard có counts, backup metadata/history và GET /activity. Activity gồm biến động ACTIVE accounts hôm nay, số Assignment 7 ngày (individual/group/practice), tối đa 50 security events trong 30 ngày; ngày Asia/Ho_Chi_Minh. Các nguồn lỗi/timeout trả từng Section UNAVAILABLE/data null; no-store, chỉ ADMIN, query lạ bị từ chối. Không lộ archive/secrets hoặc cấp quyền học vụ.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityService.java](../../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityService.java)
- [backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardService.java](../../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardService.java)
- [backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java](../../../../backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md](../../build-and-test/2026-10-25-u05-u15-audit-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md](../../build-and-test/2026-10-25-u05-u15-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md](../../build-and-test/2026-10-26-u05-u15-followup-report.md)
- [aidlc-docs/construction/build-and-test/u15-verification.md](../../build-and-test/u15-verification.md)
