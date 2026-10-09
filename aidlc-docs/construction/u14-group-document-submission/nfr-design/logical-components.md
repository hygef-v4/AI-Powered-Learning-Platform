# U14 Group Document & Submission - Logical Components

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (thành viên; giảng viên chính của lớp)
      |  REST               ^  SSE (chỉ thành viên)
      v                     |
+------------------------------------------------------------------+
| backend                                                          |
| GroupDocController --> SectionService (P1)                       |
|                    --> GroupSubmitter (P4)                       |
|                    --> GroupSubmissionQueryService               |
| GroupDocSseController --> SseHub                                 |
| SseHub <-- jobs.realtime.{instanceId} <-- platform.realtime      |
| GroupDocEventPublisher --> platform.realtime                     |
| AssignmentLifecycleAdapter (U08), GroupChangeAdapter (U12)       |
+------------------------------------------------------------------+
      |  việc GROUP_DOC_CREATE; scanner mỗi phút
      v
+------------------------------------------------------------------+
| worker                                                           |
| GroupDocInitializer                                              |
| GroupAutoSubmitScanner --> GroupSubmitter                        |
+------------------------------------------------------------------+
```

**Text alternative**: Thành viên nhóm và giảng viên chính của lớp thao tác qua REST tới `GroupDocController`; `SectionService` giao/nhận/khóa/lưu/xong/nhả mục trong dòng `group_documents` đang khóa, `GroupSubmitter` tạo bản nộp, `GroupSubmissionQueryService` trả tài liệu của nhóm mình, tiến độ cho giảng viên và bản nộp cho U11, U13, U15, U16. Sau mỗi thay đổi, `GroupDocEventPublisher` gửi sự kiện lên fanout `platform.realtime`; mỗi backend nhận qua queue riêng và `SseHub` đẩy tới trình duyệt của thành viên đang mở tài liệu. `AssignmentLifecycleAdapter` gửi việc dựng tài liệu khi bài mở và đẩy tin ngưng giao; `GroupChangeAdapter` gửi việc dựng tài liệu cho nhóm mới, nhả khóa và đóng kênh khi thành viên rời nhóm. Trên worker, `GroupDocInitializer` dựng tài liệu cho mọi nhóm của lớp, `GroupAutoSubmitScanner` tự nộp khi bài hết hạn hoặc ngưng giao và cũng phát sự kiện qua fanout.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `GroupDocInitializer` | worker | F1; P5 |
| `SectionService` | backend | F4-F8; P1 |
| `GroupSubmitter` | backend, worker | F9; P4 |
| `GroupDocEventPublisher`, `SseHub` | backend, worker (chỉ phát) | P2 |
| `GroupChangeAdapter`, `AssignmentLifecycleAdapter` | backend, worker | Cài port của U12, U08: gửi việc tạo tài liệu nhóm; đẩy tin ngưng giao (F10); nhả khóa khi rời nhóm (P3) |
| `GroupAutoSubmitScanner` | worker | BR-U14-33; P4 |
| `GroupSubmissionQueryService` | backend | F2, F3, F11-F13; cài `GroupSubmissionQueryPort` |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U14_SSE_HEARTBEAT_SECONDS` | 25 |
| `U14_SSE_TIMEOUT_MINUTES` | 30 |
| `U14_AUTO_SUBMIT_GRACE_SECONDS` | 30 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung |
| SECURITY-05 | Compliant | Kiểm và làm sạch block |
| SECURITY-08 | Compliant | P3 |
| SECURITY-15 | Compliant | P1, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
