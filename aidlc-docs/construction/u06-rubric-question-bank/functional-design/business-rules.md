# U06 Rubric & Question Bank - Business Rules

**Bản tài liệu 2026-10-08**: UC 32, 33, 44, 55, 56; primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Phạm vi và quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-01 | Subject Question Bank UC 55–56: Subject Manager/Administrator được giao môn R2 tạo/sửa/xóa theo lifecycle. Teacher chỉ đọc/dùng câu ACTIVE được phép trong selector soạn bài, không quản trị/mở bank môn độc lập. | FR-016, R2 |
| BR-U06-02 | Class Question Bank UC 32–33: Teacher hoặc Subject Manager/Administrator được giao dạy lớp R3/R4. Quản lý môn/Full cấu trúc không đủ đọc/sửa bank lớp. | FR-016, R3/R4 |
| BR-U06-03 | Clone lớp → môn cần đọc lớp R3/R4 và ghi môn R2; môn → lớp cần dùng câu ACTIVE hợp lệ và ghi lớp R3/R4; lớp → lớp cần dạy cả hai lớp. Bản clone DRAFT với clonedFrom; rubric clone theo bài. | FR-028 |
| BR-U06-04 | Ngoài phạm vi → "không tìm thấy". | SEC-002 |

## 2. Phiên bản

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-10 | Mỗi dòng là một phiên bản bất biến sau khi `ACTIVE`; chỉ `DRAFT` được sửa. | FR-016 |
| BR-U06-11 | Sửa bản `ACTIVE` tạo phiên bản `DRAFT` mới (`version + 1`, cùng `lineage_id`); mỗi `lineage_id` tối đa 1 `DRAFT`. | FR-016, US-QBK-001 S2 |
| BR-U06-12 | Bài của U08 lưu `id` phiên bản; phiên bản mới **không** tự áp dụng và **không** báo cho bài đang dùng bản cũ. | Câu 7 |
| BR-U06-13 | Tìm kiếm hiển thị bản `ACTIVE` mới nhất mỗi `lineage_id`; người quản lý xem được lịch sử phiên bản. | UC 39, 40, 41, 42, 43 |
| BR-U06-14 | `RETIRED`: không còn trong tìm kiếm để thêm vào bài mới; bài đang dùng vẫn đọc được. | Thiết kế |
| BR-U06-15 | Xóa DRAFT chưa dùng/chưa từng ACTIVE theo scope/version; bản có tham chiếu hoặc từng ACTIVE chỉ RETIRED, ẩn khỏi chọn mới và giữ version cho bài/lượt cũ; không xóa lịch sử. | FR-016, UC 33/56 |
| BR-U06-16 | Sửa câu hỏi của bài đang giao (US-QBK-002 S2, S3: snapshot lượt làm, gia hạn, làm lại) thuộc U08/U11, không thuộc U06. | Câu 7 |

## 3. Câu hỏi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-20 | `MCQ_SINGLE`: 2-6 lựa chọn, đúng 1 đáp án. `MCQ_MULTI`: ≥ 1 đáp án đúng. Lựa chọn không trùng nội dung. | FR-017 |
| BR-U06-21 | `ESSAY`: `stem` bắt buộc; không giới hạn số từ hay số dòng. | FR-017, U09 Câu 3 |
| BR-U06-22 | `DOCUMENT` (thay `DRAWIO`): `stem` bắt buộc; `skeleton` hợp lệ theo mô hình tài liệu của U09; `requiredDiagrams` mỗi loại 1-20. | U09 Câu 5-8 |
| BR-U06-23 | `CODE`: `language` thuộc `JAVA`, `PYTHON`, `C`, `CPP`, `JAVASCRIPT`, `DART`, `CSHARP` (U13 hỗ trợ); ≤ 20 file, mỗi file ≤ 64 KB; `memoryLimitMb` 64-1024; 1-50 test case, ≥ 1 test không ẩn; `timeLimitMs` 100-10 000; tổng điểm test = `defaultPoints`. | FR-017 |
| BR-U06-24 | Câu hỏi không gắn rubric: rubric gắn vào từng câu của bài Text Essay (`config.questionRubrics`) hoặc từng phần của khung (`config.parts`, Diagram Essay và bài nhóm) (BR-U06-34) để hai bài không dùng chung rubric. | Người dùng chốt 2026-10-03 |
| BR-U06-25 | `stem`, lựa chọn, hướng dẫn là markdown ≤ 20 000 ký tự, hiển thị đã làm sạch. | SEC-003 |
| BR-U06-26 | Kích hoạt câu hỏi yêu cầu `definition` hợp lệ theo loại. | FR-017 |
| BR-U06-27 | `lessonRefs` phải là module/học liệu U05 thuộc cùng môn (học liệu của lớp chỉ dùng cho câu hỏi của lớp đó). | Câu 9 |
| BR-U06-28 | Ngân hàng chứa câu hỏi của mọi dạng bài theo bảng dạng bài ở `domain-entities.md` §3. Tìm kiếm lọc được theo dạng bài; khi thêm vào bài, U08 chỉ nhận câu khớp dạng (BR-U08-10, 11). Câu `DOCUMENT` dùng được cho cả Diagram Essay và bài nhóm; phần tự tính theo heading nhỏ nhất của mỗi nhánh, khung không có heading là một phần (BR-U09-25). | UC 39, 40, 41, 42, 43, thay đổi 2026-10-01 |

## 4. Rubric

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-30 | Rubric gồm 1-20 tiêu chí, mỗi tiêu chí 1-20 mục checklist; mỗi mục điểm > 0, tối đa 2 chữ số thập phân. | Câu 3, 5 |
| BR-U06-31 | `scaleMax` = tổng điểm mọi mục (tự tính, không nhập tay). | Câu 5 |
| BR-U06-32 | Chấm: mục đạt/không đạt; điểm tiêu chí = tổng mục đạt; điểm rubric = tổng tiêu chí. `score` từ chối `itemId` không thuộc rubric. | Câu 5 |
| BR-U06-33 | Rubric đã dùng chấm không bị ghi đè (BR-U06-10). | FR-016 |
| BR-U06-34 | Rubric được tạo khi soạn đề: trong Assignment Editor (U08) hoặc Template Editor (U10), U09 gọi `RubricPort.createForAssignment`, U06 lưu rubric cùng phạm vi với bài (lớp hoặc môn); U09 ghi `rubricId` vào `config`: Text Essay mỗi câu một rubric, Diagram Essay và bài nhóm mỗi phần một rubric (BR-U09-23, 26). Quiz và Code Lab không có rubric. Question Bank chỉ xem và sửa rubric đã có; không có thao tác tạo rubric ở Question Bank. | Người dùng chốt 2026-10-03 |
| BR-U06-35 | Rubric nhân bản theo đề: khi bài được nhân bản, tạo version mới, copy từ template hoặc copy sang lớp khác, U09 (`TypeConfigPort.copy`) gọi `RubricPort.cloneForAssignment` để U06 nhân bản rubric của từng câu hoặc từng phần thành rubric mới (`lineage_id` mới, phạm vi của bài mới); không có nhân bản rubric riêng và hai bài không dùng chung một rubric. Bản được nhân bản là phiên bản `ACTIVE` mới nhất của rubric. | Người dùng chốt 2026-10-03 |
| BR-U06-36 | Sửa rubric luôn tạo phiên bản mới (`ACTIVE` không bị ghi đè). Bài sở hữu rubric còn `DRAFT` thì chuyển sang phiên bản mới ngay: sửa trong Assignment Editor/Template Editor thì U09 ghi `rubricId` mới vào câu hoặc phần; sửa trong Question Bank thì U06 báo U08 qua `RubricOwnerPort.repoint(oldId, newId)`. Bài đã duyệt/phát hành giữ phiên bản đã ghim (BR-U06-12); phiên bản mới dùng khi bài được nhân bản, tạo version mới hoặc copy (BR-U06-35). | US-QBK-001 S2 |

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
| BR-U06-51 | Phân tích độ khó/độ phân biệt câu hỏi từ kết quả thực tế nằm ngoài phạm vi dự án, không thiết kế hoặc triển khai. | Quyết định phạm vi 2026-09-25 |
