# U12 Group Assignment — code summary

**Current source snapshot 2026-10-30:** [as-built-baseline.md](as-built-baseline.md) — source-observed; các số test/ngữ cảnh checkout dưới đây là lịch sử, không phải kết quả chạy lại tại HEAD hiện tại.

## Ghi nhận trước baseline 2026-10-30 (lịch sử)

Code viết ngày 2026-10-21 đến 2026-10-22 theo [plan](../../plans/code-generation-plan/u12-group-assignment-code-generation-plan.md), trên nhánh `group-assignment` tạo lại từ `staging`, chưa push. Người code: Võ Minh Tài (Người 2). U07 (soạn bài, khung, `DocumentEditor`) và U08 (Assignment Detail, `useActivityTracker`) chưa có code nên luồng thật phát hành bài nhóm → tạo tài liệu → làm → nộp → U09 chấm chỉ thử được khi hai unit đó vào `staging`; integration test do tester viết.

## Commit

| Bước | Commit | Nội dung |
|---|---|---|
| Plan | `60f0cc8` | Chi tiết hóa plan |
| 1 | `eea1a80` | Migration 7 bảng, `GROUP_MANAGE`, `GROUP_WORK` |
| 2 | `67b066d` | Domain và repository |
| 3 | `ccf357f` | Quản lý nhóm, chia ngẫu nhiên |
| 4 | `e2edcd7` | Yêu cầu đổi trưởng nhóm |
| 5 | `d13ea5e` | Kiểm sẵn sàng, tạo tài liệu khi bài nhóm mở |
| 6 | `74e269e` | Nhận, giao, sửa, nhả mục; mở workspace |
| 7 | `698b75c` | Nộp tay, tự nộp, DOCX |
| 8 | `bad07c5` | Realtime SSE qua RabbitMQ |
| 9 | `1a3b0b2` | Interface cho U08, U09; ghi hoạt động; Nhóm của tôi |
| 10 | `114c970` | REST API (kèm sửa chọn constructor của `ClassGroupService` phát hiện khi khởi động thử) |
| 11 | `fc2a3e0` | Màn giảng viên |
| 12 | `a2c8c69` | Màn sinh viên và workspace tài liệu nhóm |

## Business logic

| Luồng | Class chính |
|---|---|
| F1, F2, F14 Nhóm | `ClassGroupService` (đọc bộ nhóm kèm người đã rời lớp, sinh viên chưa có nhóm, yêu cầu chờ, `version`; lưu cả bộ: kiểm toàn bộ bằng `GroupSetRules` trước, nhóm đã dùng không xóa được, một transaction; người rời hoặc chuyển nhóm được nhả mục đang giữ và đóng kênh SSE với tài liệu cũ; đổi trưởng nhóm hủy yêu cầu chờ; nhóm mới có tài liệu cho bài đang mở; audit từng nhóm; sự kiện sau commit), `RandomGroupSplitter` (2–20 người, chênh tối đa một, trưởng nhóm ngẫu nhiên, chỉ là bản xem trước), `GroupAccess` (giảng viên chính hoặc sinh viên đang học; ngoài phạm vi 404 và `ACCESS_DENIED`; U01, U05 lỗi thì 503) |
| F3, F5 Trưởng nhóm | `LeaderRequestService` (gửi: thành viên, lớp `OPEN`, lý do 10–1.000, đề cử là thành viên khác trưởng nhóm, một yêu cầu chờ mỗi nhóm; hủy: chỉ người gửi; duyệt: mặc định người được đề cử, bắt buộc chọn khi không có; từ chối: bắt buộc lý do) |
| F4 Nhóm của tôi | `MyGroupService` (chỉ nhóm của mình, bài nhóm đã mở kèm tiến độ và nộp, hai yêu cầu gần nhất của mình; popup Student Detail) |
| F6 Sẵn sàng | `GroupReadinessService` cài `IGroupAssignmentSupport` (tính theo thành viên hiệu lực) |
| F7 Tài liệu | `GroupAssignmentLifecycle` cài `IAssignmentLifecycleListener` (mở bài: job `GROUP_DOC_CREATE`; ngưng giao: đánh dấu tự nộp), `GroupDocumentService` (mỗi nhóm có thành viên hiệu lực một tài liệu, mỗi phần một mục, `ON CONFLICT DO NOTHING`; tạo ngay khi người học mở trước job), `GroupDocCreateJobHandler` |
| F8–F11 Mục | `GroupWorkspaceService` (workspace, bản nháp chỉ người giữ thấy), `GroupSectionService` (khóa dòng mục; nhận, giao, nhả, lưu nháp kèm `version` và ân hạn 30 giây, Xong kiểm `IDocumentModel` và thêm lịch sử; giới hạn 2.000 block, 2 MB; audit giao và nhả do trưởng nhóm hoặc giảng viên), `GroupDocAccess` |
| F12, F13 Nộp | `GroupSubmissionService` (chỉ trưởng nhóm hiện tại, kèm `version`; `validateForSubmit`; ảnh chụp phần chung và nội dung đã Xong kèm tác giả, `receiptHash` SHA-256, `late`; gọi `IGroupEvaluation.onGroupSubmitted` trong cùng transaction; DOCX qua `IDocx`), `GroupAutoSubmitScanner` (mỗi phút: quá hạn cuối 30 giây hoặc bài ngưng giao; chốt mục đang giữ bằng bản nháp; mỗi tài liệu một transaction, cập nhật có điều kiện), `GroupSnapshot` |
| Realtime | `RabbitSseHub` cài `ISseHub` và `IGroupDocStreams` (fanout `platform.realtime` sau commit, queue tạm mỗi instance, heartbeat 25 giây, đóng kênh của người rời nhóm trên mọi instance), `SseChannels` |
| F15 Interface | `GroupWorkQueryService` cài `IGroupWorkStatus` (U08) và `IGroupSubmissionSource` (U09, thay bản tạm); tiến độ cho tab Sinh viên; ghi hoạt động qua `IActivityWrite` loại `GROUP_DOCUMENT` sau khi kiểm thành viên |

## API

| Endpoint | UC |
|---|---|
| `GET`, `PUT /api/v1/classes/{classId}/groups`; `POST .../groups:random-preview` | 57 |
| `GET /api/v1/classes/{classId}/leader-requests`; `POST /api/v1/leader-requests/{id}:approve`, `:reject` | 57 |
| `GET /api/v1/classes/{classId}/students/{studentId}/group` | 23, 57 |
| `GET /api/v1/classes/{classId}/group-assignments`; `GET .../group-assignments/{assignmentId}/progress` | 42, 57 |
| `GET /api/v1/me/classes/{classId}/group`; `POST /api/v1/groups/{groupId}/leader-requests`; `POST /api/v1/leader-requests/{id}:cancel` | 54, 55 |
| `GET /api/v1/assignments/{assignmentId}/my-group-doc`; `GET /api/v1/group-docs/{id}`; `GET .../events` (SSE, `X-Accel-Buffering: no`); `POST .../activity` | 56 |
| `POST /api/v1/group-docs/{id}/sections/{sectionId}:claim`, `:assign`, `:release`, `:done`; `PUT .../sections/{sectionId}/draft` | 56 |
| `POST /api/v1/group-docs/{id}/submit`; `GET .../export.docx?submitted=` | 56 |

Lỗi là Problem Detail kèm `code`; bộ nhóm mâu thuẫn trả `issues` theo nhóm, tài liệu chưa đủ điều kiện nộp trả `issues` cần sửa (`GroupExceptionHandler`).

## Repository và migration

- `GroupRepository`, `GroupDocumentRepository` dùng `JdbcClient`; nội dung mục, bản nộp là `jsonb`; khóa dòng mục và tài liệu bằng `SELECT ... FOR UPDATE`; ghi mục và tài liệu có điều kiện theo `version`.
- `db/migration/groups/V20260925_1800__create_group_tables.sql`: 7 bảng gồm `class_group_sets` (version của bộ nhóm); chưa có FK tới `assignments` (U07 thêm sau).

## Frontend

- `features/groups`: `ClassGroupsPanel` (tab Sinh viên: thẻ nhóm, người đã rời lớp, `GroupEditor`, `RandomSplitDialog`, `LeaderRequestList`, `GroupProgressView` kèm nhả mục), `GroupCountCard` (tab Thông tin lớp), `StudentGroupInfo` (popup Student Detail), `GroupAssignmentPanel` (Assignment Form bài nhóm), `MyGroupCard`, `MyGroupPage` (`app/classes/[id]/group`), `LeaderChangeRequestDialog`.
- `features/groupdocs`: `GroupWorkspacePage` (`app/group-docs/[id]`, layout cho Student, Teacher, Subject Manager), `SectionEditorDialog` (che kín trang, tự lưu 10 giây), `AssignSectionDialog`, `SubmitGroupDialog`, thẻ Bản nộp, lớp phủ "Đang nộp…", `useGroupDocStream` (EventSource, tải lại khi kết nối lại), `OpenGroupWorkButton` (slot `GroupWorkEntry`).
- Đăng ký slot trong `features/groups/register.ts`, `features/groupdocs/register.ts`.

## Kiểm tra

- Backend U12: 60 test pass (quy tắc nhóm 3, mục 4, repository trên PostgreSQL 16 2, nhóm 7, chia ngẫu nhiên 7, yêu cầu đổi trưởng nhóm 5, Nhóm của tôi 2, tạo tài liệu và sẵn sàng 5, mục 7, nộp 5, truy vấn cho U08/U09 5, SSE 3, controller 5); `ArchitectureTest` pass.
- `mvn -B verify` toàn backend: 888 test, 33 bỏ qua, 4 lỗi. Cả 4 là integration test của unit khác dừng ở migration `V20260925_1500__create_grading_tables.sql` của U09 (cần bảng `assignments` chưa có trên `staging`); U12 không sửa file này.
- Frontend: 305 test (U12: 13), lint, typecheck, build pass (có route `/classes/[id]/group`, `/group-docs/[id]`).
- Khởi động thử backend với PostgreSQL và Redis local (tắt Flyway và kiểm schema vì migration 1500): context lên; bản tạm `ISseHub`, `IGroupSubmissionSource`, `IGroupAssignmentSupport`, `IGroupWorkStatus` đã tắt; API nhóm trả 401 khi chưa đăng nhập.
- Kiểm trên trình duyệt với API giả: workspace tài liệu nhóm (trưởng nhóm, mục đang giữ, mục đã Xong), Nhóm của tôi, tab Sinh viên (yêu cầu chờ, thẻ nhóm, người đã rời lớp), trình sửa nhóm.

## Khác với thiết kế

- Không cài `IPendingSweeper` cho `GROUP_DOC_CREATE`: không có bảng job chờ để quét; message mất được bù bằng tạo tài liệu ngay khi người học mở (F7 bước 3) và khi lưu nhóm.
- Trình soạn mục là ô đoạn văn tạm (mỗi đoạn một block `PARAGRAPH`) tới khi U07 có `DocumentEditor`, trình vẽ Draw.io; hiển thị block là bản rút gọn.
- `useActivityTracker` của U08 chưa có: backend nhận sự kiện qua `POST .../activity`, frontend chưa gắn.
- Tool MCP `read_authorized_group_submission` không thuộc plan này (giao người làm AI chấm của U09).
- `GroupAssignmentPanel` tính sẵn sàng ở giao diện từ `GET .../groups`; U07 gọi `IGroupAssignmentSupport` khi phát hành.
- Thêm `GET .../students/{studentId}/group`, `GET .../group-assignments` và trường `viewerId` trong workspace (không có trong domain-entities mục 5).
- Bản nộp đặt nội dung của mục sau các block khung của phần, ngay trước heading kế tiếp.
- Tài liệu chưa nộp lần nào vẫn được tự nộp khi hết hạn (kể cả trống), vì "có thay đổi sau lần nộp cuối" coi như đúng khi chưa nộp.

## Việc cho nhóm

- U07: gọi `IGroupAssignmentSupport.checkReadiness` khi phát hành, `IAssignmentLifecycleListener.onOpened/onRetired` khi mở và ngưng giao; thêm FK `assignment_id` khi có bảng `assignments`; cung cấp `DocumentEditor`.
- U08: hiện slot `GroupWorkEntry`, gọi `IGroupWorkStatus`, cấp `useActivityTracker`.
- U09: đã đọc bản nộp qua `IGroupSubmissionSource` (bản tạm đã tắt khi U12 có).

## Review bước 14

### UC và business rule

| Phạm vi | Đáp ứng ở | Ghi chú |
|---|---|---|
| UC 57 nhóm; BR-U12-01–07 | `ClassGroupService`, `GroupSetRules`, `RandomGroupSplitter`, `ClassGroupsPanel` | Lưu cả bộ kèm version; nhóm đã dùng chỉ chuyển thành viên; người rời lớp có nhãn và bị bỏ khi lưu |
| UC 57 duyệt, UC 55 gửi và hủy; BR-U12-10–13 | `LeaderRequestService`, `LeaderRequestList`, `LeaderChangeRequestDialog` | Đổi trưởng nhóm không đổi mục ai đang giữ |
| UC 54; BR-U12-50, 51 | `MyGroupService`, `MyGroupCard`, `MyGroupPage`, `StudentGroupInfo`, `OpenGroupWorkButton` | Không thấy nhóm khác hay yêu cầu của người khác |
| UC 53 phần nhóm; BR-U12-20–22, 30 | `GroupReadinessService`, `GroupAssignmentLifecycle`, `GroupDocumentService`, `GroupAssignmentPanel` | Soạn đề, phát hành thuộc U07 |
| UC 56; BR-U12-31–44 | `GroupSectionService`, `GroupSubmissionService`, `GroupAutoSubmitScanner`, `RabbitSseHub`, `GroupWorkspacePage` | Trình soạn tạm; theo dõi hoạt động chờ U08 |
| BR-U12-52 tiến độ, chấm | `GroupWorkQueryService`, `GroupProgressView` | U09 đọc qua `IGroupSubmissionSource` |
| BR-U12-60 AI | - | Bài nhóm không có tính năng AI cho người học; tool MCP cho giảng viên ngoài plan |
| BR-U12-61, 62 audit, quyền | Audit từng service; `GroupAccess`, `GroupDocAccess` | Không audit từng lần tự lưu; sự kiện và SSE sau commit |

### Extension (Security Baseline, Resiliency Baseline theo phạm vi rút gọn 2026-09-24)

| Rule | Kết quả | Lý do |
|---|---|---|
| SECURITY-03 Logging | Tuân thủ | SLF4J; log lỗi chỉ ghi loại lỗi và mã, không ghi nội dung tài liệu |
| SECURITY-04 Header HTTP | N/A | Không thêm endpoint trả HTML; DOCX có `X-Content-Type-Options: nosniff` |
| SECURITY-05 Kiểm đầu vào | Tuân thủ | Tên nhóm ≤ 100, lý do 10–1.000, lý do từ chối ≤ 1.000, sĩ số 2–20, tối đa 2.000 block và 2 MB mỗi mục, tối đa 200 sự kiện hoạt động mỗi lần; SQL dùng tham số |
| SECURITY-08 Kiểm quyền | Tuân thủ | Mọi service kiểm qua `IAuthorization` và `IClassAccess`; tài liệu chỉ thành viên hiệu lực và giảng viên chính của lớp; kênh SSE kiểm quyền trước khi mở; ngoài phạm vi 404 |
| SECURITY-09 Hardening | Tuân thủ | Lỗi trả Problem Detail không có stack trace |
| SECURITY-12 Credential | N/A | U12 không xử lý mật khẩu hay khóa |
| SECURITY-15 Lỗi và fail closed | Tuân thủ | U01, U05, U07 lỗi thì 503; cập nhật có điều kiện theo version; tự nộp mỗi tài liệu một transaction, lỗi không chặn tài liệu khác |
| RESILIENCY-04 Triển khai | N/A | Không đổi pipeline hay hạ tầng |
| RESILIENCY-06 Health check | N/A | Không thêm phụ thuộc mới (RabbitMQ, PostgreSQL đã có health) |
| RESILIENCY-10 Cô lập phụ thuộc | Tuân thủ | Realtime gửi lỗi chỉ ghi log; job tạo tài liệu thử lại theo backoff của U03; tạo tài liệu ngay khi mở nếu job chưa chạy |

## Sửa sau kiểm thử hộp đen của nhóm (2026-10-24)

Plan: `construction/plans/2026-10-24-u12-test-plan-gaps.md`, nhánh `fix/u12-test-plan-gaps`. Chỉ frontend; API, quy tắc và dữ liệu không đổi.

| Ca | Thay đổi | File |
|---|---|---|
| U12-03 | Kéo thả thành viên sang nhóm khác hoặc khu Chưa có nhóm (ô ⇄ Chuyển giữ cho bàn phím); nhóm mới ghi "(mới)" | `features/groups/GroupEditor.tsx` |
| U12-05 | Nút "Mở tab Sinh viên" trong hộp Kiểm tra sẵn sàng, mở tab mới `/teaching/classes/{id}?tab=students`; hộp tự kiểm lại khi quay về cửa sổ; trang lớp nhận `?tab=` | `features/groups/TeacherGroupWidgets.tsx`, `features/academics/ReadOnlyClassPage.tsx` |
| U12-08, 09 | Bấm vào mục Mở/Xong để nhận và mở luôn trình soạn; mục mình giữ thì soạn tiếp; mục người khác giữ chỉ cuộn tới nội dung; dòng người giữ ghi "Đang giữ: …"; nút Nhận mục cũng mở trình soạn | `features/groupdocs/GroupWorkspacePage.tsx` |
| U12-13 | Popup tự nộp ghi "Đã hết hạn nộp lúc …" | `features/groupdocs/GroupWorkspacePage.tsx` |

Test thêm: kéo thả và nhãn "(mới)" (`ClassGroupsPanel.test.tsx`), nút mở tab và tự kiểm lại (`TeacherGroupWidgets.test.tsx`, mới), `?tab=` (`academics.test.tsx`), bấm vào mục, giảng viên không nhận được, popup tự nộp (`GroupWorkspacePage.test.tsx`). Frontend 82 file / 473 test đạt; `tsc`, eslint phần đổi, `next build` đạt. Chưa chạy thử bằng tay trên localhost:8088.

## Sửa sau kiểm thử end-to-end (2026-10-24)

Bằng chứng: `construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md`. Nhánh `fix/e2e-u06-u12-findings`.

- `GroupSnapshot.withSection` và `GroupSectionService.validate`: nháp và Xong được kiểm trong khung chung. Trước đây bộ kiểm của U07 thấy thiếu block của người soạn nên mọi lần lưu đều bị từ chối; unit test không phát hiện vì `IDocumentModel` là mock. Thêm test dùng `DocumentModelService` thật.
- `GroupDocAccess.window`, `GroupAutoSubmitScanner`: ân hạn 30 giây chỉ nới hạn đóng. Trước đây 30 giây đầu sau khi mở bài bị coi là quá hạn, bộ quét tự nộp và đóng tài liệu của mọi nhóm. Thêm `GroupAutoSubmitScannerTest`.

## Trình soạn chung và Draw.io cho tài liệu nhóm (2026-10-25, KAN-64, KAN-66)

Plan: `construction/plans/2026-10-24-u12-document-editor.md`, nhánh `feat/u12-document-editor`.

- **`SectionEditorDialog`:** dùng `DocumentEditor` (`@/shared/document`) ở `mode="student"` thay cho ô gõ chữ tạm.
  - `partOf` lấy heading của phần (`partId` là id heading) làm block khóa; hướng dẫn của giảng viên trong phần hiện chỉ xem bằng `DocumentViewer`.
  - Chỉ block `STUDENT` được gửi lên khi lưu nháp; Draw.io theo `diagramEnabled` của bài.
  - Giữ nguyên tự lưu 10 giây, "Bạn đang giữ", nút Xong.
- **`GroupWorkspacePage`:**
  - Hướng dẫn chung và nội dung đã Xong hiện bằng `DocumentViewer` (có sơ đồ, bảng, danh sách). Bỏ `features/groupdocs/BlockView.tsx`.
  - `useActivityTracker` gửi vào `POST /api/v1/group-docs/{id}/activity` khi bài bật giám sát.
- **`types.ts`:** dùng kiểu `DocumentBlock`, `DocumentModel` chung; `WorkspaceView.diagramEnabled`.
- **Backend:**
  - `GroupWorkspaceService.WorkspaceView.diagramEnabled` lấy từ `AssignmentView`, kèm `GroupWorkspaceServiceTest`.
  - Migration `groups/V20261025_1800__add_group_documents_assignment_fk.sql` thêm khóa ngoại `group_documents.assignment_id → assignments(id)` (`NOT VALID`).
- **Giới hạn:** chưa tải ảnh trong tài liệu nhóm (không có `uploadImage`/`imageUrl`); ảnh trong khung của giảng viên chưa hiện ảnh thật vì endpoint ảnh của U07 dành cho người soạn.

## Cộng tác trên mục, nhập file, đánh giá chéo (2026-11-02)

Plan: `construction/plans/code-generation-plan/2026-11-02-u06-u12-feedback.md`, nhánh `feat/u06-u12-feedback-2026-11-02`. Bổ sung FB-U12-01..04 (ngoài BR đã chốt, xem `2026-11-02-feedback-additions.md`).

- **Migration** `groups/V20261102_1000__group_section_collaboration.sql`: `group_document_sections.locked_by/locked_at` (chỉ khi `DONE`), `group_document_revisions.kind` (`DONE`, `TAKEOVER`, `IMPORT`), bảng `group_peer_reviews` (1–5, không tự chấm, một người chấm một người một lần).
- **`GroupSectionService`:**
  - `takeOver`: tiếp quản mục người khác giữ, giữ bản nháp và ghi lịch sử `TAKEOVER`.
  - `lock`, `unlock`: trưởng nhóm chốt hoặc mở chốt; `SectionRules.canClaim/canAssign/canLock` chặn mục đã chốt.
  - `revisions`, `restore`: lịch sử 50 bản mới nhất, khôi phục vào bản nháp.
  - `saveDraft` gắn ảnh `fileRef` qua `IArtifact` (ảnh trong tài liệu nhóm dùng được).
- **`GroupDocImportService`:** xem trước DOCX vào mục; xem trước và áp dụng DOCX/Draw.io cho cả tài liệu theo tên mục (không AI); XML Draw.io đọc an toàn.
- **`GroupPeerReviewService`, `GroupPeerReviewRepository`:** cửa sổ mở sau khi nộp tới 7 ngày sau hạn cuối; giảng viên xem trung bình và thang 10.
- **API mới:** `:take-over`, `:lock`, `:unlock`, `/revisions`, `:restore` trên mục; `/sections/{id}/docx:preview`, `/import:preview`, `/import:apply`, `GET/PUT /peer-reviews` (`GroupDocCollaborationController`).
- **Frontend:**
  - `GroupWorkspacePage`: nút Tiếp quản, Chốt/Mở chốt, Lịch sử, Nhập từ file; thẻ Đánh giá chéo.
  - `SectionEditorDialog`: tải ảnh, Nhập DOCX, Lịch sử kèm khôi phục, chỉ xem khi bị tiếp quản.
  - `CollaborationPanels.tsx`: `SectionHistoryDialog`, `GroupImportDialog`, `PeerReviewPanel`.
- **Test:** `GroupSectionServiceTest` (+5), `GroupDocCollaborationServicesTest` (9, mới), `GroupControllersTest` (+2), `GroupRepositoriesIntegrationTest` (khóa, loại lịch sử, đánh giá chéo), `GroupWorkspacePage.test.tsx` (+8).
- **Giới hạn:**
  - Sơ đồ nhập từ `.drawio` chưa có ảnh xem trước cho tới khi mở sơ đồ và lưu trong Draw.io.
  - Điểm đánh giá chéo chưa ghép vào điểm cuối; trọng số ba đầu điểm (tài liệu, thành viên, thuyết trình) cần U07/U09.

## Chỉ trưởng nhóm tiếp quản, chốt ghi công người từng làm (2026-11-02)

Người dùng chốt lại BR-U12-33, 39 sau khi xem bản đã gộp (PR #35). Nhánh `fix/u12-leader-takeover-credit`.

- **Backend:** `SectionRules.canTakeOver` chỉ cho trưởng nhóm (`403 GROUP_DOC_LEADER_ONLY`); `GroupDocumentRepository.lockedContributors` lấy tác giả các bản lịch sử tạo tới lúc chốt; `SectionView.contributorIds` (rỗng khi chưa chốt); `GroupWorkspaceService` và `lock` trả ghi công. Không đổi schema.
- **Frontend:** nút Tiếp quản chỉ hiện với trưởng nhóm, thành viên bấm mục người khác giữ thì chỉ xem; dòng "Ghi công: …" ở mục đã chốt; cột Tác giả ở thẻ Bản nộp hiện người được ghi công.
- **Test:** `GroupSectionServiceTest` (thành viên bị chặn tiếp quản, ghi công khi chốt), `GroupRepositoriesIntegrationTest` (ghi công theo lịch sử tới lúc chốt, bản sau lúc chốt không tính), `GroupWorkspacePage.test.tsx`, e2e `u12c`.

## Bỏ tiếp quản, chốt ghi người phụ trách, trả BR về bản chốt (2026-11-02)

BR đã chốt nên không sửa `business-rules.md`; đã trả về đúng bản chốt. Nhánh `fix/u12-follow-frozen-br`. Danh sách bổ sung: `construction/plans/code-generation-plan/2026-11-02-feedback-additions.md`.

- **Bỏ tiếp quản** (trái BR-U12-33): gỡ `GroupSectionService.takeOver`, `SectionRules.canTakeOver`, `GroupSection.takenOverBy`, API `:take-over`, nút và hộp thoại, trạng thái "Đã bị tiếp quản" của trình soạn. Nhả và giao lại vẫn bỏ bản nháp theo BR-U12-36.
- **Chốt mục (FB-U12-01)**: người phụ trách là `lastAuthorId`, hiện "Phụ trách: …"; bỏ `lockedContributors` và `SectionView.contributorIds`.
- **Test:** bỏ test tiếp quản và ghi công; e2e `u12c` kiểm mục đã có người nhận thì kể cả trưởng nhóm không nhận được, API tiếp quản không còn, chốt ghi người phụ trách.

## Phân công rồi chốt (FB-U12-01, 2026-11-02)

Thay chốt từng mục. Nhánh `fix/u12-follow-frozen-br`.

- **Migration** `groups/V20261102_1200__group_assignment_phase.sql`: `group_documents.assignment_locked_by/at`, `group_document_sections.assignee_id`; dữ liệu cũ: người phụ trách = người giữ hoặc người làm cuối, tài liệu đã có người làm coi như đã chốt phân công.
- **Backend:** `SectionRules` theo giai đoạn (`canClaim/canAssign/canRelease/canEdit` nhận `working`); `GroupSectionService.claim` (phân công: nhận làm người phụ trách; đã chốt: người phụ trách bắt đầu soạn), `assign` chỉ khi chưa chốt, `release`, `lockAssignment` (đủ người phụ trách mới chốt), `unlockAssignment`; API `POST /group-docs/{id}/assignment:lock|:unlock`; `WorkspaceView.assignmentLockedBy/At`, `SectionView.assigneeId`; người rời nhóm thôi phụ trách (`clearAssignee`).
- **Frontend:** thanh "Đang phân công / Đã chốt phân công" với nút Chốt phân công (khóa khi còn mục trống) và Mở chốt (hỏi xác nhận); mục hiện "Phụ trách / Đang soạn / Chưa có người nhận"; nút Nhận mục, Bỏ nhận, Bỏ giao, Giao mục (khi phân công), Làm (khi đã chốt).
- **Test:** backend 1749/1749; frontend 735/735 (5 test chậm của unit khác chạy lại đạt); e2e lớp SE-TEST15: U12 59/59, U12b 36/36, u12c 32/32, notify 3/3; U02 16/16, U06 24/24.

## Tối giản theo BR đã chốt (2026-11-08)

Người dùng chọn giữ luồng bài nhóm tối giản: sinh viên làm bài nhóm, giảng viên chấm, không thêm luồng phức tạp. Bỏ FB-U12-01..04 (ngoài BR), quay về BR-U12-33, 34, 36. Nhánh `refactor/u12-simplify`. Không đổi schema: cột và bảng của các tính năng đã bỏ còn trong CSDL (migration đã chạy), code không dùng.

- **Bỏ:** phân công rồi chốt (`assignment:lock/unlock`, `AssignmentLock`, `GroupSection.assigneeId`, các mã `ASSIGNMENT_*`, `SECTION_ASSIGNED_TO_OTHER`); khôi phục bản lịch sử (`:restore`); nhập DOCX/Draw.io cho cả tài liệu (`import:preview`, `import:apply`); đánh giá chéo (`GroupPeerReviewService`, `GroupPeerReviewRepository`, `PeerReview`, `GET/PUT /peer-reviews`). `GroupDocCollaborationController` bỏ; xem trước DOCX vào mục chuyển sang `GroupDocController`.
- **Giữ:** nhận mục Mở hoặc Xong là soạn ngay; trưởng nhóm giao mục (mục đang giữ thì nhả trước khi `replaceHolder`); người giữ, trưởng nhóm, giảng viên nhả mục; lịch sử mục chỉ xem; nhập DOCX vào mục đang soạn; nộp, nộp lại, tự nộp, Bản nộp.
- **Bổ sung lại:** `GroupProgressCard` trên trang Sinh viên của giảng viên (BR-U12-52): từng nhóm, trưởng nhóm, mục Xong, trạng thái nộp, mục đang soạn, link Xem tài liệu. Bảng tiến độ cũ (`GroupProgressView`) đã bị bỏ trong commit giao diện `da0be0d8`.
- **Frontend:** `GroupWorkspacePage` còn 2 thẻ (Tài liệu, Bản nộp); mỗi mục có Nhận mục / Sửa mục / Soạn tiếp, Giao mục (trưởng nhóm), Nhả mục, Lịch sử. `SectionHistoryDialog.tsx` chỉ xem; bỏ `CollaborationPanels.tsx`.
- **Test:** backend `GroupSectionServiceTest`, `GroupSectionTest`, `GroupRepositoriesIntegrationTest`, `GroupControllersTest` trả về bản theo BR và thêm ca nhận lại mục Xong, lịch sử, ảnh; `GroupDocImportServiceTest` mới; bỏ `GroupDocCollaborationServicesTest`. Frontend `GroupWorkspacePage.test.tsx` viết lại theo luồng mới, `GroupProgressCard.test.tsx` mới. E2E `u12`, `u12b`, `u12c` theo luồng mới; `u12c` kiểm các API đã bỏ trả 404.


## Sửa 2 lỗi khi kiểm lại (2026-11-09)

Nhánh `fix/u12-editor-lost-hold`. Không đổi BR, schema hay API cũ; chỉ thêm trường `authors` vào `WorkspaceView.lastSubmission`.

- **Trình soạn bị kẹt khi mất mục:** trưởng nhóm giao lại mục, người khác nhả mục hoặc hệ thống tự nộp thì trình soạn toàn trang của người đang giữ vẫn mở; còn phần chưa lưu thì Đóng lưu thất bại và không thoát được. `GroupWorkspacePage` đóng trình soạn khi bản xem mới cho thấy người dùng không còn giữ mục (bỏ qua bản xem cũ hơn mục đang soạn) hoặc tài liệu đã khóa, và báo lý do. `SectionEditorDialog`: lưu lỗi khi Đóng thì nút thành "Đóng, bỏ phần chưa lưu", bấm lần nữa là thoát.
- **Tác giả ở thẻ Bản nộp (BR-U12-44):** thẻ lấy `lastAuthorId` hiện tại nên mục sửa sau khi nộp làm đổi tác giả của bản nộp. `WorkspaceView.lastSubmission.authors` trả `section_authors` đã lưu lúc nộp; thẻ hiện cột "Tác giả trong bản nộp", mục chưa Xong lúc nộp ghi "Chưa Xong, không vào bản nộp".
- **Test:** `GroupWorkspaceServiceTest.lastSubmissionCarriesSectionAuthorsCapturedAtSubmit`; `GroupWorkspacePage.test.tsx` thêm ca giao lại mục và tự nộp đóng trình soạn, ca vừa nhận mục vẫn mở, Đóng lần hai, tác giả bản nộp; mock nhận mục tăng `version` như server.

## Cải thiện luồng nhận, soạn mục (2026-11-09)

Nhánh `feat/u12-section-ux`. Không đổi BR, schema hay trường cũ; chỉ thêm `SectionView.claimedAt`, `holderSavedAt`.

- **Hỏi lại trước khi sửa mục người khác viết:** mục Xong có `lastAuthorId` khác người đang xem thì nút "Sửa mục" (và bấm vào tên mục) mở hộp "Sửa mục …?" nói rõ người nhận khóa mục và thành người viết khi bấm Xong. Mục do chính mình viết hoặc mục Mở thì nhận ngay như cũ.
- **Cảnh báo rời trang:** `SectionEditorDialog` gắn `beforeunload` khi còn phần chưa lưu (tự lưu 10 giây sau lần sửa cuối).
- **Nhắc hạn:** `model/deadline.ts` (`describeDeadline`, `formatAgo`), `hooks/useClock.ts`. Còn ≤ 30 phút tới `closesAt`, hoặc đang trong khoảng nộp trễ tới `lateUntil`, workspace hiện banner: còn bao lâu, số mục chưa ai làm (không vào bản nộp), số mục đang soạn (tự chốt bằng bản nháp khi đóng nhận bài, BR-U12-43), nhóm chưa nộp. Trình soạn hiện cùng dòng nhắc hạn. Giảng viên không thấy banner.
- **Người giữ mục còn soạn không:** dòng mục đang giữ hiện "Đang soạn: Bình · lưu lần cuối 3 phút trước" hoặc "· nhận 2 giờ trước, chưa lưu". Backend `SectionView.of` trả `claimedAt`, `holderSavedAt` (= `draftSavedAt` khi `CLAIMED`) cho mọi người; `draftBlocks`, `draftSavedAt` vẫn chỉ người giữ thấy (BR-U12-34). Lưu nháp không đẩy SSE (BR-U12-37) nên trang tải lại mỗi 60 giây khi có mục người khác đang giữ và tab đang hiện.
- **Test:** `GroupWorkspaceServiceTest.everyoneSeesWhenTheHolderClaimedAndLastSavedButOnlyTheHolderSeesTheDraft`; `GroupControllersTest` theo constructor mới; `GroupWorkspacePage.test.tsx` thêm hỏi lại khi sửa mục người khác, mục của mình không hỏi, dòng người giữ, tải lại mỗi phút, nhắc hạn đúng hạn và trong khoảng trễ, `beforeunload` và nhắc hạn trong trình soạn.

## Sidebar mục lục cho tài liệu nhóm (2026-11-09)

Nhánh `feat/u12-section-ux`. Chỉ frontend.

- `features/groupdocs/components/groupDocNav.tsx` (`groupDocContext`) đăng ký ở `register.ts` cho Sinh viên, Giảng viên, Chủ nhiệm môn: ở `/group-docs/{id}` sidebar là `AssignmentNav` của U07 (đã `export`), cùng kiểu trang làm bài cá nhân; không có nút "+" (BR-U12-30).
- `GroupWorkspacePage` đăng dàn ý `outlineOf(sharedBlocks)` qua `publishOutline`; lối về: sinh viên `/my-assignments/{assignmentId}?classId=`, giảng viên `/teaching/classes/{classId}?tab=students`. Bỏ nút Thoát.
- Bấm dòng (`document:show-part`): về thẻ Tài liệu, cuộn tới mục chứa heading (`model/outline.ts` `findSectionForHeading`: chính mục, mục cha gần nhất, hoặc mục đầu tiên sau heading cha). Cuộn trang thì báo `document:current-part` để sidebar sáng đúng mục.
- **Test:** `tests/features/groupdocs/model/outline.test.tsx` (ánh xạ heading → mục, ngữ cảnh sidebar, trang đăng mục lục, lối về, không có "+", bấm dòng cuộn tới mục).

## Sinh viên tự chia nhóm, giảng viên duyệt (2026-11-11)

Nhánh `feat/student-self-grouping` (`--no-track` từ `origin/staging@e78ac7b9`). US-GRP-007…010, [yêu cầu SG-01…09](../../../inception/requirements/2026-11-11-student-self-grouping.md), BR-U12-70…96, F16–F24, [kế hoạch](../../plans/code-generation-plan/2026-11-11-u12-student-self-grouping-code-generation-plan.md). API, JSON và hành vi cũ giữ nguyên; payload `group.membership-changed` chỉ thêm `reason = PROPOSAL_APPROVED` khi nhóm đến từ đề xuất được duyệt.

- **Migration** `groups/V20261111_1800__group_self_formation.sql`: `group_formation_settings`, `group_proposals`, `group_proposal_members`; partial unique index `uq_group_proposal_members_active (class_id, account_id) where active`; role `app` chỉ SELECT/INSERT/UPDATE (không xóa, giữ lịch sử).
- **Model** (`groups/model`): `GroupFormationSettings` (đợt mở tính khi đọc/ghi), `GroupProposal`, `ProposalMember`, `ProposalRules`.
- **Repository**: `GroupProposalRepository` (Spring JDBC, CAS `version` của cài đặt, khóa `FOR UPDATE` đề xuất).
- **Service**:
  - `GroupFormationService`: đọc/lưu đợt, audit `GROUP_FORMATION_UPDATED`, mở lại đợt thì lời mời cũ hết hạn.
  - `GroupProposalService`: màn sinh viên, tạo, đổi tên, mời, thu hồi, chấp nhận, từ chối, rời, hủy, gửi duyệt, rút về; dọn lười thành viên không còn hiệu lực.
  - `GroupProposalReviewService`: danh sách cho giảng viên, duyệt một hoặc nhiều (một transaction, một lần tăng `version` bộ nhóm, đề xuất lỗi trả theo id), từ chối.
  - `GroupProposalCleanup`: sinh viên vừa có nhóm chính thức rời đề xuất (gọi trong `ClassGroupService.save` và sau khi duyệt).
  - `ClassGroupService`: thêm `addApprovedGroups`; `apply` trả người vừa được xếp nhóm; so trưởng nhóm bằng `Objects.equals`.
  - `GroupAccess.denyProposal`: `404` kèm audit `ACCESS_DENIED`.
- **API**: `GroupProposalController`, 15 mapping (xem [api-inventory](../../../inception/reverse-engineering/api-inventory.md)); `GroupExceptionHandler` áp cho controller mới.
- **Thông báo**: `EventTypes.GROUP_PROPOSAL_INVITED/SUBMITTED/APPROVED/REJECTED`; `NotificationType` cùng tên, tiêu đề và tóm tắt cố định, không email. Lý do từ chối không nằm trong thông báo (tóm tắt cố định không mang dữ liệu sự kiện); sinh viên xem lý do ở trang Nhóm của tôi. `RecipientResolver` bỏ qua membership-changed có `reason = PROPOSAL_APPROVED`.
- **Frontend** (`features/groups`):
  - `model/types.ts`, `api/index.ts` thêm kiểu và hàm gọi API; `model/proposals.ts` (nhãn, tóm tắt đợt, kiểm form, giờ Việt Nam, thông điệp lỗi).
  - Sinh viên: `GroupProposalSection.tsx` (banner đợt, đề xuất bị từ chối, lời mời, thẻ đề xuất), `ProposalDialogs.tsx` (đặt tên, mời bạn); `MyGroup.tsx` hiện phần này khi chưa có nhóm, thẻ Nhóm của tôi hiện tóm tắt.
  - Giảng viên: `SelfGroupingSettingsDialog.tsx`, `ProposalReviewList.tsx` (duyệt nhiều, từ chối, sinh viên chưa có nhóm) đặt đầu tab Sinh viên trong `ClassGroupsPanel.tsx`.
  - Nhãn audit mới trong `features/audit/model/labels.ts`.
  - Gộp component nhỏ theo nhóm chức năng thay vì mỗi component một tệp như kế hoạch: `FormationBanner`, `InvitationList`, `ProposalCard` nằm trong `GroupProposalSection.tsx`; `RejectProposalDialog`, `UngroupedStudentsList` trong `ProposalReviewList.tsx`; `ProposalNameDialog` dùng chung cho tạo và đổi tên.
- **Test backend**: `ProposalRulesTest`, `GroupProposalRepositoryIntegrationTest` (PostgreSQL thật), `GroupFormationServiceTest`, `GroupProposalServiceTest`, `GroupProposalReviewServiceTest`, `GroupProposalCleanupTest`, `GroupProposalFlowIntegrationTest` (luồng đầy đủ, duyệt nhiều có đề xuất lỗi, lưu bộ nhóm dọn đề xuất, hai lần chấp nhận đồng thời), `GroupProposalControllerTest`, `GroupProposalNotificationTest`; `ClassGroupServiceTest` thêm 3 ca.
- **Test frontend**: `model/proposals.test.ts`, `GroupProposalSection.test.tsx`, `ProposalReviewList.test.tsx`, `SelfGroupingSettingsDialog.test.tsx`; `MyGroup.test.tsx` thêm ca chưa có nhóm.
