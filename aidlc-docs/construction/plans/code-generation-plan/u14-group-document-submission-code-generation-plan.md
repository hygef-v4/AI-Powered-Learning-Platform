# U14 Group Document & Submission - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U14. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-GRP-004, US-GRP-005; dựng tài liệu nhóm cho US-GRP-003 (UC 45, U09 chủ trì); dữ liệu cho US-GRP-006 (U15).
- **Primary UC hiện hành**: UC 27 Complete Group Assignment theo bản 73 UC. Phần bài nhóm của UC 16 View My Group (U12), UC 23 View Assignment Detail và UC 28 View Submission History (U11), UC 37 View Student Submissions (U15) do U14 cung cấp component/API gắn vào màn của unit đó. Supporting flows theo current-srs-contract.md.
- **Màn theo screen flow**: Student Assignments → Assignment Detail → Group Essay Workspace hoặc Submission History; giảng viên chính của lớp: Teacher Class Detail → tab Evals (danh sách bài nộp) → Submission Detail → Grading Workspace.
- **Quyền**: thành viên nhóm là sinh viên ghi danh `ACTIVE` của lớp `OPEN` (R5); trưởng nhóm hiện tại giao mục và nộp; giảng viên chính của lớp (R3/R4) xem tiến độ, bản nộp, nhả khóa mục; `ADMIN` không có quyền.
- **Thay đổi 2026-10-04**: tài liệu nhóm ở bảng `group_documents` (một nhóm của lớp × một bài nhóm); mỗi phần của khung là một mục, trưởng nhóm giao mục (không có mục chi tiết); người giữ sửa mục trong popup che kín trang; không có bước review; trưởng nhóm nộp bất kỳ lúc nào trước hạn; hết hạn tự nộp gồm phần đang làm.
- **Quyết định 2026-10-09**: bài nhóm chỉ có ở lớp, chỉ `GRADED`; khung chia phần theo heading nhỏ nhất của mỗi nhánh, mỗi phần một rubric (U09), U14 khóa mục theo `partId`; `onOpened` dựng tài liệu, `onRetired` đẩy tin ngưng giao và scanner tự nộp; U10 (template) đã xóa.
- **Thiết kế nguồn**: `construction/u14-group-document-submission/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `ScheduledScanner`, `EventPublisherPort` | U03 | Dùng thật |
| `ArtifactPort` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật (R5, R3/R4) |
| `AssignmentQueryPort`, `isSubmissionOpen` | U08 | Dùng thật; U14 cài `AssignmentLifecyclePort` của U08 |
| `DocumentModelPort`, `DocxExportPort`, `DocumentEditor` | U09 | Dùng thật (`SkeletonPart`: `partId`, `ancestorHeadingIds`) |
| `GroupMembershipPort` | U12 | Dùng thật; U14 cài `GroupChangePort` của U12 |
| U14 cung cấp `GroupSubmissionQueryPort` (U11, U13, U15, U16), event `group.submitted` (U16), `SseHub` (U16) | cho U11, U13, U15, U16 | Các unit đó dùng khi được code |
| `GroupSubmittedPort` | U15 (`C`, U14 khai báo) | Adapter rỗng: bản nộp nhóm chưa vào hàng chấm; U15 (ngay sau U14 trong wave 3) cài |

### Dữ liệu U14 sở hữu

PostgreSQL: `group_documents` (mục, lịch sử, bản nộp nằm trong cột JSON của bảng này); Redis `ratelimit:section-save:*`; RabbitMQ fanout `platform.realtime` và queue tạm `jobs.realtime.{instanceId}` mỗi backend; việc `GROUP_DOC_CREATE` trên `jobs.triggered`; `GroupAutoSubmitScanner` đăng ký `ScheduledScanner` (U03).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  groupdocs/
    api/                GroupDocController, GroupDocSseController, DTO
    application/        GroupDocInitializer, SectionService, GroupSubmitter,
                        GroupSubmissionQueryService, GroupDocAccessGuard
    realtime/           GroupDocEventPublisher, SseHub, RealtimeListener
    domain/             GroupDocument, Section, SectionStatus, SectionRevision,
                        GroupSubmission
    infrastructure/     JPA repository, NoopGroupSubmittedAdapter
    worker/             GroupAutoSubmitScanner, GroupDocCreateHandler
    adapter/            AssignmentLifecycleAdapter (U08), GroupChangeAdapter (U12)
    port/               GroupSubmissionQueryPort, GroupSubmittedPort
/backend/src/main/resources/db/migration/groupdocs/V20260925_1850__create_group_documents.sql
/frontend/src/app/classes/[id]/group-docs/[groupDocumentId]/   GroupDocumentPage (Group Essay Workspace)
/frontend/src/shared/group-docs/   GroupAssignmentPanel, GroupSubmissionView, GroupDocsOfGroupList,
                                   GroupDocsOverviewPanel
/frontend/src/features/group-doc/useGroupDocStream.ts
/contracts/openapi/group-docs.yaml
/contracts/messages/group-submission-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Cấu hình U14 theo `logical-components.md` §3; Tomcat async/timeout; Nginx route SSE và lưu nháp mục.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain, trạng thái mục và trạng thái tài liệu `IN_PROGRESS`/`CLOSED` (BR-U14-10…13, 24); port `GroupSubmissionQueryPort`, `GroupSubmittedPort` (adapter rỗng).
- [ ] **Bước 3** - `GroupDocAccessGuard`: R5 + thành viên hiện tại, trưởng nhóm hiện tại, người giữ mục, R3/R4 giảng viên chính của lớp; còn lại `404` và audit (P3, BR-U14-03, NFR-U14-22).
- [ ] **Bước 4** - `GroupDocInitializer`: dựng tài liệu cho mọi nhóm của lớp từ `config.parts` (khóa `partId`, sao `headingBlockId`, `ancestorHeadingIds`) khi bài mở và cho nhóm mới sau đó (F1, P5, BR-U14-01).
- [ ] **Bước 5** - `SectionService`: trưởng nhóm giao mục; nhận, lưu nháp (ân hạn 30 giây sau hạn), Xong (revision), nhả (xóa bản nháp chưa Xong, cả giảng viên nhả); khóa dòng `group_documents`; không ai thêm/xóa/sửa mục (F4-F8, P1, BR-U14-02, 04, 05, 10…13, 24).
- [ ] **Bước 6** - Realtime: `GroupDocEventPublisher` (sau commit → fanout), `RealtimeListener` (queue tạm `jobs.realtime.{instanceId}`), `SseHub` phân kênh theo khóa tổng quát để U16 dùng lại (heartbeat, timeout, đóng kênh người bị bỏ); kênh tài liệu chỉ cho thành viên (P2, P3, BR-U14-20…22).
- [ ] **Bước 7** - `GroupChangeAdapter` (`onGroupCreated` gửi việc `GROUP_DOC_CREATE`; `onMemberRemoved` nhả khóa, đóng kênh người rời nhóm; `hasGroupWork`), `AssignmentLifecycleAdapter` (`onOpened` gửi việc `GROUP_DOC_CREATE` cho mọi nhóm; `onRetired` sau commit đẩy `ASSIGNMENT_RETIRED`) và `GroupAutoSubmitScanner` (tự nộp khi quá hạn + 30 giây hoặc bài `RETIRED`) (F1, F9, F10, P4, BR-U14-13, 33).
- [ ] **Bước 8** - `GroupSubmitter` một đường (trưởng nhóm hiện tại nộp bất kỳ lúc nào khi bài còn nhận; tự nộp chốt mục đang giữ bằng bản nháp đã lưu, tài liệu `CLOSED`), bản chụp nhất quán, biên nhận, `late`, event (F9, P4, BR-U14-30…34, 36).
- [ ] **Bước 9** - `GroupSubmissionQueryService`: tài liệu nhóm của mình theo bài (Assignment Detail), tài liệu theo nhóm (My Group), tiến độ các nhóm của bài (giảng viên), bản nộp cuối, tác giả theo `partId`, trạng thái cho Student Assignments (U11); xuất DOCX (F2, F3, F11-F13, BR-U14-35, 37, 40…43).
- [ ] **Bước 10** - Audit theo BR-U14-50.
- [ ] **Bước 11** - Unit test mọi `BR-U14-xx`.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u14-group-document-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Flyway `db/migration/groupdocs/V20260925_1850__create_group_documents.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 14** - JPA repository.
- [ ] **Bước 15** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: hai người nhận cùng mục; giao mục đang có người giữ; mọi mục xong không đổi trạng thái; rời nhóm hoặc nhả bằng tay thì xóa bản nháp chưa Xong, mục về đúng `OPEN`/`DONE`; tự nộp tại hạn và khi ngưng giao lấy bản nháp của mục đang giữ, tài liệu `CLOSED`, không nộp trùng; hai client SSE nhận đúng sự kiện; nộp lại ghi đè `submitted_snapshot` và ghi audit.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 17** - `/contracts/openapi/group-docs.yaml` (gồm SSE) và schema event; cần sửa theo mục 7.
- [ ] **Bước 18** - Controller + DTO + validation; controller SSE.
- [ ] **Bước 19** - Test MockMvc: nhóm khác, tài khoản ngoài lớp và `ADMIN` nhận `404`; người không giữ mục không lưu được; chỉ trưởng nhóm hiện tại giao mục và nộp; không ai thêm/sửa/xóa mục; giảng viên chính của lớp chỉ xem tiến độ, bản nộp, nhả khóa; bản nháp không trả cho người khác người giữ.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `GroupDocumentPage` (màn Group Essay Workspace: `GroupDocHeader`, `SectionOutline`, `SharedBlocksView`, `SectionView`, `AssignSectionsDialog`, `ReleaseConfirmDialog`, `SubmitGroupDialog`, `AutoSubmitOverlay`), `useGroupDocStream`.
- [ ] **Bước 22** - `SectionEditorDialog` popup che kín trang, hiện các heading trên nhánh của mục cùng nội dung của chúng và các block của mục, không hiện nhánh khác (`DocumentEditor` của U09, tự lưu, Xong, Nhả có hộp xác nhận bỏ bản nháp); Xong đóng popup và workspace cập nhật realtime.
- [ ] **Bước 23** - Component gắn vào màn của unit khác: `GroupAssignmentPanel` (Assignment Detail, U11), `GroupSubmissionView` (Submission History U11, Submission Detail U15), `GroupDocsOfGroupList` (`MyGroupCard`, U12), `GroupDocsOverviewPanel` (danh sách bài nộp trong tab Evals, U15).
- [ ] **Bước 24** - Test frontend: nhận mục bị người khác nhận trước hiện thông báo; sự kiện SSE cập nhật trạng thái mục; mất kết nối thì tải lại; `ASSIGNMENT_RETIRED` hiện vòng chờ rồi chuyển Submission History; My Group mở Assignment Detail.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: luồng bài nhóm (giảng viên soạn khung, hệ thống tự chia phần, rubric từng phần → bài mở dựng tài liệu cho mỗi nhóm → trưởng nhóm giao phần → thành viên làm trong popup và Xong → trưởng nhóm nộp, hoặc hết hạn/ngưng giao tự nộp), cấu hình SSE qua Nginx.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRP-003 (UC 45, dựng tài liệu nhóm; U09 chủ trì) | 4, 7 |
| US-GRP-004 (UC 27: nhận, giao, làm, xong, nhả) | 3, 5, 6, 21, 22 |
| US-GRP-005 (UC 27: realtime, nộp, tự nộp) | 6, 7, 8, 21 |
| UC 16 (tài liệu bài nhóm trong My Group) | 9, 23 |
| UC 23, 28 (Assignment Detail, Submission History của bài nhóm) | 9, 23 |
| UC 37 (tiến độ, nhả khóa, bản nộp cho giảng viên) | 5, 9, 23 |
| US-GRP-006 (dữ liệu cho U15) | 9 |

## 5. Ngoài phạm vi

- Chấm và điểm cuối (U15), thông báo (U16), nhóm của lớp và trưởng nhóm (U12), soạn khung và rubric phần (U08, U09, U06), danh sách bài và Assignment Detail (U11).

## 6. Revision implementation scope - 2026-10-08
- [ ] Primary UC 23 Complete Group Assignment; authoring UC 43 do U09. Group Essay Workspace/Submission History/Student Submissions đúng nhãn, R5 membership và Teacher support R3/R4. (Đánh số cũ 70 UC; thay bằng mục 7.)

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Đánh số theo 73 UC: primary UC 27 (cũ 23); soạn bài nhóm UC 45 (cũ 43); phần bài nhóm của UC 16, 23, 28, 37. Bỏ mọi chỗ Template Editor, bài nhóm cấp môn, Admin.
- [ ] Contract `group-docs.yaml`: thêm `GET /api/v1/assignments/{assignmentId}/group-docs/mine` (Assignment Detail) và `GET /api/v1/groups/{groupId}/group-docs` (My Group); `x-roles` của `listGroupDocsOverview` bỏ `ADMIN`; `streamGroupDocument` chỉ `STUDENT`; `claim`, `assign`, `draft`, `done`, `submissions` chỉ `STUDENT` (quyền thật là R5, trưởng nhóm, người giữ); `GroupDocument` của giảng viên không có `draftBlocks`; `GroupSubmission` thêm `late`, `submittedBy`, `receiptHash` nếu thiếu; `Section` thêm `headingBlockId`, `ancestorHeadingIds`; mô tả lỗi ngoài quyền là `404`.
- [ ] Event SSE: thêm `ASSIGNMENT_RETIRED`, `SECTION_ASSIGNED` vào schema sự kiện realtime (không đổi `group.submitted` trên `platform.events`).
- [ ] Code: `GroupDocAccessGuard` (R5 lớp `OPEN`, trưởng nhóm hiện tại, R3/R4, `ADMIN` → `404`); `AssignmentLifecycleAdapter.onRetired` đẩy `ASSIGNMENT_RETIRED`; ân hạn 30 giây cho lần lưu nháp cuối sau hạn, không có ân hạn khi ngưng giao; `GroupSubmissionQueryPort` thêm truy vấn trạng thái nhóm mình theo bài cho U11 và tiến độ theo nhóm.
- [ ] Migration: đổi tên `V20260925_1850__u14_group_workspace.sql` thành `db/migration/groupdocs/V20260925_1850__create_group_documents.sql` (chưa áp dụng ở môi trường nào; nếu đã áp dụng thì giữ tên cũ và thêm migration forward-only); index `(assignment_id, status)`, `(group_id)` thay `(assignment_id, submitted_at)`.
- [ ] Frontend: route `app/learning/group-docs/[id]` đổi sang `app/classes/[id]/group-docs/[groupDocumentId]`; Group Essay Workspace chỉ mở từ Assignment Detail; thêm `GroupAssignmentPanel`, `GroupDocsOfGroupList`; `GroupDocsOverviewPanel` gắn vào danh sách bài nộp trong tab Evals (U15), không còn route `app/teaching/...`; `ReleaseLockButton` thay bằng `ReleaseConfirmDialog` dùng chung.
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U11 gắn `GroupAssignmentPanel` vào Assignment Detail và `GroupSubmissionView` vào Submission History khi bài là bài nhóm, Student Assignments đọc trạng thái nhóm qua `GroupSubmissionQueryPort`; U12 gắn `GroupDocsOfGroupList` vào `MyGroupCard`; U15 gắn `GroupDocsOverviewPanel` vào danh sách bài nộp của bài nhóm và dùng `GroupSubmissionView`/`GET /group-docs/{id}/submission` ở Submission Detail; U03 giữ `GROUP_DOC_CREATE` trên `jobs.triggered`.
