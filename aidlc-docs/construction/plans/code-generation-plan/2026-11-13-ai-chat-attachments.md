# AI chat attachments — implementation plan

**Goal:** Khung chat sửa đề cho phép chọn nguồn có sẵn, tải từ máy, dán ảnh, kéo thả và đọc link công khai, với vai trò nguồn và trạng thái rõ ràng.

**Architecture:** Tái sử dụng `useSourceUploads`, `readTemplateFile`, signed FileRef, `SourceImagePreview` và API sửa đề hiện tại. Tệp mặc định là tham khảo; một DOCX có thể đổi sang mẫu đề. Nguồn link được backend đọc an toàn rồi lưu thành TXT riêng tư để đi qua cùng luồng nguồn có sẵn, không đưa URL chưa đọc cho AI như thể đã trích xuất nội dung.

**Tech stack:** React/TypeScript, UI và document components hiện có; Spring/JDBC, IArtifact, source text extractor, Jsoup hiện đã có trong pom. Không thêm thư viện cho paste/drop/upload; không mặc định thêm dependency cho HTTP.

**Spec:** Các quyết định trong cuộc trao đổi trước yêu cầu “giúp tôi lập plan trển khai”: menu ba mục, tệp ngoài mặc định tham khảo, DOCX đổi được vai trò, Ctrl+V ảnh, kéo thả và đường dẫn có trạng thái đọc thật. Phạm vi là khung `AiReviseChat` sau khi bài đã có assignmentId. Không mở rộng popup tạo bài trước khi có ID trong đợt này.

**Status:** User đã cho phép triển khai giai đoạn 1 với yêu cầu “thôi hiện tại chauw cần đọc link đâu cứ làm đọc file thôi, giúp tôi trienrek hai”. Giai đoạn 2 đọc link hoãn toàn bộ; không triển khai API/nhận diện/đọc URL. Giữ nguyên WIP cũ, không commit/push/deploy.

Ruling: Giữ thẻ khi đóng/ẩn chat theo hành vi hiện có; hủy callback/tải khi đổi assignment hoặc rời trang. Không làm mất tệp chỉ vì thu gọn trợ lý. Reviewer độc lập/subagent không dùng theo yêu cầu người dùng; tự review kết nối và chạy test.

## Giao diện và hành vi đã đề xuất

Menu **+** có đúng ba mục:

1. **Chọn đề mẫu có sẵn** — chỉ tìm/chọn bài trong danh sách hiện có; không còn dòng tải tệp trong danh sách.
2. **Chọn học liệu của môn** — chỉ tìm/chọn học liệu đã có; giữ quyền, giới hạn và lọc nguồn đọc được hiện tại.
3. **Tải từ máy** — chọn nhiều DOCX/PDF/TXT/PNG/JPG qua một file input.

Một vùng đính kèm ngay trên ô nhập: ảnh có thumbnail, tài liệu có tên và trạng thái, thẻ có nhãn **Mẫu đề** hoặc **Tham khảo** và nút gỡ. DOCX có thao tác **Dùng làm mẫu đề / Dùng làm tham khảo**. Không thêm một hàng nút thường trực. Khi kéo tệp vào, chỉ vùng chat hiện “Thả tệp để đính kèm”.

Giai đoạn 1: link vẫn là chữ bình thường trong ô nhập. Giai đoạn 2: nhận diện URL trong văn bản và hiện thẻ chờ **Đọc liên kết**; không tự gọi ra ngoài chỉ vì người dùng dán chữ. Khi bấm đọc: **Đang đọc → Đã đọc / Không truy cập được**, có gỡ/thử lại. Không đổi link thành mẫu đề. Chỉ nguồn có trạng thái READY được đính kèm vào request AI.

## Ràng buộc chung

- Nguồn tham khảo tải lên/nhập từ URL: tối đa **3 nguồn**, **2 MB/nguồn**, **6 MB tổng**. Nguồn link chiếm một trong ba vị trí này. Tệp lỗi vẫn giữ chỗ đến khi gỡ hoặc đổi vai trò; không cho vượt trần qua thao tác liên tiếp.
- Mẫu đề: tối đa **1**, DOCX; giữ giới hạn import DOCX hiện hành **20 MB**. Mẫu đã chọn và DOCX ngoài dùng chung một vị trí. Kiểm lại giới hạn thực tế ở bước triển khai, không hard-code một giá trị trái backend.
- DOCX ngoài mặc định tham khảo. Nếu vượt 2 MB nhưng còn trong giới hạn mẫu đề, giữ thẻ lỗi có tùy chọn chuyển sang mẫu; không tự đổi vai trò hoặc bỏ tệp âm thầm. Nếu đã có mẫu, cần xác nhận thay mẫu; hủy thì giữ mẫu cũ.
- Chuyển vai trò không gửi cùng tệp dưới hai vai trò. Một mẫu đang có chỉ bị thay sau khi mẫu mới đọc thành công. Lỗi chuyển vai trò giữ nguồn cũ còn dùng được và có đường thử lại/gỡ.
- Không thêm thư viện, sửa grading, thay cơ chế tự áp dụng/undo/version hoặc làm AI sửa đề chạy nền trong đợt này. Kiểm tra server vẫn là ranh giới tin cậy.
- Mọi nguồn là dữ liệu không đáng tin; giữ guardrail, quyền quản lý assignment, kiểm FileRef, quota và chọn model có vision khi có ảnh.
- Không gửi yêu cầu AI chỉ có tệp mà không có lời yêu cầu; giữ hành vi hiện tại. Enter gửi, Shift+Enter xuống dòng, IME đang nhập không gửi.
- Thực hiện tuần tự trong workspace phù hợp, không tự gọi subagent. Không commit/push nếu chưa được yêu cầu.

## File map

**Đã có, sửa đúng nơi dùng:**

- `frontend/src/features/assignments/components/AiReviseChat.tsx`: menu, vùng composer, paste/drop, trạng thái gửi, lịch sử nguồn.
- `frontend/src/features/assignments/components/sourceUploads.ts`: hàng đợi có giới hạn, hủy/thử lại, phản hồi muộn. Giữ contract các caller `AiDraftDialog` đang dùng.
- `frontend/src/features/assignments/components/SourceImagePreview.tsx`: dùng lại thumbnail và cleanup object URL.
- `frontend/src/features/assignments/api/index.ts`, `model/types.ts`: chỉ mở rộng khi cần API đọc link; request AI tiếp tục dùng formatText/templateAssignmentId/materialIds/sourceFileRefs.
- `backend/src/main/java/edu/aiplatform/assignments/controller/AssignmentAiDraftController.java`, `AssignmentRequests.java`: endpoint nhập nguồn link ở giai đoạn 2.
- `backend/src/main/java/edu/aiplatform/assignments/service/AssignmentAiDraftService.java`: dùng nguồn TXT đã attach, giữ chuẩn bị prompt và version như cũ.
- `backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java`: tái sử dụng store/attach; không mở truy cập tệp công khai.

**Chỉ tách các phần có trách nhiệm độc lập:**

- Tạo `frontend/src/features/assignments/components/AiChatAttachments.tsx` cho danh sách thẻ/vai trò/trạng thái, tránh nhồi thêm vào component chat dài hiện tại.
- Tạo `frontend/src/features/assignments/hooks/useAiChatAttachments.ts` quản lý mẫu và nguồn trong composer, gọi lại `useSourceUploads` thay vì tạo đường upload thứ hai.
- Giai đoạn 2: `AssignmentLinkSourceService.java` điều phối quyền/nguồn TXT; `PublicLinkReader.java` chỉ tải/trích xuất URL công khai với kiểm tra mạng. Mỗi phần có test riêng, không tạo framework crawler chung.

## Giai đoạn 1 — composer và tệp (có thể giao độc lập)

### Task 1: Hợp nhất menu và vai trò tệp

**Files/tests:** AiReviseChat, AiChatAttachments, useAiChatAttachments, sourceUploads; `frontend/tests/features/assignments/components/AiReviseChat.test.tsx`; tạo `frontend/tests/features/assignments/hooks/useAiChatAttachments.test.tsx`.

**Interfaces:** Hook nhận assignmentId, trạng thái enabled/busy và callback báo lỗi. Trả attachments, pending, addFiles(File[]), remove(id), retry(id), setRole(id, role), cùng payload gồm đúng các field request hiện có. Vai trò `REFERENCE | TEMPLATE`; không lộ fileId nội bộ. Trạng thái đọc/tải của tệp giữ cách đặt tên hiện có.

- [x] Viết test RED: menu chỉ có ba mục; chọn danh sách có sẵn không gọi picker; tải ngoài mặc định tham khảo; DOCX đổi vai trò không bị gửi hai lần; không thay mẫu nếu hủy hoặc import mới thất bại.
- [x] Chạy test để chứng minh các hành vi mới chưa có.
- [x] Tạo hook/thẻ và nối một file input chung. Cổng addFiles kiểm đủ toàn bộ số nguồn đang READY/đang tải/chờ/đang lỗi; giữ thứ tự và báo số tệp vượt giới hạn, không im lặng cắt.
- [x] Giữ thao tác gỡ/hủy/thử lại; vô hiệu hóa chuyển vai trò trong lúc gửi AI; callback cũ không ghi đè vai trò mới. Dùng request generation/id hoặc abort theo tệp tại đúng nơi state thay đổi, không thêm state machine tổng quát.
- [x] Kiểm thử DOCX 2 MB và 20 MB ở hai vai trò, nhiều thao tác add cùng một tick, trùng event, lỗi quyền/loại/dung lượng. Không gộp hai tệp khác nội dung chỉ vì cùng tên.
- [x] Chạy GREEN và các test AiDraftDialog/AiReviseChat để giữ tương thích caller của sourceUploads.

### Task 2: Dán ảnh và kéo thả

**Files/tests:** AiReviseChat/useAiChatAttachments; tạo `frontend/tests/features/assignments/components/aiChatInput.test.tsx` nếu tách nhóm test giúp tránh file test hiện tại quá dài.

- [x] Viết test RED cho ClipboardEvent/DataTransfer: một ảnh, nhiều ảnh, text thường, text + ảnh, files trùng representation trong clipboard, kéo qua phần tử con, thả định dạng lạ/thư mục và vượt giới hạn.
- [x] Nối onPaste trên ô nhập vào addFiles. Đọc file items ảnh; chỉ chặn hành vi paste mặc định khi thật sự có ảnh được xử lý. Text thường/URL giữ paste bình thường; paste hỗn hợp vẫn giữ plain text tại selection của ô nhập, không chèn HTML vào DOM.
- [x] Nối dragenter/dragover/dragleave/drop ở vùng composer/chat đang mở. Chỉ preventDefault đối với Files; kéo chữ/link vẫn là thao tác chữ bình thường. Dùng depth counter để overlay không nhấp nháy khi qua phần tử con, reset khi drop/đóng/rời trang.
- [x] Kéo thả và file picker dùng chung addFiles, validation, progress, retry và thumbnail. Không quét thư mục, không dùng global document handler để lấy clipboard/file từ màn khác.
- [x] Chạy GREEN: gỡ trong lúc tải, đóng chat, đổi assignment, paste trong lúc busy, rời trang và URL.createObjectURL được thu hồi đúng. Nút chọn tệp vẫn dùng được với bàn phím/mobile khi clipboard hoặc drag-drop không có.

### Task 3: Giữ nội dung và trạng thái gửi khi lỗi

**Evidence hiện tại:** `AiReviseChat.send()` xóa input trước prepare()/request; nguồn chỉ bị clear sau khi server nhận, nên lỗi trước khi nhận có thể làm mất chữ đang gõ.

- [x] Test RED: prepare thất bại, POST lỗi, mạng đứt sau submit, tệp chưa READY, bấm Enter hai lần và IME. Assert input/tệp còn phục hồi được; không tạo request trùng ngoài requestRef.
- [x] Chỉ clear composer sau khi server xác nhận nhận task. Nếu chưa được nhận, giữ text/tệp và không để lại bong bóng như thể đã gửi thành công. Lượt không rõ trạng thái phải kiểm theo requestRef hiện có trước khi retry.
- [x] Sau khi đã nhận, snapshot tên/vai trò nguồn trong message của lượt đó. Lỗi AI sau đó có đường khôi phục lời yêu cầu/nguồn còn hiệu lực, không tự gọi model lại. Không đính kèm lại nguồn cũ vào lượt tiếp theo ngoài ý muốn.
- [x] Test và giữ nguyên luồng apply/undo/stale version; không ghi đè đề vừa sửa trong lúc xử lý.

**Lệnh xác minh giai đoạn 1:** `npx vitest run tests/features/assignments`; scoped ESLint; TypeScript; UI teacher và subject manager: chọn nguồn, paste ảnh, kéo DOCX/PDF, gỡ/thử lại, đổi mẫu, gửi lỗi, desktop/mobile và bàn phím. Dùng model stub/fixture trước; không cần trả tiền để chứng minh sự kiện file được gửi đúng.

## Giai đoạn 2 — HOÃN: đọc link thành nguồn tham khảo

Không thuộc phạm vi user cho phép triển khai trong đợt này. Các checkbox dưới đây chỉ là backlog chưa thực hiện.

### Task 4: Chốt và chứng minh đường tải URL an toàn

**Phạm vi ban đầu:** trang **HTTPS công khai**, HTML hoặc text/plain; không cookie, không tài khoản đăng nhập, không chạy JavaScript, không crawl các link con. PDF/DOCX/ảnh từ URL tạm báo chọn tải tệp từ máy; chưa tự tải binary. Drive/Docs riêng tư, trang JS-only, video và paywall phải báo không đọc được, không hứa đã xem nội dung.

- [ ] Đánh giá HTTP client hiện có và tạo test server/transport giả lập để chứng minh DNS/connect gắn với IP đã kiểm tra và TLS vẫn kiểm đúng hostname. Không chỉ DNS check rồi dùng một lần resolve khác để kết nối. Nếu client hiện có không đáp ứng được, ghi lựa chọn client/egress cần thiết để duyệt; không bật endpoint chỉ với kiểm URL sơ bộ.
- [ ] Test RED cho URL có userinfo, IP literal, localhost, IPv4/IPv6 loopback/private/link-local/metadata/special-use, DNS có một địa chỉ nội bộ lẫn công khai, mapped IPv4, rebinding và redirect vào nội bộ. Chỉ HTTPS port 443, không lấy cookie/header/token của phiên đăng nhập để gửi tới trang ngoài.
- [ ] Chọn giới hạn ban đầu: URL tối đa 2.048 ký tự, tối đa 3 redirect (kiểm lại từng hop), connect timeout 3 giây, tổng thời gian 10 giây, body đọc tối đa 2 MB kể cả sau giải nén, đầu ra tối đa 30.000 ký tự. Tối đa 2 lượt đọc đồng thời mỗi instance; đầy thì trả lỗi bận có thể thử lại. Giữ timeout qua tất cả hop, stream và đóng kết nối khi hủy/vi phạm giới hạn.
- [ ] Trích xuất bằng Jsoup có sẵn, bỏ script/style/form/nội dung không phục vụ bài; không thực thi trang. Body là dữ liệu không đáng tin. Chặn hoặc xử lý rõ content encoding để không mở đường giải nén không giới hạn. Test timeout/redirect loop/body quá lớn/không có chữ/HTTP lỗi.

### Task 5: API nguồn link và thẻ trên chat

**Contract đề xuất:** `POST /api/v1/assignments/{id}/ai-sources/link`, body `{url, requestRef}`. Response `{url, title, fileRef, extractedChars, truncated}`; fileRef là dạng signed ref đang dùng. Cần kiểm lại route hiện hữu trước khi đặt tên cuối.

- [ ] Test RED cho quyền teacher/manager, assignment ngoài phạm vi, request trùng, nguồn URL quá dài và lỗi tải/trích xuất; chưa gọi model ở bước đọc link.
- [ ] Controller gọi AssignmentLinkSourceService; kiểm quyền quản lý assignment trước truy cập mạng. Nội dung đã lấy được lưu dưới dạng TXT UTF-8 riêng tư qua IArtifact.store, tên tệp an toàn; trả signed ref và tóm tắt. Metadata nguồn có URL/trích xuất/cắt ngắn, không log query/token hoặc header nhạy cảm.
- [ ] Tái sử dụng hạn mức/tính chống trùng hiện có nếu phù hợp; test retry không nhân đôi tệp ngoài ý muốn. Không thêm bảng/job mới mặc định. Nếu cần lưu idempotency bền vững, nêu rõ thay đổi tối thiểu trước triển khai.
- [ ] Frontend nhận diện URL trong input nhưng không thay chữ hoặc tự gửi AI; hiển thị đề nghị Đọc liên kết. Chỉ khi người dùng chọn đọc mới gọi endpoint. Một link READY dùng sourceFileRefs hiện có; tổng link + tệp tham khảo không vượt 3. Bản text lớn bị cắt phải báo Đã đọc một phần.
- [ ] Lỗi đọc giữ text và URL; có gỡ, thử lại hoặc **Chỉ gửi đường dẫn** với nhãn rõ AI chưa được cung cấp nội dung trang. Không chuyển lỗi đọc thành READY. Nếu giữ thẻ lỗi, chặn gửi như tệp lỗi tới khi người dùng quyết định.
- [ ] Request AI kiểm lại signed ref/quyền như tệp thường. Test model request chứa nội dung đã đọc, không chỉ URL; URL không đọc được không tạo ra lời khẳng định đã xem trang. Kiểm prompt injection qua trang và cơ chế safe error.

## Review focus và kiểm thử cuối

- [ ] Giới hạn bị vượt qua do paste/drop liên tiếp trong lúc upload; addFiles phải đặt chỗ đồng bộ và server vẫn kiểm lại.
- [ ] Một DOCX đổi vai trò giữa hai Promise; chỉ phản hồi của thao tác hiện tại được cập nhật thẻ, mẫu cũ không bị mất khi lỗi.
- [ ] Text tiếng Việt/IME/selection không bị mất do xử lý paste và Enter; test ảnh + text và retry lỗi gửi.
- [ ] Nguồn riêng tư/URL nội bộ không bị lộ cho actor khác hoặc model; kiểm quyền server, DNS/redirect/connection, log an toàn.
- [ ] READY không bị dùng để che nguồn trống, cắt ngắn, timeout hay trang cần đăng nhập; link và file được ghi đúng vào lượt chat đã gửi.
- [ ] Chạy full frontend, backend liên quan U03/U06/U07/U13 và required integration guard khi sửa backend; lint, TypeScript, build từ output sạch. Không xóa cache hoặc restart process đã từng bị review chặn; ghi rõ giới hạn môi trường.
- [ ] UI thật teacher/manager ở desktop/mobile: menu ba mục, dán ảnh, kéo tệp, đổi DOCX thành mẫu, state loading/error/ready, gửi và undo, link công khai/không đọc được. Test bảo vệ mạng dùng fixture có kiểm soát, không truy cập metadata thật.
- [ ] Cập nhật U07 frontend/API docs, build-and-test evidence và aidlc-state. Review diff từng giai đoạn; commit đề xuất riêng `feat(assignments): simplify AI chat attachments` và `feat(assignments): import public links as AI reference sources` khi user yêu cầu commit.

## Trình tự và điều kiện giao

Task 1 → 2 → 3 → kiểm thử/giao giai đoạn 1. Task 4 phải chứng minh bảo vệ mạng trước Task 5; nếu chưa đạt thì giai đoạn 1 vẫn giao độc lập và đọc link chưa bật. Không đánh dấu cả kế hoạch hoàn tất khi mới có paste/drop, hoặc khi link chỉ là chữ chưa được đọc.

Security/Resiliency dùng phạm vi hiện hữu; phần URL mở thêm ranh giới truy cập mạng nên cần review SSRF, timeout, dung lượng và quyền trước bật. PBT vẫn tắt; không có thay đổi điểm số/hạ tầng triển khai được ngầm cho phép. Tại thời điểm lập kế hoạch chưa chạy test tính năng mới hoặc thay source; kết quả triển khai được ghi bên dưới.


## Kết quả giai đoạn 1 — 2026-11-13

Đã giao phần tệp theo phạm vi mới; đọc link hoãn toàn bộ. Frontend 1038 test/150 file, full lint, isolated TypeScript/build qua. Lượt kiểm tra cuối chạy tuần tự, hai worker sau khi chạy song song bị hết virtual memory trên Windows; không đổi cấu hình sản phẩm. UI tải DOCX, đổi vai trò hai chiều và kéo thả TXT qua; paste PNG có thumbnail nhưng backend đang chạy vẫn chính sách cũ, cần reload bản sửa trước. Test paste/drop là browser event simulation, chưa physical clipboard. Một số clipboard regression cases không có RED độc lập; xem báo cáo thay vì suy ra toàn bộ case đều test-first. Không gọi model hoặc commit/push.

Evidence: [2026-11-13-ai-chat-files](../../build-and-test/2026-11-13-ai-chat-files.md). Không đánh dấu task link/SSRF là đã triển khai.
