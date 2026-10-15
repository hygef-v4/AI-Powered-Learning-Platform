# U05 Reference Content & Announcements — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 62, 63, 60, 61, 64, 58, 59. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- GET/POST/PATCH /api/v1/subjects/{id}/materials; class equivalents; /api/v1/classes/{id}/announcements.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- MVP reference files môn/lớp/bài riêng tư qua U03, không bắt buộc Lesson/module/indexing/RAG để tạo/làm Assignment/Quiz.
- Reference permission R2 môn, R3/R4 lớp, R5 Student. Archived source không cho truy cập mới nhưng preserve historical refs.
- Announcement chỉ teacher lớp create/update/soft-delete; mới phát class.announcement-posted sau commit với important/sendEmail flag. Sửa/xóa không phát lại creation notification.
- YouTube educational video links are MVP reference materials (store/update validated URL and title; authorized open/watch). Full Lesson processing, captions/transcription, summary/embedding/advanced RAG remain EXTENSION US-CNT-005; adding files or links never starts AI processing automatically.
- Agent chỉ đọc authorized reference tối thiểu qua domain port; không coi prompt là instruction được phép override scope.

## UI acceptance

- Upload/reference works khi AI/indexer absent; unauthorized link/MCP denied.
- Important creation email event once; edit không resend; archived reference history preserved; no summary charge/job on upload.

Current material acceptance: authorized create/update/remove/list/open of FILE and YOUTUBE_LINK (UC62/60/61/58/59); keep kind/title/fileRef-or-videoUrl/scope/version and historical references. Validate URL/type/size, deny unauthorized ALAP access, handle unavailable videos and preserve no-automatic-summary behavior. No video upload/download, captions or embedding are required.
