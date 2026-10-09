# Rà soát Inception và Construction — 2026-10-09

> Findings bên dưới ghi nhận trạng thái trước revision. Phần resolution cuối tài liệu ghi kết quả sửa theo yêu cầu tiếp theo của người dùng.

## Kết luận và phạm vi

Tài liệu **chưa đồng bộ hoàn toàn và chưa nên dùng làm baseline triển khai thống nhất**. Các thay đổi 2026-10-09 đã vào nhiều thiết kế Construction và requirements/story, nhưng kiến trúc Inception, ownership, shared contract và trạng thái vẫn chứa quy tắc 2026-10-08. Ngoài lệch baseline, thiết kế quét học liệu có mâu thuẫn về retry và credit.

Nguồn đối chiếu tại checkout: [73 UC](../../../docs/use-cases-73.md), `docs/G21_Diagrams.drawio` (Page-2), [requirements](../../inception/requirements/requirements.md), [stories](../../inception/user-stories/stories.md) và thiết kế/code plans hiện hành. Không xác minh lại Google SRS, không kiểm thử code hay dịch vụ thật. Tài liệu lịch sử, câu hỏi cũ, approvals và U03 generated summaries không được coi là lỗi chỉ vì khác baseline mới.

## R01 — P1: Quyền Administrator còn hai cách hiểu trái nhau

**Bằng chứng:** FR-002/003 hiện cấm Admin nhận phân công môn/lớp và dùng chức năng giảng dạy; [shared contract](../current-srs-contract.md) dòng 13–15 cũng cấm. Nhưng [application-design](../../inception/application-design/application-design.md) dòng 24–25 và [component-methods](../../inception/application-design/component-methods.md) dòng 226 vẫn cho Admin dùng R2/R4 khi được phân công. [personas](../../inception/user-stories/personas.md) dòng 22 vẫn ghi Administrator → Subject Manager; [stories](../../inception/user-stories/stories.md) dòng 8 và US-RPT-003 cũng giữ actor Admin được giao dạy. Đây là nội dung hiện hành, không phải đoạn lịch sử đã đánh dấu.

**Ảnh hưởng:** Người triển khai theo kiến trúc/actor có thể cấp quyền học liệu, bài, chấm và gradebook cho Admin trái requirements; các unit có thể dùng authorization khác nhau.

**Hướng sửa:** Đồng bộ actor, method preconditions, personas và acceptance criteria theo FR-002; giữ Subject Manager kế thừa Teacher có điều kiện phân công lớp. Kiểm denial cho Admin tại API môn/lớp và AI, không chỉ chặn route.

## R02 — P1: Ownership và UC map vẫn là bản 70 UC/16 unit

**Bằng chứng:** [stories mục 14](../../inception/user-stories/stories.md) dòng 1548 trở đi, [unit-of-work-story-map](../../inception/application-design/unit-of-work-story-map.md), [shared contract](../current-srs-contract.md) dòng 30–51 và [state](../../aidlc-state.md) vẫn dùng 70 UC/16 unit. Trong catalog hiện tại, UC 70/71 là Settings và UC 73 là Audit; shared contract vẫn ghi Audit UC 70. U03 đã nhận UC 70/71 và US-SET-001 trong thiết kế hiện hành nhưng ownership vẫn ghi U03 không primary UC/story.

Construction có **15 thư mục unit** hiện hữu, không có U10. Story catalog vẫn có **51 heading**, nhưng thành phần đã đổi: US-CAT-005 không còn heading, US-SET-001 đã thêm. Bảng ownership vẫn có US-CAT-005 và thiếu US-SET-001. U08 plan đã nhận US-ASM-010 từ U10; US-ASM-009 bài của môn chưa có primary ownership hiện hành thống nhất (header U08 chỉ ghi US-ASM-001/008).

**Ảnh hưởng:** Phân công sai unit, bỏ sót Settings/story hoặc triển khai chức năng mang số UC của chức năng khác. Con số 51 đúng không chứng minh coverage đúng.

**Hướng sửa:** Lập lại map theo tên/chức năng của 73 UC; mỗi UC/story đúng một primary unit, phân biệt unit hỗ trợ. Chốt ownership US-ASM-009 và US-ASM-010, cập nhật current state và wave/gate sau khi bỏ U10. Giữ approvals 16 plan ngày 05/10 như lịch sử.

## R03 — P1: Inception vẫn yêu cầu các chức năng đã bỏ/thay thế

**Bằng chứng:** FR-027 và [U08 domain](../u08-assessment-core-publication/functional-design/domain-entities.md) đã thay template bằng bài của môn, copy về U08, bỏ U10. Tuy nhiên [unit-of-work](../../inception/application-design/unit-of-work.md) dòng 28 và [component-methods](../../inception/application-design/component-methods.md) dòng 170–175 vẫn thiết kế TemplateService/release/copy template. US-AIG-002 dòng 748–750 vẫn nói template, có khung bài nhóm cấp môn và không có đề chung giao thẳng cho lớp, trái FR-027/U13 hiện hành.

[components](../../inception/application-design/components.md) dòng 13–16, 31–37, 72 và [services](../../inception/application-design/services.md) dòng 16–22 vẫn có mã mời, bình luận, ngân hàng hai scope CLASS/SUBJECT và U10. FR-022/023 và U04/U05/U06 hiện hành đã bỏ mã mời, bình luận và Class Question Bank.

**Ảnh hưởng:** Triển khai lại chức năng ngoài scope, tạo thêm API/bảng hoặc giao diện không còn được yêu cầu.

**Hướng sửa:** Sửa nội dung kiến trúc và story hiện hành, chuyển ownership copy sang U08; giữ scope câu riêng `ASSIGNMENT` và scope rubric CLASS/SUBJECT vì chúng không phải ngân hàng lớp. Không thay từ khóa toàn cục làm hỏng các scope hợp lệ.

## R04 — P1: Retry quét học liệu có thể bị bỏ qua và kẹt SCANNING

**Bằng chứng:** [U05 NFR pattern P1](../u05-content-material-rag/nfr-design/nfr-design-patterns.md) dòng 7 chỉ claim lesson `PENDING`, không cập nhật được thì bỏ. Dòng 9–10 lại trả lỗi tạm cho U03 retry và yêu cầu sweeper gửi lại `BUSY`. [Functional F6](../u05-content-material-rag/functional-design/business-logic-model.md) dòng 35 cho nhận `PENDING` hoặc `BUSY`, nhưng cũng bỏ mọi trạng thái khác. Không có bước reset sau lỗi tạm hoặc phục hồi `SCANNING` quá hạn được mô tả ở các đoạn này.

**Kịch bản:** Lần đầu đặt SCANNING rồi gặp timeout; message retry đến thấy SCANNING và bị bỏ. Backend/worker chết sau claim cũng để SCANNING. Với BUSY, sweeper gửi lại nhưng SQL NFR chỉ nhận PENDING nên bỏ.

**Hướng sửa:** Quy định cùng một state machine cho functional/NFR/plan: nhận BUSY khi đến hạn, chuyển lỗi tạm sang trạng thái retry được, có cơ chế phục hồi worker chết và chống hai worker xử lý cùng lesson. Dùng mốc hết hạn tuyệt đối cho giới hạn 24 giờ; `scanned_at` hiện là thời điểm đổi trạng thái gần nhất nên không đủ để chứng minh tổng thời gian chờ khi bị cập nhật nhiều lần. Kiểm các kịch bản timeout, worker chết, BUSY hồi phục và message trùng.

## R05 — P1: Chính sách credit khi quét thất bại chưa thống nhất

**Bằng chứng:** [BR-U05-46](../u05-content-material-rag/functional-design/business-rules.md) dòng 52 nói quét thất bại thì không trừ và trả phần đã giữ. Nhưng BR-U05-39 nói trả phần còn giữ; [U13 BR-U13-52](../u13-ai-code-execution/functional-design/business-rules.md) nói `release(holdId)` settle tổng credit đã dùng, chỉ trả toàn bộ khi chưa gọi AI. [U05 F6](../u05-content-material-rag/functional-design/business-logic-model.md) dòng 39–42 đã complete tóm tắt trước khi embedding, nên embedding FAILED có thể xảy ra sau một lời gọi đã dùng credit. Domain Lesson dòng 44 lại ghi summary rỗng khi FAILED, trong khi flow lưu summary ngay và không nói xóa khi embedding thất bại.

**Ảnh hưởng:** Một tình huống có hai kết quả số dư/summary tùy tài liệu được dùng; test và thông báo người dùng không có oracle thống nhất.

**Hướng sửa:** Diễn đạt rõ FAILED trước/sau lời gọi AI, credit dùng thật và phần chưa dùng; làm rõ summary đã tạo có giữ/hiển thị khi embedding thất bại không. Quyết định phải khớp yêu cầu chỉ trừ cho lời gọi AI thật; cập nhật rule, flow, domain và test cùng nhau.

## R06 — P2: Ví Admin vẫn tồn tại trong tài liệu nguồn của U01/story

**Bằng chứng:** FR-010, shared contract và U07 BR-U07-01 cấm ví Admin. Nhưng [U01 domain](../u01-account-and-access/functional-design/domain-entities.md) dòng 84 vẫn cấp ví/tặng tháng/mua cho cả bốn role; US-AIG-003 S4 dòng 804 cũng ghi bốn vai trò nhận mức tặng. [Inception services](../../inception/application-design/services.md) dòng 55 cho cả bốn role tạo payment.

**Hướng sửa:** Đồng bộ eligibility Student/Teacher/Subject Manager. Không tự xóa dữ liệu hoặc giao dịch Admin lịch sử từ việc bỏ quyền hiện hành; cập nhật seed/activation/monthly grant và denial scenarios khi triển khai.

## R07 — P2: Quy trình ingest Inception thiếu tóm tắt và giữ credit trước tải

**Bằng chứng:** FR-004 và US-CNT-001 S6 yêu cầu đủ credit trước tạo học liệu, tóm tắt rồi embedding từ summary. [Services: Quét học liệu](../../inception/application-design/services.md) dòng 50–52 lại mô tả lesson hiện ngay, worker mới giữ credit và chỉ gọi embedding; bảng job còn NO_CREDIT không retry. Không mô tả summary, hold lúc tạo và BUSY retry 24 giờ như U05/U13 hiện hành.

**Hướng sửa:** Đồng bộ orchestration Inception → U05/U13/U07/U03, thứ tự giữ/settle và các trạng thái. Phân biệt upload byte qua U03 với tạo lesson qua U05: U03 hiện giữ file upload thành công dù chưa gắn, vì vậy “không đủ credit thì không tải lên” cần nêu rõ điểm kiểm bắt buộc, không chỉ chặn nút frontend.

## R08 — P2: Plan U05 còn nguồn cấu hình AI đã bị thay thế

**Bằng chứng:** [U05 code plan](./code-generation-plan/u05-content-material-rag-code-generation-plan.md) dòng 38 yêu cầu U13 thay adapter tạm bằng `ai_services`; dòng 82 và [U05 infrastructure](../u05-content-material-rag/infrastructure-design/infrastructure-design.md) dòng 24 vẫn dùng AI_KILL_SWITCH như biến cấu hình. U13 hiện hành bỏ bảng ai_services, lấy kill-switch/model/trần từ Settings của U03.

**Hướng sửa:** Ghi rõ biến môi trường nào chỉ phục vụ adapter giả/local, Settings nào là nguồn runtime thật. Bỏ chỉ dẫn tạo/đọc bảng ai_services trong bước triển khai chưa làm; nối model của SummaryPort/EmbeddingPort với cùng snapshot cấu hình và AiUsagePort.

## R09 — P2: Link nguồn và nhãn màn còn cũ

**Bằng chứng:** Quét 163 file Markdown thiết kế/requirements/story/shared hiện hành tìm **5 link local hỏng trong 4 file**, đều trỏ `docs/use-cases-and-screens.md` đã không tồn tại: requirements.md, stories.md (2 link), unit-of-work-story-map.md, current-srs-contract.md. Inception components và shared contract còn My Classes/Assigned Classes/Subject Classes/Subject Template/Template Editor và năm danh sách bài riêng. Page-2 của G21_Diagrams.drawio hiện dùng Class Dashboard/Manager Dashboard/Admin Dashboard, Student Assignments, Quiz Practice và các màn Settings mới.

**Hướng sửa:** Trỏ catalog sang use-cases-73.md và mô tả nhãn/entry theo Page-2. Tên class/route nội bộ có thể giữ nếu UI và quyền đúng; không coi khác tên kỹ thuật là lỗi sản phẩm.

## Thứ tự xử lý đề xuất

1. Chốt một baseline 73 UC, quyền Admin và 15 unit; sửa shared contract, story/UC ownership, state và wave/gate.
2. Đồng bộ Inception architecture/services/personas/story theo scope mới; bỏ chức năng đã rút khỏi nội dung hiện hành.
3. Làm rõ retry, credit và summary của U05/U13; đồng bộ functional/NFR/plan và kịch bản kiểm chứng.
4. Sửa link/màn/config references, rồi kiểm coverage và links lần nữa trước khi triển khai revision.

## Kiểm tra đã thực hiện

- [x] Đọc CLAUDE.md, common rules, state, extensions và lịch sử review/sync 08/10; tiếp tục review, không khởi động workflow phát triển mới.
- [x] Đối chiếu catalog 73 UC, Page-2 Drawio, requirements/stories, shared contract và các mô tả kiến trúc/ownership.
- [x] Rà các thiết kế/code plans liên quan đến thay đổi quyền, Settings, học liệu, câu/rubric, bài của môn, AI và thanh toán.
- [x] Quét structural 163 Markdown hiện hành (không gồm plans, generated-code summaries, question files/round 2): 5 link local hỏng, không thấy code fence lẻ; Drawio XML parse được.
- [x] Đếm catalog 73 hàng UC, 51 heading story và 15 thư mục unit; kiểm các sai lệch mapping cụ thể, không coi đếm đủ là coverage đạt.
- [x] Ghi báo cáo và audit; giữ nguyên thiết kế, code, approvals và stage checkpoints.

## Extension status và giới hạn

SECURITY-08: có finding R01/R06 ở baseline quyền mô tả; không kết luận authorization đạt. SECURITY-03/04/05/09/12/15: không tạo thay đổi triển khai trong review, không chứng nhận runtime; cần giữ validation, sanitization, audit, secret và authentication khi sửa. RESILIENCY-10 chỉ áp dụng timeout theo state: không kết luận vi phạm timeout từ finding retry/state recovery R04; R04 là lỗi nhất quán thiết kế nghiệp vụ. RESILIENCY-04/06: deployment/health verification N/A cho review tài liệu này. Các rule ngoài phạm vi đồ án N/A theo state; Property-Based Testing disabled, đã bỏ qua.

Báo cáo là kết quả review, không phải phê duyệt thiết kế/plan mới hoặc bằng chứng các yêu cầu đã triển khai. Không sửa lại lịch sử 08/10 thành kết quả cho bản 09/10.

## Resolution — revision tài liệu 2026-10-09

Đã xử lý R01–R09 trong tài liệu hiện hành:

- R01/R06: requirements, personas, stories, kiến trúc và U01 thống nhất Admin chỉ chức năng quản trị, không quyền học thuật/ví/AI. Subject Manager dùng quyền Teacher khi được phân công lớp.
- R02/R03: [primary map](../../inception/application-design/unit-of-work-story-map.md) và [contract](../current-srs-contract.md) bao phủ 73 UC, 51 story, 15 unit. Settings U03; bài môn/version/copy U08; bỏ U10, mã mời, comments, template và ngân hàng lớp khỏi thiết kế hiện hành.
- R04/R05: U05 có deadline tuyệt đối 24 giờ, claim/lease/recovery, retry hữu hạn và checkpoint. U13 tách HOLD khỏi từng lời gọi AI, settle lượng dùng thật, trả dư. Embedding lỗi giữ summary; chưa INDEXED thì chưa vào RAG. Không cam kết exactly-once cho provider bên ngoài.
- R07/R08: orchestration Inception khớp tóm tắt rồi embedding; giữ credit tại tạo lesson, phân biệt upload staging U03. Runtime AI dùng Settings U03 qua U13; bỏ chỉ dẫn dùng ai_services/AI_KILL_SWITCH cho adapter thật.
- R09: sửa link catalog và đồng bộ tên màn với Page-2 G21_Diagrams.drawio.

Kiểm chứng: 179 tài liệu hiện hành, 150 design headers, 73 UC và 51 story đúng một primary owner; 15 unit, không vòng dependency H, không link local hỏng hoặc fence lẻ. Giữ nguyên 64 historical stage plans, U03 completed code plan và lịch sử state. Hình dependency cũ có nhãn historical; đồ thị chữ hiện hành dùng 15 unit.

Kết quả này xác nhận tài liệu, chưa xác minh implementation/integration. Approval và checkpoint triển khai giữ nguyên. Xem [checklist revision](../../inception/plans/uc-73-sync-2026-10-09.md).
