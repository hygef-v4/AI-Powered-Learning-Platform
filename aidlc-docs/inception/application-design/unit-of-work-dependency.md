# Unit of Work Dependencies

## 1. Ký hiệu

| Ký hiệu | Ý nghĩa |
|---|---|
| H | Hard dependency: cần public contract/behavior ổn định trước integration gate của consumer |
| C | Contract dependency: có thể phát triển song song bằng versioned schema/fake adapter, nhưng phải compatibility-test trước khi đóng wave |
| E | Event/read-model dependency: consumer nhận event hoặc dữ liệu projection, không ghi trực tiếp vào owner |
| - | Không có dependency trực tiếp |

Dependency thể hiện quyền sử dụng public contract, không cho phép truy cập repository hoặc schema/table của owner.

## 2. Dependency matrix

Hàng là consumer, cột là provider.

| Consumer | U01 | U02 | U03 | U04 | U05 | U06 | U07 | U08 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| U01 Foundation | - | - | - | - | - | - | - | - |
| U02 Academic | H | - | - | - | - | - | - | - |
| U03 Content/Learning/Banks | H | H | - | - | - | - | - | C |
| U04 Assessment | H | H | H | - | - | - | - | - |
| U05 Submission/Group | H | H | - | H | - | - | - | - |
| U06 Grading/AI/Code | H | H | H | H | H | - | - | - |
| U07 Reporting/Notification | H | H | E | E | E | E | - | E |
| U08 Payment/Entitlement | H | H | - | - | - | - | - | - |

U03 và U08 dùng entitlement contract theo hướng dependency inversion: contract nằm trong shared contracts/Foundation, U08 cung cấp implementation; U03 chỉ tích hợp implementation ở G2. U08 không được gọi ngược vào U03, nhờ đó không tạo cycle.

## 3. Direct dependency contracts

### U02 phụ thuộc U01

- Actor/session context và role/scope authorization.
- Audit append contract cho thay đổi môn, lớp, phân công và enrollment.
- Idempotency cho command quan trọng.

### U03 phụ thuộc U01, U02 và contract U08

- U02 cung cấp subject/class/enrollment/member checks.
- U01 cung cấp file/artifact, job, audit và authorization.
- Entitlement port được định nghĩa trung lập; U08 cung cấp implementation khi payment được bật.

### U04 phụ thuộc U01, U02 và U03

- U02 cung cấp author scope và tập lớp thuộc môn.
- U03 cung cấp immutable rubric/question/content version references.
- U01 cung cấp job/audit/artifact primitives; U04 không gọi AI provider trực tiếp.

### U05 phụ thuộc U01, U02 và U04

- U04 cung cấp publication/version/schedule/attempt policy.
- U02 cung cấp enrollment và class membership.
- U01 cung cấp artifact, idempotency, authorization và audit.
- U05 sở hữu submission; U04 không ghi vào submission tables.

### U06 phụ thuộc U01 đến U05 theo public contract

- Đọc immutable submission version từ U05.
- Đọc publication/rubric/content references từ U04/U03.
- Kiểm tra instructor/class/subject scope qua U02/U01.
- Gửi job tới worker qua versioned job contract; không đặt full Draw.io XML trong payload.
- Trả grade event; không cho U05 hoặc worker ghi final grade trực tiếp.

### U07 phụ thuộc event/read model

- Nhận event từ U02-U06 và U08 qua outbox/job platform.
- Tạo projection riêng và rebuild được; không sửa transaction nguồn.
- Notification failure không rollback assessment, submission, grading hoặc payment.

### U08 phụ thuộc U01 và U02

- U01 cung cấp identity, audit, idempotency và job primitives.
- U02 cung cấp learner/product/class reference khi cần.
- U08 phát entitlement event; không ghi enrollment trực tiếp.

## 4. Worker dependencies

Worker là deployable riêng nhưng không phải bounded context riêng. Mỗi handler có owner nghiệp vụ:

| Handler | Owner | Input tối thiểu | Output |
|---|---|---|---|
| RAG ingestion | U03 | Material version ID, scope reference | Index status/reference |
| AI authoring | U06 | Generation request ID | Draft proposal reference |
| AI grading | U06 | Grading request ID | Grade proposal reference |
| Compact Draw.io | U06 | AI job ID, full artifact reference | Derived artifact reference |
| Code execution | U06 | Run ID, source artifact reference | Immutable execution result |
| Report export | U07 | Export request ID | Scoped artifact reference |
| Notification delivery | U07 | Notification delivery ID | Delivery status |
| Payment reconciliation | U08 | Reconciliation period/job ID | Reconciliation result |

Worker gọi scoped backend service/port hoặc storage adapter theo machine identity có least privilege. Worker không truy cập toàn bộ database bằng một quyền dùng chung và không được cập nhật final grade.

## 5. Dependency waves và critical path

```text
Wave 0: U01
Wave 1: U02
Wave 2: U03 || U08
Wave 3: U04
Wave 4: U05
Wave 5: U06
Wave 6: U07
```

`||` biểu thị có thể phát triển song song sau khi contract được chốt.

Critical path của hành trình học và đánh giá:

```text
U01 -> U02 -> U03 -> U04 -> U05 -> U06 -> U07
```

U08 nằm ngoài critical path học/chấm cơ bản và có thể phát triển song song với U03, nhưng integration gate G2 phải kiểm tra entitlement contract trước khi bật paywall.

## 6. Integration gate theo dependency

| Gate | Producer cần ổn định | Consumer được mở | Kiểm thử bắt buộc |
|---|---|---|---|
| G0 | U01 | U02 | Auth, object authorization, audit, artifact và job contract tests |
| G1 | U02 | U03, U08 | Subject/class/enrollment integration và negative authorization tests |
| G2 | U03, U08 | U04 | Content/rubric version references, RAG job và entitlement compatibility tests |
| G3 | U04 | U05 | Publication/schedule/attempt-policy contract tests |
| G4 | U05 | U06 | Immutable submission, leader-only DOCX và full Draw.io XML tests |
| G5 | U06 | U07 | Grade event, AI proposal/final-grade separation và worker retry/idempotency tests |
| G6 | U07 | System checkpoint | Authorized reporting, notification isolation và MVP journey tests |

## 7. Cycle-prevention rules

- U01 không phụ thuộc unit nghiệp vụ.
- U02 không phụ thuộc Content, Assessment, Submission hoặc Payment implementation.
- U03 không gọi U08 implementation trực tiếp; dùng entitlement port.
- U04 không cập nhật Submission; U05 không cập nhật Assessment version.
- U05 không cập nhật Grade; U06 chỉ tham chiếu immutable Submission.
- U07 chỉ tiêu thụ event/read contract và không trở thành transaction coordinator.
- Backend và worker chia sẻ schema contract, không chia sẻ repository implementation.

## 8. Rủi ro dependency và kiểm soát

| Rủi ro | Kiểm soát |
|---|---|
| Shared Foundation thành god module | Chỉ giữ primitive/contract dùng chung; business policy ở unit owner |
| Contract backend-worker lệch phiên bản | Schema version, compatibility test và hỗ trợ rolling upgrade |
| Event giao trùng | Idempotency key và consumer inbox/deduplication |
| Reporting phụ thuộc transaction schema | Dùng versioned event/read API và projection riêng |
| Payment tạo cycle với Academic/Learning | Entitlement port trong shared contract; U08 là provider, không ghi enrollment |
| AI vượt quyền dữ liệu | Job dùng scoped references; backend authorize trước khi worker tải dữ liệu |
| Lỗi provider lan sang API | Timeout, retry hữu hạn, circuit breaker và worker isolation |
