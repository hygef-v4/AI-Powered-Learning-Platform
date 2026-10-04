# U02 Audit - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Bảng `audit_logs` | PostgreSQL + Spring Data JPA | Đã chốt |
| Lưu JSON audit | Cột `jsonb` | Đã chốt ở Application Design |
| Test | JUnit 5, Testcontainers (PostgreSQL) | Theo NFR-004 |

Messaging, worker, sweeper và scanner thuộc U03 (xem `u03-file-and-artifact/nfr-requirements/tech-stack-decisions.md`).
