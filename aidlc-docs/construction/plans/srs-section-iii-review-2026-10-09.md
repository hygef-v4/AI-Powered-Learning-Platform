# Rà soát SRS — III Functional Requirements

Nguồn: [SRS hiện tại](https://docs.google.com/document/d/1ebPCxJyusasIm8aiMmm3pWiIAaEr7D7n/edit), đọc trực tiếp qua Drive ngày 2026-10-09 sau lần sửa III.7/III.8/IV/V. Đối chiếu `docs/G21_Diagrams.drawio` Page-2, `docs/use-cases-73.md`, current-srs-contract và thiết kế U01/U03/U05/U06/U07/U08/U09/U13/U15/U16. Đây là kết quả review, chưa sửa phần mô tả màn hình trong doc online.

## Kết luận

- III.1–III.6 có đủ 51 màn/popup, Screen #1–#51 liên tục; 69 Figure vì một màn có nhiều view/tab/bước. Có 51 bảng Field Description tương ứng. Không thiếu màn độc lập so với danh mục đã đồng bộ ở I.5.
- Các caption khớp danh mục; điều này không xác nhận nội dung bên trong từng ảnh mockup đã đúng nghiệp vụ.
- Related use cases trong mô tả màn bao phủ 71/73 UC; UC 03 Sign Out và UC 12 View Notifications chưa được mô tả. Hai UC này thuộc shell chung, không cần tăng số màn thành 53.
- Description, action list và Field Description chưa đồng bộ với nhau hoặc với AI-DLC. Sửa mỗi đoạn Description sẽ vẫn để lại lỗi trong bảng field.
- III.7–III.8 đã được đồng bộ ở lượt trước. Kết quả dưới đây tập trung phần màn III.1–III.6.

## Các sai lệch cần sửa

| Vị trí | Bằng chứng trong SRS | Nội dung đúng theo AI-DLC |
|---|---|---|
| III.2.2–2.3, #7–8 Settings | `daily request limit`, `requests per day`, `grant day`, chọn `roles` | AI: kill-switch, trần chi phí ngày, tần suất/phút, model/enabled theo task. Credit: `credit.monthlyFreeCredits` 0–10000, mặc định 100; reset lười theo tháng, không cộng dồn, áp dụng mức mới tại reset tiếp theo; ví chỉ Student/Teacher/Subject Manager. Tệp có cấu hình riêng MATERIAL và DOCUMENT_IMAGE. Save có version/audit; hiệu lực tối đa 30 giây. |
| III.3.2–3.3, #19–20 Questions | Question row có `rubric state`; Question Detail chỉ có loại Quiz/True-False/Short Essay và `2-10 options` | Ngân hàng môn có MCQ_SINGLE/MCQ_MULTI, ESSAY, CODE, DOCUMENT; field phụ thuộc loại. MCQ có 2–6 lựa chọn; không thêm True/False như loại độc lập. Câu ngân hàng không sở hữu rubric. DRAFT sửa trực tiếp; sửa ACTIVE tạo DRAFT mới, không phải mọi save đều tạo version. Cần mô tả ACTIVE/RETIRED, import và AI soạn câu. |
| III.3.4–3.5, #21–22 Quiz | Filter mode, `Practice / Graded`; thiếu bước duyệt/phát hành và cấu hình số lượt trong bảng | Quiz chỉ PRACTICE, gắn đúng một học liệu; không lịch mở/đóng, không Gradebook/hàng chấm/Assignment List. DRAFT → REVIEWED → OPEN → RETIRED; 1–10 lượt hoặc không giới hạn, có timer tùy chọn. Phân biệt quiz môn R2 và quiz lớp R3/R4. |
| III.3.6, #23 Material List | `extraction and embedding run in the background`; `retry scanning`; trạng thái có `Thử lại` | Upload chỉ lưu/trích chữ hoặc phụ đề, không Gemini/HOLD; EXTRACTED chờ người xem yêu cầu. Không có nút Quét lại. Tách trạng thái extraction, summary và indexing; lỗi vẫn xem được tài liệu. |
| III.3.7, #24 Material Detail | `replace the file`, `Thay tệp`, chuyển module; không mô tả summary | Chỉ sửa thông tin cho người có quyền quản lý; không thay tệp/link của lesson đã tạo. Muốn thay nguồn thì xóa/archive học liệu cũ và tạo mới, không tự chuyển quiz. Teacher đọc học liệu môn từ lớp không được sửa nguồn học liệu môn. Bổ sung nút Tóm tắt tài liệu cho Teacher/Subject Manager có quyền xem, trạng thái và kết quả dùng chung. Chức năng chuyển module không được xác nhận bởi BR-U05-11; không giữ như quyền mặc định. |
| III.4.3, #32 Learning Material | Description/action/field chỉ xem, tải và làm quiz | Bổ sung Student bấm Tóm tắt tài liệu ở EXTRACTED; requester trả một HOLD summary+embedding, người khác đọc lại miễn phí. Đang xử lý thì khóa nút; hoàn tất hiện nhãn AI. Thiếu credit/AI off trước nhận giữ EXTRACTED; embedding lỗi giữ summary; không regenerate/rescan. |
| III.3.8, #25 Assignment List | Liệt kê bài/quizzes của các lớp, gồm Group, actor Subject Manager/Teacher, class switch | Đây là list bài của môn từ Manager Dashboard, chỉ Chủ nhiệm môn R2 soạn Text Essay/Diagram Essay/Code Lab, PRACTICE hoặc GRADED; một lịch chung cho mọi lớp OPEN, kể cả lớp mở mới. Không Group/Quiz. Teacher chỉ xem nội dung bài môn và chấm sinh viên lớp mình qua Teacher Class Detail/Submission Detail. |
| III.3.9, #26 Assignment Form | Nội dung mô tả chủ yếu bài của lớp; `preview ... or publish`; language list chỉ Java/Python/C++ | Tách R2 bài môn và R3/R4 bài lớp, no Group cấp môn. Bước Duyệt bắt buộc trước phát hành; rubric thiếu chặn duyệt/phát hành. Code hỗ trợ bảy ngôn ngữ đã chốt. Mô tả kiểm lời giải hiện thời, copy giữa lớp mình dạy, clone/version và các trường hợp không copy lịch/bài nộp/điểm. |
| III.3.10, #27 Rubric Detail | Chỉ thêm tiêu chí/điểm; thiếu checklist, quyền sửa và khóa | Rubric tự tạo trống từng câu Essay hoặc từng leaf Diagram/Group, không version; DRAFT được sửa, khóa khi phát hành. 1–20 tiêu chí, mỗi tiêu chí 1–20 checklist có điểm; tổng tự tính. Không rubric cho Code/Quiz và không rubric của bank question. |
| III.4.2, #31 Student Class Detail | Chỉ nói `class assignments` | Bổ sung bài của môn áp dụng cho lớp, cùng bài lớp. Group chỉ ở lớp, quyền theo enrollment/membership; Quiz có đường riêng, không trộn vào danh sách bài. |
| III.4.4, #33 Class Announcements | Student `comment`; Teacher `hide comments`; bảng `Bình luận` | Bỏ toàn bộ comment/moderation. Chỉ đọc thông báo; giảng viên được giao lớp tạo/sửa/soft-delete có version/audit; chỉ tạo gửi notification. |
| III.4.5, #34 Student Assignments | Chưa nói bài môn, điểm/phân bố, kết quả retired | Gồm bài lớp và bài môn đủ quyền; Quiz không có ở đây. Mô tả published score, bài RETIRED chỉ xem khi có kết quả theo U08, phân bố GRADED khi Subject Manager bật và đủ mẫu (>=20; bins <5 ẩn). |
| III.4.14, #43 Quiz Taking | `time expiry or the deadline` | Quiz không có hạn theo lịch bài tập; timer của lượt nếu cấu hình vẫn được dùng. Không mô tả closes_at/late policy cho Quiz. |
| III.5.1, #45 Teacher Class Detail | `class assignments and quizzes` trong cùng Đánh giá list; upload/delete không tách quyền học liệu môn/lớp | Tách bài tập non-Quiz và entry soạn quiz luyện tập, không gộp Quiz vào Assignment List/hàng chấm. Bài môn read-only authoring, chấm trong lớp mình. Teacher/Subject Manager được giao lớp chỉ quản lý học liệu lớp; học liệu môn đọc/summarize. Teacher không sửa cấu trúc/enrollment/distribution lớp. |
| III.5.3–5.4, #47–48 Grading | Bảng nút `AI đề xuất (5 credit)`; rubric UI quá chung | AI không có giá cố định 5 credit: hiển thị quote, giữ và settle theo usage. Chỉ Essay/Diagram/tài liệu nhóm có AI proposal; Code chấm test. Tách initial finalization (khác AI có thể không cần lý do) với published correction/code correction bắt buộc lý do/history. Group cần điểm tài liệu và điểm đóng góp từng thành viên (mặc định bằng điểm tài liệu; điều chỉnh tay, không AI contribution). |
| III.6.1–6.3, #49–51 Credit | Action role chỉ Student/Teacher; reset chỉ nói chung `each period` | Ghi rõ Student/Teacher/Subject Manager, owner-only; Admin không ví/purchase. Reset free balance theo tháng từ Settings, không cộng dồn. Return path tùy vai trò thay vì luôn Class Dashboard. |

## Chức năng có màn nhưng mô tả còn thiếu

- Common shell: Đăng xuất UC 03; bell/popup notifications UC 12, unread count, đọc/đánh dấu tất cả, mở link có kiểm scope, email preferences cho Student. Không tạo màn Notifications độc lập.
- Admin Dashboard #6: thiếu AI usage metrics theo U13 (lượt/lỗi/latency/token/estimated cost, không nội dung hoặc danh tính người dùng); phần I.5 đã nhắc chức năng này.
- Question List #19: import CSV/XLSX theo loại, kiểm từng dòng; AI soạn từ học liệu môn ACTIVE/INDEXED, người soạn chọn giữ câu DRAFT. Không thêm màn import/AI độc lập.
- Quiz Detail #22: AI soạn câu, chọn ngân hàng, số lượt, review/publish/retire; chuẩn mode/timer đã nêu ở trên.
- Assignment Form #26: random N ACTIVE bank questions theo bộ lọc, copy/clone/version, giữ hoặc bỏ AI đề xuất và kiểm lại sample sau đổi code/tests; không xem screenshot là bằng chứng thay thế cho description.
- Class Detail #29: action/field chưa thể hiện đầy đủ open/archive/reopen, thêm/gỡ Student, distribution toggle của Subject Manager.
- Group Essay Workspace #40: bổ sung leader allocate/reassign theo thiết kế, phân biệt manual snapshot chỉ phần DONE với auto-submit có CLAIMED drafts.
- Submission Detail #47: progress của Student/group, cần classId khi bài của môn; Gradebook bao gồm GRADED bài lớp và bài môn.
- Diagram/Group documents và teacher view: mô tả export DOCX nếu giữ đúng scope U09; không tạo thêm màn độc lập.

## Checklist 51 màn

| Screen | Kết quả kiểm tra |
|---|---|
| #1 User Login, #2 Activate Account, #3 Password Reset, #4 User Profile, #5 Password Change | Đã có màn; mô tả chính tương đối khớp. Bổ sung shell/logout/notification, không đổi password policy thành yêu cầu uppercase/lowercase khi U01 chỉ chốt letter+digit. |
| #6 Admin Dashboard | Đã có; thiếu AI metrics. |
| #7 Setting List, #8 Setting Detail | Đã có; sai setting keys/đơn vị rate/reset. |
| #9 Account List, #10 New Account, #11 Account Detail | Đã có; mô tả chính tương đối khớp; quyền/state/version cần giữ. |
| #12 Payment History | Đã có; bổ sung account filter theo UC72; phân biệt read-only query dữ liệu giao dịch nền tảng với gọi PayOS trực tiếp khi mở trang. |
| #13 Subject List, #14 Subject Detail, #15 Credit Package List, #16 Credit Package Detail, #17 Audit Log, #18 Manager Dashboard | Đã có; chủ yếu đúng, cần giữ Admin structural access và R2/R3/R4 tách biệt. |
| #19 Question List, #20 Question Detail | Đã có; sai/mất field theo loại, rubric, option count và supporting actions. |
| #21 Quiz List, #22 Quiz Detail | Đã có; sai mode và thiếu lifecycle/attempts. |
| #23 Material List, #24 Material Detail | Đã có; sai upload-AI/rescan/replace và thiếu summary. |
| #25 Assignment List, #26 Assignment Form, #27 Rubric Detail | Đã có; sai scope list và thiếu rules authoring/rubric. |
| #28 Class List, #29 Class Detail | Đã có; Class Detail thiếu thao tác lifecycle/enrollment/distribution trong actions/fields. |
| #30 Class Dashboard, #31 Student Class Detail | Đã có; bổ sung Subject Manager assigned teaching và bài môn. |
| #32 Learning Material | Đã có; thiếu summary action/result/credit. |
| #33 Class Announcements | Đã có; bỏ comment/moderation. |
| #34 Student Assignments | Đã có; bổ sung bài môn/score distribution/retired visibility. |
| #35 Assignment Detail, #36 Submission History | Đã có; chính sách Practice AI cơ bản đúng; tránh áp controls attempt cá nhân máy móc cho Group. |
| #37 Codelab Workspace, #38 Text Essay Workspace, #39 Diagram Essay Workspace | Đã có; chính tương đối đúng; giữ input/time/type scope, bổ sung DOCX export hợp lệ. |
| #40 Group Essay Workspace | Đã có; bổ sung leader allocation và manual/auto snapshot differences. |
| #41 Quiz Practice History, #42 Quiz Practice Detail, #43 Quiz Taking, #44 Quiz Result | Đã có; #43 sai calendar deadline; giữ Practice privacy/attempt/timer. |
| #45 Teacher Class Detail, #46 Student Detail | Đã có; #45 cần tách bài/Quiz, bài môn và quyền materials; #46 read-only cơ bản đúng. |
| #47 Submission Detail, #48 Grading Workspace | Đã có; bổ sung progress/group scores và sửa fixed AI credit/correction rules. |
| #49 My Credit Package, #50 Public Credit Packages, #51 Credit Package Checkout | Đã có; ghi rõ SM/no Admin, monthly Settings/reset và return destination. |

## Cách sửa trong template hiện tại

Giữ 51 mục, numbering/captions và template hiện có. Sửa đồng thời ba lớp nội dung ở mỗi màn: Description, danh sách “This screen allows…”, bảng Field Description. Thêm component/field/hành động còn thiếu vào màn hoặc shell tương ứng; không tạo lại màn retired, không đổi luồng nghiệp vụ theo ảnh mockup cũ. Hình chỉ cần chỉnh riêng nếu kiểm ảnh xác nhận chúng còn vẽ controls đã bỏ.

## Trạng thái sau chỉnh sửa — 2026-10-09T10:24:54Z

Đã cập nhật trực tiếp III.1–III.6 trên SRS theo yêu cầu tiếp theo: 51 Description ngắn bằng tiếng Anh, sửa action lists và field descriptions tương ứng; tên field/nút tiếng Việt được giữ. Bổ sung shell UC03/UC12, AI metrics, question import/AI, summary action, review/copy/rubric/group/report/credit rules. Drive modified `2026-10-09T10:24:41.089Z`; readback xác nhận 51 description/51 màn và không còn các chuỗi cũ daily request limit/retry scanning/replace the file/hide comments/AI đề xuất (5 credit).

Template, numbering/captions, hình/mockup và toàn bộ nội dung ngoài III.1–III.6 giữ nguyên. Mọi package part trừ word/document.xml byte-identical; hình chứa trong paragraph cũng giữ nguyên XML. Word chạy ẩn render preview riêng nội dung/bảng (bỏ hình khỏi preview kiểm tra), đã kiểm 24 trang; không thay hình trong bản upload. Đây là đồng bộ nội dung tài liệu, không xác nhận mockup hoặc implementation đã được sửa.
