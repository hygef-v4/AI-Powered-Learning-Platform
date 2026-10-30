# U09 Assignment Grading — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 32 | Teacher Class Detail: tab Sổ điểm (`t02b`), popup Xuất sổ điểm (`t02j`); tab Thông tin lớp, phần tiến độ và phân bố (`t18`); tab Đánh giá, cột Bài nộp (`t21`) | `ClassGradebookTab`, `GradebookExportDialog`, `ClassProgressSection`, `SubmissionsLink` | 42, 44, 45 | Teacher, Subject Manager được giao dạy |
| 33 | Student Detail, phần bài nộp và điểm (`t19`) | `StudentGradesSection` | 45 | Như trên |
| 34 | Submission Detail (`t16`), popup AI đề xuất hàng loạt (`t16b`), Chốt / Công bố điểm (`t16c`) | `SubmissionDetailPage`, `BatchAiDialog`, `BatchFinalizeDialog`, `BatchPublishDialog` | 42, 43, 44 | Như trên |
| 35 | Grading Workspace: bài cá nhân (`t17`), bài nhóm (`t17b`) | `GradingWorkspacePage` | 42, 43, 44 | Như trên |
| 38, 39, 41 | Điểm trên Student Assignments (`s19`), Assignment Detail đã công bố (`s12c`), Submission History đã chấm (`s13b`) | `StudentGradeCell`, `StudentGradePanel` | 37, 38, 40 | Student |

Điểm luôn hiện "x / tổng điểm rubric", 2 chữ số thập phân, không quy đổi thang 10 (BR-U09-23).

## 2. Cây component

```
(TeacherClassTab "info" của U05)
  ClassProgressSection                       thẻ Bài đang mở, Chờ chấm; Tiến độ nộp bài; Phân bố điểm
(TeacherClassTab "gradebook" của U05)
  ClassGradebookTab                          tab Sổ điểm: bảng sinh viên × bài Graded, nút Xuất
    GradebookExportDialog                    CSV hoặc XLSX, toàn bộ hoặc một bài
(dòng bài ở tab Đánh giá, đăng ký qua AssignmentRowAction của U07)
  SubmissionsLink                            "x / N nộp · n chờ chấm", mở Submission Detail
(StudentDetailSection của U05)
  StudentGradesSection                       bài nộp và điểm của một sinh viên trong lớp
app/teaching/classes/[classId]/assignments/[assignmentId]/submissions/   SubmissionDetailPage   #34
  ProgressSummary                            đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại
  SubmissionFilters, SubmissionTable         tab trạng thái, lọc đúng hạn/trễ, chọn nhiều
  BatchAiDialog                              AiSourcePicker (U06), kết quả từng bài
  BatchFinalizeDialog, BatchPublishDialog    xác nhận, kết quả từng bài
app/teaching/evaluations/[evaluationId]/      GradingWorkspacePage   #35
  GradingTopBar, SubmissionNavList, PrevNextNav
  SubmissionViewer                           DocumentViewer (U07); chọn lượt; bài nhóm: bản nộp theo mục kèm tác giả
  RubricChecklistPanel                       tick mục chấm theo phần · tiêu chí, điểm tự tính
  AdjustmentField, FeedbackEditor
  MemberContributionPanel                    bài nhóm: điểm nhóm và điểm từng thành viên
  AiProposalPanel                            AiSourcePicker, AiTaskStatus (U06), các version đề xuất, Dùng đề xuất
  ActivityTimeline (U08)                     khi bài bật theo dõi
  GradeHistoryPanel                          lịch sử sửa điểm
  FinalizeButton, PublishButton
(đăng ký vào slot của U08)
  StudentGradeCell                           StudentAssignmentResult: điểm đã công bố, nút Phân bố
  StudentGradePanel                          SubmissionResultSection: điểm, nhận xét, mục rubric đạt, nhãn "đã cập nhật"
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubmissionDetailPage`, `ProgressSummary` | `GET .../assignments/{assignmentId}/submissions` | Nút quay lại; "Bài nộp · tên bài" kèm nhãn trạng thái; đường dẫn "Lớp được phân công › lớp › Đánh giá"; nút Xem sổ điểm. Thẻ: Đã nộp (trên N sinh viên), Đang làm (có lượt làm dở), Chưa nộp (chưa mở bài), Nộp trễ (sau hạn nộp), Còn lại (đếm ngược tới hạn) |
| `SubmissionFilters`, `SubmissionTable` | như trên | Tab Tất cả, Chờ chấm, Đã chấm (nháp), Đã chốt, Đã công bố (kèm số); lọc Nộp (đúng hạn, trễ); ô chọn từng dòng và chọn tất cả, "Đã chọn n bài"; nút AI đề xuất hàng loạt, Chốt điểm, Công bố điểm. Cột Sinh viên (avatar, tên, email), Nộp lúc (kèm "trễ n phút"), Lượt (x / y), Trạng thái, Điểm ("x / tổng"), Chấm hoặc Xem; phân trang; ghi chú "Bài Practice không có ở đây. Công bố điểm thì sinh viên nhận thông báo, không kèm điểm trong thông báo." |
| `BatchAiDialog` | `POST /api/v1/evaluations:ai-proposals` | Tên bài · số bài chờ chấm; chọn nguồn (API của trường: còn x / y token, trường chịu chi phí; API cá nhân theo từng nhà cung cấp: tính phí vào tài khoản của bạn), không chọn sẵn; ước tính token cho cả lô; "Đề xuất chỉ là bản nháp; bạn kiểm tra và lưu từng bài."; bảng kết quả từng bài (Đã có đề xuất, Đang chạy, Lỗi, Hết hạn mức, Ngoài quyền; Điểm đề xuất); Đóng, Chạy cho n bài; không tự đổi nguồn khi hết quota |
| `BatchFinalizeDialog`, `BatchPublishDialog` | `POST /api/v1/evaluations:finalize`, `:publish` | "Công bố điểm cho n bài?": sinh viên thấy điểm và nhận xét sau khi công bố, thông báo không kèm điểm; bảng Sinh viên, Kết quả (Sẽ công bố; Bỏ qua: chưa chốt điểm); "Chỉ bài đã chốt mới công bố được."; Chốt điểm có xác nhận và bảng kết quả tương tự |
| `GradingWorkspacePage`, `GradingTopBar`, `SubmissionNavList` | `GET`, `PUT /api/v1/evaluations/{id}` | Thanh trên: về Bài nộp, tên bài · lớp, sinh viên (hoặc nhóm) · thứ tự i / N · email, Lượt x / y, nộp lúc · đúng hạn hoặc trễ, nút trước / sau, Lưu nháp, Chốt, Công bố. Cột trái: danh sách bài nộp (bài cá nhân) hoặc danh sách nhóm (bài nhóm) kèm trạng thái chấm |
| `SubmissionViewer` | `GET /api/v1/evaluations/{id}` | Bài làm chỉ đọc; chọn lượt (lượt mới nhất được chấm, các lượt trước để xem). Bài nhóm: thời điểm nộp, trưởng nhóm nộp, bảng Mục, Tác giả, Điểm mục; xem nội dung từng mục |
| `RubricChecklistPanel`, `AdjustmentField`, `FeedbackEditor` | `PUT /api/v1/evaluations/{id}` | "Chấm theo mục chấm": mỗi phần · tiêu chí là danh sách ô tick mục chấm kèm điểm, điểm tiêu chí tự tính; "Theo rubric x / y"; Điều chỉnh (+/−) và Lý do điều chỉnh (bắt buộc khi có điều chỉnh); Điểm cuối "x / tổng"; Nhận xét (tùy chọn); sửa điểm đã chốt hoặc đã công bố bắt buộc lý do; `409` báo tải lại; lưu xong sang bài kế |
| `AiProposalPanel` | `POST`, `GET .../ai-proposals` | "Đề xuất AI": nguồn đang chọn và Đổi nguồn (`AiSourcePicker`); các version đề xuất (#n · thời điểm · điểm, tóm tắt); đề xuất cũ ghi "Cũ · bài đã sửa" và không dùng được (`STALE`); "Dùng đề xuất" chỉ điền sẵn rubric và nhận xét; Yêu cầu đề xuất mới; đề xuất do agent MCP ghi mang nhãn Agent |
| `MemberContributionPanel` | `PUT .../members/{accountId}` | "Điểm nhóm và thành viên": Điểm nhóm "x / tổng"; điểm từng thành viên mặc định bằng điểm nhóm, giảng viên sửa từng người, lý do tùy chọn (BR-U09-51) |
| `ActivityTimeline` (U08) | `IActivityQuery` của U08 | "Hoạt động khi làm bài": thời điểm rời trang và thời lượng; "Chỉ để tham khảo; nền tảng không kết luận gian lận." |
| `GradeHistoryPanel` | `GET /api/v1/evaluations/{id}` | "Lịch sử sửa điểm": Lúc (thời điểm · người), Trước → sau, Lý do |
| `ClassGradebookTab` | `GET .../gradebook` | Tab Sổ điểm: ô tìm sinh viên theo tên hoặc email; "n sinh viên · n bài Graded"; nút Xuất. Bảng mỗi dòng một sinh viên (avatar, tên, email), mỗi cột một bài Graded (tên, hình thức, trạng thái bài): "x / tổng" và trạng thái (Chưa nộp, Trễ, Chờ chấm, Đã chấm, Đã chốt, Đã công bố; bài nhóm lấy điểm thành viên, chưa có điểm thì Nhóm đang làm hoặc Nhóm đã nộp); phân trang; "Chỉ bài Graded; lượt được chấm; không có tổng điểm có trọng số; không gồm Practice và Quiz." |
| `GradebookExportDialog` | `GET .../gradebook/export` | Định dạng CSV hoặc XLSX; Phạm vi toàn bộ bài Graded hoặc một bài; "Việc xuất được ghi vào Nhật ký audit; tệp không lưu lại trên hệ thống."; Hủy, Xuất tệp; tải tệp dạng luồng |
| `ClassProgressSection` | `GET /api/v1/classes/{classId}/submission-progress`, `GET .../grade-distribution` | Trên tab Thông tin lớp: thẻ Bài đang mở (kèm số bài hết hạn hôm nay), Chờ chấm (bài nộp chưa có điểm); "Tiến độ nộp bài": từng bài (tên, hình thức · chế độ, hạn, số đã nộp / N sinh viên hoặc nhóm, phần trăm), link mở tab Đánh giá; "Phân bố điểm" của một bài đã công bố: số bài theo khoảng điểm (theo phần trăm tổng điểm) |
| `SubmissionsLink` | `GET .../assignments/{assignmentId}/submissions` (tóm tắt) | Cột Bài nộp: "x / N nộp · n chờ chấm", "đã công bố", bài nhóm "x / k nhóm nộp"; bấm mở Submission Detail |
| `StudentGradesSection` | `GET /api/v1/classes/{classId}/students/{accountId}/grades` | Trong popup Student Detail: Đã nộp x / y bài Graded, Nộp trễ n; bảng Bài, Hình thức, Nộp (Đúng hạn, Trễ, Chưa nộp), Điểm ("x / tổng" hoặc Chờ chấm) |
| `StudentGradeCell`, `StudentGradePanel` | `GET /api/v1/me/grades`, `/me/grades/attempts/{attemptId}`, `/me/grade-distributions` | Chỉ điểm đã công bố của chính mình. Ô Điểm: "x / tổng", nút Phân bố khi lớp cho xem (BR-U09-62). Phần Kết quả: Điểm chính thức · lượt được chấm, "x / tổng", ngày công bố, nhãn "Đã cập nhật" kèm dòng "Giảng viên sửa điểm a → b ngày …" khi sửa sau công bố, Nhận xét, Mục rubric đạt |

Route `/teaching/*` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm phân công lớp thật.
