# Đánh giá đóng góp thành viên theo 5 mức

Trạng thái: người dùng đã duyệt và yêu cầu triển khai bằng “giúp tôi thử sửa như vậy xem, sau tôi tự test”. Người yêu cầu: Quang Hung.

## Mục đích và phạm vi

Yêu cầu: “giúp tôi có thể bổ sung phần này ko, nhớ ghi rõ cách chấm vào”.
Giảng viên cần ghi nhận mức hoàn thành của từng thành viên trong bài nhóm và hiểu
cách dùng mức đó khi chấm điểm. Đây là đề xuất bổ sung từ ghi chú phỏng vấn,
không diễn giải thành quy định điểm chính thức của nhà trường.

Hiện có MemberContributionPanel, API saveMemberGrade, điểm riêng và lý do.
EvaluationRepository lưu Evaluation trong grading_document JSONB, có khóa và version.
Tái sử dụng luồng này; không cần bảng mới, migration hoặc thư viện mới.

## Phương án

1. Chỉ thêm hướng dẫn 5 mức: ít thay đổi nhưng không ghi nhận được mức đã chọn.
2. Thêm mức đánh giá được lưu cùng điểm và lý do, giảng viên quyết định điểm:
   đề xuất vì đúng nội dung trao đổi và giữ được nhận định chuyên môn.
3. Tự nhân điểm nhóm với hệ số mỗi mức: chưa có hệ số được thống nhất trong
   phỏng vấn, không triển khai ở phạm vi này.

## Giao diện và cách chấm

Tại Điểm thành viên, mỗi người có Mức đánh giá (tùy chọn), Điểm, Nhận xét/lý do
và nút Lưu điểm thành viên hiện có. Mức chưa chọn hiển thị Chưa đánh giá.
Giữ bố cục thích ứng màn hẹp; không trải cả 5 mô tả vào từng hàng.
Một mục thu gọn Cách đánh giá ở đầu danh sách chứa hướng dẫn chung:

| Mức | Tên | Căn cứ lựa chọn |
|---|---|---|
| 1 | Vượt kỳ vọng | Hoàn thành tốt phần được giao, chủ động và hỗ trợ thành viên khác. |
| 2 | Đạt yêu cầu | Hoàn thành đầy đủ phần được giao, chất lượng đáp ứng yêu cầu. |
| 3 | Đạt một phần | Có đóng góp nhưng phần việc còn thiếu hoặc chất lượng thấp. |
| 4 | Cần hỗ trợ nhiều | Cần được hướng dẫn, hỗ trợ hoặc nhắc nhở nhiều mới hoàn thành; chưa tự giải thích được phần mình làm. |
| 5 | Không hoàn thành | Không hoàn thành phần việc hoặc không phối hợp; cần có căn cứ ghi nhận. |

Nội dung hướng dẫn hiển thị trong sản phẩm:

1. Chấm sản phẩm chung theo rubric để xác định chất lượng bài nhóm.
2. Đối chiếu phần việc được phân công, sản phẩm/ghi nhận đóng góp và phần trình
   bày hoặc vấn đáp của từng người để chọn mức phù hợp.
3. Nhập điểm thực tế cho từng thành viên từ 0 đến điểm tối đa của bài; nhập
   nhận xét cụ thể về phần đã làm, thiếu sót và căn cứ điều chỉnh.
4. Lưu điểm thành viên, kiểm tra lại rồi công bố theo luồng hiện có.

Mức 1 là cao nhất, mức 5 thấp nhất. Các mức không phải điểm 1–5, không tự đổi
thành điểm hoặc tự cộng/trừ. Ví dụ: bài nhóm 8/10; một thành viên thiếu một phần
chức năng, giảng viên có thể chọn Đạt một phần, nhập 7/10 và ghi rõ phần thiếu.
Đây là ví dụ minh họa, không phải quy tắc “mức 3 luôn được 7”.

## Lưu trữ, quyền và vòng đời

- Thêm contributionLevel dạng enum có thể null vào đánh giá MEMBER và lệnh lưu
  thành viên. Tương thích caller cũ bỏ trường; không tự suy ra mức từ điểm.
- Lưu mức, điểm và lý do trong cùng transaction/version hiện tại. Việc chỉ đổi
  mức cũng phải ghi nhận thay đổi; không chỉ lưu ở state trình duyệt.
- Khi tải lại, giữ đúng mức và lý do đã lưu. Chấm lại bài nhóm không được ghi đè
  đánh giá đóng góp riêng; nhóm nộp phiên bản mới thì xóa mức cùng điểm cũ.
- Giữ quyền giảng viên phụ trách, khóa ARCHIVED, kiểm tra bài nộp hiện hành và
  chống ghi đè phiên bản. Không giao AI quyền chọn mức hoặc lưu điểm thành viên.
- Sinh viên chỉ thấy mức và nhận xét của chính mình khi điểm đã công bố;
  bản nháp không lộ. Sửa bản công bố cập nhật theo cơ chế hiện hành.
- Không thêm cột Excel, hệ số, file effort, trường vấn đáp riêng hoặc MCP tool.

## Luồng triển khai và kiểm chứng sau khi duyệt

- [x] Kiểm tra workspace sạch, caller UI/API/service, model và JSON persistence.
- [x] Soạn yêu cầu, thiết kế nhỏ và cách chấm để duyệt một phạm vi cụ thể.
- [x] Bổ sung enum/field và MemberSave tương thích, validation và audit mức thay đổi.
- [x] Nối saveMemberGrade, kế thừa/reset, DTO sinh viên chỉ khi công bố.
- [x] Nối type/API/frontend, select tùy chọn và hướng dẫn thu gọn; khôi phục lý do đã lưu.
- [x] Test model/service/JSON round-trip: null/caller cũ, 5 mức, giá trị sai,
      đổi mức không đổi điểm, đồng thời, nộp lại, giữ override khi sửa điểm nhóm.
- [x] Test quyền và hiển thị sinh viên: bản nháp, công bố, sửa công bố, chỉ chính mình.
- [x] Test component UI chọn/lưu/khôi phục dữ liệu đã lưu, hướng dẫn, không tự đổi điểm và trạng thái khóa/lỗi.
- [x] Chạy kiểm tra backend/frontend liên quan, lint và TypeScript riêng; người dùng tự test UI.
- [x] Cập nhật tài liệu U09 và kết quả thực chạy. Không tự commit/push/deploy.

Thứ tự: yêu cầu/thiết kế này → kế hoạch mã chi tiết → test và triển khai U09 → kiểm chứng.
User story duy nhất: giảng viên ghi mức đóng góp có căn cứ và điểm riêng cho mỗi
thành viên; sinh viên đọc đánh giá của mình sau công bố. Không cần tách unit hoặc
thay kiến trúc. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt; RBAC, validation,
transaction/version là các kiểm chứng bắt buộc. Kết quả tại
../../construction/build-and-test/2026-11-13-member-contribution-levels.md.

## Xác nhận phạm vi

Đồng ý triển khai phương án 2, lưu 5 mức kèm hướng dẫn trên và không tự quy đổi điểm?

A) Đồng ý phạm vi và cách chấm đã mô tả.

B) Điều chỉnh thiết kế/cách chấm trước khi triển khai.

C) Khác — ghi rõ mong muốn.

[Answer]: A — người dùng duyệt triển khai sau khi thảo luận cách dùng điểm nhóm làm mốc.
