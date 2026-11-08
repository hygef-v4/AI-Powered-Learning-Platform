# Chốt yêu cầu U04/U13/U14 theo từng bước

Phương án được chọn: thống nhất yêu cầu bằng quyết định được xác nhận, sau đó sửa test yếu và bổ sung scenario thiếu. Chỉ mở một câu hỏi tại một thời điểm; không tự chọn thay người dùng. Người dùng đã duyệt và triển khai riêng phần bỏ học kỳ khỏi API/dữ liệu và liên quan trên nhánh fix/remove-term. Các quyết định khác tiếp tục theo từng bước; chưa tiếp tục soạn report.

## Câu hỏi 1 — Lưu trữ môn U04

Nguồn đang mâu thuẫn:

- [BR-U04-04](../../construction/u04-subject-administration/functional-design/business-rules.md) và F6 trong [business-logic-model.md](../../construction/u04-subject-administration/functional-design/business-logic-model.md) quy định lưu trữ môn sẽ lưu trữ luôn mọi lớp DRAFT/OPEN trong cùng transaction.
- [domain-entities.md](../../construction/u04-subject-administration/functional-design/domain-entities.md), phần trạng thái và contract ISubjectClasses, vẫn ghi chỉ cho lưu trữ môn khi các lớp đã được lưu trữ hết.

Khi Admin lưu trữ một môn vẫn còn lớp DRAFT hoặc OPEN, hệ thống phải xử lý thế nào?

A) Tự lưu trữ các lớp DRAFT/OPEN cùng với môn trong một transaction. Nếu bất kỳ bước nào lỗi thì không lưu trữ phần nào. Đây là phương án đề xuất: phù hợp BR-U04-04 và F6 hiện hành, tránh phải thao tác từng lớp.

B) Từ chối lưu trữ môn; người có quyền phải lưu trữ các lớp trước, sau đó Admin mới lưu trữ môn. Phương án này hạn chế việc vô tình ngưng nhiều lớp nhưng cần thêm thao tác và sửa lại BR-U04-04/F6 cùng các test cascade hiện tại.

C) Khác — mô tả hành vi mong muốn sau dòng trả lời.

[Answer]: a

Các quy tắc liên quan hiện có được giữ làm nền cho cả A/B: giữ enrollment và lịch sử học tập; lưu trữ môn gỡ Chủ nhiệm môn; mở lại môn không tự mở lớp hoặc gán lại Chủ nhiệm. Nếu người dùng muốn đổi các quy tắc này, ghi rõ trong câu trả lời, không suy diễn từ việc chọn A/B.

Trạng thái câu 1: đã xác nhận A từ câu trả lời `a` của người dùng.

Quyết định D-U04-01: lưu trữ môn sẽ lưu trữ luôn các lớp DRAFT/OPEN trong cùng transaction; lỗi thì rollback toàn bộ. Giữ enrollment/lịch sử, gỡ Chủ nhiệm môn; mở lại môn không tự mở lại lớp hoặc gán lại Chủ nhiệm. Khi đồng bộ yêu cầu, giữ BR-U04-04/F6 và sửa phần trạng thái/contract cũ trong domain-entities.md. Chưa thực hiện thay đổi implementation hoặc chứng nhận integration đã được kiểm đủ.

## Câu hỏi 2 — Số lớp trên danh sách môn U04

Nguồn đang khác nhau:

- BR-U04-50 trong [business-rules.md](../../construction/u04-subject-administration/functional-design/business-rules.md) và SubjectListPage trong [frontend-components.md](../../construction/u04-subject-administration/functional-design/frontend-components.md) yêu cầu mỗi dòng danh sách môn có số lớp.
- Assertion trong `frontend/tests/features/subjects/pages/SubjectListPage.test.tsx` hiện yêu cầu không có cột Số lớp.

Danh sách môn của Admin cần hiển thị số lớp ngay trên mỗi dòng hay chỉ xem thông tin đó tại chi tiết môn?

A) Hiện cột Số lớp trên danh sách môn. Đây là phương án đề xuất: phù hợp BR-U04-50 và giúp Admin xem nhanh quy mô từng môn. Nếu chưa lấy được số lớp thì hiển thị không khả dụng, không hiển thị 0 giả.

B) Không hiện số lớp trên danh sách môn; Admin xem thông tin lớp tại chi tiết môn. Đồng bộ BR-U04-50 và thiết kế danh sách theo quyết định này.

C) Khác — mô tả hành vi mong muốn sau dòng trả lời.

[Answer]: B

Trạng thái câu 2: đã xác nhận B. Người dùng yêu cầu ghi nhận sau khi xem mô tả cách frontend đang hiển thị số lớp.

Quyết định D-U04-02: không có cột Số lớp trên danh sách môn. Số lớp hiển thị cạnh tab Lớp học tại chi tiết môn, lấy tổng số lớp mọi trạng thái từ API; số 0 hiển thị, giá trị không khả dụng thì ẩn số như frontend hiện tại. Không yêu cầu thêm số đếm theo bộ lọc hoặc vào thẻ Trạng thái từ quyết định này. Khi đồng bộ yêu cầu, sửa BR-U04-50 và mô tả danh sách/chi tiết liên quan để thống nhất; không thay đổi backend contract classCount.

## Câu hỏi 3 — Nội dung xác nhận lưu trữ môn U04

Nguồn đang khác nhau:

- SubjectStatusActions trong [frontend-components.md](../../construction/u04-subject-administration/functional-design/frontend-components.md) yêu cầu popup giải thích các lớp chưa lưu trữ sẽ được lưu trữ theo và Chủ nhiệm môn bị gỡ.
- Frontend hiện chỉ hỏi xác nhận lưu trữ môn theo tên; `SubjectStatusActions.test.tsx` còn assert popup không có nội dung về lớp hoặc Chủ nhiệm môn.
- Quyết định D-U04-01 ở câu 1 đã chốt việc lưu trữ đồng thời các lớp DRAFT/OPEN và gỡ Chủ nhiệm môn.

Popup xác nhận lưu trữ môn cần hiển thị rõ các thay đổi đối với lớp và Chủ nhiệm môn hay giữ câu xác nhận ngắn như hiện tại?

A) Hiển thị thêm lời giải thích ngắn về việc các lớp Nháp/Đang mở được lưu trữ theo và Chủ nhiệm môn bị gỡ; enrollment/lịch sử học tập vẫn được giữ. Đây là phương án đề xuất: giúp Admin hiểu đúng tác động của hành động đã chốt ở câu 1. Không cần thêm một bước xác nhận hoặc cơ chế nhập tên môn.

B) Giữ popup ngắn chỉ xác nhận lưu trữ môn theo tên như hiện tại; cập nhật thiết kế để không bắt buộc lời giải thích về lớp/Chủ nhiệm môn trong popup.

C) Khác — mô tả nội dung mong muốn sau dòng trả lời.

[Answer]: a

Trạng thái câu 3: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U04-03: popup xác nhận lưu trữ môn phải giải thích ngắn việc các lớp Nháp/Đang mở được lưu trữ theo và Chủ nhiệm môn bị gỡ; enrollment/lịch sử học tập vẫn được giữ. Giữ một popup xác nhận, không thêm bước hoặc yêu cầu nhập tên môn. Khi thực thi, sửa UI và test đang assert không có nội dung về lớp/Chủ nhiệm để kiểm hành vi đã chốt; chưa sửa code/test trong bước hỏi đáp này.

## Câu hỏi 4 — Bộ lọc học kỳ trong danh sách lớp của môn U04

Nguồn đang khác nhau:

- SubjectClassesTable trong [frontend-components.md](../../construction/u04-subject-administration/functional-design/frontend-components.md) yêu cầu lọc trạng thái và học kỳ.
- Frontend hiện có các tab Tất cả/Nháp/Đang mở/Lưu trữ và ô tìm kiếm; không có bộ lọc học kỳ. `SubjectClassesTable.test.tsx` còn assert không có control lọc lớp theo học kỳ.

Tab Lớp học trong chi tiết môn của Admin cần thêm bộ lọc học kỳ hay giữ các bộ lọc hiện tại?

A) Thêm bộ lọc Học kỳ, mặc định Tất cả học kỳ; kết hợp được với bộ lọc trạng thái và ô tìm kiếm. Đây là phương án đề xuất: giúp phân biệt lớp của cùng một môn qua nhiều học kỳ, phù hợp yêu cầu thiết kế hiện có.

B) Không thêm bộ lọc học kỳ; giữ các bộ lọc hiện có và cập nhật thiết kế để bỏ yêu cầu lọc học kỳ.

C) Khác — mô tả hành vi mong muốn sau dòng trả lời.

[Answer]: B

Trạng thái câu 4: đã xác nhận B bằng lời nhắn người dùng; mở rộng phạm vi theo lời làm rõ: "ý tôi là có thể sửa code để bỏ phần term khỏi API/dữ liệu và những phần liên quan".

Quyết định D-U04-04: giữ giao diện không có học kỳ; bỏ hẳn trường học kỳ của lớp (`term`) khỏi request/response, domain, contract U04/U05, frontend types/payloads, repository và schema `course_classes`. Thêm migration mới, không sửa migration cũ. Không chỉ tiếp tục gửi chuỗi rỗng hoặc giữ trường legacy. Các câu trả lời trước chưa ghi nhận việc xóa API/database; quyết định này xác nhận phần mở rộng đó.

Thiết kế ngắn: U05 vẫn sở hữu lớp/enrollment, U04 chỉ đọc qua contract; giữ code/name/description/status/teacher/version, scope quyền và khóa/transaction. Frontend/backend được cập nhật cùng đợt; request còn gửi `term` sẽ bị từ chối như unknown field theo strict-input hiện có. Migration mới xóa cột `term` và giá trị học kỳ cũ, giữ dữ liệu lớp/enrollment/bài học còn lại. Không chạy migration trên DB đang dùng, không triển khai, không viết lại audit/backup hoặc migration lịch sử trong công việc local này.

## Câu hỏi 5 — Duyệt kế hoạch bỏ term khỏi API và dữ liệu lớp

[Kế hoạch cụ thể](../../construction/plans/code-generation-plan/2026-11-07-remove-class-term.md) đã liệt kê phạm vi, migration, test RED/GREEN, kiểm PostgreSQL và giới hạn triển khai. Chỉ thực hiện lát cắt học kỳ; các quyết định khác tiếp tục riêng sau đó.

Có tiếp tục thực hiện kế hoạch bỏ `term` đã mô tả, bao gồm chuẩn bị migration xóa cột dữ liệu học kỳ, hay cần điều chỉnh kế hoạch trước?

A) Thực hiện kế hoạch local: code, test, migration và đồng bộ aidlc-docs; không chạy migration live, commit/push hoặc deploy.

B) Điều chỉnh kế hoạch — ghi rõ phần cần thay đổi sau dòng trả lời.

C) Khác — mô tả mong muốn sau dòng trả lời.

[Answer]: A — thực hiện trên nhánh mới fix/remove-term tạo từ staging.

Trạng thái câu 5: đã duyệt và hoàn tất riêng phần học kỳ trên nhánh fix/remove-term; kiểm chứng ghi trong kế hoạch. Các thay đổi khác chưa triển khai.

## Câu hỏi 6 — Đóng popup Thêm môn học U04

Nguồn đang khác nhau:

- SubjectFormDialog trong [frontend-components.md](../../construction/u04-subject-administration/functional-design/frontend-components.md) yêu cầu nút Hủy cạnh nút Thêm môn học.
- Frontend hiện có nút X (nhãn Đóng) và hỗ trợ Escape; không có nút Hủy riêng. `SubjectFormDialog.test.tsx` kiểm X/Escape đóng popup mà không gọi mutation, đồng thời assert không có nút Hủy.

Popup Thêm môn học cần giữ cách đóng bằng X/Escape hay bổ sung nút Hủy riêng?

A) Giữ nút X và phím Escape, không thêm nút Hủy riêng. Đây là phương án đề xuất: thao tác đóng hiện đã có và có test; đồng bộ thiết kế để mô tả đúng lựa chọn này.

B) Thêm nút Hủy cạnh nút Thêm môn học; nút này dùng cùng hành vi đóng với X/Escape và không tạo môn. Giữ các cách đóng hiện có.

C) Khác — mô tả hành vi mong muốn sau dòng trả lời.

[Answer]: a

Trạng thái câu 6: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U04-06: giữ nút X và phím Escape để đóng popup Thêm môn học, không thêm nút Hủy riêng; đóng popup không tạo môn. Khi đồng bộ tài liệu, sửa mô tả SubjectFormDialog đang yêu cầu nút Hủy để phản ánh lựa chọn này. UI/test hiện tại đã có hành vi tương ứng; chưa sửa popup trong bước hỏi đáp này.

## Câu hỏi 7 — Định dạng tệp học liệu U13

Nguồn đang khác nhau:

- BR-U13-10 trong [business-rules.md](../../construction/u13-reference-materials/functional-design/business-rules.md) liệt kê PDF, DOCX, PPTX cho học liệu FILE.
- PurposePolicy và PurposePolicyTest hiện cho phép PDF, DOCX, PPTX, XLSX; frontend DocumentViewer có trình xem XLSX.

Học liệu FILE của môn/lớp cần cho phép những định dạng nào?

A) Giữ PDF, DOCX, PPTX và XLSX như hệ thống hiện tại. Đây là phương án đề xuất: giữ hỗ trợ bảng tính Excel đang có và cập nhật yêu cầu để thống nhất.

B) Chỉ PDF, DOCX và PPTX theo danh sách trong BR-U13-10 hiện tại; không cho tải lên học liệu XLSX mới. Việc xem/tải học liệu XLSX đã có vẫn được giữ để không mất quyền truy cập dữ liệu cũ.

C) Khác — ghi rõ các định dạng mong muốn sau dòng trả lời.

[Answer]: a

Câu 7 chỉ chốt danh sách định dạng tệp học liệu; chưa quyết định Admin được đổi danh sách qua Settings hay danh sách cố định. Chính sách dung lượng, quyền truy cập, kiểm nội dung tệp và link YouTube giữ nguyên ở bước này.

Trạng thái câu 7: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U13-07: học liệu FILE cho phép PDF, DOCX, PPTX và XLSX. Giữ hỗ trợ XLSX hiện có; khi đồng bộ yêu cầu, bổ sung XLSX vào danh sách của BR-U13-10 và các mô tả liên quan. Quyết định này không chứng nhận luồng upload/trích chữ XLSX đã có đủ test hoặc đã kiểm E2E; phần coverage còn thiếu vẫn phải bổ sung riêng.

## Câu hỏi 8 — Cấu hình định dạng học liệu qua Settings

Nguồn đang khác nhau:

- [FR-013](requirements.md), BR-U13-10 trong [BR U13](../../construction/u13-reference-materials/functional-design/business-rules.md) và BR-U03-03/87 trong [BR U03](../../construction/u03-job-and-settings/functional-design/business-rules.md) mô tả loại tệp cho phép lấy từ Settings.
- PurposePolicy hiện dùng danh sách định dạng cố định cho học liệu; Settings chỉ điều chỉnh dung lượng trong trần hệ thống. Câu 7 đã chốt danh sách hỗ trợ là PDF, DOCX, PPTX, XLSX.

Danh sách định dạng học liệu cần cố định hay cho phép Admin bật/tắt từng định dạng qua Settings?

A) Cố định cả bốn định dạng PDF, DOCX, PPTX, XLSX; Admin chỉ điều chỉnh dung lượng tối đa trong giới hạn hiện có. Đây là phương án đề xuất: giữ cách hệ thống đang hoạt động và tránh thêm cấu hình không cần thiết; đồng bộ lại yêu cầu đang nói loại học liệu tùy chỉnh qua Settings.

B) Cho Admin chọn tập con khác rỗng trong bốn định dạng trên qua Settings. Chỉ chấp nhận các định dạng hệ thống đã hỗ trợ; thay đổi áp dụng cho upload mới, không xóa hoặc chặn xem/tải học liệu đã lưu.

C) Khác — mô tả cách quản lý định dạng mong muốn sau dòng trả lời.

[Answer]: a

Câu 8 chỉ chốt cấu hình định dạng của purpose MATERIAL. Không đổi loại ảnh/tệp nguồn của purpose khác, trần dung lượng, quyền upload hoặc cơ chế kiểm nội dung tệp trong câu này.

Trạng thái câu 8: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U13-08: purpose MATERIAL có danh sách định dạng cố định PDF, DOCX, PPTX, XLSX; không cung cấp Settings để bật/tắt từng định dạng học liệu. Admin tiếp tục chỉnh dung lượng tối đa trong trần hiện có. Khi đồng bộ tài liệu, sửa FR-013 và các BR U13/U03 liên quan riêng phần học liệu; không tự thay chính sách ảnh/tệp nguồn của purpose khác. Không thêm cấu hình hoặc thay runtime trong bước hỏi đáp này.

## Câu hỏi 9 — Email mặc định cho Teacher và Subject Manager U14

Nguồn đang khác nhau:

- BR-U14-21 trong [business-rules.md](../../construction/u14-announcements-and-notifications/functional-design/business-rules.md) và F8 trong [business-logic-model.md](../../construction/u14-announcements-and-notifications/functional-design/business-logic-model.md) yêu cầu các loại email staff mặc định tắt cho tới khi người dùng bật.
- NotificationPreferenceService và test hiện bật mặc định các loại email phù hợp role; người dùng được tắt. Comment code nhắc yêu cầu 05/11 nhưng quyết định thay thế chưa được đồng bộ trong BR hiện hành.

Khi Teacher hoặc Subject Manager chưa lưu tùy chọn email, các loại email phù hợp role nên mặc định bật hay tắt?

A) Mặc định bật như code hiện tại; người dùng có thể tắt từng loại. Đây là phương án đề xuất: tránh bỏ lỡ sự kiện cần xử lý và giữ khả năng tự chọn không nhận email; cập nhật yêu cầu cho thống nhất.

B) Mặc định tắt theo BR-U14-21 hiện hành; người dùng phải chủ động bật các loại muốn nhận. Sửa code/test mặc định tương ứng.

C) Khác — mô tả chính sách theo role/loại email mong muốn sau dòng trả lời.

[Answer]: a

Cả A/B chỉ áp dụng khi chưa có tùy chọn được lưu, không ghi đè lựa chọn bật/tắt đã lưu của người dùng. Student giữ mặc định hiện có; Admin BACKUP_FAILED luôn bật và không được tắt; loại email/người nhận theo role, thông báo trong app và trần gửi hiện có không đổi.

Trạng thái câu 9: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U14-09: các loại email phù hợp role mặc định bật cho Teacher/Subject Manager khi chưa có tùy chọn lưu; người dùng có thể tắt từng loại. Giữ nguyên tùy chọn đã lưu, mặc định Student và email BACKUP_FAILED bắt buộc của Admin. Khi đồng bộ tài liệu, thay chính sách staff mặc định tắt trong BR-U14-21/F8 và các mô tả hiện hành liên quan; không mở rộng loại/người nhận hoặc trần gửi. Code hiện tại có chính sách mặc định tương ứng; chưa sửa runtime trong bước hỏi đáp này.

## Câu hỏi 10 — Quyền xem học liệu đã lưu trữ U13

Nguồn đang khác nhau:

- BR-U13-12/13 trong [business-rules.md](../../construction/u13-reference-materials/functional-design/business-rules.md) giữ tham chiếu/lịch sử và ẩn học liệu đã lưu trữ với người học.
- ArchiveMaterialDialog trong [frontend-components.md](../../construction/u13-reference-materials/functional-design/frontend-components.md) lại mô tả học liệu sẽ ẩn cả với sinh viên và giảng viên.
- MaterialAccessPolicy hiện từ chối Student đọc học liệu ARCHIVED, nhưng vẫn cho Teacher/Subject Manager đang có quyền hợp lệ xem/tải; test MCP kiểm staff đọc được tham chiếu đã lưu trữ nhưng không chọn làm nguồn AI mới.

Sau khi học liệu được lưu trữ, phạm vi giảng viên nào còn được xem/tải lại nội dung?

A) Giữ quyền xem/tải cho Teacher/Subject Manager còn quyền trên môn/lớp tương ứng, kể cả quyền đọc học liệu kế thừa từ môn. Đây là phương án đề xuất: tra cứu được tham chiếu/lịch sử trong phạm vi được phân công, phù hợp access policy hiện tại. Không yêu cầu học liệu xuất hiện trong danh sách Đang dùng.

B) Chỉ người đang quản lý học liệu được tra cứu trong phạm vi của mình: Chủ nhiệm môn với học liệu môn, giảng viên được giao lớp với học liệu riêng của lớp. Giảng viên chỉ được đọc kế thừa học liệu môn không còn xem/tải học liệu đó sau lưu trữ; sửa quyền và test tương ứng.

C) Khác — mô tả vai trò/phạm vi còn được truy cập sau dòng trả lời.

[Answer]: A — ghi nhận theo yêu cầu của người dùng.

Cả A/B giữ nguyên: Student không mở học liệu đã lưu trữ; Admin không có quyền học vụ; mọi lần truy cập kiểm phân công hiện hành. Học liệu không bị xóa, giữ các phiên bản/tham chiếu cũ, không chọn làm nguồn AI mới hoặc gắn vào Quiz mới. Câu này không tự cho phép sửa nội dung hoặc khôi phục học liệu đã lưu trữ.

Trạng thái câu 10: đã xác nhận A bằng lời nhắn "vậy nó giống với phương án a đúng không? ghi nhận câu 10 thay tôi".

Quyết định D-U13-10: giữ quyền xem/tải học liệu đã lưu trữ cho Teacher/Subject Manager còn được phân công hợp lệ, bao gồm đọc học liệu môn kế thừa qua lớp; Student và người ngoài quyền bị từ chối, Admin không có quyền học vụ. Danh sách module Đang dùng chỉ hiện ACTIVE, không phải bằng chứng staff mất quyền truy cập trực tiếp. Giữ lịch sử/phiên bản; không cho sửa hoặc chọn nguồn AI mới. MCP đọc tham chiếu hiện có vẫn kiểm quyền và có thể đọc bản đã lưu trữ. Khi đồng bộ yêu cầu, sửa lời mô tả ẩn cả với giảng viên và phân biệt UI filtering với quyền đọc.

## Câu hỏi 11 — Dùng XLSX làm nguồn AI

Phần cần chốt sau quyết định giữ XLSX:

- Câu 7 đã chốt cho phép học liệu XLSX, nhưng BR-U13-30 trong [business-rules.md](../../construction/u13-reference-materials/functional-design/business-rules.md) mới liệt kê PDF/DOCX/PPTX cho trích chữ.
- Backend hiện đưa mọi học liệu FILE qua job trích chữ/parser OOXML chung, không có nhánh loại XLSX ra; chưa có regression riêng chứng minh workbook XLSX được trích và dùng làm nguồn đúng yêu cầu.

Học liệu XLSX cần được dùng làm nguồn nội dung cho AI hay chỉ hỗ trợ xem/tải?

A) Cho trích nội dung bảng tính XLSX để chọn làm nguồn AI, tương tự các tệp học liệu khác, với quyền truy cập và giới hạn xử lý hiện có. Đây là phương án đề xuất: dữ liệu bảng tính cũng hữu ích khi soạn Assignment/Quiz; bổ sung yêu cầu và test workbook thật thay vì suy ra từ whitelist upload.

B) Chỉ hỗ trợ xem/tải XLSX; không cho chọn XLSX làm nguồn AI trong phạm vi này. Cần cập nhật chính sách chọn nguồn và test tương ứng, vẫn giữ upload/view/download đã chốt.

C) Khác — mô tả phạm vi hỗ trợ XLSX mong muốn sau dòng trả lời.

[Answer]: a

Trích dữ liệu không đồng nghĩa tự gọi AI khi upload, chạy macro, thực thi công thức hoặc OCR ảnh trong workbook. Các quyền/phạm vi, quyết định lưu trữ và dung lượng không tự thay đổi từ câu hỏi này.

Trạng thái câu 11: đã xác nhận A từ câu trả lời `a` trong file và lời nhắn người dùng.

Quyết định D-U13-11: XLSX được trích nội dung bảng tính để làm nguồn AI trong phạm vi quyền và các giới hạn xử lý học liệu hiện có. Không tự gọi AI khi upload, không chạy macro/thực thi công thức hoặc OCR ảnh. Bổ sung XLSX vào yêu cầu trích chữ và kiểm bằng workbook thật; không suy ra coverage chỉ từ whitelist hoặc parser chung.

Trạng thái câu 11: đã chốt A; yêu cầu hiện hành đã được đồng bộ theo quyết định 1–11. Phần bỏ term đã thực hiện. Report unit test vẫn tạm dừng.

## Câu hỏi 12 — Duyệt kế hoạch sửa test theo yêu cầu đã chốt

Các câu hỏi nghiệp vụ chính đã xong. [Kế hoạch thực hiện](../../construction/plans/code-generation-plan/2026-11-08-u04-u13-u14-requirements-tests.md) nêu từng nhóm scenario/BR, các file dự kiến sửa, RED/GREEN, test dữ liệu thật và các gate chưa thể kết luận bằng unit test. Đây là bước duyệt Code Generation Part 1, không phải câu hỏi nghiệp vụ mới.

Có thực hiện kế hoạch trên nhánh fix/remove-term hiện tại hay cần điều chỉnh kế hoạch trước?

A) Thực hiện kế hoạch: bổ sung/sửa test, thêm lời giải thích popup archive đã chốt và sửa tối thiểu lỗi code nếu regression chứng minh vi phạm yêu cầu; chạy kiểm thử và cập nhật bằng chứng. Giữ nguyên phần bỏ term, không commit/push/deploy/live migration hoặc soạn report.

B) Điều chỉnh kế hoạch — ghi rõ phần cần thay đổi sau dòng trả lời.

C) Khác — mô tả mong muốn sau dòng trả lời.

[Answer]: a

Trạng thái: câu 1–11 đã chốt; phần bỏ term đã thực hiện; baseline yêu cầu đã đồng bộ. Câu 12 đã được chọn A; đang thực hiện kế hoạch trên fix/remove-term, không mở thêm câu hỏi nghiệp vụ.
