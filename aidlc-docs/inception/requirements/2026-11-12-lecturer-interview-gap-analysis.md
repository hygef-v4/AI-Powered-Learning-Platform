# Đối chiếu phỏng vấn giảng viên với hệ thống ALAP

**Ngày:** 2026-11-12
**Nguồn:** `2026-11-12-tom-tat-nghiep-vu-phong-van.docx`, tóm tắt buổi phỏng vấn giảng viên ngày 12/11/2026, chia theo chủ đề A–K.
**Đối chiếu với:** [requirements.md](requirements.md) và source code trên nhánh `feat/ai-prompt-guardrail` (sau commit `01bac4aa`).
**Phạm vi:** chỉ đọc tài liệu và code tĩnh, không chạy test và không sửa code.

> **Lưu ý từ buổi phỏng vấn:** cô chỉ kể cách cô đang làm để nhóm tham khảo, **không yêu cầu thêm chức năng**. Mọi chức năng thêm hay bớt phải được giảng viên hướng dẫn (GVHD) duyệt. Vì vậy báo cáo này chỉ là **đề xuất để đưa GVHD duyệt**, chưa phải yêu cầu đã chốt.

## 0. Cập nhật trạng thái — 2026-11-20

Đối chiếu lại bằng code tĩnh và các tài liệu sau ngày 12/11. Các bảng từ mục 1 trở đi giữ nguyên làm lịch sử; mục này ghi những gì đã đổi.

| Mục | Trước | Nay | Bằng chứng |
|---|---|---|---|
| D2 Thang mức đóng góp | ❌ | ✅ Có 5 mức tùy chọn, giảng viên tự quyết điểm, không tự quy đổi | [2026-11-13-member-contribution-levels.md](2026-11-13-member-contribution-levels.md) |
| F2 Mỗi kỳ một file | 🟡 | 🟡 Lớp đã có học kỳ tự tính từ ngày bắt đầu để lọc (D-U04-05). Sổ điểm vẫn mỗi bài một cột, chưa gom theo đầu điểm | [2026-11-14-class-term.md](2026-11-14-class-term.md) |
| J4 Đánh giá AI chấm | ❌ | 🟡 Đã có bộ đánh giá (`backend/src/test/.../grading/eval`) và lần chạy Gemini. Điểm kỳ vọng do nhóm tự soạn, **chưa** được giảng viên xác nhận | [kết quả 2026-11-13](../../construction/build-and-test/2026-11-13/2026-11-13-ai-grading-eval-gemini-results.md) |
| C6/H Ký pháp sơ đồ | 🟡 | 🟡 Đã sửa lỗi xóa nhãn `<<include>>`/`<<extend>>` trước khi gửi AI (`DiagramXml` giữ stereotype). Chưa có rubric mẫu | `DiagramXml.java:293-302` |
| M1/M2 + P2-5 Quiz GRADED | ⚠️ | **Rút khỏi ưu tiên, giữ D01**: Syllabus cho thấy progress test chính thức chạy trên LMS hoặc tại lớp | [2026-11-20-lecturer-feedback-syllabus.md](2026-11-20-lecturer-feedback-syllabus.md) mục 3 |
| D3 Vấn đáp, present | ❌ | ❌ Syllabus xác nhận nhu cầu: oral test ở SWR302, present cá nhân 60% ở SWD392 | như trên, mục 2 |
| E1 Điểm danh | ❌ | ❌ Điều kiện dự 80% slot quản lý trên hệ thống của trường; giữ ngoài phạm vi | như trên, mục 2.3 |

**Nhu cầu mới từ Syllabus:**
- N1: Context diagram, DFD (SWR302) và communication diagram (COMET) chưa có loại riêng. Xem [2026-11-20-diagram-types.md](2026-11-20-diagram-types.md).
- N2: SWD392 bắt buộc khai báo dùng AI và có rubric AI 4 tiêu chí. Làm bằng khung bài và rubric, không cần code.

Kiểm lại ngày 2026-11-20, chưa đổi: C3 (trần điểm tiêu chí), C4 (mục trừ điểm, `points.signum() < 0` vẫn bị từ chối), A1, G2, G3, G6 (`QuestionScope` vẫn chỉ `BANK, QUIZ`), G7, D1, I3, B2.

## 1. Tóm tắt

| Mức | Số mục | Ý nghĩa |
|---|---|---|
| ✅ Đã đáp ứng | 22 | Có code và yêu cầu tương ứng |
| 🟡 Đáp ứng một phần | 16 | Đã có nền tảng, còn thiếu một vài chi tiết |
| ❌ Chưa có | 11 | Hệ thống chưa có chức năng nào cho nhu cầu này |
| ⚠️ Mâu thuẫn với quyết định đã chốt | 2 (+1 ở mục 3) | Phải xin GVHD quyết định trước khi làm (M1–M3) |

**Kết luận chính:**

1. Phần cốt lõi khớp tốt với cách cô làm: chấm theo rubric, AI chỉ đề xuất còn giảng viên quyết định, chỉnh điểm từng thành viên nhóm, sinh đề ngẫu nhiên theo chủ đề, AI soạn đề, xuất Excel và nhập tài khoản bằng Excel.
2. Các khoảng trống lớn nhất nằm ở **quản lý đầu điểm**: chưa có cấu trúc cột điểm (PT1–3/Lab/Assignment), chưa có điểm cộng, chưa có điểm danh, và Quiz không vào sổ điểm.
3. Rubric hiện chỉ có **mục cộng điểm**. Chưa có điểm trần cho từng tiêu chí, mục trừ điểm (ví dụ nét liền bị trừ) và mẫu rubric dùng lại.
4. Cô rất quan tâm đến **chất lượng chấm của AI**. Hệ thống có nhiều lớp kiểm tra kỹ thuật nhưng chưa có bộ dữ liệu chuẩn để đo độ chính xác và mức bịa của AI.
5. SRS và NFR cần bổ sung số liệu đo được và bằng chứng test hiệu năng/bảo mật (mục J).

## 2. Bảng đối chiếu chi tiết

### A. Cấu trúc đầu điểm của môn

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| A1 | Mỗi môn có bộ đầu điểm cố định (PT1–PT3, Lab, Assignment…) | Sổ điểm chỉ có một cột cho mỗi bài GRADED ([GradebookService.java:431](../../../backend/src/main/java/edu/aiplatform/grading/service/GradebookService.java:431)). Chưa có khái niệm "đầu điểm"/loại bài và chưa gom nhiều bài vào một cột | ❌ |
| A2 | Mỗi loại bài có tính chất riêng (PT trắc nghiệm, Lab thực hành, Assignment đồ án) | Chỉ có hai loại ASSIGNMENT/QUIZ, cộng `submissionMode` và `diagramEnabled` (FR-007). Chưa gắn được nhãn "PT/Lab/Assignment" cho bài | 🟡 |
| A3 | Giảng viên tự điều chỉnh bài theo lớp mình | Giảng viên tạo được bài cấp lớp và sao chép/chỉnh bài (FR-027, FR-028) | ✅ |
| A4 | Không theo quy trình khảo thí (hạn gửi điểm, phạt nộp muộn) | Cô đã chấp nhận phạm vi "giáo viên giao bài tùy ý", nên không cần làm | ✅ (ngoài phạm vi) |

### B. Gian lận và bảo mật khi làm bài

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| B1 | Ghi nhận rời tab/F12 (nhóm tự xử lý) | FR-039 có `activityMonitoringEnabled`, ghi sự kiện hidden/visible và gợi ý trừ điểm Quiz để giảng viên quyết định. Hiện chưa phát hiện F12/DevTools | 🟡 |
| B2 | Đề trắc nghiệm dùng nhiều năm sẽ bị lộ | Có chế độ `GENERATED` sinh đề riêng cho từng lượt từ ngân hàng ([DrawRule.java](../../../backend/src/main/java/edu/aiplatform/quizzes/model/DrawRule.java)), có đảo câu/đáp án và `QuestionFingerprint` để chống trùng câu. Chưa thống kê câu đã dùng bao nhiêu lần/năm nào để thay câu cũ | 🟡 |
| B3 | Bài về nhà không chặn được AI/chép bài, trọng tâm là cách đánh giá | Hợp với hướng hiện tại: không chấm theo hành vi, giảng viên chấm cuối. Có lịch sử revision theo thành viên ở bài nhóm | ✅ |

### C. Tiêu chí chấm (rubric)

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| C1 | Điểm quy về tiêu chí cụ thể, không chấm theo cảm nhận | Rubric theo phần, 1–20 tiêu chí, mỗi tiêu chí 1–20 mục có điểm, khóa khi phát hành ([AssignmentRubric.java](../../../backend/src/main/java/edu/aiplatform/assignments/model/AssignmentRubric.java)) | ✅ |
| C2 | Điểm chia nhỏ (0,1; 0,2 cho mỗi use case) | Điểm mục là số thập phân, chấm được một phần ([RubricScoring.java](../../../backend/src/main/java/edu/aiplatform/grading/model/RubricScoring.java)) | ✅ |
| C3 | **Trần điểm cho tiêu chí** (mỗi use case 0,2 nhưng tổng câu có trần) | Điểm tối đa luôn bằng tổng các mục, chưa có `maxPoints` cho từng tiêu chí và chưa có mục kiểu "0,2 × số lượng" | ❌ |
| C4 | **Mục trừ điểm** (include/extend vẽ nét liền bị trừ) | Điểm mục bắt buộc ≥ 0 (`item.points().signum() < 0` bị từ chối). Chỉ làm được bằng cách đảo thành mục cộng "vẽ đúng nét đứt" | 🟡 |
| C5 | Tiêu chí sinh từ chính đề bài | AI soạn được rubric nháp từ đề (FR-006), giảng viên duyệt | ✅ |
| C6 | Tiêu chí ký pháp diagram và nội dung đặc tả use case (actor, pre/post-condition, flows, business rule) | Có `CriterionKind.DIAGRAM`/`TEXT`, nhưng chưa có bộ tiêu chí soạn sẵn cho đặc tả use case | 🟡 |
| C7 | **Mẫu tiêu chí chung** cho hệ thống cơ bản (đăng ký tài khoản…) | FR-016 ghi rõ "không tự thêm thực thể template cấp môn". Hiện chỉ sao chép được rubric khi sao chép cả bài | ⚠️ |
| C8 | AI chấm phải dựa vào tiêu chí giảng viên đưa | Prompt và JSON schema bắt AI trả về theo đúng mục rubric, có validator kiểm cấu trúc/version ([GradingPromptBuilder.java](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingPromptBuilder.java), `GradingProposalValidator`) | ✅ |

### D. Chấm bài nhóm và từng thành viên

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| D1 | Mỗi thành viên phụ trách một phần, có ghi nhận đóng góp công khai | Tài liệu nhóm chia section, mỗi section một người sửa, có lịch sử `SectionRevision` theo tác giả (FR-026). Chưa có trang "bảng đóng góp" tổng hợp để cả nhóm và giảng viên cùng xem | 🟡 |
| D2 | **Thang 5 mức đánh giá thành viên** (vượt kỳ vọng → không hoàn thành) | Hiện chỉ cho ghi đè điểm thành viên bằng số và lý do (`overrideMemberScore`). Chưa có thang mức và quy tắc đổi mức thành điểm | ❌ |
| D3 | Hỏi trực tiếp từng bạn (vấn đáp), đối chiếu SRS, kết hợp phần present | Chưa có chỗ ghi kết quả vấn đáp/present cho từng thành viên. Hiện chỉ ghi được vào lý do override | ❌ |
| D4 | Giảng viên quyết định, không suy điểm từ số thao tác | FR-026 và prompt AI cấm đánh giá đóng góp cá nhân | ✅ |

### E. Điểm cộng (bonus)

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| E1 | Chuyên cần theo dữ liệu điểm danh | Không có module điểm danh (tìm `attendance` trong backend: 0 kết quả) | ❌ |
| E2 | Phát biểu (~0,5 điểm/lần) cộng dồn cả kỳ | Không có sổ ghi chú bonus | ❌ |
| E3 | Nghiên cứu/chuẩn bị kỹ | Không có | ❌ |
| E4 | Giảng viên tự chọn cộng bonus vào đầu điểm nào | Phụ thuộc A1 (chưa có đầu điểm) | ❌ |

### F. Quản lý và gửi điểm

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| F1 | Xuất file điểm để nhập vào phần mềm của trường | Xuất GradeBook CSV/XLSX, mỗi bài một cột, có STT/Họ tên/Email (FR-024) | ✅ |
| F2 | Cột theo đầu điểm (PT1, Lab, Assignment…) và mỗi kỳ một file | Cột theo từng bài, chưa gom theo đầu điểm. Chưa có khái niệm học kỳ trong sổ điểm | 🟡 |
| F3 | Ghi chú kèm điểm (bonus, lần 1 thiếu gì) | Có feedback theo bài, chưa có cột ghi chú tự do trong sổ điểm | 🟡 |
| F4 | Chỉ giảng viên đó sửa điểm, chống sửa điểm | Chỉ Teacher được giao lớp chấm/chốt; điểm chốt bất biến; mọi thay đổi đều có audit (FR-020) | ✅ |
| F5 | Sinh viên nộp bài theo cấu trúc rõ ràng | Nộp ngay trong hệ thống, có lịch sử lần nộp (FR-018), không cần Google Drive | ✅ |

### G. Tạo đề và giao đề

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| G1 | Thời gian làm bài (60 phút), ẩn kết quả, chỉ cho xem điểm | `QuizSettings` có `timeLimitMinutes`, `showScoreAfterSubmit`, `showCorrectAnswers`, `maxAttempts` | ✅ |
| G2 | **Giờ kết thúc** bài trắc nghiệm | FR-007: Quiz "không lịch mở đóng" | ⚠️ |
| G3 | **Điểm tối thiểu để đạt (%)** | Không có trường ngưỡng đạt | ❌ |
| G4 | Mỗi sinh viên một đề khác nhau, sinh từ ngân hàng | Chế độ `GENERATED` bốc câu cho từng lượt | ✅ |
| G5 | Chọn câu theo chương/mục với số câu cho mỗi phần | `DrawRule(tag, difficulty, questionCount)` làm được nếu dùng tag làm chương. Chưa có cây chương/mục chuẩn, giảng viên phải tự đặt tag thống nhất | 🟡 |
| G6 | Hai nguồn câu: ngân hàng chung và **kho câu cá nhân** của giảng viên | `QuestionScope` chỉ có `BANK` (cấp môn) và `QUIZ` (riêng một quiz). Chưa có kho câu cá nhân dùng lại cho nhiều quiz | 🟡 |
| G7 | **Nhiều đề Assignment/đề vẽ, chia theo chẵn/lẻ** để hai bạn ngồi cạnh không trùng đề | Một Assignment chỉ có một đề. Chưa có biến thể đề và quy tắc phân đề | ❌ |
| G8 | AI sinh đề tương tự (đổi bối cảnh, giữ cấu trúc) | AI soạn nháp Assignment từ học liệu và nhập đề từ ảnh (exam-image-import). Chưa có thao tác "sinh đề tương tự từ đề X" | 🟡 |
| G9 | Sinh viên vẽ diagram và nộp | Có workspace Draw.io lưu XML (thay cho Visual Paradigm) | ✅ |

### H. Dùng AI để chấm

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| H1 | AI đọc hết bài và chấm theo tiêu chí | Đề xuất chấm từng bài hoặc theo lô qua API (UC 43) hoặc MCP (`read_grading_batch`, `record_grading_proposals_batch`) | ✅ |
| H2 | Điểm AI chỉ để tham khảo, giảng viên verify và chấm lại | AI chỉ tạo proposal; giảng viên phải bấm lưu/chốt; AI không tự công bố điểm | ✅ |
| H3 | AI hiểu sai khi tiêu chí không rõ (cần 2 normal flow nhưng thấy 1 là chấm đủ) | Chưa có cảnh báo khi rubric mơ hồ (ví dụ mục không nêu số lượng) và chưa có ô "hướng dẫn chấm cho AI" riêng cho từng mục | 🟡 |
| H4 | Cô thích dùng Claude | Có adapter Claude, kết nối MCP từ Claude | ✅ |
| H5 | Giảng viên là người quyết định cuối cùng | Đúng nguyên tắc FR-008 | ✅ |

### I. Bảo mật dữ liệu điểm khi dùng AI

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| I1 | Không ai hỏi AI mà ra được điểm sinh viên | MCP không có tool đọc sổ điểm/điểm chính thức. Student không đọc được rubric/official grade (FR-035). Batch chấm dùng `submissionRef` thay cho danh tính | ✅ |
| I2 | Phân quyền theo role và phạm vi | Mỗi tool call kiểm role, phân công lớp và scope READ_ONLY/READ_WRITE; Admin không có MCP | ✅ |
| I3 | Dữ liệu gửi AI có thể bị dùng để train | Chưa có cảnh báo trên giao diện về chính sách dữ liệu của provider. Chưa có tùy chọn ẩn danh hóa nội dung bài trước khi gửi API (tên/MSSV có thể nằm trong nội dung bài nộp) | 🟡 |

### J. Chất lượng SRS và kiểm thử

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| J1 | SRS chi tiết (độ dài trường, trường bắt buộc, nội dung thông báo lỗi) | Có business rules theo unit (BR-Uxx) và giới hạn trong code (ví dụ `TITLE_MAX = 200`). Chưa có bảng tổng hợp field spec và message lỗi trong SRS | 🟡 |
| J2 | Nghiệp vụ cấu hình được, không fix cứng | Quota/lịch backup/AI qua Settings. Nhiều giới hạn vẫn là hằng số trong code (ví dụ `CRITERIA_MAX`, retention activity 1 ngày) | 🟡 |
| J3 | NFR đo được và có test chứng minh (số request, thời gian phản hồi) | NFR-003 có số đo (p95 ≤ 500 ms, < 100 user đồng thời), có [performance-test-instructions.md](../../construction/build-and-test/performance-test-instructions.md). Chưa thấy kết quả chạy tải thực tế (k6/JMeter) | 🟡 |
| J4 | **Test hệ thống AI** bằng dữ liệu chuẩn: độ chính xác, bịa, lạc đề, trích nguồn (6–7 tiêu chí) | Chỉ có test kỹ thuật (schema, quota, stale, provider mock). Chưa có bộ bài mẫu đã chấm tay để đo độ lệch điểm và tỷ lệ bịa. FR-024 còn ghi "phân tích độ lệch điểm AI nằm ngoài phạm vi" | ❌ |

### K. Đồng bộ dữ liệu với trường

| # | Nhu cầu | Hiện trạng | Mức |
|---|---|---|---|
| K1 | Admin nhập tài khoản bằng Excel | Có, dùng mẫu .xlsx (UC 09, delta 2026-11-09) | ✅ |
| K2 | Chủ nhiệm môn phân công giảng viên dạy lớp | Có (FR-003) | ✅ |

## 3. Mâu thuẫn với quyết định đã chốt (cần GVHD quyết định)

| Mã | Quyết định hiện tại | Điều cô mô tả | Câu hỏi cho GVHD |
|---|---|---|---|
| M1 | **D01** (2026-10-15): Quiz chỉ Practice, không vào sổ điểm | PT là điểm thành phần, máy tự chấm và gửi khảo thí | Có cho Quiz chế độ GRADED để làm PT không? |
| M2 | **FR-007**: Quiz không có lịch mở/đóng | Quiz trên hệ thống trường có giờ kết thúc | Có thêm hạn đóng cho Quiz không? (đi kèm M1) |
| M3 | **FR-016**: không thêm thực thể template rubric cấp môn | Cần mẫu tiêu chí chung cho hệ thống cơ bản | Có cho thêm thư viện mẫu rubric không? |

## 4. Đề xuất bổ sung (theo ưu tiên)

Ưu tiên dựa trên mức cô nhắc đến, giá trị cho đề tài "chấm diagram cho sinh viên SWD" và công sức ước tính. Tất cả cần GVHD duyệt trước.

### P1 — Nên làm, gắn với cốt lõi chấm bài

1. **Nâng cấp rubric (C3, C4, H3).** Thêm `maxPoints` cho tiêu chí (tổng mục vượt trần thì cắt), mục trừ điểm (giá trị âm, không làm tổng tiêu chí < 0), mục định lượng "điểm × số lượng, tối đa N", và ô "hướng dẫn chấm cho AI" cho từng mục. Ảnh hưởng: U07 assignments, U09 grading, prompt AI và frontend soạn/chấm rubric.
2. **Thang mức đóng góp thành viên nhóm (D2, D3).** Thêm 5 mức theo đề xuất của cô, mỗi mức map ra hệ số hoặc điểm gợi ý, giảng viên vẫn chỉnh được. Kèm ghi chú vấn đáp/present cho từng thành viên. Tận dụng `overrideMemberScore` có sẵn.
3. **Bộ đánh giá chất lượng AI chấm (J4).** Chuẩn bị 20–30 bài mẫu đã chấm tay làm dữ liệu chuẩn, chạy AI và đo: độ lệch điểm, đúng/sai từng mục rubric, bịa (nêu chi tiết không có trong bài), lạc đề, có dẫn chứng phần bài. Kết quả làm bằng chứng cho báo cáo và SRS. Có thể làm ngoài app (script + báo cáo), không cần sửa chức năng.

### P2 — Nên làm nếu GVHD đồng ý mở phạm vi

4. **Đầu điểm và sổ điểm (A1, F2, F3, E2–E4).** Thêm "đầu điểm" cho môn/lớp (PT1, Lab1, Assignment…), gán mỗi bài GRADED vào một đầu điểm, thêm cột bonus và ghi chú. Xuất Excel theo đúng cột đầu điểm để giảng viên chép sang phần mềm trường. Vẫn giữ quy tắc không tính điểm tổng theo hệ số nếu GVHD muốn.
5. **Quiz GRADED cho PT (M1, M2, G3).** Thêm chế độ GRADED, hạn đóng và ngưỡng đạt %. Nối vào sổ điểm theo đầu điểm.
6. **Biến thể đề Assignment (G7, G8).** Cho một Assignment có nhiều đề con, phân theo STT chẵn/lẻ hoặc ngẫu nhiên. Thêm nút "AI sinh đề tương tự" từ đề có sẵn (đổi bối cảnh, giữ cấu trúc), kết quả là nháp chờ duyệt.
7. **Kho câu hỏi cá nhân và cây chương/mục (G5, G6).** Thêm scope `PERSONAL`, chuẩn hóa tag chương/mục theo môn để quy tắc bốc câu dễ dùng.

### P3 — Có thể để sau

8. **Thư viện mẫu rubric (C7, M3)**, ví dụ mẫu "Đặc tả use case" và "Ký pháp Use Case Diagram" (C6).
9. **Bảng đóng góp nhóm (D1)**: tổng hợp số section, revision và thời điểm theo thành viên, chỉ để tham khảo, không tự ra điểm.
10. **Ghi nhận điểm danh/phát biểu (E1, E2)**: module điểm danh đơn giản hoặc nhập điểm danh từ Excel xuất từ FAP.
11. **Bảo mật dữ liệu AI (I3)**: hiện thông báo chính sách dữ liệu của provider trước khi chấm bằng API chung, tùy chọn che tên/MSSV trong bài trước khi gửi.
12. **Thống kê độ lộ câu hỏi (B2)**: số lần câu được dùng/theo kỳ để giảng viên thay câu cũ.

### Tài liệu, không cần code

13. **SRS (J1, J2):** thêm bảng field spec (độ dài, bắt buộc, message lỗi) cho các màn hình chính và danh sách quy tắc nghiệp vụ nào cấu hình được, quy tắc nào cố định.
14. **NFR (J3):** chạy thử tải theo performance-test-instructions và ghi số đo thực tế (p95, số user đồng thời), kèm kết quả security test.
15. **Report 1 (AS-IS):** dùng buổi phỏng vấn này làm bằng chứng khảo sát AS-IS thật, vì requirements.md §2.3 còn ghi "không tuyên bố AS-IS đã khảo sát".

## 5. Bước tiếp theo đề xuất

1. Gửi mục 3 (M1–M3) và danh sách P1/P2 cho GVHD để chốt phạm vi.
2. Với mục được duyệt, mở Requirements Analysis theo AI-DLC (file câu hỏi và cập nhật FR liên quan) trước khi code.
3. Mục 13–15 (tài liệu) và mục 3 (bộ đánh giá AI) làm được ngay vì không đổi chức năng.
