# Góp ý giảng viên và Syllabus SWR302/SWD392 — đối chiếu với ALAP

**Ngày:** 2026-11-20
**Nguồn:**
- Góp ý bằng văn bản của giảng viên về quy trình giảng dạy, chuẩn sơ đồ SWR/SWD, tiêu chí chấm và MCP (nguyên văn trong `aidlc-docs/audit.md`, mục 2026-11-20).
- Syllabus FLM SWD392 *Software Architecture and Design* (Syllabus ID 14179) và SWR302 *Software Requirement* (Syllabus ID 14176), cùng quyết định 377/QĐ-ĐHFPT ngày 04/09/2026.
- Đối chiếu với code tĩnh trên nhánh `audit-staging` (sau commit `7d51a16f`). Chưa chạy ứng dụng hay test.

> Tài liệu chỉ ghi điều Syllabus và giảng viên nêu, cùng nhận định từ code. Chưa quan sát trực tiếp lớp học. Mọi thay đổi chức năng vẫn phải được GVHD duyệt.

## 1. Định vị: ALAP không áp quy trình giảng dạy riêng

Giảng viên cho biết khung chung là Syllabus trên FLM, còn mỗi thầy cô có phong cách riêng, và tiêu chí chấm "theo Syllabus, tùy từng thầy cô". Cả hai Syllabus đều cho phép **Option 2 (Constructivism): "Follow lecturer's proposal"** ở các đầu điểm on-going.

ALAP vì vậy:
- **không** mô hình hóa Syllabus, CLO, slot hay trọng số đầu điểm, và không tính điểm tổng;
- để giảng viên tự tạo bài, khung bài, rubric, hạn nộp và chế độ GRADED/PRACTICE cho từng lớp. Giảng viên có thể sao chép đề mẫu cấp môn (FR-027, FR-028);
- cho phép đính kèm Syllabus hoặc template của môn (Vision and Scope, Use Case, SRS…) làm học liệu tham khảo (FR-004).

## 2. AS-IS theo Syllabus và góp ý (bằng chứng cho Report 1)

### 2.1 SWR302 — Software Requirement

| Đầu điểm | Trọng số | Cách làm theo Syllabus | Bước ALAP hỗ trợ |
|---|---:|---|---|
| Assignment | 20% | Giảng viên ra dự án, sinh viên làm dần theo template (Vision and Scope, Use Case, SRS, Data Dictionary…). Đánh giá giữa kỳ và cuối kỳ bằng **vấn đáp** (slot 38–39, 58–60) | Bài GROUP hoặc INDIVIDUAL có khung bài, nộp nhiều lần, rubric, AI đề xuất điểm. **Chưa** có chỗ ghi kết quả vấn đáp |
| LAB | 10% | **Cá nhân** vẽ 1 Context, 1 Swimlane, 1 State diagram theo đề tài Assignment, trình bày tuần 7, sau đó đưa vào tài liệu Assignment | Bài INDIVIDUAL có sơ đồ draw.io. Context không có loại riêng (xem mục 5) |
| Progress test | 20% | 3 bài trắc nghiệm 30 phút, chạy **trên LMS** | Ngoài phạm vi chấm chính thức. Quiz PRACTICE dùng để ôn |
| Final: PE | 25% | 85 phút trên LSM, lấy từ Assignment | Giảng viên soạn được đề tương tự để luyện, có nhập đề từ ảnh |
| Final: TE | 25% | 60 câu trắc nghiệm, chấm bằng máy | Ngoài phạm vi |

- Công cụ trong Syllabus là Visual Paradigm, BOUML và MS Office. Workspace draw.io của ALAP thay cho bước vẽ khi làm bài trên hệ thống.
- Chương 12 (slot 22–24) dạy DFD, Swimlane, State-transition diagram và state table, Dialog map, Decision table/tree, Event-response table.

### 2.2 SWD392 — Software Architecture and Design

| Đầu điểm | Trọng số | Cách làm theo Syllabus | Bước ALAP hỗ trợ |
|---|---:|---|---|
| Course Project | 25% | Nhóm 4–5 người. On-going 40% gồm Evaluation 1 (20%: đặc tả use case, UML tĩnh và động) và Evaluation 2 (20%: kiến trúc, hệ con, OO design, schema DB). Final presentation 60% gồm demo 30%, mastery 20%, soft skills 10%. Rubric AI cá nhân tối đa 10 điểm | Bài GROUP có tài liệu chia section, mức đóng góp thành viên 5 mức (2026-11-13). **Chưa** có chỗ ghi present cá nhân |
| Progress test | 15% | 3 bài, mỗi bài 5%, trắc nghiệm 30 phút, làm tại lớp | Ngoài phạm vi chấm chính thức. Quiz PRACTICE dùng để ôn |
| Final: PE | 20% | Bài làm 85 phút | Như SWR302 |
| Final: TE | 40% | 60 câu trắc nghiệm, đề khác đề cũ ít nhất 70% | Ngoài phạm vi. Chế độ `GENERATED` và `QuestionFingerprint` hỗ trợ ngân hàng câu luyện tập |

- Phương pháp chính là COMET (Gomaa 2011). Các sơ đồ gồm use case, class, đối tượng tương tác (communication/sequence), statechart, kiến trúc và component.
- **Khai báo dùng AI là bắt buộc:** nộp prompt, nêu phần do AI sinh và tự giải thích. Rubric AI gồm 4 tiêu chí: hiểu output, tùy chỉnh và cải tiến, minh bạch (prompt và khai báo), dùng hiệu quả và đúng đạo đức.
- Syllabus khuyến khích sinh UML bằng PlantUML hoặc Mermaid (slot 18, 38, 54). ALAP chưa nhập được hai dạng này.

### 2.3 Chung cho hai môn và cách giảng viên làm

- Sinh viên phải dự ít nhất 80% số slot mới được thi. Điểm danh quản lý trên hệ thống của trường; ALAP không làm.
- Giảng viên thường giao bài thực hành về nhà, sinh viên làm rồi lên lớp trao đổi.
- Đầu điểm chính thức được nhập vào hệ thống của trường. ALAP xuất sổ điểm CSV/XLSX để giảng viên chép sang (FR-024).

## 3. D01 được Syllabus xác nhận

Progress test chính thức của cả hai môn chạy trên LMS hoặc tại lớp, và thi cuối kỳ do khảo thí tổ chức. Vì vậy **giữ D01 (Quiz chỉ PRACTICE, không vào sổ điểm)**. Đề xuất "Quiz GRADED cho PT" (P2-5 trong [gap analysis 2026-11-12](2026-11-12-lecturer-interview-gap-analysis.md), mâu thuẫn M1/M2) rút khỏi danh sách ưu tiên.

## 4. MCP, AI và chi phí

### 4.1 ALAP không chạy model và không cần GPU

| Kênh | Model chạy ở đâu | Ai trả chi phí token |
|---|---|---|
| API chung của trường | Nhà cung cấp cloud (`AiProvider`: Gemini, OpenAI, Claude) | Đơn vị vận hành. Có quota ngày cho từng giảng viên, trần toàn hệ thống và kill-switch (FR-034, D04) |
| API cá nhân | Nhà cung cấp cloud | Chủ key (Student, Teacher, Subject Manager) |
| Agent MCP | Ở client và nhà cung cấp mà người dùng tự chọn. ALAP chỉ cung cấp tool đọc dữ liệu và ghi bản nháp | Tài khoản của người dùng; ALAP ghi token là `externallyReported` hoặc `unavailable` (FR-035, FR-036) |

- Server ALAP chỉ chạy web, API, worker, PostgreSQL, Redis và RabbitMQ. Giới hạn container trong `infra/docker-compose.yml` cộng lại khoảng 5,5 vCPU và 5 GB RAM, không có GPU. Đây là trần cấu hình, chưa phải số đo tải.
- **Model local trên GPU nằm ngoài phạm vi.** `AiProvider` chỉ có 3 nhà cung cấp cloud. Nếu sau này cần chạy local, phải thêm adapter mới và chạy lại bộ đánh giá AI chấm (mục 4.3), vì chất lượng chấm sơ đồ phụ thuộc nhiều vào model. Chi phí mua và bảo trì GPU chưa được ước tính.
- Hướng triển khai trùng với góp ý của giảng viên: **dùng Cloud làm PoC**. Khi chạy thật, trường phải có nguồn trả cho API chung, hoặc giảng viên và sinh viên dùng key hoặc agent của mình. Không mặc định API miễn phí hay ngân sách của trường (REL-005).

### 4.2 Ước tính token khi AI chấm bài

| Số liệu | Giá trị | Nguồn |
|---|---|---|
| Trần token đầu ra mỗi lần chấm | `min(8192, 4096 + 220 × số mục rubric)`, ví dụ rubric 13 mục là 6.956 token | `GradingPromptBuilder.calculateMaxOutputTokens` |
| Token đầu ra đo thật, rubric 13 mục | 3.120–5.698 token, trong đó tối đa 1.739 token suy nghĩ | Đánh giá Gemini 2.5 Flash, [kết quả 2026-11-13](../../construction/build-and-test/2026-11-13-ai-grading-eval-gemini-results.md) |
| Mức token giữ trước (reservation) | `ký tự prompt / 2 + trần đầu ra`; mỗi ảnh tính 3.200 ký tự | `AiTokenEstimate` (BR-U06-56) |
| Token đầu vào thật | **Chưa có trong repo.** Đo bằng màn usage của chủ key (FR-036) trên vài bài thật | — |

**Công thức chi phí một lớp trong một kỳ:**

```text
Chi phí ≈ số sinh viên (hoặc số nhóm) × số bài chấm bằng AI × số lần chấm mỗi bài
          × (token vào × giá vào + token ra × giá ra)
```

Ví dụ chỉ để minh họa khối lượng: lớp 30 sinh viên, 4 bài, chấm AI một lần mỗi bài, cần 120 lượt chấm. Lấy mức token ra đo được cao nhất là 5.698 thì cần khoảng **684.000 token đầu ra**, cộng thêm token đầu vào chưa đo. Đơn giá phải lấy từ trang giá của nhà cung cấp và model đang cấu hình, ghi ngày tra cứu. Tài liệu này không ghi đơn giá vì giá thay đổi theo thời điểm.

### 4.3 Cách hệ thống giới hạn chi phí và việc bảo trì

- AI chỉ chạy khi giảng viên hoặc sinh viên bấm yêu cầu. Không có chấm tự động hàng loạt ngầm.
- Quota ngày theo giảng viên, trần ngày toàn hệ thống và kill-switch do Admin đặt. Hết quota thì không tự chuyển sang key cá nhân (FR-034).
- Phản hồi bài Practice chỉ dùng key cá nhân của sinh viên, không tốn ngân sách chung (FR-030).
- Việc bảo trì phải lường trước: nhà cung cấp đổi tên model hoặc ngừng model cũ (danh sách model nằm trong cấu hình backend), giá thay đổi, và cần chạy lại bộ đánh giá AI chấm sau mỗi lần đổi model (`ai-grading-evaluation-instructions.md`).

## 5. Loại sơ đồ theo chuẩn môn học

`DiagramType` hiện có `USE_CASE, CLASS, SEQUENCE, ACTIVITY, ER, COMPONENT, STATE, OTHER`. Theo code, Context diagram, DFD (SWR302) và communication diagram (COMET) bị nhận thành `OTHER`, hoặc thành `CLASS` với context diagram kiểu COMET. Đề xuất, phân tích ảnh hưởng và câu hỏi nằm ở [2026-11-20-diagram-types.md](2026-11-20-diagram-types.md).

## 6. Việc cấu hình không cần code

- Khung bài SWD392 có phần "Khai báo sử dụng AI" (prompt, phần do AI sinh, tự giải thích), cùng rubric 4 tiêu chí AI ở mục 2.2.
- Bài mẫu LAB SWR302 (Context, Swimlane, State) và Evaluation 1 SWD392 làm đề mẫu cấp môn để giảng viên sao chép.
