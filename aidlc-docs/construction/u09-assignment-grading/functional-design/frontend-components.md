# U09 Assignment Grading — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Teacher Class Detail, tab Evals (Sổ điểm; lối vào bài nộp trên dòng bài) | `ClassGradebookSection`, `SubmissionsLink` | 42, 45 | Teacher, Subject Manager được giao dạy |
| Submission Detail | `SubmissionDetailPage` | 42, 43, 44 | Như trên |
| Grading Workspace | `GradingWorkspacePage` | 42, 43, 44 | Như trên |
| Student Assignments, Submission History (phần điểm) | `StudentGradeCell`, `StudentGradePanel` | 37, 40 | Student |

## 2. Cây component

```
(tab Evals, đăng ký qua TeacherClassTab của U05)
  ClassGradebookSection                      sinh viên đóng mở, điểm từng bài, nút Xuất
    GradebookExportDialog                    CSV hoặc XLSX, tùy chọn một bài
(dòng bài ở tab Evals, đăng ký qua AssignmentRowAction của U07)
  SubmissionsLink                            "Bài nộp" kèm số đã nộp, chờ chấm
app/teaching/classes/[classId]/assignments/[assignmentId]/submissions/   SubmissionDetailPage
  ProgressSummary                            đã nộp, đang làm, chưa nộp, trễ, thời gian còn lại
  SubmissionTable, SubmissionFilters
  BatchAiDialog                              AiSourcePicker (U06), kết quả từng mục
  BatchFinalizeDialog, BatchPublishDialog
app/teaching/evaluations/[evaluationId]/      GradingWorkspacePage
  SubmissionViewer                           DocumentViewer (U07); bài nhóm theo phần kèm người làm
  RubricChecklistPanel                       mục đạt từng phần, điểm tự tính
  AdjustmentField, FeedbackEditor
  AiProposalPanel                            AiSourcePicker, AiTaskStatus (U06), các version đề xuất, Dùng đề xuất
  MemberContributionPanel                    bài nhóm
  ActivityTimeline (U08)                     khi bài bật theo dõi
  GradeHistoryPanel, FinalizeButton, PublishButton, PrevNextNav
(đăng ký vào slot của U08)
  StudentGradeCell                           điểm đã công bố, GradeDistributionBadge
  StudentGradePanel                          điểm, nhận xét, mục rubric đạt, nhãn "đã cập nhật"
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubmissionDetailPage` | `GET .../assignments/{assignmentId}/submissions` | Lọc trạng thái, trễ; chọn nhiều cho chấm AI, chốt, công bố |
| `BatchAiDialog` | `POST /api/v1/evaluations:ai-proposals` | Hiện kết quả từng mục; không tự đổi nguồn khi hết quota |
| `BatchFinalizeDialog`, `BatchPublishDialog` | `POST /api/v1/evaluations:finalize`, `:publish` | Hộp xác nhận; kết quả từng mục |
| `GradingWorkspacePage`, `RubricChecklistPanel`, `AdjustmentField`, `FeedbackEditor` | `GET`, `PUT /api/v1/evaluations/{id}` | Điểm tự tính; điều chỉnh và sửa sau chốt bắt buộc lý do; `409` báo tải lại; lưu xong sang bài kế |
| `AiProposalPanel` | `POST`, `GET .../ai-proposals` | Đề xuất `STALE` không cho dùng; Dùng đề xuất chỉ điền sẵn |
| `MemberContributionPanel` | `PUT .../members/{accountId}` | Mặc định bằng điểm tài liệu chung |
| `ClassGradebookSection`, `GradebookExportDialog` | `GET .../gradebook`, `GET .../gradebook/export` | Không có điểm tổng; tải tệp dạng luồng |
| `StudentGradeCell`, `StudentGradePanel` | `GET /api/v1/me/grades`, `/me/grades/attempts/{attemptId}`, `/me/grade-distributions` | Chỉ điểm đã công bố của chính mình |

Route `/teaching/*` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm phân công lớp thật.
