# U09 Assignment Grading — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U09-01 | Submission Detail p95 ≤ 500 ms với lớp 200 sinh viên. | NFR-003 |
| NFR-U09-02 | Sổ điểm p95 ≤ 1 giây với 200 sinh viên và 30 bài. | NFR-003 |
| NFR-U09-03 | Xuất bảng điểm 200 sinh viên × 30 bài trong ≤ 10 giây, ghi dạng luồng. | US-RPT-003 |
| NFR-U09-04 | Chốt, công bố hàng loạt tối đa 200 mục mỗi yêu cầu, p95 ≤ 2 giây. | FR-020 |
| NFR-U09-10 | Khóa lạc quan `version` cho đánh giá; chốt, công bố là câu lệnh có điều kiện trạng thái; mỗi mục một transaction để mục lỗi không ảnh hưởng mục khác. | BR-U09-40, 41 |
| NFR-U09-11 | Tạo đánh giá chạy trong transaction nộp của U08; `grade.published` phát sau commit. | BR-U03-64 |
| NFR-U09-12 | Unique: đánh giá theo `attemptId`; theo (`groupDocumentId`, `accountId`) với `MEMBER`; đề xuất theo `requestRef`. | BR-U09-10 |
| NFR-U09-30 | CSV chặn công thức (giá trị bắt đầu bằng `=`, `+`, `-`, `@` được thêm dấu nháy); tên tệp không chứa dữ liệu cá nhân. | SEC-003 |
| NFR-U09-31 | Không log nội dung bài nộp, nhận xét, đề xuất AI. | SEC-005 |
| NFR-U09-32 | Prompt AI không có tên, email người học. | FR-008 |
| NFR-U09-40 | Người code viết unit test cho mọi `BR-U09-xx`; tester viết integration test, gồm hai giảng viên lưu cùng đánh giá, chốt hàng loạt có mục lỗi, nộp lượt mới sau khi đã chấm, đề xuất AI với hash cũ, xuất ngoài quyền, phân bố dưới ngưỡng, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U09-31 |
| SECURITY-05 | Compliant | Kiểm mục rubric, điểm, lý do, bộ lọc xuất |
| SECURITY-08 | Compliant | Chỉ giảng viên chính của lớp; người học chỉ điểm của mình |
| SECURITY-11 | Compliant | NFR-U09-30, 32; ngưỡng ẩn danh của phân bố |
| SECURITY-13 | Compliant | Lịch sử điểm chỉ thêm; đề xuất gắn hash bài nộp |
| SECURITY-15 | Compliant | Fail closed khi U01, U05, U07, U08 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài trực tiếp |
