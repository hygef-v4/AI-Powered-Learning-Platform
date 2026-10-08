# U08 Assessment Core & Publication - Deployment Architecture

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U08] --> [postgres: assignments, assignment_questions]
                                                              ^
                                         scanner mỗi phút ----+----- [worker: U08]
                                                                              |
                                           port onOpened/onRetired --> U11, U14; event assignment.opened --> U16
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U08 trong backend; U08 lưu bài (gồm lịch) và câu của bài vào PostgreSQL. Scanner trong worker đổi trạng thái theo lịch mỗi phút; trong cùng transaction gọi `AssignmentLifecyclePort` để U11, U14 ghi dòng hoặc gửi việc của mình (tạo tài liệu nhóm, tự nộp), sau commit phát event `assignment.opened` cho U16 gửi thông báo.

U08 không thêm container hay volume.
