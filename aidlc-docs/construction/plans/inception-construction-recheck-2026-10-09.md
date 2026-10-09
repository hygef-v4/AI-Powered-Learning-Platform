# Kiểm lại đồng bộ Inception/Construction — 2026-10-09

> Các findings C01–C05 ghi nhận snapshot trước lần sửa bổ sung. Đã xử lý trong revision tiếp theo theo yêu cầu “giúp tôi đồng bộ lại doc”; xem Resolution cuối tài liệu.

## Kết luận tại thời điểm recheck trước sửa bổ sung

**Khung mapping đã đồng bộ, nội dung chưa đồng bộ hoàn toàn.** Lần rà lại sau revision tìm thấy năm nhóm còn sót. Kết quả resolution trong review trước chỉ phản ánh phạm vi sửa lúc đó; không dùng làm bằng chứng mọi nội dung đã nhất quán.

Nguồn: [catalog 73 UC](../../../docs/use-cases-73.md), Page-2 `docs/G21_Diagrams.drawio`, requirements/stories, application design, shared contract và thiết kế unit tại checkout. Đây là review tài liệu, không sửa thiết kế hoặc code và không xác minh SRS online/runtime.

## C01 — P1: Credit của U07 còn trái luồng HOLD U05/U13

[U07 BR-U07-40/43](../u07-payment-credit/functional-design/business-rules.md) dòng 49–52 yêu cầu reserve trước mỗi lời gọi Gemini và trả toàn bộ khi AI lỗi/quá hạn. [U07 domain](../u07-payment-credit/functional-design/domain-entities.md) dòng 81–84 còn mô tả scanner 25 giờ hoàn toàn bộ hold học liệu.

Trong khi [Inception services](../../inception/application-design/services.md) dòng 53–56 và U05/U13 hiện hành giữ một HOLD khi tạo lesson, child calls không reserve thêm; terminal lỗi sau summary phải settle phần đã dùng và trả dư. Theo U07 có thể giữ thêm cho embedding hoặc hoàn lại cả credit summary đã xử lý. Đây là mâu thuẫn chính sách, không phải chỉ khác tên method.

Cần phân biệt `AiUsagePort.release(holdId)` của U13 (chốt tổng thật) với `CreditPort.release(accountId, reserved, fromFree)` của U07 (hoàn toàn bộ khi lượng dùng bằng 0). [component-methods](../../inception/application-design/component-methods.md) dòng 129 đang ghi U07 release là “chốt lượng thật” dù chữ ký không nhận actualCredits. Đồng bộ U07 rule/state text, hai tầng release và scanner trước triển khai.

## C02 — P2: FR-024 còn dùng UC và màn hình của catalog cũ

[requirements](../../inception/requirements/requirements.md) dòng 254–258:

- Statistics ghi UC 57; catalog/Construction là Admin Dashboard UC 58. UC 57 hiện là Add/Edit Question.
- Phân bố điểm ghi UC 17 và năm danh sách bài; hiện là Student Assignments UC 22, quiz tách riêng. UC 17 hiện là Request Group Leader Change.
- Tiến độ nộp ghi UC 34; U16 hiện hỗ trợ UC 37 Student Submissions. UC 34 hiện là Add/Edit Class Material.
- AI proposals/điểm cuối ghi UC 35–36; đúng luồng là UC 38/39, và công bố/gradebook theo UC 40. UC 35/36 hiện là quiz/thông báo lớp.

Bảng trace cuối stories đúng không loại bỏ các tham chiếu sai trong nội dung FR. Cần sửa đoạn FR-024 theo U16/U15 và catalog.

## C03 — P2: Acceptance criteria phân bố điểm còn sai actor/entry

[stories](../../inception/user-stories/stories.md) dòng 1189 và 1255 còn yêu cầu mở “một trong năm danh sách bài”; US-RPT-001 S3 dòng 1254 đặt điều kiện giảng viên bật phân bố điểm. US-RPT-002 dòng 1260/1269 còn dùng màn Statistic.

[U04 BR-U04-13/17](../u04-subject-class-enrollment/functional-design/business-rules.md) dòng 22/26 quy định chỉ Chủ nhiệm môn bật/tắt trong UC 52; Teacher được giao dạy chỉ đọc cấu trúc lớp. [U16 BR-U16-42](../u16-reporting-notification/functional-design/business-rules.md) dòng 52 đặt phân bố trên Student Assignments UC 22 và loại quiz/Practice. Cần đổi actor, entry và tên Admin Dashboard trong story để acceptance test không cấp quyền hoặc dựng màn cũ.

## C04 — P2: Contract AiUsagePort chưa mô tả đủ checkpoint mới

U05 F6 yêu cầu worker đọc lại HOLD và kết quả chunk/merge/embedding READY khi restart, và complete lưu checkpoint cùng lượng dùng trong transaction. U13 domain dòng 56 cũng yêu cầu result có cấu trúc. Nhưng [AiUsagePort](../u13-ai-code-execution/functional-design/domain-entities.md) dòng 141 vẫn chỉ có `complete(ticket, tokens, cost)`; không mô tả result/checkpoint input, lookup hold hoặc dữ liệu trả về khi begin gặp lời gọi đã READY. Inception component-methods dòng 194 giữ cùng chữ ký.

Chưa đủ thông tin cho U05 thực hiện recovery qua public contract mà không đọc repository nội bộ U13. Cần mô tả rõ DTO/result của begin, cách nhận lại hold/checkpoint, complete với checkpoint và điều kiện claim hợp lệ; không nhất thiết thêm API nếu mở rộng giá trị trả về/param của port đã đủ. Đây là thiếu đặc tả contract, chưa phải bằng chứng runtime lỗi.

## C05 — P3: Shared Infrastructure còn ghi 16 unit

[shared-infrastructure](../shared-infrastructure.md) dòng 3 vẫn ghi hạ tầng dùng chung cho 16 unit; unit map/state/Construction hiện có 15 unit và không U10. Cần đổi nhãn hiện hành. Hình dependency cũ đã được đánh dấu historical, không tính là finding.

## Kiểm tra đạt và giới hạn

- Catalog có 73 ID liên tục; bảng stories có 73 dòng đúng tên UC và đúng owner.
- 51 story heading duy nhất; mỗi UC/story đúng một primary unit trong 15 unit, không U10.
- 150 current design headers khớp primary stories và baseline ngày 09/10.
- Quét 178 tài liệu hiện hành (architecture, requirements/stories, shared và unit designs/code plans): không link local hỏng, không fence lẻ. Dependency matrix 15 unit không có vòng H. Git whitespace check đạt.
- Không dùng câu hỏi, approvals, hình historical hay completed code làm lỗi chỉ vì thuộc baseline trước. Chưa kiểm implementation/integration hoặc render mọi diagram bằng engine Mermaid.

SECURITY-08: C03 là mâu thuẫn quyền trong acceptance criteria, cần sửa trước dùng làm baseline triển khai. SECURITY-03/04/05/09/12/15: không thay đổi triển khai, runtime N/A. RESILIENCY-10: C04 liên quan recovery/checkpoint, không kết luận vi phạm timeout; deployment/health RESILIENCY-04/06 N/A. Các rule ngoài phạm vi đồ án N/A; Property-Based Testing disabled nên bỏ qua. Không chuyển stage hoặc cấp approval mới.

## Resolution — sửa bổ sung sau recheck

- [x] C01: U07 rule/domain/flow, U13 rule/flow/scanner/NFR và Inception methods/shared contract thống nhất một HOLD cho cả lần quét; child call không reserve thêm. AiUsagePort.release chốt lượng thật; CreditPort.release chỉ hoàn toàn bộ khi chưa dùng. Lỗi/quá hạn sau sử dụng settle phần dùng thật và trả dư. Các checklist triển khai U07/U13 vẫn chưa hoàn thành.
- [x] C02: FR-024 dùng Admin Dashboard UC 58, Student Assignments UC 22, cấu hình phân bố UC 52, tiến độ UC 37, AI UC 38, chấm tay UC 39 và Gradebook UC 40.
- [x] C03: US-GRD-004, US-RPT-001/002 sửa entry/tên màn và quyền bật phân bố điểm chỉ Chủ nhiệm môn; Teacher chỉ được giao dạy bị từ chối bật/tắt. Quiz/Practice không có phân bố điểm; bài của môn tính theo lớp.
- [x] C04: [AiUsagePort U05](../u05-content-material-rag/functional-design/domain-entities.md) định nghĩa HoldSnapshot, findHold, UsageStart RUN/REPLAY/BUSY/IN_PROGRESS/CLOSED, CallSnapshot, checkpoint và ticket scanClaimId. U13 lưu metadata/result; U05 kiểm claim cùng transaction, chỉ RUN gọi provider và REPLAY dùng kết quả qua DTO. complete/fail/release khóa cùng HOLD; ticket cũ hoặc HOLD đã chốt không ghi mới. Đã đồng bộ Inception và U05/U13 functional/NFR/infrastructure/code plans.
- [x] C05: Shared Infrastructure ghi 15 unit hiện hành, không U10.

Kiểm chứng mới: 178 tài liệu hiện hành cùng review/checklist revision, 150 headers; 73 UC/51 story đúng một primary owner trong 15 unit; không link hỏng/fence lẻ/vòng H. Kiểm nội dung riêng C01–C05 đạt. Giữ nguyên 64 historical stage plans, các completed checklist code, toàn bộ U03 code plan và history của state; Git diff chỉ tài liệu, whitespace đạt. Không render mọi diagram bằng engine Mermaid hay xác minh code/provider thật.

SECURITY-08: mâu thuẫn actor C03 đã sửa ở cấp đặc tả; không chứng nhận runtime authorization. SECURITY-03/04/05/09/12/15 giữ ràng buộc thiết kế, runtime N/A; RESILIENCY-04/06 N/A và RESILIENCY-10 giữ timeout hiện hành, làm rõ recovery/checkpoint. Rule ngoài phạm vi đồ án N/A; PBT disabled bỏ qua. Không phát sinh approval stage hoặc hoàn tất implementation.
