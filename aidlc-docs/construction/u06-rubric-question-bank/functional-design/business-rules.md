# U06 Rubric & Question Bank - Business Rules

## 1. Phạm vi và quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-01 | Ngân hàng cấp môn: Chủ nhiệm môn và ADMIN tạo/sửa/kích hoạt/ngưng; mọi giảng viên có lớp thuộc môn được xem và dùng bản `ACTIVE`. | Câu 1 |
| BR-U06-02 | Ngân hàng cấp lớp: giảng viên của lớp, Chủ nhiệm môn, ADMIN tạo/sửa; chỉ người quản lý lớp đó thấy. | Câu 1 |
| BR-U06-03 | Nhân bản: câu/rubric cấp lớp → cấp môn chỉ Chủ nhiệm môn làm; cấp môn → cấp lớp mọi người quản lý lớp làm được; cấp lớp → cấp lớp khác chỉ giảng viên dạy cả hai lớp (FR-028). Bản nhân bản là `DRAFT` mới, lưu `clonedFrom`. | Câu 1 |
| BR-U06-04 | Ngoài phạm vi → "không tìm thấy". | SEC-002 |

## 2. Phiên bản

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-10 | Mỗi dòng là một phiên bản bất biến sau khi `ACTIVE`; chỉ `DRAFT` được sửa. | FR-016 |
| BR-U06-11 | Sửa bản `ACTIVE` tạo phiên bản `DRAFT` mới (`versionNo + 1`); mỗi `stableKey` tối đa 1 `DRAFT`. | FR-016, US-QBK-001 S2 |
| BR-U06-12 | Bài của U08 lưu `id` phiên bản; phiên bản mới **không** tự áp dụng và **không** báo cho bài đang dùng bản cũ. | Câu 7 |
| BR-U06-13 | Tìm kiếm hiển thị bản `ACTIVE` mới nhất mỗi `stableKey`; người quản lý xem được lịch sử phiên bản. | UC-QBK-02 |
| BR-U06-14 | `RETIRED`: không còn trong tìm kiếm để thêm vào bài mới; bài đang dùng vẫn đọc được. | Thiết kế |
| BR-U06-15 | Chỉ xóa được bản `DRAFT` chưa từng kích hoạt; bản đã `ACTIVE` không bao giờ xóa. | US-QBK-001 S2 |
| BR-U06-16 | Sửa câu hỏi của bài đang giao (US-QBK-002 S2, S3: snapshot lượt làm, gia hạn, làm lại) thuộc U08/U11, không thuộc U06. | Câu 7 |

## 3. Câu hỏi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-20 | `MCQ_SINGLE`: 2-6 lựa chọn, đúng 1 đáp án. `MCQ_MULTI`: ≥ 1 đáp án đúng. Lựa chọn không trùng nội dung. | FR-017 |
| BR-U06-21 | `ESSAY`: `stem` bắt buộc; không giới hạn số từ. | FR-017, U09 Câu 3 |
| BR-U06-22 | `DOCUMENT` (thay `DRAWIO`): `stem` bắt buộc; `skeleton` hợp lệ theo mô hình tài liệu của U09; `requiredDiagrams` mỗi loại 1-20. | U09 Câu 5-8 |
| BR-U06-23 | `CODE`: `language` thuộc danh sách ngôn ngữ cho phép (cấu hình, U13 hỗ trợ); 1-50 test case, ≥ 1 test không ẩn; `timeLimitMs` 100-10 000; tổng điểm test = `defaultPoints`. | FR-017 |
| BR-U06-24 | `rubricId` (nếu có) phải là rubric `ACTIVE` cùng phạm vi hoặc cấp môn của lớp. | US-QBK-001 |
| BR-U06-25 | `stem`, lựa chọn, hướng dẫn là markdown ≤ 20 000 ký tự, hiển thị đã làm sạch. | SEC-003 |
| BR-U06-26 | Kích hoạt câu hỏi yêu cầu `definition` hợp lệ theo loại. | FR-017 |
| BR-U06-27 | `lessonRefs` phải là chương/bài U05 thuộc cùng môn (hoặc lớp). | Câu 9 |

## 4. Rubric

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-30 | Rubric gồm 1-20 tiêu chí, mỗi tiêu chí 1-20 mục checklist; mỗi mục điểm > 0, tối đa 2 chữ số thập phân. | Câu 3, 5 |
| BR-U06-31 | `scaleMax` = tổng điểm mọi mục (tự tính, không nhập tay). | Câu 5 |
| BR-U06-32 | Chấm: mục đạt/không đạt; điểm tiêu chí = tổng mục đạt; điểm rubric = tổng tiêu chí. `score` từ chối `itemId` không thuộc rubric. | Câu 5 |
| BR-U06-33 | Rubric đã dùng chấm không bị ghi đè (BR-U06-10). | FR-016 |

## 5. Nhập hàng loạt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-40 | Nhận `.xlsx` hoặc `.csv` (UTF-8), mỗi loại câu hỏi một file mẫu riêng (`MCQ`, `ESSAY`, `DOCUMENT`, `CODE`); ≤ 500 dòng, ≤ 5 MB. | Câu 4, 6, 8 |
| BR-U06-41 | Kiểm từng dòng như tạo tay; dòng hợp lệ tạo câu `DRAFT`, dòng lỗi không tạo; trả kết quả từng dòng. | US-QBK-002 S1 |
| BR-U06-42 | Cột riêng: MCQ `lua_chon_1..6`, `dap_an_dung` (ví dụ `1,3`); ESSAY `goi_y_dap_an`; DOCUMENT `so_do_bat_buoc` (ví dụ `CLASS:1,SEQUENCE:2`; khung tài liệu chỉ tạo trên giao diện hoặc nhập DOCX); CODE `ngon_ngu`, `code_mau`, `test_cases` (JSON), `gioi_han_ms`. Cột chung: `tieu_de`, `noi_dung`, `diem`, `do_kho`, `tag`, `ma_bai`. | Câu 8 |
| BR-U06-43 | Nhập file chỉ tạo câu hỏi, không tạo rubric. | Thiết kế |

## 6. Audit và ngoài phạm vi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-50 | Audit: kích hoạt, ngưng, nhân bản lên cấp môn, nhập file (một sự kiện mỗi lần nhập). | FR-014 |
| BR-U06-51 | US-QBK-003 (phân tích chất lượng câu hỏi, Phase 2) chưa thiết kế, chờ nhóm hội ý. | Phase 2 |
