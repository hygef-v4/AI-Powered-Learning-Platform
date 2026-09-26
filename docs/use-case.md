# Use case — 78 mục MVP

Mỗi sơ đồ là một lát cắt của cùng hệ thống. Actor được ghi đúng như catalog; nhãn có dấu / biểu thị các vai trò đều có thể thực hiện use case đó. Mỗi mã UC xuất hiện đúng một lần. Hệ thống ngoài nằm trong [context diagram](context-diagram.md).

Nguồn: [catalog use case](../aidlc-docs/inception/user-stories/use-cases.md) và [ánh xạ unit](../aidlc-docs/inception/application-design/unit-of-work-story-map.md).

## Identity and Access Management

```mermaid
flowchart LR
    A1["Tất cả người dùng"]
    A2["Người dùng đã đăng nhập"]
    A3["Quản trị viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_IAM_01["UC-IAM-01 · Kích hoạt tài khoản"]
        UC_IAM_02["UC-IAM-02 · Đăng nhập"]
        UC_IAM_03["UC-IAM-03 · Đăng xuất"]
        UC_IAM_04["UC-IAM-04 · Khôi phục mật khẩu"]
        UC_IAM_05["UC-IAM-05 · Đổi mật khẩu"]
        UC_IAM_06["UC-IAM-06 · Xem hồ sơ cá nhân"]
        UC_IAM_07["UC-IAM-07 · Cập nhật hồ sơ cá nhân"]
        UC_IAM_08["UC-IAM-08 · Xem tài khoản người dùng"]
        UC_IAM_09["UC-IAM-09 · Tạo tài khoản thủ công"]
        UC_IAM_10["UC-IAM-10 · Nhập tài khoản hàng loạt"]
        UC_IAM_11["UC-IAM-11 · Cập nhật tài khoản và role"]
        UC_IAM_12["UC-IAM-12 · Quản lý trạng thái tài khoản"]
    end
    A1 --> UC_IAM_01
    A1 --> UC_IAM_02
    A2 --> UC_IAM_03
    A1 --> UC_IAM_04
    A2 --> UC_IAM_05
    A2 --> UC_IAM_06
    A2 --> UC_IAM_07
    A3 --> UC_IAM_08
    A3 --> UC_IAM_09
    A3 --> UC_IAM_10
    A3 --> UC_IAM_11
    A3 --> UC_IAM_12
```

**Diễn giải bằng chữ:** UC-IAM-01 — Kích hoạt tài khoản; UC-IAM-02 — Đăng nhập; UC-IAM-03 — Đăng xuất; UC-IAM-04 — Khôi phục mật khẩu; UC-IAM-05 — Đổi mật khẩu; UC-IAM-06 — Xem hồ sơ cá nhân; UC-IAM-07 — Cập nhật hồ sơ cá nhân; UC-IAM-08 — Xem tài khoản người dùng; UC-IAM-09 — Tạo tài khoản thủ công; UC-IAM-10 — Nhập tài khoản hàng loạt; UC-IAM-11 — Cập nhật tài khoản và role; UC-IAM-12 — Quản lý trạng thái tài khoản.

## Academic Structure and Enrollment

```mermaid
flowchart LR
    A1["Quản trị viên"]
    A2["Quản trị viên / Giảng viên / Chủ nhiệm môn"]
    A3["Quản trị viên / Giảng viên"]
    A4["Người học"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_CAT_01["UC-CAT-01 · Xem môn học"]
        UC_CAT_02["UC-CAT-02 · Tạo môn học"]
        UC_CAT_03["UC-CAT-03 · Cập nhật môn học"]
        UC_CAT_04["UC-CAT-04 · Phân công Chủ nhiệm môn"]
        UC_CAT_05["UC-CAT-05 · Xem lớp học"]
        UC_CAT_06["UC-CAT-06 · Tạo lớp học"]
        UC_CAT_07["UC-CAT-07 · Cập nhật lớp học"]
        UC_CAT_08["UC-CAT-08 · Phân công giảng viên chính"]
        UC_CAT_09["UC-CAT-09 · Quản lý vòng đời lớp"]
        UC_CAT_10["UC-CAT-10 · Xem danh sách học viên"]
        UC_CAT_11["UC-CAT-11 · Ghi danh học viên"]
        UC_CAT_12["UC-CAT-12 · Gỡ học viên khỏi lớp"]
        UC_CAT_13["UC-CAT-13 · Tự ghi danh bằng mã mời"]
    end
    A1 --> UC_CAT_01
    A1 --> UC_CAT_02
    A1 --> UC_CAT_03
    A1 --> UC_CAT_04
    A2 --> UC_CAT_05
    A1 --> UC_CAT_06
    A3 --> UC_CAT_07
    A1 --> UC_CAT_08
    A3 --> UC_CAT_09
    A3 --> UC_CAT_10
    A3 --> UC_CAT_11
    A3 --> UC_CAT_12
    A4 --> UC_CAT_13
```

**Diễn giải bằng chữ:** UC-CAT-01 — Xem môn học; UC-CAT-02 — Tạo môn học; UC-CAT-03 — Cập nhật môn học; UC-CAT-04 — Phân công Chủ nhiệm môn; UC-CAT-05 — Xem lớp học; UC-CAT-06 — Tạo lớp học; UC-CAT-07 — Cập nhật lớp học; UC-CAT-08 — Phân công giảng viên chính; UC-CAT-09 — Quản lý vòng đời lớp; UC-CAT-10 — Xem danh sách học viên; UC-CAT-11 — Ghi danh học viên; UC-CAT-12 — Gỡ học viên khỏi lớp; UC-CAT-13 — Tự ghi danh bằng mã mời.

## Learning Content and RAG

```mermaid
flowchart LR
    A1["Chủ nhiệm môn"]
    A2["Giảng viên"]
    A3["Người học"]
    A4["Người học / Giảng viên"]
    A5["Giảng viên / Chủ nhiệm môn"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_CNT_01["UC-CNT-01 · Quản lý học liệu và RAG cấp môn"]
        UC_CNT_02["UC-CNT-02 · Quản lý nội dung lớp"]
        UC_CNT_03["UC-CNT-03 · Xuất bản nội dung lớp"]
        UC_CNT_04["UC-CNT-04 · Truy cập bài học"]
        UC_CNT_06["UC-CNT-06 · Đăng thông báo lớp"]
        UC_CNT_07["UC-CNT-07 · Trao đổi hỏi đáp trong lớp"]
        UC_CNT_08["UC-CNT-08 · Dùng YouTube làm nguồn RAG theo bài giảng"]
    end
    A1 --> UC_CNT_01
    A2 --> UC_CNT_02
    A2 --> UC_CNT_03
    A3 --> UC_CNT_04
    A2 --> UC_CNT_06
    A4 --> UC_CNT_07
    A5 --> UC_CNT_08
```

**Diễn giải bằng chữ:** UC-CNT-01 — Quản lý học liệu và RAG cấp môn; UC-CNT-02 — Quản lý nội dung lớp; UC-CNT-03 — Xuất bản nội dung lớp; UC-CNT-04 — Truy cập bài học; UC-CNT-06 — Đăng thông báo lớp; UC-CNT-07 — Trao đổi hỏi đáp trong lớp; UC-CNT-08 — Dùng YouTube làm nguồn RAG theo bài giảng.

## Group Management and Group Assignment

```mermaid
flowchart LR
    A1["Người học / Giảng viên"]
    A2["Giảng viên"]
    A3["Người học"]
    A4["Người học (trưởng nhóm) / Giảng viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_GRP_01["UC-GRP-01 · Xem thông tin nhóm"]
        UC_GRP_02["UC-GRP-02 · Quản lý nhóm và trưởng nhóm"]
        UC_GRP_03["UC-GRP-03 · Gửi yêu cầu đổi trưởng nhóm"]
        UC_GRP_04["UC-GRP-04 · Xử lý yêu cầu đổi trưởng nhóm"]
        UC_GRP_05["UC-GRP-05 · Soạn khung mục việc cho bài nhóm"]
        UC_GRP_06["UC-GRP-06 · Nhận và làm mục trong tài liệu nhóm"]
        UC_GRP_07["UC-GRP-07 · Theo dõi tài liệu chung và nộp bài nhóm"]
        UC_GRP_08["UC-GRP-08 · Đối chiếu và chấm tài liệu nhóm"]
    end
    A1 --> UC_GRP_01
    A2 --> UC_GRP_02
    A3 --> UC_GRP_03
    A2 --> UC_GRP_04
    A2 --> UC_GRP_05
    A3 --> UC_GRP_06
    A4 --> UC_GRP_07
    A2 --> UC_GRP_08
```

**Diễn giải bằng chữ:** UC-GRP-01 — Xem thông tin nhóm; UC-GRP-02 — Quản lý nhóm và trưởng nhóm; UC-GRP-03 — Gửi yêu cầu đổi trưởng nhóm; UC-GRP-04 — Xử lý yêu cầu đổi trưởng nhóm; UC-GRP-05 — Soạn khung mục việc cho bài nhóm; UC-GRP-06 — Nhận và làm mục trong tài liệu nhóm; UC-GRP-07 — Theo dõi tài liệu chung và nộp bài nhóm; UC-GRP-08 — Đối chiếu và chấm tài liệu nhóm.

## Learning Journey

```mermaid
flowchart LR
    A1["Người học"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_LRN_01["UC-LRN-01 · Xem tổng quan học tập"]
        UC_LRN_02["UC-LRN-02 · Truy cập lớp đã ghi danh"]
    end
    A1 --> UC_LRN_01
    A1 --> UC_LRN_02
```

**Diễn giải bằng chữ:** UC-LRN-01 — Xem tổng quan học tập; UC-LRN-02 — Truy cập lớp đã ghi danh.

## Question and Rubric Bank

```mermaid
flowchart LR
    A1["Giảng viên / Chủ nhiệm môn"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_QBK_01["UC-QBK-01 · Quản lý ngân hàng rubric"]
        UC_QBK_02["UC-QBK-02 · Quản lý ngân hàng câu hỏi"]
    end
    A1 --> UC_QBK_01
    A1 --> UC_QBK_02
```

**Diễn giải bằng chữ:** UC-QBK-01 — Quản lý ngân hàng rubric; UC-QBK-02 — Quản lý ngân hàng câu hỏi.

## AI-Assisted Authoring and Administration

```mermaid
flowchart LR
    A1["Giảng viên"]
    A2["Chủ nhiệm môn"]
    A3["Quản trị viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_AIG_01["UC-AIG-01 · Tạo và duyệt bản nháp assignment cấp lớp bằng AI"]
        UC_AIG_02["UC-AIG-02 · Tạo bản nháp template/câu hỏi cấp môn bằng AI"]
        UC_AIG_03["UC-AIG-03 · Quản lý và giám sát AI"]
    end
    A1 --> UC_AIG_01
    A2 --> UC_AIG_02
    A3 --> UC_AIG_03
```

**Diễn giải bằng chữ:** UC-AIG-01 — Tạo và duyệt bản nháp assignment cấp lớp bằng AI; UC-AIG-02 — Tạo bản nháp template/câu hỏi cấp môn bằng AI; UC-AIG-03 — Quản lý và giám sát AI.

## Assignment Authoring and Submission

```mermaid
flowchart LR
    A1["Giảng viên / Chủ nhiệm môn"]
    A2["Giảng viên"]
    A3["Người học"]
    A4["Chủ nhiệm môn / Giảng viên"]
    A5["Người học / Giảng viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_ASM_01["UC-ASM-01 · Xem assignment quản lý"]
        UC_ASM_02["UC-ASM-02 · Soạn bài viết luận"]
        UC_ASM_03["UC-ASM-03 · Soạn bài trắc nghiệm"]
        UC_ASM_04["UC-ASM-04 · Soạn bài tài liệu (DOCUMENT) có sơ đồ Draw.io"]
        UC_ASM_05["UC-ASM-05 · Soạn và kiểm thử Code Lab"]
        UC_ASM_06["UC-ASM-06 · Soạn bài tập nhóm"]
        UC_ASM_07["UC-ASM-07 · Duyệt và phát hành assignment cho lớp"]
        UC_ASM_09["UC-ASM-09 · Xem assignment được giao"]
        UC_ASM_10["UC-ASM-10 · Làm và nộp bài viết luận"]
        UC_ASM_11["UC-ASM-11 · Làm và nộp bài trắc nghiệm"]
        UC_ASM_12["UC-ASM-12 · Làm và nộp bài tài liệu có sơ đồ Draw.io"]
        UC_ASM_13["UC-ASM-13 · Làm và nộp bài Code Lab"]
        UC_ASM_14["UC-ASM-14 · Xem lịch sử và nộp lại assignment"]
        UC_ASM_15["UC-ASM-15 · Quản lý vòng đời assignment"]
        UC_ASM_16["UC-ASM-16 · Phát hành và copy template đề cấp môn"]
        UC_ASM_17["UC-ASM-17 · Copy assignment và rubric giữa lớp"]
        UC_ASM_18["UC-ASM-18 · Cấu hình và làm simulation exam"]
    end
    A1 --> UC_ASM_01
    A1 --> UC_ASM_02
    A1 --> UC_ASM_03
    A1 --> UC_ASM_04
    A1 --> UC_ASM_05
    A2 --> UC_ASM_06
    A2 --> UC_ASM_07
    A3 --> UC_ASM_09
    A3 --> UC_ASM_10
    A3 --> UC_ASM_11
    A3 --> UC_ASM_12
    A3 --> UC_ASM_13
    A3 --> UC_ASM_14
    A1 --> UC_ASM_15
    A4 --> UC_ASM_16
    A2 --> UC_ASM_17
    A5 --> UC_ASM_18
```

**Diễn giải bằng chữ:** UC-ASM-01 — Xem assignment quản lý; UC-ASM-02 — Soạn bài viết luận; UC-ASM-03 — Soạn bài trắc nghiệm; UC-ASM-04 — Soạn bài tài liệu (DOCUMENT) có sơ đồ Draw.io; UC-ASM-05 — Soạn và kiểm thử Code Lab; UC-ASM-06 — Soạn bài tập nhóm; UC-ASM-07 — Duyệt và phát hành assignment cho lớp; UC-ASM-09 — Xem assignment được giao; UC-ASM-10 — Làm và nộp bài viết luận; UC-ASM-11 — Làm và nộp bài trắc nghiệm; UC-ASM-12 — Làm và nộp bài tài liệu có sơ đồ Draw.io; UC-ASM-13 — Làm và nộp bài Code Lab; UC-ASM-14 — Xem lịch sử và nộp lại assignment; UC-ASM-15 — Quản lý vòng đời assignment; UC-ASM-16 — Phát hành và copy template đề cấp môn; UC-ASM-17 — Copy assignment và rubric giữa lớp; UC-ASM-18 — Cấu hình và làm simulation exam.

## Grading and Feedback

```mermaid
flowchart LR
    A1["Giảng viên"]
    A2["Người học"]
    A3["Giảng viên / Quản trị viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_GRD_01["UC-GRD-01 · Xem và review bài nộp"]
        UC_GRD_02["UC-GRD-02 · Chấm bài thủ công"]
        UC_GRD_03["UC-GRD-03 · Chấm bài với AI hỗ trợ"]
        UC_GRD_04["UC-GRD-04 · Chốt và công bố điểm"]
        UC_GRD_05["UC-GRD-05 · Chốt điểm hàng loạt"]
        UC_GRD_06["UC-GRD-06 · Xem điểm và phản hồi cá nhân"]
        UC_GRD_07["UC-GRD-07 · Xem sổ điểm và lịch sử điểm"]
    end
    A1 --> UC_GRD_01
    A1 --> UC_GRD_02
    A1 --> UC_GRD_03
    A1 --> UC_GRD_04
    A1 --> UC_GRD_05
    A2 --> UC_GRD_06
    A3 --> UC_GRD_07
```

**Diễn giải bằng chữ:** UC-GRD-01 — Xem và review bài nộp; UC-GRD-02 — Chấm bài thủ công; UC-GRD-03 — Chấm bài với AI hỗ trợ; UC-GRD-04 — Chốt và công bố điểm; UC-GRD-05 — Chốt điểm hàng loạt; UC-GRD-06 — Xem điểm và phản hồi cá nhân; UC-GRD-07 — Xem sổ điểm và lịch sử điểm.

## Reporting and Analytics

```mermaid
flowchart LR
    A1["Giảng viên"]
    A2["Người học"]
    A3["Giảng viên / Quản trị viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_RPT_01["UC-RPT-01 · Theo dõi tình trạng nộp bài"]
        UC_RPT_02["UC-RPT-02 · Xem dashboard kết quả cá nhân"]
        UC_RPT_03["UC-RPT-03 · Xuất bảng điểm"]
    end
    A1 --> UC_RPT_01
    A2 --> UC_RPT_02
    A3 --> UC_RPT_03
```

**Diễn giải bằng chữ:** UC-RPT-01 — Theo dõi tình trạng nộp bài; UC-RPT-02 — Xem dashboard kết quả cá nhân; UC-RPT-03 — Xuất bảng điểm.

## Payment

```mermaid
flowchart LR
    A1["Người dùng"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_PAY_01["UC-PAY-01 · Mua credit AI"]
    end
    A1 --> UC_PAY_01
```

**Diễn giải bằng chữ:** UC-PAY-01 — Mua credit AI; khi thiếu webhook, hệ thống tự xác minh trạng thái qua PayOS.

## Notification and Audit

```mermaid
flowchart LR
    A1["Người dùng"]
    A2["Quản trị viên"]
    subgraph SYSTEM["AI-Powered Learning Platform"]
        UC_OPS_01["UC-OPS-01 · Nhận và xem thông báo"]
        UC_OPS_02["UC-OPS-02 · Xem nhật ký audit"]
    end
    A1 --> UC_OPS_01
    A2 --> UC_OPS_02
```

**Diễn giải bằng chữ:** UC-OPS-01 — Nhận và xem thông báo; UC-OPS-02 — Xem nhật ký audit.

## Kiểm soát phạm vi

- 77 UC thuộc một MVP; không có catalog Phase 2.
- AI chỉ tạo bản nháp hoặc đề xuất; giảng viên chốt điểm cuối.
- Không có use case hoàn tiền sau giao dịch đã trả vì chính sách chưa chốt.
- Screen flow theo vai trò xem tại [screen-flow.md](screen-flow.md).
