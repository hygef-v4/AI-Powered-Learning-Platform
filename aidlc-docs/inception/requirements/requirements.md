# Yêu cầu sản phẩm: AI-Powered Learning Platform

## 1. Tóm tắt phân tích ý định

- **Yêu cầu ban đầu**: "giúp tôi triển khai quy trình ai dlc"
- **Loại yêu cầu**: Dự án mới
- **Độ rõ ban đầu**: Mơ hồ; đã được làm rõ qua ba vòng câu hỏi
- **Phạm vi ước tính**: Toàn hệ thống, gồm web frontend, backend, dữ liệu, tích hợp AI, lưu trữ tệp, thanh toán, email và tài liệu quy trình AI-DLC
- **Độ phức tạp**: Phức tạp
- **Mức độ chi tiết**: Comprehensive
- **Mục tiêu**: Xây dựng MVP nền tảng học tập ứng dụng AI đồng thời duy trì bộ tài liệu và checkpoint AI-DLC có thể tái sử dụng

## 2. Bối cảnh và phạm vi

### 2.1 Người dùng và mô hình vận hành

Nền tảng phục vụ một trường học hoặc trung tâm đào tạo. Bốn vai trò chính là người học, giảng viên, Chủ nhiệm môn (Subject Manager) và quản trị viên. Một môn học có thể có nhiều lớp; Chủ nhiệm môn chịu trách nhiệm học thuật và tài nguyên dùng chung của các môn được phân công, còn giảng viên phụ trách hoạt động và nội dung riêng của các lớp được giao. Phiên bản đầu vận hành trong phạm vi một tổ chức; multi-tenancy không thuộc MVP.

### 2.2 Phạm vi MVP

MVP bao gồm tài khoản, phân quyền, quản lý môn học/khóa học/lớp học, kho học liệu và RAG cấp môn, nội dung riêng của lớp, tải tài liệu, theo dõi tiến độ, đánh giá, tạo câu hỏi/bài tập bằng AI, phản hồi hoặc chấm điểm tự động, thanh toán và email/thông báo. Sản phẩm chỉ cung cấp web responsive; không đặt yêu cầu riêng cho ứng dụng mobile hoặc API dành cho mobile trong đợt đầu.

### 2.3 Ngoài phạm vi MVP

- Ứng dụng mobile native
- Multi-tenancy và cô lập dữ liệu giữa nhiều tổ chức
- Đồng bộ LMS hoặc SSO của tổ chức
- Active/active đa region
- Property-based testing
- Chứng nhận tuân thủ một khung pháp lý cụ thể
- Operations automation hoàn chỉnh ngoài các yêu cầu sẵn sàng và tài liệu được xác định trong quy trình này

## 3. Các bên liên quan

| Bên liên quan | Nhu cầu chính |
|---|---|
| Người học | Truy cập lớp học, học nội dung, làm bài, nhận phản hồi và xem tiến độ |
| Giảng viên | Quản lý nội dung/lớp học, dùng AI tạo bài, duyệt kết quả và theo dõi người học |
| Chủ nhiệm môn | Quản lý kho học liệu/RAG cấp môn và biên soạn, phát hành đề chung cho mọi lớp thuộc môn được phân công |
| Quản trị viên | Quản lý người dùng, vai trò, cấu hình nền tảng, thanh toán và audit |
| Đơn vị đào tạo | Vận hành thử nghiệm ổn định, bảo vệ dữ liệu người học và đo hiệu quả MVP |
| Nhóm phát triển | Quy trình AI-DLC rõ ràng, test tự động, container local và hướng dẫn triển khai |

## 4. Yêu cầu chức năng

### FR-001 - Xác thực và tài khoản

Hệ thống phải cho phép đăng ký hoặc cấp tài khoản, đăng nhập, đăng xuất, khôi phục mật khẩu và quản lý hồ sơ ở mức tối thiểu cần thiết.

**Tiêu chí chấp nhận:**

- Người dùng đã xác thực có thể đăng nhập và đăng xuất an toàn.
- Phiên hết hạn theo cấu hình và bị vô hiệu hóa khi đăng xuất.
- Luồng khôi phục mật khẩu không tiết lộ tài khoản có tồn tại hay không.

### FR-002 - Phân quyền

Hệ thống phải cung cấp bốn vai trò: người học, giảng viên, Chủ nhiệm môn và quản trị viên; mọi thao tác đặc quyền phải được kiểm tra phía server.

**Tiêu chí chấp nhận:**

- Người học không thể gọi chức năng của giảng viên, Chủ nhiệm môn hoặc quản trị viên.
- Giảng viên chỉ truy cập lớp học, khóa học và bài nộp được phân công.
- Chủ nhiệm môn chỉ có quyền cấp môn đối với các môn được phân công, kể cả các lớp do giảng viên khác phụ trách trong những môn đó.
- Quản trị viên có thể quản lý người dùng, phân vai trò và gán phạm vi môn học cho Chủ nhiệm môn.

### FR-003 - Quản lý môn học, khóa học và lớp học

Quản trị viên phải có thể quản lý môn học, gán Chủ nhiệm môn và tổ chức các lớp thuộc môn. Giảng viên hoặc quản trị viên phải có thể tạo, sửa, xuất bản và lưu trữ khóa học/lớp theo phạm vi quyền; quản lý phân công giảng viên và ghi danh người học.

**Tiêu chí chấp nhận:**

- Nội dung chưa xuất bản không hiển thị cho người học.
- Người học chỉ truy cập lớp mình được ghi danh.
- Mỗi lớp thuộc một môn và kế thừa học liệu hoặc đề chung đã được phát hành ở cấp môn.
- Các thay đổi quan trọng về khóa học được ghi audit.

### FR-004 - Nhập và quản lý nội dung học

Hệ thống phải hỗ trợ soạn nội dung trực tiếp và tải lên PDF, DOCX hoặc slide. Chủ nhiệm môn quản lý kho học liệu và nguồn trích xuất RAG dùng chung ở cấp môn; giảng viên vẫn quản lý nội dung riêng của lớp được phân công. Việc xử lý tệp phải có trạng thái, giới hạn loại/kích thước và thông báo lỗi an toàn.

**Tiêu chí chấp nhận:**

- Tệp không hợp lệ bị từ chối trước khi xử lý.
- Tệp hợp lệ được lưu riêng tư và gắn đúng phạm vi môn hoặc lớp.
- Người tải lên và người quản lý được ủy quyền xem được trạng thái chờ, đang xử lý, thành công hoặc thất bại.
- Giảng viên không thể sửa kho học liệu/RAG cấp môn nếu không có quyền Chủ nhiệm môn tương ứng.

### FR-005 - Trải nghiệm học theo lớp

Người học phải có thể xem nội dung theo cấu trúc khóa học/lớp, đánh dấu hoàn thành và tiếp tục từ vị trí gần nhất.

**Tiêu chí chấp nhận:**

- Tiến độ được lưu theo người học và đơn vị nội dung.
- Giảng viên xem được tiến độ của người học trong lớp được phân công.

### FR-006 - Tạo câu hỏi và bài tập bằng AI

Giảng viên phải có thể yêu cầu AI tạo câu hỏi hoặc bài tập từ nội dung được phép của lớp; Chủ nhiệm môn phải có thể thực hiện tương tự từ kho học liệu/RAG của môn, với loại câu hỏi, số lượng và mức độ khó có giới hạn hợp lệ.

**Tiêu chí chấp nhận:**

- AI chỉ sử dụng nội dung thuộc phạm vi lớp hoặc môn mà người yêu cầu được phép truy cập.
- Nội dung do AI tạo ở trạng thái bản nháp và cần chính vai trò có quyền xuất bản duyệt trước khi giao cho người học.
- Hệ thống lưu nguồn nội dung, cấu hình tạo và trạng thái duyệt để truy vết.

### FR-007 - Đánh giá và bài nộp

Giảng viên phải có thể xuất bản bài đánh giá riêng cho lớp được phân công. Chủ nhiệm môn phải có thể biên soạn và phát hành trực tiếp bài đánh giá chung cho mọi lớp thuộc môn được phân công mà không cần giảng viên từng lớp duyệt lại. Người học được phép phải có thể làm và nộp bài trong thời gian hiệu lực.

**Tiêu chí chấp nhận:**

- Hệ thống lưu bài nộp, thời điểm nộp và trạng thái chấm.
- Một người học không thể đọc hoặc sửa bài nộp của người khác.
- Quy tắc số lần làm và hạn nộp được thực thi phía server.
- Bài đánh giá chung chỉ được phân phối tới các lớp thuộc đúng môn và lưu actor/phạm vi phát hành trong audit.

### FR-008 - Chấm điểm và phản hồi tự động

Hệ thống phải tự chấm câu hỏi có đáp án xác định và có thể dùng AI đề xuất điểm/phản hồi cho câu trả lời mở. Giảng viên giữ quyền duyệt và ghi đè kết quả AI.

**Tiêu chí chấp nhận:**

- Điểm tự động có kèm trạng thái và phương thức chấm.
- Kết quả AI chưa duyệt không được coi là quyết định cuối đối với câu trả lời mở.
- Mọi lần ghi đè điểm lưu người thực hiện, thời gian và lý do.

### FR-009 - Sổ điểm và tiến độ

Người học phải xem được điểm và tiến độ của chính mình; giảng viên xem được tổng hợp theo lớp; quản trị viên xem được dữ liệu theo quyền quản trị.

### FR-010 - Thanh toán

Hệ thống phải tích hợp một nhà cung cấp thanh toán để tạo giao dịch, nhận kết quả qua webhook và ghi nhận quyền truy cập tương ứng mà không lưu dữ liệu thẻ thanh toán thô.

**Tiêu chí chấp nhận:**

- Webhook được xác minh chữ ký và xử lý idempotent.
- Trạng thái thanh toán được đối soát với nhà cung cấp.
- Lỗi thanh toán không tự cấp quyền truy cập.

### FR-011 - Email và thông báo

Hệ thống phải gửi được các thông báo thiết yếu như kích hoạt/khôi phục tài khoản, ghi danh, giao bài và kết quả đánh giá qua nhà cung cấp email.

### FR-012 - Tích hợp AI/LLM

Backend phải tích hợp nhà cung cấp AI/LLM qua một ranh giới dịch vụ rõ ràng, có timeout, giới hạn chi phí/sử dụng, xử lý lỗi và khả năng thay đổi nhà cung cấp mà không làm rò rỉ chi tiết vào nghiệp vụ cốt lõi.

### FR-013 - Lưu trữ tệp

Hệ thống phải lưu tệp học tập qua một dịch vụ lưu trữ riêng tư, cấp quyền truy cập ngắn hạn và không công khai trực tiếp object chứa dữ liệu học tập.

### FR-014 - Audit nghiệp vụ và bảo mật

Hệ thống phải ghi sự kiện đăng nhập thất bại, thay đổi vai trò hoặc phạm vi môn, thay đổi nội dung đã xuất bản, thay đổi điểm, phát hành đề chung, sự kiện thanh toán và truy cập đặc quyền.

## 5. Luồng người dùng chính

### USCN-001 - Chuẩn bị và giao bài cấp lớp bằng AI

Giảng viên tạo khóa học hoặc lớp, nhập nội dung/tải tài liệu, yêu cầu AI tạo câu hỏi, chỉnh sửa và duyệt bản nháp, sau đó xuất bản bài đánh giá cho lớp.

### USCN-001A - Quản lý học liệu và giao đề chung cấp môn

Chủ nhiệm môn quản lý kho học liệu/RAG của môn được phân công, dùng AI biên soạn và duyệt đề chung, sau đó phát hành trực tiếp cho mọi lớp thuộc môn; hệ thống bảo đảm phạm vi môn và ghi audit mà không yêu cầu giảng viên từng lớp duyệt lại.

### USCN-002 - Học và nhận phản hồi

Người học đăng nhập, truy cập lớp được ghi danh, học nội dung, làm bài, nộp bài và xem điểm/phản hồi khi được công bố.

### USCN-003 - Duyệt chấm điểm AI

AI đề xuất điểm cho câu trả lời mở; giảng viên kiểm tra bằng rubric, chấp nhận hoặc ghi đè và công bố kết quả. Hệ thống lưu audit cho quyết định cuối.

### USCN-004 - Thanh toán và cấp quyền

Người dùng bắt đầu thanh toán; nhà cung cấp xử lý giao dịch; backend xác minh webhook idempotent; hệ thống chỉ cấp quyền sau trạng thái thanh toán hợp lệ.

### USCN-005 - Xử lý lỗi phụ thuộc

Khi AI, email, lưu trữ hoặc thanh toán tạm thời không khả dụng, hệ thống không làm mất dữ liệu nghiệp vụ, hiển thị trạng thái an toàn và cho phép retry có kiểm soát.

## 6. Yêu cầu phi chức năng

### NFR-001 - Công nghệ

- Frontend: Next.js với TypeScript.
- Backend: Java với Spring Boot.
- Giao tiếp frontend-backend qua API web có versioning hoặc quy ước tương thích rõ ràng.
- Database và nhà cung cấp bên ngoài sẽ được chọn trong NFR/Application Design, ưu tiên giải pháp ít vận hành cho MVP.

### NFR-002 - Khả năng sử dụng và truy cập

- Giao diện phải responsive trên desktop, tablet và trình duyệt mobile hiện đại.
- Các luồng cốt lõi phải dùng được bằng bàn phím và có nhãn hỗ trợ công nghệ trợ năng.
- Thông báo lỗi phải nêu hành động khắc phục mà không lộ chi tiết nội bộ.

### NFR-003 - Quy mô và hiệu năng

- Quy mô mục tiêu ban đầu: dưới 1.000 tài khoản và dưới 100 người dùng đồng thời.
- API đồng bộ không phụ thuộc AI nên đạt p95 không quá 500 ms trong điều kiện tải mục tiêu, không tính độ trễ mạng công cộng.
- Tác vụ xử lý tệp và tạo nội dung AI phải chạy bất đồng bộ khi có thể, cung cấp trạng thái thay vì giữ request không giới hạn.
- Mọi external call phải có timeout; retry phải có giới hạn và backoff.

### NFR-004 - Khả năng kiểm thử

- MVP phải có kiểm thử ở các cấp độ: unit test, integration test, system test và end-to-end test cho các hành trình cốt lõi.
- System test phải kiểm thử toàn diện sự phối hợp giữa Frontend (Next.js), Backend (Spring Boot), Database và các mock/sandbox của dịch vụ bên ngoài (AI, Payment Gateway) trên môi trường container trước khi triển khai.
- Contract/webhook test phải bao phủ thanh toán và các adapter bên ngoài quan trọng.
- Không áp dụng Property-Based Testing theo quyết định của người dùng.

### NFR-005 - Môi trường chạy

- Môi trường phát triển và demo phải chạy local bằng container với hướng dẫn tái tạo được.
- Secret chỉ được truyền qua biến môi trường hoặc secret store, không commit vào repository.
- Image và dependency phải khóa phiên bản; không dùng tag `latest` cho artifact triển khai.

## 7. Yêu cầu bảo mật và quyền riêng tư

### SEC-001 - Bảo vệ dữ liệu

Mọi database, object storage, cache và backup phải mã hóa at rest. Mọi kết nối tới data store và mọi luồng dữ liệu qua network phải dùng TLS 1.2 trở lên, kể cả giữa các container khi có giao tiếp qua network. Dữ liệu nhạy cảm không được ghi log.

### SEC-002 - Xác thực và phiên

Mật khẩu phải được băm bằng thuật toán adaptive, được kiểm tra với danh sách mật khẩu đã lộ, và có tối thiểu 8 ký tự. Tài khoản quản trị phải hỗ trợ MFA. Cookie phiên phải có `Secure`, `HttpOnly`, `SameSite`, thời hạn server-side và bị vô hiệu khi đăng xuất. Login phải có bảo vệ brute-force.

### SEC-003 - Authorization và API

Mọi endpoint mặc định yêu cầu xác thực trừ khi được đánh dấu public. API phải kiểm tra quyền ở mức object và chức năng, xác thực toàn bộ input, giới hạn payload, dùng truy vấn tham số hóa và giới hạn CORS theo allowlist. Endpoint public phải có rate limiting. Thiết kế phải bao phủ các misuse case gồm leo thang đặc quyền, truy xuất nội dung ngoài khóa học qua prompt, thao túng điểm và webhook replay.

### SEC-004 - Bảo mật web

Endpoint phục vụ HTML phải thiết lập tối thiểu `Content-Security-Policy: default-src 'self'` mà không dùng `unsafe-inline`/`unsafe-eval` nếu không có lý do được duyệt; `Strict-Transport-Security: max-age=31536000; includeSubDomains`; `X-Content-Type-Options: nosniff`; `X-Frame-Options: DENY` trừ khi có yêu cầu framing được duyệt; và `Referrer-Policy: strict-origin-when-cross-origin`.

### SEC-005 - Logging, alerting và audit

Ứng dụng phải dùng structured logging với timestamp, correlation ID, level và message. Mọi load balancer, API gateway hoặc CDN xử lý traffic bên ngoài phải bật access logging vào kho tập trung. Log production phải lưu tối thiểu 90 ngày trong kho append-only hoặc tamper-evident. Cảnh báo phải bao phủ đăng nhập thất bại lặp lại, vi phạm authorization và thay đổi đặc quyền; dashboard phải hiển thị các chỉ số vận hành và bảo mật chính. Ứng dụng không được sửa hoặc xóa audit log của chính nó.

### SEC-006 - Chuỗi cung ứng

Dependency phải có lock file hoặc phiên bản chính xác, lấy từ registry tin cậy, được xác minh integrity khi tải, được quét lỗ hổng và loại bỏ khi không dùng. Build production phải tạo SBOM và dùng tool/base image đã khóa phiên bản. Quyền sửa pipeline phải được kiểm soát và thay đổi phải audit được. External script từ CDN, nếu có, phải dùng Subresource Integrity.

### SEC-007 - Fail-safe và error handling

External call, file I/O và database operation phải xử lý lỗi rõ ràng, giải phóng tài nguyên và fail closed. Backend phải có global error handler; phản hồi production không được lộ stack trace, path, phiên bản framework hoặc chi tiết database. Không được có default credential; sample app, tính năng không dùng, directory listing và documentation endpoint không dành cho production phải bị loại bỏ hoặc vô hiệu hóa. Object storage phải chặn public access.

### SEC-008 - Payment và integrity

Hệ thống không lưu thông tin thẻ thô. Webhook phải xác minh chữ ký, chống replay khi nhà cung cấp hỗ trợ và xử lý idempotent. Thay đổi dữ liệu quan trọng phải truy vết được actor và timestamp.

### SEC-009 - IAM và network least privilege

Mọi IAM policy phải giới hạn action và resource cụ thể; wildcard chỉ được dùng khi API không hỗ trợ resource-level permission và phải ghi lý do. Quyền đọc và ghi phải tách khi phù hợp. Network phải deny-by-default, chỉ public load balancer được mở Internet trên cổng 80/443; application, database và storage phải giới hạn nguồn/đích cần thiết, ưu tiên private subnet hoặc private endpoint.

## 8. Yêu cầu resiliency và vận hành

### REL-001 - Mức quan trọng và tác động

MVP có mức quan trọng **Trung bình**: dùng thử với người thật; downtime gây bất tiện nhưng có thể xử lý thủ công. Application Design phải phân loại từng deployable component và ghi rõ dependency/tác động khi không khả dụng.

### REL-002 - Recovery objectives

- Chiến lược DR: Backup & Restore.
- RTO mục tiêu: tính bằng giờ.
- RPO mục tiêu: tính bằng giờ, được tinh chỉnh theo lịch backup trong Infrastructure Design.
- Production topology: single-region, multi-zone.
- Local/demo: được phép chạy một instance và không phải mô hình HA.

### REL-003 - Change management

Do chưa có quy trình tổ chức, AI-DLC phải đề xuất quy trình nhẹ gồm change record, phê duyệt trước production và ghi chú rollback. Git history và tài liệu AI-DLC là nguồn truy vết thay đổi ban đầu.

### REL-004 - CI/CD, deployment và rollback

- AI-DLC phải đề xuất pipeline CI/CD phù hợp với Next.js, Spring Boot và container.
- Chiến lược MVP: direct/in-place.
- Rollback: triển khai lại artifact/container image đã khóa phiên bản trước đó.
- Database migration phải ưu tiên backward compatibility; migration phá vỡ phải có kế hoạch khôi phục riêng trước khi được duyệt.

### REL-005 - Observability và health

Mỗi component production phải phát metrics về latency, error rate, throughput và saturation; log có cấu trúc phải tập trung. Kiến trúc nhiều service phải có distributed tracing và dashboard sức khỏe vận hành. Mỗi service phải có shallow health check; component quan trọng phải có deep health check cho dependency. Health check phải tích hợp với load balancer/service discovery và endpoint public phải có synthetic monitoring hoặc lý do N/A được duyệt.

### REL-006 - Capacity và fault isolation

Production compute và data store phải phân bố ít nhất hai availability zone, có load balancing và giữ khả năng phục vụ khi một zone lỗi mà không cần control-plane operation để khôi phục. Infrastructure Design phải xác định min/max capacity, scaling trigger, quota liên quan và cảnh báo ở ngưỡng 80% khi phù hợp. Resiliency-specific alarms phải bao phủ mất redundancy, backup failure và capacity risk; resiliency assessment tool phải được cấu hình hoặc ghi nhận là cải tiến có kế hoạch.

### REL-007 - Dependency isolation

External call phải có timeout. Dependency quan trọng phải có circuit breaker khi phù hợp; connection/thread pool phải được tách theo bulkhead khi một dependency có thể làm cạn tài nguyên dùng chung; mọi pool/resource limit phải hữu hạn; tính năng không thiết yếu phải có degraded mode thay vì gây lỗi dây chuyền.

### REL-008 - Backup và recovery

Persistent data phải được backup tự động, mã hóa, có retention policy và quy trình test restore. Runbook phải mô tả failover/failback, phục hồi từ backup, kiểm tra sau phục hồi và truyền thông sự cố.

### REL-009 - Incident response

AI-DLC phải đề xuất quy trình incident response và Correction of Errors nhẹ, gồm phân loại sự cố, người chịu trách nhiệm, kênh thông báo, post-mortem và theo dõi corrective action.

### REL-010 - Resiliency testing

NFR Design phải trình người dùng lựa chọn cách kiểm thử failover/recovery theo RESILIENCY-14; kịch bản, lịch thực hiện và cơ chế lưu kết quả phải được ghi nhận trước khi hoàn tất thiết kế resiliency.

## 9. Ràng buộc và giả định đã xác nhận

- Chỉ một tổ chức trong MVP.
- Bốn vai trò trong MVP gồm người học, giảng viên, Chủ nhiệm môn và quản trị viên.
- Chủ nhiệm môn là vai trò RBAC riêng, được gán phạm vi một hoặc nhiều môn; giảng viên vẫn quản lý nội dung riêng của lớp được phân công.
- Chỉ web responsive.
- Nội dung được nhập trực tiếp hoặc tải PDF/DOCX/slide.
- Tích hợp bắt buộc gồm AI/LLM, lưu trữ tệp, thanh toán và email/thông báo.
- Triển khai đợt đầu ưu tiên local container.
- MVP phải có test tự động (bao gồm unit test, integration test, system test, e2e test), tài liệu chạy và khả năng triển khai thử nghiệm.
- Quản trị quy trình AI-DLC: Repository phải duy trì state tracking, audit trail, requirements, user stories, thiết kế, kế hoạch code, kết quả kiểm thử và các checkpoint phê duyệt trong `aidlc-docs/`; mã nguồn ứng dụng không được đặt trong thư mục này.
- Chưa chọn nhà cung cấp AI, payment, email, storage, database hoặc cloud; lựa chọn cụ thể thuộc các stage thiết kế sau và phải tuân thủ yêu cầu trong tài liệu này.

## 10. Tiêu chí thành công của MVP

- Một giảng viên có thể tạo lớp, đưa nội dung vào hệ thống, dùng AI tạo và duyệt bài đánh giá.
- Một Chủ nhiệm môn có thể quản lý kho học liệu/RAG và phát hành đề chung tới đúng mọi lớp của môn được phân công mà không cần giảng viên lớp duyệt lại.
- Một người học được ghi danh có thể học, nộp bài và nhận điểm/phản hồi đúng quyền.
- Luồng thanh toán thử nghiệm cấp quyền chính xác và chống xử lý webhook trùng lặp.
- Các vai trò không thể truy cập dữ liệu hoặc chức năng ngoài quyền.
- Dữ liệu và hành động nhạy cảm có audit trail phù hợp.
- Dự án chạy được local bằng container và bộ test cốt lõi chạy tự động.
- Tài liệu AI-DLC phản ánh đầy đủ quyết định, checkpoint và trạng thái triển khai.

## 11. Truy vết nguồn yêu cầu

| Nguồn | Yêu cầu liên quan |
|---|---|
| Phiếu xác minh Q1-Q14 | FR-001 đến FR-014, NFR-001 đến NFR-005 |
| Security Baseline Q15 | SEC-001 đến SEC-008 và Security Compliance |
| Resiliency Baseline Q16 | REL-001 đến REL-010 và Resiliency Compliance |
| Property-Based Testing Q17 | NFR-004, extension bị tắt |
| Làm rõ vòng 1 Q1-Q10 | Web, tích hợp, criticality, DR, change, CI/CD, rollback, topology, incident response |
| Làm rõ vòng 2 Q1-Q2 | Direct/in-place; production single-region multi-zone |
| Làm rõ User Stories Q1-Q3 | Vai trò Chủ nhiệm môn, quyền phát hành đề chung và ranh giới học liệu cấp môn/lớp |

## 12. Security Compliance tại Requirements Analysis

| Rule | Trạng thái | Cách đáp ứng ở requirements |
|---|---|---|
| SECURITY-01 | Compliant | SEC-001 yêu cầu mã hóa at rest và TLS 1.2+ |
| SECURITY-02 | Compliant | SEC-005 yêu cầu access/centralized logging cho thành phần network-facing ở production |
| SECURITY-03 | Compliant | SEC-005 yêu cầu structured logging và cấm log dữ liệu nhạy cảm |
| SECURITY-04 | Compliant | SEC-004 xác định đầy đủ nhóm HTTP security headers |
| SECURITY-05 | Compliant | SEC-003 yêu cầu validation, size limit, sanitization và parameterized query |
| SECURITY-06 | Compliant | SEC-009 yêu cầu IAM action/resource cụ thể và tách quyền đọc/ghi |
| SECURITY-07 | Compliant | SEC-009 yêu cầu network deny-by-default, giới hạn cổng/nguồn và private placement |
| SECURITY-08 | Compliant | FR-002 và SEC-003 yêu cầu server-side, object-level và function-level authorization |
| SECURITY-09 | Compliant | SEC-007 và NFR-005 yêu cầu hardening, safe errors, no defaults, private storage, secret handling và image pinning |
| SECURITY-10 | Compliant | SEC-006 xác định pinning, scanning, trusted registry và SBOM |
| SECURITY-11 | Compliant | FR-002, SEC-003 và thiết kế abuse controls/rate limiting được đặt làm ràng buộc downstream |
| SECURITY-12 | Compliant | SEC-002 xác định password, MFA admin, session và brute-force controls |
| SECURITY-13 | Compliant | SEC-006 và SEC-008 yêu cầu artifact/pipeline/data integrity, SRI và audit |
| SECURITY-14 | Compliant | SEC-005 xác định alerting, retention và log integrity |
| SECURITY-15 | Compliant | SEC-007 xác định fail-closed, cleanup, global handler và safe errors |

Không có blocking security finding tại Requirements Analysis. Việc triển khai từng control phải được xác minh lại ở các stage thiết kế, code và test.

## 13. Resiliency Compliance tại Requirements Analysis

| Rule | Trạng thái | Cách đáp ứng ở requirements |
|---|---|---|
| RESILIENCY-01 | Compliant | REL-001 xác định mức quan trọng Trung bình và yêu cầu impact/dependency mapping |
| RESILIENCY-02 | Compliant | REL-002 xác định RTO/RPO theo giờ và Backup & Restore |
| RESILIENCY-03 | Compliant | REL-003 yêu cầu quy trình change management nhẹ |
| RESILIENCY-04 | Compliant | REL-004 xác định CI/CD cần đề xuất, direct/in-place và version-pinned rollback |
| RESILIENCY-05 | Compliant | REL-005 yêu cầu metrics, logs, traces và dashboard downstream |
| RESILIENCY-06 | Compliant | REL-005 yêu cầu shallow/deep health checks và integration với routing downstream |
| RESILIENCY-07 | Compliant | REL-005 và REL-006 yêu cầu resiliency/capacity alarms; tool cụ thể thuộc Infrastructure Design |
| RESILIENCY-08 | Compliant | REL-002 và REL-006 xác định production single-region multi-zone; local được miễn HA |
| RESILIENCY-09 | Compliant | REL-006 yêu cầu scaling limits, triggers và quota awareness |
| RESILIENCY-10 | Compliant | REL-007 yêu cầu timeout, circuit breaker, bulkhead/resource limit và degraded mode |
| RESILIENCY-11 | Compliant | REL-002 và REL-008 xác định Backup & Restore cùng runbook |
| RESILIENCY-12 | Compliant | REL-008 yêu cầu backup tự động, mã hóa, retention và test restore |
| RESILIENCY-13 | Compliant | REL-008 yêu cầu failover/failback và recovery validation |
| RESILIENCY-14 | Compliant | REL-010 giữ decision gate bắt buộc tại NFR Design như rule cho phép |
| RESILIENCY-15 | Compliant | REL-009 yêu cầu quy trình incident response và COE nhẹ |

Không có blocking resiliency finding tại Requirements Analysis. Các quyết định và artifact chi tiết phải được xác minh lại ở các stage thiết kế, hạ tầng, code và test.
