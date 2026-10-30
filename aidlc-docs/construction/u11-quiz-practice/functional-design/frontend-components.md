# U11 Quiz Practice — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 42 | Quiz Practice History (`s20`) | `QuizPracticeHistoryPage` | 49 | Student |
| 43 | Quiz Practice Detail (`s21`), trạng thái hết lượt (`s21b`) | `QuizPracticeDetailPage` | 50 | Student |
| 44 | Quiz Taking (`s14`), popup Nộp bài (`s14b`), Hết giờ tự nộp (`s14c`) | `QuizTakingPage`, `SubmitQuizDialog`, `TimeUpOverlay` | 51 | Student |
| 45 | Quiz Result (`s22`), Quiz không hiện điểm (`s22b`) | `QuizResultPage` | 52 | Student |
| 37 | Learning Material, phần Quiz gắn với học liệu (`s05`, `s05b`) | `MaterialQuizList` | 50, 51 | Student |
| 22 | Quiz Detail, tab Lượt làm của sinh viên (`t10c`) | `QuizAttemptsSection` | 42 | Teacher, Subject Manager được giao dạy |
| 32 | Tab Đánh giá, cột Bài nộp của dòng Quiz (`t21`) | `QuizAttemptCount` | 48 | Teacher, Subject Manager được giao dạy |

U11 đăng ký `NavEntry` "Luyện tập Quiz" (Student).

## 2. Cây component

```
app/quiz-practice/                        QuizPracticeHistoryPage   #42
  QuizPracticeFilters, QuizPracticeTable
app/quiz-practice/[quizId]/               QuizPracticeDetailPage    #43
  MyQuizAttempts, QuizInfoCard, ActivityNotice, StartQuizButton
  (vùng QuizMaterialSection cho U13)
app/quiz-attempts/[attemptId]/            QuizTakingPage            #44
  QuizTopBar (đồng hồ, tự lưu, Nộp bài), QuestionView, QuestionNavigator, AutosaveStatus
  SubmitQuizDialog, TimeUpOverlay
  (useActivityTracker của U08 khi bật theo dõi)
app/quiz-attempts/[attemptId]/result/     QuizResultPage            #45
  ScoreSummary, AnswerReview, RetryButton
(đăng ký vào slot)
  MaterialQuizList                        LearningMaterialSection của U13
  QuizAttemptsSection                     QuizDetailSection của U10; ActivityTimeline (U08)
  QuizAttemptCount                        QuizRowAction của U10
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `QuizPracticeHistoryPage` | `GET /api/v1/me/quizzes` | "Luyện tập Quiz", "Quiz Practice không tính vào sổ điểm"; lọc theo lớp; cột Quiz, Lớp, Số lần làm, Điểm cao nhất, Lần gần nhất, thao tác Xem hoặc Bắt đầu (Quiz chưa làm) |
| `QuizPracticeDetailPage`, `StartQuizButton` | `GET /api/v1/me/quizzes/{quizId}`, `POST /api/v1/quizzes/{quizId}/attempts` | Nút quay lại; tên kèm nhãn Practice; đường dẫn "Luyện tập Quiz › lớp"; "Các lần làm" (số lần đã hoàn thành): Lần làm, Bắt đầu, Thời gian, Điểm (ẩn khi Quiz không hiện điểm), Xem kết quả; Học liệu gắn kèm (U13); thẻ Thông tin: Số câu, Thời gian, Số lượt (đã dùng / tối đa hoặc không giới hạn), Điểm cao nhất, Đáp án (Hiện sau khi nộp hoặc Không hiện); nút Làm lại kèm "Còn x / y lượt · không tính vào sổ điểm"; không làm được thì ẩn nút và nêu lý do (ví dụ "Bạn đã dùng hết y/y lượt", Quiz đã ngưng); thông báo ghi nhận hoạt động khi Quiz bật theo dõi |
| `QuizTakingPage`, `AutosaveStatus` | `GET /api/v1/quiz-attempts/{attemptId}`, `PUT .../answers` | Thanh trên: Thoát, tên, nhãn Practice, "Tự động lưu", đồng hồ "Còn mm:ss" (khi có giới hạn thời gian), Nộp bài; "Câu i / n" · loại · điểm; nội dung; lựa chọn (một hoặc nhiều đáp án); Câu trước, Câu tiếp; "Danh sách câu": số đã làm, nút từng câu theo trạng thái (đã trả lời, đang trả lời, chưa trả lời). Lưu 2 giây sau lần chọn cuối |
| `SubmitQuizDialog` | `POST .../submit` | "Nộp bài?": số câu chưa trả lời và câu nào, "Câu chưa trả lời được tính sai", thời gian còn lại, "Sau khi nộp, lượt này kết thúc."; Làm tiếp, Nộp bài |
| `TimeUpOverlay` | `POST .../submit` | Hết giờ thì lưu lần cuối, tự nộp, khóa trang: "Hết thời gian làm bài. Các câu đã chọn được tự lưu và nộp. Đang chuyển sang trang kết quả…" |
| `QuizResultPage`, `ScoreSummary`, `AnswerReview` | `GET .../result` | Nút quay lại; "Kết quả · tên"; đường dẫn; "Lần n · nộp lúc … · làm trong n phút". Quiz hiện điểm: điểm x / y, Đúng, Sai, Bỏ trống, "Practice · không tính vào sổ điểm". Quiz hiện đáp án sau khi nộp: "Xem lại từng câu": trạng thái câu, lựa chọn có nhãn "Đáp án đúng" và "Bạn chọn", Giải thích. Quiz không hiện điểm: "Đã nộp bài", "Quiz này không hiện điểm và đáp án sau khi nộp." Nút Về danh sách, Làm lại (khi còn lượt) |
| `MaterialQuizList` | `GET /api/v1/me/quizzes?materialId=` | "Quiz gắn với học liệu": tên, "Practice · n câu · n phút", "Đã dùng x / y lượt", Điểm cao nhất, nút Làm quiz; hết lượt thì "Bạn đã dùng hết y/y lượt làm quiz này" |
| `QuizAttemptsSection` | `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`, `GET .../quiz-attempts/{attemptId}` | Tab "Lượt làm của sinh viên (n)": cột Sinh viên (avatar, tên, email), Lượt (x / y), Điểm cao nhất, Hoạt động (tóm tắt rời trang, ẩn tab hoặc "Không có"); "n / N sinh viên đã làm"; phân trang; "Chỉ đọc. Quiz Practice không vào sổ điểm."; chọn sinh viên hiện "Dòng thời gian hoạt động" của từng lượt (bắt đầu, rời trang kèm thời lượng, nộp bài kèm điểm), "Dữ liệu hoạt động chỉ để tham khảo; nền tảng không kết luận gian lận." |
| `QuizAttemptCount` | `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts` (tóm tắt) | Cột Bài nộp của dòng Quiz: "n sinh viên đã làm" |

Route `/quiz-practice/*` và `/quiz-attempts/*` chỉ cho Student (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
