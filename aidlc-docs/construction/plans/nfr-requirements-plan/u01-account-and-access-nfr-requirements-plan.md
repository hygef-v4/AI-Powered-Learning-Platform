# U01 Account & Access - NFR Requirements Plan

**Lịch sử plan baseline trước 2026-10-08**: giữ nguyên các bước đã hoàn thành và mã UC tại thời điểm đó. Thiết kế hiện hành và revision code plan theo [current SRS contract](../../current-srs-contract.md); task đồng bộ ở [construction-sync](../construction-sync-2026-10-08.md), không dùng checklist cũ như phạm vi mới.

## 1. Nguồn

- Functional Design đã duyệt: `construction/u01-account-and-access/functional-design/`.
- Requirements: NFR-001…005, SEC-001…009, REL-001…010.
- Security Baseline và Resiliency Baseline đang bật.

## 2. Việc cần làm

- [x] Đọc Functional Design và các NFR/SEC/REL liên quan tới U01.
- [x] Liệt kê điểm chưa chốt: kiểu phiên, thời hạn phiên, MFA, thuật toán băm, nguồn kiểm mật khẩu lộ, ngưỡng rate limit OTP, dịch vụ email, hành vi khi Redis lỗi.
- [x] Hỏi người dùng qua giao diện chọn đáp án, ghi vào `../nfr-requirements-questions/u01-account-and-access-nfr-requirements-questions.md`.
- [x] Làm rõ các câu trả lời mâu thuẫn (MFA, JWT thu hồi trễ, phạm vi bỏ SEC-002, Redis lỗi).
- [x] Đồng bộ Functional Design và `SEC-002` theo quyết định.
- [x] Tạo `nfr-requirements.md`.
- [x] Tạo `tech-stack-decisions.md`.
- [x] Ghi compliance và ngoại lệ.
- [x] Trình duyệt NFR Requirements trước khi sang NFR Design.
