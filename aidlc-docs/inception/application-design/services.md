# Service orchestration — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

## Assignment

Educator tạo ASSIGNMENT với submissionMode và diagramEnabled, cấu trúc/rubric → review/publish. Student autosave tài liệu → nộp trong hạn; chỉ Assignment PRACTICE đã SUBMITTED mới request feedback UC41. U11/U14 giữ snapshot, U15 nhận submission. Group do leader nộp, member chỉ phần được phép.

## Quiz

Educator tạo QUIZ độc lập Lesson, MCQ keys/config snapshot → Student làm/nộp → scorer xác định. Tạm Practice theo D01, không tự thêm Gradebook hoặc proctored behavior.

## Direct AI

Kiểm actor/target/version/policy → user chọn nguồn → U13 resolve connection. Shared: educator-only, U07 reserve quota → provider adapter → validate output, reconcile provider usage. Personal: owner-only, không shared grant; vẫn rate/usage controls. Timeout usage chưa biết ghi unknown, không auto-fallback hoặc retry không giới hạn. Domain owner lưu draft/feedback/proposal, không provider trực tiếp ghi official grade.

## Agent qua MCP

Agent xác thực scoped credential → U13 tool auth → domain service kiểm scope/state/version → minimal context trả agent. Agent gọi model của mình → gửi kết quả về draft/private feedback/proposal. ALAP validate/provenance, không biết token thật nếu client không cung cấp. Human quyết định official trên ALAP.

## Notifications và failure

Domain commit → U16 notifications/email; lỗi email không rollback bài. Không payment.paid event. AI failure không mất draft/submission; stale result reject. Thu hồi kết nối/policy/state phải kiểm mỗi lần gọi.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## Luồng các UC gộp

Admin → UC14 View Settings (masked read) / UC15 Edit Setting → U03 orchestrator → U07 ITeacherQuotaPolicy hoặc U13 IAiConnectionAdministration → owner validate/version/audit → masked result. UI chung nhưng credentials giữ ở U13 protected records, không lưu raw key trong Settings. Provider chưa sẵn sàng báo unavailable, không false success. Hết quota không tự đổi kết nối.

Admin → UC 66/U16 → U03 IBackupStatus → metadata đã sanitize. BackupStatusUnavailable khác Failed/Success. Scheduler/restore vẫn U03/người vận hành; không thực hiện restore qua Dashboard.

Teacher → UC 42/U15 → kiểm lớp/phân công/attempt/member và policy → U11 IActivityQuery (sự kiện nhóm ghi từ U14) → timeline. Monitoring disabled/no data/unavailable phân biệt rõ; không tự kết luận cheating hoặc đổi điểm. Với Quiz Practice chỉ xem lượt được phép, không tạo Gradebook.
