# U12 Group Assignment — code summary

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
