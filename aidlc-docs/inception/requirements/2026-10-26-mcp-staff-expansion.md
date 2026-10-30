# MCP cho Teacher, Subject Manager và Admin

**Superseded role scope 2026-10-30:** Admin MCP removed; Student own submitted Practice MCP restored by explicit user instruction. This document preserves historical staff-expansion scope; current requirements are in 2026-10-30-test-remediation-student-mcp.md.

## Trạng thái và căn cứ

Yêu cầu triển khai: "giúp tôi sửa theo những gì đề xuất và đã chốt". Phạm vi sản phẩm dưới đây kế thừa trao đổi và quyết định loại Student khỏi MCP; kế hoạch kỹ thuật cần được duyệt trước Code Generation theo CLAUDE.md.

Căn cứ hiện trạng: `../../construction/build-and-test/2026-10-26-mcp-role-capability-review.md`, policy và các MCP provider trong mã nguồn. Repo brownfield Java 17/Spring Boot 3.5.6, PostgreSQL/Flyway, Next.js/React/TypeScript. Có thay đổi chưa commit từ đợt MCP readiness; phải bảo toàn.

## Phạm vi đã chốt

| ID | Yêu cầu |
|---|---|
| R01 | MCP chỉ nhận tài khoản ACTIVE có role TEACHER, SUBJECT_MANAGER hoặc ADMIN. Student không tạo/quản lý token MCP và token đã cấp cho Student bị từ chối ở lần gọi kế tiếp. |
| R02 | Tách quyền MCP khỏi AI_PERSONAL; Student tiếp tục dùng API cá nhân/Practice AI trên giao diện. Admin không được cấp quyền API cá nhân hay quyền học vụ do thay đổi MCP. |
| R03 | Teacher tìm và soạn nội dung/chấm đề xuất trong lớp được phân công. Subject Manager soạn nội dung/ngân hàng câu hỏi trong môn phụ trách, và chấm trong lớp được phân công dạy. |
| R04 | Agent tìm được lớp, môn, assignment, quiz và học liệu được phép; lọc, phân trang và lấy ID mà không cần người dùng sao chép thủ công. |
| R05 | Tạo assignment DRAFT kèm đề xuất nội dung/hướng dẫn/các mục/rubric và tài liệu tham khảo; trả assignmentId, proposalId, version. |
| R06 | Tạo quiz DRAFT kèm cài đặt, tham chiếu học liệu và đề xuất câu hỏi/đáp án/giải thích; trả quizId, proposalId, version. |
| R07 | Subject Manager tìm câu hỏi ngân hàng, đọc ngữ cảnh và lưu đề xuất câu hỏi cấp môn, tái sử dụng luồng duyệt hiện có. |
| R08 | Agent liệt kê bài nộp và đọc đầy đủ ngữ cảnh chấm: evaluationId/version, submissionRef/hash, rubric snapshot, nội dung có block ID và schema đề xuất; hỗ trợ cá nhân và tài liệu nhóm gốc. |
| R09 | Đọc ngữ cảnh và lưu đề xuất chấm theo lô; kết quả từng bài độc lập, thử lại không ghi trùng, một bài lỗi không rollback các bài thành công khác. |
| R10 | Đọc trạng thái đề xuất, tổng hợp học tập và phân bố điểm trong lớp được phân công; dữ liệu tổng hợp phải có phạm vi và thời điểm tính. |
| R11 | Admin quản lý token MCP riêng; đọc báo cáo sử dụng AI, chính sách/quota theo giáo viên, trạng thái kết nối được che và dashboard hệ thống tổng hợp. |
| R12 | Cập nhật UI: Student không thấy hoặc gọi API Agent MCP; Teacher/Subject Manager dùng phần kết nối hiện tại; Admin có trang Agent MCP trong shell Admin. |
| R13 | Cập nhật tài liệu, schema và kiểm thử role/token/scope, tạo mới, retry, snapshot cũ, batch và tương thích các tool giữ lại. |

## User stories và tiêu chí chấp nhận

- US1 (R01–R03, R12): Student mở Kết nối AI vẫn tải được API cá nhân, không gọi MCP credentials; bearer Student cũ bị từ chối. Teacher/Manager vẫn quản lý token; Admin tạo/thu hồi token ở trang riêng.
- US2 (R04–R07): Teacher yêu cầu tạo assignment/quiz cho lớp theo tên; agent tự tìm scope/học liệu, tạo một DRAFT và đề xuất có thể xem/áp dụng trên giao diện. Gửi lại cùng requestRef trả cùng đối tượng; khác nội dung cùng requestRef bị conflict. Manager làm tương tự cho môn và ngân hàng.
- US3 (R08–R10): Teacher yêu cầu chấm các bài chưa chấm; agent tìm bài cá nhân/nhóm, lấy đúng snapshot, lưu đề xuất theo lô và tiếp tục sau lỗi một phần. Giáo viên duyệt theo luồng hiện có; không tự công bố điểm.
- US4 (R11): Admin yêu cầu tổng hợp tình hình dùng AI/hạn mức/trạng thái hệ thống; chỉ nhận dữ liệu báo cáo được phép, không nhận token/provider key hay bài nộp học viên.

## Quyết định kỹ thuật đề xuất

1. Mở rộng MCP provider trong từng module và tái sử dụng application service/contract hiện có. Không tạo một service có quyền truy cập trực tiếp toàn bộ bảng học vụ.
2. Thêm action MCP_SELF cho ba role; dùng trong list/create/revoke credential, tách khỏi AI_PERSONAL. Xác thực bearer kiểm current role/account status mỗi lần. Không cần xóa token Student khỏi DB để chúng hết hiệu lực; xác thực từ chối ngay.
3. Tạo mới trả bản DRAFT và đề xuất chờ xem/áp dụng. Tái sử dụng validator, quyền, kiểm trạng thái scope/học liệu và giới hạn hiện có của assignment/quiz.
4. Thêm lưu idempotency cho hai thao tác tạo mới và bank proposal; khóa accountId + operation + requestRef, fingerprint nội dung và response chứa ID. Dữ liệu idempotency và mutation cùng transaction; kiểm quyền hiện tại trước khi replay kết quả.
5. Agent dùng mô hình của agent để soạn/chấm. MCP đọc ngữ cảnh và lưu kết quả; các tool mới không tự gọi provider của nền tảng.
6. Batch MCP là một tool domain nhận items; không đổi MCP JSON-RPC để nhận JSON-RPC batch. Không thêm hàng đợi/job status khi công việc được agent làm và các tool trả ngay.
7. Các tool mới dùng page mặc định 0, size mặc định 20/tối đa 100; batch tối đa 20 item và vẫn chịu giới hạn tham số 64 KB. Kết quả phải nằm dưới 256 KB, giới hạn nội bộ 192 KB để có chỗ cho envelope.
8. Ngữ cảnh lớn trả manifest/continuation tại ranh giới block, lặp lại hash/version; không coi nội dung cắt bớt là ngữ cảnh đầy đủ. Đọc tiếp học liệu có version, offset, limit tối đa 32.000 ký tự; giữ tương thích lời gọi chỉ có materialId.
9. Admin có token nhưng chỉ thấy tool báo cáo của Admin và get_my_capabilities; không thấy công cụ soạn/chấm hoặc đọc học liệu học vụ. Admin dùng credential UI riêng, mặc định READ_ONLY.

## Danh mục tool mục tiêu

Tên trong bảng là contract đề xuất cần duyệt cùng kế hoạch. T/M = Teacher/Subject Manager theo scope thực tế, M = Subject Manager của môn, A = Admin.

| Tool | Role | Input chính | Kết quả |
|---|---|---|---|
| get_my_capabilities | T/M/A | Không có | Role, access token, tool và quyền hiện tại; không trả secret |
| list_my_classes | T/M | query, status, page, size | items có classId/code/title/subjectId, total/page/size |
| list_my_subjects | T/M | query, page, size | Môn từ lớp được giao hoặc môn phụ trách, nhãn quyền tương ứng |
| list_assignments | T/M | classId hoặc subjectId, status, query, page, size | Danh sách assignment có scope, lịch, version |
| list_quizzes | T/M | classId hoặc subjectId, status, query, page, size | Danh sách quiz có scope, status, version |
| list_materials | T/M | classId hoặc subjectId, query, page, size | Học liệu được phép và trạng thái trích xuất/version |
| read_authoring_requirements | T/M | kind: ASSIGNMENT/QUIZ/BANK, scope | Schema/default/constraints được lấy từ validator hiện hành |
| create_assignment_draft | T/M | scope, targetId, metadata, materialIds, draft, requestRef | assignmentId, proposalId, version |
| create_quiz_draft | T/M | scope, targetId, metadata/settings, materialIds, questions, requestRef | quizId, proposalId, version |
| read_question_bank_context | M | subjectId | Ràng buộc câu hỏi, context môn và nguồn được phép |
| list_bank_questions | M | subjectId, query, type/difficulty/tag/status, page, size | Câu hỏi ngân hàng trong môn phụ trách |
| save_question_bank_draft | M | subjectId, questions, requestRef, agentLabel | proposalId/status/validation summary |
| list_submissions | T/M | classId, assignmentId, state/type/late/proposal filters, page, size | Bài nộp/evaluation có reference, version và trạng thái |
| read_grading_context | T/M | evaluationId, continuation tùy chọn | Frozen rubric/content/reference/hash/version/schema và thông tin đầy đủ/đọc tiếp |
| read_grading_batch | T/M | items: evaluationId + continuation tùy chọn | Ngữ cảnh/error/continuation theo từng item |
| record_grading_proposals_batch | T/M | items theo schema record_grading_proposal hiện có | proposalId/status hoặc safe error theo từng item |
| list_grading_proposals | T/M | evaluationId, page, size | Trạng thái đề xuất, nguồn, version/hash phù hợp quyền |
| read_class_learning_summary | T/M | classId, assignmentId tùy chọn | Thống kê chấm/nộp và tổng hợp phản hồi được phép |
| read_grade_distribution | T/M | classId, assignmentId và filter hiện hành | Phân bố điểm từ GradebookService |
| read_ai_usage_summary | A | days và filter hợp lệ của service hiện có | Số liệu usage tổng hợp có nguồn/thời gian |
| read_ai_quota_policy | A | page/size cho override giáo viên | Chính sách và quota được phép |
| read_ai_connection_status | A | Không có | Status/model/provider/masked hint, không có key/token |
| read_platform_health_summary | A | Không có | Dashboard tổng hợp và trạng thái các nguồn phụ thuộc |

Giữ các tool authoring/grading hiện có cho T/M; read_authorized_material thêm đọc tiếp và bỏ Student. Loại read_own_work/record_private_feedback khỏi đăng ký MCP, giữ service Practice dùng bởi giao diện.

## Giới hạn và xử lý lỗi

- READ_ONLY không thấy/gọi được tool WRITE; kiểm role/scope ở service mỗi lần, kể cả item batch và idempotency replay.
- Dùng safe error hiện hành cho ngoài quyền; không trả thông tin đối tượng ngoài scope qua total/filter/result.
- Hash hoặc version đổi trong lúc agent đọc/chấm: trả conflict; không áp dụng đề xuất vào bản khác. Rubric là snapshot đánh giá, không lấy bản mới nhất thay thế.
- Group root hỗ trợ; member evaluation không phải đích chấm độc lập qua đề xuất nhóm.
- Mỗi item batch có requestRef riêng; retry chỉ item lỗi, không lặp lại những item đã thành công.
- Giữ các giới hạn token credential, expiry, rate limit và audit đã có. Không ghi credentials hay toàn văn bài nộp vào log/idempotency response.

## Ngoài phạm vi đã chốt

Student MCP/tutor/quiz-result/progress tool; Admin sửa key/quota/tài khoản/backup qua MCP; tự phát hành assignment/quiz hoặc tự công bố điểm; gửi thông báo/email qua MCP; mô hình AI mới hay chi phí provider mới. Phân tích AI và soạn nội dung diễn ra ở agent bên ngoài.

## Workflow và extension

Phân tích hiện trạng và phạm vi đã hoàn thành trong trao đổi; không chạy lại reverse engineering toàn hệ thống. Requirements/user stories nằm trong tài liệu này; thiết kế contract và kế hoạch code được chuẩn bị để duyệt chung. Thực thi tuần tự trong phiên, không subagent; chưa đổi branch/worktree hay commit.

Kế thừa cấu hình extension ở aidlc-state.md: Security Baseline bật trong phạm vi SECURITY-03/04/05/08/09/12/15; Resiliency bật trong phạm vi RESILIENCY-04/06/10; PBT tắt. Kế hoạch đã ánh xạ logging không lộ secret, validation, RBAC, credential và fail-closed; headers hiện có phải giữ. Rollback/deploy checks N/A cho giai đoạn chỉ lập kế hoạch; dependency isolation/health reporting nằm trong tool báo cáo và kiểm thử lỗi. Chưa tuyên bố implementation compliant trước kiểm thử.
