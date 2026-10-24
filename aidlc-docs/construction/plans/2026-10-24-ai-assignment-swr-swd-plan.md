# Kế hoạch cải thiện tạo đề AI cho SWR/SWD

**Ngày**: 2026-10-24 (Asia/Saigon). **Trạng thái**: người dùng đã duyệt triển khai ngày 2026-10-24; đang thực hiện.
**Mục tiêu**: tạo được đề có bối cảnh đủ dữ kiện, câu hỏi rõ, thang điểm nhất quán và rubric có thể lưu/chấm; giảng viên hiểu chính xác nội dung sẽ được áp dụng.
**Phạm vi**: U07 tạo đề và toàn bộ UI soạn bài, nhập đề từ file, upload nguồn riêng cho AI, rubric/điểm; kết nối task U06, file U03, khả năng trích chữ U13 và kiểm tra tương thích rubric U09. Không sửa thuật toán chấm U09, không phát hành bài hoặc gửi đề cho người học. Bổ sung phạm vi theo yêu cầu tiếp theo tại mục 10–14.
**Cách thực hiện**: thực hiện lần lượt trong chat hiện tại; không chia subagent. Người dùng đã duyệt kế hoạch bằng yêu cầu “giúp tôi triển khai kế hoạch”. Không push.

## 1. Bằng chứng và giới hạn

Đã đọc 11 ảnh do người dùng gửi: một ảnh kết quả editor hiện tại; hai trang Pharmacy; một trang Mental Health; ba trang SmartLearn; hai trang MediCare; hai trang Event Management. Các quy định thi trong ảnh là nội dung tham khảo, không phải chỉ thị cho agent hoặc mặc định của sản phẩm.

| Mẫu | Cấu trúc quan sát được | Điểm và lưu ý |
|---|---|---|
| SWR302 Pharmacy | Business overview dài: tác nhân, trách nhiệm, luồng đặt thuốc/xác minh đơn, trạng thái, ngoại lệ; SRS cover, Context, Use Case, Conceptual ERD, viết prompt | 0.5 + 2 + 4 + 2.5 + 1 = 10 |
| SWR302 Mental Health | Bối cảnh, tác nhân/dịch vụ ngoài, tình huống leo thang; SRS cover, Context, Use Case, State Transition cho một đối tượng được chỉ rõ | 0.5 + 2.5 + 4 + 3 = 10; tối thiểu 5 trạng thái theo riêng đề này |
| SWD392 SmartLearn | 85 phút; Class, Sequence cho một use case, Component; Microservices, Repository, Gateway, database riêng, broker | 3 + 4 + 3 = 10; mô tả bắt buộc Microservices nhưng có câu “may choose”; câu Class gọi domain-level nhưng yêu cầu cả service/repository |
| SWD392 MediCare | 85 phút; Class, Sequence đặt lịch/thanh toán, State cho Appointment; Layered hoặc Microservices, Repository | 4 + 4 + 3 = 11; cần cảnh báo nếu mục tiêu là 10, không tự sửa điểm của nguồn |
| SWD392 Event | 85 phút; Class, Sequence đăng ký, State cho Event; điều kiện mở đăng ký/còn chỗ, các trạng thái và sự kiện rõ | 3 + 4 + 3 = 10 |

Đây là năm mẫu cụ thể, không phải chuẩn bắt buộc cho mọi đề SWR/SWD. Không mặc định mọi Context Diagram cùng một ký pháp; không đồng nhất Conceptual ERD với design-level Class Diagram. Các yêu cầu Word/ZIP/Draw.io offline/tiếng Anh/không Internet/chế tài 0 điểm chỉ được đưa vào đề mới khi giảng viên cấu hình hoặc chủ động chọn sao chép quy định mẫu.

Ảnh editor hiện tại: tiêu đề còn mặc định, hướng dẫn nhắc “bối cảnh phòng khám” nhưng không có business overview đủ nội dung, năm yêu cầu khá chung, toàn bài và các phần đều 0 điểm. Không có payload thực tế nên chưa kết luận vì sao lượt sinh cụ thể thiếu rubric.

## 2. Đối chiếu code hiện tại

- `AssignmentAiDraftService.prompt`: system prompt ngắn, bắt buộc tiếng Việt; gửi hình thức, grading mode, bật/tắt sơ đồ và requiredDiagrams. Chưa có profile môn, ngôn ngữ đầu ra hoặc tổng điểm mục tiêu có cấu trúc.
- Cài đặt bật sơ đồ được diễn đạt thành “có dùng sơ đồ Draw.io”; đây có thể là nguồn của yêu cầu Draw.io trong kết quả trước, không thể khẳng định AI tự bịa hoàn toàn.
- `AiDraftProposal.OUTPUT_SCHEMA`: chỉ có instructions và sections; rubric không bắt buộc. Chưa có title/businessContext/validation metadata.
- `AiDraftProposal.normalize`: cho rubric rỗng; điểm không hợp lệ có thể thành thiếu điểm; chưa kiểm tổng điểm hay độ đầy đủ theo môn. Đây là đường dẫn có thể cho phép áp dụng đề 0 điểm.
- `AssignmentAiDraftService.apply`: thay skeleton, tính lại leaf parts, thay toàn bộ rubric; tùy chọn thay instructions. Không cập nhật title. Không có lựa chọn append, sửa bản preview hoặc undo riêng cho thao tác AI.
- `PartCalculator`: chỉ heading nhỏ nhất của mỗi nhánh là phần chấm; nội dung trước heading đầu là phần chung. Phải giữ quy tắc này khi thêm business overview để không biến bối cảnh thành một câu chấm điểm.
- `AiDraftDialog`: polling proposal mỗi 2 giây, chỉ hiện một dòng PENDING; nút không có spinner, không có thời gian đã chờ/ước lượng, rubric preview chỉ là dòng tóm tắt. Ô thay instructions mặc định bật.
- Đã có `shared/ai/AiTaskStatus` và `AiSourcePicker`; có thể tái sử dụng trạng thái/nguồn thật, tránh xây luồng provider riêng cho U07.

## 3. Các phương án và lựa chọn đề xuất

1. Chỉ tăng prompt: ít thay đổi, nhưng JSON hợp schema vẫn có thể thiếu rubric, sai tổng điểm hoặc thiếu bối cảnh.
2. **Đề xuất: AI tự suy ra kiểu đề từ yêu cầu tự do/tài liệu + prompt có phiên bản + schema + validator backend + preview rõ ràng**. Không yêu cầu giảng viên chọn nhãn SWR/SWD. Nếu nội dung gợi rõ phân tích yêu cầu hoặc thiết kế phần mềm, AI dùng cấu trúc phù hợp; nếu không, làm theo dạng bài được mô tả mà không ép phân loại. Tính toán điểm và kiểm bắt buộc bằng code; đánh giá chất lượng nghiệp vụ để giảng viên review. Dùng chung luồng AI task hiện có.
3. Thêm một AI reviewer cho mọi lượt sinh: có thể góp ý ngữ nghĩa nhưng tăng thời gian/chi phí và vẫn không bảo đảm đúng. Hoãn; MVP không tự phát sinh lượt gọi AI thứ hai.

Đây là phương án được người dùng duyệt để triển khai; các giới hạn và bố cục UI ghi là đề xuất MVP sẽ được áp dụng như giới hạn sản phẩm hiện tại, không hardcode thành quy tắc bất biến.

## 4. Đầu vào và cấu trúc đề

Giữ ô yêu cầu tự do làm đầu vào chính; không hiển thị bộ chọn SWR/SWD. AI suy ra dạng bài từ yêu cầu và file nguồn khi có căn cứ, nếu không thì bám yêu cầu thông thường. Giảng viên kiểm soát riêng ngôn ngữ đầu ra, số yêu cầu chính, tổng điểm mục tiêu, thời lượng nếu cần, các loại sơ đồ/câu hỏi, kiến trúc, mẫu hoặc yêu cầu nộp bài. `AUTO` là chế độ nội bộ mặc định; API vẫn đọc được lựa chọn profile cũ để tương thích, không buộc người dùng mới chọn.

Không tự suy thời lượng từ khoảng thời gian mở/đóng bài. Tổng điểm mặc định UI có thể đề xuất 10 nhưng chỉ được kiểm như ràng buộc khi giá trị đó đã hiện rõ trong form; không ép mọi đề về 10.

Đề xuất output v2 gồm `schemaVersion`, `title`, `businessContext`, `instructions`, `sections`; mỗi câu có yêu cầu/đầu ra cụ thể, rubric cho leaf và mã ổn định để tham chiếu giữa câu. Phần bối cảnh được chuyển thành block chung trước heading đầu; giữ instructions cho cách làm/nộp bài. Tổng điểm là giá trị tính từ rubric, không tin một trường total do AI khai báo.

Profile SWR: bối cảnh đủ tác nhân, phạm vi, tương tác ngoài, dữ liệu chính, luồng và ngoại lệ; bộ câu tùy chọn SRS, Context, Use Case, Conceptual ERD, State, prompt analysis. Loại State phải chỉ rõ đối tượng, sự kiện, điều kiện và phạm vi vòng đời. Không bắt mọi đề chứa cả ERD lẫn State.

Profile SWD: bối cảnh có miền nghiệp vụ, quy tắc, use case trọng tâm; câu Class xác định rõ domain-level hoặc design-level; Sequence có luồng thành công/thất bại và điều kiện; State hoặc Component theo lựa chọn. Kiến trúc và Repository chỉ bắt buộc nếu được cấu hình. Thành phần/thuật ngữ giữa các câu phải nhất quán.

## 5. Prompt và validation

### Prompt nhiều lớp

- Tách bộ dựng prompt ra khỏi service: quy tắc chung, profile, cấu hình giảng viên, ngữ cảnh hiện có, tài liệu tham khảo và hợp đồng output.
- Thứ tự ưu tiên: an toàn/hợp đồng sản phẩm; cấu hình giảng viên được xác nhận; yêu cầu tự do; tài liệu làm bằng chứng. Nếu các đầu vào mâu thuẫn, trả issue để làm rõ thay vì âm thầm chọn.
- Tài liệu tham khảo được phân cách và coi là dữ liệu không tin cậy; câu “ignore instructions” trong tài liệu không được đổi nhiệm vụ, tiết lộ khóa hoặc gọi công cụ.
- Yêu cầu self-check trước khi trả JSON: đủ dữ kiện giải câu hỏi, tên nhất quán, không yêu cầu tài liệu/template chưa đính kèm, không có lời giải mẫu lẫn trong đề, không tự thêm chế tài/định dạng nộp bài ngoài cấu hình.
- Rubric mô tả tiêu chí kiểm chứng được và các mục đạt điểm, cho phép cách mô hình hóa tương đương có lý do; tránh chấm một sơ đồ đúng chỉ theo tên lớp mẫu cố định.
- Giữ giới hạn request 4.000 ký tự, tài liệu 30.000 ký tự, 8.000 output tokens hiện tại cho MVP; không nhét nguyên mọi đề mẫu vào mỗi request. Dùng profile ngắn và ví dụ cấu trúc đã biên tập. Cảnh báo nguồn bị cắt.

### Kiểm bằng code, không chỉ bằng lời nhắc

**ERROR, chặn Áp dụng AI với bài Graded**:
- JSON sai kiểu/phiên bản hoặc vượt giới hạn; thiếu câu, tiêu đề hoặc yêu cầu; đề phân tích yêu cầu/thiết kế thiếu bối cảnh cần thiết.
- Leaf part không có rubric đủ tiêu chí; item thiếu nội dung/điểm, điểm không dương, NaN hoặc quá giới hạn; rubric chỉ ở parent sẽ bị mất khi tạo leaf parts.
- Tổng rubric khác tổng điểm mục tiêu hoặc tổng câu khác tổng item; dùng BigDecimal/đơn vị phần trăm điểm, không so sánh float.
- Tham chiếu mã câu/đối tượng không tồn tại; loại sơ đồ không được nền tảng hỗ trợ hoặc mâu thuẫn rõ với cấu hình tắt sơ đồ; yêu cầu tệp nộp không được nền tảng hỗ trợ.
- Khi apply: kiểm lại validation, quyền, trạng thái DRAFT và version trong transaction; không chỉ dựa nút disable phía frontend.

**WARNING, giảng viên review**: thuật ngữ không nhất quán theo kiểm tra hỗ trợ, thiếu thông tin nghiệp vụ để giải câu hỏi, kiến trúc diễn đạt mơ hồ, mức domain/design lẫn nhau, khối lượng bài có vẻ quá thời lượng, yêu cầu không có căn cứ trong nguồn. Không tuyên bố các kiểm tra này chứng minh đề đúng ngữ nghĩa; validator xác định chỉ đảm bảo những ràng buộc có thể kiểm chắc bằng code.

Trả `validation.status` (`VALID`, `NEEDS_REVIEW`, `INVALID`) và `issues` có code, severity, đường dẫn section/item, giải thích và hướng sửa. Không hiển thị READY đơn thuần như “đề đã hoàn chỉnh”. Task hoàn tất và chất lượng đề là hai khái niệm riêng.

Kết quả sai được giữ để giảng viên thấy lỗi; không âm thầm bỏ câu, bỏ rubric hoặc sửa tổng điểm. MVP cho sửa cấu hình rồi tạo lại bằng thao tác chủ động, không retry provider tự động khi kết quả UNKNOWN hoặc tự gọi sửa nội dung có tính phí.

## 6. UI dự kiến

1. **Yêu cầu tạo đề**: nhập mô tả tự do; không cần biết hay chọn SWR/SWD. Chọn ngôn ngữ, số yêu cầu chính, tổng điểm và tùy chọn nâng cao nếu cần. Hiện tài liệu được chọn, tình trạng trích chữ và cấu hình sơ đồ ảnh hưởng đến prompt.
2. **Đang tạo**: spinner và khóa gửi lặp ngay từ lúc submit; thời gian đã chờ; QUEUED/RUNNING lấy từ task thật. Không bịa các giai đoạn mà backend không cung cấp; “Đang kiểm tra đề” chỉ hiện khi validation thực sự đang diễn ra. Poll lỗi có thông báo/thử lại, dọn timer khi đóng.
3. **Thời gian dự kiến**: MVP có elapsed và giải thích phụ thuộc model/tài liệu. Chỉ hiện khoảng ước lượng khi có đủ lịch sử cùng provider/model và nhóm độ dài; không đặt một khoảng giây tùy ý, không đếm ngược hay phần trăm giả. Telemetry chỉ metadata thời gian/trạng thái, không lưu key/nội dung bài hoặc dữ liệu sinh viên.
4. **Xem trước**: Tiêu đề → Bối cảnh nghiệp vụ → Hướng dẫn làm/nộp bài → Câu hỏi và điểm → Rubric chi tiết → Kết quả kiểm tra. Hiện tổng ví dụ “10/10 điểm”, “5 câu, 5 câu đủ rubric”; lỗi có liên kết tới câu tương ứng.
5. Bài trống: “Thêm hướng dẫn làm bài do AI tạo”. Đã có instructions: “Thay hướng dẫn làm bài hiện tại”, mặc định không chọn thay khi có nội dung. Cho xem trước cũ/mới. Đổi nhãn không thay đổi ý nghĩa dữ liệu instructions hiện tại.
6. Nút “Thêm vào bài tập” khi trống; khi có nội dung ghi rõ “Thay nội dung bài tập” và phạm vi thay khung/rubric/tiêu đề/hướng dẫn. Tiêu đề mới chỉ áp dụng khi được chọn. Chặn apply khi có ERROR, giữ nguyên đề cũ nếu hủy hoặc apply thất bại.
7. MVP vẫn thay khung theo cơ chế hiện có; append, chỉnh preview trực tiếp và hoàn tác toàn bộ là giai đoạn sau vì cần xử lý ID/điểm/version. Không quảng cáo các khả năng chưa có.

## 7. Kế hoạch thực hiện theo bước

### P1 — Hợp đồng đầu vào/đầu ra và bộ mẫu kiểm thử

Files: `AssignmentCommands.java`, `AssignmentAiDraftController.java`, `AssignmentViews.java`, `AiDraftProposal.java`, frontend `types.ts`, `api.ts`; bổ sung fixture SWR/SWD đã biên tập trong test resources và tài liệu hợp đồng U07 hiện có.

- [ ] Chốt request options và output v2; schema của AI_TASK và MCP dùng chung, sửa definition tool MCP tương ứng.
- [ ] Giữ đọc được proposal cũ instructions/sections; không tự đoán profile cho bản cũ. Legacy Graded thiếu rubric có issue và bị chặn apply cho tới khi có đề hợp lệ; thao tác soạn tay vẫn cho phép nháp chưa hoàn chỉnh.
- [ ] Kiểm thử hợp đồng cho v1/v2, thiếu optional options, English, AUTO suy ra loại phù hợp hoặc theo yêu cầu tự do không thuộc SWR/SWD, API profile cũ, kiểu lạ/vượt giới hạn và source material bị cắt.

### P2 — Prompt có profile và kiểm chất lượng backend

Files: `AssignmentAiDraftService.java`, `AiDraftProposal.java`; tạo `AssignmentDraftPromptBuilder.java`, `AssignmentDraftValidator.java`; test mới tương ứng và mở rộng `AssignmentAiDraftServiceTest.java`.

- [ ] Viết regression fail: JSON thiếu rubric vẫn được chấp nhận; tổng 11 với target 10; parent-only rubric; title mặc định không có proposal title; thiếu bối cảnh với đề phân tích yêu cầu/thiết kế.
- [ ] Dựng prompt ngắn, tự suy ra kiểu bài từ yêu cầu và nguồn khi có bằng chứng; cho phép ngôn ngữ được cấu hình; không tự sinh bài giải hoặc luật thi không được chọn.
- [ ] Áp dụng validation ở nhận kết quả, MCP save và apply; trả issue an toàn, giữ lifecycle và idempotency hiện tại.
- [ ] Test tài liệu có prompt injection, tham chiếu câu sai, yêu cầu template không tồn tại, loại diagram tắt/không hỗ trợ và decimal rounding.
- [ ] Xác nhận tổng điểm sau apply/reload đúng trước preview, businessContext không thành leaf chấm, rubric gắn đúng part; lỗi version không sửa dữ liệu.

### P3 — Form, preview và tiến độ

Files: `AiDraftDialog.tsx`, `types.ts`, `api.ts`, caller truyền current title/instructions; tái sử dụng `shared/ai/AiSourcePicker.tsx`, `AiTaskStatus.tsx`; tạo `AiDraftDialog.test.tsx`.

- [ ] Spinner ngay khi click; requestRef giữ cho retry cùng yêu cầu, đổi khi giảng viên chủ động tạo yêu cầu mới; tránh submit trùng và stale poll ghi đè draft mới.
- [ ] Dùng task status thật; xử lý poll error/UNKNOWN/FAILED/cancel race, bảo toàn yêu cầu đã nhập; không tự gửi lại lúc không biết provider đã nhận hay chưa.
- [ ] Hiển thị đủ bối cảnh, điểm/rubric và issues; wording instructions theo bài trống/có nội dung; cảnh báo phạm vi thay và chặn apply invalid.
- [ ] Test bàn phím, role=status/aria-live, focus, reduced motion, màn hình hẹp, timer cleanup, modal đóng khi đang chờ và preview rubric dài.

### P4 — Tích hợp và kiểm thử thật

- [ ] Backend: chạy nhóm assignments/AI contracts, schema tool MCP, architecture và rubric/grading compatibility. Frontend: dialog regression, assignments tests, lint, typecheck, build; full suite khi các nhóm đạt.
- [ ] Chạy fixture ngoại tuyến năm cấu trúc mẫu; MediCare target 10 phải báo tổng 11, case kiến trúc mâu thuẫn phải có warning. Không cần kết quả AI hoàn toàn giống mẫu.
- [ ] Test Gemini cá nhân trong app khi môi trường truy cập được: SWR Pharmacy (ERD), SWR Mental Health (State), SWD SmartLearn (Component), SWD Event (State); kiểm thêm prompt ngắn và tiếng Anh. Lượt thử thật chỉ gọi khi thực thi kế hoạch, ghi model/thời gian/kết quả, không ghi credential.
- [ ] Với mỗi lần hợp lệ: preview → apply vào bài QA DRAFT → reload → đối chiếu title/bối cảnh/câu/leaf rubric/tổng; giữ title/instructions cũ nếu không chọn thay; không publish hoặc tạo điểm sinh viên.
- [ ] Kiểm xuất đề/hiển thị phía người học theo khả năng sản phẩm và rubric U09 đọc được các phần vừa tạo. Không đánh dấu ca bị chặn môi trường là Đạt.
- [ ] Cập nhật ledger/artifact cho các ca thực sự có bằng chứng; commit riêng: contract/prompt+validator, UI/progress, docs/results. Không push.

## 8. Tiêu chí nghiệm thu và giới hạn

- Một yêu cầu SWR/SWD có cấu hình đủ trả về đề có title, business overview đủ để đọc độc lập, hướng dẫn, câu có sản phẩm nộp và rubric; apply/reload không mất rubric hay trở thành 0 điểm.
- Không áp dụng đề Graded thiếu rubric hoặc sai tổng điểm; issue chỉ đúng câu/tiêu chí lỗi.
- Các câu dùng cùng bối cảnh/kiến trúc; giảng viên thấy và xử lý các cảnh báo ngữ nghĩa trước khi dùng đề.
- Spinner xuất hiện từ lúc submit, không bấm lặp được; thời gian đã chờ hoạt động; mọi ước lượng phải có cơ sở đo.
- Người dùng hiểu hướng dẫn chung sẽ được thêm/thay như thế nào; hủy/thất bại/version conflict không làm mất bài cũ.
- Schema v2 cần kiểm khả năng structured output của provider, tránh schema lồng quá lớn; không tăng max output/retry để che lỗi.
- Security đang bật trong dự án: áp dụng validation, authorization, an toàn nội dung và không log secrets; giữ auth/CSRF/headers. Resiliency: giữ timeout/hủy/idempotency, cho phép soạn tay khi AI lỗi. Deployment/health changes N/A ở lượt chỉ lập kế hoạch; property-based testing đang tắt, không bật mới.

## 9. Ngoài MVP

Ước lượng thời gian dựa trên lịch sử nếu chưa có telemetry; AI reviewer thứ hai; tự sửa bằng model; import nguyên đề ảnh/OCR; sửa preview tùy ý; append/undo toàn bộ; thư viện đề mẫu quản trị được. Các phần này cần đánh giá chi phí và hợp đồng riêng, không gộp ngầm vào cải thiện prompt.

## 10. Mở rộng UI soạn bài theo yêu cầu mới

**Bằng chứng mới**: ảnh tham khảo có ba tab Soạn thủ công / Import từ file / Soạn bằng AI, ô bối cảnh, upload nguồn, số yêu cầu, tổng điểm và loại diagram. Dùng làm tham khảo về chức năng; bố cục mới theo design system hiện tại, không sao chép nguyên form dài trong ảnh. Các giới hạn 3 file/2 MB là đề xuất khởi đầu từ ảnh, chưa phải cấu hình hiện có.

**Code đối chiếu thêm**: `AssignmentFormPage.tsx` đang dùng editor dạng trang tài liệu, rubric bên lề và autosave 1.500 ms có hàng đợi version; đã có DOCX import qua `skeleton:import-docx`. `RubricEditor.tsx` cho nhập điểm từng mục chấm nhưng ô nhỏ, thêm bằng dấu +; giữ giới hạn backend 20 criteria/phần, 20 items/criterion, điểm item tối đa 1.000 và tối đa 2 số thập phân. U03 hiện chỉ có purpose MATERIAL và DOCUMENT_IMAGE; MATERIAL nhận PDF/DOCX/PPTX, chưa có TXT hoặc purpose nguồn riêng tư. Vì vậy không thể chỉ thêm một input file ở frontend là hoàn thành chức năng.

### 10.1 Bố cục và đường đi chung

Màn hình vẫn là một bài tập DRAFT duy nhất. Ba cách khởi tạo đều đưa về cùng editor và cùng bộ kiểm tra nội dung/điểm:

| Khu vực | Nội dung và thao tác |
|---|---|
| Thanh đầu trang | Tiêu đề sửa trực tiếp; trạng thái Nháp; Đang lưu/Đã lưu/Lưu lỗi; Xem trước bài của sinh viên; Xếp lịch |
| Cách bắt đầu | Soạn thủ công / Nhập đề từ file / Tạo đề bằng AI; chuyển tab không xóa nội dung hay lựa chọn đang nhập |
| Thông tin đề | Yêu cầu tự do (AI tự suy ra dạng); Tiếng Việt/English; thời lượng tùy chọn; số yêu cầu chính; tổng điểm mục tiêu |
| Nội dung chính | Bối cảnh nghiệp vụ; Hướng dẫn làm và nộp bài; danh sách yêu cầu có tiêu đề, mô tả, loại câu/sơ đồ |
| Rubric | Bên phải câu đang chọn trên desktop; mở dưới câu hoặc drawer trên màn hình nhỏ; từng tiêu chí/mục chấm có mô tả và điểm rõ |
| Tóm tắt cố định | Số yêu cầu hiện có/mục tiêu, số phần chấm, số phần đủ rubric, tổng điểm thực tế/mục tiêu, danh sách chỗ cần hoàn thiện |

Đề xuất luồng AI là panel rộng trong trang hoặc modal lớn có ba bước “Thiết lập → Đang tạo → Xem và kiểm tra”, thay modal nhỏ chứa mọi thứ. Editor tài liệu hiện có được giữ lại; tách các vùng trách nhiệm để không tiếp tục phình `AssignmentFormPage.tsx`.

Ngôn ngữ đề là lựa chọn riêng với ngôn ngữ UI. Chọn English sinh title, context, instructions, yêu cầu và rubric bằng tiếng Anh; nhãn thao tác vẫn theo ngôn ngữ UI. Đổi lựa chọn khi bài đã có nội dung không tự dịch hoặc ghi đè nội dung đã lưu. UI cho chọn ngôn ngữ ngay trong lần tạo lại và xem proposal trước khi áp dụng. Thuật ngữ UML và tên miền nghiệp vụ có thể giữ nguyên.

### 10.2 Phân biệt yêu cầu, phần chấm và tiêu chí

- Nhãn chính: “Số yêu cầu/câu hỏi”, nghĩa là số câu được đánh số ở cấp cao nhất; không phải số dòng rubric hoặc số business requirements được kể trong bối cảnh.
- Câu có thể có mục con. Ví dụ Q1 có Q1.1/Q1.2 thì cả hai là leaf parts được chấm theo mô hình hiện tại; tổng Q1 bằng tổng các leaf, không cộng lại điểm parent.
- Với AI, số yêu cầu là mục tiêu có cấu trúc và validator kiểm đúng số câu cấp cao nhất. Khi soạn tay, hiện “Đang có 3/4 yêu cầu”; người dùng có thể sửa mục tiêu, không bị hệ thống tự thêm/xóa câu để khớp số.
- Phân biệt số tiêu chí và số mục đạt điểm. Mỗi mục có ô “Nội dung cần đạt” và “Điểm”; giữ nhóm tiêu chí/kind TEXT hoặc DIAGRAM từ backend thay vì làm mất metadata khi sửa qua UI đơn giản.
- Giới hạn số câu và nesting phải kiểm theo trần 100 sections/level 1–6 hiện có, số leaf và chi phí output. UI đề xuất 1–10 câu cho lượt AI đầu tiên; backend vẫn có giới hạn và thông báo nếu cấu hình quá lớn, không tăng output token ngầm.

### 10.3 Thiết lập điểm và chỉnh rubric

Hiện rõ ba tầng: **Tổng điểm mục tiêu → Điểm từng yêu cầu → Điểm từng mục chấm**. Điểm thực tế tính từ các mục rubric; không có ba ô tổng độc lập có thể lệch nhau mà không cảnh báo.

Ví dụ mục tiêu 10 điểm, ba yêu cầu 3/4/3; Q1 có bốn mục 1/0.75/0.75/0.5. Tóm tắt cho biết “Q1: 3/3; Toàn bài: 10/10”. Mỗi ô điểm có nhãn hiển thị, vùng bấm đủ rộng, tối đa hai số thập phân; cho nhập 0.1/0.25/0.5 theo nhu cầu, không ép bội 0.25 chỉ vì step hiện tại.

- Cho chọn “AI đề xuất phân bổ” hoặc “Tự nhập điểm từng yêu cầu” trước khi sinh. Trường hợp tự nhập phải cộng đúng tổng mục tiêu trước khi gửi; AI chia tiếp điểm xuống các mục chấm và backend kiểm lại.
- Sau khi sinh hoặc nhập tay, sửa mô tả/điểm ở rubric của câu; hiện “Còn thiếu 0.5 điểm” hoặc “Vượt 1 điểm” sát vùng liên quan. Không âm thầm scale hoặc làm tròn để ép tổng.
- Điểm trống là “Chưa nhập điểm”, khác 0 điểm. Cho lưu DRAFT chưa hoàn thiện; chặn Áp dụng AI Graded/Xếp lịch khi thiếu rubric hoặc tổng lệch mục tiêu. Practice vẫn kiểm kiểu/số hợp lệ nhưng không tự ép có điểm nếu chế độ hiện tại không yêu cầu.
- “Chia đều” là thao tác chủ động có preview, chỉ ảnh hưởng nhóm được chọn; giữ tổng chính xác bằng đơn vị 0.01, phần dư được phân bổ xác định theo thứ tự. Không tự ghi đè điểm người dùng đã chỉnh.
- Đổi/xóa/di chuyển heading có thể đổi leaf parts; báo ảnh hưởng rubric trước thao tác làm mất phần chấm. Không bỏ rubric có điểm mà không báo. Giữ ID ổn định nếu chỉ đổi tên/cấp trong trường hợp domain cho phép.
- Thẻ cạnh câu hiển thị tên, điểm, số mục chấm và nút có chữ “Thêm tiêu chí”; không chỉ dùng dấu +. Mục chưa có điểm hiện trạng thái cần bổ sung, không để một badge 0 điểm gây hiểu nhầm đã chấm.

## 11. File đầu vào: import đề và nguồn tham khảo cho AI

### 11.1 Hai hành vi riêng biệt

**Nhập đề từ file**: đọc cấu trúc đề để chuyển vào title/context/instructions/câu/rubric trong editor. MVP giữ DOCX importer hiện có; mở preview, đánh dấu các heading/điểm chưa nhận diện, giảng viên xác nhận mới thay nội dung. Điểm nhận từ văn bản phải kiểm và đối chiếu, không xem chuỗi “3 điểm” là rubric hoàn chỉnh. PDF/TXT được hỗ trợ làm nguồn AI ở MVP; không quảng cáo là import giữ định dạng. PDF-to-structured-editor và OCR để sau.

**Tài liệu đầu vào cho AI**: upload tài liệu mô tả nghiệp vụ, đề mẫu hoặc rubric mẫu; chỉ phần chữ đã trích và được chọn mới đưa vào request. Upload không trực tiếp thay nội dung đề, không publish tài liệu và không tự gửi AI. Có thể kết hợp bối cảnh gõ tay + file upload + học liệu có sẵn trong phạm vi được phép.

### 11.2 UX upload

- Kéo thả hoặc nút “Chọn tài liệu”; đề xuất hỗ trợ `.txt` UTF-8, `.docx`, PDF có lớp chữ; tối đa 3 file riêng, 2 MiB/file và 6 MiB tổng file riêng. Hiển thị giới hạn lấy từ backend, không hardcode khác nhau giữa UI/API. Đây là giới hạn dự kiến cần chốt khi review thiết kế.
- Mỗi file có tên, loại, dung lượng, tiến độ upload thật, Hủy/Xóa và trạng thái trích chữ: Chờ → Đang đọc → Sẵn sàng / Không có chữ / Không đọc được. Phần upload dùng phần trăm bytes, phần AI không dùng phần trăm giả.
- Cho xem trước đoạn chữ sẽ gửi, số ký tự và nhãn “đã rút gọn”; báo riêng tài liệu bị bỏ hoặc cắt. Tổng ngân sách chữ vẫn theo 30.000 ký tự hiện có cho toàn bộ nguồn đã chọn, không 30.000 cho từng file.
- File scan/ảnh không có OCR trong MVP: thông báo rõ và cho thay bằng PDF có chữ hoặc dán nội dung. File hỏng, mã hóa bằng mật khẩu, sai loại thực, TXT lỗi encoding hoặc vượt giới hạn có lỗi ngay trên hàng file.
- Nút tạo bị khóa khi còn nguồn đã chọn đang upload/trích chữ. Khi một file lỗi, cho sửa, bỏ chọn hoặc gỡ file rồi tiếp tục; không tự loại file khỏi prompt mà giảng viên không biết.
- Hiện nguồn chỉ giảng viên có quyền quản lý bài được xem; không gộp với “Tài liệu tham khảo cho sinh viên”. Nếu muốn chia sẻ cho sinh viên, phải dùng luồng học liệu riêng được phép.

### 11.3 Backend và vòng đời nguồn

- Tạo purpose có phạm vi riêng `ASSIGNMENT_SOURCE` trong U03 hoặc khả năng tương đương qua contract đã thống nhất; không dùng MATERIAL rồi để file lọt vào danh sách học liệu công khai. Mở rộng allowlist TXT và nhận dạng nội dung theo purpose, không mở toàn cục cho mọi loại upload.
- Metadata nguồn gắn assignmentId, uploader, stored file ID, trạng thái extraction, char count, hash/version và thời gian tạo; truy vấn bằng ID luôn kiểm quyền quản lý bài/scope. Liên kết nguồn/task dùng ID/version ổn định, không chỉ dùng tên file.
- Dùng cơ chế fileRef/attach hiện có cho file pending; cần schema/migration nguồn riêng nếu chưa có nơi lưu liên kết. Không nhét raw text lớn hay binary vào provenance JSON. Trích chữ qua hợp đồng có timeout/giới hạn tài nguyên; tái sử dụng phần parser của U13 thông qua contract phù hợp, không gọi trực tiếp service nội bộ xuyên unit.
- Validate loại thật, dung lượng, chữ trích và giới hạn nén DOCX. Render preview bằng text an toàn. Tài liệu chỉ là dữ liệu tham khảo, không được thay system prompt hoặc chỉ thị công cụ.
- Khi đã gắn vào bài: giữ file nguồn riêng tư để giảng viên đối chiếu. Gỡ khỏi bài thì không gửi cho các task mới; xóa bytes khi không còn tham chiếu theo cơ chế cleanup hiện có. Task đang chạy dùng snapshot nguồn đã chọn để không thay đổi ngầm giữa chừng. Pending upload hết hạn theo U03; quyền đọc nguồn không được kế thừa cho sinh viên khi phát hành bài.
- API mới phải gồm policy/đính kèm/danh sách/trạng thái/gỡ; dùng auth/CSRF, lỗi an toàn và kiểm assignment version cho thao tác thay cấu hình bài. Không thay hạn mức file MATERIAL hiện hữu.

## 12. Mô hình dữ liệu và việc lưu trong editor

Mở rộng metadata authoring tùy chọn: output language, chế độ suy luận `AUTO` (vẫn đọc profile cũ để tương thích), expectedRequirementCount, targetTotalPoints, cấu hình điểm theo mã câu, source attachments và aiPromptVersion. Tách mục tiêu với giá trị tính ra từ skeleton/rubric. Bài cũ không có mục tiêu vẫn mở và sửa được, hiển thị tổng thực tế; không tự gán mục tiêu 10 cho toàn bộ dữ liệu cũ.

Giữ autosave hiện có. Phần cấu hình và file phải phản ánh rõ trạng thái Đã lưu/Lưu lỗi. Trước import/apply AI hoặc xếp lịch, flush hàng đợi lưu và dùng version mới nhất; apply kết quả cũ không được đè nội dung vừa sửa ở editor. Generation chỉ lưu proposal riêng; bài hiện tại chỉ đổi sau xác nhận Áp dụng. Không diễn đạt “không lưu gì” vì bản nháp editor và proposal vẫn có thể được lưu theo cơ chế này.

Sau áp dụng: refresh đồng bộ title/context/instructions/parts/rubrics/sources/target, hủy cache rubric cũ, không để UI tạm hiển thị tổng 0 khi rubric đang tải. Spinner “Đang tải tiêu chí chấm” thay cho số 0 giả. Khôi phục sau reload phải giữ các giá trị đã lưu; lỗi autosave giữ nội dung local và có thao tác thử lưu lại.

## 13. Các gói triển khai bổ sung

Mở rộng P1–P4, thực hiện theo phụ thuộc; các tên file mới là đề xuất thiết kế, chưa được tạo.

| Gói | Files/ranh giới chính | Kết quả độc lập có thể kiểm |
|---|---|---|
| U1 — Cấu hình đề | U07 domain/commands/views/controller/repository/migration; frontend assignments `types.ts`, `api.ts`, `AssignmentSettingsPanel.tsx` | Lưu/reload ngôn ngữ, số câu và tổng điểm mục tiêu; tương thích bài cũ |
| U2 — Nguồn upload riêng | U03 `ArtifactPurpose`, `PurposePolicy`, `ContentInspector`, upload/attach/access contracts; U07 source service/controller/repository; parser contract U13; frontend `files` và `AssignmentSourceFiles.tsx` mới | File TXT/DOCX/PDF có chữ upload–extract–select được, có tiến độ và quyền riêng tư |
| U3 — Màn hình khởi tạo | `CreateAssignmentDialog.tsx`, `AssignmentFormPage.tsx`, `AiDraftDialog.tsx`, `AssignmentAuthoringSetup.tsx` mới; dùng `DocxImportDialog.tsx` | Ba cách bắt đầu dùng chung một draft; chuyển tab không mất nội dung; form AI có nguồn, ngôn ngữ, số câu và điểm |
| U4 — Nội dung và rubric | `AssignmentFormPage.tsx`, `RubricEditor.tsx`, shared document helpers; `AssignmentScoreSummary.tsx` và `AssignmentRubricPanel.tsx` mới | Từng câu/tiêu chí sửa rõ ràng, điểm cập nhật chính xác; context không thành phần chấm |
| U5 — Sinh và preview | P2/P3 của kế hoạch gốc, schema/prompt/validator + request sources/options | Kết quả bám ngôn ngữ, số câu, điểm; có spinner/status/elapsed; preview và apply an toàn |
| U6 — Tích hợp | Test backend nguồn/assignments/files/contracts và frontend editor/dialog/upload; test app thật và ledger | Import/manual/AI cùng đạt điều kiện xuất đề, đọc rubric U09, reload và xếp lịch đúng |

### Regression và acceptance bắt buộc

- [ ] English và Tiếng Việt: tạo đề ở cả hai ngôn ngữ, gồm context và rubric; đổi lựa chọn không tự dịch đề cũ; tài liệu nguồn khác ngôn ngữ đầu ra vẫn xử lý rõ.
- [ ] Count: 3 câu cấp cao có 5 leaf parts; validator phân biệt hai số; đổi heading không đếm business requirements trong paragraph thành câu; số 0/âm/quá giới hạn bị từ chối.
- [ ] Điểm: target 10, Q1/Q2/Q3 = 3/4/3, item thập phân; sửa/xóa/di chuyển item; tổng thiếu/vượt; mục chưa có điểm; đổi target; auto chia đều có remainder 0.01; không double count parent; sau save/reload không đổi tổng.
- [ ] Rubric AI có nhóm nhiều item và DIAGRAM giữ đủ metadata khi sửa tay; không mất tiêu chí khi đổi câu đang chọn hoặc rubric đang tải.
- [ ] Upload: 3 file hợp lệ, file thứ 4, đúng/sai biên 2 MiB và tổng 6 MiB; đuôi giả, scan không chữ, DOCX lỗi/nén lớn, PDF mật khẩu, TXT encoding lỗi; cancel/retry/drop/delete và polling lỗi.
- [ ] Privacy: sinh viên không list/download nguồn tác giả kể cả bài đã publish; giảng viên lớp khác không dùng sourceId của bài khác; không gửi nguồn chưa chọn hoặc file đã gỡ vào request mới.
- [ ] Đồng thời: upload hoàn tất khi modal đóng, generation hoàn tất sau khi người dùng sửa đề, apply trong lúc autosave, hai tab version conflict, retry cùng requestRef không tạo task/provider charge trùng.
- [ ] Ba cách bắt đầu: import DOCX có preview; PDF/TXT xuất hiện đúng vai trò nguồn AI; nhập tay không bắt gọi AI; AI lỗi vẫn tiếp tục soạn tay; tab đổi không xóa dữ liệu.
- [ ] UI desktop/màn hẹp/keyboard: nhãn điểm đọc được, thêm/xóa tiêu chí dễ thấy, focus lỗi tới đúng ô, trạng thái lưu/poll không spam screen reader, preview người học không lộ rubric nội bộ hoặc file nguồn.
- [ ] Live flow QA: chọn ngôn ngữ + upload nguồn + đặt 3 câu/10 điểm + tạo AI → preview kiểm đủ dữ kiện → áp dụng → sửa một tiêu chí → reload → đối chiếu điểm; ghi bằng chứng thực tế, không đánh dấu môi trường bị chặn là Đạt.

## 14. Điều kiện hoàn thành phạm vi UI mở rộng

Giảng viên tạo được một đề hoàn chỉnh qua nhập tay, DOCX hoặc AI; biết rõ nguồn AI đang đọc gì và đề sẽ ở ngôn ngữ nào; nhập được số yêu cầu, tổng điểm và điểm từng mục chấm; nhìn thấy phần còn thiếu trước khi xếp lịch. File nguồn riêng tư không lẫn với học liệu sinh viên. Kết quả AI không được coi là hoàn chỉnh chỉ vì đã sinh xong JSON.

Các lựa chọn về giới hạn file, số câu mặc định, panel/modal và khả năng chỉnh preview được ghi ở đây là đề xuất để review; chưa có code, migration, provider call hoặc thay đổi dữ liệu nào được thực hiện trong lượt mở rộng kế hoạch này.
