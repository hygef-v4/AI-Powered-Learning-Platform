# 2. Main Business Flow

Tài liệu này mô tả các luồng nghiệp vụ đầu-cuối quan trọng của nền tảng. Swimlane thể hiện trách nhiệm của tác nhân, hệ thống và dịch vụ ngoài; các bước kỹ thuật không làm thay đổi kết quả nghiệp vụ được lược bỏ.

## 2.1 Đăng nhập và quản lý phiên (BF-01)

**Trigger:** Người dùng đã được nhà trường cấp tài khoản và nhập email trường cùng mật khẩu.

**End condition:** Người dùng vào được hệ thống với đúng vai trò, hoặc nhận lỗi an toàn; phiên hợp lệ được lưu trong Redis và có thể bị thu hồi.

```mermaid
flowchart LR
    subgraph U["Người dùng"]
        U1([Bắt đầu]) --> U2[Nhập email và mật khẩu]
        U3[Truy cập chức năng theo vai trò]
        U4[Đăng xuất]
        U5([Kết thúc])
    end
    subgraph B["Backend"]
        B1[Kiểm tra định dạng và trạng thái tài khoản]
        B2{Thông tin hợp lệ?}
        B3[Trả lỗi trung tính]
        B4[Phát access token và refresh token]
        B5[Thu hồi phiên]
    end
    subgraph R["Redis"]
        R1[Kiểm tra rate limit và số lần sai]
        R2[Lưu refresh session với TTL]
        R3[Xóa session]
    end
    U2 --> B1 --> R1 --> B2
    B2 -- Không --> B3 --> U5
    B2 -- Có --> B4 --> R2 --> U3 --> U4 --> B5 --> R3 --> U5
```

**Text alternative:** Người dùng đăng nhập; backend kiểm tra tài khoản và giới hạn thử sai qua Redis. Nếu hợp lệ, backend tạo token và lưu refresh session có TTL. Khi đăng xuất, session bị xóa khỏi Redis.

## 2.2 Thiết lập môn học, lớp và phân công (BF-02)

**Trigger:** Quản trị viên cần mở môn/lớp mới hoặc thay đổi người phụ trách.

**End condition:** Môn và lớp hợp lệ được lưu; mỗi môn có một Chủ nhiệm môn, mỗi lớp có một giảng viên chính và các bên được thông báo.

```mermaid
flowchart LR
    subgraph A["Quản trị viên"]
        A1([Bắt đầu]) --> A2[Nhập thông tin môn]
        A3[Nhập thông tin lớp]
        A4[Chọn Chủ nhiệm môn và giảng viên chính]
        A5[Xác nhận]
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền và dữ liệu]
        B2{Hợp lệ?}
        B3[Hiển thị lỗi cần sửa]
        B4[Lưu môn lớp và phân công]
        B5[Gửi thông báo]
    end
    subgraph D["PostgreSQL"]
        D1[(Accounts Subjects Classes)]
    end
    A2 --> A3 --> A4 --> A5 --> B1 --> B2
    B2 -- Không --> B3 --> A2
    B2 -- Có --> B4 --> D1 --> B5 --> A6([Kết thúc])
```

**Text alternative:** Quản trị viên tạo môn, lớp và chọn người phụ trách. Backend kiểm tra role, dữ liệu trùng và phạm vi; dữ liệu hợp lệ được lưu rồi thông báo cho người được phân công.

## 2.3 Upload tài liệu, AI tóm tắt và chia bài học (BF-03)

**Trigger:** Giảng viên hoặc Chủ nhiệm môn tải DOCX/PDF lên môn hay lớp được phân công và yêu cầu AI xử lý.

**End condition:** File gốc được lưu riêng tư trên Google Drive; các bài học và bản tóm tắt ở trạng thái nháp được lưu để giảng viên duyệt, hoặc lỗi được báo an toàn.

```mermaid
flowchart LR
    subgraph T["Giảng viên hoặc Chủ nhiệm môn"]
        T1([Bắt đầu]) --> T2[Chọn phạm vi và tải tài liệu]
        T3[Yêu cầu tóm tắt và chia bài]
        T4[Xem sửa và duyệt bản nháp]
        T5{Chấp nhận?}
        T6[Xuất bản bài học]
        T7([Kết thúc])
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền loại file và kích thước]
        B2{File hợp lệ?}
        B3[Trả lỗi an toàn]
        B4[Lưu metadata artifact]
        B5[Gửi job có version]
        B6[Lưu lesson và summary ở trạng thái DRAFT]
    end
    subgraph G["Google Drive"]
        G1[Lưu file gốc riêng tư]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Nhận job]
        W2[Tải file theo quyền nội bộ]
        W3[Trích xuất tóm tắt và chia bài học]
        W4[Trả kết quả]
    end
    T2 --> B1 --> B2
    B2 -- Không --> B3 --> T7
    B2 -- Có --> G1 --> B4 --> T3 --> B5 --> W1 --> W2 --> W3 --> W4 --> B6 --> T4 --> T5
    T5 -- Không --> T4
    T5 -- Có --> T6 --> T7
```

**Text alternative:** File hợp lệ được lưu trên Google Drive và metadata được lưu trong `artifacts`. Backend gửi job qua RabbitMQ; worker tạo tóm tắt và các bài học. Backend lưu kết quả dưới dạng nháp để giảng viên chỉnh sửa, duyệt rồi mới xuất bản.

## 2.4 Soạn, duyệt và phát hành assignment (BF-04)

**Trigger:** Giảng viên hoặc Chủ nhiệm môn muốn tạo một assignment mới, thủ công hoặc có AI hỗ trợ.

**End condition:** Một phiên bản assignment bất biến đã được phát hành tới đúng lớp, hoặc bản nháp được giữ lại để chỉnh sửa.

```mermaid
flowchart LR
    subgraph T["Giảng viên hoặc Chủ nhiệm môn"]
        T1([Bắt đầu]) --> T2[Chọn phạm vi loại bài và rubric]
        T3{Dùng AI tạo nháp?}
        T4[Soạn hoặc chỉnh sửa nội dung]
        T5[Xem trước như người học]
        T6{Duyệt phát hành?}
        T7[Chọn lớp và lịch]
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền và nguồn]
        B2[Gửi job tạo nháp]
        B3[Lưu assignment version]
        B4[Đóng băng phiên bản và tạo publication]
        B5[Thông báo người học]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Tạo nội dung đề xuất có căn cứ]
    end
    subgraph L["Người học"]
        L1[Nhận assignment đã phát hành]
        L2([Kết thúc])
    end
    T2 --> B1 --> T3
    T3 -- Có --> B2 --> W1 --> T4
    T3 -- Không --> T4
    T4 --> T5 --> T6
    T6 -- Chưa --> B3 --> T4
    T6 -- Có --> T7 --> B4 --> B5 --> L1 --> L2
```

**Text alternative:** Người có quyền chọn phạm vi, loại bài và rubric. AI có thể tạo bản nháp nhưng người dùng phải chỉnh sửa và duyệt. Khi phát hành, backend đóng băng phiên bản, gắn lịch và lớp rồi thông báo người học.

## 2.5 Truy cập bài học và ghi nhận tiến độ (BF-05)

**Trigger:** Sinh viên mở một bài học thuộc lớp đã ghi danh và có quyền truy cập.

**End condition:** Nội dung được hiển thị tại vị trí gần nhất; tiến độ mới được lưu hoặc truy cập bị từ chối rõ ràng.

```mermaid
flowchart LR
    subgraph S["Sinh viên"]
        S1([Bắt đầu]) --> S2[Chọn bài học]
        S3[Đọc nội dung hoặc tải file]
        S4[Tiếp tục hoặc đánh dấu hoàn thành]
        S5([Kết thúc])
    end
    subgraph B["Backend"]
        B1[Kiểm tra enrollment và access grant]
        B2{Có quyền?}
        B3[Từ chối truy cập]
        B4[Lấy nội dung và vị trí gần nhất]
        B5[Lưu tiến độ idempotent]
    end
    subgraph G["Google Drive"]
        G1[Đọc file riêng tư theo provider file ID]
    end
    subgraph D["PostgreSQL"]
        D1[(Learning resources và progress)]
    end
    S2 --> B1 --> B2
    B2 -- Không --> B3 --> S5
    B2 -- Có --> B4 --> D1 --> G1 --> S3 --> S4 --> B5 --> D1 --> S5
```

**Text alternative:** Backend kiểm tra ghi danh và quyền truy cập. Khi hợp lệ, hệ thống lấy học liệu, file Google Drive nếu có và vị trí học gần nhất. Tiến độ mới được lưu idempotent vào PostgreSQL.

## 2.6 Tổ chức nhóm, phân việc và đổi leader (BF-06)

**Trigger:** Giảng viên cần chia lớp thành nhóm và phân công các phần cá nhân của một bài chung.

**End condition:** Mỗi nhóm có đúng một leader, mỗi thành viên có phần việc; yêu cầu đổi leader nếu có đã được giảng viên quyết định và lưu lịch sử.

```mermaid
flowchart LR
    subgraph T["Giảng viên"]
        T1([Bắt đầu]) --> T2[Tạo nhóm từ sinh viên đã ghi danh]
        T3[Chỉ định một leader]
        T4[Chia bài chung thành các phần cá nhân]
        T5[Gán phần việc cho từng thành viên]
        T6[Xem yêu cầu đổi leader]
        T7{Phê duyệt?}
        T8[Chỉ định leader mới]
        T9[Từ chối và ghi lý do]
    end
    subgraph B["Backend"]
        B1[Kiểm tra thành viên và tính duy nhất]
        B2[Lưu nhóm leader và allocations]
        B3[Lưu yêu cầu]
        B4[Cập nhật leader và ghi lịch sử]
        B5[Thông báo kết quả]
    end
    subgraph S["Sinh viên"]
        S1[Nhận nhóm và phần việc]
        S2{Muốn đổi leader?}
        S3[Gửi yêu cầu và lý do]
        S4[Nhận quyết định]
        S5([Kết thúc])
    end
    T2 --> T3 --> T4 --> T5 --> B1 --> B2 --> S1 --> S2
    S2 -- Không --> S5
    S2 -- Có --> S3 --> B3 --> T6 --> T7
    T7 -- Có --> T8 --> B4 --> B5 --> S4 --> S5
    T7 -- Không --> T9 --> B5
```

**Text alternative:** Giảng viên tạo nhóm, chọn đúng một leader và gán phần riêng cho từng thành viên. Sinh viên có thể yêu cầu đổi leader; chỉ giảng viên được phê duyệt hoặc từ chối và hệ thống lưu quyết định.

## 2.7 Làm và nộp bài cá nhân hoặc bài chung (BF-07)

**Trigger:** Assignment đang mở và sinh viên muốn nộp phần cá nhân hoặc leader muốn nộp DOCX chung.

**End condition:** Một submission bất biến và biên nhận được tạo; file đầy đủ nằm trên Google Drive, hoặc yêu cầu bị từ chối vì không hợp lệ.

```mermaid
flowchart LR
    subgraph S["Sinh viên hoặc Leader"]
        S1([Bắt đầu]) --> S2[Mở assignment và làm bài]
        S3[Chọn loại bài nộp]
        S4{Bài chung?}
        S5[Leader chọn DOCX chung]
        S6[Sinh viên chọn câu trả lời hoặc XML đầy đủ]
        S7[Xác nhận nộp]
        S8[Nhận biên nhận]
        S9([Kết thúc])
    end
    subgraph B["Backend"]
        B1[Kiểm tra thời hạn attempt và quyền]
        B2{Hợp lệ?}
        B3[Từ chối và nêu lý do]
        B4[Kiểm tra cấu trúc và metadata file]
        B5[Tạo submission bất biến]
    end
    subgraph G["Google Drive"]
        G1[Lưu DOCX hoặc XML đầy đủ riêng tư]
    end
    subgraph D["PostgreSQL"]
        D1[(Artifact metadata và submission)]
    end
    S2 --> S3 --> S4
    S4 -- Có --> S5 --> B1
    S4 -- Không --> S6 --> B1
    B1 --> B2
    B2 -- Không --> B3 --> S9
    B2 -- Có --> B4 --> G1 --> S7 --> B5 --> D1 --> S8 --> S9
```

**Text alternative:** Sinh viên nộp phần cá nhân; chỉ leader được nộp DOCX chung. Backend kiểm tra quyền, thời hạn, số lần nộp và file. File đầy đủ được lưu riêng tư trên Google Drive, còn metadata và submission bất biến được lưu trong PostgreSQL.

## 2.8 Chọn phương pháp chấm và công bố điểm (BF-08)

**Trigger:** Giảng viên mở một submission hợp lệ chưa được chốt điểm.

**End condition:** Điểm cuối cùng do giảng viên quyết định được lưu, lịch sử thay đổi được bảo toàn, công bố cho đúng sinh viên; bài chung chỉ được chấm tay.

```mermaid
flowchart LR
    subgraph T["Giảng viên"]
        T1([Bắt đầu]) --> T2[Mở bài nộp và rubric]
        T3{Bài chung?}
        T4{Chọn AI hay chấm tay?}
        T5[Nhập điểm và phản hồi thủ công]
        T6[Xem đề xuất AI]
        T7[Chấp nhận sửa hoặc bỏ đề xuất]
        T8[Chốt và công bố]
    end
    subgraph B["Backend"]
        B1[Kiểm tra quyền và submission]
        B2[Tạo XML rút gọn nếu cần]
        B3[Gửi job AI với dữ liệu tối thiểu]
        B4[Hiển thị đề xuất không phải điểm cuối]
        B5[Lưu grade và grade history]
        B6[Gửi thông báo]
    end
    subgraph W["RabbitMQ và AI Worker"]
        W1[Phân tích bài cá nhân]
        W2[Trả score feedback và căn cứ đề xuất]
    end
    subgraph S["Sinh viên"]
        S1[Xem điểm đã công bố]
        S2([Kết thúc])
    end
    T2 --> B1 --> T3
    T3 -- Có --> T5
    T3 -- Không --> T4
    T4 -- Chấm tay --> T5
    T4 -- AI đề xuất --> B2 --> B3 --> W1 --> W2 --> B4 --> T6 --> T7
    T5 --> T8
    T7 --> T8 --> B5 --> B6 --> S1 --> S2
```

**Text alternative:** Giảng viên quyết định chấm tay hoặc yêu cầu AI sau khi nhận bài. Bài chung luôn chấm tay. Với Draw.io, XML đầy đủ được giữ nguyên, còn bản rút gọn chỉ tạo khi gọi AI. Đề xuất AI không tự trở thành điểm cuối; giảng viên chốt và công bố điểm.

## 2.9 Thanh toán và cấp quyền truy cập (BF-09)

**Trigger:** Sinh viên chọn một gói học hoặc nội dung yêu cầu thanh toán.

**End condition:** Payment được xác minh và access grant được cấp đúng một lần, hoặc giao dịch thất bại/hủy mà không cấp quyền.

```mermaid
flowchart LR
    subgraph S["Sinh viên"]
        S1([Bắt đầu]) --> S2[Chọn gói và xác nhận thanh toán]
        S3[Hoàn tất trên trang cổng thanh toán]
        S4[Xem kết quả và quyền truy cập]
        S5([Kết thúc])
    end
    subgraph B["Backend"]
        B1[Tạo payment idempotent]
        B2[Chuyển hướng thanh toán]
        B3[Nhận webhook]
        B4[Kiểm tra chữ ký số tiền và trạng thái]
        B5{Webhook hợp lệ và đã thanh toán?}
        B6[Giữ trạng thái thất bại hoặc chờ]
        B7[Ghi PAID và cấp access grant đúng một lần]
        B8[Thông báo kết quả]
    end
    subgraph P["Cổng thanh toán"]
        P1[Xử lý giao dịch]
        P2[Gửi webhook đã ký]
    end
    subgraph D["PostgreSQL"]
        D1[(Payments và access grants)]
    end
    S2 --> B1 --> D1 --> B2 --> P1 --> S3
    P1 --> P2 --> B3 --> B4 --> B5
    B5 -- Không --> B6 --> D1 --> B8 --> S4 --> S5
    B5 -- Có --> B7 --> D1 --> B8
```

**Text alternative:** Backend tạo payment idempotent và chuyển sinh viên tới cổng thanh toán. Chỉ webhook hợp lệ mới được dùng để đánh dấu `PAID` và tạo `access_grants` đúng một lần; kết quả redirect từ trình duyệt không tự cấp quyền.

## 2.10 Business Flow Coverage

| Business flow | Nghiệp vụ chính được bao phủ |
|---|---|
| BF-01 | Tài khoản được cấp sẵn, đăng nhập, session Redis, logout |
| BF-02 | Môn, lớp, Chủ nhiệm môn, giảng viên chính |
| BF-03 | Google Drive, artifact, AI tóm tắt và chia bài học |
| BF-04 | Assignment thủ công/AI, duyệt và phát hành |
| BF-05 | Enrollment, access grant, học liệu và tiến độ |
| BF-06 | Nhóm, leader, phần việc cá nhân và yêu cầu đổi leader |
| BF-07 | Bài cá nhân, XML Draw.io đầy đủ, DOCX chung và biên nhận |
| BF-08 | Giảng viên chọn AI/chấm tay, XML rút gọn, grade history |
| BF-09 | Payment webhook và access grant idempotent |

Các use case quản trị AI, báo cáo, audit và notification là luồng hỗ trợ hoặc luồng quản trị, được gọi từ các business flow chính khi cần và không tách thành Main Business Flow riêng.
