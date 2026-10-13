# U05 Reference Content & Announcements — domain-entities

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 15, 30, 33, 34, 36, 54, 55. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- ReferenceMaterial: id, subjectId/classId/assignmentId scoped target, fileId(U03), title/status/version, uploaderId/timestamps.
- ClassAnnouncement: id/classId/authorId/title/content/important/sendEmail/version/status/timestamps; event chứa id/classId/flags/version, không raw confidential data.
- Lesson/embedding entities chỉ migration extension riêng nếu approved; không required relationship trong core.

## Boundary

attach/list/download references; announcements; optional Lesson extension

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- MVP reference files môn/lớp/bài riêng tư qua U03, không bắt buộc Lesson/module/indexing/RAG để tạo/làm Assignment/Quiz.
- Reference permission R2 môn, R3/R4 lớp, R5 Student. Archived source không cho truy cập mới nhưng preserve historical refs.
- Announcement chỉ teacher lớp create/update/soft-delete; mới phát class.announcement-posted sau commit với important/sendEmail flag. Sửa/xóa không phát lại creation notification.
- YouTube educational video links are MVP reference materials (store/update validated URL and title; authorized open/watch). Full Lesson processing, captions/transcription, summary/embedding/advanced RAG remain EXTENSION US-CNT-005; adding files or links never starts AI processing automatically.
- Agent chỉ đọc authorized reference tối thiểu qua domain port; không coi prompt là instruction được phép override scope.

Current material acceptance: authorized create/update/remove/list/open of FILE and YOUTUBE_LINK (UC15/33/34/54/55); keep kind/title/fileRef-or-videoUrl/scope/version and historical references. Validate URL/type/size, deny unauthorized ALAP access, handle unavailable videos and preserve no-automatic-summary behavior. No video upload/download, captions or embedding are required.
