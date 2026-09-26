# U14 Group Document & Submission - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (thành viên, GV)  --REST-->  +------------------ backend --------------------------+
      ^                                   | GroupDocController --> SectionService (P1)          |
      |  SSE (EventSource)                |                    --> GroupSubmitter (P4)          |
      +---------------------------------- | SseHub <-- jobs.realtime.{id} <-- platform.realtime |
                                          | GroupDocEventPublisher --> platform.realtime        |
                                          | GroupChangeAdapter, PublicationLifecycleAdapter     |
                                          | GroupSubmissionQueryService (cho U13, U15, U16)     |
                                          +-----------------------------------------------------+
                                                  | job GROUP_DOC_CREATE, GROUP_AUTO_SUBMIT
                                                  v
                                          worker: GroupDocInitializer; AutoSubmitHandler --> GroupSubmitter
```

**Text alternative**: Thành viên và giảng viên thao tác qua REST tới `GroupDocController`; `SectionService` khóa/lưu/xong mục bằng UPDATE có điều kiện, `GroupSubmitter` tạo bản nộp. Sau mỗi thay đổi, `GroupDocEventPublisher` gửi sự kiện lên fanout `platform.realtime`; mỗi backend nhận qua queue riêng và `SseHub` đẩy tới trình duyệt đang mở tài liệu qua SSE. `GroupDocInitializer` dựng tài liệu khi bài mở; `MembershipListener` nhả khóa và đóng kênh khi thành viên rời nhóm. Worker chạy job tự nộp tại hạn và cũng phát sự kiện qua fanout.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `GroupDocInitializer` | worker | F1; P5 |
| `SectionService` | backend | F3-F6; P1 |
| `GroupSubmitter` | backend, worker | F7; P4 |
| `GroupDocEventPublisher`, `SseHub` | backend, worker (chỉ phát) | P2 |
| `GroupChangeAdapter`, `PublicationLifecycleAdapter` | backend, worker | Cài port của U12, U08: tạo job tạo tài liệu nhóm, tự nộp; nhả khóa khi rời nhóm (P3) |
| `AutoSubmitHandler` | worker | BR-U14-33 |
| `GroupSubmissionQueryService` | backend | F8 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U14_SSE_HEARTBEAT_SECONDS` | 25 |
| `U14_SSE_TIMEOUT_MINUTES` | 30 |
| `U14_CLAIM_IDLE_HINT_HOURS` | 48 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung |
| SECURITY-05 | Compliant | Kiểm block, bình luận |
| SECURITY-08 | Compliant | P3 |
| SECURITY-15 | Compliant | P1, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
