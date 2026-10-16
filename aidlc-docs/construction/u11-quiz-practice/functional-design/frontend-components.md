# U11 Quiz Practice — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Quiz Practice History | `QuizPracticeHistoryPage` | 49 | Student |
| Quiz Practice Detail | `QuizPracticeDetailPage` | 50 | Student |
| Quiz Taking | `QuizTakingPage` | 51 | Student |
| Quiz Result | `QuizResultPage` | 52 | Student |
| Learning Material (phần Quiz) | `MaterialQuizList` | 50, 51 | Student |
| Quiz Detail (phần Lượt làm) | `QuizAttemptsSection` | 42 | Teacher, Subject Manager được giao dạy |

## 2. Cây component

```
app/quiz-practice/                        QuizPracticeHistoryPage   Quiz Practice History
  QuizPracticeFilters, QuizPracticeTable
app/quiz-practice/[quizId]/               QuizPracticeDetailPage    Quiz Practice Detail
  QuizInfoCard, ActivityNotice, MyQuizAttempts, StartQuizButton
  (vùng QuizMaterialSection cho U13)
app/quiz-attempts/[attemptId]/            QuizTakingPage            Quiz Taking
  QuestionView, QuestionNavigator, QuizTimer, AutosaveStatus, SubmitQuizDialog
  (useActivityTracker của U08 khi bật theo dõi)
app/quiz-attempts/[attemptId]/result/     QuizResultPage            Quiz Result
  ScoreSummary, AnswerReview, RetryButton
(đăng ký vào slot)
  MaterialQuizList                        LearningMaterialSection của U13
  QuizAttemptsSection                     QuizDetailSection của U10; ActivityTimeline (U08)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `QuizPracticeHistoryPage` | `GET /api/v1/me/quizzes` | Lọc theo lớp; Quiz chưa làm có nhãn |
| `QuizPracticeDetailPage`, `StartQuizButton` | `GET /api/v1/me/quizzes/{quizId}`, `POST /api/v1/quizzes/{quizId}/attempts` | Ẩn nút và nêu lý do khi không làm được |
| `QuizTakingPage`, `QuizTimer`, `AutosaveStatus` | `GET /api/v1/quiz-attempts/{attemptId}`, `PUT .../answers` | Lưu 2 giây sau lần chọn cuối; hết giờ thì lưu lần cuối và chuyển sang kết quả |
| `SubmitQuizDialog` | `POST .../submit` | Báo số câu chưa trả lời |
| `QuizResultPage`, `AnswerReview` | `GET .../result` | Chỉ hiện điểm, đáp án theo cài đặt |
| `MaterialQuizList` | `GET /api/v1/me/quizzes?materialId=` | Lượt đã dùng, còn lại, nút Làm Quiz |
| `QuizAttemptsSection` | `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`, `GET .../quiz-attempts/{attemptId}` | Chỉ đọc; dòng thời gian hoạt động khi có |

Route `/quiz-practice/*` và `/quiz-attempts/*` chỉ cho Student (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
