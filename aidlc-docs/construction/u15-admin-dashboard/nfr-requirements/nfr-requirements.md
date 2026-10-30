# U15 Admin Dashboard — nfr-requirements

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-01 | `GET /api/v1/admin/dashboard` p95 ≤ 1 giây; mỗi nguồn timeout 2 giây, gọi song song. | NFR-003 |
| NFR-U15-02 | Không cache số liệu. | BR-U15-11 |
| NFR-U15-30 | Chỉ `ADMIN`; response chỉ có số đếm và metadata backup đã làm sạch. | SEC-002, SEC-005 |
| NFR-U15-40 | Người code viết unit test cho mọi `BR-U15-xx`; tester viết integration test gồm vai trò khác gọi API, một nguồn lỗi mà các thẻ khác vẫn hiện, U03 lỗi không hiện thành công. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-08 | Compliant | Chỉ `ADMIN`, kiểm phía server |
| SECURITY-03 | Compliant | Không có dữ liệu cá nhân trong response, log |
| SECURITY-15 | Compliant | Nguồn lỗi hiện "không khả dụng" |
| Rule còn lại | N/A | Không có dữ liệu riêng, không gọi dịch vụ ngoài |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
