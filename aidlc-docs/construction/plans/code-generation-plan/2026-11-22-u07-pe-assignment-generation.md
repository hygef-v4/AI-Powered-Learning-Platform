# U07 — Cải thiện tạo đề AI theo mẫu PE SWR302 và SWD392

**Trạng thái** · Được duyệt và triển khai ngày 2026-11-22. Xem kết quả tại [báo cáo U07](../../build-and-test/2026-11-22/2026-11-22-u07-pe-assignment-evaluation.md).

**Mục tiêu** · Đề sinh ra có đủ đầu ra cần nộp, rubric khớp câu hỏi và thang điểm đã chốt, đồng thời đưa các mâu thuẫn trong mẫu ra cho giảng viên xem trước.

**Cách triển khai** · Dùng luồng `Tạo từ AI`, AI task, schema và validator hiện có. Bổ sung rule cho `AUTO`, kiểm điểm mẫu theo từng câu bằng Java và dùng lại màn hình xem trước đề xuất khi cần giảng viên xử lý.

**Phạm vi và căn cứ** · U07 tạo đề, rubric và xem trước, với luồng tải nguồn U03/U13 và tác vụ AI U06 hiện có. Đối chiếu 14 đề SWR302 và 6 đề SWD392 khác nhau, tính cả đề thi lại và đề luyện. [Thiết kế U07 trước đó](../2026-10-24-ai-assignment-swr-swd-plan.md) · [Kế hoạch triển khai U07 hiện có](u07-ai-authoring-code-generation-plan.md)

## Ràng buộc

- Giữ schema đề hiện có, các section sinh mới ở cấp 1, chế độ `AUTO` mặc định, lựa chọn VI/EN và tổng 10 điểm mặc định khi tạo theo bố cục thông thường. Giữ luồng sửa đề, nhập đề, soạn tay, sao chép và MCP. Tập trung vào prompt **tạo đề U07**. Prompt chấm bài, dataset và báo cáo U09 thuộc phạm vi riêng.
- Dùng quyền truy cập mẫu hiện có, version khi áp dụng, requestRef và trạng thái DRAFT. Bản ghi đề xuất cũ vẫn mở được. Giữ giới hạn provider và file nguồn hiện tại. Các bước này không cần migration hay provider mới.
- Chỉ gửi dữ liệu đề tự soạn trong lượt so sánh model. Bộ đề gốc và tài liệu của trường dùng đối chiếu cục bộ. Không đưa tài liệu gốc, bài nộp hay thông tin tài khoản vào fixture, log, PR hoặc report chia sẻ.

## Step 1 — Giữ câu con và làm rõ `AUTO`

**File** · `backend/src/main/java/edu/aiplatform/assignments/service/AssignmentDraftPromptBuilder.java`, `AssignmentAiDraftService.java` và `backend/src/test/java/edu/aiplatform/assignments/service/AssignmentDraftPromptBuilderTest.java`, `AssignmentAiDraftServiceTest.java`. Giữ chữ ký `buildPrompt(Assignment, String, List<MaterialText>, DraftOptions)`.

- [x] Dựng sáu brief tổng hợp ở Step 4. Trước khi sửa, lưu prompt hiện tại và chạy baseline nếu đã có provider được phép dùng. Cố định yêu cầu, mẫu, model và cấu hình để so với lượt sau. Nếu chưa thể gọi model, lưu prompt và báo rõ phần baseline live còn thiếu.
- [x] Thêm test cho ngữ cảnh `AUTO`, yêu cầu giữ các ý a/b và rubric tương ứng, ưu tiên cài đặt giảng viên đã chọn, ghi nhận mâu thuẫn, và dữ liệu mẫu được phân cách với chỉ thị dựng prompt. Chạy test và ghi nhận lỗi trước khi sửa.
- [x] Dùng mẫu để học thứ tự, số câu, tên loại đầu ra và bố cục. Nếu giảng viên đã xác nhận lựa chọn khác, ưu tiên cấu hình đó. Tách hướng dẫn xử lý mẫu do backend tạo khỏi khối yêu cầu tự do. Giữ văn bản/ảnh nguồn trong các khối dữ liệu không tin cậy.
- [x] Giữ câu chính của mẫu ở section cấp 1. Ghi các ý a/b và đầu ra trong `guidance`, với một mục rubric cho mỗi yêu cầu có điểm riêng. Dùng `DIAGRAM` cho mục cần xem sơ đồ và `TEXT` cho phần giải thích, khi phù hợp. Yêu cầu từng mục rubric truy được về câu hỏi, điểm công bố khớp tổng các mục và nội dung dành cho sinh viên không có lời giải mẫu.
- [x] Cho `AUTO` nhận diện theo đầu ra được yêu cầu. SWR có thể cần phạm vi, actor, luồng ngoại lệ, quy tắc nghiệp vụ hoặc NFR có chỉ số, điều kiện và cách đo. SWD có thể cần phân biệt conceptual/logical ERD, cấp độ Class, đối tượng của Statechart, chức năng và nhánh lỗi của Sequence hoặc cách chia điểm pattern. Chỉ yêu cầu ký pháp, công cụ, kiến trúc, định dạng nộp và chế tài khi đề bài hoặc giảng viên quy định. Đề ngoài SWR/SWD đi theo yêu cầu trực tiếp.
- [x] Chạy lại test prompt và schema. Đối chiếu các brief tổng hợp ở Step 4 để kiểm tra model có thực sự tuân theo, đồng thời kiểm tra đề không lẫn tên actor/thực thể giữa các câu.

## Step 2 — Kiểm đúng điểm từng câu khi giữ thang điểm mẫu

**File** · `AssignmentRequests.java`, `AssignmentCommands.java`, `AssignmentAiDraftService.java`, `AssignmentDraftValidator.java`, `frontend/src/features/assignments/api/index.ts`, `components/CreateAssignmentDialog.tsx` trong các thư mục tương ứng. Kiểm thử trong `AssignmentAiDraftServiceTest`, `AssignmentDraftValidatorTest` và `frontend/tests/features/assignments/components/assignments.test.tsx`.

- [x] Thêm trường boolean tùy chọn `preserveTemplateQuestionPoints` cho yêu cầu tạo AI, mặc định tắt. Hiện lựa chọn **Giữ điểm từng câu của đề mẫu** khi chọn assignment đã lưu cho bài có chấm điểm. Khi bật, frontend gửi ID mẫu và lựa chọn giữ điểm để backend lấy số câu và tổng từ mẫu đã xác thực. Luồng thông thường tiếp tục gửi tổng 10. File DOCX và OCR tiếp tục làm nguồn bố cục.
- [x] Viết ca mẫu `0.50 / 2.50 / 4.00 / 3.00`. Đầu ra `0.50 / 3.00 / 3.50 / 3.00` phải bị chặn dù tổng bằng 10. Kiểm tra thêm đề mẫu có rubric thiếu, quyền đọc mẫu không hợp lệ, lựa chọn giữ điểm trên bài luyện tập và yêu cầu cũ chỉ đặt tổng điểm. Chạy trước khi sửa.
- [x] Lấy đề mẫu theo quyền hiện có. Dùng `PartCalculator` và rubric đã lưu để xác định điểm theo thứ tự câu chính. Bản đầu hỗ trợ mẫu có 1–10 câu H1 được gắn rubric trực tiếp, đủ điểm dương hợp lệ và tổng trong giới hạn `DraftOptions`. Mẫu không xác định được tất cả điểm trả thông báo trước khi gọi AI, cho phép dùng lại mẫu theo bố cục. Cấu hình API có số câu hoặc tổng khác mẫu trả lỗi rõ ràng trước khi gọi AI.
- [x] Lưu danh sách `questionPoints` theo thứ tự vào provenance hiện có và gửi cùng số câu, tổng điểm trong prompt. Lượt callback và apply đọc cùng danh sách đã lưu. So tổng rubric từng section bằng `BigDecimal.compareTo`, trả `QUESTION_POINTS_MISMATCH` ở section bị lệch và chặn áp dụng. Với yêu cầu cũ, danh sách điểm có thể vắng mặt. Kiểm tra lại bằng ca hợp lệ, ca cùng tổng sai điểm từng câu và ca đề mẫu bị sửa sau khi đã tạo task.

## Step 3 — Hiển thị mâu thuẫn nguồn trước khi áp dụng

**File** · `AssignmentDraftGenerationSchema.java`, `AiDraftProposal.java`, `AssignmentDraftValidator.java`, `AssignmentAiDraftService.java` và frontend tại `CreateAssignmentDialog.tsx`, `ClassAssignmentsSection.tsx`, `SubjectAssignmentListPage.tsx`. Dùng lại `AiDraftDialog` và `AiDraftPreview` qua `proposalId`. Sửa nhánh tự áp dụng trong `AiDraftDialog` để chờ giảng viên xác nhận khi có cảnh báo nguồn.

- [x] Thêm regression với đề tổng hợp có câu yêu cầu liệt kê trạng thái `Order` và vẽ state diagram cho `Dish`. Output hợp schema phải báo đúng đối tượng mâu thuẫn, hiển thị cảnh báo khi mở bản nháp và chưa gọi `:apply`. Ca `VALID` vẫn được ghi vào bản nháp theo luồng hiện tại. Ca `INVALID` do điểm lệch vẫn bị chặn.
- [x] Bổ sung `authoringWarnings` cho schema sinh đề, danh sách tối đa 5 mục với mã `TEMPLATE_CONFLICT` hoặc `MISSING_REQUIRED_REFERENCE` và `message` tối đa 300 ký tự. Danh sách rỗng khi không có lỗi nguồn phát hiện được. Model chỉ ra câu hoặc đầu ra cần xác nhận, không tự chốt dữ kiện có nhiều cách hiểu. Lưu cảnh báo hợp lệ trong `validation.issues` của đề xuất ở mức `WARNING`. Đề cũ có thể thiếu trường này. Chỉ nới schema ở luồng sinh đề mới.
- [x] Với hai mã cảnh báo nguồn, gọi `onCreated(assignment, proposalId)` để mở assignment với proposal và giữ `classId` nếu có. Dùng lại phần hiển thị cảnh báo và nút áp dụng thủ công trong preview. Kiểm tra cả tạo bài trong lớp và tạo mẫu môn học. Schema từ chối cảnh báo sai định dạng, UI hiển thị nội dung hợp lệ dưới dạng văn bản.

## Step 4 — Hồi quy và so sánh prompt

- [x] Đo trên sáu brief đã chuẩn bị ở Step 1. Dùng cùng yêu cầu, mẫu, model và cấu hình giữa hai lượt. Mỗi lượt sinh một lần cho mỗi brief. Lưu phản hồi gốc và ghi riêng lỗi provider.

| Brief | Tiêu chí kiểm tra |
|---|---|
| SWR kiểu FA25 | Bìa SRS, Context, Use Case và Conceptual ERD giữ điểm `0.50 / 2.50 / 4.00 / 3.00`. |
| SWR kiểu SP25 | Sơ đồ tổ chức, đặc tả use case, actor chính/phụ và NFR đo được. Chỉ chấm nét đứt khi đầu vào yêu cầu. |
| SWD kiểu REST | Conceptual và Logical ERD có yêu cầu riêng. Kiểm tra phân lớp và phân quyền nếu đề yêu cầu. Phần pattern giữ ba đầu ra `0.50 / 1.00 / 0.50`. |
| SWD kiểu Statechart | Đúng đối tượng có vòng đời, trạng thái, sự kiện và guard. Chấp nhận lựa chọn kiến trúc phù hợp nếu đề cho phép. |
| SWD mẫu mâu thuẫn | Chỉ ra điểm xung đột giữa hai đối tượng. Mở xem trước và giữ proposal mà chưa áp dụng. |
| Đề tổng hợp ngoài SWR/SWD | Tuân theo đầu ra yêu cầu và ngôn ngữ đã chọn. Giữ cách tạo theo bố cục 10 điểm mặc định. |

- [x] Chạy ba lớp kiểm tra. Maven kiểm prompt, schema, điểm và áp dụng bản nháp. Vitest kiểm thao tác tạo và review. Chrome chạy trên app local với API mock cho đề hợp lệ và đề có cảnh báo. Đã kiểm tra trạng thái DRAFT và nội dung trước/sau khi bấm áp dụng. Kết quả và lệnh chạy ghi trong báo cáo U07.

```powershell
# Chạy từ thư mục gốc của repo. Dùng Maven đã cài hoặc mvn.cmd trong wrapper cache trên máy này.
mvn -f backend/pom.xml '-Dtest=AssignmentDraftPromptBuilderTest,AssignmentDraftGenerationSchemaTest,AssignmentDraftValidatorTest,AssignmentAiDraftServiceTest,AssignmentMcpAuthoringServiceTest' test
npm --prefix frontend run test -- tests/features/assignments/components/assignments.test.tsx tests/features/assignments/components/AiDraftDialog.test.tsx tests/features/assignments/components/AiDraftPreview.test.tsx
npm --prefix frontend run typecheck
git diff --check
```

- [x] Ghi số câu, đầu ra, điểm từng câu, bằng chứng cần có cho rubric, cảnh báo mâu thuẫn và quy định tự thêm cho từng brief. Điều kiện chốt đợt đầu là các ca xác định bằng code đạt, rubric bám từng đầu ra chính và app mở xem trước đúng cho ca mâu thuẫn. Cho phần sinh thật, công bố 6 kết quả riêng, điểm khác biệt với baseline và ca chưa chạy. Mở rộng lên 20 brief tự soạn nếu cần thêm bằng chứng trước khi phát hành.

## Ảnh đề gốc

Đợt đầu dùng bản OCR đã đối chiếu cục bộ thành một TXT/DOCX, hoặc 1–3 PNG/JPEG đã kiểm theo giới hạn luồng Tạo từ AI. Chỉ nhập PDF vào luồng tạo AI khi PDF có văn bản trích được. Thêm WebP trực tiếp là việc riêng vì whitelist ảnh của U06 hiện chỉ có PNG/JPEG. Nếu làm tiếp, kiểm tên tệp, loại MIME khớp nội dung, thứ tự trang và định dạng ảnh thực sự gửi tới provider trước khi tăng số trang.

## Step 5 — Sửa ba điểm phát hiện qua lượt đo lặp

- [x] Dùng tên hiển thị ở hai đầu cạnh trong dữ liệu gửi U09, kèm ID để phân biệt các hình trùng tên. Giữ hướng cạnh và nét thật từ XML. Chấm lại sơ đồ dùng ID cũ dễ gây nhầm.
- [x] Đánh dấu bản nháp sinh từ đề mẫu đã lưu hoặc DOCX để mở xem trước kể cả khi `VALID`. Kiểm thử mở bản xem trước chưa làm thay đổi đề, thao tác **Áp dụng đề xuất** mới ghi nội dung.
- [x] Thêm cấu hình tùy chọn điểm từng câu cho đề có chấm điểm. Backend xác định số câu và tổng từ dãy điểm đã chọn, lưu cùng tác vụ và kiểm chính xác ở callback và apply. Chạy lại sáu brief với thang điểm được chốt.

## Step 6 — Chốt số câu độc lập với điểm

- [x] Thêm ô số câu tùy chọn trong `CreateAssignmentDialog.tsx`, dùng chung bộ nhập số câu với `AiDraftDialog.tsx`. Bỏ trống thì không gửi số câu. Khi nhập điểm từng câu, hiển thị số câu và tổng suy ra từ dãy điểm. Khi giữ thang điểm mẫu, lấy số câu từ mẫu.
- [x] Trong `AssignmentDraftPromptBuilder.java` và `AssignmentDraftGenerationSchema.java`, ưu tiên số câu đã chốt và yêu cầu cảnh báo khi yêu cầu tự do ghi rõ con số khác. Dùng luồng xem trước và thao tác sửa yêu cầu hiện có. Giữ kiểm tra số câu và tổng điểm trước khi áp dụng.
- [x] Kiểm bằng Maven, Vitest, typecheck và test đề cùng tổng nhưng sai số câu. Chạy Luna medium với ca 4 câu cấu hình và 5 câu trong lời nhắc, cùng ca đối chứng hai bên đều ghi 4. [Kết quả từng ca](../../build-and-test/2026-11-22/2026-11-22-u07-pe-assignment-evaluation.md#khi-số-câu-trong-cấu-hình-và-lời-nhắc-khác-nhau)

## Tuân thủ AI-DLC

- Security áp dụng cho quyền đọc rubric mẫu, ràng buộc API, nguồn không tin cậy và phản hồi lỗi. Xác minh bằng mẫu thiếu quyền và dữ liệu sai định dạng.
- Resiliency áp dụng cho việc tái sử dụng timeout, idempotency, lưu proposal và xử lý lỗi provider theo luồng hiện tại. Hạ tầng triển khai, health checks và rollback sẽ dùng quy trình hiện có khi phát hành.
- Property-based testing đang tắt theo cấu hình dự án. Dùng các ca hồi quy cụ thể ở trên.
