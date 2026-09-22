# Component Dependencies and Data Flow

## 1. Dependency principles

- Dependency đi từ adapter/controller vào application/domain, rồi ra port; domain không phụ thuộc SDK provider.
- Cross-module write phải qua public application service hoặc event contract.
- Database có thể cùng instance ở modular monolith nhưng schema/table ownership theo module.
- Worker dùng cùng module contracts và không vượt authorization/context scope của job nguồn.

## 2. Dependency matrix

| Consumer | Identity/Auth | Academic | Group | Content | Assessment | Submission | Grading | AI | File | Job | Audit |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Learning | R | R | - | R | - | - | - | - | - | - | W |
| Group | R | R | O | - | R | - | - | - | - | - | W |
| Content | R | R | - | O | - | - | - | W | W | W | W |
| Assessment | R | R | R | R | O | - | - | W | R | W | W |
| Submission | R | R | R | - | R | O | - | - | W | - | W |
| Grading | R | R | R | - | R | R | O | W | R | W | W |
| Reporting | R | R | R | - | R | R | R | - | R | W | W |
| Payment | R | R | - | - | - | - | - | - | - | W | W |
| Notification | R | R | R | - | R | R | R | - | - | W | W |

`O`: owner; `R`: read/use public contract; `W`: invokes/writes through public contract; `-`: không phụ thuộc trực tiếp.

## 3. Sơ đồ dependency

```mermaid
flowchart LR
    Web["Next.js Web"] --> Api["Spring Boot REST API"]
    Api --> Auth["Identity and Authorization"]
    Api --> Core["Domain Modules"]
    Core --> Db["Relational Database"]
    Core --> FilePort["File Artifact Port"]
    Core --> Jobs["Job Platform"]
    Jobs --> Workers["Workers"]
    Workers --> AiPort["AI Provider Port"]
    Workers --> CodePort["Code Sandbox Port"]
    Workers --> NotifyPort["Notification Port"]
    Workers --> FilePort
    Core --> PayPort["Payment Gateway Port"]
    Core --> Audit["Append Only Audit"]
```

### Text alternative

Next.js gọi REST API. API xác thực và chuyển vào domain modules. Domain lưu transactional data trong relational database, dùng artifact port cho file và job platform cho tác vụ dài. Worker gọi AI, code sandbox, notification và artifact ports. Payment dùng gateway port riêng. Mọi module phát audit event bất biến.

## 4. Data ownership

| Data | Owner | Tham chiếu bởi |
|---|---|---|
| User, role, scope, session | Identity & Access | Tất cả module qua actor/resource contract |
| Subject, class, enrollment | Academic | Group, Content, Learning, Assessment, Reporting |
| Group, leader, allocation | Group | Submission, Grading, Reporting |
| Material/content/source/transcript version | Content | Learning, AI, Assessment |
| Assessment/template/copy/publication/simulation policy | Assessment | Submission, Grading, Reporting |
| Attempt snapshot, draft/submission/composite/artifact refs | Submission | Grading, Reporting |
| Grade/proposal/publication state | Grading | Learning, Reporting, Notification |
| Object bytes/checksum/scan/derivation | File & Artifact | Content, Submission, AI, Reporting |
| Payment/event/entitlement | Payment | Academic/access checks qua entitlement contract |
| Audit events | Audit | Admin query only |

## 5. Trust boundaries

- Browser, upload content, webhook và mọi provider response là untrusted input.
- API authorization chạy trước load/return resource nhạy cảm.
- Worker payload chỉ chứa ID/reference; worker tải dữ liệu qua scoped service.
- Signed artifact access có TTL, purpose và actor binding khi khả thi.
- Full Draw.io XML không được gửi thẳng sang AI; derived artifact được tạo trong trusted worker sau validation.

## 6. Change-specific communication contracts

- Content → Job: `YOUTUBE_TRANSCRIPT_INGEST` chỉ mang source/version reference đã được authorize; worker trả transcript artifact và timestamp metadata qua Content service contract.
- Assessment → Submission: `AttemptSnapshot` đóng băng assignment, question/rubric component versions và simulation policy khi attempt bắt đầu.
- Copy operation chỉ đọc source version rồi tạo stable identity mới ở lớp đích; không có event đồng bộ ngược hoặc xuôi.
- Submission → Job: `GROUP_COMPOSITE_GENERATE` mang danh sách part-version bất biến có thứ tự; kết quả là derived composite artifact/version.
- Submission → Grading: composite evidence và individual-part evidence là read-only. Grading lưu kết quả tách biệt và điểm cuối từng thành viên do giảng viên nhập.
