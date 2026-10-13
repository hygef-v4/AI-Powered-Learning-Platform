# Dependency revision — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

## Contracts

H = provider behavior phải sẵn sàng trước tích hợp; C = public neutral contract cho phát triển song song; E = event schema/data thật tại gate. Không đọc bảng/repository của unit khác. Không cần hoàn tất toàn wave trước mở việc khác.

| Consumer | Hard prerequisites | Contract/event integrations |
|---|---|---|
| U02/U03 | Khung tối thiểu | U01 authorization qua C, deny khi chưa có adapter thật; U03 → U07 quota-policy C cho UC 70, không hard prerequisite nền |
| U01 | U02/U03 | Audit, OTP/jobs |
| U04 | U01/U02/U03 | Tài liệu/bài của U05/U08 qua query ports |
| U05/U06/U12 | U04 + nền | U08/U09 references qua C |
| U07 | U01/U02/U03 | U13 usage checkpoint qua C; không U04/Payment hard dependency |
| U08 | U04/U06 + nền | U09 config, U12 readiness, U13 draft qua C; U05 tài liệu optional |
| U09 | U08 | Document/rubric/Quiz config |
| U11 | U09 | U13 feedback, U15 submission handling qua C |
| U14 | U09/U12 | U11 ActivityEvent port ready when monitoring enabled; U13 feedback, U15 group submission qua C |
| U15 | U11/U14/U06 | U11 ActivityQueryPort cho UC 37; U13 proposal, U16 events |
| U13 | U01/U03/U07 | Domain readers/writers U04/U08/U09/U11/U14/U15 qua C, optional U05 reference/RAG |
| U16 | U01/U04; gradebook sau U15 | U03 BackupStatusPort cho UC 58; E backup failure/domain owners, không payment event |

## Waves

| Wave | Units | Điều kiện |
|---|---|---|
| 1 — nền | U03, U02, U01, U04 | U03/U02 song song, U01 tích hợp auth, U04 theo U01. Iter 1 chỉ quản trị môn, lớp/enrollment chuyển Iter 2. |
| 2 — dữ liệu và cấu hình | U05, U06, U07, U12, U08 | U05/U06/U12 dùng U04; U07 dùng identity/settings, không payment. U08 cần rubric/bank, file và academic scope; tài liệu nguồn là tùy chọn, không Lesson prerequisite. |
| 3 — workspaces và chấm | U09, U11, U14, U15 | U09 theo U08; U11 theo U09; U14 thêm U12; U15 theo bài nộp. Manual workflow không chờ AI. |
| 4 — AI và kết quả | U13, U16 | U13 có thể mở khi identity/settings/U07 đủ, domain integrations qua neutral ports; không chờ Lesson/RAG. U16 cần owners/events, phần Admin Dashboard có thể làm Iter 1. |

## Text paths

- Manual cá nhân: U02/U03 → U01 → U04 → U06 → U08 → U09 → U11 → U15 → U16.
- Nhóm: U04 → U12; U09/U12 → U14 → U15.
- AI: U01/U03/U07 → U13; domain services cung cấp scoped context và tiếp nhận draft/proposal qua C. Personal/MCP không dựa Lesson/RAG.
- Legacy diagram unit-of-work-dependency.png/.drawio giữ lịch sử 09/10, không dùng lập lịch revision.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## UC consolidation — 66 active UC

UC 70 Manage System Settings gộp 71/78: U03 primary; U07 giữ quota service/data và US-AIC-003. UC 58 gộp 80: U16 primary dashboard; U03 giữ backup service/data và US-OPS-001. UC 37 gộp 81: U15 primary review; U11/U14 giữ activity capture. UC 76 giữ riêng credentials/provider. Story count vẫn 50 MVP + 1 extension, 15 units; không bỏ chức năng hoặc acceptance vì gộp UC. Quiz Practice activity chỉ đọc trong scope/policy cho phép, không tự thêm Gradebook.
