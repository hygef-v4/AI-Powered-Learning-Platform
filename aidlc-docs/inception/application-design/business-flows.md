# 2. Main Business Flow

Tài liệu này mô tả các luồng nghiệp vụ đầu-cuối quan trọng của nền tảng. Swimlane thể hiện trách nhiệm của tác nhân, hệ thống và dịch vụ ngoài; các bước kỹ thuật không làm thay đổi kết quả nghiệp vụ được lược bỏ.

Sơ đồ Draw.io (nhãn tiếng Anh) và ảnh PNG dùng cho SRS nằm trong `../main-business-flows/`. Sơ đồ Mermaid dưới đây giữ cùng bước, nhánh quyết định và kết quả với các sơ đồ Draw.io đó.

Quy ước chung:

- Mỗi điểm kết thúc ghi rõ kết quả nghiệp vụ.
- Lane PostgreSQL xuất hiện khi luồng lưu kết quả nghiệp vụ lâu dài; Redis chỉ giữ trạng thái phiên tạm thời; RabbitMQ và AI Worker được gộp chung một lane.
- Chủ nhiệm môn (Subject Manager) được thực hiện chức năng giảng viên nhưng vẫn bị giới hạn bởi môn/lớp được phân công.

## 2.1 Đăng nhập và quản lý phiên (BF-01)

**Trigger:** Người dùng đã được nhà trường cấp tài khoản và nhập email trường cùng mật khẩu.

**End condition:** Người dùng vào được hệ thống với đúng vai trò, hoặc đăng nhập thất bại với lỗi trung tính; phiên kết thúc khi người dùng đăng xuất (session bị thu hồi) hoặc khi refresh session hết TTL trong Redis.

```mermaid
flowchart LR
    subgraph U["Người dùng"]
        U1([Bắt đầu]) --> U2[Nhập email trường và mật khẩu]
        U3(["Kết thúc: Đăng nhập thất bại"])
        U4[Sử dụng chức năng theo vai trò]
        U5{Đăng xuất?}
        U6(["Kết thúc: Đã đăng xuất"])
        U7(["Kết thúc: Phiên hết hạn"])
    end
    subgraph B["Backend"]
        B1[Kiểm tra miền email và trạng thái tài khoản]
        B2[Xác minh hash mật khẩu]
        B3{Cho phép đăng nhập?}
        B4[Phát access token và refresh token]
        B5[Thu hồi refresh session]
    end
    subgraph R["Redis"]
        R1[Kiểm tra rate limit và số lần sai]
        R2[Ghi nhận kết quả đăng nhập]
        R3[Lưu refresh session với TTL]
        R4[Session hết hạn theo TTL]
        R5[Xóa session keys]
    end
    U2 --> B1 --> R1 --> B2 --> R2 --> B3
    B3 -->|Không| U3
    B3 -->|Có| B4 --> R3 --> U4 --> U5
    U5 -->|Có| B5 --> R5 --> U6
    U5 -->|Không, không hoạt động| R4 --> U7
```

**Text alternative:** Người dùng nhập email trường và mật khẩu. Backend kiểm tra miền email và trạng thái tài khoản, Redis kiểm tra rate limit và số lần sai, sau đó backend xác minh hash mật khẩu và Redis ghi nhận kết quả (tăng hoặc đặt lại bộ đếm). Nếu không được phép đăng nhập, người dùng nhận lỗi trung tính không tiết lộ tài khoản có tồn tại hay không. Nếu hợp lệ, backend phát token và lưu refresh session có TTL. Phiên kết thúc khi người dùng đăng xuất (backend thu hồi và xóa session keys) hoặc khi session hết TTL do không hoạt động. Luồng quên mật khẩu bằng OTP là luồng hỗ trợ và không tách thành Main Business Flow riêng.

## 2.2 Thiết lập môn học, lớp và phân công (BF-02)

**Trigger:** Quản trị viên cần mở môn/lớp mới hoặc thay đổi người phụ trách.

**End condition:** Môn và lớp hợp lệ được lưu; mỗi môn có một Chủ nhiệm môn, mỗi lớp có một giảng viên chính và người được phân công được thông báo. Dữ liệu không hợp lệ được trả lại để quản trị viên sửa và xác nhận lại.

```mermaid
flowchart LR
    subgraph A["Quản trị viên"]
        A1([Bắt đầu]) --> A2[Tạo hoặc chọn môn học]
        A3[Nhập thông tin lớp]
        A4[Chọn Chủ nhiệm môn và giảng viên chính]
        A5[Xác nhận thiết lập]
        A6[Sửa các lỗi được đánh dấu]
        A7(["Kết thúc: Thiết lập hoàn tất"])
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền, dữ liệu trùng và phạm vi]
        B2{Hợp lệ?}
        B3[Thông báo người được phân công]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu môn, lớp và phân công nhân sự)]
    end
    A2 --> A3 --> A4 --> A5 --> B1 --> B2
    B2 -->|Không| A6 --> A5
    B2 -->|Có| D1 --> B3 --> A7
```

**Text alternative:** Quản trị viên tạo hoặc chọn môn, nhập lớp và chọn Chủ nhiệm môn cùng giảng viên chính rồi xác nhận. Backend kiểm tra role, dữ liệu trùng và phạm vi. Nếu có lỗi, quản trị viên sửa các trường được đánh dấu và xác nhận lại. Dữ liệu hợp lệ được lưu vào PostgreSQL và người được phân công nhận thông báo.

## 2.3 Upload tài liệu, AI tóm tắt và chia bài học (BF-03)

**Trigger:** Giảng viên hoặc Chủ nhiệm môn tải DOCX/PDF lên môn hay lớp được phân công và yêu cầu AI xử lý.

**End condition:** File gốc được lưu riêng tư trên Google Drive và các bài học được xuất bản sau khi người có quyền duyệt bản nháp; hoặc upload bị từ chối, hoặc xử lý AI thất bại mà không tạo bài học.

```mermaid
flowchart LR
    subgraph T["Giảng viên hoặc Chủ nhiệm môn"]
        T1([Bắt đầu]) --> T2[Chọn phạm vi và tải DOCX hoặc PDF]
        T3(["Kết thúc: Upload bị từ chối"])
        T4[Yêu cầu tóm tắt và chia bài học]
        T5(["Kết thúc: Xử lý thất bại"])
        T6[Xem và chỉnh sửa bài học nháp]
        T7{Duyệt bản nháp?}
        T8(["Kết thúc: Bài học đã xuất bản"])
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền, loại file và kích thước]
        B2{File hợp lệ?}
        B3[Tạo AI job có version]
        B4{Xử lý thành công?}
        B5[Xuất bản bài học đã duyệt]
    end
    subgraph G["Google Drive"]
        G1[Lưu file gốc riêng tư]
        G2[Worker đọc file gốc]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Đưa job vào hàng đợi]
        W2[Trích xuất, tóm tắt và chia bài học]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu metadata artifact)]
        D2[(Lưu bài học và tóm tắt DRAFT)]
        D3[(Chuyển bài học sang PUBLISHED)]
    end
    T2 --> B1 --> B2
    B2 -->|Không| T3
    B2 -->|Có| G1 --> D1 --> T4 --> B3 --> W1 --> G2 --> W2 --> B4
    B4 -->|Không| T5
    B4 -->|Có| D2 --> T6 --> T7
    T7 -->|Chưa| T6
    T7 -->|Có| B5 --> D3 --> T8
```

**Text alternative:** File hợp lệ được lưu riêng tư trên Google Drive và metadata được lưu trong `artifacts`. Khi người dùng yêu cầu, backend tạo job có version qua RabbitMQ; AI Worker đọc file gốc bằng quyền nội bộ, trích xuất, tóm tắt và chia bài học. Nếu xử lý thất bại, người dùng nhận thông báo an toàn và không có bài học nào được tạo. Nếu thành công, kết quả được lưu ở trạng thái `DRAFT` để người dùng chỉnh sửa; chỉ bài học đã duyệt mới được chuyển sang `PUBLISHED`.

## 2.4 Soạn, duyệt và phát hành assignment (BF-04)

**Trigger:** Giảng viên hoặc Chủ nhiệm môn muốn tạo một assignment mới, thủ công hoặc có AI hỗ trợ.

**End condition:** Một phiên bản assignment bất biến được phát hành tới các lớp mục tiêu và sinh viên nhận được thông báo; hoặc phiên bản nháp được lưu để tiếp tục chỉnh sửa.

```mermaid
flowchart LR
    subgraph T["Giảng viên hoặc Chủ nhiệm môn"]
        T1([Bắt đầu]) --> T2[Chọn phạm vi, loại assignment và rubric]
        T3{Dùng AI tạo nháp?}
        T4[Soạn hoặc chỉnh sửa nội dung]
        T5[Xem trước như sinh viên]
        T6{Sẵn sàng phát hành?}
        T7[Chọn lớp mục tiêu và lịch]
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền và phạm vi nguồn]
        B2[Tạo AI draft job]
        B3[Lưu assignment version DRAFT]
        B4[Đóng băng phiên bản và tạo publication]
        B5[Thông báo sinh viên]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Tạo bản nháp đề xuất có căn cứ]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu phiên bản nháp)]
        D2[(Lưu phiên bản bất biến và publication)]
    end
    subgraph S["Sinh viên"]
        S1[Nhận assignment đã phát hành]
        S2(["Kết thúc: Assignment đã phát hành"])
    end
    T2 --> B1 --> T3
    T3 -->|Có| B2 --> W1 --> T4
    T3 -->|Không| T4
    T4 --> T5 --> T6
    T6 -->|Chưa| B3 --> D1 --> T4
    T6 -->|Có| T7 --> B4 --> D2 --> B5 --> S1 --> S2
```

**Text alternative:** Người có quyền chọn phạm vi, loại assignment và rubric; backend kiểm tra quyền và phạm vi nguồn. AI có thể tạo bản nháp nhưng người dùng luôn phải soạn hoặc chỉnh sửa và xem trước. Nếu chưa sẵn sàng, phiên bản được lưu dạng `DRAFT` rồi quay lại chỉnh sửa. Khi phát hành, người dùng chọn lớp mục tiêu và lịch: giảng viên chỉ chọn lớp được phân công, Chủ nhiệm môn có thể phát hành cho mọi lớp thuộc môn mà không cần giảng viên lớp duyệt lại. Backend đóng băng phiên bản, lưu publication và thông báo sinh viên.

## 2.5 Truy cập bài học và ghi nhận tiến độ (BF-05)

**Trigger:** Sinh viên mở một bài học thuộc lớp đã ghi danh.

**End condition:** Nội dung (và file đính kèm nếu có) được hiển thị tại vị trí học gần nhất và tiến độ mới được lưu; hoặc truy cập bị từ chối rõ ràng.

```mermaid
flowchart LR
    subgraph S["Sinh viên"]
        S1([Bắt đầu]) --> S2[Chọn bài học]
        S3(["Kết thúc: Truy cập bị từ chối"])
        S4[Học nội dung hoặc tải file]
        S5[Tiếp tục hoặc đánh dấu hoàn thành]
        S6(["Kết thúc: Tiến độ đã lưu"])
    end
    subgraph B["Backend"]
        B1[Kiểm tra enrollment và access grant]
        B2{Có quyền?}
        B3{Có file đính kèm?}
        B4[Trả nội dung tại vị trí gần nhất]
        B5[Lưu tiến độ idempotent]
    end
    subgraph D["PostgreSQL"]
        D1[(Đọc nội dung bài học và vị trí gần nhất)]
        D2[(Upsert bản ghi tiến độ)]
    end
    subgraph G["Google Drive"]
        G1[Đọc file riêng tư theo provider file ID]
    end
    S2 --> B1 --> B2
    B2 -->|Không| S3
    B2 -->|Có| D1 --> B3
    B3 -->|Có| G1 --> B4
    B3 -->|Không| B4
    B4 --> S4 --> S5 --> B5 --> D2 --> S6
```

**Text alternative:** Backend kiểm tra ghi danh và access grant. Khi hợp lệ, backend đọc nội dung bài học và vị trí học gần nhất từ PostgreSQL; chỉ khi bài học có file đính kèm, backend mới đọc file riêng tư trên Google Drive theo provider file ID. Mọi truy cập dữ liệu đều đi qua backend. Tiến độ mới được lưu idempotent vào PostgreSQL.

## 2.6 Tổ chức nhóm, phân việc và đổi leader (BF-06)

**Trigger:** Giảng viên cần chia lớp thành nhóm và phân công các phần cá nhân của một bài chung.

**End condition:** Mỗi nhóm có đúng một leader và mỗi phần cá nhân được gán cho đúng một thành viên; nếu có yêu cầu đổi leader, giảng viên phê duyệt hoặc từ chối, quyết định được lưu kèm lịch sử audit và thông báo cho nhóm.

```mermaid
flowchart LR
    subgraph T["Giảng viên"]
        T1([Bắt đầu]) --> T2[Tạo nhóm từ sinh viên đã ghi danh]
        T3[Chỉ định đúng một leader mỗi nhóm]
        T4[Chia bài chung thành các phần cá nhân]
        T5[Gán mỗi phần cho một thành viên]
        T6[Sửa thiết lập nhóm]
        T7[Xem yêu cầu đổi leader]
        T8{Phê duyệt?}
        T9[Chọn leader mới]
    end
    subgraph B["Backend"]
        B1[Kiểm tra ghi danh, tính duy nhất và một leader]
        B2{Hợp lệ?}
        B3[Lưu yêu cầu đổi leader]
        B4[Giữ leader hiện tại và ghi lý do]
        B5[Thay leader đang hiệu lực]
        B6[Thông báo quyết định cho nhóm]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu nhóm, leader và phân công)]
        D2[(Lưu quyết định và lịch sử audit)]
    end
    subgraph S["Sinh viên"]
        S1[Nhận nhóm và phần được giao]
        S2{Yêu cầu đổi leader?}
        S3[Gửi yêu cầu kèm lý do]
        S4(["Kết thúc: Leader không đổi"])
        S5[Nhận quyết định]
        S6(["Kết thúc: Yêu cầu đã được quyết định"])
    end
    T2 --> T3 --> T4 --> T5 --> B1 --> B2
    B2 -->|Không| T6 --> T2
    B2 -->|Có| D1 --> S1 --> S2
    S2 -->|Không| S4
    S2 -->|Có| S3 --> B3 --> T7 --> T8
    T8 -->|Có| T9 --> B5 --> D2
    T8 -->|Không| B4 --> D2
    D2 --> B6 --> S5 --> S6
```

**Text alternative:** Giảng viên tạo nhóm từ sinh viên đã ghi danh, chỉ định đúng một leader và gán mỗi phần cá nhân cho một thành viên. Backend kiểm tra ghi danh, tính duy nhất và ràng buộc một leader; nếu không hợp lệ, giảng viên sửa thiết lập. Thành viên có thể gửi yêu cầu đổi leader kèm lý do; chỉ giảng viên được phê duyệt (chọn leader mới) hoặc từ chối (giữ leader và ghi lý do). Mọi quyết định được lưu lịch sử audit và thông báo cho nhóm.

## 2.7 Làm và nộp bài cá nhân hoặc bài chung (BF-07)

**Trigger:** Assignment đang mở và sinh viên muốn nộp phần cá nhân được giao hoặc leader muốn nộp DOCX chung.

**End condition:** Một submission bất biến được tạo, file đầy đủ được lưu riêng tư trên Google Drive và người nộp nhận biên nhận; hoặc bài nộp bị từ chối kèm lý do mà không lưu file.

```mermaid
flowchart LR
    subgraph S["Sinh viên hoặc Leader"]
        S1([Bắt đầu]) --> S2[Mở assignment hoặc phần được giao]
        S3{Bài chung DOCX?}
        S4[Leader đính kèm DOCX chung]
        S5["Hoàn thành phần cá nhân (câu trả lời hoặc XML Draw.io)"]
        S6[Xác nhận nộp bài]
        S7(["Kết thúc: Bài nộp bị từ chối"])
        S8[Nhận biên nhận]
        S9(["Kết thúc: Bài nộp đã ghi nhận"])
    end
    subgraph B["Backend"]
        B1[Kiểm tra hạn nộp, số lần nộp, quyền và file]
        B2{Hợp lệ?}
        B3[Cấp biên nhận nộp bài]
    end
    subgraph G["Google Drive"]
        G1[Lưu DOCX hoặc XML đầy đủ riêng tư]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu artifact metadata và submission bất biến)]
    end
    S2 --> S3
    S3 -->|Có| S4 --> S6
    S3 -->|Không| S5 --> S6
    S6 --> B1 --> B2
    B2 -->|Không| S7
    B2 -->|Có| G1 --> D1 --> B3 --> S8 --> S9
```

**Text alternative:** Loại bài nộp do assignment quyết định. Với bài chung, chỉ leader hiện tại được đính kèm và nộp hoặc nộp lại DOCX chung; với phần cá nhân, chỉ thành viên được giao phần đó được nộp câu trả lời hoặc XML Draw.io đầy đủ. Sau khi người dùng xác nhận, backend kiểm tra hạn nộp, số lần nộp, quyền và file. Chỉ bài nộp hợp lệ mới được lưu file riêng tư lên Google Drive, nên không phát sinh file mồ côi khi người dùng không xác nhận hoặc bị từ chối. Metadata và submission bất biến được lưu trong PostgreSQL rồi backend cấp biên nhận.

## 2.8 Chọn phương pháp chấm và công bố điểm (BF-08)

**Trigger:** Giảng viên mở một submission hợp lệ chưa được chốt điểm.

**End condition:** Điểm cuối cùng do giảng viên nhập hoặc xác nhận được lưu kèm lịch sử thay đổi; điểm phần cá nhân và điểm bài chung được lưu riêng và công bố cho đúng sinh viên hoặc nhóm; bài chung chỉ được chấm tay.

```mermaid
flowchart LR
    subgraph T["Giảng viên"]
        T1([Bắt đầu]) --> T2[Mở bài nộp và rubric]
        T3{Bài chung DOCX?}
        T4{Yêu cầu AI đề xuất?}
        T5["Xem đề xuất AI (không phải điểm cuối)"]
        T6[Nhập hoặc xác nhận điểm và phản hồi cuối cùng]
        T7[Chốt và công bố điểm]
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền và trạng thái bài nộp]
        B2["Gửi dữ liệu tối thiểu cho AI (XML rút gọn với Draw.io)"]
        B3{Nhận được đề xuất?}
        B4[Lưu grade và grade history]
        B5[Thông báo sinh viên hoặc nhóm]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Phân tích bài cá nhân và trả đề xuất]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu riêng điểm phần cá nhân hoặc bài chung)]
    end
    subgraph S["Sinh viên"]
        S1[Xem điểm đã công bố]
        S2(["Kết thúc: Đã nhận điểm"])
    end
    T2 --> B1 --> T3
    T3 -->|Có, đối chiếu các phần cá nhân| T6
    T3 -->|Không| T4
    T4 -->|Không| T6
    T4 -->|Có| B2 --> W1 --> B3
    B3 -->|Có| T5 --> T6
    B3 -->|Lỗi hoặc quá thời gian| T6
    T6 --> T7 --> B4 --> D1 --> B5 --> S1 --> S2
```

**Text alternative:** Bài chung DOCX luôn được chấm tay; giảng viên xem bài chung cạnh các phần cá nhân để đối chiếu. Với phần cá nhân, giảng viên chọn chấm tay hoặc yêu cầu AI đề xuất. Với Draw.io, XML đầy đủ được giữ nguyên, bản rút gọn chỉ được tạo khi gọi AI. Nếu AI lỗi hoặc quá thời gian, giảng viên chuyển sang chấm tay. Đề xuất AI không tự trở thành điểm cuối: dù chấp nhận, sửa hay bỏ đề xuất, giảng viên luôn phải nhập hoặc xác nhận điểm và phản hồi cuối cùng trước khi chốt. Backend lưu grade cùng grade history, lưu riêng điểm phần cá nhân và bài chung, rồi thông báo sinh viên hoặc nhóm.

## 2.9 Thanh toán và cấp quyền truy cập (BF-09)

**Trigger:** Sinh viên chọn một gói học hoặc nội dung yêu cầu thanh toán.

**End condition:** Webhook hợp lệ báo thanh toán thành công thì payment chuyển `PAID` và access grant được cấp đúng một lần; thanh toán thất bại hoặc bị hủy thì payment chuyển `FAILED` và không cấp quyền; webhook không hợp lệ bị từ chối, ghi log và không làm đổi trạng thái payment.

```mermaid
flowchart LR
    subgraph S["Sinh viên"]
        S1([Bắt đầu]) --> S2[Chọn gói và xác nhận thanh toán]
        S3[Hoàn tất thanh toán trên trang cổng]
        S4[Xem kết quả và quyền truy cập]
        S5(["Kết thúc: Đã hiển thị kết quả"])
    end
    subgraph B["Backend"]
        B1[Tạo payment với idempotency key]
        B2[Chuyển hướng tới trang thanh toán]
        B3[Xác minh chữ ký, số tiền và chống replay]
        B4{Webhook hợp lệ?}
        B5(["Kết thúc: Từ chối webhook, không đổi trạng thái"])
        B6{Thanh toán thành công?}
        B7[Thông báo kết quả thanh toán]
    end
    subgraph P["Cổng thanh toán"]
        P1[Xử lý giao dịch]
        P2[Gửi webhook đã ký]
    end
    subgraph D["PostgreSQL"]
        D1[(Lưu payment PENDING)]
        D2[(Đánh dấu FAILED, không cấp quyền)]
        D3[(Đánh dấu PAID và cấp quyền đúng một lần)]
    end
    S2 --> B1 --> D1 --> B2 --> S3 --> P1 --> P2 --> B3 --> B4
    B4 -->|Không| B5
    B4 -->|Có| B6
    B6 -->|Không| D2 --> B7
    B6 -->|Có| D3 --> B7
    B7 --> S4 --> S5
```

**Text alternative:** Backend tạo payment với idempotency key, lưu trạng thái `PENDING` và chuyển sinh viên tới trang của cổng thanh toán. Sinh viên hoàn tất thanh toán trước, sau đó cổng mới xử lý giao dịch và gửi webhook đã ký. Backend xác minh chữ ký, số tiền và chống replay; webhook không hợp lệ bị từ chối, ghi log bảo mật và không làm đổi trạng thái. Webhook hợp lệ báo thất bại thì payment chuyển `FAILED`; báo thành công thì payment chuyển `PAID` và `access_grants` được tạo đúng một lần, kể cả khi webhook bị gửi lặp. Kết quả redirect từ trình duyệt không tự cấp quyền. Đối soát định kỳ với nhà cung cấp là luồng hỗ trợ.

## 2.10 Business Flow Coverage

| Business flow | Nghiệp vụ chính được bao phủ |
|---|---|
| BF-01 | Tài khoản được cấp sẵn, xác minh mật khẩu, rate limit, session Redis, đăng xuất và hết hạn phiên |
| BF-02 | Môn, lớp, Chủ nhiệm môn, giảng viên chính và sửa lỗi dữ liệu |
| BF-03 | Google Drive, artifact, AI tóm tắt và chia bài học, lỗi xử lý AI, duyệt và xuất bản |
| BF-04 | Assignment thủ công/AI, bản nháp, xem trước và phát hành tới lớp mục tiêu |
| BF-05 | Enrollment, access grant, học liệu, file đính kèm tùy chọn và tiến độ |
| BF-06 | Nhóm, leader, phần việc cá nhân, kiểm tra hợp lệ và yêu cầu đổi leader có audit |
| BF-07 | Bài cá nhân, XML Draw.io đầy đủ, DOCX chung, xác nhận trước khi lưu và biên nhận |
| BF-08 | Chấm tay hoặc AI đề xuất, fallback khi AI lỗi, XML rút gọn, điểm lưu riêng và grade history |
| BF-09 | Payment `PENDING`, webhook đã ký, `FAILED`/`PAID` và access grant idempotent |

Các use case quản trị AI, báo cáo, audit, notification, quên mật khẩu bằng OTP và đối soát thanh toán là luồng hỗ trợ hoặc luồng quản trị, được gọi từ các business flow chính khi cần và không tách thành Main Business Flow riêng.
