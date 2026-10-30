# U05 Class & Enrollment — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 20–30; FR-003, FR-005, FR-024.

CourseClass.create tạo DRAFT. OPEN không bắt buộc teacher: domain, AcademicsService và migration 27/10 đã bỏ guard. Nếu có teacher thì account vẫn phải ACTIVE và đúng role. Scope teaching dựa teacher_id hiện hành, manager dựa subject ownership, Student dựa enrollment/status; không tự cấp teaching cho manager chưa được giao.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java](../../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java)
- [backend/src/main/java/edu/aiplatform/academics/web/AcademicsController.java](../../../../backend/src/main/java/edu/aiplatform/academics/web/AcademicsController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md](../../build-and-test/2026-10-25-u05-u15-audit-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md](../../build-and-test/2026-10-25-u05-u15-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md](../../build-and-test/2026-10-26-u05-u15-followup-report.md)
