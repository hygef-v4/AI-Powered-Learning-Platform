# U12 Group Assignment — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Chi tiết hóa ngày 2026-10-20 trước khi code (Part 1).

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005 (US-GRP-006 phần agent đọc bản nộp theo UC 43). **Requirements:** FR-025, FR-026, FR-018, FR-039. **Jira:** epic KAN-28; story KAN-151 (UC 57), KAN-62 (UC 54), KAN-63 (UC 55), KAN-64 (UC 53, phần nhóm), KAN-66 (UC 56).

Thiết kế: [business-logic-model](../../u12-group-assignment/functional-design/business-logic-model.md), [business-rules](../../u12-group-assignment/functional-design/business-rules.md), [domain-entities](../../u12-group-assignment/functional-design/domain-entities.md), [frontend-components](../../u12-group-assignment/functional-design/frontend-components.md), [NFR](../../u12-group-assignment/nfr-requirements/nfr-requirements.md), [NFR design](../../u12-group-assignment/nfr-design/nfr-design-patterns.md), [infrastructure](../../u12-group-assignment/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chi tiết hóa ngày 2026-10-20 (Part 1). Người dùng cho làm tiếp sau U09 cùng quy trình ("hãy code hết đi đã rồi để max2min đẩy lên stagging sau", 2026-10-20); plan được trình bày lại khi review. Nhánh git: `group-assignment` tạo lại từ `staging` ngày 2026-10-21 (U09 của Khuất Quang Hưng đã vào `staging` kèm `IGroupEvaluation`, `IGroupSubmissionSource`; nhánh `grading` cũ không dùng); mỗi bước một commit scope `u12`. Max2Min gộp vào `staging`.

## Phạm vi

| Trong plan này | Không trong plan này |
|---|---|
| UC 57 nhóm của lớp (tạo tay, chia ngẫu nhiên, sửa, xóa, đổi thành viên, duyệt yêu cầu đổi trưởng nhóm) | Tool MCP `read_authorized_group_submission` (agent đọc bản nộp nhóm để AI chấm, UC 43): giao người làm AI chấm bài như phần UC 43 của U09 |
| UC 54, 55 Nhóm của tôi, gửi và hủy yêu cầu đổi trưởng nhóm | Trình soạn `DocumentEditor`, trình vẽ Draw.io của U07 (U07 chưa có code): popup soạn mục dùng trình soạn đoạn văn tạm, thay khi U07 có |
| UC 53 phần nhóm của bài nhóm (kiểm sẵn sàng, tạo tài liệu khi bài mở, tự nộp khi ngưng giao) | Soạn đề, rubric, phát hành bài nhóm (U07) |
| UC 56 tài liệu nhóm: nhận, giao, nhả, lưu nháp, Xong, nộp, tự nộp, bản nộp, DOCX, realtime SSE | `useActivityTracker` của U08 chưa có: backend nhận sự kiện hoạt động, frontend gắn khi U08 có |

## Ngữ cảnh unit

| Mục | Nội dung |
|---|---|
| Package | `groups` (nhóm, thành viên, yêu cầu đổi trưởng nhóm) và `groupdocs` (tài liệu nhóm, mục, nộp, realtime); cùng unit 12 trong `ArchitectureTest` nên gọi trực tiếp nhau |
| Interface cài | `IGroupAssignmentSupport`, `IAssignmentLifecycleListener` (U07); `IGroupWorkStatus` (U08); `IGroupSubmissionSource` (U09); `IJobHandler` `GROUP_DOC_CREATE`, `IPendingSweeper`, `IScheduledScanner` (U03); `ISseHub` (khai báo sẵn ở `groupdocs/contract`) |
| Interface dùng | `IAuthorization`, `IAccountLookup` (U01), `IAudit` (U02), `IJob`, `IEventPublisher` (U03), `IClassAccess` (U05), `IAssignmentQuery`, `IDocumentModel`, `IDocx` (U07), `IActivityWrite` (U08), `IGroupEvaluation` (U09) |
| Dữ liệu sở hữu | 6 bảng của domain-entities và bảng `class_group_sets` (version của bộ nhóm) |
| Slot frontend cài | `teacherClassTabs` tab `students`, `info`; `studentDetailSections`; `studentClassSections` (U05); `assignmentFormSections` (U07); `groupWorkEntries` (U08) |
| Design | Màn #26, #32, #33, #36, #39, #40 theo design G21 bản 17/10 (`t02`, `t02c`–`t02h`, `t15`, `t18`, `t19`, `s02`, `s03`, `s04`, `s12d`, `s18`, `s18b`–`s18f`) |
| Extension | Security Baseline (SECURITY-03, 05, 08, 13, 15), Resiliency Baseline (RESILIENCY-10) |

## Khác biệt phát hiện khi chi tiết hóa

1. **U07, U08 chưa có code.** Khung, phần, hạn nộp, kiểm tài liệu đọc qua contract (bản tạm). Luồng thật (phát hành bài nhóm → tạo tài liệu → làm → nộp → U09 chấm) chỉ thử được khi U07, U08 vào `staging`.
2. **Version của bộ nhóm.** domain-entities ghi "bộ nhóm của lớp có `version` chung" nhưng không có bảng giữ; thêm bảng `class_group_sets (class_id, version)`.
3. **Migration.** `V20260925_1800`; không có FK tới `assignments` (U07 chưa có bảng), U07 thêm sau; chạy được nhờ `out-of-order` đã bật ở U09.
4. **Quyền.** Thêm `Actions.GROUP_MANAGE` (TEACHER, SUBJECT_MANAGER; phạm vi lớp) và `Actions.GROUP_WORK` (STUDENT; phạm vi lớp) vào U01, như U09.
5. **Lưu trữ.** tech-stack ghi JPA; dùng `JdbcClient` như U01–U03, U09; nội dung mục, bản nộp là `jsonb`; khóa dòng mục bằng `SELECT ... FOR UPDATE` (P1).
6. **Realtime.** `RabbitSseHub` cài `ISseHub`: gửi qua exchange fanout `platform.realtime` sau commit; mỗi instance một queue tạm tự xóa, đẩy tới `SseEmitter` của kênh; heartbeat 25 giây (Nginx `proxy_read_timeout` 120 giây) và header `X-Accel-Buffering: no` (Nginx đã ghi chú sẵn), không sửa cấu hình Nginx.
7. **Danh sách bài nhóm cho tab Sinh viên.** Design dùng API của U07; U12 thêm `GET /api/v1/classes/{classId}/group-assignments` lọc bài `GROUP` từ `IAssignmentQuery.listForClass`.
8. **Route.** Next.js đã có `app/classes/[id]`; trang Nhóm của tôi đặt ở `app/classes/[id]/group`; workspace ở `app/group-docs/[id]` kèm layout đăng nhập cho Student, Teacher, Subject Manager.
9. **DOCX.** Xuất qua `IDocx.export` của U07; bản tạm có thể trả tệp rỗng cho tới khi U07 có.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/.../groups/` | 5 tầng, chỉ `.gitkeep` |
| `backend/.../groupdocs/` | 5 tầng; `contract/ISseHub` và bản tạm `GroupDocsContractStubs` (tự tắt khi có bean thật) |
| `backend/.../grading/` (nhánh `grading`) | `IGroupEvaluation` đã cài thật; `IGroupSubmissionSource` còn bản tạm, U12 cài |
| `frontend/src/features/groups/`, `groupdocs/` | Chỉ có `index.ts` trống |

## Checklist

Unit test viết cùng bước tạo code; mỗi bước xong chạy lệnh như CI rồi mới commit.

- [x] **Bước 1 — Migration và quyền:** `db/migration/groups/V20260925_1800__create_group_tables.sql`: `class_group_sets`, `class_groups` (unique `(class_id, name)`), `class_group_members` (unique `(class_id, account_id)`), `leader_change_requests` (unique riêng phần `group_id` với `PENDING`, lý do 10–1.000), `group_documents` (unique `(group_id, assignment_id)`, index `(status, assignment_id)`), `group_document_sections` (unique `(group_document_id, part_id)`), `group_document_revisions` (index `(section_id, created_at)`); FK `course_classes`, `accounts`; `app` có `SELECT, INSERT, UPDATE`, thêm `DELETE` chỉ trên `class_group_members`, `class_groups`. `Actions.GROUP_MANAGE`, `GROUP_WORK` và dòng `AccessPolicy` kèm test. Commit `feat(u12): add group tables and group actions`.
- [x] **Bước 2 — Domain (BR-U12-02, 10, 30, 33–36):** `groups/domain`: `ClassGroup`, `GroupMember`, `LeaderChangeRequest`, enum trạng thái, `GroupSetRules` (mỗi người một nhóm, tên không trùng ≤ 100, trưởng nhóm là thành viên, có thành viên; trả từng lỗi), repository; `groupdocs/domain`: `GroupDocument`, `GroupSection`, `SectionRevision`, enum, `SectionRules` (chuyển trạng thái mục), repository (khóa dòng mục, tài liệu). Unit test quy tắc nhóm và chuyển trạng thái mục. Commit `feat(u12): add group and group document domain`.
- [x] **Bước 3 — Nhóm (F1, F2, F14; BR-U12-01–07; P2):** `ClassGroupService`: đọc bộ nhóm (thành viên đã rời lớp có nhãn, sinh viên chưa có nhóm, yêu cầu chờ, `version`); lưu cả bộ kèm `version` (kiểm toàn bộ trước, một transaction; nhóm có tài liệu có nội dung hoặc bản nộp không xóa được; lớp `ARCHIVED` chỉ xem); đổi trưởng nhóm trực tiếp hủy yêu cầu chờ; người rời hoặc chuyển nhóm khi có bài đang mở: nhả mục đang giữ, nhóm mới có tài liệu; audit; sự kiện `group.membership-changed`, `group.leader-changed` sau commit. `RandomGroupSplitter`: chỉ chia người chưa có nhóm, sĩ số 2–20, chênh tối đa một người, trưởng nhóm ngẫu nhiên, trả bản xem trước. Unit test: bộ mâu thuẫn bị từ chối cả bộ, xóa nhóm đã dùng, chia ngẫu nhiên đều, lớp lưu trữ, `version` lệch. Commit `feat(u12): manage class groups`.
- [x] **Bước 4 — Trưởng nhóm (F3, F5; BR-U12-10–13):** `LeaderRequestService`: gửi (thành viên hiệu lực, lớp `OPEN`, lý do 10–1.000, đề cử là thành viên khác trưởng nhóm, một yêu cầu chờ mỗi nhóm), hủy (chỉ người gửi), duyệt (chọn trưởng nhóm mới, mặc định người được đề cử), từ chối (bắt buộc lý do); audit; sự kiện sau commit. Unit test các nhánh. Commit `feat(u12): handle leader change requests`.
- [x] **Bước 5 — Bài nhóm và tài liệu (F6, F7, F13 bước 2; BR-U12-20–22, 30; P3):** `GroupReadinessService` cài `IGroupAssignmentSupport` (`CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`, `GROUP_WITHOUT_MEMBER`, cảnh báo `STUDENT_WITHOUT_GROUP`); `GroupAssignmentLifecycle` cài `IAssignmentLifecycleListener` (mở bài: gửi job `GROUP_DOC_CREATE`; ngưng giao: đánh dấu tự nộp); `GroupDocumentService.ensureDocuments` dựng tài liệu từ khung và phần (`IAssignmentQuery.getContent`), mỗi phần một mục, phần chung chỉ đọc, `ON CONFLICT DO NOTHING`; `GroupDocCreateJobHandler`, `IPendingSweeper`. Unit test: sẵn sàng, dựng mục từ phần, tạo lại không trùng. Commit `feat(u12): create group documents when a group assignment opens`.
- [x] **Bước 6 — Mục (F8–F11; BR-U12-31–36; P1):** `GroupSectionService`: mở workspace (thành viên hiệu lực hoặc giảng viên của lớp, còn lại `404`); nhận, giao (trưởng nhóm), nhả (người giữ, trưởng nhóm, giảng viên), lưu nháp kèm `version` mục (ân hạn 30 giây, lệch `409`), Xong (kiểm `IDocumentModel`, nháp thành đã đăng, thêm lịch sử); mọi sửa chỉ khi tài liệu `IN_PROGRESS` và bài còn nhận bài; audit giao mục, nhả do trưởng nhóm hoặc giảng viên; đẩy SSE sau commit. Unit test: nhận mục người khác giữ bị từ chối, giao nhả trước, nhả về `DONE`/`OPEN`, nháp lệch `version`, người ngoài nhóm `404`, bài hết hạn. Commit `feat(u12): claim, assign, edit and release group sections`.
- [x] **Bước 7 — Nộp (F12, F13; BR-U12-40–44; P5):** `GroupSubmissionService`: chỉ trưởng nhóm hiện tại, `validateForSubmit`, chụp phần chung và nội dung đã đăng kèm tác giả, `receiptHash`, `late`; gọi `IGroupEvaluation.onGroupSubmitted` trong cùng transaction; audit; `group.submitted`, SSE sau commit; nộp lại trước hạn. `GroupAutoSubmitScanner` (`IScheduledScanner`): tài liệu `IN_PROGRESS` quá hạn cuối 30 giây hoặc bài ngưng giao, có thay đổi sau lần nộp cuối: chốt mục `CLAIMED` bằng bản nháp, chụp `AUTO_DEADLINE`/`AUTO_RETIRED`, `CLOSED`, câu lệnh có điều kiện. Xuất DOCX qua `IDocx`. Unit test: thành viên không nộp được, mục chưa Xong không vào bản nộp, gọi U09 đúng tham số, tự nộp chốt nháp, tự nộp không trùng. Commit `feat(u12): submit group documents manually and on deadline`.
- [ ] **Bước 8 — Realtime (P4; BR-U12-37; NFR-U12-30):** `RabbitSseHub` cài `ISseHub` (fanout `platform.realtime`, queue tạm mỗi instance, gửi sau commit, heartbeat 25 giây, dọn emitter đã đóng); kênh `group-doc:<id>` chỉ mở cho thành viên hiệu lực và giảng viên của lớp. Unit test: phát sau commit, chuyển tới emitter đúng kênh, emitter lỗi bị bỏ. Commit `feat(u12): push group document changes over SSE`.
- [ ] **Bước 9 — Interface cho unit khác (F15):** `GroupWorkQueryService` cài `IGroupWorkStatus` và `IGroupSubmissionSource` (thay bản tạm của U09); ghi hoạt động qua `IActivityWrite` loại `GROUP_DOCUMENT` sau khi kiểm thành viên và tài liệu. Unit test. Commit `feat(u12): expose group work to assignment work and grading`.
- [ ] **Bước 10 — API:** controller theo domain-entities mục 5 và `GET /api/v1/classes/{classId}/group-assignments`; `Actor` từ `@AuthenticationPrincipal`; lỗi là Problem Detail có `code`; ngoài phạm vi `404`. Unit test MockMvc standalone. Commit `feat(u12): expose group REST API`.
- [ ] **Bước 11 — Frontend giảng viên (#32, #33, #26):** `features/groups`: `ClassGroupsPanel` (tab `students`: chưa có nhóm, thẻ nhóm, `GroupEditor`, `RandomSplitDialog`, `LeaderRequestList`, `GroupProgressTable`, `ReleaseSectionDialog`), `GroupCountCard` (tab `info`), `StudentGroupInfo` (popup Student Detail), `GroupAssignmentPanel` (slot `assignmentFormSections`). Test component. Commit `feat(u12): add teacher group screens`.
- [ ] **Bước 12 — Frontend sinh viên (#36, #39, #40):** `MyGroupCard` (slot `studentClassSections`), `MyGroupPage` (`app/classes/[id]/group`), `LeaderChangeRequestDialog`, `OpenGroupWorkButton` (slot `groupWorkEntries`); `features/groupdocs`: `GroupWorkspacePage` (`app/group-docs/[id]`), `SectionList`, `GroupDocumentView`, `SectionEditorDialog` (trình soạn đoạn văn tạm, tự lưu 10 giây), `AssignSectionDialog`, `SubmitGroupDialog`, `GroupSubmissionTab`, `AutoSubmitOverlay`, `useGroupDocStream` (SSE, tải lại khi kết nối lại). Test component. Commit `feat(u12): add student group and group workspace screens`.
- [ ] **Bước 13 — Tài liệu:** `aidlc-docs/construction/u12-group-assignment/code/code-summary.md`; ghi phạm vi tách ra; đánh dấu plan; cập nhật `aidlc-state.md`. Commit `docs(u12): add code summary and record U12 progress`.
- [ ] **Bước 14 — Review:** đối chiếu UC 53–57, story, BR-U12 với code; tóm tắt tuân thủ extension; người dùng duyệt; Max2Min gộp `group-assignment` vào `staging`.

## Integration test cases (tester viết)

- Hai thành viên nhận cùng một mục cùng lúc: chỉ một người được.
- Hết hạn khi có mục đang làm: bản nháp được chốt vào bản nộp, U09 nhận `onGroupSubmitted`.
- Gỡ thành viên đang giữ mục khi bài đang mở: mục được nhả, nội dung đã Xong giữ tác giả.
- SSE: thay đổi trên instance A tới người mở tài liệu ở instance B trong 2 giây.
- Phân quyền 4 role trên mọi endpoint, gồm sinh viên nhóm khác mở tài liệu và giảng viên lớp khác nhả khóa.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
