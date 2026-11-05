# U09 Assignment Grading — business-rules

## Server-managed grading workflow — 2026-11-01

Agent đọc `read_agent_workflow(workflow=GRADE_WITH_RUBRIC)` để nhận đầy đủ quy trình vận hành từ server. `read_grading_context.gradingInstructions` vẫn là chính sách chấm chung API/MCP, không bị thay bằng hướng dẫn vận hành. Đọc đủ context, kiểm tra fingerprint/tiến độ, rubric, bằng chứng, idempotency và duyệt giảng viên tiếp tục bắt buộc như trước. Không cài skill/hook cá nhân để dùng luồng này. Nguồn đầy đủ duy nhất: `backend/src/main/resources/agent-workflows/grade-with-rubric/SKILL.md`; wrapper cục bộ chỉ gọi tool đọc, không giữ bản sao đầy đủ.

## External-agent grading amendment — 2026-11-01

Giao việc và xác định phạm vi trong chat ngoài; hỏi lại khi chấm hết chưa rõ bài tập hoặc có bao gồm DRAFT không. Chỉ tạo đề xuất cho gốc bài PENDING/DRAFT, không ghi đè điểm đã chốt/công bố hay chấm đóng góp MEMBER.

Tool ghi đơn và từng item batch bắt buộc có snapshotFingerprint. Backend kiểm tra fingerprint hiện hành và tiến độ đọc liên tiếp đủ context theo actor/evaluation/fingerprint. Redis lưu offset cao nhất với TTL 30 phút không đọc, không lưu bài/token. Đọc lại không giảm tiến độ; nhảy trang bị từ chối; bài trống vẫn cần lần đọc đầu. Batch context bị bỏ do giới hạn byte không ghi nhận tiến độ.

MCP_CONTEXT_READ_REQUIRED: đọc lại từ offset 0. MCP_CONTEXT_READ_UNAVAILABLE: dừng, không bỏ guard. Snapshot thay đổi: đọc/chấm lại. Retry ghi không đổi giữ requestRef/payload nhưng vẫn cần context hiện hành. API callbacks, lịch sử và chấm tay không phụ thuộc progress MCP.

Ghi đơn, batch và danh sách đề xuất trả reviewUrl mở đúng evaluation/proposalId trên màn hình phẳng, kể cả bản cũ. Mở link không áp dụng/lưu/chốt/công bố điểm. Link mất đề xuất báo rõ, không tạo thay thế. Tiến độ chỉ chứng minh server đã phục vụ đủ trang cho tài khoản, không chứng minh model hiểu/chấm đúng; vẫn cần chuẩn giáo viên.

[Yêu cầu và stories](../../../inception/requirements/2026-11-01-external-agent-workflow.md).

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

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
| BR-U09-10 | Nộp lại làm điểm lượt cá nhân cũ mất hiệu lực: mọi đánh giá cũ của cùng bài/người học chuyển SUPERSEDED, kể cả FINALIZED/PUBLISHED; lượt mới tạo PENDING. Điểm cũ không còn hiển thị cho sinh viên hoặc vào sổ điểm/phân bố. Không xóa bản ghi audit. | Quyết định mới nhất của người dùng 2026-11-02 |
| BR-U09-11 | Submission Detail của một bài trong lớp: tiến độ (đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại) theo từng sinh viên, hoặc theo nhóm với bài nhóm; bảng bài nộp có trạng thái chấm, điểm, nhãn trễ; lọc theo trạng thái, trễ. | US-RPT-001 S1, FR-019 |
| BR-U09-12 | Grading Workspace hiện bài nộp đầy đủ (văn bản, bảng, ảnh, sơ đồ; bài nhóm theo từng phần kèm người làm), rubric từng phần, đánh giá, đề xuất AI và lịch sử điểm; nút ‹ › và lưu-sang-bài-kế giữ bộ lọc trạng thái/nộp trễ từ danh sách, theo thứ tự danh sách và chỉ chọn hàng có bản chấm hiện hành. Xác định bài kế từ danh sách trước khi lưu vì PENDING có thể đổi thành DRAFT; không có bài kế thì ở lại. Chọn sinh viên trực tiếp là đổi sang danh sách toàn bài của assignment và bỏ bộ lọc ban đầu. Link trực tiếp/MCP không có bộ lọc giữ hành vi tương thích; mọi thao tác ghi vẫn kiểm tra scope, receipt và version. | UC 42 |
| BR-U09-13 | Bài có theo dõi hoạt động: Grading Workspace hiện dòng thời gian từ `IActivityQuery` của U08 (bài nhóm theo từng thành viên); phân biệt tắt, chưa có dữ liệu, có dữ liệu, đã hết hạn giữ. Dữ liệu chỉ để đối chiếu, không đổi điểm, không kết luận gian lận. Lượt Quiz và hoạt động của lượt Quiz do U11 hiện trên Quiz Detail (slot `QuizDetailSection` của U10). | FR-039, US-INT-001 |

## 3. Chấm tay (UC 44)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-20 | Chấm theo rubric snapshot đã khóa; mỗi item cho partial points0..max, tối đa2 decimal, tổng phần/bài từ các item. Dữ liệu checkbox cũ vẫn tương thích; item scores tường minh có ưu tiên. Không nhập điểm phần vượt rubric hoặc dùng AI thành điểm official tự động. | UC 44, BR-U07-22 |
| BR-U09-21 | Điều chỉnh tùy chọn (cộng hoặc trừ, ví dụ trừ nộp trễ) bắt buộc lý do; điểm cuối kẹp trong 0 tới tổng điểm. Hệ thống không tự trừ điểm trễ. | FR-008 |
| BR-U09-22 | Nhận xét tùy chọn, ≤ 10.000 ký tự. Lưu thì đánh giá thành `DRAFT`, ghi người chấm và thời điểm; người học chưa thấy cho tới khi công bố. | US-GRD-003 S3 |
| BR-U09-23 | Hiển thị "x / tổng điểm", 2 chữ số thập phân, không quy đổi thang 10. | Thiết kế |

## 4. AI đề xuất (UC 43)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-30 | Giảng viên chọn nguồn: API chung trong quota, API cá nhân (qua `IAiTask` của U06) hoặc agent MCP (đọc bài qua `read_authorized_submission`, ghi qua `record_grading_proposal`). Hết quota không tự chuyển nguồn. | UC 43, FR-012 |
| BR-U09-31 | Prompt gồm hướng dẫn, khung, rubric (id và nội dung từng mục) và bài nộp dạng chữ, sơ đồ ở dạng XML rút gọn (`IDocumentModel`); không có tên, email người học. Điểm đóng góp thành viên không gửi AI. | FR-008 |
| BR-U09-32 | AI proposal có scores/structured feedback/evidence theo item rubric, đúng schema/version/submissionHash. Trùng hoặc thiếu item, bằng chứng sai và payload quá giới hạn bị từ chối. Proposal không tự finalize/publish; nhiều proposal có version và stale state. | US-GRD-002 S1, S2 |
| BR-U09-33 | Đề xuất chỉ để tham khảo: "Dùng đề xuất" điền sẵn checklist và nhận xét; giảng viên sửa rồi lưu như chấm tay. Điểm khác đề xuất không cần lý do; đề xuất gốc vẫn truy vết được. AI không lưu, không chốt, không công bố điểm. | US-GRD-003 S1, S2 |
| BR-U09-34 | Chấm hàng loạt bằng AI: chọn nhiều bài `PENDING` hoặc `DRAFT` trong Submission Detail và một nguồn; mỗi bài một task; trả kết quả từng mục (đã gửi, hết quota, ngoài quyền, không chấm được). Giảng viên xác nhận từng bài trong Grading Workspace. | UC 43 |
| BR-U09-35 | AI lỗi hoặc đề xuất sai thì bài nộp không mất, không có điểm giả; giảng viên chấm tay hoặc thử lại. | US-GRD-002 S2 |

## 5. Chốt, công bố và sửa điểm (UC 43, 44)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-40 | Finalize chỉ DRAFT có điểm/version hợp lệ; structured feedback phải được review/acknowledge theo từng item và điểm hiện hành. Điểm đã đổi sau review cần xác nhận lại tương ứng, không lấy review cũ cho giá trị mới. Batch trả kết quả riêng; publish là thao tác tách biệt. | US-GRD-005 S1, S2, FR-020 |
| BR-U09-41 | Công bố từng bài hoặc hàng loạt (có "chọn tất cả đã chốt"): đánh giá `FINALIZED` thành `PUBLISHED`; mỗi lần công bố phát `grade.published` kèm bài, lớp và danh sách người học vừa có điểm. Bài của môn công bố riêng ở từng lớp. | US-GRD-005 S3 |
| BR-U09-42 | `FINALIZED` và `PUBLISHED` khóa điểm, rubric, điều chỉnh, nhận xét và điểm thành viên. Mọi lưu hoặc áp dụng AI trả `409 GRADE_LOCKED`, kể cả có lý do; không có mở khóa. Bản `FINALIZED` hiện hành vẫn được công bố mà không đổi nội dung chấm. Lịch sử chỉnh sửa cũ chỉ đọc. | Quyết định người dùng 2026-11-02 |
| BR-U09-43 | Lịch sử điểm chỉ thêm, chỉ giảng viên của lớp xem. | FR-009 |

## 6. Bài nhóm (khi U12 có)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-50 | U12 nộp lại trong hạn thì dùng lại root/member hiện có, reset điểm, rubric checks, nhận xét, điều chỉnh và dữ liệu AI review, trở về PENDING kể cả trước đó FINALIZED/PUBLISHED. Điểm trước mất hiệu lực; lịch sử/audit vẫn chỉ đọc. Đây là reset do nộp mới, không phải cho giảng viên sửa điểm đã chốt. Không đổi DB schema/index. | Quyết định mới nhất của người dùng 2026-11-02 |
| BR-U09-51 | Tài liệu nhóm chấm theo rubric từng phần; điểm thành viên mặc định bằng điểm nhóm. Chỉ sửa đóng góp khi root và thành viên hiện hành chưa chốt; FINALIZED/PUBLISHED khóa cả hai theo BR-U09-42. Lý do sửa draft tùy chọn; không có công thức ghép điểm. | Quyết định người dùng 2026-11-02 |

## 7. Sổ điểm, xuất, phân bố (UC 45)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-60 | Sổ điểm lớp (tab Evals): bài `GRADED` đã giao (`OPEN`, `CLOSED`, `RETIRED`; không lấy `DRAFT`) của lớp và của môn chứa lớp; `/submission-progress` dùng cùng điều kiện; gom theo sinh viên đang ghi danh (mỗi sinh viên một mục đóng mở); mỗi bài là điểm của lượt được chấm "x / tổng" và trạng thái (chưa nộp, trễ, chờ chấm, đã chấm, đã chốt, đã công bố); bài nhóm lấy điểm thành viên. Không có Quiz, `PRACTICE`, đề xuất AI; không tính điểm tổng. | UC 45, US-GRD-004 S2 |
| BR-U09-61 | Xuất CSV hoặc XLSX theo lớp, tùy chọn một bài: sinh viên, bài, trạng thái nộp và chấm, thời gian nộp, điểm đã chốt hoặc đã công bố, nhận xét; ghi rõ chưa nộp, chưa chốt. Kiểm quyền trước khi tạo tệp; tạo khi yêu cầu, không lưu; giá trị CSV được chặn công thức bảng tính; tên tệp không chứa dữ liệu cá nhân; ghi audit. | US-RPT-003 |
| BR-U09-62 | Phân bố điểm ẩn danh cạnh bài trên Student Assignments: chỉ bài `GRADED` (không Quiz/Practice), cài đặt U09 theo cặp assignment–class đã bật (mặc định tắt), lớp có ít nhất 20 người đang ghi danh có điểm công bố dùng được của bài đó; trả khoảng điểm tổng hợp, ẩn khoảng có dưới 5 người; không trả tên hay điểm của người khác. | US-RPT-001 S3, BR-U09-63 |
| BR-U09-63 | Chỉ giảng viên được phân công dạy lớp hiện tại bật/tắt phân bố cho bài thuộc lớp; Chủ nhiệm môn không được giao dạy và Admin không có quyền. Bài SUBJECT có cài đặt riêng từng lớp; chỉ đọc nội dung bài không hạn chế quyền cài đặt này. U09 lưu cặp `assignmentId,classId`, `enabled`, `version`; không chuyển cờ true lịch sử của lớp. PUT kiểm version, lớp còn cho phép sửa, phạm vi bài và chế độ GRADED; xung đột trả `409`, ngoài quyền từ chối. | FR-024, US-RPT-001 S3 |

## 8. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U09-70 | Audit: GRADE_SAVED, GRADE_FINALIZED, GRADE_PUBLISHED, GRADE_CHANGED (điểm thành viên chưa chốt; lý do tùy chọn), AI_GRADING_REQUESTED, GRADEBOOK_EXPORTED. Không ghi nội dung bài nộp. GRADE_CHANGED của điểm official trong lịch sử giữ nguyên, không tạo mới. | FR-014, BR-U09-42 |
| BR-U09-71 | Sửa đánh giá phải kèm `version`; lệch thì `409`. U01, U05, U07, U08 không trả lời thì từ chối (fail closed). | SEC-002 |
| BR-U09-72 | Xin gia hạn cá nhân, phúc khảo và kiểm tra tương đồng bài nộp nằm ngoài phạm vi. | Phạm vi 2026-09-25 |
