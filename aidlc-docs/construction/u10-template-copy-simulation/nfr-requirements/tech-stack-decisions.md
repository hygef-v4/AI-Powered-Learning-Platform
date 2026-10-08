# U10 Template & Copy - Tech Stack Decisions

**Bản tài liệu 2026-10-08**: UC 53, 54; primary stories: US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Diff văn bản | `java-diff-utils` (`io.github.java-diff-utils`) | Diff theo dòng, nhẹ |
| Diff thành phần | So khớp theo `question_id`/`lineage_id` hoặc hash `definition` của câu riêng, rồi so thứ tự và điểm | Không cần thư viện |
| Lưu | PostgreSQL + JPA | Như các unit trước |
| Frontend | Next.js + Tailwind, component tự viết | Như các unit trước |
