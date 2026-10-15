# U05 Reference Content & Announcements — nfr-requirements

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 62, 63, 60, 61, 64, 58, 59. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## Required controls

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Validation MIME/XML/schema/IDs/filter allowlists phù hợp unit, bounded payloads và rate limit phía server. Không log secret, raw prompt hoặc dữ liệu học vụ nhạy cảm.
- Truy vấn phân trang/index theo owner/resource/time, timeout hữu hạn mọi port/provider; không promise external API p95 như query local.
- Target baseline local APIs p95 500ms tại 100 concurrent/1000 accounts là mục tiêu cần benchmark, chưa có evidence; heavy files/AI/backup chạy worker và status queries.
- Session/password/header controls giữ SEC-001–006; quota/backup/credential protected riêng, không yêu cầu mã hóa toàn DB.

## Unit-specific NFR risks

- MVP reference files môn/lớp/bài riêng tư qua U03, không bắt buộc Lesson/module/indexing/RAG để tạo/làm Assignment/Quiz.
- Reference permission R2 môn, R3/R4 lớp, R5 Student. Archived source không cho truy cập mới nhưng preserve historical refs.
- Announcement chỉ teacher lớp create/update/soft-delete; mới phát class.announcement-posted sau commit với important/sendEmail flag. Sửa/xóa không phát lại creation notification.
- YouTube educational video links are MVP reference materials (store/update validated URL and title; authorized open/watch). Full Lesson processing, captions/transcription, summary/embedding/advanced RAG remain EXTENSION US-CNT-005; adding files or links never starts AI processing automatically.
- Agent chỉ đọc authorized reference tối thiểu qua domain port; không coi prompt là instruction được phép override scope.

## Test evidence cần có

- Upload/reference works khi AI/indexer absent; unauthorized link/MCP denied.
- Important creation email event once; edit không resend; archived reference history preserved; no summary charge/job on upload.

Security enabled subset 03/04/05/08/09/12/15: constraints auth/input/audit/secret/failure được thể hiện, chưa runtime verified. Resiliency enabled subset 04/06/10: deployment/health/timeouts giữ; rule khác N/A theo state, PBT disabled.

Current material acceptance: authorized create/update/remove/list/open of FILE and YOUTUBE_LINK (UC62/60/61/58/59); keep kind/title/fileRef-or-videoUrl/scope/version and historical references. Validate URL/type/size, deny unauthorized ALAP access, handle unavailable videos and preserve no-automatic-summary behavior. No video upload/download, captions or embedding are required.
