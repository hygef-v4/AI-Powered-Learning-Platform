# U12 Group & Allocation - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U12. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-GRP-001, US-GRP-002, US-GRP-003. **Use case**: UC 9, UC 15, UC 28.
- **Thay đổi 2026-10-01**: nhóm thuộc lớp, quản lý trong danh sách sinh viên của lớp, có chia ngẫu nhiên, không có dùng lại nhóm; tài liệu nhóm chuyển sang bảng `group_documents` của U14. Plan cần duyệt lại.
- **Thiết kế nguồn**: `construction/u12-group-allocation/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo: `../demo_do_an/docs/ai-dlc/PLAN-bai-tap-nhom.md` (chia ngẫu nhiên, đổi leader).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật |
| U12 cài `GroupReadinessPort` cho U08 | | Thay adapter tạm của U08 |
| U12 cung cấp `GroupMembershipPort` (U14, U16) | cho U14, U16 | Các unit đó dùng khi được code |

### Dữ liệu U12 sở hữu

PostgreSQL `student_groups` (nhóm của lớp), `group_members`, `leader_change_requests`; event `group.leader-changed`, `group.leader-request-rejected`, `group.membership-changed` (chỉ cho thông báo U16); khai báo `GroupChangePort` (U14 cài, adapter rỗng tới khi có U14).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  groups/
    api/                ClassGroupsController, LeaderRequestController, MyGroupController, DTO
    application/        ClassGroupsSaver, LeaderRequestService,
                        GroupReadinessService, MembershipQueryService
    domain/             ClassGroups (gom theo lớp), StudentGroup, GroupMember,
                        LeaderChangeRequest, ClassGroupsValidator, RandomSplitter
    infrastructure/     JPA repository
    port/               GroupMembershipPort, GroupChangePort
/backend/src/main/resources/db/migration/u12/
/frontend/src/app/teaching/classes/[id]/students/   (panel nhóm trong danh sách sinh viên)
/frontend/src/app/learn/classes/[classId]/group/
/contracts/openapi/u12-groups.yaml
/contracts/messages/u12-group-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain và `ClassGroupsValidator` thuần (BR-U12-02, 03, 08, 21, 22); `RandomSplitter` thuần (BR-U12-05).
- [ ] **Bước 2** - `ClassGroupsSaver`: lưu nguyên khối nhóm của lớp (khóa theo lớp + `version` từng nhóm), đóng thay vì xóa, gọi `GroupChangePort` khi lớp có bài nhóm đang mở, audit trong transaction, event sau commit (F1, F5, P1).
- [ ] **Bước 3** - Chia ngẫu nhiên sinh viên chưa có nhóm của lớp (xem trước) (F2, BR-U12-05).
- [ ] **Bước 4** - `LeaderRequestService`: gửi/hủy/duyệt/từ chối, đổi trực tiếp (F6, P3, BR-U12-10…14).
- [ ] **Bước 5** - `GroupReadinessService` (cài `GroupReadinessPort`, thay adapter tạm U08), `MembershipQueryService` (`GroupMembershipPort`) (F4, F7, P4, P5).
- [ ] **Bước 6** - Unit test mọi `BR-U12-xx`, gồm chia 7 người sĩ số 3 → 3/2/2 và giữ nhóm cũ.
- [ ] **Bước 7** - Tóm tắt: `aidlc-docs/construction/u12-group-allocation/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 8** - Flyway `V20260925_1900__u12_groups.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 9** - JPA repository.
- [ ] **Bước 10** - Integration test: hai lần lưu đồng thời → một `409`; hai yêu cầu đổi trưởng nhóm đồng thời → một thành công; `app` không xóa được lịch sử; U08 phát hành bài `GROUP` bị chặn khi chưa sẵn sàng.
- [ ] **Bước 11** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 12** - `/contracts/openapi/u12-groups.yaml` và schema event.
- [ ] **Bước 13** - Controller + DTO + validation.
- [ ] **Bước 14** - Test MockMvc: sinh viên không sửa nhóm, không thấy nhóm khác; giảng viên lớp khác `404`.
- [ ] **Bước 15** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 16** - `ClassStudentsGroupsPanel` trong Class Detail của U04 (`GroupCard`, `UngroupedStudentsPanel`, kéo thả `@dnd-kit`), `RandomSplitDialog`.
- [ ] **Bước 17** - `GroupReadinessPanel` (U08 nhúng khi phát hành bài nhóm), `LeaderRequestsPanel`.
- [ ] **Bước 18** - Sinh viên: `MyGroupCard`, `LeaderChangeRequestDialog`.
- [ ] **Bước 19** - Test frontend: không lưu khi còn lỗi, cảnh báo sinh viên chưa có nhóm.
- [ ] **Bước 20** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 21** - Cập nhật `README.md`: luồng chia nhóm, đổi trưởng nhóm; cách U14 dùng `GroupMembershipPort`.
- [ ] **Bước 22** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRP-001 (UC 9) | 1, 2, 3, 16 |
| US-GRP-002 (UC 9, UC 15) | 4, 17, 18 |
| US-GRP-003 (bài nhóm dùng nhóm của lớp) | 1, 2, 5, 17 |

## 5. Ngoài phạm vi

- Tài liệu nhóm, nhận mục, nộp bài nhóm (U14), chấm (U15), thông báo (U16).
