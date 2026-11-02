# U09 Assignment Grading — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Tạo đánh giá khi nộp | - | - |
| F2 Danh sách bài nộp và tiến độ | UC 42 | Submission Detail |
| F3 Mở bài để chấm | UC 42 | Grading Workspace |
| F4 Chấm tay | UC 44 | Grading Workspace |
| F5 AI đề xuất một bài | UC 43 | Grading Workspace, popup chọn nguồn AI |
| F6 AI chấm hàng loạt | UC 43 | Submission Detail |
| F7 Đề xuất qua agent MCP | UC 43 | - |
| F8 Chốt | UC 43, 44 | Submission Detail, Grading Workspace |
| F9 Công bố | UC 43, 44 | Submission Detail, Grading Workspace |
| F10 Sửa điểm đã chốt hoặc đã công bố | UC 44 | Grading Workspace |
| F11 Sổ điểm | UC 45 | Teacher Class Detail, tab Evals |
| F12 Xuất bảng điểm | UC 45 | Teacher Class Detail, tab Evals |
| F13 Người học xem điểm và phân bố | UC 37, 40 (U08) | Student Assignments, Submission History |
| F14 Bài nhóm | UC 56, 57 (U12) | Submission Detail, Grading Workspace |

## 2. Bài nộp

### F1 - Tạo đánh giá khi nộp
1. U08 gọi `ISubmissionListener.onSubmitted(attemptId)` trong transaction nộp.
2. U09 đọc lượt qua `ISubmissionQuery.getSubmission` và bài qua `IAssignmentQuery.get`; bài `PRACTICE` thì bỏ qua.
3. Tạo đánh giá `PENDING` (lượt, bài, lớp, người học, `submissionHash`, tổng điểm từ `IRubric.maxPoints`).
4. Đánh giá cũ chưa công bố của cùng (bài, người học) chuyển `SUPERSEDED` (BR-U09-10).

### F2 - Danh sách bài nộp và tiến độ (UC 42)
1. Giảng viên bấm "Bài nộp" trên dòng bài ở tab Evals (U09 gắn qua slot `AssignmentRowAction` của U07); frontend gọi `GET /api/v1/classes/{classId}/assignments/{assignmentId}/submissions`.
2. Kiểm giảng viên chính của lớp (BR-U09-01); bài là `GRADED` của lớp hoặc của môn chứa lớp.
3. Lấy tiến độ từng sinh viên qua `ISubmissionQuery.listSubmissions` (bài nhóm qua `IGroupSubmissionSource`), ghép với đánh giá của lớp.
4. Trả số đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại và bảng bài nộp (trạng thái chấm, điểm, trễ); lọc theo trạng thái, trễ.
5. Từ đây: mở một bài (F3), AI chấm hàng loạt (F6), chốt (F8), công bố (F9).

### F3 - Mở bài để chấm (UC 42)
1. Bấm một bài; frontend gọi `GET /api/v1/evaluations/{id}`.
2. Kiểm giảng viên chính của lớp ghi trên đánh giá.
3. Trả bài nộp (nội dung qua `ISubmissionQuery.getSubmission` hoặc `IGroupSubmissionSource`), rubric đã khóa từng phần (`IRubric.listRubrics`), đánh giá, các đề xuất AI, lịch sử điểm.
4. Bài có theo dõi hoạt động: hiện `ActivityTimeline` với dữ liệu từ `IActivityQuery.summary` (bài nhóm theo từng thành viên) (BR-U09-13).
5. Nút ‹ › chuyển bài theo bộ lọc của Submission Detail hoặc theo lô vừa chấm AI.

## 3. Chấm

### F4 - Chấm tay (UC 44)
1. Giảng viên tích mục đạt của từng phần; điểm phần và điểm bài tự tính qua `IRubric.score`.
2. Tùy chọn nhập điều chỉnh (kèm lý do) và nhận xét.
3. Lưu: `PUT /api/v1/evaluations/{id}` kèm `version`; kiểm quyền, đánh giá không `SUPERSEDED`, mục thuộc rubric, điều chỉnh có lý do, điểm cuối trong 0 tới tổng.
4. Đánh giá `PENDING` hoặc `DRAFT` thành `DRAFT`; ghi người chấm, thời điểm; audit `GRADE_SAVED`. Đánh giá đã chốt hoặc đã công bố đi theo F10.
5. Tự mở bài kế.

### F5 - AI đề xuất một bài (UC 43)
1. Trong Grading Workspace bấm "AI đề xuất"; `AiSourcePicker` của U06 hiện API chung (kèm quota còn lại) và kết nối cá nhân; giảng viên chọn.
2. Frontend gọi `POST /api/v1/evaluations/{id}/ai-proposals` kèm nguồn, `requestRef`.
3. Kiểm quyền, đánh giá chưa `SUPERSEDED`; dựng prompt theo BR-U09-31 (`IAssignmentQuery.getContent`, `IRubric.listRubrics`, `IDocumentModel.toPlainText`, `compactDiagramXml`) kèm JSON schema.
4. Gọi `IAiTask.submit` (loại `GRADING_PROPOSAL`, đích là đánh giá, `inputVersion` là `submissionHash`); tạo đề xuất `PENDING` version kế tiếp; audit `AI_GRADING_REQUESTED`.
5. U06 gọi `onReady`: kiểm quyền người yêu cầu, hash còn khớp, schema, id mục thuộc rubric; đạt thì lưu đề xuất `READY` kèm provenance, không thì `STALE` hoặc `REJECTED`. `onFailed` chuyển `FAILED`.
6. Giảng viên bấm "Dùng đề xuất": điền sẵn checklist và nhận xét; sửa nếu muốn rồi lưu như F4 bước 3–4; ghi đề xuất đã dùng.

### F6 - AI chấm hàng loạt (UC 43)
1. Trong Submission Detail chọn nhiều bài `PENDING` hoặc `DRAFT`, bấm "Chấm hàng loạt bằng AI", chọn một nguồn.
2. Frontend gọi `POST /api/v1/evaluations:ai-proposals` kèm danh sách và nguồn.
3. Với từng bài chạy F5 bước 3–4 (`requestRef` riêng); hết quota ở bài nào thì bài đó và các bài sau trả "hết quota".
4. Trả kết quả từng mục; danh sách theo dõi trạng thái các task.
5. Giảng viên mở Grading Workspace theo lô, xác nhận từng bài như F5 bước 6.

### F7 - Đề xuất qua agent MCP (UC 43)
1. Agent của giảng viên đọc bài qua `read_authorized_submission` (U08) hoặc `read_authorized_group_submission` (U12, bài nhóm) và rubric qua `read_assignment_context` (U07).
2. Agent gọi `record_grading_proposal(submissionRef, submissionHash, proposal)` với credential `READ_WRITE`.
3. U09 tìm đánh giá theo lượt hoặc tài liệu nhóm; kiểm giảng viên chính của lớp, hash khớp, schema và id mục; lưu đề xuất `READY` nguồn MCP (nhãn agent, model nếu agent báo).
4. Giảng viên dùng đề xuất như F5 bước 6.

## 4. Chốt, công bố, sửa

### F8 - Chốt (UC 43, 44)
1. Chốt một bài trong Grading Workspace, hoặc chọn nhiều bài trong Submission Detail rồi xác nhận; frontend gọi `POST /api/v1/evaluations:finalize` kèm id và `version`.
2. Với từng mục: kiểm quyền, `DRAFT`, điểm hợp lệ, `version` khớp; đạt thì `FINALIZED`, audit `GRADE_FINALIZED`.
3. Trả kết quả từng mục; mục lỗi không ảnh hưởng mục khác.

### F9 - Công bố (UC 43, 44)
1. Công bố một bài, hoặc chọn nhiều bài đã chốt ("chọn tất cả đã chốt"); frontend gọi `POST /api/v1/evaluations:publish`.
2. Với từng mục: kiểm quyền, `FINALIZED`, `version`; đạt thì `PUBLISHED`, ghi `publishedAt`; audit `GRADE_PUBLISHED`.
3. Sau commit phát `grade.published` (bài, lớp, người học vừa có điểm; bài nhóm là các thành viên) cho U14.
4. Trả kết quả từng mục.

### F10 - Sửa điểm đã chốt hoặc đã công bố (UC 44)
1. Trong Grading Workspace sửa checklist, điều chỉnh, nhận xét của đánh giá `FINALIZED` hoặc `PUBLISHED`, nhập lý do.
2. `PUT /api/v1/evaluations/{id}` kèm `version` và lý do; thiếu lý do thì từ chối.
3. Giữ trạng thái, thêm một mục lịch sử; audit `GRADE_CHANGED`.
4. Đánh giá đã công bố: người học thấy điểm mới kèm nhãn "đã cập nhật".

## 5. Sổ điểm và người học

### F11 - Sổ điểm (UC 45)
1. Giảng viên mở phần Sổ điểm trong tab Evals (U09 gắn qua `TeacherClassTab`); frontend gọi `GET /api/v1/classes/{classId}/gradebook`.
2. Kiểm giảng viên chính của lớp.
3. Lấy bài `GRADED` của lớp và của môn chứa lớp (`IAssignmentQuery.listForClass`), sinh viên đang ghi danh (`IClassAccess.listActiveStudents`), đánh giá của lớp và tiến độ nộp (`ISubmissionQuery`).
4. Trả mỗi sinh viên một mục đóng mở chứa từng bài: "x / tổng" của lượt được chấm và trạng thái (BR-U09-60); không có điểm tổng.

### F12 - Xuất bảng điểm (UC 45)
1. Bấm Xuất trên Sổ điểm, chọn CSV hoặc XLSX, tùy chọn một bài; frontend gọi `GET /api/v1/classes/{classId}/gradebook/export`.
2. Kiểm quyền và bộ lọc trước khi tạo tệp; ngoài quyền trả `404`.
3. Tạo tệp theo BR-U09-61 dạng luồng; chặn công thức trong CSV; tên tệp gồm mã lớp, mã bài, ngày.
4. Ghi audit `GRADEBOOK_EXPORTED` (phạm vi, định dạng); không lưu tệp.

### F13 - Người học xem điểm và phân bố
1. Student Assignments (U08) nhúng `StudentGradeCell` qua slot `StudentAssignmentResult`; component gọi `GET /api/v1/me/grades` và `GET /api/v1/me/grade-distributions` kèm lớp.
2. U09 kiểm người học đang ghi danh lớp; trả điểm `PUBLISHED` của chính mình theo từng bài.
3. Phân bố: kiểm BR-U09-62 với từng bài; đủ điều kiện thì trả khoảng điểm, ẩn khoảng dưới 5 người; không đủ thì không trả.
4. Submission History (U08) nhúng `StudentGradePanel` qua slot `SubmissionResultSection`; gọi `GET /api/v1/me/grades/attempts/{attemptId}`: điểm, nhận xét, mục rubric đạt, nhãn "đã cập nhật" nếu có; lượt chưa công bố thì trả "chưa có điểm".

### F14 - Bài nhóm (khi U12 có)
1. U12 gọi `IGroupEvaluation.onGroupSubmitted` trong transaction nộp: tạo hoặc đưa về `PENDING` đánh giá tài liệu nhóm và đánh giá từng thành viên (BR-U09-50).
2. Submission Detail hiện nhóm thay cho sinh viên; Grading Workspace đọc tài liệu nhóm qua `IGroupSubmissionSource`, hiện từng phần kèm người làm.
3. Chấm tài liệu nhóm như F4, F5; điểm thành viên mặc định bằng điểm tài liệu chung; giảng viên sửa từng người qua `PUT /api/v1/evaluations/{id}/members/{accountId}` (BR-U09-51).
4. Chốt, công bố cả đánh giá tài liệu và đánh giá thành viên như F8, F9.
