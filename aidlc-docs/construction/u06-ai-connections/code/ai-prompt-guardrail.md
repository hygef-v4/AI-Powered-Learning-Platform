# U06 — Guardrail hai tầng cho prompt người dùng (2026-11-11)

Yêu cầu: xây cơ chế kiểm duyệt tập trung cho mọi điểm người dùng nhập chữ tự do gửi tới AI, gồm Tầng 1 (quy tắc/regex) và Tầng 2 (ngữ cảnh, RBAC, máy trạng thái, chính sách logic), fail-closed, HTTP 400, không tốn credit, và chuẩn hóa cách ghép prompt chống jailbreak. Stack thực tế của dự án là Java 17/Spring Boot (không phải Next.js API Routes), nên guardrail đặt ở backend, nơi duy nhất gọi nhà cung cấp AI.

## 1. Điểm nhận prompt tự do

| Điểm vào (API/MCP) | Service | Thao tác | Phạm vi |
|---|---|---|---|
| Soạn đề bằng AI | `AssignmentAiDraftService.request` | `CREATE_DRAFT` (cho phép để trống) | `EXAM_EDITING` |
| Sửa đề bằng AI (`mode=REVISE`) | `AssignmentAiDraftService.request` | `REVISE_DRAFT` | `EXAM_EDITING` |
| Soạn câu hỏi quiz / ngân hàng câu hỏi (cả MCP) | `QuizAiDraftService.requestForQuiz/requestForBank` | `GENERATE_QUESTIONS` | `EXAM_EDITING` |
| Tạo lại quiz theo tài liệu | `QuizSourceRetry.retry` | `GENERATE_QUESTIONS` (chữ mặc định của hệ thống) | `EXAM_EDITING` |
| Đề xuất chấm AI | `GradingProposalService` | không có chữ tự do | chỉ đóng gói dữ liệu |
| Góp ý bài luyện tập | `PracticeFeedbackService` | không có chữ tự do | chỉ đóng gói dữ liệu |

Hiện chưa có API nào cho người dùng gõ chữ tự do để chỉnh barem bằng AI hoặc hỏi AI về góp ý; `GRADING_RUBRIC`, `STUDENT_FEEDBACK`, `GENERAL` đã có chính sách và test, dùng ngay khi thêm điểm vào mới. Bài nộp của sinh viên và tài liệu nguồn **không** đi qua bộ lọc (bài môn CSDL/bảo mật hợp lệ có thể chứa SQL hay `<script>`), mà được đóng thẻ dữ liệu không tin cậy.

## 2. Vị trí file

```text
backend/src/main/java/edu/aiplatform/ai/
├── contract/                     # dùng chung cho U07, U08, U09, U10 (ArchUnit: unit khác chỉ dùng contract)
│   ├── IPromptGuardrail.java     # evaluate(), requireValid() -> HTTP 400 AI_PROMPT_REJECTED
│   ├── UserPromptInput.java      # actor, context, action, state, text
│   ├── PromptContext.java        # EXAM_EDITING, GRADING_RUBRIC, STUDENT_FEEDBACK, GENERAL
│   ├── PromptAction.java         # thao tác, gắn với đúng một PromptContext
│   ├── PromptFlowState.java      # trạng thái nghiệp vụ do server xác định
│   ├── GuardrailCategory.java    # VALID, PROMPT_INJECTION, OFF_TOPIC, HARMFUL_MALICIOUS
│   ├── GuardrailVerdict.java     # category, tier, ruleId (chỉ log/test), message thân thiện
│   ├── UntrustedPrompt.java      # thẻ phân định + chỉ thị system chống jailbreak
│   └── AiTaskRequest.java        # thêm thành phần userInput (constructor cũ giữ nguyên)
└── service/
    ├── PromptGuardrailService.java   # pipeline tuần tự, fail-closed, log không chứa nội dung
    ├── PromptRuleScanner.java        # Tầng 1
    ├── PromptTextNormalizer.java     # chuẩn hóa chống né bộ lọc (Tầng 1)
    ├── PromptPolicyEngine.java       # Tầng 2
    ├── PromptFlowMachine.java        # máy trạng thái (Tầng 2)
    ├── PromptTextAnalysis.java       # tách trích dẫn/câu/từ bằng duyệt ký tự (Tầng 2)
    ├── PromptLexicon.java            # nhóm khái niệm ý định (Tầng 2)
    └── PromptContextPolicy.java      # độ dài, vai trò, câu báo theo phạm vi
```

## 3. Luồng xử lý

```text
User text --> Service gọi (U07/U10) --> IPromptGuardrail.requireValid  (trong transaction, trước khi lưu draft)
                                           |
                     +---------------------+---------------------+
                     | Tầng 1 vi phạm      | Tầng 2 vi phạm/lỗi  | VALID
                     v                     v                     v
               HTTP 400              HTTP 400            Lưu draft PENDING
               rollback              rollback (fail-     --> IAiTask.submit(userInput)
               không gọi AI          closed)                  |
                                                              v
                                      AiTaskService.requireGuardedInput (kiểm lại tập trung)
                                      - task chữ tự do thiếu userInput -> 400
                                      - userInput khác người gửi       -> 400
                                      - chạy lại guardrail             -> 400
                                                              |
                                                              v
                                      Giữ quota -> tạo AI task -> job -> mô hình chính
```

Kiểm lại trong `AiTaskService.submit` chạy trước khi giữ quota, tạo task hay gửi job, nên một unit mới quên gọi guardrail vẫn không gửi được chữ tự do tới AI (`FREE_TEXT_TASK_TYPES`).

## 4. Tầng 1 — quy tắc, regex, heuristic

| Nhóm | Mã quy tắc | Phân loại |
|---|---|---|
| Độ dài theo phạm vi (4.000/2.000/1.000/2.000; tối thiểu 2 khi bắt buộc nhập) | `T1_LENGTH_MAX`, `T1_LENGTH_MIN` | `OFF_TOPIC` |
| Ký tự tag Unicode, đảo chiều bidi | `T1_HIDDEN_CHARACTERS` | `PROMPT_INJECTION` |
| Ghi đè/vượt chỉ dẫn (Anh/Việt) | `T1_OVERRIDE_EN/VI`, `T1_OVERRIDE_ALL_VI`, `T1_BYPASS_EN/VI`, `T1_UNBOUND_EN` | `PROMPT_INJECTION` |
| Nhập vai nguy hiểm ("bây giờ bạn là", DAN, developer mode) | `T1_ROLEPLAY_*`, `T1_JAILBREAK_MODE` | `PROMPT_INJECTION` |
| Dò system prompt | `T1_SYSTEM_PROMPT_EN/VI`, `T1_TEXT_ABOVE_EN` | `PROMPT_INJECTION` |
| Giả thẻ phân định/lượt hội thoại | `T1_DELIMITER_SPOOF`, `T1_CHAT_TOKEN`, `T1_ROLE_HEADER` | `PROMPT_INJECTION` |
| `<script>`, `onerror=`, `javascript:` | `T1_SCRIPT` | `HARMFUL_MALICIOUS` |
| SQL payload | `T1_SQL_UNION/STACKED/TAUTOLOGY/FUNCTION` | `HARMFUL_MALICIOUS` |
| Base64/Hex chứa lệnh, khối mã hóa mờ | `T1_ENCODED_INJECTION`, `T1_ENCODED_PAYLOAD` | `PROMPT_INJECTION` |

Quy tắc chạy trên nhiều biến thể đã chuẩn hóa: NFKC, bỏ ký tự vô hình, chữ thường, bỏ dấu tiếng Việt, leetspeak, gộp chữ tách rời, giải mã URL và thực thể HTML.

## 5. Tầng 2 — đánh giá logic xác định

Không dùng regex, AI hay API ngoài. Thứ tự kiểm:

1. Đầu vào đủ; thao tác thuộc đúng phạm vi (`T2_ACTION_CONTEXT`).
2. RBAC theo phạm vi: `EXAM_EDITING` = TEACHER/SUBJECT_MANAGER, `GRADING_RUBRIC` = TEACHER, `STUDENT_FEEDBACK` = STUDENT, `GENERAL` = mọi vai trò (`T2_RBAC_ROLE`). Đây là lớp phòng thủ thêm; quyền trên đối tượng vẫn do service của unit kiểm.
3. Máy trạng thái (`T2_FLOW_STATE`): chỉ các cặp `AUTHORING_DRAFT + CREATE/REVISE/GENERATE`, `RUBRIC_EDITABLE + ADJUST_RUBRIC`, `ATTEMPT_SUBMITTED + EXPLAIN_FEEDBACK`, `OPEN + ASK`.
4. Tách phần trích dẫn (`"…"`, `“…”`, `«…»`, khối ```` ``` ````, dòng `>`) khỏi phần chỉ dẫn; ngoặc mở không đóng vẫn tính là chỉ dẫn.
5. Tự nhận vai trò cao hơn vai trò thật hoặc đòi sửa điểm khi là sinh viên (`T2_PRIVILEGE_ESCALATION`).
6. Câu chỉ dẫn ra lệnh cho AI: động từ điều khiển + đối tượng là chỉ dẫn AI + (nhắc tới AI hoặc từ chỉ định "trước đó/của bạn") (`T2_SYSTEM_DIRECTIVE`). Cùng câu đặt trong ngoặc được coi là nội dung trích dẫn hợp lệ.
7. Nội dung gây hại: nhóm luôn cấm, hoặc động từ tạo + đối tượng nguy hiểm trong cùng câu, không có khung đề bài, hoặc đòi chi tiết vận hành (`T2_HARMFUL_*`).
8. Ý định bị cấm theo phạm vi: giải hộ toàn bộ bài (`STUDENT_FEEDBACK`), thao túng điểm (`GRADING_RUBRIC`) (`T2_FORBIDDEN_INTENT`).
9. Lạc phạm vi: không có dấu hiệu đúng chức năng và có dấu hiệu lạc đề; phạm vi hẹp (barem, góp ý) bắt buộc có dấu hiệu đúng chức năng (`T2_OFF_TOPIC`).

Lỗi bất kỳ trong lúc kiểm cho kết quả `HARMFUL_MALICIOUS` (fail-closed), log `WARN` chỉ ghi mã quy tắc, phạm vi, tài khoản và loại lỗi.

## 6. Ghép prompt chống jailbreak

`UntrustedPrompt.wrapUserRequest` đặt chữ người dùng trong `<user_request>…</user_request>`; `wrap(tag, …)` dùng cho `source_material`, `current_document`, `student_submission`. Thẻ dành riêng xuất hiện trong dữ liệu bị đổi `<` thành `&lt;` (không đụng `List<String>` hay `x < 3`). `secureSystem` gắn `SYSTEM_DIRECTIVE` vào cuối system prompt. Đã áp dụng cho prompt soạn đề, sửa đề, soạn quiz, góp ý luyện tập và chỉ thị system của đề xuất chấm.

## 7. Thay đổi hành vi và giới hạn

- API soạn/sửa đề và soạn quiz có thêm lỗi `400 AI_PROMPT_REJECTED`; frontend hiển thị `detail` của Problem Detail như các lỗi 400 khác. Đã kiểm trên trình duyệt ở "Sửa đề bằng AI" (2026-11-12); màn soạn đề mới và soạn quiz chưa kiểm.
- Tầng 1 không phân biệt trích dẫn: câu "ignore previous instructions" bị chặn kể cả khi trích lại. Đề về SQL injection hay XSS có payload thật trong yêu cầu cũng bị chặn; đề nêu khái niệm thì không.
- Từ điển Tầng 2 là heuristic có thể bỏ sót cách diễn đạt mới; lớp chính vẫn là thẻ phân định + chỉ thị system. Chưa đo tỷ lệ chặn nhầm trên dữ liệu thật.
- Không đổi schema, migration, cấu hình hay dependency.
