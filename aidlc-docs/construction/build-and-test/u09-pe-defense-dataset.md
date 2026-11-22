# Dataset PE cho buổi bảo vệ U09

Bộ dữ liệu có **20 ca hồi quy có thể chạy** và **20 chỗ thu bài chấm độc lập**. Ca hồi quy dùng điểm tổng hợp do nhóm thiết kế. Phiếu thu bài còn trống và chưa có điểm giảng viên.

## Danh sách ca

Mỗi tên dataset là một thư mục dưới [grading](../../../backend/src/test/resources/ai-eval/grading/). Một thư mục chứa một đề với rubric riêng và các bài nộp dạng JSON hoặc Draw.io XML.

| Dataset | Ca | Tình huống kiểm tra |
|---|---|---|
| `badminton-booking` | 8 ca hiện có | Bài đầy đủ, thiếu nội dung, lạc đề, chèn chỉ dẫn, sai ký pháp và sai actor |
| `swr302-lab-equipment` | `swr-lab-complete` | Sơ đồ và đặc tả đúng phạm vi đặt thiết bị, actor và luồng nghiệp vụ |
| `swr302-lab-equipment` | `swr-lab-solid-required` | Nét liền làm giảm điểm mục ký pháp khi rubric yêu cầu nét đứt |
| `swr302-lab-equipment` | `swr-lab-wrong-secondary` | Sai tên một actor phụ làm giảm điểm đúng phần actor đó trong đặc tả |
| `swr302-lab-equipment-relations` | `swr-lab-solid-semantic` | Cùng bài nộp với ca nét liền. Rubric chấm ngữ nghĩa quan hệ nên giữ điểm ở các mục đó |
| `swr302-room-scheduling` | `swr-room-complete` | Sơ đồ, luồng ngoại lệ và NFR có chỉ số để kết luận đạt |
| `swr302-room-scheduling` | `swr-room-vague-nfr` | Thiếu điều kiện đo làm giảm điểm mục khả năng kiểm thử của NFR |
| `swd392-workshop-registration` | `swd-workshop-complete` | ERD khái niệm, ERD logical và sequence REST đủ thông tin |
| `swd392-workshop-registration` | `swd-workshop-missing-fk` | Thiếu FK chỉ ảnh hưởng mục logical ERD |
| `swd392-workshop-registration` | `swd-workshop-no-authorization` | Đăng nhập có nhưng thiếu kiểm quyền trước khi ghi |
| `swd392-internship-portal` | `swd-internship-valid-alternative` | Thiết kế thay thế đáp ứng ràng buộc của đề và có lý do |
| `swd392-internship-portal` | `swd-internship-wrong-guard` | Điều kiện chuyển sai chỉ ảnh hưởng mục guard |
| `swd392-internship-portal` | `swd-internship-pattern-name-only` | Tên pattern đọc được. Phần giải thích cơ chế bị cắt nên cần giảng viên xem |

Điểm trong `expected` là các nhãn `SYNTHETIC_TEAM`. `expectedBehavior` mô tả điều QA kiểm. Runner Java dựng prompt từ đề, rubric và bài nộp. Điểm kỳ vọng và mô tả điều QA kiểm nằm ngoài prompt chấm.

## Thu bài để đối chiếu với giảng viên

1. Mở [4 đề và rubric chấm](u09-pe-holdout-rubrics.md). Mỗi môn có hai đề khác với bộ dùng để hiệu chỉnh.
2. Sao chép [phiếu nhập điểm CSV](u09-pe-holdout-scores.csv) ra thư mục riêng. Mỗi đề có 5 mã bài, mỗi bài có 6 hàng để nhập điểm.
3. Thu bài đã ẩn danh và được phép dùng. Điền `source_type` là `ANONYMIZED_REAL` hoặc `SYNTHETIC`. Để giảng viên chấm từng mục, ghi căn cứ. Giữ các ô chưa có nhãn ở trạng thái `PENDING`.
4. Sau khi chốt prompt và model, chạy chấm trên bộ độc lập. Chỉ gắn `LECTURER_VERIFIED` cho nhãn đã được xác nhận. Ghi riêng nguồn bài thật hoặc bài tổng hợp.

## Chạy kiểm thử

Chạy hai lệnh đầu từ thư mục gốc repo. Dùng Java 17 cho Maven.

```powershell
mvn -o -B -ntp -f backend/pom.xml '-Dtest=GradingEvalHarnessTest' '-Dai.eval.mode=disabled' test
.\scripts\ai-grading-eval\run-tests.ps1
```

Để replay, dùng `results.json` và thư mục `recordings/` của **cùng đề và phiên bản rubric**. Đổi `$results` thành đường dẫn tới lượt đã lưu.

```powershell
$dataset = '.\backend\src\test\resources\ai-eval\grading\swr302-lab-equipment'
& .\scripts\ai-grading-eval\run-evaluation.ps1 -Mode replay -Dataset $dataset -Results $results -Case swr-lab-solid-required
```

Replay cần bản ghi thật của lượt trước. Kiểm thử fixture và chạy Python unit test không gọi AI. Nếu chạy Live cho một đề mới, dùng `-Dataset`, giới hạn `-Case` và `-Repeats 1`. Xem [hướng dẫn U09](ai-grading-evaluation-instructions.md) để bật Live bằng khóa của phiên chạy.

Trong báo cáo bảo vệ, ghi độ phủ các ca, sai lệch điểm từng mục và tổng điểm trên bài có nhãn giảng viên. Ghi lỗi provider, lỗi định dạng, kết quả review và chất lượng feedback ở các cột tương ứng.

## Trạng thái kiểm chứng

- [x] Soạn 12 ca đối chứng tổng hợp và giữ 8 ca badminton-booking.
- [x] Chuẩn bị 4 đề độc lập và phiếu thu điểm cho 20 bài.
- [x] Kiểm tra 12 ca qua adapter Python và bộ dựng prompt Java. Chạy 24 kiểm thử Java, 17 kiểm thử Python và replay một ca đã lưu qua PowerShell.
- [ ] Thu bài, xác nhận điểm giảng viên và chạy đánh giá trên bộ độc lập.
