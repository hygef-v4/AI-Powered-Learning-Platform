# U15 Grading - Business Logic Model

## F1 - Nhận bài nộp
1. U11 nộp gọi `SubmissionSubmittedPort.onSubmitted` (U15 cài) trong transaction nộp → tạo job `GRADE_INIT {attemptId}`. Job: tạo `Grade` `ATTEMPT` `PENDING`, `maxScore` = tổng điểm bài.
2. Trắc nghiệm → tự chấm ngay trong job (BR-U15-10), áp BR-U15-12. Code Lab → gọi `CodeRunPort.grade` (U13).
3. U14 nộp gọi `GroupSubmittedPort.onGroupSubmitted` → job `GRADE_INIT {groupSubmissionId}`: `Grade` `GROUP_DOCUMENT` + `MEMBER_CONTRIBUTION`/`MEMBER_FINAL` cho mỗi thành viên, `PENDING` (bản nộp mới thay bản cũ: điểm chưa chốt gắn sang bản mới).
4. U13 chấm xong gọi `CodeGradedPort.onGraded` (U15 cài) → cập nhật `autoScore`/`items`, `DRAFT` (BR-U15-11).

## F2 - Chấm
1. Giảng viên mở bài: nội dung (U11/U14), đề, rubric, điểm tự chấm, đề xuất AI nếu có.
2. Chấm tay (BR-U15-21) hoặc nhờ AI (BR-U15-22, 23) → `DRAFT`.

## F3 - Chốt
1. Từng bài hoặc hàng loạt (BR-U15-31) → `FINALIZED`; lịch sử; audit.

## F4 - Công bố
1. Bấm công bố cho lượt phát hành (BR-U15-32) → `PUBLISHED` hàng loạt; event.

## F5 - Sửa điểm
1. Lý do bắt buộc (BR-U15-33); lịch sử; event nếu đã công bố.

## F6 - Bài nhóm
1. Màn hình đối chiếu: tài liệu nhóm, từng thành viên (mục đã viết), đề xuất AI phần đóng góp, ba vùng điểm (BR-U15-40…43).

## F7 - Sổ điểm
1. Giảng viên/ADMIN/Chủ nhiệm môn: ma trận (BR-U15-50); người học: điểm của mình (BR-U15-51); lịch sử (BR-U15-52).
