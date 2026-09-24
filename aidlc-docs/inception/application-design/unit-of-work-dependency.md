# Unit of Work Dependencies - 16 unit logic

## 1. Ký hiệu và nguyên tắc

Hàng là consumer, cột là provider. `H` cần behavior/contract ổn định trước integration gate; `C` phát triển song song qua contract có version và fake adapter; `E` tiêu thụ event/read model; `-` không phụ thuộc trực tiếp. Không ký hiệu nào cho phép đọc bảng hoặc repository của unit khác. Các unit vẫn thuộc một backend modular monolith; worker là process riêng.

## 2. Dependency matrix

| Consumer \ Provider | U01 | U02 | U03 | U04 | U05 | U06 | U07 | U08 | U09 | U10 | U11 | U12 | U13 | U14 | U15 | U16 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| U01 | - | - | C | - | - | - | - | - | - | - | - | - | - | - | - | - |
| U02 | C | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| U03 | H | H | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| U04 | H | H | - | - | C | - | C | - | - | - | - | - | - | - | - | - |
| U05 | H | H | H | H | - | - | - | - | - | - | - | - | - | - | - | - |
| U06 | H | H | - | H | - | - | - | - | - | - | - | - | - | - | - | - |
| U07 | H | H | - | H | - | - | - | - | - | - | - | - | - | - | - | - |
| U08 | H | H | - | H | H | H | - | - | - | - | - | - | C | - | - | - |
| U09 | H | H | - | H | - | H | - | H | - | - | - | - | - | - | - | - |
| U10 | H | H | - | H | - | H | - | H | H | - | - | - | - | - | - | - |
| U11 | H | H | H | H | - | H | - | H | H | H | - | - | C | - | - | - |
| U12 | H | H | - | H | - | - | - | H | - | - | - | - | - | - | - | - |
| U13 | H | H | H | - | H | H | - | - | - | - | - | - | - | - | - | - |
| U14 | H | H | H | H | - | - | - | H | - | - | H | H | - | - | - | - |
| U15 | H | H | - | H | - | H | - | H | - | - | H | - | H | H | - | - |
| U16 | H | H | H | E | E | - | E | E | - | - | E | E | - | E | E | - |

### Dependency graph theo wave

```mermaid
flowchart LR
    subgraph W1["WAVE 1"]
        U01((U01))
        U02((U02))
        U03((U03))
        U04((U04))
    end
    subgraph W2["WAVE 2"]
        U05((U05))
        U06((U06))
        U07((U07))
        U08((U08))
    end
    subgraph W3["WAVE 3"]
        U09((U09))
        U10((U10))
        U11((U11))
        U12((U12))
        U13((U13))
    end
    subgraph W4["WAVE 4"]
        U14((U14))
        U15((U15))
        U16((U16))
    end

    U01 --> U03
    U01 --> U04
    U02 --> U03
    U02 --> U04
    U03 --> U05
    U04 --> U05
    U04 --> U06
    U04 --> U07
    U05 --> U08
    U06 --> U08
    U05 --> U13
    U06 --> U13
    U08 --> U09
    U08 --> U12
    U09 --> U10
    U10 --> U11
    U11 --> U14
    U12 --> U14
    U13 --> U15
    U14 --> U15
    U03 --> U16

    U01 -.-> U02
    U03 -.-> U01
    U05 -.-> U04
    U07 -.-> U04
    U13 -.-> U08
    U13 -.-> U11
    U15 -.-> U16
```

U01 phụ thuộc U03 bằng cạnh `C` cho ảnh đại diện: U01 khai báo `AvatarPort`, U03 cung cấp implementation, nên U01 vẫn khởi động không cần chờ ai. U04 phụ thuộc U05 và U07 bằng cạnh `C`: phần Learning Access của U04 dùng port trung lập ở tầng contract, U05 và U07 cung cấp implementation, nên không tạo chu trình cứng với cạnh `H` theo chiều ngược lại. Mọi unit nghiệp vụ đều phụ thuộc `H` vào U01 để kiểm quyền actor/object và vào U02 để ghi audit append-only; các cạnh này không vẽ lại trong sơ đồ vì đã được phủ bắc cầu qua U03/U04/U05. Mũi tên liền là cạnh `H` tối giản theo bắc cầu: nếu A → B → C thì không lặp A → C. Mũi tên đứt U01 → U02, U13 → U08/U11 và U05/U07 → U04 là tích hợp contract `C`; hai mũi tên cuối là port trung lập cho phần Learning Access của U04. U15 → U16 minh họa event/read model `E`. Ma trận phía trên vẫn là danh sách đầy đủ, gồm các cạnh `E` khác đi vào U16. Chiều mũi tên luôn từ provider sang consumer. Khung wave là nhóm và điểm dừng tích hợp, **không phải hàng rào đồng bộ**: node ở wave sau có thể mở khi provider trực tiếp của nó xong, dù node khác của wave trước vẫn chạy. Các mũi tên trong cùng khung thể hiện thứ tự mở việc của từng nhánh.

**Diễn giải bằng chữ:** Wave 1 có U01 và U02 khởi động song song; U03/U04 mở khi cả hai cung cấp contract/behavior cần thiết. Wave 2 có U05/U06/U07 song song sau U04; U08 theo U05/U06. Phần Learning Access của U04 hoàn tất tại wave này khi implementation của U05 và U07 cắm vào port trung lập. Wave 3 có U13 chạy khi nguồn U03/U05/U06 sẵn sàng, trong khi U09/U12 theo U08; U10 theo U09 và U11 theo U04/U10. Wave 4 có U14 sau U11/U12, U15 sau U14/U13 và U16 sau các event của owner. Các nhánh vượt ranh giới wave ngay khi dependency trực tiếp đạt; số unit đang triển khai đồng thời trên toàn nhóm không quá năm.

## 3. Public contract theo luồng

| Luồng | Provider → Consumer | Contract và giới hạn |
|---|---|---|
| Identity → audit/job query | U01 → U02 (`C`) | U02 ghi audit/điều phối job độc lập bằng actor/scope reference; `queryAudit` và `getJobStatus` gọi authorization contract có version, fail closed khi chưa tích hợp |
| Identity, audit, file, job | U01/U02/U03 → mọi unit cần dùng | Actor/resource authorization, append-only audit, scoped artifact, idempotent job; không dùng shared repository |
| Môn/lớp/ghi danh | U04 → U05-U15 khi cần | Subject/class/enrollment reference và scoped authorization |
| Learning access within U04 | U04 enrollment + U07 entitlement + U05 published content → Learning Access capability in U04 | Đây là orchestration nội bộ của U04, không phải self-dependency giữa unit; chỉ trả nội dung khi enrollment, entitlement và publication hợp lệ; không lưu lesson progress |
| Ngân hàng → đề/attempt/chấm | U06 → U08/U09/U10/U11/U15 | QuestionVersion/RubricVersion immutable và snapshot đúng version |
| Tạo đề | U05/U06 → U13 → U08; U09/U10 cấu hình | U13 trả AI draft proposal; U08 review, sửa, lưu và publish. RAG chỉ hỗ trợ nguồn khi được chọn |
| Đề → attempt | U08/U09/U10 → U11 | Publication, schedule, simulation policy và assignment/question/rubric snapshot; sửa đề tạo version mới |
| Nhóm → phần nộp | U12 + U11 → U14 | Allocation, immutable part submission và ordered source versions; giảng viên chốt composite |
| Chấm | U11/U14/U06 → U15; U13 hỗ trợ | AI chỉ trả proposal; U15 lưu manual/final grade, composite luôn chấm tay |
| Báo cáo/thông báo | Owner events → U16 | Projection theo quyền, outbox delivery; lỗi gửi không rollback transaction nguồn |

### Ranh giới tránh vòng phụ thuộc

- U13 định nghĩa provider-neutral `AiDraftProposal` và `CodeRunResult`; U08/U11/U15 chuyển yêu cầu đã kiểm quyền sang contract đó. U13 không đọc bảng Assessment/Submission/Grading và không publish/chốt thay owner.
- U02 không chờ U01 hoàn thiện để triển khai append-only audit, enqueue/lease/retry/outbox. Chỉ các read API có actor (`queryAudit`, `getJobStatus`) tích hợp `AuthorizationService` của U01 qua contract `C`; adapter thật là bắt buộc trước khi phát hành API và mặc định từ chối nếu không kiểm quyền được.
- U08 có thể hoàn thành luồng tạo đề thủ công trước khi U13 tích hợp AI. `C` ở U08 → U13 là contract tích hợp, không tạo hard cycle.
- U04 chỉ sở hữu kiểm quyền và truy cập học liệu. Dashboard frontend ghép dữ liệu từ API của U04, U08 và U16 khi các API có sẵn; U04 backend không phụ thuộc ngược U08/U16.
- U07 cung cấp entitlement qua port trung lập. U04 cần contract này để kiểm quyền truy cập nội dung có paywall; U07 không ghi enrollment vào U04.

## 4. Worker ownership

| Handler | Owner | Kết quả qua owner contract |
|---|---|---|
| File/YouTube ingestion và RAG index | U05 | Content/source/transcript version và index reference |
| AI assessment draft | U13 | Draft proposal cho U08 duyệt/lưu |
| Code sandbox run | U13 | Run result bất biến cho U11/U09 dùng |
| Group composite | U14 | Derived artifact/composite version với lineage |
| AI grade proposal và compact Draw.io | U13 phối hợp U15 | Proposal/derived artifact; U15 quyết định điểm |
| Payment reconciliation | U07 | Payment/entitlement state |
| Notification/export | U16 | Delivery status/scoped export artifact |

Job Platform U02 giữ lease/retry/dead-letter/status, còn owner nghiệp vụ kiểm tra idempotency và lưu kết quả. Worker payload chỉ chứa ID/reference và scope; worker tải nguồn qua contract có quyền.

## 5. Bốn wave với lịch mở việc liên tục

| Wave | Unit | Số unit | Nhánh và điều kiện mở |
|---|---|---:|---|
| 1 | U01, U02, U03, U04 | 4 | U01 và U02 song song (`C` cho read API của U02); U03/U04 sau U01 và U02; phần Learning Access của U04 chỉ khai báo port, hoàn tất ở wave 2 |
| 2 | U05, U06, U07, U08 | 4 | U05/U06/U07 sau U04; U08 sau U05/U06; U05 và U07 cắm implementation vào port Learning Access của U04 |
| 3 | U09, U10, U11, U12, U13 | 5 | U09/U12 sau U08; U10 sau U09; U11 sau U04/U10; U13 sau U03/U05/U06 |
| 4 | U14, U15, U16 | 3 | U14 sau U11/U12; U15 sau U14/U13; U16 nhận event sau U15 |

Wave biểu thị nhóm và checkpoint kết quả, không buộc toàn bộ unit của wave trước đóng mới cho mở unit tiếp theo. Scheduler mở unit khi các provider `H` của riêng unit đã sẵn sàng và còn slot; giới hạn tối đa năm unit đang triển khai cùng lúc tính trên toàn bộ wave. U08 có thể làm phần thủ công trước U13; AI tích hợp sau qua contract `C`. U16 có thể chuẩn bị schema/projection từ đầu, nhưng chỉ hoàn tất khi event từ các owner, gồm U15, đã có.

| Gate | Producer cần ổn định | Kiểm tra theo nhánh |
|---|---|---|
| G1 | U01-U04 | Authorization, audit/job/outbox, artifact/XML/checksum và class/enrollment scope |
| G2 | U05-U08 và phần Learning Access của U04 | Content/bank versions, verified entitlement, Learning access và đề thủ công/publication |
| G3 | U09-U13 | Question type, template/copy/simulation, group allocation, AI/Code và attempt/submission |
| G4 | U14-U16 | Composite lineage/chốt, final grade, reporting/notification đúng scope |

Gate tổng kiểm tra toàn bộ phạm vi của wave; nhánh ở wave sau được mở ngay khi provider trực tiếp đạt kiểm tra tương ứng, không phải chờ gate tổng.

## 6. Dependency paths và critical path

- **Truy cập học liệu:** U01 và U02 song song → U04 → U05 và U07 → U04. U04 kiểm enrollment, entitlement và publication; không có learning path hay lesson progress.
- **Bài cá nhân:** U01/U02 → U04/U06 → U08 → U09 → U10 → U11 → U15 → U16. U03 cung cấp artifact; U13 cung cấp AI draft/Code run khi được chọn.
- **Bài nhóm:** U08 → U12, đồng thời U08/U09/U10 → U11; sau đó U11 + U12 → U14 → U15 → U16.
- **AI tạo đề:** U03/U05/U06 → U13 → U08 (contract `C`); U08 có thể soạn thủ công trước khi AI hoàn tất. **AI hỗ trợ chấm:** U11/U14 → U15 gọi U13, rồi giảng viên chốt.
- **Một đường phụ thuộc dài nhất theo các cạnh `H`:** U01 hoặc U02 → U03 → U05 → U08 → U09 → U10 → U11 → U14 → U15 (9 unit). Nhánh U01 hoặc U02 → U04 → U06 → U08 cũng hội vào đường này ở U08. U16 nhận event/read model sau U15 qua cạnh `E`; thêm người không làm các cạnh bắt buộc biến mất.

| Rủi ro | Kiểm soát |
|---|---|
| 16 unit bị hiểu thành 16 deployable/service | Một backend modular monolith; unit chỉ là boundary phát triển và kiểm thử |
| Contract AI tạo vòng phụ thuộc | U13 trả proposal chung; owner U08/U15 lưu kết quả, không cho U13 ghi ngược |
| Phần Learning Access của U04 bị phình thành learning path/progress | Chỉ kiểm quyền, trả nội dung/dashboard shell; không có state hoàn thành bài học |
| Cross-unit data leak | Actor/resource scope ở U01, gọi public contract, negative authorization tests |
| Job/provider lỗi hoặc giao trùng | U02 retry/idempotency; owner xử lý kết quả và safe failure |
