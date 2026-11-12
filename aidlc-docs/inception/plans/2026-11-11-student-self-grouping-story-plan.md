# Sinh viên tự chia nhóm — đánh giá và kế hoạch User Stories

Dựa trên [yêu cầu SG-01…09](../requirements/2026-11-11-student-self-grouping.md), đã duyệt 2026-11-11.

## Đánh giá

- **Yêu cầu gốc**: sinh viên tự chia nhóm trong lớp, giảng viên duyệt.
- **Ảnh hưởng người dùng**: trực tiếp, hai vai trò (sinh viên, giảng viên).
- **Độ phức tạp**: vừa; nhiều trạng thái (nháp, chờ duyệt, đã duyệt, từ chối, lời mời) và nhiều tình huống biên.
- **Tiêu chí High Priority**: tính năng mới người dùng thao tác trực tiếp; nhiều persona; nhiều kịch bản nghiệp vụ.
- **Quyết định**: chạy User Stories ở độ sâu vừa.
- **Lợi ích**: acceptance criteria Given/When/Then làm căn cứ cho test backend, frontend và E2E; truy vết UC/FR như các story US-GRP hiện có.

## Cách chia story đề xuất

Theo hành trình người dùng, mỗi story là một bước hoàn chỉnh của một vai trò:

| Story | Vai trò | Nội dung | Yêu cầu |
|---|---|---|---|
| US-GRP-007 | Giảng viên | Bật/tắt đợt, sĩ số, hạn chót | SG-01 |
| US-GRP-008 | Sinh viên | Tạo đề xuất, mời, chấp nhận/từ chối lời mời, rời, hủy | SG-02, SG-08 |
| US-GRP-009 | Sinh viên (trưởng nhóm) | Gửi duyệt, rút về nháp | SG-03, SG-08 |
| US-GRP-010 | Giảng viên | Duyệt/từ chối, duyệt nhiều, xem sinh viên chưa có nhóm, sửa tay khi đợt mở | SG-04…07, SG-08 |

Các cách khác: theo tính năng (một story lớn "Đề xuất nhóm") khó test riêng; theo epic thì quá nặng cho bốn story.

## Câu hỏi

## Question 1
Chia story theo cách nào?

A) Bốn story theo hành trình như bảng trên (đề xuất)

B) Gộp thành hai story: một cho sinh viên, một cho giảng viên

C) Một story mỗi yêu cầu SG (9 story)

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Ghi story ở đâu?

A) Thêm vào `user-stories/stories.md` ngay sau US-GRP-006, cùng định dạng (Primary unit, UC trace, Truy vết, Scenario Given/When/Then) và thêm dòng vào bảng story–unit (đề xuất)

B) Tệp riêng trong `user-stories/`, `stories.md` chỉ trỏ tới

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Persona?

A) Dùng lại P-STUDENT và P-TEACHER, thêm một dòng trách nhiệm tự chia nhóm vào `personas.md`; không persona mới (đề xuất)

B) Thêm persona riêng "trưởng nhóm đề xuất"

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Mức chi tiết acceptance criteria?

A) Mỗi story 2–4 scenario Given/When/Then: luồng chính, lỗi quyền/trạng thái, tình huống biên chính (đề xuất)

B) Liệt kê mọi tình huống biên của SG-01…09 thành scenario

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Các bước thực hiện (sau khi duyệt)

- [x] Viết US-GRP-007…010 vào nơi đã chọn ở Q2, theo cách chia ở Q1, mức chi tiết ở Q4
- [x] Mỗi story có truy vết UC 54/55/57, FR-025, SG tương ứng và yêu cầu bảo mật liên quan
- [x] Cập nhật persona theo Q3 và gắn persona với story
- [x] Thêm dòng vào bảng story–unit (U12, MVP)
- [x] Kiểm INVEST: độc lập, có giá trị, ước lượng được, nhỏ, test được
- [x] Kiểm liên kết Markdown của các tệp đã sửa
