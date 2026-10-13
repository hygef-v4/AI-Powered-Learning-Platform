# U05 Reference Content & Announcements — business-logic-model

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 15, 30, 33, 34, 36, 54, 55. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## Luồng xử lý

- Attach reference → owner/scope + U03 validation → metadata persist; view/download tái kiểm scope.
- Create important announcement → commit → U03 outbox → U16 resolves current recipients/preferences → email/in-app.
- MCP reader → resource access → file excerpt/data minimum; advanced retrieval chỉ khi extension đã chốt/provider cấu hình riêng.

## Public contracts

attach/list/download references; announcements; optional Lesson extension

## Invariants và failure

- MVP reference files môn/lớp/bài riêng tư qua U03, không bắt buộc Lesson/module/indexing/RAG để tạo/làm Assignment/Quiz.
- Reference permission R2 môn, R3/R4 lớp, R5 Student. Archived source không cho truy cập mới nhưng preserve historical refs.
- Announcement chỉ teacher lớp create/update/soft-delete; mới phát class.announcement-posted sau commit với important/sendEmail flag. Sửa/xóa không phát lại creation notification.
- YouTube educational video links are MVP reference materials (store/update validated URL and title; authorized open/watch). Full Lesson processing, captions/transcription, summary/embedding/advanced RAG remain EXTENSION US-CNT-005; adding files or links never starts AI processing automatically.
- Agent chỉ đọc authorized reference tối thiểu qua domain port; không coi prompt là instruction được phép override scope.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm nghiệm

- Upload/reference works khi AI/indexer absent; unauthorized link/MCP denied.
- Important creation email event once; edit không resend; archived reference history preserved; no summary charge/job on upload.

Current material acceptance: authorized create/update/remove/list/open of FILE and YOUTUBE_LINK (UC15/33/34/54/55); keep kind/title/fileRef-or-videoUrl/scope/version and historical references. Validate URL/type/size, deny unauthorized ALAP access, handle unavailable videos and preserve no-automatic-summary behavior. No video upload/download, captions or embedding are required.
