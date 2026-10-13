# U03 File, Job, Event & Settings — tech-stack-decisions

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 70. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## Stack giữ theo dự án

Java 17/Spring Boot 3.5 modular monolith; Next.js 16/TypeScript, PostgreSQL/Redis/RabbitMQ, Nginx/Docker Compose, U03 Drive private và U01/U16 MailPort. Dependency lock/pin và actual APIs xác minh khi code; không chọn version SDK mới trong revision tài liệu.

- UC 70 primary U03: Admin xem/sửa non-secret Settings; quota qua U07 TeacherQuotaPolicyPort có version/audit riêng. Không transaction nguyên tử Settings+quota; unavailable không báo saved.
- File private, MIME/size/purpose/scope kiểm server trước upload/download; short-lived token hoặc server stream không cấp thêm quyền. File provider ID và secrets không đưa vào log.
- Backup schedule/retention là cấu hình bắt buộc xác nhận, không tự gán số. Consistent PostgreSQL dump + version/schema + manifest và bản sao độc lập các tệp tại snapshot; bản live Drive không là backup.
- BackupRun CREATED → RUNNING → SUCCESS/FAILED; SUCCESS chỉ sau toàn bộ dump/files/encrypted upload/checksum. Retention chỉ xóa bản thành công cũ sau khi giữ tối thiểu bản tốt gần nhất; bản lỗi không thay bản tốt.
- Archive mã hóa bằng khóa giữ ngoài VPS/archive; protected AI credentials trong database vẫn là ciphertext, secret store/key recovery riêng. Lỗi upload/disk/permission retry hữu hạn, phát backup.failed sau ghi trạng thái.
- Restore runbook vào môi trường cô lập; không web production-restore hoặc MCP restore tool. Kiểm records/scope/links/XML/submissions/grades/quota; đo RPO/RTO, không cam kết zero loss.
- Jobs enqueue-after-commit qua outbox; claim/lease/dedup theo requestRef, retry bounded, terminal lỗi ghi rõ. DB job/outbox là nguồn bền vững; Redis mất không được reset quota bền vững.

## Cost và extension

AI API tổ chức/cá nhân do bên tương ứng trả, không giả định free/đã có key; infrastructure dùng resources nhóm có. Full Lesson/RAG không prerequisite; mọi embedding/provider config là extension riêng. Numeric policies D04/retention chưa giả định. Không thêm unit/container cho từng module.
