# U10 Quiz Authoring — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 19 | Question List (`m07`), popup Nhập từ tệp (`m07b`), Tạo nháp bằng AI (`m07c`) | `QuestionListPage`, `QuestionImportDialog`, `QuestionAiDraftDialog` | 46, 47 | Subject Manager |
| 20 | Question Detail (`m08`) | `QuestionDetailPage` | 46, 47 | Subject Manager |
| 21 | Quiz List (`m12`) | `SubjectQuizListPage` | 48 | Subject Manager |
| 22 | Quiz Detail (`t10`), popup Lấy từ ngân hàng: chọn tay (`t10b`), bốc ngẫu nhiên (`t10d`) | `QuizDetailPage`, `BankQuestionPicker` | 48 | Người có quyền với Quiz; giảng viên lớp chỉ xem Quiz của môn |
| 32 | Teacher Class Detail tab Đánh giá, phần Quiz (`t21`) | `ClassQuizzesSection` | 48 | Teacher, Subject Manager được giao dạy |
| 17, 18 | Số câu hỏi, Quiz trên Manager Dashboard, My Subject (`m11`, `m01`) | `QuizBankSummary` | 46, 48 | Subject Manager |

Tab "Lượt làm của sinh viên" trên Quiz Detail (`t10c`) do U11 gắn. U10 đăng ký `NavEntry` "Ngân hàng câu hỏi" và "Quiz" (Subject Manager).

## 2. Cây component

```
app/manager/questions/           QuestionListPage     #19 Question List
  QuestionFilters, QuestionTable
  QuestionImportDialog                                tệp mẫu, kết quả từng dòng
  QuestionAiDraftDialog                               yêu cầu, tài liệu, AiSourcePicker (U06), chấp nhận từng câu
app/manager/questions/[id]/      QuestionDetailPage   #20 Question Detail
  McqEditor, QuestionMetaPanel (độ khó, thẻ), QuestionVersionPanel, QuestionStateActions
app/manager/quizzes/             SubjectQuizListPage  #21 Quiz List
(TeacherClassTab "evals" của U05)
  ClassQuizzesSection                                 dòng Quiz của lớp và của môn trong bảng tab Đánh giá; Tạo Quiz, Copy Quiz lớp khác
    (vùng QuizRowAction cho U11)
(SubjectSummarySection của U05)
  QuizBankSummary                                     số câu hỏi, số Quiz theo trạng thái
app/quizzes/[id]/                QuizDetailPage       #22 Quiz Detail
  QuizTopBar                                          tên, nhãn trạng thái, phiên bản, "Đã lưu", nút thao tác, Thao tác khác
  QuizItemList                                        thứ tự, nguồn câu, điểm, tổng điểm
  BankQuestionPicker                                  chọn tay, bốc ngẫu nhiên
  InlineQuestionEditor                                dùng McqEditor
  QuizAiDraftPanel                                    AiSourcePicker, AiTaskStatus (U06), chấp nhận từng câu
  QuizSettingsPanel, MaterialRefPicker (khi U13 có)
  QuizPreviewDialog, ReviewButton, PublishButton, RetireDialog, CloneButton, NewVersionButton, DeleteButton
  (vùng QuizDetailSection cho U11: tab Lượt làm của sinh viên)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `QuestionListPage` | `GET /api/v1/subjects/{subjectId}/questions` | "Ngân hàng câu hỏi"; đường dẫn "Ngân hàng câu hỏi môn › mã môn"; nút chọn môn; Nhập từ tệp, Tạo nháp bằng AI, Thêm câu hỏi; tab Tất cả, Một đáp án, Nhiều đáp án; lọc Độ khó, Trạng thái; ô "Lọc theo nội dung hoặc thẻ"; cột Câu hỏi, Loại, Độ khó, Trạng thái (Nháp, Đang dùng, Ngưng; cờ "có bản nháp mới"), Cập nhật; phân trang; ghi chú "Sửa câu đang dùng sẽ mở bản nháp mới; quiz đã phát hành giữ phiên bản đã ghim." |
| `QuestionImportDialog` | `POST .../questions:import` | "Tệp .xlsx hoặc .csv theo mẫu: nội dung, loại, các lựa chọn, đáp án đúng, độ khó, thẻ"; ô tệp, Chọn tệp khác, Tải tệp mẫu; nhãn đếm số dòng, hợp lệ, lỗi; "Câu nhập vào ở trạng thái Nháp."; bảng Dòng, Câu hỏi, Số lựa chọn, Kết quả (Hợp lệ, hoặc lý do như cần 2–6 lựa chọn, loại không hỗ trợ); Hủy, Nhập n câu hợp lệ |
| `QuestionAiDraftDialog` | `POST .../question-ai-drafts` | Yêu cầu*, Tài liệu tham khảo (chỉ học liệu đã trích chữ), Nguồn AI đang chọn kèm "Đổi nguồn" (`AiSourcePicker`); Đề xuất (đã chấp nhận / tổng): mỗi câu có nội dung, lựa chọn, đáp án đúng, nút Bỏ, Chấp nhận; "Câu được chấp nhận vào ngân hàng ở trạng thái Nháp; bạn kiểm tra lại trước khi kích hoạt."; Đóng, Lưu n câu đã chấp nhận; không tự kích hoạt |
| `QuestionDetailPage` | `GET`, `PATCH`, `DELETE /api/v1/questions/{id}`; `POST .../versions`, `/activate`, `/retire` | Nút quay lại; "Chi tiết câu hỏi" kèm nhãn trạng thái và phiên bản; đường dẫn; nút Kích hoạt (câu Nháp) hoặc Ngưng (câu Đang dùng), Lưu thay đổi. Câu đang dùng: hộp "Câu này đang dùng trong n quiz. Lưu thay đổi sẽ mở bản nháp mới; quiz đã phát hành vẫn dùng phiên bản đã ghim." |
| `McqEditor` | như trên | Loại câu hỏi (Một đáp án, Nhiều đáp án), Nội dung câu hỏi*, Các lựa chọn (2–6) có nút chọn đáp án đúng, Thêm lựa chọn, Điểm (mặc định), Giải thích (tùy chọn); kiểm số lựa chọn, đáp án ngay trên form |
| `QuestionMetaPanel`, `QuestionVersionPanel` | như trên | "Phân loại": Độ khó (Dễ, Trung bình, Khó), Thẻ (tối đa 10). "Trạng thái": trạng thái, phiên bản · ngày, người tạo, số quiz đang dùng |
| `SubjectQuizListPage` | `GET /api/v1/subjects/{subjectId}/quizzes` | "Quiz", ghi chú "Quiz của môn chỉ dùng để luyện tập (Practice): không có lịch, không vào sổ điểm."; nút chọn môn, Tạo quiz; tab Tất cả, Nháp, Đã duyệt, Đang mở, Ngưng; ô tìm; cột Quiz, Số câu, Thời gian, Trạng thái, Cập nhật |
| `ClassQuizzesSection` | `GET /api/v1/classes/{classId}/quizzes`, `GET .../quiz-copy-sources`, `POST .../quizzes:copy` | Trong bảng chung của tab Đánh giá (cùng phần Bài tập của U07): nút Tạo Quiz, Copy Quiz từ lớp khác; dòng Quiz (tên, "Quiz · Practice"), Phạm vi (Của môn, Của lớp), Trạng thái, cột Bài nộp do U11 gắn (số sinh viên đã làm); Quiz của môn chỉ xem |
| `QuizTopBar`, `QuizDetailPage` | `GET`, `PATCH /api/v1/quizzes/{id}`; `/review`, `/publish`, `/retire`, `/clone`, `/versions` | Đường dẫn "Quiz", tên, nhãn trạng thái, phiên bản, "Đã lưu hh:mm"; Xem trước, Lưu, Duyệt (Nháp) hoặc Phát hành (Đã duyệt); "Thao tác khác": Ngưng (bắt buộc lý do), Nhân bản, Tạo version mới, Xóa (chỉ bản nháp); lỗi duyệt hiện theo câu; giảng viên có thêm tab Lượt làm của sinh viên (U11) |
| `QuizItemList`, `InlineQuestionEditor` | `PUT .../items`, `.../inline-questions` | "Câu hỏi (n)", tổng điểm; mỗi câu: thứ tự, loại, nguồn ("Từ ngân hàng · vN" hoặc câu riêng của Quiz), điểm, nội dung, lựa chọn và đáp án đúng; Thêm câu, Lấy từ ngân hàng, Tạo nháp bằng AI; `409` báo tải lại |
| `BankQuestionPicker` | `GET /api/v1/subjects/{subjectId}/questions`, `POST .../items:random-preview` | Popup "Lấy câu hỏi từ ngân hàng", hai tab. Chọn tay: lọc Độ khó, Thẻ, ô tìm; bảng chọn nhiều (Câu hỏi, Loại, Độ khó, Thẻ); chỉ câu Đang dùng, ẩn câu đã có trong quiz; "Đã chọn n câu"; Thêm n câu. Bốc ngẫu nhiên: Số câu, Độ khó, Thẻ, Bốc thử ("Có n câu phù hợp"), bảng xem trước; Lưu n câu vào quiz (chỉ xem trước, lưu mới ghim phiên bản) |
| `QuizSettingsPanel`, `MaterialRefPicker` | `PATCH /api/v1/quizzes/{id}` | "Cấu hình": Hướng dẫn (tùy chọn), Thời gian làm bài (phút, 1–300 hoặc để trống), Số lượt tối đa (1–10 hoặc không giới hạn), Hiện điểm sau khi nộp (công tắc), Hiện đáp án (Không bao giờ, Sau khi nộp), Đảo thứ tự câu, Đảo thứ tự lựa chọn, Ghi nhận hoạt động kèm Lời báo cho sinh viên, Gắn với học liệu (tối đa 10, khi U13 có), Tổng điểm |
| `QuizAiDraftPanel` | `.../ai-drafts` | Chấp nhận từng câu; không tự kích hoạt |
| `QuizPreviewDialog` | `GET /api/v1/quizzes/{id}` | Xem như sinh viên, không hiện đáp án |
| `QuizBankSummary` | `GET .../questions`, `GET .../quizzes` (đếm) | Dòng tóm tắt trên thẻ môn (Manager Dashboard); thẻ số liệu "Câu hỏi trắc nghiệm" (n đang dùng · n nháp) và "Quiz" (n đang mở · n đã duyệt · n nháp) trên My Subject |

Route `/manager/*` cho Subject Manager, `/quizzes/*` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.

| Slot U10 khai báo | Unit gắn | Dùng để |
|---|---|---|
| `QuizDetailSection` | U11 | Tab Lượt làm của sinh viên trên Quiz Detail |
| `QuizRowAction` | U11 | Số sinh viên đã làm trên dòng Quiz ở tab Đánh giá |
