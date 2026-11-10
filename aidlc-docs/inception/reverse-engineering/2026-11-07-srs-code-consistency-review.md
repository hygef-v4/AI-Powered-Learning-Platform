# Đối chiếu SRS với code hiện tại — 2026-11-07

**Tài liệu:** [Report 3 Software Requirement Specification](https://docs.google.com/document/d/1keJ0pUGo1VZ9Dzqd4ScREgFw-92LPSWm/edit), Word trên Drive, modifiedTime đọc được `2026-10-20T04:03:17.463Z`. Đã đọc nội dung qua connector, gồm bảng entity/UC/field/BR/MSG; không chỉnh file từ xa. **Code snapshot:** `2ee1a095` trên `staging`.

Kết luận: có 13 nhóm lệch hoặc thiếu đã đối chiếu với source. Bảng tổng quan 66 UC và phân vai không đủ để xác nhận phần đặc tả chi tiết đã đồng bộ. Đây là review source/document, không phải kết quả chạy test hay xác nhận implementation đúng mọi yêu cầu.

## Ưu tiên sửa quy tắc ảnh hưởng chấm điểm và quyền

### S01 — Điểm đã chốt: SRS vẫn cho sửa với lý do

**Vị trí:** III.4.6 Grading Workspace; entity Evaluation; V.BR17 và MSG22. SRS cho sửa FINALIZED/PUBLISHED nếu có reason. [Evaluation](../../../backend/src/main/java/edu/aiplatform/grading/model/Evaluation.java) chặn cả hai bằng `GRADE_LOCKED` qua `requireMutableGrade`, không có ngoại lệ vì điền lý do. Điều chỉnh điểm DRAFT có reason vẫn là trường hợp khác.

**Cần sửa:** trạng thái được sửa, UC43/44, nút/action và thông báo khóa điểm. Bổ sung `SUPERSEDED` vào Evaluation lifecycle và quy tắc nộp lại: [EvaluationLifecycleService](../../../backend/src/main/java/edu/aiplatform/grading/service/EvaluationLifecycleService.java) supersede bản cá nhân cũ, còn nhóm reset bản group/member về PENDING. Không tự bịa lịch sử snapshot chấm nhóm mà code không lưu.

### S02 — Sai người cấu hình và sai phạm vi biểu đồ điểm

**Vị trí:** UC29, III.3.13 Class Detail, các mô tả visibility và BR24. SRS giao Subject Manager bật distribution ở cấp lớp. [GradeDistributionVisibilityService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradeDistributionVisibilityService.java) yêu cầu đúng teacherId hiện hành của lớp; quản lý môn đơn thuần không đủ. Dữ liệu cấu hình nằm theo assignment–class, không phải một công tắc chung của U05.

**Cần sửa:** permission matrix/UC29, màn quản lý lớp và Gradebook/Assignment, entity/settings scope. Các ngưỡng 20 điểm published và ẩn bin dưới 5 không bị kết luận sai trong review này.

### S03 — Nguồn AI và retry/fallback không còn như SRS

**Vị trí:** R6, phần dẫn III, III.1.6, BR21–22, IV.1 và IV.2.4. SRS bắt mở picker cho mọi AI action, không auto-fallback, không retry provider. [AiTaskService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskService.java) có automatic mode khi bỏ source: shared có quyền/đủ quota ước tính rồi personal ACTIVE; PERSONAL không provider chỉ xét personal. [AiTaskJobHandler](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java) retry lỗi tạm REJECTED 429/503/529, rồi xét key kế tiếp, gồm khả năng từ shared sang personal của actor. [ConnectionOrder](../../../backend/src/main/java/edu/aiplatform/ai/service/ConnectionOrder.java) định thứ tự provider/model/key.

**Cần sửa:** tách lựa chọn rõ với automatic mode, mô tả fallback và bên chịu chi phí, settlement REJECTED/UNKNOWN, trường hợp không còn key hợp lệ. Không mô tả duplicate queue redelivery như retry provider trong cùng một task. Không đổi quyền Student/Admin khi cập nhật quy tắc nguồn.

### S04 — AI key/MCP credential: cardinality và reveal đã đổi

**Vị trí:** entity AI Connection/MCP Credential; III.1.6 và field tables; BR20. SRS quy định một key/provider/owner và không bao giờ trả key/token lần nữa. [Migration personal keys](../../../backend/src/main/resources/db/migration/ai/V20261105_1300__allow_many_personal_keys.sql) cho nhiều key cá nhân/provider. [AiConnectionService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiConnectionService.java) có bật/tắt và owner reveal; [McpCredentialController](../../../backend/src/main/java/edu/aiplatform/ai/controller/McpCredentialController.java) có PATCH nhãn/quyền và GET token qua service có guard.

**Cần sửa:** giữ list masked nhưng mô tả reveal có kiểm owner/trạng thái/ciphertext và audit phù hợp; Admin không được đọc personal key của người khác. CRUD/test/status/reveal theo connection ID, không nhận provider thay UUID. Không hiểu reveal có kiểm quyền thành công bố secret đại trà.

## Quiz và học liệu

### S05 — Quiz còn REVIEWED/Approve và thiếu các thay đổi authoring

**Vị trí:** entity Quiz; III.3.3–3.6 và bảng Screen List; UC51 liên quan snapshot đề. [QuizStatus](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStatus.java) chỉ có DRAFT/OPEN/RETIRED; [QuizPublicationService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizPublicationService.java) publish trực tiếp từ DRAFT. Bước duyệt còn ở đề xuất câu vào ngân hàng, theo [QuestionProposalService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionProposalService.java), không phải duyệt Quiz.

**Cần sửa:** bỏ Approve/REVIEWED cho Quiz; bổ sung đề cố định hoặc sinh theo quy tắc tag/difficulty ở [QuizPaperService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizPaperService.java), kiểm chống trùng và teacher đề xuất câu để Subject Manager duyệt. [Migration quiz rework](../../../backend/src/main/resources/db/migration/quizzes/V20261101_1200__quiz_rework.sql) xác nhận `reference_material_ids` không còn dùng, nên bỏ mô tả gắn tối đa 10 tài liệu vào Quiz. Assignment có lifecycle riêng; không xóa REVIEWED trên toàn SRS.

### S06 — Activity Quiz có thể dẫn tới quyết định trừ điểm của giảng viên

**Vị trí:** BR31; III.3.6, III.5.9–5.10; IV.2.1. SRS nói activity không bao giờ ảnh hưởng điểm và teacher chỉ đọc timeline. [AttemptActivityAssessor](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/AttemptActivityAssessor.java) tính mức trừ gợi ý theo [LeavePenalty](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/LeavePenalty.java). [TeacherQuizAttemptService](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/TeacherQuizAttemptService.java) cho teacher quyết định áp dụng/miễn/đổi số điểm với reason và audit.

**Cần sửa:** ghi rawScore, suggested/applied penalty và điểm sau quyết định, cùng permission/action/message. Hệ thống không tự trừ chỉ từ signal; giảng viên quyết định. Không mô tả tính năng này thành tự kết luận gian lận hoặc thay đổi điểm Assignment đã chốt.

### S07 — Module học liệu đã thuộc implementation cơ bản

**Vị trí:** phần dẫn III ghi không có module; III.3.7–3.8/III.5.2; conceptual model và BR03–04. [MaterialModuleController](../../../backend/src/main/java/edu/aiplatform/materials/controller/MaterialModuleController.java) và [MaterialModuleService](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialModuleService.java) quản lý module theo môn; [MaterialService](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialService.java) yêu cầu moduleId hợp lệ cùng subject.

**Cần sửa:** thêm module, thứ tự, quản lý/quyền xem, liên kết material và Quiz theo mô hình hiện hành. Module cơ bản không đồng nghĩa đưa Lesson nâng cao, summaries, embeddings hoặc RAG vào MVP. Giữ change log loại module trước đây như lịch sử và thêm revision mới khi sửa.

## Các ràng buộc và flow khác

### S08 — Mở lớp không còn bắt buộc có teacher

**Vị trí:** I.3.3 data rule về teacher assignment trước khi mở lớp; màn Class Detail. SRS ghi phải phân công teacher trước OPEN. [CourseClass.changeStatus](../../../backend/src/main/java/edu/aiplatform/academics/model/CourseClass.java) không có guard đó; OPEN kiểm subject ACTIVE và enrollment conflict. Teacher nếu được gán vẫn phải qua kiểm role/status/scope của service.

**Cần sửa:** cho phép OPEN với teacherId null và định nghĩa hành vi UI/quyền giảng dạy khi chưa có teacher; không tự cấp quyền teacher cho manager.

### S09 — Lý do xin đổi trưởng nhóm không còn bắt buộc 10 ký tự

**Vị trí:** BR13 và field Leader-change request. [LeaderRequestService](../../../backend/src/main/java/edu/aiplatform/groups/service/LeaderRequestService.java) nhận null/rỗng, trim và giới hạn tối đa 1.000 ký tự. SRS yêu cầu 10–1.000 và bắt buộc.

**Cần sửa:** validation/input/message cho lý do của người gửi. Không tự bỏ reason ở quyết định approve/reject của teacher vì đó là trường hợp riêng.

### S10 — Thiếu các flow cộng tác nhóm hiện có

**Vị trí:** UC56, III.5.5 Group Workspace và entity/field tables. [GroupDocCollaborationController](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocCollaborationController.java) có DOCX preview/import/apply và peer reviews; [GroupDocController](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocController.java) có assignment lock/unlock và section revisions/restore. Nội dung được đọc chưa có peer-review/phase-lock flow tương ứng.

**Cần bổ sung:** actor/scope, điều kiện khóa, bản nháp/revision, hành vi nhập nội dung và quyền đọc/ghi đánh giá chéo; giữ ranh giới với official grades.

### S11 — Chấm rubric/feedback đang mô tả thiếu mức chi tiết

**Vị trí:** UC43–44, III.4.6 và fields rubric/AI proposal. SRS chưa mô tả điểm từng item dạng partial, structured feedback, evidence/recommendations và review acknowledgement. [GradingCommands](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingCommands.java) đã có itemScoresByRubric, structuredFeedback, acknowledgedReviewItems/reviewedItemScores; model xử lý qua [RubricScoring](../../../backend/src/main/java/edu/aiplatform/grading/model/RubricScoring.java) và [GradingFeedback](../../../backend/src/main/java/edu/aiplatform/grading/model/GradingFeedback.java).

**Cần bổ sung:** dữ liệu, validation và bước giảng viên rà proposal; không coi số proposal/task thành bằng chứng official grade đã được duyệt.

### S12 — Mail vẫn chỉ mô tả Brevo SMTP

**Vị trí:** JOB-02, API-06, IV.1 và V.3. [application.yml](../../../backend/src/main/resources/application.yml) hỗ trợ MAIL_TRANSPORT smtp mặc định hoặc brevo-api với BREVO_API_KEY; [BrevoApiMailAdapter](../../../backend/src/main/java/edu/aiplatform/identity/integration/BrevoApiMailAdapter.java) dùng HTTPS khi outbound SMTP bị chặn.

**Cần bổ sung:** hai transport và cấu hình secret tương ứng; giữ Mailpit local/test và phân biệt provider acceptance với inbox delivery.

### S13 — BF09 gộp backup thành công với restore đã kiểm thử

**Vị trí:** BF09, JOB-18, BR10 và IV.2.4. BF09 nêu backup được lưu và restore được test như end condition. [BackupJobHandler](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupJobHandler.java) mark SUCCESS sau dump/encrypt/upload/checksum, không tự restore mỗi lần. BR10 và [operator runbook](../../../docs/runbooks/deploy-vps.md) đặt restore verification ở flow vận hành riêng.

**Cần sửa:** phân biệt acceptance của scheduled backup với isolated restore verification; không suy ra mọi SUCCESS đã được restore thử hoặc có RPO/RTO đã đo.

## Điểm nên làm rõ nhưng không coi là lỗi đã chứng minh

- SRS dùng “school email” nhiều nơi. [EmailPolicy](../../../backend/src/main/java/edu/aiplatform/identity/model/EmailPolicy.java) chấp nhận mọi tên miền đúng định dạng, tối đa 254 ký tự, account vẫn do Admin cấp. Nên ghi rõ nếu người đọc có thể hiểu thành domain allowlist; chỉ cụm “school email” chưa chứng minh SRS có validation sai.
- IV.2.4 nên dẫn [VPS pull deployment runbook](../../../docs/runbooks/deploy-vps.md), tách rollback image khỏi đảo ngược migration và giữ compatibility/restore limits. Không khẳng định VPS hiện healthy từ việc đọc source.
- UI/mock-up/diagram có thể còn thiếu module, trạng thái và action mới. Chưa render hoặc kiểm hình trong tài liệu; không tự ghi lỗi hình cụ thể khi chưa quan sát.
- Các ngưỡng NFR vẫn là mục tiêu. Review không đo latency, coverage, RPO/RTO hoặc chứng nhận pass rate.

## Thứ tự cập nhật đề xuất

S01–S04 trước vì ảnh hưởng quyền, điểm và chi phí AI; tiếp theo S05–S07; rồi S08–S13. Mỗi thay đổi cần đồng bộ entity/lifecycle, UC flow, permission, màn/field, BR và MSG liên quan; tránh chỉ thêm một đoạn delta rồi để quy tắc cũ vẫn được trình bày là hiện hành. Giữ change log lịch sử, thêm revision mới khi sửa thật.

Không sửa SRS từ xa, code/config hoặc các quy tắc theo suy đoán. Không chạy lại test/build, không xác minh live provider/VPS/Jira, không commit/push. Đây là danh sách lệch đã xác nhận trong phạm vi source review, không phải chứng minh mọi câu còn lại của SRS đều đúng.
