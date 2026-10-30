# U08 Assignment Work — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Danh sách và chi tiết bài (UC 37, 38)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-01 | Chỉ Student (R5) dùng màn và API làm bài; Teacher, Subject Manager, Admin không có. Người học thấy bài theo BR-U07-37: bài `OPEN`, `CLOSED` của lớp `OPEN` mình đang ghi danh, gồm bài của lớp và bài của môn chứa lớp; bài `RETIRED` chỉ hiện khi mình đã có lượt nộp. Quiz không nằm trong danh sách này. | FR-007, UC 37 |
| BR-U08-02 | Student Assignments là danh sách gộp các lớp `OPEN` đang học, lọc theo lớp (mở từ Student Class Detail thì lọc sẵn lớp đó), hình thức, chế độ và trạng thái. Mỗi dòng: tiêu đề, lớp, nhãn "Của môn"; Hình thức và Chế độ là hai cột riêng; hạn; trạng thái của mình (chưa làm, đang làm, đã nộp, nộp trễ, hết hạn; bài nhóm lấy từ U12). Điểm đã công bố và phân bố ẩn danh do U09 gắn vào. | UC 37, US-RPT-001 |
| BR-U08-03 | Assignment Detail là trang thông tin trước khi làm bài (yêu cầu 2026-10-21): lớp, hình thức, chế độ (không ghi tổng điểm), số phần cần làm, sơ đồ cần có, mở lúc, hạn nộp, nộp trễ, số lượt đã dùng và còn lại, lượt gần nhất, thông báo theo dõi hoạt động nếu bật; tài liệu tham khảo gắn kèm do U13 hiện qua slot `AssignmentMaterialSection`. Không hiện hướng dẫn, khung, rubric: đề chỉ mở khi bấm Bắt đầu hoặc Tiếp tục. Nút Bắt đầu hoặc Tiếp tục chỉ hiện khi đủ BR-U08-10, 11; bài nhóm có nút mở bài nhóm do U12 gắn. | UC 38 |

## 2. Lượt làm (UC 39)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-10 | Bắt đầu: người học ghi danh `ACTIVE` của lớp `OPEN`; bài cá nhân thuộc lớp đó hoặc là bài của môn chứa lớp; `submissionWindow` của U07 là `ON_TIME` hoặc `LATE`. | US-ASM-003 |
| BR-U08-11 | Bấm Bắt đầu tạo lượt và tính vào số lượt (`maxAttempts` 1–10 của bài); hết lượt thì từ chối. Mỗi người tối đa một lượt `IN_PROGRESS` mỗi bài; bấm lại thì mở lượt đang làm. Lượt ghi `classId` là lớp người học làm bài (với bài của môn là lớp người học đang ghi danh trong môn) để giảng viên lớp đó chấm. | FR-007 |
| BR-U08-12 | Lượt chụp version bài, chế độ, hạn (`closesAt`, `lateUntil`), `requiredDiagrams` và chính sách theo dõi hoạt động. Hạn của lượt (`deadlineAt`) là `lateUntil` nếu có, không thì `closesAt`; bài cá nhân không giới hạn giờ làm. Nội dung lượt mới bắt đầu từ khung của đề. | FR-018 |
| BR-U08-13 | Tự lưu 10 giây sau lần sửa cuối và khi rời trang; hiện "Đã lưu lúc …". Lưu kèm `contentVersion`; lệch thì `409`, trang báo "Bài đang mở ở nơi khác, bản đang sửa chưa lưu" kèm Tải lại, không ghi đè im lặng. Lỗi mạng giữ trạng thái chưa lưu và tự thử lại ("Mất kết nối, chưa lưu"); đang lưu thì lần lưu sau chờ lần trước xong. Bài lớn hơn khoảng 60 KB khi rời trang lưu bằng yêu cầu thường thay cho `keepalive`. Thông báo chỉ ghi trạng thái. | FR-018 |
| BR-U08-14 | Lưu kiểm qua `IDocumentModel.validateForSave` của U07 (BR-U07-15): người học chỉ viết trong các phần lá, không thêm tiêu đề; block của người soạn không đổi, trừ sơ đồ trong phần (người học vẽ vào) và bảng trong phần (điền ô, chèn hàng, cột; ô thêm mang cờ `added`, chỉ hàng, cột gồm toàn ô có cờ mới xóa được); sơ đồ luôn dùng được; cả bài ≤ 20 MB. Khi lưu, sơ đồ của đề giữ XML, loại (suy từ hình vẽ) và ảnh xem trước người học vẽ; bảng của đề giữ các hàng người học điền kèm cờ; các trường khác lấy theo khung. Ảnh qua U03 `DOCUMENT_IMAGE`. | BR-U07-15 |
| BR-U08-15 | Bản nháp không phải bài nộp, chỉ chính người học đọc và sửa. Lưu sau `deadlineAt` bị từ chối, trừ lần lưu cuối gửi trong 30 giây ân hạn. | FR-018 |
| BR-U08-16 | Nhập DOCX (≤ 20 MB) vào lượt đang làm qua `IDocx` của U07: block của người học, xem trước rồi chèn vào cuối phần đang chọn (không chọn phần thì cuối bài, luôn thuộc phần cuối) kèm `contentVersion`, không đụng block của người soạn. Người học tải bài của mình ra DOCX. | BR-U07-60, 61 |

## 3. Nộp (UC 39)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-20 | Nộp tay: lượt `IN_PROGRESS` của chính mình, trước `deadlineAt` (cộng 30 giây ân hạn), nội dung qua `validateForSubmit` (khung còn đủ, ô sơ đồ trống của đề đã được vẽ, đủ `requiredDiagrams` theo loại suy từ hình vẽ, sơ đồ `OTHER` bù loại thiếu, có nội dung của người học: chữ, ảnh, sơ đồ, sơ đồ của đề đã vẽ khác đề, bảng của đề đã điền khác đề). Trang kiểm sơ bộ trước (tổng số sơ đồ, có nội dung) và không chặn bài server sẽ nhận. | US-ASM-003 S1 |
| BR-U08-21 | Nộp thì nội dung bất biến; `submittedAt` theo giờ server; `late` khi nộp sau `closesAt`; `receiptHash` là SHA-256 của nội dung. Trong cùng transaction gọi `ISubmissionListener.onSubmitted` để U09 tạo dòng chờ chấm với bài `GRADED`. | FR-018, BR-U03-64 |
| BR-U08-22 | Biên nhận gồm mã lượt, lượt thứ mấy, thời điểm nộp, trễ hay không, mã băm. Nộp lặp (bấm hai lần, mạng gửi lại) trả cùng biên nhận. | UC 39 |
| BR-U08-23 | Tự nộp bản đã lưu gần nhất (kể cả rỗng), không kiểm điều kiện nộp, ghi cảnh báo cho giảng viên: khi hết `deadlineAt` (`AUTO_DEADLINE`, scanner mỗi phút) và khi bài bị ngưng giao (`AUTO_RETIRED`, trong `onRetired`). | FR-007 |

## 4. Lịch sử nộp (UC 40)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-30 | Mỗi lượt giữ nguyên, lượt mới không ghi đè. Bài `GRADED`: lượt được chấm là lượt nộp cuối, giao diện đánh dấu rõ. Bài `PRACTICE`: mỗi lượt riêng, không vào sổ điểm. | FR-007, FR-030 |
| BR-U08-31 | Submission History: danh sách lượt của mình (lượt thứ, thời điểm nộp, trễ, cách nộp, "được chấm"), nội dung chỉ đọc, biên nhận, tải DOCX; điểm và nhận xét đã công bố do U09 gắn vào; bài `PRACTICE` có lịch sử phản hồi AI và nút xin phản hồi. | UC 40 |
| BR-U08-32 | Người học chỉ xem lượt của mình; ID của người khác trả "không tìm thấy". | US-ASM-003 S3 |

## 5. Phản hồi bài Practice (UC 41)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-40 | Chỉ xin phản hồi trên lượt `SUBMITTED` của chính mình, bài `PRACTICE`, khi còn ghi danh `ACTIVE`. Lượt đang làm, bài `GRADED`, dữ liệu ngoài quyền bị từ chối; không có tùy chọn bật phản hồi cho bản nháp. | FR-030, US-ASM-012 S3 |
| BR-U08-41 | Nguồn: API cá nhân (qua `IAiTask`, nguồn `PERSONAL`; Student không có API chung) hoặc agent MCP (đọc qua `read_own_work`, ghi qua `record_private_feedback`). Không có kết nối nào vẫn làm và nộp bài bình thường. | FR-030 |
| BR-U08-42 | Phản hồi gồm nhận xét chung, điểm mạnh, nhận xét từng phần và gợi ý cải thiện, có nhãn "AI ước tính, không phải điểm chính thức"; không có đánh giá theo rubric, không có điểm ước tính (người học không xem rubric, yêu cầu 2026-10-21); không vào sổ điểm, không sửa bài nộp. | FR-030, FR-008 |
| BR-U08-43 | Mỗi phản hồi gắn `submissionHash` của lượt; kết quả có hash khác, lượt không còn thuộc người học, hoặc sai schema thì không lưu. Một lượt có nhiều phản hồi, lưu thành lịch sử kèm provenance. | US-ASM-012 S2, S4 |
| BR-U08-44 | Giới hạn: mỗi lượt tối đa một yêu cầu đang chờ; mỗi người tối đa 20 yêu cầu mỗi ngày (web và MCP cộng chung); `requestRef` chống gửi trùng. API/MCP lấy cùng transaction-scoped advisory lock theo account trước pending/quota check và insert; nhiều attempts/transports không vượt20lượt/ngày. | FR-030 |
| BR-U08-45 | Prompt gồm hướng dẫn, khung và bài nộp dạng chữ (sơ đồ ở dạng XML rút gọn qua `IDocumentModel`); không có rubric, không có dữ liệu người khác, không có key. Tool `read_own_work` cũng không trả rubric. Chỉ chính người học xem phản hồi. | FR-030 |

## 6. Dữ liệu hoạt động (FR-039)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-50 | Chỉ thu khi lượt đã chụp chính sách bật và đang `IN_PROGRESS`; người học đã thấy thông báo trên Assignment Detail trước khi bắt đầu. Chính sách tắt thì không thu gì. | FR-039, US-INT-001 S1 |
| BR-U08-51 | Trình duyệt gửi `SESSION_START`, `HIDDEN`, `VISIBLE` (theo trạng thái hiển thị của trang), `HEARTBEAT` mỗi 60 giây khi trang mở và `SESSION_END` khi rời trang (nếu gửi được); mỗi sự kiện có `sessionId`, `clientEventId`, `sequence`, giờ client. Chuyển vào iframe Draw.io không tính là rời trang. | FR-039 |
| BR-U08-52 | Server kiểm actor là chủ lượt (lượt Quiz và tài liệu nhóm do U11, U12 kiểm rồi gọi `IActivityWrite`), chính sách và trạng thái; bỏ trùng theo `clientEventId`; tối đa 60 sự kiện mỗi phút mỗi công việc; ghi giờ server. | FR-039 |
| BR-U08-53 | Tóm tắt: số lần rời trang, tổng thời gian rời đã biết, các khoảng rời, các khoảng thiếu dữ liệu ghi là "không rõ" (không có heartbeat quá 2 phút, sự kiện lệch thứ tự, giờ client bất thường); trạng thái `DISABLED`, `NO_DATA`, `AVAILABLE`, `EXPIRED`. Không kết luận gian lận, không đổi điểm, không tự nộp. | FR-039, US-INT-001 S2 |
| BR-U08-54 | ActivityService.RETENTION cố định 1 ngày; scanner xóa dữ liệu quá hạn, tối đa 10.000 mỗi lượt. activity.retentionDays không còn là Settings khai báo; không chặn bật monitoring vì chưa đặt setting cũ. Policy/notice và quyền ghi/đọc vẫn được kiểm theo source. | FR-039 |
| BR-U08-55 | Giảng viên xem qua U09 (U09 kiểm phân công lớp rồi gọi `IActivityQuery`); Admin không xem. | US-INT-001 |

## 7. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U08-60 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. Bị gỡ khỏi lớp thì mất quyền lưu, nộp, xin phản hồi ở request tiếp theo. | SEC-002 |
| BR-U08-61 | Audit: `ASSIGNMENT_SUBMITTED` (tay hoặc tự, trễ), `SUBMIT_REJECTED` (quá hạn, hết lượt), `PRACTICE_FEEDBACK_REQUESTED`. Không audit từng lần lưu nháp và từng sự kiện hoạt động. | FR-014 |
| BR-U08-62 | U01, U05, U07 không trả lời thì từ chối (fail closed); AI lỗi không ảnh hưởng bài làm và bài nộp. | BR-U01-93 |
