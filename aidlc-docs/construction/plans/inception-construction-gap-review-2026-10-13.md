> Historical gap review trước sync: các findings dưới đây ghi tình trạng lúc review. Construction current docs đã revision; xem README và plans/construction-66-uc-sync-2026-10-13.md. Runtime chưa triển khai theo scope mới.

# Đối chiếu Inception và Construction — 2026-10-13

## Kết luận

**Chưa đồng bộ.** Inception hiện hành là 66 UC, 50 MVP stories + 1 extension, 15 logical units và 39 FR IDs. Construction vẫn mô tả baseline 73 UC/51 stories/15 units ngày 2026-10-09. Sửa đường dẫn tới docs/history/use-cases-73.md chỉ bảo toàn nguồn lịch sử, không đồng bộ thiết kế nghiệp vụ.

Đây là review tài liệu, không sửa Construction design/code, không thay approval/checklist và không kiểm runtime. Tổng số story cùng là 51 không chứng minh khớp: baseline còn US-PAY/Code trong khi scope mới có US-AIC/backup/activity và phân biệt extension.

## Các điểm lệch và hướng sửa

| Mức | Nội dung | Bằng chứng Construction | Thiết kế cần khớp Inception |
|---|---|---|---|
| P1 | Catalog và ownership cũ | [current-srs-contract](../current-srs-contract.md) vẫn ghi 73 UC; U03 có 70/71, U07 payment, U13 không primary UC | Thay active map 66 UC; U03 primary 70, U07 primary 77, U13 primary 74/75/76; UC 58 gồm backup, UC 37 gồm activity. Ghi 71/78/80/81 đã gộp, không renumber IDs. |
| P1 | Mua credit/payment còn trong scope | [U07 business rules](../u07-payment-credit/functional-design/business-rules.md), [U07 code plan](code-generation-plan/u07-payment-credit-code-generation-plan.md) còn ví/gói/PayOS/HOLD | Revision U07 thành Usage/Teacher Quota, policy/reserve/reconcile token và provenance; không bán token, không cấp quota chung Student, không auto fallback personal. |
| P1 | AI/MCP/personal connection chưa được mô tả | [U13 rules](../u13-ai-code-execution/functional-design/business-rules.md) còn Gemini/credit, Judge0, không primary UC, Admin bị chặn mọi AI | U13 Connections & MCP: 3 provider API, protected shared/personal credentials, test synthetic/revoke, authenticated tools; Admin được test cấu hình, không học vụ. MCP usage externallyReported/unavailable, không đo giả. |
| P1 | Mô hình bài cũ | [U08 entities](../u08-assessment-core-publication/functional-design/domain-entities.md) type TEXT_ESSAY/DIAGRAM_ESSAY/CODE_LAB/GROUP_ASSIGNMENT/MULTIPLE_CHOICE_QUIZ | Chỉ ASSIGNMENT/QUIZ; submissionMode và diagramEnabled độc lập; revision document model, DTO/schema, snapshots/validation và authoring U09/workspace U11/U14/chấm U15. |
| P1 | Code execution và bank Code chưa bỏ | [U06 entities](../u06-rubric-question-bank/functional-design/domain-entities.md) còn CODE; [U13 plan](code-generation-plan/u13-ai-code-execution-code-generation-plan.md) còn Judge0/tests/7 ngôn ngữ | Loại thiết kế Code Lab/test runner/verification khỏi active scope; bank Quiz MCQ, rubric Assignment. Giữ baseline cũ để truy vết, không xóa lịch sử. |
| P1 | Backup bị loại rõ ràng | [shared infrastructure](../shared-infrastructure.md) còn “không backup” và mất toàn bộ dữ liệu; U01 deployment cũng nói dữ liệu không khôi phục được | U03 encrypted consistent backup + file copies/manifest/checksum/status/retention, restore runbook/evidence; U16 backup status trên UC 58 và failure email; secrets giải mã riêng, không coi Drive live storage là backup. |
| P1 | Activity và review chưa có thiết kế tương ứng | Thiết kế domain còn baseline; tìm không thấy activityMonitoringEnabled/ActivityEvent trong Construction được rà | U09 policy snapshot, U11 ActivityEvent cá nhân/Quiz/nhóm qua U14 port, U15 review trong UC 37; disabled/no data/unavailable rõ; không tự phán cheating/chỉnh điểm, không thêm Gradebook Quiz. |
| P2 | Quiz/Lesson và RAG còn ràng buộc cũ | U08 lesson_id cho Quiz; U13 draft cần nguồn INDEXED; U05 summary/embedding gắn HOLD credit | Quiz độc lập Lesson; reference files MVP, full Lesson/YouTube/RAG extension; soạn/làm/nộp/chấm thủ công không chờ AI/indexing. |
| P2 | Phản hồi sớm chưa khớp | U13 BR-U13-23 chỉ Student Practice Text/Diagram đã nộp, credit của Student | UC 29 feedback Practice đã nộp; UC 79 feedback bản nháp/version, graded cần teacher policy, personal API/MCP, private feedback không official grade. |
| P2 | Email chỉ đồng bộ một phần | U16 BR-U16-06 cấm email announcement; BR-U16-08 nói Admin không có notification; BR-U16-10 chỉ bốn loại email học tập | Thêm important announcement theo lựa chọn giảng viên, Admin backup failure; cập nhật event schema/recipients/preferences/retry/dedup và UI. Giữ OTP activation/recovery đúng luồng U01. |
| P2 | Contracts/dependency/code kế hoạch còn nguồn cũ | Các unit và code-generation plans tham chiếu shared contract 09/10, US-PAY/Code và Settings70/71 | Revision public ports, owner boundaries, migrations, routes/DTO, negative tests/integration cases, dependency/gates; approval cũ không được xem là duyệt revision mới. |

## Những nền tảng có thể giữ sau rà soát

- 15 logical units và modular monolith, không tạo 15 microservices.
- Bốn vai trò; current subject/class ownership và enrollment checks; Admin không dạy/chấm, Subject Manager cần phân công dạy để chấm lớp.
- Grade/publication do người có quyền quyết định; Practice không vào Gradebook.
- Group sections/leader submit, snapshots/version/audit, autosave và nền file/jobs.
- U16 đã có after-commit, idempotent event, retry hữu hạn, email không chứa điểm số; cần mở rộng đúng recipients/events mới.

Đây là các nguyên tắc tương thích, không tuyên bố toàn bộ unit đã khớp hoặc code đã triển khai.

## Thứ tự revision đề xuất

1. Active current-srs-contract và ownership/FR-story mapping làm nguồn chung 66 UC; archive baseline cũ.
2. Shared infrastructure, U03 Settings/backup, U07 quota/usage, U13 connection/MCP; loại contracts/payment/code không còn scope.
3. U05 reference/extension, U06 MCQ/rubric, U08/U09 hai loại bài và policy; U11/U14 workspaces/feedback/activity; U15 chấm/review.
4. U16 backup dashboard/notifications; cập nhật NFR/infrastructure/dependency và code-generation plans cho các unit liên quan.
5. Kiểm UC/story owners, FR traceability, field/status/ports/event schema và test scenarios. Runtime/contracts/code revision là công việc tiếp theo, không tự đánh completed.

D01–D04 vẫn còn mở: Quiz Graded, Group Practice, quyền AI đọc cả bài nhóm và quota số cụ thể. Giữ defaults trong Inception, không biến review thành quyết định mới. D05 đã được người dùng bổ sung scope tối thiểu.

## Phạm vi kiểm tra

Đối chiếu current shared contract, Inception catalog/unit map/decisions, các business rules/entities/code plans bị tác động trực tiếp; tìm kiếm rộng Construction về types cũ, credit/Judge0, backup/activity/MCP và email. Không suy trạng thái đồng bộ chỉ từ tên folder hay số lượng files; các findings có nguồn tài liệu rõ ở bảng trên. Không đọc lại từng dòng của toàn bộ baseline hoặc kiểm chứng triển khai thực tế.
