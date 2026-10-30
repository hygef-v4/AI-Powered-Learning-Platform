# U10 Quiz Authoring — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Ngân hàng câu hỏi (UC 46, 47)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-01 | Chỉ có ngân hàng cấp môn. Chủ nhiệm môn của môn (R2) xem, tạo, sửa, xóa trên Question List và Question Detail mở từ Manager Dashboard. Giảng viên chính của một lớp thuộc môn chỉ đọc câu `ACTIVE` khi chọn vào Quiz. Không có ngân hàng của lớp; Admin không vào. | UC 46, 47 |
| BR-U10-02 | Loại câu: `MCQ_SINGLE` (2–6 lựa chọn, đúng 1 đáp án) và `MCQ_MULTI` (2–6 lựa chọn, ít nhất 1 đáp án đúng); lựa chọn không trùng nội dung. Không có câu tự luận, tài liệu hay code. | UC 46, 47, FR-016 |
| BR-U10-03 | Nội dung câu, lựa chọn, giải thích là markdown ≤ 20.000 ký tự, hiển thị đã làm sạch; `defaultPoints` > 0, tối đa 2 chữ số thập phân; độ khó `EASY`, `MEDIUM`, `HARD`; tối đa 10 tag. | FR-016 |
| BR-U10-04 | Mỗi dòng là một phiên bản, không sửa được khi đã `ACTIVE`. Sửa câu `ACTIVE` tạo phiên bản `DRAFT` mới (`versionNo + 1`, cùng `lineageId`); mỗi lineage tối đa một bản `DRAFT`. Quiz ghim id phiên bản; phiên bản mới không tự áp vào Quiz đang dùng bản cũ. | US-QBK-002 S2 |
| BR-U10-05 | Kích hoạt khi nội dung hợp lệ theo BR-U10-02, 03. `RETIRED`: không chọn được vào Quiz nữa, Quiz đã phát hành vẫn dùng. Xóa: bản `DRAFT` chưa từng kích hoạt thì xóa hẳn; bản đã kích hoạt chỉ ngưng dùng. | UC 47 |
| BR-U10-06 | Question List hiện bản `ACTIVE` mới nhất của mỗi lineage kèm cờ "có bản nháp", và các câu mới chỉ có bản nháp; lọc theo loại, độ khó, tag, trạng thái, từ khóa. Question Detail xem được lịch sử phiên bản. | UC 46 |
| BR-U10-07 | Nhập tệp `.xlsx` hoặc `.csv` UTF-8 theo mẫu (≤ 500 dòng, ≤ 5 MB): cột `noi_dung`, `loai`, `lua_chon_1` tới `lua_chon_6`, `dap_an_dung` (ví dụ `1,3`), `giai_thich`, `diem`, `do_kho`, `tag`. Kiểm từng dòng như tạo tay; dòng hợp lệ tạo câu `DRAFT`, dòng lỗi không tạo; trả kết quả từng dòng. | US-QBK-002 S1 |

## 2. Quiz (UC 48)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-10 | Quiz của lớp: giảng viên chính của lớp (R3/R4) soạn từ tab Evals. Quiz của môn: Chủ nhiệm môn (R2) soạn từ Quiz List trên Manager Dashboard, mọi lớp `OPEN` của môn thấy. Giảng viên lớp chỉ xem Quiz của môn. Admin không soạn, không xem. | UC 48, FR-027, US-AIG-002 |
| BR-U10-11 | Quiz không cần học liệu. Tùy chọn gắn tối đa 10 tài liệu tham khảo (khi U13 có) để Learning Material hiện Quiz. | US-ASM-006 S1 |
| BR-U10-12 | Quiz chỉ có chế độ `PRACTICE` (D01 chốt 2026-10-15): không có lịch, không vào sổ điểm. | FR-007, D01 |
| BR-U10-13 | Quiz1–200 câu ngân hàng ACTIVE ghim version hoặc câu inline. OPEN cho sửa nội dung/inline theo quyền/version và vẫn OPEN; snapshot attempt cũ bất biến. REVIEWED edit về DRAFT, RETIRED không editable. Điểm câu>0, tổng từ item points. | FR-017 |
| BR-U10-14 | Thêm câu từ ngân hàng: chọn tay, hoặc bốc ngẫu nhiên N câu `ACTIVE` khớp bộ lọc (độ khó, tag), không trùng câu đã có, N không vượt chỗ còn trống; người soạn xem lại, bỏ hoặc bốc lại từng câu trước khi lưu. | FR-016 |
| BR-U10-15 | Cài đặt: trộn câu, trộn đáp án (mỗi lượt một thứ tự, U11 lưu seed); giới hạn giờ (trống hoặc 1–300 phút); số lượt (1–10 hoặc không giới hạn); hiện điểm sau khi nộp; hiện đáp án đúng (`NEVER`, `AFTER_SUBMIT`). Theo dõi hoạt động mặc định tắt, kèm thông báo, retention cố định 1 ngày, không phụ thuộc setting cũ. | FR-017, FR-039 |
| BR-U10-16 | Chấm: `MCQ_SINGLE` đúng thì đủ điểm câu; `MCQ_MULTI` phải chọn đủ đáp án đúng và không chọn sai mới có điểm. Chấm theo đáp án của phiên bản đã ghim. Đáp án không gửi cho người học trước khi nộp; sau khi nộp chỉ trả theo cài đặt. | US-ASM-006 S2 |

## 3. Vòng đời Quiz

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-20 | Có DRAFT/REVIEWED/OPEN/RETIRED. Publication vẫn qua REVIEWED. Quiz.edited: REVIEWED về DRAFT; OPEN sửa giữ OPEN theo service guards; RETIRED không editable. Không diễn giải góp ý bỏ review 29/10 là đã triển khai. | FR-007 |
| BR-U10-21 | Duyệt: chính người có quyền tự duyệt sau khi xem trước; kiểm 1–200 câu, câu riêng hợp lệ, câu ngân hàng còn `ACTIVE`, điểm hợp lệ, cài đặt hợp lệ. | US-ASM-001 |
| BR-U10-22 | Phát hành từ `REVIEWED`: Quiz của lớp cần lớp `OPEN`, Quiz của môn cần môn `ACTIVE`; chuyển `OPEN` ngay. Phát hành version mới thì version cũ chuyển `RETIRED` trong cùng transaction. Sau commit phát `quiz.published`. | US-ASM-001 S2 |
| BR-U10-23 | Retire mọi trạng thái chưa RETIRED, reason trống mặc định Lưu trữ, kiểm version/callback. Never-published giữ publishedAt=null; reopen về DRAFT nếu null, ngược lại OPEN với container active. Migration cũ equal timestamps chỉ giữ publication khi có SUCCESS QUIZ_PUBLISHED audit đúng object; không có proof thì null/review. Không suy ra đã phát hành chỉ từ RETIRED hoặc timestamp equality. | FR-028 |
| BR-U10-24 | Xóa: Quiz `DRAFT` hoặc `REVIEWED` chưa từng phát hành, xóa hẳn kèm câu riêng và đề xuất AI. | UC 48 |
| BR-U10-25 | Người học thấy Quiz `OPEN` của lớp `OPEN` mình đang ghi danh, gồm Quiz của lớp và Quiz của môn chứa lớp. | FR-007 |

## 4. Nhân bản, version, copy

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-30 | Nhân bản (cùng lớp hoặc môn), version mới (từ Quiz `OPEN` hoặc `RETIRED`; mỗi Quiz nguồn tối đa một version đang `DRAFT` hoặc `REVIEWED`), copy giữa lớp (chỉ Quiz của lớp; giảng viên phải dạy cả hai lớp). | FR-028 |
| BR-U10-31 | Cả ba cách sao tiêu đề, hướng dẫn, cài đặt, theo dõi hoạt động, tài liệu gắn kèm và câu (câu ngân hàng giữ phiên bản đã ghim, câu riêng sao thành câu riêng mới). Không sao lượt làm, kết quả, đề xuất AI. | FR-028 |

## 5. AI và MCP

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-40 | AI soạn câu cho Quiz `DRAFT` (giảng viên, Chủ nhiệm môn) hoặc cho ngân hàng (Chủ nhiệm môn): nhập số câu (1–50), loại, độ khó, chủ đề; chọn tài liệu tham khảo qua `IReferenceMaterial` (U06 khai báo, U13 cài); chọn nguồn AI trong popup của U06. | FR-006, US-AIG-001, 002 |
| BR-U10-41 | Đề xuất gồm câu, lựa chọn, đáp án, giải thích, kèm provenance. Người soạn chọn, sửa rồi chấp nhận: câu thành câu riêng của Quiz hoặc câu `DRAFT` của ngân hàng. AI không tự kích hoạt câu, không duyệt, không phát hành. Prompt không có dữ liệu người học. Không có AI vẫn soạn tay được. | FR-006 |
| BR-U10-42 | Tool MCP chỉ cho giảng viên, Chủ nhiệm môn có quyền với Quiz: `read_quiz_context` (Quiz kèm đáp án) và `save_quiz_draft` (Quiz `DRAFT`, kèm `expectedVersion`); tool ghi chỉ lưu đề xuất chờ người soạn chấp nhận như BR-U10-41. Người học không có tool đọc Quiz. | FR-035 |

## 6. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U10-50 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. Sửa câu, Quiz kèm `version`; lệch thì `409`. | SEC-002 |
| BR-U10-51 | Audit: `QUESTION_CREATED`, `QUESTION_ACTIVATED`, `QUESTION_RETIRED`, `QUESTION_DELETED`, `QUESTIONS_IMPORTED` (một sự kiện mỗi lần nhập), `QUIZ_CREATED`, `QUIZ_DELETED`, `QUIZ_REVIEWED`, `QUIZ_PUBLISHED`, `QUIZ_RETIRED` (lý do), `QUIZ_CLONED`, `QUIZ_VERSION_CREATED`, `QUIZ_COPIED`, `QUIZ_AI_DRAFT_ACCEPTED`. | FR-014 |
| BR-U10-52 | U01, U04, U05 không trả lời thì từ chối (fail closed); AI, U11, U13 lỗi không chặn soạn tay. | BR-U01-93 |
| BR-U10-53 | Phân tích độ khó, độ phân biệt câu hỏi từ kết quả làm bài nằm ngoài phạm vi. | Phạm vi 2026-09-25 |
