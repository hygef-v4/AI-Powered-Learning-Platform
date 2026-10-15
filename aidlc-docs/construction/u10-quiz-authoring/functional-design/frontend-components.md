# U10 Quiz Authoring — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Question List | `QuestionListPage` | 46, 47 | Subject Manager |
| Question Detail | `QuestionDetailPage` | 46, 47 | Subject Manager |
| Quiz List | `SubjectQuizListPage` | 48 | Subject Manager |
| Teacher Class Detail, tab Evals (phần Quiz) | `ClassQuizzesSection` | 48 | Teacher, Subject Manager được giao dạy |
| Quiz Detail | `QuizDetailPage` | 48 | Người có quyền với Quiz; giảng viên lớp chỉ xem Quiz của môn |

## 2. Cây component

```
app/manager/questions/           QuestionListPage     Question List
  QuestionFilters, QuestionTable
  QuestionImportDialog                                tệp mẫu, kết quả từng dòng
  QuestionAiDraftDialog                               yêu cầu, tài liệu, AiSourcePicker (U06), chấp nhận từng câu
app/manager/questions/[id]/      QuestionDetailPage   Question Detail
  McqEditor, QuestionVersionHistory, QuestionStateActions
app/manager/quizzes/             SubjectQuizListPage  Quiz List
(tab Evals, đăng ký qua TeacherClassTab của U05)
  ClassQuizzesSection                                 Quiz của lớp và của môn, Tạo Quiz, Copy Quiz lớp khác
app/quizzes/[id]/                QuizDetailPage       Quiz Detail
  QuizInfoPanel, QuizSettingsPanel, MaterialRefPicker (khi U13 có)
  QuizItemList                                        thứ tự, điểm, tổng điểm
  BankQuestionPicker                                  chọn tay, bốc ngẫu nhiên
  InlineQuestionEditor                                dùng McqEditor
  QuizAiDraftPanel                                    AiSourcePicker, AiTaskStatus (U06), chấp nhận từng câu
  QuizPreviewDialog, ReviewButton, PublishButton, RetireDialog, CloneButton, NewVersionButton, DeleteButton
  (vùng QuizDetailSection cho U11)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `QuestionListPage`, `QuestionDetailPage` | `/api/v1/subjects/{subjectId}/questions`, `/api/v1/questions/{id}` | Hiện cờ "có bản nháp"; sửa bản `ACTIVE` mở bản nháp mới |
| `QuestionImportDialog` | `POST .../questions:import` | Tải tệp mẫu; bảng kết quả từng dòng |
| `QuizDetailPage`, `QuizItemList`, `BankQuestionPicker` | `GET`, `PATCH /api/v1/quizzes/{id}`, `PUT .../items`, `POST .../items:random-preview` | Bốc ngẫu nhiên chỉ xem trước, lưu mới ghim; `409` báo tải lại |
| `InlineQuestionEditor` | `.../inline-questions` | Kiểm số lựa chọn, đáp án ngay trên form |
| `QuizAiDraftPanel`, `QuestionAiDraftDialog` | `.../ai-drafts`, `.../question-ai-drafts` | Chấp nhận từng câu; không tự kích hoạt |
| `QuizPreviewDialog` | `GET /api/v1/quizzes/{id}` | Không hiện đáp án |
| `ReviewButton`, `PublishButton`, `RetireDialog` | `/review`, `/publish`, `/retire` | Lỗi duyệt hiện theo câu; ngưng bắt buộc lý do |

Route `/manager/*` cho Subject Manager, `/quizzes/*` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
