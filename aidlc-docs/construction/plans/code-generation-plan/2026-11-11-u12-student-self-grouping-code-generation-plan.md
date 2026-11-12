# U12 — Sinh viên tự chia nhóm — kế hoạch Code Generation

Kế hoạch này là nguồn duy nhất cho phần sinh mã. Thiết kế: [domain-entities](../../u12-group-assignment/functional-design/domain-entities.md), [business-rules](../../u12-group-assignment/functional-design/business-rules.md) (BR-U12-70…96), [business-logic-model](../../u12-group-assignment/functional-design/business-logic-model.md) (F16–F24), [frontend-components](../../u12-group-assignment/functional-design/frontend-components.md). Quy ước: [code-conventions](../../code-conventions.md), [persistence-conventions](../../persistence-conventions.md).

## Bối cảnh unit

- **Unit**: U12 Group Assignment, brownfield. Workspace root `D:\Github\AI-Powered-Learning-Platform`; backend `backend/src/main/java/edu/aiplatform/groups`, frontend `frontend/src/features/groups`.
- **Story**: US-GRP-007 (cài đặt đợt), US-GRP-008 (tạo, mời, chấp nhận), US-GRP-009 (gửi duyệt), US-GRP-010 (duyệt, dọn khi lưu bộ nhóm).
- **Phụ thuộc**: U01 `IAccountLookup`, `IAuthorization` (qua `GroupAccess`); U02 `IAudit`; U03 `IEventPublisher`, `EventTypes`; U05 `IClassAccess`; U14 `notifications` (người nhận, loại thông báo).
- **Dữ liệu U12 sở hữu thêm**: `group_formation_settings`, `group_proposals`, `group_proposal_members`.
- **Không đổi**: endpoint, JSON và hành vi hiện có; chỉ thêm trường `reason` vào payload `group.membership-changed` khi duyệt (U14 bỏ qua lý do đó).
- **Kỹ thuật**: Spring JDBC (`NamedParameterJdbcTemplate`/`JdbcClient` như `GroupRepository`), Flyway, JUnit 5 + Mockito, Testcontainers PostgreSQL theo `GroupRepositoriesIntegrationTest`; Next.js + Vitest + Testing Library.

## Điều kiện trước khi sinh mã

Thư mục làm việc hiện có thay đổi chưa commit của phiên khác. Mã được viết trên nhánh `feat/student-self-grouping` tạo bằng `--no-track` từ `origin/staging`, theo cách người dùng chọn khi duyệt kế hoạch này.

## Các bước

### Cơ sở dữ liệu và repository

- [x] **Step 1** — Migration `backend/src/main/resources/db/migration/groups/V20261111_1800__group_self_formation.sql`: ba bảng, check constraint trạng thái/sĩ số/độ dài, partial unique index `(class_id, account_id) where active`, index đọc theo lớp/trạng thái, grant cho role `app` như migration nhóm cũ. (BR-U12-70, 74–79; domain-entities)
- [x] **Step 2** — Model `groups/model/`: `GroupFormationSettings` (kèm `isOpen(classStatus, now)`, `closedReason`), `GroupProposal` (enum `Status`, `EndReason`), `ProposalMember` (enum `Status`); quy tắc thuần trong `ProposalRules` (tên, sĩ số, số chỗ còn, kiểm chuyển trạng thái). (BR-U12-70…81)
- [x] **Step 3** — Repository `groups/repository/GroupProposalRepository.java`: đọc/ghi cài đặt kèm CAS `version`; tạo, khóa (`FOR UPDATE`), cập nhật đề xuất kèm `version`; thêm/cập nhật dòng thành viên; truy vấn đề xuất đang hoạt động theo lớp/sinh viên, lời mời theo sinh viên, đề xuất bị từ chối gần nhất; chuyển hàng loạt `INVITED`→`EXPIRED`/`REVOKED`, `ACCEPTED`→`LEFT`; dịch vi phạm unique index thành lỗi nghiệp vụ.
- [x] **Step 4** — Test: `ProposalRulesTest`, `GroupFormationSettingsTest` (unit); `GroupProposalRepositoryIntegrationTest` (PostgreSQL thật: migration chạy, partial unique index chặn hai đề xuất đang hoạt động, mời lại cập nhật dòng cũ, CAS `version`).

### Service

- [x] **Step 5** — `groups/service/GroupFormationService.java`: F16 đọc/lưu cài đặt, kiểm quyền và giá trị, audit `GROUP_FORMATION_UPDATED`, hết hạn lời mời khi mở lại. (US-GRP-007)
- [x] **Step 6** — `groups/service/GroupProposalService.java`: F17–F20 cho sinh viên: đọc màn, tạo, đổi tên, mời, thu hồi, chấp nhận, từ chối, rời, hủy, gửi duyệt, rút về; dọn lười (BR-U12-72, 89); sự kiện `proposal-invited`, `proposal-submitted`; audit gửi duyệt. (US-GRP-008, 009)
- [x] **Step 7** — `groups/service/GroupProposalReviewService.java`: F21 duyệt một/nhiều, F22 từ chối, F24 danh sách cho giảng viên. Tách lõi kiểm và `apply` của `ClassGroupService.save` thành method dùng chung (không đổi hành vi `save`), thêm tham số lý do cho sự kiện membership. (US-GRP-010)
- [x] **Step 8** — Sửa `ClassGroupService.save`: sau `apply`, gọi dọn đề xuất cho người vừa có nhóm (F23, BR-U12-88) trong cùng transaction.
- [x] **Step 9** — `jobs/contract/EventTypes.java`: 4 hằng `GROUP_PROPOSAL_INVITED`, `_SUBMITTED`, `_APPROVED`, `_REJECTED`. `notifications`: `NotificationType` 4 loại, ánh xạ trong `NotificationEventService`, `RecipientResolver` (người nhận từ payload, link tới trang Nhóm của lớp), tiêu đề và tóm tắt trong `NotificationSummaries`; `GROUP_MEMBERSHIP_CHANGED` có `reason = PROPOSAL_APPROVED` thì không tạo thông báo. Không gửi email cho loại mới.
- [x] **Step 10** — Test service (Mockito, theo mẫu `ClassGroupServiceTest`, `LeaderRequestServiceTest`): `GroupFormationServiceTest`, `GroupProposalServiceTest`, `GroupProposalReviewServiceTest` phủ các scenario của US-GRP-007…010 và mã lỗi; bổ sung `ClassGroupServiceTest` cho dọn đề xuất; test notifications cho 4 loại mới và bỏ qua membership khi duyệt.

### API

- [x] **Step 11** — `groups/controller/GroupProposalController.java`: các endpoint ở domain-entities (API mới); record request có kiểm đầu vào (BR-U12-93); mã lỗi mới trong `GroupExceptionHandler` nếu cần định dạng riêng (lỗi duyệt nhiều trả trong body kết quả, không ném).
- [x] **Step 12** — Test controller theo mẫu `GroupControllersTest`: định tuyến, quyền `404`, kiểm đầu vào `400`, xung đột `409`.
- [x] **Step 13** — Integration test PostgreSQL `GroupProposalFlowIntegrationTest`: luồng đầy đủ bật đợt → tạo → mời → chấp nhận → gửi → duyệt thành nhóm chính thức (có tài liệu cho bài nhóm đang mở nếu fixture có), duyệt nhiều có một đề xuất lỗi, lưu bộ nhóm dọn đề xuất, hai chấp nhận đồng thời.

### Frontend

- [x] **Step 14** — `features/groups/model/types.ts` thêm kiểu (frontend-components "Hợp đồng dữ liệu"); `features/groups/api/index.ts` thêm hàm cho mỗi API; `model/proposals.ts` cho tính toán hiển thị (số chỗ còn, nút được phép, nhãn trạng thái).
- [x] **Step 15** — Sinh viên: component mới `GroupProposalSection`, `FormationStatusBanner`, `InvitationList`, `ProposalCard`, `ProposalNameDialog` (dùng cho tạo và đổi tên), `InviteMembersDialog`; sửa `MyGroup.tsx` để hiện `GroupProposalSection` khi chưa có nhóm, và thẻ Nhóm của tôi hiện trạng thái đợt/lời mời. `data-testid` dạng `{component}-{element-role}`.
- [x] **Step 16** — Giảng viên: `SelfGroupingSettingsDialog`, `ProposalReviewList`, `RejectProposalDialog`, `UngroupedStudentsList`; sửa `ClassGroupsPanel.tsx` thêm nút "Tự chia nhóm", nhãn trạng thái đợt, đặt các khu mới; tải lại bộ nhóm sau khi duyệt.
- [x] **Step 17** — Test Vitest: `tests/features/groups/model/proposals.test.ts`; `components/GroupProposalSection.test.tsx` (các trạng thái màn, mời, chấp nhận, gửi, `409`); `components/SelfGroupingSettingsDialog.test.tsx` (kiểm hợp lệ); `components/ProposalReviewList.test.tsx` (duyệt nhiều, lỗi từng thẻ, từ chối); cập nhật `MyGroup.test.tsx`, `ClassGroupsPanel.test.tsx`. Nhãn audit mới trong `features/audit/model/labels.ts`.

### Tài liệu

- [x] **Step 18** — `aidlc-docs/construction/u12-group-assignment/code/code-summary.md`: mục tự chia nhóm (tệp tạo/sửa, API, migration, test); `inception/reverse-engineering/api-inventory.md`: các mapping mới; `current-code-baseline.md`: tệp mới.
- [x] **Step 19** — Kiểm trùng tệp (không có bản `_new`/`_modified`), compile backend và `tsc`/`eslint` frontend trên phần đã sửa; kết quả đầy đủ để Build and Test.

Không có artifact triển khai mới: migration chạy qua Flyway trong pipeline hiện có, không đổi Docker/Compose/biến môi trường.

## Truy vết story

| Story | Bước |
|---|---|
| US-GRP-007 | 1, 2, 3, 5, 10, 11, 16, 17 |
| US-GRP-008 | 1, 2, 3, 6, 9, 10, 11, 13, 14, 15, 17 |
| US-GRP-009 | 2, 3, 6, 9, 10, 11, 13, 15, 17 |
| US-GRP-010 | 3, 7, 8, 9, 10, 11, 13, 16, 17 |

- [x] US-GRP-007
- [x] US-GRP-008
- [x] US-GRP-009
- [x] US-GRP-010

## Phạm vi ước tính

19 bước; khoảng 12–14 tệp backend mới, 6 tệp backend sửa, 1 migration, 10 tệp frontend mới, 4 tệp frontend sửa, 9–11 tệp test mới.
