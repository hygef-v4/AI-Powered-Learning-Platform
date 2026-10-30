# Góp ý Quiz, Assignment, chấm bài, bài nhóm — 2026-10-29

Ghi lại góp ý (một phần theo thầy Kiên) để chuẩn bị làm lại phần Quiz (nằm trong Learning Material) và Assignment. Đây là danh sách việc cần làm và câu hỏi còn mở, chưa phải thiết kế đã chốt. Mỗi mục ghi unit dự kiến bị ảnh hưởng.

## 1. Learning Material — U13 (U05 tích hợp trang lớp)

- [x] [source-observed 30/10] Chia tài liệu theo module: MaterialModuleService/Controller, migration 29/10 và ModuleBoard đã có; runtime acceptance chưa chạy lại.
- Quiz sẽ nằm trong Learning Material (xem mục 2).

## 2. Quiz — U10 (soạn), U11 (làm và kết quả), U09 (review activity có scope)

- [ ] [proposed/pending] Phần "Tài liệu tham khảo" trong Quiz có vẻ thừa → xem lại, có thể bỏ.
- [ ] [proposed/pending] **Quiz config (sinh đề):** khi sinh viên bắt đầu làm thì hệ thống sinh đề mới, không cố định bộ câu hỏi. Người tạo cấu hình theo tag, ví dụ: tag 1 lấy 4 câu, tag 2 lấy 5 câu, tag 3 lấy 7 câu.
- [ ] [partial/source-observed] **Chống gian lận:** dùng JS ghi số lần sinh viên rời tab (out tab).
  - Hạn chế đã biết: sinh viên có thể tắt JS / chặn script, nên đây chỉ là tín hiệu, không phải bằng chứng.
  - [ ] [conflicting/decision-needed] Cho cấu hình **bao nhiêu lượt out tab thì trừ bao nhiêu điểm**.
  - ⚠ Mâu thuẫn với quyết định cũ FR-039 (council-features-2026-10-13, mục 2: "không tự trừ điểm, không kết luận gian lận"). Cần chốt lại trước khi làm.
- [ ] [partial/source-observed] Hiển thị câu hỏi **mỗi câu một trang**; màn xem kết quả cũng mỗi câu một trang.
- [ ] [proposed/pending] **Ngân hàng câu hỏi:** thêm cơ chế chống trùng câu hỏi, hoặc cho chủ nhiệm môn duyệt khi có câu hỏi mới vào ngân hàng.
- [ ] [proposed/pending] **Bước duyệt hiện tại đang thừa.** Chỉ giữ duyệt cho một trường hợp: giảng viên đưa câu hỏi vào ngân hàng câu hỏi → chủ nhiệm môn duyệt.
- [ ] [partial/source-observed] **Tạo đề bằng AI:** không dùng popup (hoặc nếu giữ popup thì phải kéo rộng ra).

## 3. Assignment — U07 (soạn), U08 (làm)

- [ ] [proposed/pending] Assignment có thể có **tiêu chí chung** (áp cho cả bài) → cần thêm.
- [ ] [partial/source-observed] Màn tạo đề chuyển thành **trang phẳng (flat page)**, giảng viên gõ và sửa trực tiếp trên trang.
- [ ] [proposed/pending] Nội dung do **AI sinh hiện màu vàng**; khi người dùng sửa phần đó thì trở về màu bình thường, để phân biệt rõ phần AI sinh và phần người viết.
- [ ] [proposed/pending] Khi tạo đề có thể tạo **câu trả lời mẫu**.
- [ ] [partial/source-observed] Giao diện hiện khó dùng → chuyển hết thành flat page.
- [ ] [proposed/pending] **Cấu hình cách dùng AI một lần:** người dùng cài đặt trước dùng nguồn AI nào (API hệ thống / API cá nhân / Agent MCP), không bắt chọn lại mỗi lần dùng chức năng AI.

## 4. Chấm bài — U09

- [ ] [partial/source-observed] Bỏ popup duyệt đề xuất của AI khi chấm bài.
- [ ] [proposed/pending] Tiêu chí chấm không đặt bên cạnh bài làm của sinh viên nữa (đổi bố cục).
- [ ] [proposed/pending] Thêm **tìm kiếm** và **chuyển nhanh** giữa bài làm của các sinh viên trong màn chấm bài.

## 5. Bài nhóm — U12

- [ ] [proposed/pending] Hiện tại một thành viên nhận phần thì người khác, kể cả nhóm trưởng, không sửa được → không nên như vậy.
  - Hướng đề xuất: thành viên nhận phần thoải mái, **nhóm trưởng chốt phần**; và/hoặc thêm **lịch sử thay đổi**.
- [ ] [conflicting/decision-needed] Câu hỏi mở: có nên thêm **đánh giá chéo giữa các thành viên**?
  - Nếu có, điểm bài có thành % điểm giảng viên + % điểm thành viên đánh giá không?
  - Thực tế ASIS: 3 đầu điểm — điểm tài liệu, điểm thành viên nhóm cho nhau, điểm presentation (giống Edunext).
- [ ] [proposed/pending] **Nộp file rồi hệ thống bóc tách:** vẫn cho nộp file; hệ thống tách nội dung và AI điền vào từng ô.
  - Ảnh có thể mapping vào ô tương ứng.
  - Sinh viên nộp 2 file: 1 file Word, 1 file diagram; quy ước mỗi page trong file Draw.io tương ứng một diagram.
  - Cảnh báo người dùng là việc này có thể tốn nhiều token.

## 6. Chung

- [ ] [proposed/pending] Giao diện có cả **tiếng Anh và tiếng Việt**.
- [ ] [proposed/pending] Làm **tài liệu hướng dẫn sử dụng**.

## Câu hỏi cần chốt trước khi làm

1. Trừ điểm theo số lần out tab có thay quyết định FR-039 "không tự trừ điểm" không?
2. Quiz sinh đề theo tag: lấy ngẫu nhiên từ ngân hàng câu hỏi của môn hay của riêng quiz? Không đủ câu theo tag thì xử lý thế nào?
3. Chống trùng câu hỏi: tự phát hiện trùng (so khớp nội dung / AI) hay chỉ dựa vào chủ nhiệm môn duyệt?
4. Đánh giá chéo trong nhóm: có làm không, và tỷ lệ điểm giảng viên / thành viên / presentation là bao nhiêu?
5. Song ngữ: áp cho toàn bộ giao diện ngay hay làm sau phần Quiz/Assignment?

## Đối chiếu source — 2026-10-30

| Góp ý | Tình trạng tại staging@794a4053 | Nguồn/giới hạn |
|---|---|---|
| Module/chương và Quiz trong learning materials | source-observed | U13 MaterialModuleService + ModuleBoard; Quiz attach có quyền riêng. Không phải RAG/Lesson nâng cao |
| Sinh đề mỗi lần theo tag/count | proposed/pending | Chưa có bằng chứng đủ về tag allocation/sampling contract; không tick theo commit UI |
| Out-tab signal | partial/source-observed | Activity hiện có theo policy, retention 1 ngày; không automatic penalty |
| Trừ điểm theo out-tab | conflicting/decision-needed | Trái FR-039 no automatic penalty; chưa thay quyết định cũ |
| Mỗi câu một trang lúc làm/kết quả | partial | QuizTakingPage dùng index, một question; ResultQuestions vẫn map toàn bộ questions |
| Chống trùng/người duyệt ngân hàng | proposed/pending | Version/ACTIVE bank không chứng minh duplicate-detection hoặc teacher proposal approval đầy đủ |
| Bỏ bước REVIEWED Quiz | proposed/pending | Source vẫn có REVIEWED/review/publish; không mô tả đã bỏ |
| AI tạo đề không popup/rộng hơn | partial | AssignmentFormPage vẫn dùng AiDraftDialog; phạm vi resize không chứng minh bỏ modal |
| Tiêu chí chung toàn Assignment | proposed/pending | Rubric theo part có source; cần kiểm general rubric contract trước khi xác nhận |
| Flat-page Assignment | partial/source-observed | AssignmentFormPage là trang editor, vẫn có settings/AI dialogs; không tick yêu cầu chuyển hết flat |
| AI màu vàng rồi về bình thường khi sửa | proposed/pending | Chưa xác minh đủ provenance-to-edit UI behavior |
| Câu trả lời mẫu | proposed/pending | Chưa xác minh đủ model/API/editor flow |
| Cấu hình nguồn AI một lần | proposed/pending | Có source picker/settings không đủ chứng minh preference áp dụng xuyên chức năng |
| Bỏ popup AI grading, đổi vị trí rubric | partial | U09 có UI/AI proposal review; yêu cầu bỏ hẳn modal cần kiểm và thiết kế riêng |
| Tìm/chuyển nhanh bài Student | partial/pending | Submission list và evaluation routes có; chưa xác minh toàn bộ yêu cầu chuyển ngay trong màn |
| Thay claim bằng leader chốt/lịch sử | proposed/pending | SectionRules vẫn kiểm holder; có release/assign không đồng nghĩa bỏ lock |
| Peer evaluation/tỷ lệ 3 đầu điểm | decision-needed | Chưa được phê duyệt; không tự thêm vào điểm official |
| Import Word + Draw.io, AI mapping ảnh/ô và token warning | proposed/pending | Có upload/import không chứng minh pipeline hai file này |
| Song ngữ toàn bộ UI | proposed/pending | Chưa có evidence toàn bộ locale coverage |
| Hướng dẫn sử dụng hoàn chỉnh | proposed/pending | Có MCP/API guides; chưa chứng minh user manual toàn sản phẩm |

Checkbox hoàn thành ở đây chỉ nói source đã có đúng phần được mô tả, không phải E2E acceptance.
