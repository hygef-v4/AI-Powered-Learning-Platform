# U12 Group & Allocation - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U12. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-GRP-001, US-GRP-002 (đóng góp US-GRP-003).
- **Primary UC hiện hành**: UC 16 View My Group, UC 17 Request Leader Change (bản 73 UC). Luồng phụ: quản lý nhóm, chia ngẫu nhiên, duyệt/từ chối yêu cầu đổi trưởng nhóm trong tab Students của Teacher Class Detail (UC 32); nhóm trên popup Student Detail (UC 32); sẵn sàng phát hành bài nhóm (UC 45). 73 UC không có UC riêng cho các luồng phụ này.
- **Thay đổi 2026-10-01**: nhóm thuộc lớp, quản lý trong danh sách sinh viên của lớp, có chia ngẫu nhiên, không có dùng lại nhóm; tài liệu nhóm chuyển sang bảng `group_documents` của U14.
- **Quyết định 2026-10-09**: giảng viên lớp = R3/R4 (Teacher hoặc Subject Manager là giảng viên chính); sinh viên = R5 (ghi danh `ACTIVE` lớp `OPEN`); Admin không có chức năng nhóm, không vào lớp; chỉ Chủ nhiệm môn thêm/gỡ sinh viên (UC 51), Teacher không; bài nhóm chỉ có ở lớp, phát hành hỏi `GroupReadinessPort` (lớp ≥ 1 nhóm, nhóm hợp lệ có một trưởng nhóm; sinh viên chưa có nhóm là cảnh báo cần xác nhận). Screen flow không có màn nhóm riêng: Nhóm của tôi nằm trong Student Class Detail, quản lý nhóm nằm trong tab Students. UC 17 cho phép không đề cử người thay.
- **Thiết kế nguồn**: `construction/u12-group-allocation/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo: `../demo_do_an/docs/ai-dlc/PLAN-bai-tap-nhom.md` (chia ngẫu nhiên, đổi leader).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `EventPublisherPort` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật (trạng thái lớp, giảng viên chính, `isActiveStudent`, `listActiveStudents`) |
| U12 cung cấp `GroupReadinessService` cho U08 | | U12 code trước U08 trong wave 2; U08 khai báo `GroupReadinessPort` và cắm service này |
| U12 cung cấp `GroupMembershipPort` (U14, U16) | cho U14, U16 | Các unit đó dùng khi được code |
| `GroupChangePort` | U14 (`C`, U12 khai báo) | Adapter rỗng: `onGroupCreated`/`onMemberRemoved` không làm gì, `hasGroupWork` trả `false`; U14 (wave 3) cài |

### Dữ liệu U12 sở hữu

PostgreSQL `student_groups` (nhóm của lớp), `group_members`, `leader_change_requests`; event `group.membership-changed`, `group.leader-requested`, `group.leader-changed`, `group.leader-request-rejected` (chỉ cho thông báo U16); khai báo `GroupChangePort` (U14 cài, adapter rỗng tới khi có U14).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  groups/
    api/                ClassGroupsController, LeaderRequestController, MyGroupController, DTO
    application/        ClassGroupsSaver, LeaderRequestService, MyGroupQueryService,
                        GroupReadinessService, MembershipQueryService
    domain/             ClassGroups (gom theo lớp), StudentGroup, GroupMember,
                        LeaderChangeRequest, ClassGroupsValidator, RandomSplitter
    infrastructure/     JPA repository, NoopGroupChangeAdapter
    port/               GroupMembershipPort, GroupChangePort
/backend/src/main/resources/db/migration/groups/
/frontend/src/shared/groups/            ClassStudentsGroupsPanel, RandomSplitDialog, LeaderRequestsPanel,
                                         GroupReadinessPanel, MyGroupPanel, LeaderChangeRequestDialog
/contracts/openapi/groups.yaml
/contracts/messages/group-events.json
```

Component frontend được gắn vào route của unit sở hữu màn: `app/classes/[id]/teaching/` (tab Students, U04), `app/classes/[id]/teaching/assignments/[assignmentId]/` (hộp phát hành, U08), `app/classes/[id]/` (Student Class Detail, U04). U12 không có route riêng.

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain và `ClassGroupsValidator` thuần (BR-U12-02, 03, 08, 09, 14, 15, 21, 22); `RandomSplitter` thuần (BR-U12-05).
- [ ] **Bước 2** - `ClassGroupsSaver`: kiểm R3/R4 và lớp `DRAFT`/`OPEN`, lưu nguyên khối nhóm của lớp (khóa theo lớp + `version` từng nhóm), gỡ thành viên (cả người đã rời lớp) xóa dòng `group_members`, chỉ xóa nhóm khi `GroupChangePort.hasGroupWork` = false, gọi `GroupChangePort`, audit trong transaction, event sau commit (F1, F5, F8, P1).
- [ ] **Bước 3** - Chia ngẫu nhiên sinh viên chưa có nhóm của lớp (xem trước) (F2, BR-U12-05).
- [ ] **Bước 4** - `LeaderRequestService`: gửi (đề cử tùy chọn)/hủy/duyệt (bắt buộc chọn trưởng nhóm khi không có đề cử)/từ chối (lý do bắt buộc), đổi trực tiếp tự hủy yêu cầu đang chờ; event `group.leader-requested`, `group.leader-changed`, `group.leader-request-rejected` sau commit (F6, P3, BR-U12-10…15, 31).
- [ ] **Bước 5** - `GroupReadinessService` (U08 code sau sẽ cắm vào `GroupReadinessPort`), `MembershipQueryService` (`GroupMembershipPort`, chỉ thành viên hiệu lực), `MyGroupQueryService` (UC 16) (F4, F7, F8, P4, P5, P6).
- [ ] **Bước 6** - Unit test mọi `BR-U12-xx`, gồm chia 7 người sĩ số 3 → 3/2/2 và giữ nhóm cũ; yêu cầu không đề cử; thành viên đã rời lớp; lớp `ARCHIVED` chỉ xem.
- [ ] **Bước 7** - Tóm tắt: `aidlc-docs/construction/u12-group-allocation/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 8** - Flyway `db/migration/groups/V20260925_1450__create_groups.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 9** - JPA repository.
- [ ] **Bước 10** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: hai lần lưu đồng thời → một `409`; hai yêu cầu đổi trưởng nhóm đồng thời → một thành công; `app` không xóa được `leader_change_requests`; nhóm có `hasGroupWork` = true (adapter giả) không xóa được; `GroupReadinessService` trả lỗi khi lớp chưa có nhóm hoặc nhóm thiếu trưởng nhóm và cảnh báo khi còn sinh viên chưa có nhóm (U08 chặn phát hành kiểm ở plan U08); sinh viên bị gỡ khỏi lớp không còn trong `GroupMembershipPort`.
- [ ] **Bước 11** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 12** - `/contracts/openapi/groups.yaml` và schema event; cần sửa theo mục 7.
- [ ] **Bước 13** - Controller + DTO + validation.
- [ ] **Bước 14** - Test MockMvc: sinh viên không sửa nhóm, không thấy nhóm khác; giảng viên lớp khác, Admin, Chủ nhiệm môn không dạy lớp → `404`; sinh viên chưa có nhóm nhận `group` rỗng.
- [ ] **Bước 15** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 16** - `ClassStudentsGroupsPanel` gắn vào tab Students của Teacher Class Detail (U04) (`GroupCard`, `UngroupedStudentsPanel`, kéo thả `@dnd-kit`), `RandomSplitDialog`; cấp dữ liệu nhóm cho popup Student Detail của U04.
- [ ] **Bước 17** - `GroupReadinessPanel` (U08 code sau nhúng vào `PublishDialog` của bài nhóm), `LeaderRequestsPanel` trong tab Students.
- [ ] **Bước 18** - Sinh viên: `MyGroupPanel` gắn vào Student Class Detail (U04) (`MyGroupCard`, `LeaderRequestStatus`), `LeaderChangeRequestDialog`.
- [ ] **Bước 19** - Test frontend: không lưu khi còn lỗi, cảnh báo sinh viên chưa có nhóm, đề cử tùy chọn, nút yêu cầu khóa khi nhóm có yêu cầu chờ.
- [ ] **Bước 20** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 21** - Cập nhật `README.md`: luồng chia nhóm, đổi trưởng nhóm; cách U14 dùng `GroupMembershipPort` và cài `GroupChangePort`.
- [ ] **Bước 22** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| UC 16 View My Group (US-GRP-002) | 5, 13, 18 |
| UC 17 Request Leader Change (US-GRP-002) | 4, 13, 18 |
| US-GRP-001 (luồng phụ UC 32: tab Students, Student Detail) | 1, 2, 3, 16 |
| US-GRP-002 S1, S2 (duyệt/từ chối trong tab Students, UC 32) | 4, 17 |
| US-GRP-003, UC 45 (bài nhóm dùng nhóm của lớp, sẵn sàng phát hành) | 1, 2, 5, 17 |
| UC 51 (Chủ nhiệm môn gỡ sinh viên, ảnh hưởng nhóm) | 1, 2, 5 |

## 5. Ngoài phạm vi

- Tài liệu nhóm, nhận mục, nộp bài nhóm (U14), chấm (U15), thông báo (U16), thêm/gỡ sinh viên (U04).

## 6. Revision implementation scope - 2026-10-08
- [ ] My Group UC 16/Request Leader Change UC 17 (đánh số theo bản 73 UC): Student own membership; duyệt/chia nhóm support chỉ giảng viên lớp R3/R4 (Teacher hoặc Subject Manager là giảng viên chính); Admin không có quyền nhóm.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] `contracts/openapi/groups.yaml`: bỏ `ADMIN` khỏi `x-roles` của mọi API giảng viên (chỉ `TEACHER`, `SUBJECT_MANAGER`; R3/R4 kiểm ở server).
- [ ] `requestLeaderChange`: `nomineeId` không bắt buộc; schema `LeaderRequest.nomineeId` không bắt buộc.
- [ ] `approveLeaderRequest`: `newLeaderId` bắt buộc khi yêu cầu không có đề cử (`422`); mô tả kiểm thành viên hiệu lực.
- [ ] `previewRandomSplit`: `maxSize` thêm `maximum: 20`.
- [ ] `saveClassGroups`, `previewRandomSplit`: `422` khi lớp `ARCHIVED`.
- [ ] `getMyGroup`: `group` cho phép `null` (chưa có nhóm); thêm `myLeaderRequest` (yêu cầu gần nhất của mình: id, trạng thái, `decisionReason`) và `hasPendingRequest`.
- [ ] Schema `Member` thêm `activeEnrollment` (BR-U12-15); `GET /classes/{classId}/groups` dùng cho popup Student Detail (không thêm API).
- [ ] Code: `GroupMembershipPort.leaderOf` trả `Optional<UUID>` (nhóm có trưởng nhóm đã rời lớp); `GroupReadinessService` thêm mã `GROUP_WITHOUT_MEMBER`; thêm `MyGroupQueryService`, `MyGroupController`.
- [ ] Migration: `db/migration/groups/V20260925_1450__create_groups.sql` (thay tên `V20260925_1450__u12_groups.sql`): `leader_change_requests.nominee_id` cho phép NULL, index `(requester_id, created_at)`, index `group_members (account_id)`.
- [ ] Frontend: bỏ route `app/teaching/classes/[id]/students/` và `app/learning/classes/[classId]/group`; component gắn vào tab Students và Student Class Detail của U04, hộp phát hành của U08.
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U08 `PublishDialog` nhúng `GroupReadinessPanel` và gửi xác nhận cảnh báo khi phát hành bài nhóm; U14 cung cấp `GroupDocsOfGroupList` và `GET /groups/{groupId}/group-docs` cho phần Nhóm của tôi; U04 `StudentClassPage` có chỗ gắn `MyGroupPanel`, `StudentsTab` có chỗ gắn `ClassStudentsGroupsPanel`, popup Student Detail đọc nhóm từ dữ liệu U12; U16 deep link thông báo `group.leader-requested` tới tab Students, `group.leader-changed`/`group.leader-request-rejected` tới Student Class Detail.

### Cần chốt

| Vấn đề | Lựa chọn | Khuyến nghị (đang áp dụng) |
|---|---|---|
| 73 UC không có UC cho giảng viên quản lý nhóm và duyệt yêu cầu đổi trưởng nhóm | (A) giữ làm luồng phụ của UC 32 (tab Students); (B) thêm UC riêng vào bảng UC | (A) |
| Chủ nhiệm môn gỡ sinh viên (UC 51) đang ở trong nhóm | (A) U12 xử lý khi đọc (BR-U12-15), giảng viên dọn ở lần lưu sau; (B) U04 gọi port/event để U12 tự gỡ thành viên và báo U14 ngay | (A), không thêm phụ thuộc U04 → U12 |
| Tài liệu nhóm ở Nhóm của tôi | (A) mở Assignment Detail rồi Group Essay Workspace theo screen flow; (B) mở thẳng Group Essay Workspace | (A) |
