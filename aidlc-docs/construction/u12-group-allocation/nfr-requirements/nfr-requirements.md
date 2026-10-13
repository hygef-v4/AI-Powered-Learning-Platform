# U12 Group & Allocation — nfr-requirements

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 16, 17. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-01 | Lưu nhóm của lớp 200 sinh viên, 50 nhóm ≤ 1 s. | BR-U12-07 |
| NFR-U12-02 | `groupOf`/`leaderOf` p95 ≤ 20 ms (U14 gọi mỗi lần nhận mục/nộp), gồm kiểm ghi danh `ACTIVE`. | GroupMembershipPort, BR-U12-15 |
| NFR-U12-03 | `my-group` (UC 16) p95 ≤ 300 ms, gồm đọc danh sách tài liệu nhóm từ U14. | BR-U12-30 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-10 | Lưu nhóm của lớp trong một transaction có khóa theo lớp (advisory lock) và khóa lạc quan `version` trên từng nhóm; lệch → `409`. | BR-U12-07 |
| NFR-U12-11 | Một sinh viên tối đa một nhóm đang hiệu lực trong lớp (kiểm trong transaction có khóa theo lớp); một yêu cầu `PENDING` mỗi nhóm. | BR-U12-02, 10 |
| NFR-U12-12 | Yêu cầu đổi trưởng nhóm không bị xóa (`REVOKE DELETE`); lịch sử thành viên (thêm, gỡ, đổi trưởng nhóm) nằm trong audit. | BR-U12-08, 32 |
| NFR-U12-13 | Chia ngẫu nhiên dùng `SecureRandom`; kết quả là bản xem trước, chỉ ghi khi giảng viên lưu. | BR-U12-05 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-20 | Chỉ giảng viên lớp (R3/R4, kiểm giảng viên chính ở server) sửa nhóm/duyệt yêu cầu; sinh viên (R5) chỉ xem nhóm của mình và gửi/hủy yêu cầu của mình; Admin và Chủ nhiệm môn không dạy lớp bị từ chối; ngoài quyền `404`. | SEC-002, current SRS contract |
| NFR-U12-21 | Lý do yêu cầu 10-1000 ký tự, lý do từ chối ≤ 1000 ký tự, hiển thị dạng văn bản thuần. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-30 | Unit test mọi `BR-U12-xx`; chia ngẫu nhiên 7 người sĩ số 3 → 3/2/2, giữ nhóm cũ; yêu cầu không đề cử phải chọn trưởng nhóm khi duyệt; thành viên đã rời lớp bị bỏ qua. | NFR-004 |
| NFR-U12-31 | Kịch bản integration test chuyển cho tester (unit không viết): hai giảng viên lưu cùng lúc → một `409`; bỏ thành viên đang nhận mục thì U14 nhả khóa (khi có U14). | NFR-004, quyết định 2026-10-05 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | NFR-U12-21 |
| SECURITY-08 | Compliant | NFR-U12-20 |
| SECURITY-15 | Compliant | NFR-U12-10 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |

## Quy tắc tích hợp revision hiện hành

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.
