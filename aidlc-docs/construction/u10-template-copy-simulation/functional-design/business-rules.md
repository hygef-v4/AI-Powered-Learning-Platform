# U10 Template, Copy & Simulation - Business Rules

## 1. Template cấp môn

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-01 | Chỉ Chủ nhiệm môn soạn template (bài U08 `SUBJECT_TEMPLATE`, dùng cùng trình soạn) và phát hành version template. | FR-027 |
| BR-U10-02 | Template không có publication; không giao thẳng cho lớp. | FR-027, không có đề chung |
| BR-U10-03 | Version template đã phát hành chỉ đọc; sửa template tạo version mới (quy tắc BR-U08-43 áp dụng: version đang phát hành không sửa). | FR-027, US-ASM-009 S2 |
| BR-U10-04 | Rút template (`WITHDRAWN`): không copy thêm; bản đã copy không bị ảnh hưởng. | Thiết kế |
| BR-U10-05 | Giảng viên của lớp thuộc môn copy một version `RELEASED` thành bài `DRAFT` của lớp mình; lưu lineage `TEMPLATE_COPY`; không đồng bộ khi template có version mới. | US-ASM-009 S1 |

## 2. Copy giữa lớp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-10 | Giảng viên phải dạy cả lớp nguồn và lớp đích; lớp đích ngoài quyền → "không tìm thấy". | FR-028, US-ASM-010 S2 |
| BR-U10-11 | Copy tạo bài `DRAFT` mới ở lớp đích (identity mới, lineage `CLASS_COPY`), gồm thành phần, điểm, hướng dẫn, cấu hình loại bài, khung tài liệu. | FR-028 |
| BR-U10-12 | Không copy lịch, publication, lượt làm, bài nộp, điểm. | FR-028 |
| BR-U10-13 | Câu/rubric ngân hàng cấp môn: giữ nguyên tham chiếu version. Câu/rubric ngân hàng cấp lớp nguồn: sao chép sang ngân hàng lớp đích qua U06 (identity/version riêng) rồi trỏ tới bản mới. | FR-028 |
| BR-U10-14 | Copy riêng rubric giữa hai lớp: dùng U06 (BR-U06-03). | FR-028 |

## 3. Version và diff

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-20 | Xem diff giữa hai version cùng `stableKey`, hoặc giữa bài copy và nguồn trong lineage. | US-ASM-008 S1, Câu 4 |
| BR-U10-21 | Người xem diff phải xem được cả hai bài (giảng viên lớp; Chủ nhiệm môn với template và bài copy từ template của môn). | SEC-002 |
| BR-U10-22 | Diff hiển thị: hướng dẫn (theo dòng), câu thêm/bớt/đổi thứ tự/đổi điểm/đổi nội dung, cấu hình loại bài, tổng điểm. | US-ASM-008 |

## 4. Thi thử (simulation)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-30 | Mọi loại bài phát hành được dạng thi thử (`deliveryMode = SIMULATION`) cho một lớp. | Câu 2 |
| BR-U10-31 | `maxAttempts` để trống = không giới hạn; có giá trị thì ≥ 1. | Câu 3 |
| BR-U10-32 | `resultPolicy`: `HIGHEST`, `LATEST`, `AVERAGE`; chỉ tính trên lượt đã có điểm (bài cần giảng viên chấm thì chờ). | FR-029, Câu 2 |
| BR-U10-33 | `answerRelease`: `AFTER_ATTEMPT` (sau mỗi lượt, chỉ với phần tự chấm), `AFTER_CLOSE`, `NEVER`. | FR-029 |
| BR-U10-34 | `countsTowardGrade`: tắt → chỉ luyện tập, không vào điểm chính thức (U15). | FR-029 |
| BR-U10-35 | Chính sách khóa khi lượt đầu tiên bắt đầu (`lockedAt`); sau đó chỉ kéo dài được cửa sổ (theo U08). | FR-029 |
| BR-U10-36 | Giao diện luôn ghi "Thi thử", số lượt (hoặc "không giới hạn"), cách lấy kết quả, có/không tính điểm; không hiển thị như thi chính thức. | US-ASM-011 S3 |

## 5. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-40 | Audit: phát hành/rút template, copy template, copy giữa lớp (nguồn, đích, actor), tạo/sửa chính sách thi thử. | FR-014, FR-027, FR-028 |
