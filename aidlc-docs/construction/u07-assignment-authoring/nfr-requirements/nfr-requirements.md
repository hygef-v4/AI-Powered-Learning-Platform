# U07 Assignment Authoring — nfr-requirements

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U07-01 | Lưu khung ≤ 2.000 block (kiểm, tính phần, đồng bộ rubric) p95 ≤ 500 ms. | NFR-003 |
| NFR-U07-02 | API danh sách bài p95 ≤ 300 ms, trang ≤ 100. | NFR-003 |
| NFR-U07-03 | Phát hành (kiểm, khóa rubric, ghi lịch) trong một transaction p95 ≤ 1 giây. | BR-U07-33 |
| NFR-U07-04 | Mở, đóng theo lịch trễ ≤ 1 phút. | BR-U07-34 |
| NFR-U07-05 | Nhập DOCX ≤ 20 MB xong trong ≤ 15 giây; xuất DOCX có tới 100 sơ đồ trong ≤ 15 giây. | BR-U07-60, 61 |
| NFR-U07-10 | Khóa lạc quan `version` cho bài và rubric; phát hành, ngưng giao, mở, đóng là câu lệnh có điều kiện trạng thái. | BR-U07-70 |
| NFR-U07-11 | Listener `onOpened`, `onRetired` chạy trong cùng transaction; scanner chạy lại không tạo trùng. | BR-U03-64 |
| NFR-U07-30 | XML Draw.io qua parser an toàn; SVG xem trước được làm sạch (bỏ script, thuộc tính sự kiện, tham chiếu ngoài). | SEC-003 |
| NFR-U07-31 | DOCX đọc trong bộ nhớ, chặn file nén bất thường (số mục, tổng dung lượng giải nén ≤ 100 MB); ảnh đi qua kiểm loại của U03. | SEC-003 |
| NFR-U07-32 | Draw.io chỉ nhúng từ `embed.diagrams.net` (CSP `frame-src`), kiểm origin của mọi message. | SEC-003 |
| NFR-U07-33 | Không log nội dung đề, hướng dẫn, rubric, prompt. | SEC-005 |
| NFR-U07-40 | Người code viết unit test cho mọi `BR-U07-xx`; tester viết integration test, gồm phát hành song song với sửa rubric, scanner mở đúng giờ và gọi listener, copy giữa lớp ngoài quyền, nhập DOCX đã qua Word và Google Docs vẫn giữ sơ đồ, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U07-33 |
| SECURITY-05 | Compliant | Kiểm độ dài, giới hạn khung, rubric, lịch, DOCX |
| SECURITY-08 | Compliant | R2, R3/R4, R5 kiểm phía server; ngoài phạm vi trả `404` |
| SECURITY-10 | Compliant | Khóa phiên bản Apache POI, Batik, thư viện trình soạn |
| SECURITY-11 | Compliant | NFR-U07-30, 31, 32 |
| SECURITY-13 | Compliant | Rubric khóa khi phát hành; bài đã phát hành không sửa |
| SECURITY-15 | Compliant | Fail closed khi U01, U04, U05 lỗi |
| Rule còn lại | N/A | Hạ tầng mạng, xác thực, giám sát thuộc U01, U03 và hạ tầng chung |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
