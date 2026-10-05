# U14 Group Document & Submission - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U14. Mỗi bước xong thì đánh `[x]` ngay.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-GRP-003 (phần tài liệu nhóm), US-GRP-004, US-GRP-005; hỗ trợ US-GRP-006 (dữ liệu cho U15). **Use case**: UC 16; dựng tài liệu nhóm và nhả khóa mục cho UC 27 (U09 chủ trì).
- **Thay đổi 2026-10-04**: tài liệu nhóm ở bảng `group_documents` (một nhóm của lớp × một bài nhóm); mỗi phần của khung là một mục, trưởng nhóm giao mục (không có mục chi tiết); người giữ sửa mục trong popup che kín trang; không có bước review; trưởng nhóm nộp bất kỳ lúc nào trước hạn; hết hạn tự nộp gồm phần đang làm.
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
| `ClassAccessPort` | U04 | Dùng thật |
| `AssignmentQueryPort`, `isSubmissionOpen` | U08 | Dùng thật; U14 cài `AssignmentLifecyclePort` của U08 |
| `DocumentModelPort`, `DocxExportPort`, `DocumentEditor` | U09 | Dùng thật |
| `GroupMembershipPort` | U12 | Dùng thật; U14 cài `GroupChangePort` của U12 |
| U14 cung cấp `GroupSubmissionQueryPort` (U13, U15, U16), event `group.submitted` (U16) | cho U13, U15, U16 | Các unit đó dùng khi được code |
| `GroupSubmittedPort` | U15 (`C`, U14 khai báo) | Adapter rỗng: bản nộp nhóm chưa vào hàng chấm; U15 (ngay sau U14 trong wave 3) cài |

### Dữ liệu U14 sở hữu

PostgreSQL: `group_documents` (mục, lịch sử, bản nộp nằm trong cột JSON của bảng này); Redis `ratelimit:section-save:*`; RabbitMQ fanout `platform.realtime` và queue tạm `jobs.realtime.{instanceId}` mỗi backend; việc `GROUP_DOC_CREATE` trên `jobs.triggered`; `GroupAutoSubmitScanner` đăng ký `ScheduledScanner` (U03).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  groupdocs/
    api/                GroupDocController, GroupDocSseController, DTO
    application/        GroupDocInitializer, SectionService, GroupSubmitter,
                        GroupSubmissionQueryService
    realtime/           GroupDocEventPublisher, SseHub, RealtimeListener
    domain/             GroupDocument, Section, SectionStatus, SectionRevision,
                        GroupSubmission
    infrastructure/     JPA repository, NoopGroupSubmittedAdapter
    worker/             GroupAutoSubmitScanner, GroupDocCreateHandler
    adapter/            AssignmentLifecycleAdapter (U08), GroupChangeAdapter (U12)
    port/               GroupSubmissionQueryPort, GroupSubmittedPort
/backend/src/main/resources/db/migration/groupdocs/
/frontend/src/app/learning/group-docs/
/frontend/src/components/group-docs/ (GroupDocsOverviewPanel)
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
- [ ] **Bước 3** - `GroupDocInitializer`: dựng tài liệu cho mọi nhóm của lớp từ các phần của khung (`config.parts`) khi bài mở, cho nhóm mới sau đó (F1, P5, BR-U14-01).
- [ ] **Bước 4** - `SectionService`: trưởng nhóm giao mục; nhận, lưu nháp, Xong (revision), nhả (xóa bản nháp chưa Xong); khóa dòng `group_documents`; không ai thêm/xóa/sửa mục (F3-F7, P1, BR-U14-02, 05, 10…13, 24).
- [ ] **Bước 5** - Realtime: `GroupDocEventPublisher` (sau commit → fanout), `RealtimeListener` (queue tạm `jobs.realtime.{instanceId}`), `SseHub` phân kênh theo khóa tổng quát để U16 dùng lại (heartbeat, timeout, đóng kênh người bị bỏ) (P2, P3, BR-U14-20…22).
- [ ] **Bước 6** - `GroupChangeAdapter` (`onGroupCreated` gửi việc `GROUP_DOC_CREATE`; `onMemberRemoved` nhả khóa, đóng kênh người rời nhóm) và `AssignmentLifecycleAdapter` (`onOpened` gửi việc `GROUP_DOC_CREATE` cho mọi nhóm) và `GroupAutoSubmitScanner` (tự nộp khi quá hạn hoặc bài `RETIRED`) (F1, F8, P4, BR-U14-13, 33).
- [ ] **Bước 7** - `GroupSubmitter` một đường (trưởng nhóm nộp bất kỳ lúc nào trước hạn; tự nộp tại hạn + 30 giây hoặc ngừng giao, chốt mục đang giữ bằng bản nháp đã lưu, tài liệu `CLOSED`), bản chụp nhất quán, biên nhận, event (F8, P4, BR-U14-30…34, 36).
- [ ] **Bước 8** - `GroupSubmissionQueryService`: bản nộp cuối, mục theo tác giả; xuất DOCX (F9, BR-U14-35, 40).
- [ ] **Bước 9** - Audit theo BR-U14-50.
- [ ] **Bước 10** - Unit test mọi `BR-U14-xx`.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u14-group-document-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_1850__u14_group_workspace.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: hai người nhận cùng mục; giao mục đang có người giữ; mọi mục xong không đổi trạng thái; rời nhóm hoặc nhả bằng tay thì xóa bản nháp chưa Xong, mục về đúng `OPEN`/`DONE`; tự nộp tại hạn lấy bản nháp của mục đang giữ, tài liệu `CLOSED`, không nộp trùng; hai client SSE nhận đúng sự kiện; nộp lại ghi đè `submitted_snapshot` và ghi audit.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 16** - `/contracts/openapi/group-docs.yaml` (gồm SSE) và schema event.
- [ ] **Bước 17** - Controller + DTO + validation; controller SSE.
- [ ] **Bước 18** - Test MockMvc: nhóm khác `404`; người không giữ mục không lưu được; chỉ trưởng nhóm giao mục; không ai thêm/sửa/xóa mục; chỉ trưởng nhóm nộp.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `GroupDocumentPage` (`GroupDocHeader`, `SectionOutline`, `SharedBlocksView`, `SectionView`, `AssignSectionsDialog`, `SubmitGroupDialog`, `AutoSubmitOverlay`), `useGroupDocStream`; `GroupSubmissionView` cho màn Submitted Assignment.
- [ ] **Bước 21** - `SectionEditorDialog` popup che kín trang, hiện các heading trên nhánh của mục cùng nội dung của chúng và các block của mục, không hiện nhánh khác (`DocumentEditor` của U09, tự lưu, Xong, Nhả có hộp xác nhận bỏ bản nháp); Xong đóng popup và workspace cập nhật realtime.
- [ ] **Bước 22** - `GroupDocsOverviewPanel` cho giảng viên, gắn vào Grading Queue (U15) của bài nhóm (tiến độ, nhả khóa, xem bản nộp).
- [ ] **Bước 23** - Test frontend: nhận mục bị người khác nhận trước hiện thông báo; sự kiện SSE cập nhật trạng thái mục; mất kết nối thì tải lại.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: luồng bài nhóm (giảng viên soạn khung, hệ thống tự chia phần, rubric từng phần → trưởng nhóm giao phần → thành viên làm trong popup và Xong → trưởng nhóm nộp, hoặc hết hạn tự nộp), cấu hình SSE qua Nginx.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRP-003 (UC 27, phần tài liệu nhóm; U09 chủ trì) | 3, 4, 22 |
| US-GRP-004 (UC 16) | 4, 5, 20, 21 |
| US-GRP-005 (UC 16) | 5, 7, 20 |
| US-GRP-006 (dữ liệu cho U15) | 8 |

## 5. Ngoài phạm vi

- Chấm và điểm cuối (U15), thông báo (U16), nhóm của lớp (U12).
