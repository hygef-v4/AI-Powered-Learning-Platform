# Nhận xét Report 2 — Project Management Plan

**Ngày rà soát:** 22/11/2026
**Tệp được rà soát:** `D:\Download\Report-2_Project Management Plan (2).docx` (change log mới nhất 14/10/2026, "Working revision")
**Đối chiếu với:** [unit-of-work.md](../application-design/unit-of-work.md), [requirements.md](requirements.md), [nhận xét Report 1](2026-11-22-report-1-review.md), `infra/docker-compose.yml`, `.github/workflows/ci.yml`, `aidlc-state.md` và lịch sử Git của nhánh hiện tại.
**Phạm vi:** chỉ đọc. Không sửa report, không sửa code. Chưa đối chiếu trực tiếp Jira (KAN-18…32).

## 1. Kết luận

Report 2 nhìn chung **đúng và nhất quán nội bộ**. Các con số chính đã được kiểm lại và đều khớp:

- Tổng effort WBS: 4 + 15 + 20 + 60 + 17 + 10 = **126 pds**; mỗi iteration đúng 20 pds; từng stage cộng đúng.
- Năng lực: 5 × 20 × 14 = 1.400 giờ (175 pds); dự phòng 20% = 280 giờ; còn 1.120 giờ (140 pds); dư 14 pds.
- Training: 54 giờ/người, 270 giờ cả nhóm.
- Phạm vi 66 UC, 50 story MVP + 1 extension, 15 unit, 3 iteration × 5 unit; tên và thứ tự U01–U15 khớp unit-of-work.md.
- D01–D04 (Quiz chỉ PRACTICE, bài nhóm chỉ GRADED, không AI cho Student ở bài nhóm, quota theo ngày và trần hệ thống) khớp.
- Công nghệ: Next.js, Spring Boot modular monolith + worker, PostgreSQL 16, Redis, RabbitMQ, Nginx, Docker Compose, GitHub Actions, adapter Gemini/OpenAI/Claude, Brevo SMTP/HTTPS API, không có metrics stack: **đều khớp mã nguồn**.

Tuy vậy report dừng ở **14/10/2026** và đang ghi "Pending team review". Hiện đã là tuần 12, nên có một số chỗ mâu thuẫn số liệu, thông tin môi trường đã cũ và cột *Actual* vẫn để TBD.

| Mức | Số mục | Ý nghĩa |
|---|---|---|
| 🔴 Sai lệch / mâu thuẫn | 6 | Report mâu thuẫn với chính nó, với tài liệu AI-DLC hoặc với hệ thống đang chạy |
| 🟡 Thiếu / cũ | 6 | Thông tin đúng tại 14/10 nhưng đã cũ, hoặc còn thiếu so với kế hoạch quản lý thực tế |
| ⚪ Trình bày | 8 | Bìa, change log, bảng viết tắt, đánh số, câu chữ |

## 2. Sai lệch và mâu thuẫn cần sửa

### S1. Training 270 giờ không nằm vừa trong 14 pds còn trống

- **Vị trí:** I.4 Capacity Planning, II.3 Training Plan.
- **Report viết:** WBS dùng 126/140 pds, "leaving 14 person-days unallocated". Training "totalling 54 hours per member or 270 team hours".
- **Vấn đề:** 14 pds = 112 giờ, trong khi training cần 270 giờ (≈ 33,75 pds). Nếu training tính riêng thì tổng là 126 + 33,75 ≈ **159,75 pds, vượt 140 pds**. Report chỉ ghi "The Leader confirms their allocation within relevant work packages", nghĩa là chưa chốt.
- **Đề xuất:** chọn một cách và ghi rõ:
  1. Training đã nằm trong effort của các work package (ví dụ "Next.js, Spring Boot" nằm trong 3.1 POC). Khi đó viết: *"Training hours are included in the WBS work-package estimates and are not added to the 126 pds."*
  2. Hoặc giảm training xuống ≤ 112 giờ cả nhóm (≈ 22 giờ/người) để vừa 14 pds.
  3. Hoặc lấy một phần từ 280 giờ dự phòng 20% và ghi rõ con số.
- Hội đồng rất dễ cộng lại hai bảng này, nên đây là lỗi nên sửa đầu tiên.

### S2. "1.000 Students" mâu thuẫn với quy mô đã chốt

- **Vị trí:** I.1.1 Purpose.
- **Report viết:** "approximately 35 classes and 1,000 Students per semester".
- **Thực tế:** requirements.md (NFR quy mô) ghi *dưới 1.000 tài khoản và dưới 100 người dùng đồng thời*. 1.000 sinh viên cộng giảng viên, Subject Manager, Admin là vượt 1.000 tài khoản.
- **Đề xuất:** đổi thành *"up to about 1,000 users"*, hoặc ghi rõ con số 1.000 là bối cảnh của trường còn MVP nhắm dưới 1.000 tài khoản, dưới 100 người dùng đồng thời. Nên lấy số lớp/số sinh viên từ buổi phỏng vấn 12/11 làm nguồn.

### S3. Phân công U06 và U09 khác unit-of-work.md

- **Vị trí:** II.4, bảng "Jira code assignee".
- **Report viết:** Võ Minh Tài: U02, **U06**, U12. Khuất Quang Hưng: U05, **U09**, U15.
- **unit-of-work.md (§Phân công code):** Người 2: U02, **U09**, U12. Người 5: U05, **U06**, U15.
- **Lịch sử Git:** module `ai` (U06) có nhiều commit nhất của Võ Minh Tài và Quang Hung; module `grading` (U09) chủ yếu do Quang Hung (34 commit). Như vậy **report đúng với thực tế, unit-of-work.md mới là chỗ sai**.
- **Đề xuất:** giữ report. Sửa bảng "Phân công code" trong unit-of-work.md: đổi U06 ↔ U09 giữa Người 2 và Người 5, và ghi tên thật thay cho "Người N" cho dễ đối chiếu. Khi sửa, đối chiếu lại KAN-29 và KAN-31 trên Jira.
- **Lưu ý thêm:** commit của module `announcements` (U14) chủ yếu là của HNam1315, trong khi report giao U14 cho Võ Chiến Thắng. Số commit không bằng quyền sở hữu, nhưng nên kiểm lại assignee của KAN-30 trên Jira.

### S4. Change log v1.4 mâu thuẫn với bảng phân công

- **Vị trí:** Change Log v1.4 (09/10) và II.4.
- **Report viết:** v1.4: "Feature/unit owners remain unassigned until team planning". II.4 lại có bảng giao đủ 15 unit kèm mã KAN.
- **Vấn đề:** dòng "Working revision" ngày 14/10 không nói là đã thêm phân công.
- **Đề xuất:** ở dòng change log mới, ghi *"Added Jira code assignees for U01–U15 (KAN-18…32)."*

### S5. Iteration 3 làm thiết kế và test case cùng tuần với code

- **Vị trí:** I.3, các mục 4.3.1 và 4.3.3; rủi ro R07.
- **Report viết:** SDS v1.3 và Integration Test Cases của iteration 3 đều đến hạn **Week 11**, cùng tuần với code package. Iteration 1 và 2 làm hai mục này ở tuần đầu (Week 6, Week 8).
- **Vấn đề:** phá mẫu "thiết kế trước, code sau" mà II.1 và II.2 (Design Quality: "reviewed before implementation") cam kết. R07 cũng lặp lại "Weeks 6, 8 and 11".
- **Đề xuất:** đổi 4.3.1 và 4.3.3 sang **Week 10** và sửa R07 thành "Weeks 6, 8 and 10". Nếu cố ý để Week 11 thì thêm một câu giải thích.

### S6. Môi trường triển khai chỉ nhắc nhánh main

- **Vị trí:** II.6.3, các dòng *Continuous Integration* và *Deployment Environment*; II.6.2.
- **Report viết:** "builds commit-SHA container images after successful main checks", "Linux VPS pulls successful main CI images".
- **Thực tế:** `ci.yml` chạy và deploy cho cả `main` (production) lẫn `staging` (môi trường kiểm thử chung, `docs/runbooks/deploy-staging.md`). Nhóm đang làm theo luồng nhánh tính năng → PR vào `staging` → PR vào `main`.
- **Đề xuất:** viết lại: *"GitHub Actions builds commit-SHA images on main and staging. The VPS runs separate production (main) and staging environments, each behind Nginx HTTPS with health-gated deployment and automatic rollback."* Ở II.6.2 thêm bước merge qua `staging` trước `main`.

## 3. Thông tin cũ hoặc còn thiếu

| # | Vấn đề | Vị trí | Đề xuất |
|---|---|---|---|
| T1 | **File storage** ghi "Organizational Google Shared Drive". Từ 16/11 (FR-013 as-built) hệ thống hỗ trợ Shared Drive qua service account hoặc Drive cá nhân qua OAuth `drive.file`, và theo kiểm tra staging thì production đang lưu trên đĩa VPS (cùng vấn đề S3 của Report 1). | II.6.3 | Kiểm cấu hình production rồi ghi: *"Google Drive (Shared Drive or OAuth personal Drive); local disk for development"*. |
| T2 | **Cột Actual toàn TBD**, trong khi đã có số đo thật: backend 2.570 test, 0 lỗi; frontend Vitest 1.160/1.160; CI staging xanh 4 job; 66 UC đã đối chiếu tĩnh với 50 controller/346 mapping. | I.5 | Điền Actual cho các chỉ số đã đo (3, 5, 6, 9, 12) và ghi ngày đo. Chỉ để TBD những mục chưa đo. |
| T3 | **Chỉ số 4 "Business-rule unit-test coverage ≥ 70%"**: dự án không có JaCoCo; cách tính trong report dựa trên danh mục BR, nhưng report chưa nói danh mục ở đâu. | I.5 | Dẫn nguồn danh mục: `doc-code-traceability.md` và các `business-rules.md` từng unit. Ghi rõ đây không phải line coverage. |
| T4 | **Phạm vi I.1.2** chưa có các tính năng đã duyệt sau 14/10: nhập Excel `.xlsx`, nhập đề từ ảnh/PDF, sinh viên tự đề xuất nhóm, đánh giá đóng góp 5 mức, MSSV, học kỳ lớp, Đóng nhận bài, giao diện 3 ngôn ngữ (xem T1–T12 trong nhận xét Report 1). | I.1.2, R01 | Không cần liệt kê hết, nhưng thêm một câu: *"Approved changes after the baseline are tracked as change requests in aidlc-docs and Jira"*, rồi ghi lại tác động effort theo R01. Nếu không thì R01 tự mâu thuẫn ("check changes… before approval"). |
| T5 | **Chưa có bảng tiến độ thực tế.** Đã qua demo iteration 1–3 (cuối tuần 7, 9, 11) nhưng report không ghi gate G1–G3 đã đạt hay chưa. | I.3, II.1 Integration Gates | Thêm cột "Status / Date passed" cho G1–G4, hoặc dẫn tới Jira. |
| T6 | **R02 nhắc "fallback demonstration plan"** nhưng không nói kế hoạch đó ở đâu. | I.6 | Dẫn tới runbook hoặc tài liệu demo. Nếu chưa có thì thêm vào WBS 5.2 hoặc 6.3. |

## 4. Trình bày và định dạng

1. **Ngày trên bìa** là "Hanoi, September 2026" trong khi bản mới nhất là 14/10. Nếu nộp lại thì đổi ngày và thêm dòng change log.
2. **Dòng change log 14/10 không có số phiên bản** ("Working revision") và vẫn "Pending team review". Nên đặt số **v1.5** (hoặc v2.0 nếu đây là bản duyệt mốc) và bỏ chữ "pending" khi đã duyệt. Quy ước ở II.6.1 yêu cầu mỗi bản duyệt có số phiên bản.
3. **Bảng viết tắt:**
   - Định nghĩa vòng tròn: "G1-G4 | G1-G4 integration checkpoints". Nên viết: *"Integration gates G1 (Foundation) to G4 (Real Providers and Release)"*.
   - Chưa sắp theo bảng chữ cái: **MCP** nằm cuối bảng.
   - Có dùng nhưng chưa định nghĩa: **API, LMS, UI/UX, SMTP, HTTPS, VPS, PR, OTP, KAN, SEP490**.
   - "pds" nên viết thống nhất (pds / person-days), report dùng cả hai.
4. **Đánh số WBS:** dòng "2." có dấu chấm, các stage khác không có.
5. **Thứ tự trong Stage 2:** mục 2.3 (Week 2) đứng sau 2.1 và 2.2 (Week 3). Nên xếp theo hạn.
6. **"U01-U15 | The 15 current logical AI-DLC units…"**: chữ "current" sẽ cũ đi. Nên bỏ.
7. **Tên unit trong report dài hơn unit-of-work.md** (ví dụ "U03 File, Job, Settings & Backup" so với "Job and Settings", "U06 AI Connections & MCP", "U09 Assignment Grading & Gradebook", "U10 Quiz Authoring & Bank"). Tên trong report mô tả đúng hơn, nhưng nên thống nhất với tài liệu AI-DLC và Jira.
8. **Câu khó đọc:** đoạn II.5 "separates aidlc-current progress from aidlc-historical work" dùng nhãn Jira nội bộ. Nên viết: *"Jira labels separate work on the current baseline from historical work; historical Done issues do not prove the current baseline is complete."*

## 5. Các điểm đã kiểm và khớp

- Tổng effort, năng lực, buffer, training: phép tính đúng (chỉ có vấn đề chỗ nào chứa training, xem S1).
- RACI: mỗi dòng có đúng một A và ít nhất một R, đúng như câu mô tả. Mỗi thành viên code đúng một unit mỗi iteration, phủ đủ U01–U15.
- Integration gate G1–G3 khớp unit-of-work.md; G4 có trong Jira (tạo cùng D01–D04 theo audit).
- Phạm vi ngoài: không có mobile native, multi-tenancy, grade appeal, similarity checking, khớp requirements.md.
- Không có thanh toán/credit wallet, AI không tự công bố đề hay điểm, phản hồi Practice riêng tư: khớp quyết định đã chốt.
- PostgreSQL 16, Redis 7.4, RabbitMQ 4.1, Nginx 1.28 (image có pin digest) trong `infra/docker-compose.yml`; Flyway; GitHub Actions; health check và tự rollback; không có Prometheus/Grafana/Micrometer.
- Adapter AI Gemini, OpenAI, Claude có trong `application.yml` (U06).
- Commit đầu tiên ngày 12/09/2026, khớp lịch tuần 1–2.

## 6. Việc nên làm tiếp

1. Sửa S1 (training/năng lực) và S5 (tuần của iteration 3) vì đây là lỗi số liệu hội đồng dễ phát hiện.
2. Sửa S2, S4, S6 và T1. T1 cần kiểm `.env` production trước khi viết.
3. Điền cột Actual (T2) và trạng thái gate (T5) bằng số đo đã có.
4. Sửa các mục trình bày ở phần 4, đặt số phiên bản và thêm dòng change log cho lần sửa này.
5. Ngoài report: sửa bảng "Phân công code" trong unit-of-work.md theo S3, và kiểm assignee KAN-29/30/31 trên Jira.
