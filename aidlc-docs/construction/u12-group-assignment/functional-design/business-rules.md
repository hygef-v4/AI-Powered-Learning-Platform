# U12 Group Assignment — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006, US-GRP-007, US-GRP-008, US-GRP-009, US-GRP-010. **Requirements:** FR-025, FR-026, FR-018, FR-039; SG-01…09 (2026-11-11).

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Nhóm của lớp (UC 57)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-01 | Teacher chính quản lý group set/member/leader của lớp và leader requests. Ngoại lệ Student leader hiện hành được rename nhóm của mình trong lớp OPEN, có version/name/audit guards; không được sửa toàn group set hay nhóm khác. | UC 57, FR-025 |
| BR-U12-02 | Chỉ sinh viên ghi danh `ACTIVE` của lớp được vào nhóm; mỗi sinh viên tối đa một nhóm trong lớp. Mỗi nhóm có tên (≤ 100, không trùng trong lớp), ít nhất một thành viên và đúng một trưởng nhóm là thành viên. | US-GRP-001 S1, S2 |
| BR-U12-03 | Tạo tay: tạo nhóm, thêm, chuyển, gỡ thành viên, chọn trưởng nhóm. Chia ngẫu nhiên: nhập sĩ số tối đa 2–20; chỉ chia sinh viên chưa có nhóm, giữ nguyên nhóm đã có; các nhóm mới chênh nhau tối đa một người; trưởng nhóm chọn ngẫu nhiên; kết quả là bản xem trước để sửa rồi mới lưu. | US-GRP-001 S1, S3 |
| BR-U12-04 | Lưu cả bộ nhóm của lớp một lần kèm `version`; có chỗ mâu thuẫn thì từ chối toàn bộ và báo từng lỗi. | US-GRP-001 S2 |
| BR-U12-05 | Chỉ xóa nhóm chưa có tài liệu nhóm có nội dung hoặc bản nộp; nhóm đã dùng chỉ chuyển thành viên. Gỡ thành viên: mục người đó đang giữ được nhả (bỏ bản nháp chưa Xong), nội dung đã Xong giữ nguyên kèm tên tác giả; bản nộp đã có không đổi. | FR-026 |
| BR-U12-06 | Sửa nhóm, gửi và duyệt yêu cầu đổi trưởng nhóm chỉ khi lớp `DRAFT` hoặc `OPEN`; lớp `ARCHIVED` chỉ xem. Sinh viên chỉ thao tác ở lớp `OPEN`. | BR-U05-16 |
| BR-U12-07 | Sinh viên bị gỡ khỏi lớp không còn là thành viên hiệu lực: không tính vào sẵn sàng, không vào được tài liệu nhóm; tab Students hiện "Đã rời lớp"; lần lưu sau xóa họ khỏi nhóm. Người đó là trưởng nhóm thì nhóm bị coi là thiếu trưởng nhóm tới khi giảng viên chọn người mới; yêu cầu đang chờ của họ tự hủy. | BR-U05-25 |

## 2. Trưởng nhóm (UC 55, 57)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-10 | Thành viên kể cả leader gửi yêu cầu đổi leader, lý do tùy chọn tối đa 1.000 ký tự; null lưu chuỗi rỗng. Có thể đề cử thành viên khác leader hiện tại. Mỗi nhóm tối đa một pending request; người gửi hủy khi pending. | UC 55, US-GRP-002 |
| BR-U12-11 | Chỉ giảng viên chính của lớp duyệt/từ chối request pending. Duyệt cần leader mới hợp lệ; từ chối không bắt buộc lý do, trống thành null, nếu có tối đa 1.000 ký tự. Scope/state vẫn kiểm backend. | US-GRP-002 S1, S2 |
| BR-U12-12 | Giảng viên đổi trưởng nhóm trực tiếp thì yêu cầu đang chờ của nhóm tự hủy. Trưởng nhóm rời nhóm thì giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | FR-025 |
| BR-U12-13 | Đổi trưởng nhóm chuyển quyền giao mục và nộp bài sang người mới; không đổi mục ai đang giữ. | US-GRP-002 S1 |

## 3. Bài nhóm (UC 53)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-20 | Bài nhóm là Assignment `submissionMode = GROUP` của U07: chỉ bài của lớp, chỉ `GRADED` (D02), sơ đồ tùy chọn; soạn đề, khung, rubric bằng Assignment Form của U07. U12 thêm phần nhóm trên form (slot `AssignmentFormSection`): số nhóm, trạng thái sẵn sàng, lối vào tab Students. | UC 53, US-GRP-003 |
| BR-U12-21 | Giảng viên không giao phần cho từng sinh viên; mỗi phần của khung (heading nhỏ nhất của mỗi nhánh, U07) thành một mục, trưởng nhóm giao mục trong tài liệu nhóm. | Người dùng chốt 2026-10-04 |
| BR-U12-22 | Sẵn sàng phát hành (`IGroupAssignmentSupport.checkReadiness`): lỗi chặn `CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`, `GROUP_WITHOUT_MEMBER` (tính theo thành viên hiệu lực); cảnh báo `STUDENT_WITHOUT_GROUP`, giảng viên xác nhận thì vẫn phát hành. | FR-025 |

## 4. Tài liệu nhóm (UC 56)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-30 | Khi bài mở, mỗi nhóm của lớp có một tài liệu nhóm dựng từ khung: mỗi phần thành một mục khóa theo `partId`; heading cha và block ngoài phần là phần chung, chỉ đọc. Nhóm tạo sau khi bài mở cũng có tài liệu. Không ai thêm, xóa, đổi tên hay di chuyển mục. | UC 56 |
| BR-U12-31 | Thành viên hiệu lực mở workspace, nhận, làm, xong, nhả mục của mình; trưởng nhóm thêm quyền giao mục và nộp. Giảng viên chính của lớp xem tiến độ, bản nộp và nhả khóa mục; không sửa nội dung, không giao, không nộp thay. Nhóm khác, tài khoản ngoài lớp, Admin nhận `404`. | SEC-002 |
| BR-U12-32 | Mọi thao tác sửa chỉ khi tài liệu `IN_PROGRESS` và bài còn nhận bài nộp (`submissionWindow` là `ON_TIME` hoặc `LATE`); lần lưu nháp cuối trong 30 giây ân hạn vẫn nhận; bài bị ngưng giao thì đóng ngay. | FR-007 |
| BR-U12-33 | Trưởng nhóm giao một mục cho một thành viên: mục `CLAIMED` cho người đó, ghi người giao; mục đang do người khác giữ thì nhả trước (có cảnh báo). Thành viên tự nhận mục `OPEN` hoặc `DONE`; một người giữ được nhiều mục; mỗi mục một người sửa tại một thời điểm. | FR-026, US-GRP-004 S1 |
| BR-U12-34 | Người giữ mở mục trong popup che kín trang: hiện các heading trên nhánh của phần kèm nội dung của chúng và nội dung phần, không hiện nhánh khác; trình soạn như bài cá nhân (block khung khóa). Chỉ popup này hiện theo nhánh; workspace và các màn khác hiện toàn bộ tài liệu. Tự lưu bản nháp 10 giây sau lần sửa cuối, kèm `version` của mục; chỉ người giữ thấy bản nháp. | Người dùng chốt 2026-10-04 |
| BR-U12-35 | Lưu nháp làm sạch nội dung như bài cá nhân (`IDocumentModel.sanitize`) và giữ `fileId` của ảnh theo bản trước, không nhận `fileId` do trình duyệt gửi. Phần chung, mục và bản nộp trả về trình duyệt không có `fileId`; ảnh tải qua `GET /api/v1/group-docs/{id}/blocks/{blockId}/image` (302 tới link tải 5 phút của U03), cho người đọc được tài liệu với ảnh của phần chung, nội dung đã đăng, bản nộp và bản nháp của chính mình; không thấy block thì 404 `IMAGE_NOT_FOUND`. Bấm Xong: kiểm block qua `IDocumentModel`; nội dung nháp thành nội dung đã đăng; thêm một bản lịch sử (tác giả, thời điểm); mục `DONE`, bỏ khóa; đẩy cập nhật realtime. | US-GRP-004 S2 |
| BR-U12-36 | Nhả khóa: người giữ, trưởng nhóm hoặc giảng viên chính của lớp; người giữ rời nhóm thì tự nhả. Cảnh báo "bản nháp chưa Xong sẽ bị bỏ"; bản nháp bị xóa; mục về `DONE` nếu đã có nội dung đăng, không thì `OPEN`. | FR-026 |
| BR-U12-37 | Realtime qua SSE: thành viên đang mở workspace nhận sự kiện giao, nhận, nhả, xong, nộp, ngưng giao trong ≤ 2 giây; chỉ đẩy thay đổi đã Xong, không đồng bộ từng phím gõ; mất kết nối thì tải lại toàn bộ khi kết nối lại. Trình duyệt đóng hẳn kết nối (lỗi HTTP như 401 khi hết phiên) thì frontend làm mới phiên rồi nối lại với backoff 1, 2, 4… giây, tối đa 30 giây. | US-GRP-005 |
| BR-U12-38 | Bài bật theo dõi: workspace của từng thành viên gửi sự kiện qua `useActivityTracker`; U12 kiểm thành viên, tài liệu `IN_PROGRESS`, chính sách rồi gọi `IActivityWrite` (loại `GROUP_DOCUMENT`, theo từng thành viên). | FR-039 |

## 5. Nộp (UC 56)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-40 | Chỉ trưởng nhóm hiện tại nộp, bất kỳ lúc nào bài còn nhận bài nộp; thành viên khác không nộp thay. Còn mục chưa Xong thì cảnh báo nhưng vẫn cho nộp, phần chưa Xong không vào bản nộp; tài liệu phải qua `validateForSubmit` (đủ sơ đồ bắt buộc, sơ đồ không rỗng). | US-GRP-005 S1 |
| BR-U12-41 | Nộp chụp phần chung, nội dung đã đăng của các mục và tác giả từng mục thành bản bất biến kèm `receiptHash`, `late` nếu sau `closesAt`. Trong cùng transaction gọi `IGroupEvaluation.onGroupSubmitted` của U09; sau commit phát `group.submitted`. | US-GRP-005 S1, BR-U03-64 |
| BR-U12-42 | Trưởng nhóm nộp lại được trước hạn; bản nộp cuối thay bản trước. U09 dùng lại bản chấm nhóm/thành viên, reset điểm về PENDING kể cả đã chốt/công bố; điểm trước không còn hiệu lực. Người nộp, thời điểm, mã băm vẫn ghi audit. Sau nộp vẫn sửa được tới hạn. Không lưu nhiều lượt chấm nhóm song song, không đổi DB schema. | FR-026, quyết định mới nhất 2026-11-02, BR-U09-50 |
| BR-U12-43 | Tự nộp khi quá hạn cuối nhận bài 30 giây hoặc khi bài bị ngưng giao, nếu có thay đổi sau lần nộp cuối: mục đang `CLAIMED` được chốt bằng bản nháp đã lưu gần nhất (thêm lịch sử, `DONE`); chụp như BR-U12-41 với `AUTO_DEADLINE` hoặc `AUTO_RETIRED`; tài liệu `CLOSED`, chỉ đọc. Trễ của bản tự nộp tính theo lần sửa cuối của nhóm (`lastChangedAt`) so với `closesAt`, không theo giờ chạy tự nộp, giống bài cá nhân. Không phụ thuộc AI. Người đang mở workspace thấy trang khóa "Đang nộp…" rồi chuyển sang bản nộp. | US-GRP-005 S2 |
| BR-U12-44 | Workspace có thẻ Bản nộp: bản nộp cuối với thời điểm, người nộp, cách nộp, trễ, biên nhận và tác giả từng mục. Thành viên tải DOCX bản hiện tại hoặc bản nộp; giảng viên chính của lớp tải DOCX bản nộp. | UC 56 |

## 6. Xem nhóm và hỗ trợ màn khác (UC 54)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-50 | My Group hiện nhóm/thành viên/leader và link chung tới Assignment nhóm. Source hiện tại không render từng work/progress hoặc myRequests/rejection reason trên màn này; request được xử lý qua dialog/service. Đây là UI hiện hành, không hứa các trường hiển thị đã bỏ. | UC 54, US-GRP-001 S4 |
| BR-U12-51 | Student Assignments (U08) hiện trạng thái bài nhóm qua `IGroupWorkStatus`; Assignment Detail có nút mở workspace (slot `GroupWorkEntry`) kèm nhóm, trưởng nhóm, số mục theo trạng thái. Popup Student Detail hiện nhóm của sinh viên (slot `StudentDetailSection`). | UC 37, 38, 23 |
| BR-U12-52 | Giảng viên xem tiến độ từng nhóm theo bài nhóm trong tab Students: số mục xong, đang làm, trống, người giữ, đã nộp, trễ, cách nộp; nhả khóa mục; U09 lấy cùng dữ liệu qua `IGroupSubmissionSource` cho Submission Detail và chấm. | UC 42, FR-026 |

## 7. AI, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-60 | Bài nhóm không có tính năng AI cho người học (bài nhóm chỉ `GRADED` theo D02; D03 bỏ): không phản hồi AI, không tool đọc tài liệu nhóm cho người học. Agent của giảng viên đọc bản nộp nhóm qua tool `read_authorized_group_submission` để đề xuất chấm ở U09 (US-GRP-006). | US-GRP-004 S2, D02 |
| BR-U12-61 | Audit: tạo, sửa, xóa nhóm; chia ngẫu nhiên (khi lưu); đổi trưởng nhóm; gửi, hủy, duyệt, từ chối yêu cầu; giao mục; nhả khóa do trưởng nhóm hoặc giảng viên; nộp (tay, tự). Không audit từng lần tự lưu. | FR-014 |
| BR-U12-62 | Mọi API kiểm quyền phía server, ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. U01, U05, U07 không trả lời thì từ chối (fail closed). Sự kiện phát sau commit. | SEC-002 |

## Submission remediation — 2026-11-10

New untouched group documents have no edit timestamp (repository uses the existing non-null column's epoch sentinel). Claim/release alone do not mark content as edited. Deadline submission closes an untouched document without generating an empty receipt; an existing leader submission is closed and claims released without overwriting it. U09 preserves official group grades on resubmission and supersedes only unofficial old-group member records.

## 8. Sinh viên tự chia nhóm — 2026-11-11

US-GRP-007…010, [SG-01…09](../../../inception/requirements/2026-11-11-student-self-grouping.md). Thực thể và trạng thái ở [domain-entities](domain-entities.md).

### Đợt tự chia nhóm

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-70 | Chỉ giảng viên chính của lớp `DRAFT`/`OPEN` đọc và sửa cài đặt đợt. Bật cần `minSize`, `maxSize` là số nguyên 2–20, `minSize ≤ maxSize`; `deadline` trống hoặc sau thời điểm lưu. Tắt giữ nguyên sĩ số và hạn chót đã lưu. Lưu kèm `version`, lệch thì `409`. Audit `GROUP_FORMATION_UPDATED` với giá trị trước/sau. | SG-01, US-GRP-007 S1, S2 |
| BR-U12-71 | Đợt **mở** khi `enabled`, lớp `OPEN` và (`deadline` trống hoặc `now < deadline`); tính mỗi lần đọc/ghi, không dùng job. Lớp chưa có cài đặt coi như tắt. | SG-01 |
| BR-U12-72 | Khi đợt đóng: sinh viên không tạo, đổi tên, mời, chấp nhận, gửi duyệt được (`FORMATION_CLOSED`); vẫn rời, rút về, hủy đề xuất được. Lời mời `INVITED` hiển thị là hết hạn và chuyển `EXPIRED` ở lần ghi kế tiếp chạm tới đề xuất hoặc khi đợt được mở lại. Đề xuất `SUBMITTED` vẫn duyệt/từ chối được; `DRAFT` được giữ. | SG-01, US-GRP-007 S3 |
| BR-U12-73 | Đổi sĩ số khi đợt đang bật không đổi đề xuất đã có; đề xuất `SUBMITTED` không còn thỏa sĩ số mới hiện cảnh báo cho giảng viên, vẫn duyệt được. | SG-01 |

### Đề xuất và lời mời

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-74 | **Sinh viên hợp lệ** cho đề xuất: ghi danh `ACTIVE` trong lớp, chưa ở nhóm chính thức nào của lớp. Sinh viên không hợp lệ không tạo, được mời hay chấp nhận (`STUDENT_ALREADY_GROUPED`, hoặc `404` nếu ngoài lớp). | SG-02 |
| BR-U12-75 | Tạo đề xuất khi đợt mở, người tạo hợp lệ và chưa ở đề xuất đang hoạt động (`STUDENT_IN_PROPOSAL`). Tên cắt khoảng trắng, 1–100 ký tự (`PROPOSAL_NAME_INVALID`), không trùng (không phân biệt hoa thường) tên nhóm chính thức hay đề xuất đang hoạt động khác của lớp (`GROUP_NAME_DUPLICATE`). Người tạo là trưởng nhóm, thành viên `ACCEPTED`. | SG-02, US-GRP-008 S1 |
| BR-U12-76 | Trưởng nhóm đổi tên đề xuất `DRAFT` khi đợt mở, cùng kiểm như BR-U12-75. | Quyết định Q6 |
| BR-U12-77 | Trưởng nhóm mời khi đề xuất `DRAFT` và đợt mở; người được mời hợp lệ, khác người đã `ACCEPTED`/`INVITED` trong đề xuất; số `ACCEPTED` cộng `INVITED` sau khi mời không vượt `maxSize` (`PROPOSAL_FULL`). Người đang ở đề xuất khác vẫn được mời (họ phải rời trước khi chấp nhận). Trưởng nhóm thu hồi lời mời `INVITED` (`REVOKED`). | SG-02, US-GRP-008 S1 |
| BR-U12-78 | Người được mời chấp nhận khi đợt mở, đề xuất `DRAFT`, lời mời `INVITED`, người đó hợp lệ và không ở đề xuất đang hoạt động khác (`STUDENT_IN_PROPOSAL`; database bảo đảm khi đồng thời). Chấp nhận thì `ACCEPTED`, `active`; mọi lời mời `INVITED` khác của người đó trong lớp thành `REVOKED`. Từ chối thì `DECLINED`, được khi đề xuất còn hoạt động. | SG-02, US-GRP-008 S2, S3 |
| BR-U12-79 | Thành viên không phải trưởng nhóm rời đề xuất `DRAFT` bất kỳ lúc nào (`LEFT`). Trưởng nhóm không rời; trưởng nhóm hủy đề xuất `DRAFT` (`CANCELLED`, `LEADER_CANCELLED`): thành viên thành không hoạt động, lời mời `INVITED` thành `REVOKED`. | SG-02, US-GRP-008 S4 |

### Gửi duyệt

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-80 | Trưởng nhóm gửi đề xuất `DRAFT` khi đợt mở và số thành viên hiệu lực (`ACCEPTED`, ghi danh `ACTIVE`, chưa có nhóm chính thức) ≥ `minSize` (`PROPOSAL_BELOW_MIN`). Gửi thì `SUBMITTED`, `submittedAt`; lời mời `INVITED` thành `REVOKED`; phát `group.proposal-submitted`; audit `GROUP_PROPOSAL_SUBMITTED`. | SG-03, US-GRP-009 S1, S2 |
| BR-U12-81 | Đề xuất `SUBMITTED` bị khóa: không đổi tên, mời, rời. Trưởng nhóm rút về `DRAFT` bất kỳ lúc nào trước khi giảng viên quyết định. | SG-03, US-GRP-009 S3 |

### Duyệt và từ chối

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-82 | Chỉ giảng viên chính của lớp `DRAFT`/`OPEN` duyệt/từ chối, chỉ đề xuất `SUBMITTED` của lớp đó (khác lớp `404`, khác trạng thái `PROPOSAL_NOT_SUBMITTED`). | SG-04, US-GRP-010 S4 |
| BR-U12-83 | Duyệt dùng lõi lưu bộ nhóm (BR-U12-02, 04): bộ nhóm hiện tại cộng một nhóm mới (tên, trưởng nhóm, thành viên hiệu lực của đề xuất), kiểm `GroupSetRules`; tạo nhóm, tài liệu cho bài nhóm đang mở, audit `GROUP_CREATED`, sự kiện membership với `reason = PROPOSAL_APPROVED`. Đề xuất `APPROVED`, `approvedGroupId`, `decidedBy/At`; thành viên không hoạt động; phát `group.proposal-approved`; audit `GROUP_PROPOSAL_APPROVED`. | SG-04, US-GRP-010 S1 |
| BR-U12-84 | Duyệt nhiều (tối đa 100 id, không trùng): một transaction, đọc `version` bộ nhóm, tăng một lần. Kiểm lần lượt theo thứ tự gửi, mỗi đề xuất trên bộ nhóm đã cộng các đề xuất hợp lệ trước. Đề xuất lỗi được bỏ qua, trả mã lỗi theo id, giữ `SUBMITTED` (trừ BR-U12-85). Có ít nhất một hợp lệ thì ghi; không có thì không đổi `version`. Bộ nhóm bị lưu đồng thời thì cả lần duyệt `409 GROUP_SET_VERSION_CONFLICT`, không ghi gì. | SG-04, quyết định Q1, Q2 |
| BR-U12-85 | Lúc duyệt chỉ tính thành viên hiệu lực. Thành viên không hiệu lực (rời lớp, đã vào nhóm chính thức) bị chuyển `LEFT`. Trưởng nhóm không hiệu lực hoặc còn dưới `minSize` thì đề xuất lỗi `PROPOSAL_INCOMPLETE` và về `DRAFT`. Tên trùng nhóm chính thức thì lỗi `GROUP_NAME_DUPLICATE`, giữ `SUBMITTED`. | Quyết định Q3, Q6 |
| BR-U12-86 | Từ chối: lý do tùy chọn, cắt khoảng trắng, trống thành null, tối đa 1.000 ký tự. Đề xuất `REJECTED`, thành viên không hoạt động; phát `group.proposal-rejected`; audit `GROUP_PROPOSAL_REJECTED`. Sinh viên thấy đề xuất bị từ chối gần nhất kèm lý do tới khi tạo hoặc vào đề xuất khác. | SG-04, US-GRP-010 S2, quyết định Q5 |
| BR-U12-87 | Nhóm đã duyệt là nhóm chính thức bình thường (BR-U12-01…13); sinh viên không tự đổi thành viên. | SG-06 |

### Tương tác với bộ nhóm chính thức

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-88 | Giảng viên lưu bộ nhóm (BR-U12-04) bất kỳ lúc nào. Trong cùng transaction, mỗi sinh viên vừa có nhóm chính thức: dòng `ACCEPTED` hoạt động thành `LEFT`, lời mời `INVITED` thành `REVOKED`. Người đó là trưởng nhóm đề xuất thì đề xuất `CANCELLED` (`LEADER_PLACED`). Đề xuất `SUBMITTED` còn dưới `minSize` thành viên hiệu lực thì về `DRAFT`. | SG-07, US-GRP-010 S3 |
| BR-U12-89 | Sinh viên rời lớp không có listener: khi đọc, họ không tính là thành viên hiệu lực; lần ghi kế tiếp chạm tới đề xuất (kể cả duyệt) chuyển họ `LEFT`. Trưởng nhóm rời lớp thì đề xuất được coi và ghi `CANCELLED` (`LEADER_LEFT_CLASS`) ở lần ghi kế tiếp. | BR-U12-07, quyết định Q3 |
| BR-U12-90 | Danh sách cho giảng viên: đề xuất đang hoạt động (tên, trưởng nhóm, thành viên hiệu lực, số lời mời chờ, trạng thái, cảnh báo sĩ số) và sinh viên `ACTIVE` chưa có nhóm kèm đề xuất họ đang ở; chỉ `SUBMITTED` có nút duyệt/từ chối. | SG-05, US-GRP-010 S3 |

### Quyền, dữ liệu, audit, thông báo

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-91 | Mọi kiểm ở backend (SECURITY-08): sinh viên chỉ thao tác lớp mình ghi danh `ACTIVE`, lớp `OPEN`; chỉ trưởng nhóm thao tác đề xuất của mình; chỉ người được mời trả lời lời mời; người không liên quan nhận `404` và audit `ACCESS_DENIED` như BR-U12-62. Sinh viên chỉ thấy đề xuất mình là thành viên hoặc được mời. | SG NFR |
| BR-U12-92 | Danh sách "bạn mời được" chỉ trả khi sinh viên là trưởng nhóm đề xuất `DRAFT` và đợt mở: sinh viên hợp lệ của cùng lớp, chỉ id, họ tên, email trường và cờ đang ở đề xuất khác; không trả thông tin khác. | SG-02, SECURITY-08 |
| BR-U12-93 | Đầu vào (SECURITY-05): id dạng UUID; tên 1–100; lý do ≤ 1.000; sĩ số số nguyên 2–20; hạn chót thời điểm ISO-8601; danh sách id không trùng, tối đa `maxSize` (mời) hoặc 100 (duyệt). Truy vấn tham số hóa. | SECURITY-05 |
| BR-U12-94 | Audit: `GROUP_FORMATION_UPDATED`, `GROUP_PROPOSAL_SUBMITTED`, `GROUP_PROPOSAL_APPROVED`, `GROUP_PROPOSAL_REJECTED`, cùng `GROUP_CREATED` của nhóm được tạo. Không audit tạo nháp, mời, chấp nhận, rời. Không ghi họ tên, email hay lý do vào log ứng dụng (SECURITY-03). | FR-014, SG NFR |
| BR-U12-95 | Thông báo chỉ bốn loại ở bảng sự kiện; thay đổi do hệ thống (BR-U12-85, 88, 89) không thông báo. Sự kiện phát sau commit; lỗi gửi thông báo không hoàn tác thao tác (RESILIENCY-10, theo cơ chế sự kiện hiện có). | SG-08, quyết định Q7, Q8 |
| BR-U12-96 | Mọi thao tác ghi lên đề xuất là một transaction; lỗi thì hoàn tác toàn bộ, trả lỗi chung không lộ chi tiết nội bộ (SECURITY-15). Thao tác trưởng nhóm gửi `version` đề xuất, lệch thì `409 PROPOSAL_VERSION_CONFLICT`. | SECURITY-15 |

## 9. Mã số sinh viên (MSSV) — 2026-11-13

Nguồn: [yêu cầu SC-10, SC-11](../../../inception/requirements/2026-11-13-student-code.md); [FD Q6, Q7](../../plans/functional-design-plan/2026-11-13-u01-student-code-functional-design-plan.md). MSSV do U01 sở hữu (BR-U01-100…115).

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-97 | Các DTO người dùng của U12 có thêm trường `studentCode`, có thể rỗng: `ClassGroupService.PersonView`, `ProposalPeople.Person`, `GroupProposalService.MemberView` và DTO ứng viên mời. MSSV chỉ hiện trên những màn vốn đã hiện người đó. Phía giảng viên gồm danh sách nhóm, đề xuất chờ duyệt và sinh viên chưa có nhóm. Phía sinh viên gồm nhóm của tôi, thành viên và lời mời của đề xuất, và danh sách ứng viên mời. Phạm vi ứng viên và quyền xem giữ nguyên. | SC-10, SC-11 |
| BR-U12-98 | Ô tìm bạn để mời vào đề xuất nhóm khớp theo tên, email hoặc MSSV, không phân biệt hoa thường. Hiện nay danh sách ứng viên được lọc ở frontend, nên chỉ cần thêm MSSV vào dữ liệu ứng viên và vào điều kiện lọc. Không thêm API mới. | SC-11, FD Q7 |

**Giao diện**:
- `ClassGroupsPanel`, `GroupFormDialog`, `ProposalReviewList`, `MyGroup`, `GroupProposalSection`, `ProposalDialogs`: hiện dòng phụ `SE123456 · email` dưới tên.
- `InviteMembersDialog`: chữ gợi ý ô tìm đổi thành "Tìm theo tên, email hoặc MSSV".
