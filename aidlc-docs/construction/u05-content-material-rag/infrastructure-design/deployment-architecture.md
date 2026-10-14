# U05 Reference Content & Announcements — deployment-architecture

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 62, 63, 60, 61, 64, 58, 59. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## Mapping triển khai

Logical unit chạy trong backend/worker chung; data PostgreSQL theo owner, cache/rate Redis không nguồn durable business/quota. Package legacy mục tiêu review `content`; không tạo deployable riêng.

Nguồn hạ tầng: [shared-infrastructure](../../shared-infrastructure.md).

- Attach reference → owner/scope + U03 validation → metadata persist; view/download tái kiểm scope.
- Create important announcement → commit → U03 outbox → U16 resolves current recipients/preferences → email/in-app.
- MCP reader → resource access → file excerpt/data minimum; advanced retrieval chỉ khi extension đã chốt/provider cấu hình riêng.

## Deploy/rollback/secret

- Docker image theo commit SHA, không latest; chỉ reverse proxy public; migration additive có data mapping/backup trước thay đổi và rollback compatibility.
- Credentials protected field với encryption key ngoài DB; không plaintext Settings/environment dump/logs. Backup archive encryption key và recovery secrets lưu riêng.
- Ready/health của dependencies rõ; ports down không trả empty/0/SUCCESS giả. AI unavailable không chặn manual work; event retry/lease tối đa hữu hạn.
- Trước demo chạy isolated restore, negative authorization, migration validation và checklist unit; runtime chưa được chứng minh bởi revision docs.

## Unit integration risks

- Upload/reference works khi AI/indexer absent; unauthorized link/MCP denied.
- Important creation email event once; edit không resend; archived reference history preserved; no summary charge/job on upload.

Current material acceptance: authorized create/update/remove/list/open of FILE and YOUTUBE_LINK (UC62/60/61/58/59); keep kind/title/fileRef-or-videoUrl/scope/version and historical references. Validate URL/type/size, deny unauthorized ALAP access, handle unavailable videos and preserve no-automatic-summary behavior. No video upload/download, captions or embedding are required.
