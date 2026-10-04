# Unit of Work Dependencies - 16 unit logic

## 1. Ký hiệu và nguyên tắc

Hàng là consumer, cột là provider. `H` cần behavior/contract ổn định trước integration gate; `C` phát triển song song qua contract có version và fake adapter; `E` tiêu thụ event/read model; `-` không phụ thuộc trực tiếp. Không ký hiệu nào cho phép đọc bảng hoặc repository của unit khác. Các unit vẫn thuộc một backend modular monolith; worker là process riêng.

## 2. Dependency matrix

| Consumer \ Provider | U01 | U02 | U03 | U04 | U05 | U06 | U07 | U08 | U09 | U10 | U11 | U12 | U13 | U14 | U15 | U16 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| U01 | - | H | H | C | - | - | - | - | - | - | - | - | - | - | - | - |
| U02 | C | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| U03 | C | C | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| U04 | H | H | H | - | C | - | - | - | - | - | - | - | - | - | - | - |
| U05 | H | H | H | H | - | - | - | - | - | - | - | - | C | - | - | - |
| U06 | H | H | H | H | C | - | - | C | C | - | - | - | C | - | - | - |
| U07 | H | H | H | - | - | - | - | - | - | - | - | - | C | - | - | - |
| U08 | H | H | H | H | H | H | - | - | C | - | C | C | C | C | - | - |
| U09 | H | H | H | H | - | H | - | H | - | - | - | - | C | - | - | - |
| U10 | H | H | - | H | - | H | - | H | H | - | - | - | C | - | - | - |
| U11 | H | H | H | H | - | H | - | H | H | - | - | - | C | - | C | - |
| U12 | H | H | H | H | - | - | - | - | - | - | - | - | - | C | - | - |
| U13 | H | H | H | H | H | H | H | - | C | - | C | - | - | C | C | - |
| U14 | H | H | H | H | - | - | - | H | H | - | - | H | - | - | C | - |
| U15 | H | H | H | H | - | H | - | H | H | - | H | - | C | H | - | - |
| U16 | H | H | H | H | E | - | E | H | - | - | H | H | - | H | H | - |

### Dependency graph theo wave

![Đồ thị phụ thuộc 16 unit](unit-of-work-dependency.png)

Nguồn hình: `unit-of-work-dependency.drawio` (mở bằng draw.io để sửa). Hình vẽ các cạnh `H` tối giản theo bắc cầu; nhãn WAVE đặt trên cột đầu của mỗi wave, không mũi tên nào đi từ phải sang trái.

**Text alternative** (cạnh provider → consumer): U02 → U01; U03 → U01; U01 → U04, U07; U04 → U05, U06, U12; U05 → U08, U13; U06 → U08, U13; U07 → U13; U08 → U09; U09 → U10, U11, U14; U11 → U15; U12 → U14; U14 → U15; U15 → U16 (đọc sổ điểm).


U01 phụ thuộc `H` vào U02 (audit) và U03 (việc nền gửi OTP, ảnh đại diện): hai unit này code trước, song song với nhau; U01 phụ thuộc U04 (phạm vi môn/lớp) bằng cạnh `C`. U02 và U03 phụ thuộc U01 (kiểm quyền) và U03 phụ thuộc U02 (audit) bằng cạnh `C`, chạy với adapter giả cho tới khi U01, U02 xong (quyết định 2026-10-04: việc nền chuyển từ U02 sang U03, U03 code đầu tiên). U04 phụ thuộc U05 bằng cạnh `C`: phần Learning Access của U04 dùng port trung lập ở tầng contract, U05 cung cấp implementation, nên không tạo chu trình cứng với cạnh `H` theo chiều ngược lại. U05 phụ thuộc `AiUsagePort` của U13 bằng cạnh `C` để kiểm trần và tính credit embedding (U13 mới gọi `CreditPort` của U07); U05 chạy với adapter giả cho tới khi U13 xong, nên vẫn ở Wave 2. Mọi unit nghiệp vụ đều phụ thuộc `H` vào U01 (kiểm quyền), U02 (audit) và U03 nếu có việc nền, sự kiện hoặc tệp; các cạnh này không vẽ lại vì đã được phủ bắc cầu qua U01. Hình chỉ vẽ cạnh `H` tối giản theo bắc cầu (nếu A → B → C thì không lặp A → C); các cạnh `C` (U01 → U02, U01/U02 → U03, U04 → U01, U05 → U04, U07 → U05, U13 → U05, U05/U09/U13 → U06, U09/U11/U13/U14 → U08, U12 → U08 (`GroupReadinessPort`), U08 → U06 (`RubricOwnerPort`: U06 khai báo, U08 cài; chuyển bài `DRAFT` sang phiên bản rubric mới), U13 → U07 (`CreditUsagePort`: U07 khai báo, U13 cài; lịch sử dùng credit), U13 → U10, U13/U15 → U11, U14 → U12, U09/U11/U14/U15 → U13, U13 → U09 (`CodeLabCheckPort`: kiểm lời giải mẫu Code Lab), U13 → U15 (`AiGradingPort`, `CodeRunPort`: U15 chấm tay và chạy với adapter giả, U13 cắm vào sau), U15 → U14) không vẽ, xem ma trận. U16 đọc sổ điểm của U15 qua port nên U15 → U16 là `H`; U16 còn phụ thuộc `H` vào U11/U14 (đọc tiến độ nộp bài) nhưng đã phủ bắc cầu qua U15. Các cạnh `E` U05/U07 → U16 (event lớp, event thanh toán) không vẽ. Ma trận phía trên vẫn là danh sách đầy đủ. Chiều mũi tên luôn từ provider sang consumer. Khung wave là nhóm và điểm dừng tích hợp, **không phải hàng rào đồng bộ**: node ở wave sau có thể mở khi provider trực tiếp của nó xong, dù node khác của wave trước vẫn chạy. Không mũi tên nào đi từ phải sang trái.

**Diễn giải bằng chữ:** Wave 1 có U03 và U02 khởi động song song: U03 dựng khung dự án, việc nền, worker và tệp; U02 làm audit. U01 mở khi cả hai xong (gửi OTP qua việc nền thật, ghi audit thật) rồi cắm kiểm quyền vào U02, U03; U04 sau U01. Wave 2 có U05/U06 song song sau U03/U04, U07 mở ngay sau U01/U02 và U12 mở ngay sau U04 vì nhóm thuộc lớp; U08 theo U05/U06. Phần Learning Access của U04 hoàn tất tại wave này khi implementation của U05 cắm vào port trung lập. Wave 3 có U09 theo U08 và soạn cả năm loại bài (Text Essay, Quiz, Diagram Essay, Code Lab, Group); U11 theo U09, U14 theo U09/U12, U15 theo U11/U14. Wave 4 có U10 theo U09, U13 chạy khi nguồn U05/U06 và credit U07 sẵn sàng rồi cắm chạy code và AI vào port của U08, U09, U11, U15 (cạnh `C`, không vẽ), và U16 sau U15. Các nhánh vượt ranh giới wave ngay khi dependency trực tiếp đạt; số unit đang triển khai đồng thời trên toàn nhóm không quá năm.

## 3. Public contract theo luồng

| Luồng | Provider → Consumer | Contract và giới hạn |
|---|---|---|
| Identity → audit query, upload | U01 → U02, U03 (`C`) | U02 ghi audit, U03 điều phối việc nền độc lập bằng actor/scope reference; `queryAudit` và upload tệp gọi authorization contract có version, fail closed khi chưa tích hợp |
| Identity, audit, file, job | U01/U02/U03 → mọi unit cần dùng (U03 → U01 cho việc nền gửi OTP) | Actor/resource authorization, append-only audit (U02), scoped artifact và idempotent job/event (U03); không dùng shared repository |
| Môn/lớp/ghi danh | U04 → U05, U06, U08-U16 (U01 đọc phạm vi qua `C`) | Subject/class/enrollment reference và scoped authorization |
| Learning access within U04 | U04 enrollment + U05 học liệu đang hiển thị → Learning Access capability in U04 | Đây là orchestration nội bộ của U04, không phải self-dependency giữa unit; chỉ trả học liệu đang hiển thị khi enrollment hợp lệ; không lưu lesson progress |
| Ngân hàng → đề/attempt/chấm | U06 → U08/U09/U10/U11/U15 | QuestionVersion/RubricVersion immutable và snapshot đúng version |
| Tạo đề | U05/U06 → U13 → U08; U09/U10 cấu hình | U13 trả AI draft proposal; U08 review, sửa, lưu và publish. RAG chỉ hỗ trợ nguồn khi được chọn |
| Đề → attempt | U08/U09 → U11 | Lịch bài, dạng/chế độ bài và assignment/question/rubric snapshot; bài đã phát hành khóa nội dung |
| Nhóm → tài liệu nhóm | U12 + U09 → U14 | Thành viên/trưởng nhóm, mô hình tài liệu; nhận/khóa mục, ghép realtime, bản nộp bất biến có tác giả từng mục |
| Chấm | U11/U14/U06 → U15; U13 hỗ trợ | AI chỉ trả proposal; U15 lưu manual/final grade; tài liệu nhóm chấm như bài `DOCUMENT` (AI chỉ đề xuất), điểm đóng góp thành viên mặc định bằng điểm tài liệu chung; chấm hàng loạt bằng AI rồi xác nhận từng bài |
| Báo cáo/thông báo | U05 event lớp, owner events và điểm U15 → U16 | Thông báo trong app (SSE), email có trần 300/ngày, nhắc hạn, thống kê quản trị, phân bố điểm ẩn danh và xuất bảng điểm; lỗi gửi không rollback transaction nguồn |

### Ranh giới tránh vòng phụ thuộc

- U13 định nghĩa provider-neutral `AiDraftProposal` và `CodeRunResult`; U08/U11/U15 chuyển yêu cầu đã kiểm quyền sang contract đó. U13 không đọc bảng Assessment/Submission/Grading và không publish/chốt thay owner.
- U02 (audit) và U03 (khung, việc nền, tệp) không chờ U01: chỉ các API có actor (`queryAudit`, upload tệp) tích hợp `AuthorizationService` của U01 qua contract `C`; adapter thật là bắt buộc trước khi phát hành API và mặc định từ chối nếu không kiểm quyền được.
- U08 có thể hoàn thành luồng tạo đề thủ công trước khi U13 tích hợp AI. `C` ở U08 → U13 là contract tích hợp, không tạo hard cycle.
- U04 chỉ sở hữu kiểm quyền và truy cập học liệu; U04 backend không phụ thuộc ngược U08/U16. Thống kê quản trị của U16 đọc số đếm qua port của U01 và U04 (cạnh `H` sẵn có).
- U07 chỉ cộng credit AI sau thanh toán; không ảnh hưởng quyền vào lớp nên U04 không gọi U07. U07 không ghi enrollment vào U04.

## 4. Worker ownership

| Handler | Owner | Kết quả qua owner contract |
|---|---|---|
| Quét học liệu (tệp/YouTube) và RAG | U05 | Lesson kèm chữ trích được và embedding |
| AI assessment draft | U13 | Draft proposal (câu hỏi cho U08, U06, U10; khung cho U09) để owner duyệt/lưu |
| Code sandbox run | U13 | Run result cho U11, U15 (chấm Code Lab); kết quả kiểm lời giải mẫu lưu ở câu hỏi U06, U08/U09 đọc qua `CodeLabCheckPort` |
| Group document | U14 | Tài liệu nhóm, lịch sử mục theo tác giả, bản nộp bất biến |
| AI grade proposal và compact Draw.io | U13 phối hợp U15 | Proposal (XML rút gọn tạo trong bộ nhớ bởi U09); U15 quyết định điểm |
| Automatic payment reconciliation | U07 | Giao dịch PayOS và số dư credit AI trên `accounts` |
| Notification & reporting | U16 | Thông báo (kèm trạng thái email), nhắc hạn, thống kê quản trị đọc theo yêu cầu và xuất bảng điểm trực tiếp |

Nền tảng việc nền U03 giữ gửi sau commit, thử lại và gửi lại việc bị mất (không có dead-letter; hết lượt thì `onFailed` của owner chuyển dòng nghiệp vụ sang lỗi), còn owner nghiệp vụ kiểm tra idempotency và lưu kết quả. Worker payload chỉ chứa ID/reference và scope; worker tải nguồn qua contract có quyền.

## 5. Bốn wave với lịch mở việc liên tục

| Wave | Unit | Số unit | Nhánh và điều kiện mở |
|---|---|---:|---|
| 1 | U03, U02, U01, U04 | 4 | U03 (khung, việc nền, tệp) và U02 (audit) song song trước, dùng adapter giả cho kiểm quyền; U01 sau U02/U03; U04 sau U01; phần Learning Access của U04 chỉ khai báo port, hoàn tất ở wave 2 |
| 2 | U05, U06, U07, U12, U08 | 5 | U05/U06 sau U04; U07 sau U01; U12 sau U04; U08 sau U05/U06; U05 cắm implementation vào port Learning Access của U04 |
| 3 | U09, U11, U14, U15 | 4 | U09 sau U08 (soạn cả năm loại bài, UC 23–27); U11 sau U09; U14 sau U09/U12; U15 sau U11/U14; chạy code và AI của U13 dùng qua `C` với adapter giả |
| 4 | U10, U13, U16 | 3 | U10 sau U09; U13 sau U05/U06/U07 rồi cắm implementation vào port của U08, U09, U11, U15; U16 sau U15 (đọc sổ điểm) |

Wave biểu thị nhóm và checkpoint kết quả, không buộc toàn bộ unit của wave trước đóng mới cho mở unit tiếp theo. Scheduler mở unit khi các provider `H` của riêng unit đã sẵn sàng và còn slot; giới hạn tối đa năm unit đang triển khai cùng lúc tính trên toàn bộ wave. U08 có thể làm phần thủ công trước U13; AI tích hợp sau qua contract `C`. U16 có thể chuẩn bị schema/projection từ đầu, nhưng chỉ hoàn tất khi event từ các owner, gồm U15, đã có.

| Gate | Producer cần ổn định | Kiểm tra theo nhánh |
|---|---|---|
| G1 | U03, U02, U01, U04 | Việc nền/worker, audit, authorization, artifact/checksum, class/enrollment scope |
| G2 | U05-U08, U12 và phần Learning Access của U04 | Nhóm của lớp (UC 9 phần nhóm, UC 15), content/bank versions, verified payment event, Learning access và đề thủ công/phát hành |
| G3 | U09, U11, U14, U15 | Năm loại bài, attempt/submission, tài liệu nhóm realtime/nộp, chấm tay/final grade |
| G4 | U10, U13, U16 | Template/copy, AI/Code sandbox và chấm Practice theo credit, reporting/notification đúng scope |

Gate tổng kiểm tra toàn bộ phạm vi của wave; nhánh ở wave sau được mở ngay khi provider trực tiếp đạt kiểm tra tương ứng, không phải chờ gate tổng.

## 6. Dependency paths và critical path


- **Truy cập học liệu:** U02/U03 → U01 → U04 → U05 → U04. U04 kiểm enrollment và học liệu đang hiển thị; không có learning path hay lesson progress.
- **Bài cá nhân:** U02/U03 → U01 → U04 → U06 → U08 → U09 → U11 → U15 → U16. U03 cung cấp artifact và việc nền; U10 thêm template/copy vào U08 nhưng không chặn U11; U13 cung cấp AI draft/Code run khi được chọn.
- **Bài nhóm:** U04 → U12 (wave 2); U09 soạn bài nhóm (UC 27); U08 khai báo `GroupReadinessPort`, U12 cài (`C`); U09 + U12 → U14 → U15 → U16.
- **AI tạo đề:** U05/U06 → U13 → U08 (contract `C`); U08 có thể soạn thủ công trước khi AI hoàn tất. **AI hỗ trợ chấm:** U11/U14 → U15 gọi U13, rồi giảng viên chốt.
- **Một đường phụ thuộc dài nhất theo các cạnh `H`:** U02 hoặc U03 → U01 → U04 → U05 (hoặc U06) → U08 → U09 → U11 (hoặc U14) → U15 → U16 (9 unit). U16 đọc sổ điểm của U15 qua cạnh `H` và nhận event của U05/U07/U15; thêm người không làm các cạnh bắt buộc biến mất.

| Rủi ro | Kiểm soát |
|---|---|
| 16 unit bị hiểu thành 16 deployable/service | Một backend modular monolith; unit chỉ là boundary phát triển và kiểm thử |
| Contract AI tạo vòng phụ thuộc | U13 trả proposal chung; owner U08/U15 lưu kết quả, không cho U13 ghi ngược |
| Phần Learning Access của U04 bị phình thành learning path/progress | Chỉ kiểm quyền, trả lớp và học liệu đang hiển thị; không có state hoàn thành bài học |
| Cross-unit data leak | Actor/resource scope ở U01, gọi public contract, negative authorization tests |
| Job/provider lỗi hoặc giao trùng | U03 retry/idempotency; owner xử lý kết quả và safe failure |
