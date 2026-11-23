# U07 — Kiểm thử tạo đề AI theo mẫu PE

## Phạm vi và cách kiểm

Sáu đề tự soạn đại diện cho các cấu trúc đã đối chiếu từ 14 đề SWR302 và 6 đề SWD392. Mỗi đề dùng cùng yêu cầu, mẫu, ngôn ngữ và cấu hình `gpt-6-luna` ở mức `medium` qua phiên Codex CLI đã đăng nhập. Mỗi phiên bản prompt sinh một phản hồi mỗi đề. Lượt cuối bổ sung hướng dẫn kiểm cộng điểm và phân biệt mâu thuẫn thật với lựa chọn thiết kế mở.

Backend dựng prompt và schema. Trong lượt đo, Codex nhận prompt và schema qua CLI, sau đó Java kiểm phản hồi bằng `JsonSchemaChecker` và `AssignmentDraftValidator`. Prompt chấm điểm U09 và luồng gọi Gemini của ứng dụng thuộc phạm vi khác. Kiểm tra UI dùng máy chủ HTTP giả cục bộ để chủ động tái tạo trạng thái `VALID` và `NEEDS_REVIEW`.

## Kết quả so sánh

| Đề | Prompt cũ | Prompt cuối | Nội dung đã kiểm tra |
|---|---|---|---|
| SWR FA25 | `VALID` · 10 điểm | `VALID` · 10 điểm | Bìa SRS, Context, Use Case, Conceptual ERD. Cả hai khớp `0.50 / 2.50 / 4.00 / 3.00`. Backend hiện kiểm thêm từng câu khi bật giữ điểm mẫu. |
| SWR SP25 | `INVALID` · 7 điểm | `VALID` · 10 điểm | Có rubric riêng cho actor chính, actor phụ, quan hệ include, nét đứt và hướng mũi tên. Hai NFR đều yêu cầu ngưỡng, điều kiện và cách đo. Rubric actor vẫn cần giảng viên kiểm tra theo từng use case vì một số vai trò được mô tả ở phạm vi toàn sơ đồ. |
| SWD REST | `VALID` · 10 điểm | `VALID` · 10 điểm | Có conceptual và logical ERD, lớp theo tầng, nhánh từ chối trong sequence. Phần pattern giữ đủ ba đầu ra `0.50 / 1.00 / 0.50`. |
| SWD Statechart | `VALID` · 10 điểm | `VALID` · 10 điểm | Chỉ rõ `MaintenanceTicket`, sự kiện và guard. Đề yêu cầu giải thích lựa chọn kiến trúc và trade-off. |
| SWD mẫu mâu thuẫn | `VALID` · 10 điểm | `NEEDS_REVIEW` · 10 điểm | Lượt cuối chỉ báo mâu thuẫn `Order`/`Dish` ở câu 1. Các lựa chọn REST và kiến trúc của câu 2–3 không phát sinh cảnh báo. |
| Đề tiếng Việt ngoài SWR/SWD | `VALID` · 10 điểm | `VALID` · 10 điểm | Có hai câu đúng yêu cầu và rubric bằng tiếng Việt, không phát sinh yêu cầu sơ đồ. |

Cả hai lượt đều có 6/6 phản hồi hợp JSON schema và 0 lỗi provider. Prompt cũ có một ca không đủ tổng điểm và không có cảnh báo máy đọc được ở đề mâu thuẫn. Prompt cuối có 5/6 ca `VALID` và 1/6 ca `NEEDS_REVIEW` đúng tình huống. Lượt thử giữa cho đề tiếng Việt 9/10 và hai cảnh báo dư ở đề mâu thuẫn. Sau khi sửa hai chỉ dẫn, cả hai vấn đề đều hết trong một lượt chạy lại.

## Kiểm thử hồi quy

- Maven chạy 159 test nhóm `edu.aiplatform.assignments.service`, gồm luồng yêu cầu AI, schema, điểm, callback, apply, sửa đề, nhập đề và MCP.
- `PlatformWorkflowIntegrationTest` chạy 35 test với các container kiểm thử, gồm Postgres và RabbitMQ.
- Vitest chạy 92 test của `tests/features/assignments`. Test mới tái hiện việc tự áp dụng khi mở proposal có cảnh báo, rồi kiểm rằng mở xem trước chưa gọi `:apply` và bấm **Áp dụng đề xuất** gọi đúng một lần.
- Backend từ chối đề sai điểm từng câu dù tổng vẫn bằng 10. Khi rubric đề mẫu thay đổi sau lúc gửi tác vụ, callback tiếp tục dùng dãy điểm đã lưu của lần đó. Test cũng xác nhận từ chối giữ điểm khi mẫu không khả dụng, rubric chưa đủ, tổng điểm yêu cầu mâu thuẫn hoặc đích là bài luyện tập.
- Chrome trên app local xác nhận ca `VALID` gọi `:apply` một lần và vẫn là DRAFT. Ca `NEEDS_REVIEW` mở proposal, hiện cảnh báo trên UI và giữ đề trống tới khi người dùng bấm **Áp dụng đề xuất**. Sau thao tác đó, nội dung xuất hiện và trạng thái vẫn là DRAFT. Ảnh `review-warning.png` và thứ tự các request trong `browser-events.json` nằm trong thư mục cục bộ.
- TypeScript và ESLint được kiểm trên các thay đổi frontend. Trước khi sửa, test backend phát hiện việc chấp nhận đề lệch điểm từng câu và bỏ cảnh báo. Test frontend phát hiện việc thiếu lựa chọn giữ điểm và tự áp dụng đề có cảnh báo.

Thư mục cục bộ `.tmp/u07-pe-generation/` giữ sáu brief, prompt gửi đi, phản hồi, kết quả Java và log. `baseline/`, `candidate/` và `final/` lưu từng lượt. Dữ liệu và log cục bộ được Git bỏ qua.

Chạy từ thư mục gốc với Maven đã cài hoặc `mvn.cmd` trong wrapper cache.

```powershell
mvn -f backend/pom.xml '-Dtest=edu.aiplatform.assignments.service.*Test,PlatformWorkflowIntegrationTest' test
npm --prefix frontend run test -- tests/features/assignments
npm --prefix frontend run typecheck
node .tmp/u07-pe-generation/browser.mjs
```

## Độ ổn định của prompt cuối

Lặp lại cùng 6 brief bằng GPT-6 Luna `medium` qua Codex CLI, mỗi brief có 3 phản hồi độc lập. Dùng lượt `final` làm lượt 1 và tạo thêm `repeat-02`, `repeat-03`. SHA-256 của system prompt, user prompt và schema khớp giữa cả ba lượt. Java 17 chạy kiểm schema, số câu, tổng rubric và điểm đã cấu hình theo câu.

| Đề | Tổng điểm rubric qua 3 lượt | Kết quả |
|---|---|---|
| SWR FA25 | 10 / 10 / 10 | 3/3 giữ `0.50 / 2.50 / 4.00 / 3.00`. |
| SWR SP25 | 10 / 10 / 10.50 | Lượt 3 bị chặn do sai tổng. Cả ba lượt giữ mục riêng cho actor chính, actor phụ, quan hệ include và nét đứt. |
| SWD REST | 10 / 10 / 10 | 3/3 giữ `5 / 3 / 2` và các điểm phần pattern `0.50 / 1.00 / 0.50`. |
| SWD Statechart | 10 / 11 / 9 | Lượt 2 và 3 bị chặn do sai tổng. |
| SWD mẫu mâu thuẫn | 10 / 10 / 10 | 2/3 báo mâu thuẫn `Order`/`Dish`. Lượt 3 tự hiểu theo vòng đời từng món và thiếu cảnh báo. |
| Đề tiếng Việt ngoài SWR/SWD | 10 / 10 / 10 | 3/3 giữ hai câu bằng tiếng Việt. Phân bổ từng câu thay đổi khi đầu vào chỉ chốt tổng. |

18/18 phản hồi hợp schema, 18/18 giữ đúng số câu và 0 lỗi provider. Có 13 phản hồi `VALID`, 2 phản hồi `NEEDS_REVIEW` và 3 phản hồi `INVALID` do sai tổng. `VALID` chỉ xác nhận các điều kiện backend kiểm được. Việc phát hiện mâu thuẫn nguồn vẫn phụ thuộc vào nội dung cảnh báo model trả về.

## Tương thích với prompt chấm bài U09

Đưa 15 đề qua kiểm tra điểm vào các hàm chuẩn hóa và tạo khung/rubric của U07, sau đó dựng prompt và schema U09 trên rubric thu được. Cả 15 giữ nguyên danh sách mục, điểm tối đa và tổng 10. Trong số này có 2 đề mang cảnh báo, kiểm tra tương thích cấu trúc trong bộ QA và vẫn cần giảng viên xem trước trong ứng dụng.

Lấy nguyên rubric câu sơ đồ use case từ phản hồi SWR SP25 ở lượt 1. Tạo một bài nộp tổng hợp đầy đủ, hai bản đối chứng lần lượt chỉ đổi nét cạnh `include` và thay actor phụ. Chạy Luna medium với prompt U09 hiện tại, kiểm phản hồi bằng schema và validator của U09.

| Bài nộp | Actor chính | Actor phụ | Ngữ nghĩa include | Ký pháp nét đứt | Tổng |
|---|---:|---:|---:|---:|---:|
| Nét đứt đúng hướng, đủ actor | 1 | 1 | 1 | 1 | 4/4 |
| Đổi cạnh include thành nét liền | 1 | 1 | 1 | 0 | 3/4 |
| Thay Safety reviewer bằng Library clerk | 1 | 0 | 1 | 1 | 3/4 |

Ở lượt chẩn đoán trước, Luna đã đọc nhầm ID kỹ thuật `request` thành tên hiển thị `Request session` và cho 0 điểm mục ký pháp của cạnh đúng. Khi dùng ID dễ phân biệt, Luna đọc đúng hướng và nét ở cặp đối chứng. Bản tóm tắt có thể đưa tên hiển thị của hai đầu cạnh cùng ID để giảm việc model tự nối hai danh sách.

68/68 test U09, gồm test MCP, đạt trên Java 17 dùng trong CI và Docker. Trên Java 21, 1/68 test trượt ở cụm `ví dụ` của luật actor phụ. Đã tái hiện khác biệt `\bví dụ\b` giữa hai JDK với regex hiện tại. Các lượt này không gọi Gemini qua gateway U06.

Bằng chứng cục bộ nằm trong `repeat-02/`, `repeat-03/`, `grading-probe/`, `grading-probe-clear-ids/`, `grading-probe-complete/`, `grading-regression-java17.log` và `grading-regression.log` dưới `.tmp/u07-pe-generation/`.

## Kiểm thử lại sau khi sửa ba điểm

U09 giờ đọc tên hiển thị cùng ID ở hai đầu cạnh. U07 mở xem trước cho đề sinh từ mẫu đã lưu hoặc DOCX, kể cả khi model không gửi cảnh báo. Giảng viên có thể nhập điểm từng câu khi tạo đề chấm điểm. Backend dùng tổng và số câu suy ra từ dãy điểm, lưu ràng buộc cùng tác vụ và kiểm tại callback lẫn apply.

Lặp lại 6 brief × 3 với GPT-6 Luna `medium`. Nội dung yêu cầu và đề mẫu giữ nguyên. Đợt này cả sáu đầu vào có dãy điểm từng câu xác định, nên đây là kiểm thử luồng đã cấu hình. Đợt trước có brief chỉ xác định tổng.

| Brief | Điểm từng câu đã nhập | Kết quả 3 lượt |
|---|---|---|
| SWR FA25 | `0.50 / 2.50 / 4.00 / 3.00` | 2/3 đúng điểm. Phản hồi Codex lượt 2 có dữ liệu dư sau JSON, bộ đọc nghiêm ngặt không nhận. |
| SWR SP25 | `2 / 4 / 2 / 2` | 3/3 đúng điểm. |
| SWD REST | `5 / 3 / 2` | 3/3 đúng điểm. |
| SWD Statechart | `3 / 2 / 2 / 3` | 3/3 đúng điểm. |
| SWD mẫu mâu thuẫn | `3.50 / 3.50 / 3.00` | 3/3 đúng điểm. Model báo mâu thuẫn 1/3 lượt. Đề có mẫu đều phải mở xem trước. |
| Đề tiếng Việt | `6 / 4` | 3/3 đúng điểm. |

17/18 phản hồi đọc được và hợp schema. Cả 17 giữ đúng số câu, thứ tự điểm từng câu và tổng 10. Provider trả lời đủ 18 lượt. Chạy Chrome với API giả lập cho đề hợp lệ, đề có cảnh báo và đề sinh từ mẫu có trạng thái `VALID`. Ca có mẫu gửi `[4, 6]` và chỉ gọi apply sau khi bấm xác nhận.

Chấm lại ba bài nộp đối chứng bằng prompt U09 sau khi sửa, giữ các ID kỹ thuật `request` và `check-eligibility` dễ nhầm với tên. Tóm tắt nay ghi cạnh từ `Approve session` tới `Review safety` cùng nét. Luna cho **4/4** khi đúng, **3/4** khi đổi nét liền và **3/4** khi thay sai actor phụ. Điểm ngữ nghĩa `include` giữ 1/1 cả ba bài.

229 test backend trên Java 17 và 94 test frontend đạt. TypeScript và ESLint đạt. Bằng chứng cục bộ gồm `followup-01/` đến `followup-03/`, `grading-probe-fixed-summary/`, `followup-backend-final.log`, `followup-browser-template.log`, `browser-followup-events.json` và `review-followup-template_valid.png`.

Gemini live qua gateway chưa được chạy. Số lần đọc nhầm ở ngoài các ca đối chứng chưa được đo lại.

## Khi để trống điểm từng câu

Chạy lại 6 brief tổng hợp × 3 qua Codex CLI với GPT-6 Luna `medium`. Dùng builder và schema U07 hiện tại. Cấu hình `AUTO` chỉ chốt tổng 10, để trống số câu và điểm từng câu. Hai brief vẫn có thang điểm trong nội dung yêu cầu hoặc mẫu. Kiểm tra SHA-256 xác nhận cùng một đầu vào trong ba lượt của mỗi brief.

| Brief | Lượt 1 | Lượt 2 | Lượt 3 |
|---|---|---|---|
| SWR FA25 | JSON có dữ liệu thừa | `0.5 / 2.5 / 4 / 3` | `0.5 / 2.5 / 4 / 3` |
| SWR SP25 | `2.5 / 2.5 / 2.5 / 2.5` | `2 / 3 / 3 / 2` | **8 điểm**, 5 câu thay vì 4 |
| SWD REST | `5 / 3 / 2` | `5 / 3 / 2` | `5 / 3 / 2` |
| SWD Statechart | JSON có dữ liệu thừa | `2.5 / 2.5 / 2.5 / 2.5` | `2 / 2.5 / 2.5 / 3` |
| SWD mẫu mâu thuẫn | `3 / 4 / 3`, có cảnh báo | `4 / 4 / 2`, thiếu cảnh báo | **13.5 điểm**, thiếu cảnh báo |
| Đề tiếng Việt | `6 / 4` | `6 / 4` | `6 / 4` |

- **14/18** đúng tổng 10 và đúng số câu theo brief, gồm 13 `VALID` và 1 `NEEDS_REVIEW`. Cả 14 giữ cấu trúc rubric khi dựng đầu vào chấm U09.
- **2/18** qua schema nhưng bị validator U07 chặn do sai tổng, gồm một bản tách sai số câu. **2/18** có dữ liệu thừa sau JSON, bộ đọc của phép thử từ chối. Không có lượt lỗi kết nối provider.
- **5/5** bản đọc được từ hai brief có điểm từng câu trong nội dung giữ đúng dãy điểm ở cấp câu. Khi giảng viên chỉ chốt tổng, các cách chia hợp lệ có thể thay đổi giữa các lượt.
- **1/3** phản hồi cho mẫu mâu thuẫn có cảnh báo. Luồng có mẫu vẫn yêu cầu giảng viên xem trước với cả bản `VALID`.

Chế độ tự chia có thể dùng để lấy bản nháp. Khi cần giữ nguyên thang điểm cho từng câu, phép thử có cấu hình cụ thể ở trên cho 17/17 bản đọc được giữ đúng điểm. Đợt này chạy thêm 12 test backend và 3 test frontend về tạo đề, tất cả đều đạt. Bằng chứng cục bộ nằm trong `auto-01/` đến `auto-03/`, `auto-summary.py` và `auto-backend.log` dưới `.tmp/u07-pe-generation/`. Phép thử chạy Luna qua Codex CLI và kiểm bằng Java 17, chưa chạy Gemini qua gateway hay đánh giá chất lượng câu hỏi bằng nhãn giảng viên.

## Khi số câu trong cấu hình và lời nhắc khác nhau

Giảng viên có thể để trống số câu, chốt số câu để AI tự chia điểm, hoặc nhập dãy điểm để hệ thống suy ra số câu và tổng điểm. Khi AI báo `QUESTION_COUNT_CONFLICT`, ứng dụng mở xem trước để giảng viên xác nhận hoặc sửa yêu cầu và tạo lại. Validator chặn sai số câu hoặc sai tổng điểm.

| Cấu hình | Lời nhắc | Ngôn ngữ | Số câu sinh ra | Tổng điểm | Kết quả |
|---|---|---|---:|---:|---|
| 4 câu · 10 điểm | 5 câu | Tiếng Anh | 4 | 10 | `NEEDS_REVIEW`, cảnh báo đúng hai con số |
| 4 câu · 10 điểm | 5 câu | Tiếng Việt | 4 | 10 | `NEEDS_REVIEW`, cảnh báo đúng hai con số |
| 4 câu · 10 điểm | 4 câu | Tiếng Anh | 4 | 10 | `VALID`, không cảnh báo mâu thuẫn |
| 4 câu · 10 điểm | 4 câu | Tiếng Việt | 4 | 7,50 | `INVALID` do sai tổng, không cảnh báo mâu thuẫn |

Cả bốn phản hồi GPT-6 Luna `medium` qua Codex CLI đều đọc được và qua schema. Trong ca đối chứng tiếng Việt có các ý con a/b, Luna giữ đúng 4 câu chính, nhưng tổng rubric chỉ đạt 7,50. Cảnh báo mâu thuẫn với lời nhắc do model phát hiện. So số câu và tính tổng rubric được kiểm bằng Java.

Trước sửa, Vitest không tìm thấy ô số câu ở màn hình tạo đề và các test Java từ chối mã cảnh báo mới. Sau sửa, 32 test backend và 96 test frontend liên quan đạt. TypeScript và ESLint trên các tệp đã sửa đạt. Chrome với API giả lập xác nhận gửi cấu hình 4 câu và lời nhắc 5 câu, hiện cảnh báo, rồi gọi áp dụng sau khi nhấn nút. Bằng chứng nằm trong `count-red-*.log`, `count-green-*.log`, `counts-conflict-01/`, `counts-matched-01/` và `count-browser-events.json` dưới `.tmp/u07-pe-generation/`. Chưa chạy Gemini qua gateway.
