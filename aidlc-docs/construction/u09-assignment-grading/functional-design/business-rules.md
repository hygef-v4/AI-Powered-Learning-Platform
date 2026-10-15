# U09 Assignment Grading — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Quyền và phạm vi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-01 | Chỉ giảng viên chính của lớp (R3/R4) xem bài nộp, chấm, chốt, công bố, sửa điểm và xem, xuất sổ điểm của lớp đó. Chủ nhiệm môn chỉ có R2 không chấm; Admin không vào. Ngoài quyền trả `404` và ghi audit. | UC 42–45, US-GRD-004 S3 |
| BR-U09-02 | Bài của lớp và bài của môn đều chấm theo lớp: mỗi đánh giá thuộc đúng một lớp (lớp ghi trên lượt hoặc lớp của nhóm). Bài của môn hiện ở Submission Detail và sổ điểm của từng lớp, mỗi lớp chỉ thấy bài nộp của lớp mình. | FR-027 |
| BR-U09-03 | Chỉ bài `GRADED` có đánh giá. Bài `PRACTICE` (U08) và Quiz (U11) không vào Submission Detail, sổ điểm, chốt hay công bố; giảng viên không chấm bài Practice. | FR-030 |
| BR-U09-04 | Người học chỉ thấy điểm `PUBLISHED`, nhận xét và mục rubric đạt của chính mình, trên Student Assignments và Submission History (U08 gắn chỗ hiển thị). Không có trang điểm riêng, không mở sổ điểm. | US-GRD-004 S1 |

## 2. Bài nộp và tiến độ (UC 42)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-10 | Bài `GRADED` được nộp (U08 gọi `onSubmitted`) tạo đánh giá `PENDING` gắn lượt đó. Chỉ lượt nộp cuối được chấm: có lượt mới thì đánh giá cũ chưa công bố chuyển `SUPERSEDED`; đánh giá cũ đã công bố giữ nguyên để người học xem, sổ điểm dùng lượt mới. | FR-007 |
| BR-U09-11 | Submission Detail của một bài trong lớp: tiến độ (đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại) theo từng sinh viên, hoặc theo nhóm với bài nhóm; bảng bài nộp có trạng thái chấm, điểm, nhãn trễ; lọc theo trạng thái, trễ. | US-RPT-001 S1, FR-019 |
| BR-U09-12 | Grading Workspace hiện bài nộp đầy đủ (văn bản, bảng, ảnh, sơ đồ; bài nhóm theo từng phần kèm người làm), rubric từng phần, đánh giá, đề xuất AI và lịch sử điểm; có nút ‹ › chuyển giữa các bài theo bộ lọc hiện tại; lưu một bài thì tự sang bài kế. | UC 42 |
| BR-U09-13 | Bài có theo dõi hoạt động: Grading Workspace hiện dòng thời gian từ `ActivityQueryPort` của U08 (bài nhóm theo từng thành viên); phân biệt tắt, chưa có dữ liệu, có dữ liệu, đã hết hạn giữ. Dữ liệu chỉ để đối chiếu, không đổi điểm, không kết luận gian lận. Lượt Quiz và hoạt động của lượt Quiz do U11 hiện trên Quiz Detail (slot `QuizDetailSection` của U10). | FR-039, US-INT-001 |

## 3. Chấm tay (UC 44)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-20 | Chấm là tích mục checklist của rubric từng phần (rubric đã khóa của U07); điểm phần là tổng mục đạt qua `RubricPort.score`, điểm bài là tổng các phần. Không có ô nhập điểm tự do cho phần. | UC 44, BR-U07-22 |
| BR-U09-21 | Điều chỉnh tùy chọn (cộng hoặc trừ, ví dụ trừ nộp trễ) bắt buộc lý do; điểm cuối kẹp trong 0 tới tổng điểm. Hệ thống không tự trừ điểm trễ. | FR-008 |
| BR-U09-22 | Nhận xét tùy chọn, ≤ 10.000 ký tự. Lưu thì đánh giá thành `DRAFT`, ghi người chấm và thời điểm; người học chưa thấy cho tới khi công bố. | US-GRD-003 S3 |
| BR-U09-23 | Hiển thị "x / tổng điểm", 2 chữ số thập phân, không quy đổi thang 10. | Thiết kế |

## 4. AI đề xuất (UC 43)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-30 | Giảng viên chọn nguồn: API chung trong quota, API cá nhân (qua `AiTaskPort` của U06) hoặc agent MCP (đọc bài qua `read_authorized_submission`, ghi qua `record_grading_proposal`). Hết quota không tự chuyển nguồn. | UC 43, FR-012 |
| BR-U09-31 | Prompt gồm hướng dẫn, khung, rubric (id và nội dung từng mục) và bài nộp dạng chữ, sơ đồ ở dạng XML rút gọn (`DocumentModelPort`); không có tên, email người học. Điểm đóng góp thành viên không gửi AI. | FR-008 |
| BR-U09-32 | Đề xuất gồm mục đạt theo từng phần, nhận xét từng phần và nhận xét chung; phải đúng schema và chỉ dùng id mục của rubric đã khóa. Mỗi đề xuất gắn `submissionHash`; hash khác bài nộp hiện tại thì `STALE`, không dùng được. Mỗi đánh giá giữ nhiều đề xuất đánh số version. | US-GRD-002 S1, S2 |
| BR-U09-33 | Đề xuất chỉ để tham khảo: "Dùng đề xuất" điền sẵn checklist và nhận xét; giảng viên sửa rồi lưu như chấm tay. Điểm khác đề xuất không cần lý do; đề xuất gốc vẫn truy vết được. AI không lưu, không chốt, không công bố điểm. | US-GRD-003 S1, S2 |
| BR-U09-34 | Chấm hàng loạt bằng AI: chọn nhiều bài `PENDING` hoặc `DRAFT` trong Submission Detail và một nguồn; mỗi bài một task; trả kết quả từng mục (đã gửi, hết quota, ngoài quyền, không chấm được). Giảng viên xác nhận từng bài trong Grading Workspace. | UC 43 |
| BR-U09-35 | AI lỗi hoặc đề xuất sai thì bài nộp không mất, không có điểm giả; giảng viên chấm tay hoặc thử lại. | US-GRD-002 S2 |

## 5. Chốt, công bố và sửa điểm (UC 43, 44)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-40 | Chốt từng bài hoặc hàng loạt (có hộp xác nhận): chỉ đánh giá `DRAFT` có điểm hợp lệ và `version` khớp thành `FINALIZED`; kết quả từng mục; mục lỗi không ảnh hưởng mục khác. | US-GRD-005 S1, S2, FR-020 |
| BR-U09-41 | Công bố từng bài hoặc hàng loạt (có "chọn tất cả đã chốt"): đánh giá `FINALIZED` thành `PUBLISHED`; mỗi lần công bố phát `grade.published` kèm bài, lớp và danh sách người học vừa có điểm. Bài của môn công bố riêng ở từng lớp. | US-GRD-005 S3 |
| BR-U09-42 | Sửa điểm đã `FINALIZED` hoặc `PUBLISHED` bắt buộc lý do, giữ trạng thái và thêm một mục lịch sử (người sửa, thời điểm, trước, sau, lý do); người học thấy điểm mới kèm nhãn "đã cập nhật". | FR-008 |
| BR-U09-43 | Lịch sử điểm chỉ thêm, chỉ giảng viên của lớp xem. | FR-009 |

## 6. Bài nhóm (khi U12 có)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-50 | U12 gọi `GroupEvaluationPort.onGroupSubmitted` trong transaction nộp: tạo hoặc đưa về `PENDING` một đánh giá tài liệu nhóm và một đánh giá thành viên cho mỗi người; điểm trước đó vào lịch sử. | US-GRP-006 |
| BR-U09-51 | Tài liệu nhóm chấm như bài cá nhân theo rubric từng phần; điểm thành viên mặc định bằng điểm tài liệu chung, giảng viên sửa từng người khi cần, lý do tùy chọn; không có công thức ghép điểm. | Người dùng chốt 2026-10-04 |

## 7. Sổ điểm, xuất, phân bố (UC 45)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-60 | Sổ điểm lớp (tab Evals): bài `GRADED` của lớp và bài `GRADED` của môn chứa lớp, gom theo sinh viên đang ghi danh (mỗi sinh viên một mục đóng mở); mỗi bài là điểm của lượt được chấm "x / tổng" và trạng thái (chưa nộp, trễ, chờ chấm, đã chấm, đã chốt, đã công bố); bài nhóm lấy điểm thành viên. Không có Quiz, `PRACTICE`, đề xuất AI; không tính điểm tổng. | UC 45, US-GRD-004 S2 |
| BR-U09-61 | Xuất CSV hoặc XLSX theo lớp, tùy chọn một bài: sinh viên, bài, trạng thái nộp và chấm, thời gian nộp, điểm đã chốt hoặc đã công bố, nhận xét; ghi rõ chưa nộp, chưa chốt. Kiểm quyền trước khi tạo tệp; tạo khi yêu cầu, không lưu; giá trị CSV được chặn công thức bảng tính; tên tệp không chứa dữ liệu cá nhân; ghi audit. | US-RPT-003 |
| BR-U09-62 | Phân bố điểm ẩn danh cạnh bài trên Student Assignments: chỉ bài `GRADED`, lớp bật `showGradeDistribution` (U05), lớp có ít nhất 20 người học có điểm đã công bố của bài đó; trả khoảng điểm tổng hợp, ẩn khoảng có dưới 5 người; không trả tên hay điểm của người khác. | US-RPT-001 S3, BR-U05-17 |

## 8. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-70 | Audit: `GRADE_SAVED`, `GRADE_FINALIZED`, `GRADE_PUBLISHED`, `GRADE_CHANGED` (lý do), `AI_GRADING_REQUESTED`, `GRADEBOOK_EXPORTED`. Không ghi nội dung bài nộp. | FR-014 |
| BR-U09-71 | Sửa đánh giá phải kèm `version`; lệch thì `409`. U01, U05, U07, U08 không trả lời thì từ chối (fail closed). | SEC-002 |
| BR-U09-72 | Xin gia hạn cá nhân, phúc khảo và kiểm tra tương đồng bài nộp nằm ngoài phạm vi. | Phạm vi 2026-09-25 |
