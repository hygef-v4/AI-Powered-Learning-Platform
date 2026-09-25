# U14 Group Document & Submission - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U14. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-GRP-003 (phần tài liệu nhóm), US-GRP-004, US-GRP-005; hỗ trợ US-GRP-006 (dữ liệu cho U15). **Use case**: UC-GRP-05..07.
- **Thiết kế nguồn**: `construction/u14-group-document-submission/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `ArtifactPort` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật |
| `AssignmentQueryPort`, `isSubmissionOpen`, event `u08.assignment.*` | U08 | Dùng thật |
| `DocumentModelPort`, `DocxExportPort`, `DocumentEditor` | U09 | Dùng thật |
| `GroupMembershipPort`, event `u12.group.membership-changed` | U12 | Dùng thật |
| U14 cung cấp `GroupSubmissionQueryPort`, event `u14.group.submitted` | cho U15, U16 | Các unit đó dùng khi được code |

### Dữ liệu U14 sở hữu

PostgreSQL `group_documents`, `sections`, `section_revisions`, `section_comments`, `group_submissions`; Redis `u14:save:*`; RabbitMQ fanout `u14.realtime`, queue `jobs.u14.auto-submit`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u14/
    api/                GroupDocController, GroupDocSseController, DTO
    application/        GroupDocInitializer, SectionService, GroupSubmitter,
                        GroupSubmissionQueryService
    realtime/           GroupDocEventPublisher, SseHub, RealtimeListener
    domain/             GroupDocument, Section, SectionStatus, SectionRevision,
                        SectionComment, GroupSubmission
    infrastructure/     JPA repository
    worker/             AutoSubmitHandler, AssignmentOpenedListener, RetiredListener
    listener/           MembershipListener
    port/               GroupSubmissionQueryPort
/backend/src/main/resources/db/migration/u14/
/frontend/src/app/learn/group-docs/
/frontend/src/app/teaching/assignments/[id]/group-docs/
/frontend/src/features/group-doc/useGroupDocStream.ts
/contracts/openapi/u14-group-docs.yaml
/contracts/messages/u14-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Cấu hình U14 theo `logical-components.md` §3; Tomcat async/timeout; Nginx route SSE và lưu nháp mục.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain và trạng thái mục (BR-U14-10…15); port `GroupSubmissionQueryPort`.
- [ ] **Bước 3** - `GroupDocInitializer`: dựng tài liệu từ khung theo `workSection` khi bài mở, cho nhóm mới sau đó (F1, P5, BR-U14-01).
- [ ] **Bước 4** - `SectionService`: nhận, lưu nháp, Xong (revision), nhả, mục nhóm, bình luận; UPDATE có điều kiện (F3-F6, P1, BR-U14-02, 10…15).
- [ ] **Bước 5** - Realtime: `GroupDocEventPublisher` (sau commit → fanout), `RealtimeListener` (queue riêng), `SseHub` (heartbeat, timeout, đóng kênh người bị bỏ) (P2, P3, BR-U14-20…22).
- [ ] **Bước 6** - `MembershipListener`: nhả khóa, đóng kênh khi rời nhóm (BR-U14-13).
- [ ] **Bước 7** - `GroupSubmitter` một đường (trưởng nhóm, tự nộp tại hạn, ngừng giao), bản chụp nhất quán, biên nhận, event (F7, P4, BR-U14-30…34).
- [ ] **Bước 8** - `GroupSubmissionQueryService`: bản nộp cuối, mục theo tác giả; xuất DOCX (F8, BR-U14-35, 40).
- [ ] **Bước 9** - Audit theo BR-U14-50.
- [ ] **Bước 10** - Unit test mọi `BR-U14-xx`.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u14-group-document-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_2100__u14_group_documents.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - Integration test: hai người nhận cùng mục; rời nhóm nhả khóa; tự nộp tại hạn và không nộp trùng; hai client SSE nhận đúng sự kiện; `app` không sửa được revision/bản nộp.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 16** - `/contracts/openapi/u14-group-docs.yaml` (gồm SSE) và schema event.
- [ ] **Bước 17** - Controller + DTO + validation; controller SSE.
- [ ] **Bước 18** - Test MockMvc: nhóm khác `404`; người không giữ mục không lưu được; chỉ trưởng nhóm nộp.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `GroupDocumentPage` (`GroupDocHeader`, `SectionOutline`, `SharedBlocksView`, `SectionView` + bình luận, `AddSectionDialog`, `SubmitGroupDialog`), `useGroupDocStream`.
- [ ] **Bước 21** - `SectionWorkPage` (`DocumentEditor` của U09, tự lưu, Xong, Nhả).
- [ ] **Bước 22** - `GroupDocsOverviewPage` cho giảng viên (tiến độ, nhả khóa, xem bản nộp).
- [ ] **Bước 23** - Test frontend: nhận mục bị người khác nhận trước hiện thông báo; sự kiện SSE cập nhật trạng thái mục; mất kết nối thì tải lại.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: luồng bài nhóm (nhận mục → Xong → review → trưởng nhóm nộp), cấu hình SSE qua Nginx.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRP-003 (UC-GRP-05) | 3, 4, 22 |
| US-GRP-004 (UC-GRP-06) | 4, 5, 20, 21 |
| US-GRP-005 (UC-GRP-07) | 5, 7, 20 |
| US-GRP-006 (dữ liệu cho U15) | 8 |

## 5. Ngoài phạm vi

- Chấm và điểm cuối (U15), thông báo (U16), chia nhóm (U12).
