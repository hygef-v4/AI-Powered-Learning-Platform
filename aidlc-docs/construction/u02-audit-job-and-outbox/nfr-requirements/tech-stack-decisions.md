# U02 Audit - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Bảng `audit_logs` | PostgreSQL + Spring Data JPA | Đã chốt |
| Lưu JSON audit | Cột `jsonb` | Đã chốt ở Application Design |
| Test | JUnit 5, Testcontainers (PostgreSQL) | Theo NFR-004 |

Messaging, worker, sweeper và scanner thuộc U03 (xem `u03-file-and-artifact/nfr-requirements/tech-stack-decisions.md`).
