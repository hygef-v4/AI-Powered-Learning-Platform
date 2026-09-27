# U12 Group & Allocation - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U12. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-GRP-001, US-GRP-002, US-GRP-003. **Use case**: UC-GRP-01..05, UC-ASM-06.
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
| `AssignmentQueryPort` | U08 | Dùng thật |
| U12 cài `GroupReadinessPort` cho U08 | | Thay adapter tạm của U08 |
| U12 cung cấp `GroupMembershipPort` (U14, U16), `GroupDocumentStorePort` (U14) | cho U14, U16 | Các unit đó dùng khi được code |

### Dữ liệu U12 sở hữu

PostgreSQL `student_groups` (U14 thêm cột tài liệu nhóm), `group_members`, `leader_change_requests`; event `group.leader-changed`, `group.membership-changed` (chỉ cho thông báo U16); khai báo `GroupChangePort` (U14 cài, adapter rỗng tới khi có U14).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  groups/
    api/                GroupSetController, LeaderRequestController, MyGroupController, DTO
    application/        GroupSetSaver, GroupSetCopier, LeaderRequestService,
                        GroupReadinessService, MembershipQueryService
    domain/             GroupSet (gom theo bài), StudentGroup, GroupMember,
                        LeaderChangeRequest, GroupSetValidator, RandomSplitter
    infrastructure/     JPA repository
    port/               GroupMembershipPort, GroupDocumentStorePort
/backend/src/main/resources/db/migration/u12/
/frontend/src/app/teaching/assignments/[id]/groups/
/frontend/src/app/learn/assignments/[publicationId]/group/
/contracts/openapi/u12-groups.yaml
/contracts/messages/u12-group-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain và `GroupSetValidator` thuần (BR-U12-02, 03, 21, 22); `RandomSplitter` thuần (BR-U12-05).
- [ ] **Bước 2** - `GroupSetSaver`: lưu nguyên khối (khóa theo bài + `version` từng nhóm), đóng thay vì xóa, audit trong transaction, event sau commit (F1, F3, F5, P1).
- [ ] **Bước 3** - Chia ngẫu nhiên (xem trước) và `GroupSetCopier` (F2, BR-U12-06).
- [ ] **Bước 4** - `LeaderRequestService`: gửi/hủy/duyệt/từ chối, đổi trực tiếp (F6, P3, BR-U12-10…14).
- [ ] **Bước 5** - `GroupReadinessService` (cài `GroupReadinessPort`, thay adapter tạm U08), `MembershipQueryService` (`GroupMembershipPort`) và `GroupDocumentStorePort` (đọc/ghi cột tài liệu nhóm do U14 thêm, khóa lạc quan `doc_version`) (F4, F7, P4, P5).
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
- [ ] **Bước 14** - Test MockMvc: người học không sửa nhóm, không thấy nhóm khác; giảng viên lớp khác `404`.
- [ ] **Bước 15** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 16** - `GroupSetPage` (`GroupCard`, `UngroupedLearnersPanel`, kéo thả `@dnd-kit`), `RandomSplitDialog`, `ReuseGroupsDialog`.
- [ ] **Bước 17** - `ReadinessPanel` (lỗi, cảnh báo người chưa có nhóm), `LeaderRequestsPanel`.
- [ ] **Bước 18** - Người học: `MyGroupCard`, `LeaderChangeRequestDialog`.
- [ ] **Bước 19** - Test frontend: không lưu khi còn lỗi, cảnh báo người học chưa có nhóm.
- [ ] **Bước 20** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 21** - Cập nhật `README.md`: luồng chia nhóm, đổi trưởng nhóm; cách U14 dùng `GroupMembershipPort`.
- [ ] **Bước 22** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRP-001 (UC-GRP-01..03) | 1, 2, 3, 16 |
| US-GRP-002 (UC-GRP-04, 05) | 4, 17, 18 |
| US-GRP-003 (bộ nhóm của bài nhóm) | 1, 2, 5, 17 |

## 5. Ngoài phạm vi

- Tài liệu nhóm, nhận mục, nộp bài nhóm (U14), chấm (U15), thông báo (U16).
