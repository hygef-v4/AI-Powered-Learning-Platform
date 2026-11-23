# Checklist test UI sau restart — thay đổi chấm điểm 2026-11-10

Người dùng báo đã restart. Checklist này chưa được thực thi/đánh dấu Pass; agent chưa xác minh runtime đang nạp đúng artifact hoặc Flyway đã chạy. Source hiện tại vẫn khớp snapshot đã qua 2.095 test backend, 870 test frontend, lint/typecheck/build. [Tóm tắt đầy đủ và bằng chứng](2026-11-10-grading-remediation-results.md).

## Chuẩn bị

- Dùng một lớp thử ở trạng thái OPEN, một tài khoản giảng viên chính và hai sinh viên A/B đang ghi danh. Mở phiên giảng viên và sinh viên ở hai profile hoặc cửa sổ thường/ẩn danh riêng.
- Chuẩn bị ba bài: tính điểm cá nhân, tính điểm nhóm và Practice. Bài nhóm còn trong hạn, A là trưởng nhóm, B là thành viên.
- Bài tính điểm có hai mục rubric, mỗi mục 5 điểm. A/B đều nộp bài cá nhân; bài A dùng để chấm tay, bài B để thử AI khi chưa chấm.
- Tạo quiz cho phép nhiều lượt; chuẩn bị một quiz bật và một quiz tắt “Hiện điểm sau khi nộp”.
- Sau restart tải lại trình duyệt. Nếu backend chạy bằng Docker, cần image được build từ changes hiện tại; restart image cũ không nạp code mới. Hai migration mới được áp dụng khi backend mới khởi động bằng Flyway.

## Luồng chính — làm theo thứ tự

### 1. Rubric và điểm tối đa

Dùng một bản soạn thử riêng cho ca này; giữ bài chấm tay ở hai mục 5 + 5 = 10 để đối chiếu số ở ca 2.

- [ ] Khi soạn bài, thêm tiêu chí “Toàn bài” nếu cấu trúc bài hỗ trợ và thêm một dòng rubric hoàn toàn trống ở cuối.
- [ ] Lưu rồi mở/giao bài. Tổng điểm phải gồm rubric Toàn bài, bỏ dòng trống khi khóa và không lỗi 500.
- [ ] Dòng chỉ điền một nửa (có chữ nhưng thiếu điểm, hoặc ngược lại) vẫn phải được kiểm tra khi phát hành, không âm thầm coi là dòng trống.

### 2. Chấm tay, điểm lẻ, điều chỉnh và lịch sử nháp

Vào **Lớp được phân công → chọn lớp → Chấm điểm → Theo bài tập → chọn bài cá nhân → mở bài A**.

- [ ] Nhập 2,5/5 cho mục thứ nhất và 5/5 cho mục thứ hai: điểm rubric là 7,5/10; mục 2,5 điểm không được đánh dấu đạt tối đa.
- [ ] Nhập nhận xét, cộng 0,5 và lý do cộng điểm: tổng là 8/10. Chờ lưu tự động hoặc bấm “Lưu nháp / thay đổi”. Nút này có thể chuyển sang bài kế; mở lại bài A để kiểm tra dữ liệu.
- [ ] Sửa mục thứ nhất thành 3/5: điểm rubric 8/10, cộng 0,5 thành 8,5/10. Lưu lại và kiểm tra lịch sử có lần đổi 8 → 8,5, vẫn là điểm nháp.
- [ ] Lúc này sinh viên chưa được thấy điểm và nhận xét chưa công bố.

### 3. AI điền trực tiếp nhưng không tự lưu

Mở bài B còn chưa chấm. Ca này cần nguồn AI hoạt động và sẽ dùng một lời gọi AI thực tế do người test bấm.

- [ ] Bấm “Chấm bằng AI”, chờ hoàn tất. Đề xuất điền trực tiếp vào form, không tự mở popup duyệt.
- [ ] Không chạm vào form trong khoảng 5 giây: bài chưa chấm không tự chuyển thành bản chấm đã lưu. Có thể tải lại và xác nhận bỏ nháp để kiểm tra điểm cũ chưa bị ghi; sau đó mở lại đề xuất để tiếp tục.
- [ ] Chỉnh minh chứng/giải thích/nhận xét. Nếu mục yêu cầu review, kiểm tra ô “Đã kiểm tra và xác nhận giải thích này phù hợp với điểm số”.
- [ ] Thử chốt khi còn mục bắt buộc chưa xác nhận: phải báo đúng mục cần review. Xác nhận đầy đủ rồi lưu/chốt thành công.
- [ ] Nếu AI lỗi/hết quota, vẫn chấm tay được, bài nộp không mất và không có điểm giả được tự ghi.

### 4. Chấm nhóm và sửa điểm thành viên — trước khi chốt

Trưởng nhóm nộp bài; giảng viên mở **Chấm điểm → bài nhóm → Xem bản nộp chung**.

- [ ] Chấm rubric 7,5, cộng 0,5: nhóm 8/10; thành viên chưa override nhận 8/10.
- [ ] Mở “Điểm thành viên”, đổi riêng B thành 6/10. Lý do override là tùy chọn.
- [ ] Sửa điểm nhóm rồi chờ lưu; ngay sau đó sửa B thêm lần nữa. Không được lỗi 409 do version thành viên cũ.
- [ ] Điểm mặc định của A theo nhóm; điểm override của B không bị kéo về điểm nhóm khi lưu lại nhóm.
- [ ] Nếu còn trong hạn, trưởng nhóm sửa và nộp lại khi mới chấm nháp: các bản chấm chưa chốt trở về chờ chấm; bản nộp mới phải mở đúng nội dung.

### 5. Chốt, công bố và xem kết quả sinh viên

- [ ] Chấm lại phần nháp nếu bước 4 vừa reset. Lưu xong bấm “Chốt điểm”, sau đó “Công bố điểm”.
- [ ] Sau chốt/công bố, điểm, rubric, nhận xét và điều chỉnh phải bị khóa; vẫn đọc được lịch sử.
- [ ] Sinh viên A mở bài cá nhân → lịch sử nộp: thấy điểm đã công bố, nhận xét chung, tóm tắt và minh chứng/giải thích/gợi ý từng mục khi bản chấm có các trường đó.
- [ ] Thành viên mở bài nhóm → tab “Bản nộp”: thấy kết quả được công bố của mình; B thấy điểm override, không phải điểm mặc định của A.
- [ ] Tải lại ở cả hai phiên: kết quả không mất hoặc đổi ngoài thao tác đã thực hiện.

### 6. Giữ điểm chính thức khi nhóm nộp lại

Dùng bài nhóm còn trong hạn; ghi lại điểm sau khi chốt/công bố.

- [ ] Trưởng nhóm sửa nội dung và nộp lại. Điểm nhóm/thành viên đã chốt hoặc công bố phải giữ nguyên, không về 0 hay chờ chấm.
- [ ] Điểm này vẫn thuộc receipt đã được chấm; không coi là chấm lại tự động cho nội dung mới. Không được mở khóa sửa điểm official.
- [ ] Đây là quy tắc cho bài nhóm. Bài cá nhân nộp lại vẫn làm lượt cũ mất hiệu lực theo quy tắc hiện hữu.

### 7. Thao tác hàng loạt

Vào **Chấm điểm → Theo bài tập → chọn bài → Thao tác hàng loạt**.

- [ ] Checkbox hiện cho các bản nộp có đánh giá; bài nhóm chọn bản nộp chung, không chấm lặp từng thành viên. Không chọn bài thì nút thao tác bị khóa.
- [ ] Chọn vài bài rồi thử “Chấm AI đã chọn”, “Chốt điểm đã chọn”, “Công bố đã chọn” trên dữ liệu tương ứng. Mỗi thao tác có hộp xác nhận/kết quả riêng; tối đa 100 lựa chọn mỗi đợt UI.
- [ ] Trộn một bài hợp lệ và một bài còn thiếu xác nhận review rồi công bố: bài hợp lệ có thể thành công, bài lỗi phải giữ thông báo cụ thể và không bị công bố nhầm.
- [ ] Tải lại bảng điểm để kiểm tra kết quả cuối, không chỉ dựa vào thông báo thành công.

## Luồng bổ sung

### 8. Deadline bài nhóm và ân hạn cá nhân

Dùng bài thử riêng với hạn gần, tránh phải chờ bài nhóm chính hết hạn.

- [ ] Nhóm đã mở tài liệu nhưng chưa gõ/lưu nội dung: qua hạn và đợi lượt quét tự động, tài liệu đóng mà không tạo bản nộp rỗng.
- [ ] Nhóm đã được trưởng nhóm nộp tay: qua hạn, bản nộp tay không bị thay bằng một receipt tự nộp mới.
- [ ] Bài cá nhân không có cửa sổ nộp muộn: nộp trong vòng dưới 30 giây sau hạn, nếu thao tác đến server trong ân hạn thì không bị đánh dấu trễ. Đây là ca nhạy thời gian; ghi giờ thao tác nếu thất bại.

### 9. Quiz và ngân hàng câu hỏi

- [ ] Quiz tắt “Hiện điểm sau khi nộp”: danh sách/lịch sử/chi tiết không lộ điểm của lượt đó.
- [ ] Quiz bật tùy chọn này: điểm hiện đúng. Nếu UI cho phép đổi cài đặt sau một lượt đã nộp, bật lại cờ hiện điểm không được làm lộ lượt cũ đã chụp snapshot ẩn điểm. Nếu cấu hình đã khóa thì để ca đổi policy cho test tự động, không sửa DB để ép test UI.
- [ ] Với quiz thuộc môn được dùng ở hai lớp mà sinh viên cùng học, số lượt còn lại phải khớp; đổi lớp không làm có thêm lượt.
- [ ] Khi quiz ngưng giao hoặc lớp đóng, không được bắt đầu/lưu/nộp tiếp lượt đang làm. Quyền xem kết quả lịch sử vẫn theo chính sách hiện hữu.
- [ ] Trong ngân hàng, lineage có bản ACTIVE và bản RETIRED mới hơn: chọn câu hỏi tự động/tìm bản đang dùng không được mất bản ACTIVE.

### 10. Góp ý Practice và phổ điểm

- [ ] Với lượt Practice đã nộp, yêu cầu góp ý. Nếu task chờ lâu hơn khoảng 3 phút, tự làm mới phải dừng và có nút “Kiểm tra lại góp ý”; bấm chỉ đọc lại, không tạo thêm yêu cầu AI. Task hoàn tất sớm thì ca hết thời gian chờ là chưa thực hiện, không đánh Pass.
- [ ] Giảng viên bật “Cho sinh viên xem phổ điểm ẩn danh” trong cài đặt bài tính điểm của lớp: tải lại vẫn giữ lựa chọn, không lỗi quyền DB.
- [ ] Với ít hơn 20 sinh viên đang ghi danh có điểm công bố dùng được, không có phổ điểm là đúng. Khi đủ dữ liệu, chỉ hiện các khoảng có ít nhất 5 người, không lộ điểm/danh tính người khác. Tắt công tắc thì phổ điểm không còn hiện sau tải lại.

## Phần phù hợp hơn với kiểm thử tự động

AI JSON cố tình sai schema/tổng/evidence, số thập phân quá chính xác, requestRef 200 ký tự, cuộc đua đồng thời lúc khóa quiz/chuyển nhóm, dữ liệu adjustment legacy, giới hạn số event và quyền SQL không có cách tái hiện ổn định chỉ bằng thao tác UI. Các phần này đã có bằng chứng tự động; không đánh dấu đã nghiệm thu UI nếu chưa thao tác thực tế.

## Ghi nhận kết quả

Mỗi ca đánh dấu Pass / Fail / Chưa thực hiện. Khi Fail, ghi: vai trò, lớp/bài/lượt hoặc nhóm, các bước, kỳ vọng, kết quả thực tế, giờ xảy ra và ảnh màn hình/thông báo lỗi. Không kèm token, mật khẩu hay API key.
