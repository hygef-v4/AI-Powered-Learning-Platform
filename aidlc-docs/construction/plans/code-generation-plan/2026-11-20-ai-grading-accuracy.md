# Kế hoạch: cải thiện độ chính xác AI chấm bài

**Ngày:** 2026-11-20
**Trạng thái:** người dùng duyệt 2026-11-20 làm giai đoạn 1–3 trước (không cần key). Giai đoạn 1–3 đã xong; giai đoạn 0, 4, 5 chưa làm.
**Unit:** U09 Assignment Grading (chính); U07 (`DiagramXml`, `DocumentModelService`); U08 (phản hồi Practice); `agent-skills/grade-with-rubric`.
**Nguồn:**
- [Kết quả đánh giá AI chấm 2026-11-13/15](../../build-and-test/2026-11-13-ai-grading-eval-gemini-results.md) và [hướng dẫn đánh giá](../../build-and-test/ai-grading-evaluation-instructions.md).
- [Góp ý giảng viên và Syllabus 2026-11-20](../../../inception/requirements/2026-11-20-lecturer-feedback-syllabus.md) và [phân tích loại sơ đồ](../../../inception/requirements/2026-11-20-diagram-types.md).
- Đọc code tĩnh trên `audit-staging` sau commit `7d51a16f`.

## 1. Mục tiêu và ràng buộc

AI đề xuất điểm sát giảng viên hơn, nêu được bằng chứng ở đúng phần bài, và tự báo "cần giảng viên xem" khi không chắc. Áp dụng cho cả bài SWR302 (Wiegers: Context, DFD, Swimlane, State) và SWD392 (COMET).

**Ràng buộc:**
- **Giai đoạn 0–4 không đổi cách giao đề**, không đổi giao diện, API, schema DB hay MCP tool name. Chỉ đổi phần dữ liệu và chỉ dẫn gửi cho AI, cùng bộ đánh giá.
- AI vẫn chỉ đề xuất; giảng viên quyết định và công bố điểm (FR-008).
- Mọi thay đổi prompt phải được đo lại bằng bộ đánh giá trước khi coi là cải thiện.
- Giai đoạn 5 đổi giao diện hoặc dữ liệu, nên cần duyệt riêng (và GVHD nếu mở phạm vi).

## 2. Hiện trạng (lần chạy gần nhất 2026-11-15, Gemini 2.5 Flash, bộ tự soạn)

| Chỉ số | Ngưỡng | Hiện tại | Kết luận |
|---|---|---|---|
| Cohen's kappa từng mục | ≥ 0,60 | 0,89 | Đạt |
| Đồng thuận từng mục | ≥ 80% | 96,5% | Đạt |
| Sai số điểm tổng | ≤ 10% | 1,5% | Đạt |
| Bịa điểm | ≤ 5% | 0/13 | Đạt |
| Định dạng hợp lệ | ≥ 95% | 11/11 | Đạt |
| Chống chèn lệnh | 100% | 3/3 | Đạt (mẫu nhỏ) |
| **Trích dẫn nguồn** | ≥ 90% | **14,0%** | **Không đạt (F4)** |
| **Tự đánh dấu cần xem** | — | **0%** | **Không có tác dụng (F3)** |
| **Nhận ra bài lạc đề** | 100% | Chưa đo sau khi sửa F7 (trước đó 1/3) | **Chưa biết** |

**Giới hạn của các số trên:** điểm kỳ vọng do nhóm tự soạn (`SYNTHETIC_TEAM`), chỉ có một đề use case, chỉ thử Gemini. Số liệu này chưa chứng minh được độ chính xác thật.

## 3. Nguyên nhân gốc tìm được trong code

| Mã | Vấn đề | Nguyên nhân | Vị trí |
|---|---|---|---|
| R1 | Trích dẫn nguồn 14% (F4) | Văn bản bài gửi AI **không kèm mã block**. `blockIds` gửi thành danh sách riêng, nên AI không biết mã nào ứng với đoạn nào để điền `evidenceBlockId` | `GradingPromptBuilder.java:247-268`, `DocumentModelService.toPlainText` |
| R2 | Tóm tắt sơ đồ gửi **hai lần** | `toPlainText` đã kèm `compactDiagramXml`, các dòng 253–256 lại thêm lần nữa. Tốn token (tới 60.000 ký tự mỗi lần cho mỗi sơ đồ) và làm phần bài dài, rối | `GradingPromptBuilder.java:247-257`, `DocumentModelService.java:317` |
| R3 | Không thấy system boundary và quan hệ "nằm trong" | Tóm tắt chỉ ghi kiểu hình khi style có `shape=`; hình chữ nhật mặc định không ghi kiểu; không ghi quan hệ cha–con | `DiagramXml.compactXml` |
| R4 | Sơ đồ `OTHER` (Context, DFD, Communication) | AI chỉ thấy nhãn `[Sơ đồ OTHER]` và không biết nhãn này do hệ thống đoán | `DocumentModelService.java:317`, `buildGradingInstructions` |
| R5 | Không tự báo cần xem (F3), chấm thất thường khi rubric mơ hồ (F5, H3) | Chỉ dẫn chỉ cho phép báo cần xem khi không đọc được bằng chứng. Không có quy tắc cho mục rubric không ghi rõ số lượng hay cách chia điểm | `buildGradingInstructions` |
| R6 | Không có quy tắc báo cần xem từ phía server | Server chỉ tự đánh dấu khi bằng chứng rỗng (F6). Tóm tắt sơ đồ bị cắt (`…(đã cắt)`) thì AI vẫn chấm như đủ | `GradingProposalNormalizer`, `DiagramXml.compactXml` |
| R7 | Kênh MCP và phản hồi Practice không được hưởng các sửa trên | `agent-skills/grade-with-rubric/SKILL.md` chưa có hướng dẫn về sơ đồ hay bằng chứng; phản hồi Practice dùng `toPlainText` riêng | `PracticeFeedbackService`, `SKILL.md` |
| R9 | Một mã block sai làm hỏng cả đề xuất | `GradingFeedbackValidator` ném `Unknown evidenceBlockId` (chẩn đoán `FEEDBACK_UNKNOWN_EVIDENCE_BLOCK`), giảng viên mất token. `sanitizeAndNormalizeProposal` nhận `validBlockIds` nhưng không dùng. Có thể vì vậy AI hay để mã rỗng | `GradingFeedbackValidator.java:200-207`, `GradingProposalNormalizer.java:158-171` |
| R8 | Bộ đánh giá hẹp | Một đề use case, tự soạn, một provider; chưa có mẫu SWR302 hoặc COMET | `backend/src/test/resources/ai-eval/grading` |

## 4. Các bước

### Giai đoạn 0 — Mở rộng bộ đánh giá và đo mốc (không đổi app)

- [ ] 0.1 Thêm bộ `swr302-lab`: một đề LAB (1 Context, 1 Swimlane, 1 State), rubric ghi rõ ký pháp Wiegers, 6–8 bài: đủ, Context vẽ thành DFD nhiều process, thiếu luồng dữ liệu, State thiếu sự kiện, lạc đề, sơ đồ trống. Gắn `labelSource: SYNTHETIC_TEAM`.
- [ ] 0.2 Thêm bộ `swd392-comet`: class diagram có stereotype «entity»/«boundary»/«control» và communication diagram cho một use case; 6–8 bài, có bài thiếu stereotype và bài sai thứ tự message.
- [ ] 0.3 Chỉ số mới trong harness: tỷ lệ mục được đánh dấu cần xem ở các mục mà dữ liệu cố ý làm mơ hồ (cờ `ambiguousItems` trong case). Test `GradingEvalHarnessTest` cho chỉ số này.
- [ ] 0.4 Chạy mốc live trên code hiện tại: 3 bộ × 3 lần với Gemini. Ghi kết quả làm mốc so sánh. Chia theo bộ và nghỉ giữa các đợt để tránh 429 như 2026-11-15.
- [ ] 0.5 Song song, nhờ giảng viên chấm 20–30 bài thật đã ẩn danh (bỏ tên, MSSV) theo từng mục rubric, gắn `LECTURER_VERIFIED`. Bước này mất nhiều thời gian nhất nên bắt đầu sớm; không chặn các giai đoạn khác.

### Giai đoạn 1 — Sửa dữ liệu gửi AI (R1, R2, R3)

- [x] 1.1 Trong prompt chấm, mỗi block có mã ngay trước nội dung, ví dụ `[block 3f2a…] …nội dung…`. Làm trong `GradingPromptBuilder`. **Không đổi** `toPlainText` dùng chung cho MCP, phản hồi Practice và soạn đề. Sửa câu chỉ dẫn về `evidenceBlockId` cho khớp.
  - *Thực hiện:* thêm `IDocumentModel.toPlainTextWithBlockIds` (U07, chỉ thêm method; stub trả chuỗi rỗng) dùng chung một chỗ render với `toPlainText` để không lặp quy tắc trích block của đề (`> `). `toPlainText` giữ nguyên đầu ra.
- [x] 1.1b Làm cùng 1.1: trong `sanitizeAndNormalizeProposal`, mã block không thuộc `validBlockIds` được đặt về `null` thay vì làm hỏng cả đề xuất. Nếu mục đó không còn bằng chứng thì áp quy tắc F6 hiện có (đánh dấu cần xem). Không đổi `GradingFeedbackValidator`.
- [x] 1.2 Bỏ lần gửi tóm tắt sơ đồ thứ hai (`GradingPromptBuilder.java:253-257`). Kiểm schema và chuẩn hóa không phụ thuộc phần bị bỏ.
- [x] 1.3 `DiagramXml.compactXml`: ghi `(chữ nhật)` cho hình không có `shape=`, ghi `trong <id>` khi hình có `parent` khác lớp gốc. Giữ trần 60.000 ký tự.
  - *Thực hiện:* sơ đồ mẫu thật cho thấy khung system boundary chỉ nằm đè lên use case (cùng `parent="1"`), nên ngoài quan hệ cha–con còn xét **tọa độ** (`mxGeometry`): hình nằm trọn trong hình nhỏ nhất bao nó. Sơ đồ quá 500 hình cùng cấp thì bỏ phần xét tọa độ (ghi shortcut trong code). Đầu ra mẫu: `hình uc-book (ellipse) trong sys: Đặt sân`. Tóm tắt này cũng đi vào phản hồi Practice, MCP và soạn đề vì dùng chung `compactDiagramXml`.
- [x] 1.4 Test: `GradingPromptBuilderTest` (có mã block, sơ đồ chỉ xuất hiện một lần), test chuẩn hóa (mã block sai thành `null`, đề xuất không bị từ chối), test `DiagramXml` (chữ nhật, quan hệ cha–con, giữ `<<include>>` và nét đứt), cập nhật `GradingEvalHarnessTest`.

### Giai đoạn 2 — Sửa chỉ dẫn chấm (R4, R5)

- [x] 2.1 Loại sơ đồ: "Nhãn loại sơ đồ do hệ thống tự đoán và có thể là OTHER hoặc sai. Xác định loại từ đề, tiêu đề phần chứa sơ đồ và mục rubric, rồi chấm theo ký pháp của loại đó."
- [x] 2.2 Chuẩn ký pháp: "Ưu tiên chuẩn ký pháp ghi trong đề hoặc rubric (ví dụ COMET, Wiegers). Không tự thêm tiêu chí ký pháp mà rubric không nêu."
- [x] 2.3 Rubric mơ hồ: "Nếu mục rubric yêu cầu số lượng hoặc cách chia điểm mà không nói rõ, chấm theo cách hiểu hẹp nhất có thể kiểm chứng từ đề, và đặt `requiresReview=true` kèm lý do."
- [x] 2.4 Lạc đề: giữ quy tắc F7 hiện có, thêm ví dụ cho sơ đồ ("sơ đồ về hệ thống khác được 0 điểm").
- [x] 2.5 Test `GradingPromptBuilderTest` khóa các câu chỉ dẫn mới.

### Giai đoạn 3 — Quy tắc báo cần xem phía server (R6)

- [x] 3.1 Khi tóm tắt sơ đồ bị cắt, mọi mục rubric `DIAGRAM` của phần đó được đánh dấu `requiresReview=true` với lý do "Sơ đồ quá lớn, AI chỉ đọc được một phần". Làm trong bước chuẩn hóa, không bịa điểm hay bằng chứng.
  - *Đổi cách làm:* bước chuẩn hóa lúc nhận kết quả không có tài liệu bài làm để biết sơ đồ nào bị cắt và thuộc phần nào; thêm việc tải tài liệu ở callback là thay đổi lớn cho trường hợp hiếm (trần 60.000 ký tự mỗi sơ đồ). Vì vậy dùng chỉ dẫn trong prompt: tóm tắt kết thúc bằng `…(đã cắt)` thì AI phải đặt `requiresReview=true` cho các mục sơ đồ liên quan. Nếu đo thấy AI không tuân thủ thì mới làm quy tắc phía server.
- [x] 3.2 Test: câu chỉ dẫn trong `GradingPromptBuilderTest`; mã block sai/rỗng thành `null` trong `GradingPromptBuilderTest` và `GradingProposalResultHandlerTest` (đề xuất được nhận thay vì bị từ chối).

### Kết quả kiểm thử giai đoạn 1–3 (2026-11-20)

- Maven 3.9.16 offline, JDK 21. Test liên quan: `DocumentModelServiceTest` 21, `GradingPromptBuilderTest` 16, `GradingPromptSchemaTest` 3, `GradingProposalResultHandlerTest` 13, `GradingProposalValidatorTest` 8, `GradingProposalServiceTest` 10, `GradingProposalErrorTest` 2, `GradingEvalHarnessTest` 9, `ContractStubsTest` 4: **86/86 đạt**, gồm 5 test mới.
- Toàn bộ unit test của `assignments`, `assignmentwork`, `grading`, `groupdocs`, `ai` và `ArchitectureTest`, trừ `*IntegrationTest`: **755 đạt, 0 lỗi, 8 bỏ qua**.
- Chưa chạy: integration test (Docker engine không chạy), `mvn verify` đầy đủ, và chưa gọi AI thật nên **chưa biết độ chính xác thay đổi thế nào**. Phải chạy giai đoạn 4.3 để đo.
- Hành vi đổi có chủ ý: đề xuất AI có mã block sai trước đây bị từ chối (`FEEDBACK_UNKNOWN_EVIDENCE_BLOCK`), nay được nhận với `evidenceBlockId = null`. Kênh agent MCP ghi đề xuất vẫn kiểm chặt như cũ vì không qua bước chuẩn hóa (`GradingProposalTransactions.java:411`); agent nhận lỗi và gửi lại được.

### Giai đoạn 4 — Áp dụng cho kênh khác và đo lại (R7)

- [ ] 4.1 Đưa chỉ dẫn 2.1–2.4 vào prompt phản hồi Practice (`PracticeFeedbackService`), giữ giới hạn hiện có (không lộ rubric hay điểm chính thức cho sinh viên).
- [ ] 4.2 Cập nhật `agent-skills/grade-with-rubric/SKILL.md`: cách đọc tóm tắt sơ đồ, xác định loại sơ đồ theo đề, nêu bằng chứng theo block, báo cần xem.
- [ ] 4.3 Chạy lại 3 bộ × 3 lần với Gemini, so với mốc 0.4.
  - *Đã chạy một phần (2026-11-20):* chỉ bộ badminton-booking, `gemini-3.5-flash-lite`, bản cũ `7d51a16f` và bản mới cùng ngày. Không cải thiện; bài 08 tụt (G1). [Kết quả](../../build-and-test/2026-11-20-ai-grading-accuracy-results.md).
- [ ] 4.4 Chạy thêm một provider khác (Claude hoặc OpenAI) trên cùng bộ để so sánh.
- [ ] 4.5 Ghi kết quả vào `construction/build-and-test/2026-11-xx-ai-grading-accuracy-results.md`, cập nhật hướng dẫn đánh giá và mục J4 trong gap analysis.

### Giai đoạn 5 — Tùy chọn, cần duyệt riêng (đổi giao diện hoặc dữ liệu)

- [ ] 5.1 Ô "hướng dẫn chấm cho AI" cho từng mục rubric (H3). Đổi U07 và giao diện soạn rubric.
- [ ] 5.2 Cảnh báo khi soạn rubric: mục có chữ "các", "đủ", "tất cả" mà không nêu số lượng hay điểm mỗi đơn vị.
- [ ] 5.3 Chấm hai lần, mục nào hai lần lệch nhau thì đánh dấu cần xem. Chi phí token gấp đôi; chỉ bật theo cấu hình.
- [ ] 5.4 Mục 8: thêm loại sơ đồ `CONTEXT`, `DFD`, `COMMUNICATION` ([câu hỏi Q1–Q4](../../../inception/requirements/2026-11-20-diagram-types-questions.md)).

## 5. Tiêu chí hoàn thành (giai đoạn 0–4)

Đo trên cả 3 bộ, so với mốc 0.4:

| Chỉ số | Mục tiêu |
|---|---|
| Trích dẫn nguồn | ≥ 90% (mốc hiện tại 14%) |
| Tự đánh dấu cần xem ở mục cố ý mơ hồ | ≥ 80% |
| Nhận ra bài lạc đề | 100% |
| Kappa, đồng thuận từng mục, sai số điểm tổng, bịa, định dạng, chống chèn lệnh | Không tụt dưới ngưỡng trong hướng dẫn đánh giá, và không tụt quá 0,05 kappa so với mốc |
| Token đầu vào mỗi lần chấm bài có sơ đồ | Giảm so với mốc (nhờ 1.2) |
| Test tự động | `GradingPromptBuilderTest`, `GradingPromptSchemaTest`, test `DiagramXml`, `GradingEvalHarnessTest`, test phản hồi Practice đạt; `mvn verify` không lỗi |

Chỉ khi có dữ liệu `LECTURER_VERIFIED` (0.5) mới dùng số liệu này làm bằng chứng trong báo cáo đồ án.

## 6. Rủi ro và chi phí

- **Kết quả phụ thuộc model.** Một chỉ dẫn có thể giúp Gemini nhưng làm Claude chấm khắt hơn, nên 4.4 là bắt buộc trước khi kết luận.
- **Đánh dấu cần xem nhiều quá** sẽ làm giảng viên mất công. Theo dõi tỷ lệ cần xem ở bài đầy đủ (01-complete) và giữ ở mức thấp.
- **Chi phí đánh giá:** mỗi đợt 3 bộ × khoảng 8 bài × 3 lần ≈ 72 lời gọi, cả mốc và đo lại ≈ 144 lời gọi Gemini, cộng một đợt provider thứ hai. Token ra mỗi lần khoảng 3.000–6.000. Tốn key của người chạy; có thể vượt hạn mức miễn phí theo ngày (đã gặp 429 ngày 2026-11-15).
- **Dữ liệu tự soạn** có thể lệch với cách giảng viên chấm thật; 0.5 là cách duy nhất khắc phục.

## 7. File dự kiến thay đổi

- `backend/src/main/java/edu/aiplatform/grading/service/GradingPromptBuilder.java`
- `backend/src/main/java/edu/aiplatform/grading/service/GradingProposalNormalizer.java` (giai đoạn 1 và 3)
- `backend/src/main/java/edu/aiplatform/assignments/service/DiagramXml.java`
- `backend/src/main/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackService.java`
- `agent-skills/grade-with-rubric/SKILL.md`
- Test: `backend/src/test/java/edu/aiplatform/grading/...`, `.../assignments/service/...`, `.../assignmentwork/service/...`, `.../grading/eval/...`
- Dữ liệu: `backend/src/test/resources/ai-eval/grading/swr302-lab/`, `.../swd392-comet/`
- Tài liệu: hướng dẫn đánh giá, file kết quả mới, gap analysis, `aidlc-state.md`, `audit.md`

Theo [quy ước mã nguồn](../../code-conventions.md): giữ tên method và contract hiện có; method mới theo mẫu động từ + đối tượng.

## 8. Thiết kế G3 — giới hạn suy nghĩ dùng chung cho mọi provider (chờ duyệt)

**Vấn đề (đo 2026-11-20):** model có suy nghĩ tính token suy nghĩ vào cùng trần đầu ra. Gemini 3.5 bỏ qua `thinkingBudget` (dùng 3.054–3.291 so với 1.739 được phép) và chạm trần 6.956, nên `MAX_TOKENS`: người dùng mất token mà không có đề xuất. OpenAI (`max_completion_tokens` gồm reasoning) có cùng rủi ro vì app không gửi mức suy nghĩ. Claude không bật suy nghĩ nên không bị.

**Nguyên tắc:** use case (chấm, soạn đề…) chỉ nói cần bao nhiêu token **cho câu trả lời**. Phần suy nghĩ là việc của provider, do adapter trong `HttpProviderGateway` lo và cấu hình được, không sửa code khi đổi model.

| Thành phần | Thiết kế |
|---|---|
| Cấu hình mỗi provider (`platform.ai.providers.<p>`) | Thêm `reasoning-models` (danh sách model có suy nghĩ; rỗng là không model nào), `reasoning-effort` (`none`/`low`/`medium`/`high`, mặc định `low`), `reasoning-reserve-tokens` (token cộng thêm vào trần cho phần suy nghĩ, mặc định 4.096). Mỗi khóa có biến môi trường `U06_<P>_…` |
| Trần gửi provider | `min(câu trả lời + reserve, max-output-tokens của provider)` với model trong `reasoning-models`; model khác giữ nguyên như hiện nay. Nâng mặc định `max-output-tokens` lên 16.384 để chứa phần cộng thêm |
| Tham số gốc của từng hãng | Gemini 2.5: `thinkingBudget` (= reserve); Gemini 3.x: tham số mức suy nghĩ theo `reasoning-effort`; OpenAI: `reasoning_effort`; Claude: không gửi (không bật suy nghĩ). Model không nằm trong `reasoning-models` thì không gửi tham số suy nghĩ, tránh HTTP 400 với model cũ |
| Quota giữ trước | Tính theo trần thực gửi (đã gồm reserve), để quota không bị vượt |
| Khi vẫn bị cắt | Báo lỗi rõ như hiện nay, không tự gọi lại (tránh tốn token gấp đôi). Ghi `reasoningTokens` vào usage để Admin thấy cần tăng reserve |
| Kiểm chứng | Unit test cho từng nhánh provider (có/không suy nghĩ, model cũ không nhận tham số). Mỗi provider có key: 1 lời gọi thử, rồi chạy bài 04, 06, 08 × 3. Đạt khi 0 lần `MAX_TOKENS` |

**Chưa chắc chắn, phải kiểm bằng lời gọi thử:** tên và giá trị tham số mức suy nghĩ của Gemini 3.x; OpenAI có nhận `reasoning_effort` qua Chat Completions với model đang cấu hình hay không. Nếu một provider không nhận tham số, chỉ dùng phần cộng `reasoning-reserve-tokens`.

**File dự kiến:** `ai/config/AiProperties.java`, `ai/integration/HttpProviderGateway.java`, `ai/service/ProviderCatalog.java` (đọc trần và cấu hình), nơi giữ quota trước (`AiTaskService`, `AiTaskJobHandler`), `application.yml`, `infra/.env.example`, test `HttpProviderGatewayTest`, `KeyCipherAndCatalogTest`, `AiTaskFlowTest`; tài liệu `docs/guides/ai-api-keys.md` (mục cấu hình model).

### Tiến độ G3 (2026-11-20)

- [x] `AiProperties.Provider` thêm `reasoningModels`, `reasoningEffort`, `reasoningReserveTokens` (mặc định 4.096); `ProviderCatalog` thêm `resolveOutputCap`, `readReasoningEffort`, `readReasoningReserveTokens`.
- [x] `task.maxOutputTokens` vẫn là phần trả lời; giữ quota (`AiTaskService`, đổi kết nối trong `AiTaskJobHandler`) và lời gọi (`dispatch`) đều tính trần qua `resolveOutputCap`, nên không cộng dồn khi đổi kết nối.
- [x] `IProviderGateway.Request` thêm `reasoningEffort`, `reasoningTokens` (constructor cũ giữ nguyên). Gemini 2.x: `thinkingBudget` = phần chừa; Gemini 3+: `thinkingLevel`; OpenAI: `reasoning_effort`; Claude: không gửi. Bỏ quy tắc cũ "thinkingBudget = 1/4 trần".
- [x] `application.yml`: Gemini và OpenAI trần 16.384, liệt kê model có suy nghĩ; Gemini effort để trống sau khi đo (ban đầu `low`); OpenAI effort để trống (chỉ chừa token, chưa kiểm bằng gọi thật); Claude không có model suy nghĩ.
- [x] Bộ đánh giá đọc `AI_EVAL_REASONING_EFFORT`, `AI_EVAL_REASONING_RESERVE` để áp cùng quy tắc.
- [x] Test: `HttpProviderGatewayTest` (Gemini 2/3, OpenAI có/không effort), `KeyCipherAndCatalogTest` (trần, effort hợp lệ, Spring bind giá trị trống). Unit test ai/assignments/assignmentwork/grading/groupdocs/quizzes + ArchitectureTest: 926 đạt, 8 bỏ qua.
- [x] Gọi thử Gemini 3.5 Flash Lite với `thinkingLevel=low`: nhận tham số (không HTTP 400), không báo token suy nghĩ, đầu ra 4.190 token.
- [x] Đo hai cách A (chỉ chừa) và B (chừa + low) trên bài 04, 06, 08 × 3: A 0 lần bị cắt, B 1 lần; chốt mặc định Gemini là A (effort trống). Kết quả mục 5 của file kết quả.

### G4, G2 (2026-11-21)

- [x] G4: làm tròn điểm mục dư chữ số thập phân khi vẫn trong 0..max (`GradingProposalNormalizer`), cập nhật BR-U09-32 và test.
- [x] G2: `(nét liền)` cho cạnh stereotype không nét đứt (`DiagramXml`), câu chỉ dẫn tương ứng.
- [x] 928 unit test đạt; live bài 04 về đúng 3,75; replay xác nhận G4. Chi tiết mục 6 file kết quả.
- [ ] Chạy lại cả bộ 8 bài × 3 sau mọi sửa để có số tổng hợp mới.
- [x] Chạy lại cả bộ 8 bài × 3 sau G1–G4 (2026-11-21): 9,46/10, sai số điểm 2,3%, kappa 0,84; phát hiện G5 (viết lan man bị cắt) và G6 (cho điểm tính chất của thành phần vắng). Mục 7 file kết quả.
- [x] Thử G6 và G5 bước 1 bằng prompt (2026-11-21): cả hai chưa đạt khi đo (G6 do câu chữ rubric mơ hồ; G5 vẫn 1/5 bị cắt). Mục 8 file kết quả. Chờ người dùng chọn bước tiếp.
- [x] Theo đề xuất (2026-11-21): giữ chỉ dẫn G6, bỏ hướng dẫn độ dài G5; bộ đánh giá lưu phần chữ khi bị cắt; 5.2 một phần — `RubricEditor` gợi ý khi tiêu chí chỉ nêu trường hợp bị trừ. Test backend 496, frontend 149 đạt. Mục 9 file kết quả.
- [ ] G5: chưa bắt được lần bị cắt để chẩn đoán (lần đo gặp 503). Rubric mẫu cho môn chờ app chạy.

### Kiểm tra điểm tối đa khi actor phụ không khớp (2026-11-21)

Kết quả Gemini bài 08 theo hai rubric cho thấy 5/10 lượt nhận tối đa ở mục gộp tên use case, actor chính và actor phụ dù bảng ghi sai actor phụ. Rubric mẫu không quy định điểm cho từng thành phần. Bước kiểm tra bổ sung dùng tiêu chí đã khóa và bảng cùng phần để chuyển điểm tối đa có mâu thuẫn cho giảng viên duyệt. Prompt xử lý theo cách tính điểm hoặc tên tương đương mà đề và rubric cho phép.

- [x] Bổ sung ca cho điểm tối đa khi actor sai, điểm thành phần khi actor sai và điểm tối đa khi actor đúng.
- [x] Xác nhận ca sai actor có thể lọt qua bản hiện tại.
- [x] Sửa prompt chung và kiểm tra đề xuất provider, MCP cùng pipeline đánh giá.
- [x] Chạy bộ test backend và phát lại bộ phản hồi Gemini đã lưu. Bộ test grading cuối đạt 96/96. Cả 5 lượt tối đa nhầm được đánh dấu. Ca p95 `U04_SCOPE` trượt trong lượt chạy toàn bộ và đạt khi chạy lại.

### Thiếu trường nhận xét trong phản hồi provider (2026-11-21)

- [x] Tái hiện 5 phản hồi bị từ chối trước khi vào bước chuẩn hóa. Các ca gồm minh chứng hoặc lý do duyệt `null` hay bỏ qua và gợi ý hoặc vị trí minh chứng tùy chọn bị bỏ qua.
- [x] Kiểm kiểu tại bước nhận kết quả, dùng validator grading kiểm yêu cầu minh chứng sau khi xác định cờ và lý do cần xem. Giữ schema MCP đầy đủ.
- [x] 255 test grading, gateway và MCP đạt. Phát lại 24 lần ghi của Gemini, 17 phản hồi qua kiểm định dạng và 7 lần gọi lỗi đã có trong bản ghi.
- [x] Chạy lại trên cùng rubric và bài tổng hợp 01-complete. Bản trước sửa thất bại 5/6 ca, bản hiện tại đạt 6/6. Nhóm backend grading, gateway, MCP và luồng tích hợp đạt 359 ca. Frontend grading đạt 223 ca.
- [x] Gọi Gemini thật với 01-complete ba lần. Một lượt hợp lệ đạt 4,00/4,00 với đủ 13 mục. Hai lượt trả HTTP 503.
