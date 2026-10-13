# Review Project Management Plan so với AI-DLC hiện hành — 2026-10-09

## Kết luận và nguồn

Report 2 chưa đồng bộ với revision AI-DLC ngày 09/10. Cần cập nhật scope, unit ownership, iteration/gates và traceability trước khi dùng làm kế hoạch triển khai hiện hành. Review không sửa Google Doc, code, state hoặc approvals.

- Plan: [Report 2](https://docs.google.com/document/d/1tqp6bc0Xeqh5cwGrj8PzjAV4a--N_XU9/edit), đọc qua Google Drive; modifiedTime 2026-10-06T16:18:12.080Z, change log đến v1.3 ngày 06/10.
- Nguồn hiện hành: [requirements](../requirements/requirements.md), [current SRS contract](../../construction/current-srs-contract.md), [unit boundaries](../application-design/unit-of-work.md), [unit map](../application-design/unit-of-work-story-map.md), [dependencies](../application-design/unit-of-work-dependency.md), [state](../../aidlc-state.md), [View Material revision](material-summary-on-demand-2026-10-09.md).
- Ưu tiên đoạn revision mới khi tài liệu có lịch sử quyết định. Không dùng approvals baseline cũ làm bằng chứng approval hoặc triển khai revision mới.
- Kiểm tra nội dung đọc được, gồm các bảng scope/WBS/iteration/metrics/risk/gate/RACI/training/tools; không đánh giá bố cục trang hay định dạng native.

## Findings và sửa tại từng mục

| Mức | Mục Report 2 | Hiện tại | Đối chiếu và hướng sửa |
|---|---|---|---|
| P1 | I.1; I.4; I.5 metric 9; I.6 R01; Acronyms | 16 unit, 49 story, 40 UC; giữ baseline 49/40 | Đổi thành 15 unit, 51 story, 73 UC. Giữ ID U01–U09/U11–U16, không đánh số lại. Coverage phải theo primary map, không chỉ sửa tổng. |
| P1 | I.1 Scope | Invitation-code enrollment và class communication được ghi included | Bỏ mã mời; Student chỉ được Chủ nhiệm môn thêm vào lớp. Giữ announcement nhưng nêu không có comments. FR-022/023. |
| P1 | I.1 Scope; II.1; RACI; G4 | U10 Template & Copy; template/copy lineage; U03 chưa có Settings | Bỏ U10; bài của môn, copy/version thuộc U08; đổi tên U08 thành Assessment Core, Publication & Copy. U03 File, Job, Event & Settings, UC 70–71; U02 Audit UC 73; U07 có global Payment History UC 72. |
| P1 | I.1 Scope | Năm dạng bài đều GRADED/PRACTICE, trừ Group chỉ GRADED | Quiz chỉ luyện tập, gắn học liệu, không lịch và không vào Gradebook. Text Essay/Diagram Essay/Code Lab có hai mode; Group chỉ GRADED và chỉ cấp lớp. Bài của môn gồm Text Essay/Diagram Essay/Code Lab, giao mọi lớp OPEN cùng lịch; Teacher từng lớp chấm lớp mình. FR-017/027. |
| P1 | I.1 Scope; II.2 AI governance | RAG processing dùng credit uploader | Upload/tạo lesson chỉ lưu và trích chữ/phụ đề, không AI/credit. Student/Teacher/Subject Manager có quyền xem bấm tóm tắt; người bấm trả credit, một summary dùng chung, xem lại không trừ thêm. Chưa INDEXED không vào RAG. FR-004 và revision View Material. |
| P1 | I.3 iteration mapping; II.1 waves/gates | Iteration 2 cho U10 mở sớm; Iteration 3 và wave/G4 có U10 | Xóa U10 khỏi toàn bộ lịch/gate/RACI. Wave 4 chỉ U13/U16. Copy/version và bài của môn nghiệm thu trong U08/G2; bổ sung Settings vào foundation/G1 và kiểm quyền sau tích hợp U01. Critical dependency path chính còn đúng; chỉ bỏ câu U10 phụ thuộc U09. |
| P2 | I.1; II.2 authorization/testing; II.4 responsibility | Role/scope chung, chưa nêu giới hạn Admin và quyền quản trị lớp mới | Nêu Admin chỉ quản trị, không nhận phân công môn/lớp, không ví/AI hay quyền học thuật. Teacher không thêm/gỡ Student, sửa vòng đời lớp hoặc bật phân bố điểm; Chủ nhiệm môn quản lý các việc đó. Thêm negative tests vào DoD/gates. FR-002/003/024. Đây là thiếu quy định rõ, không khẳng định Doc đang cấp quyền Admin sai. |
| P2 | I.1 banks; G2; II.2 tests | Question/rubric bank và immutable bank/rubric versions | Chỉ Subject Question Bank; Teacher chọn câu ACTIVE hoặc tạo câu riêng của bài. Rubric thuộc bài, tự tạo trống theo câu Text Essay/phần Diagram/Group, điền đủ trước phát hành và khóa khi phát hành. Giữ question version/attempt snapshot, copy rubric độc lập; tránh diễn đạt rubric như ngân hàng version riêng. FR-016. |
| P2 | I.6 R02/R03; G2/G4; II.2 AI/test/DoD | Quy tắc credit/provider và test chung | Thêm kiểm: một HOLD tại yêu cầu summary, child calls không reserve thêm; settle lượng dùng thật/trả dư; message trùng/retry/worker chết/claim lease; embedding lỗi vẫn giữ summary nhưng chưa vào RAG. AI deadline 24 giờ và HOLD fallback 25 giờ tính từ yêu cầu. Các mốc này dành cho học liệu; không áp vào Student Practice AI grading có timeout riêng. |
| P2 | II.1 approval; II.6 baseline/change control | Có yêu cầu re-approval code plan nhưng chưa ghi trạng thái revision | Nêu approvals 16 plans ngày 05/10 là lịch sử baseline cũ; thiết kế 15 unit revision 09/10 chưa tự chứng minh code/contracts/migration đã đổi. Mỗi unit cần rà plan và approval hiện hành trước triển khai; integration tests do tester riêng theo state. |

## Lịch, capacity và phân công

Các phép tính trong Report 2 nhất quán: 5 người × 20 giờ × 14 tuần = 1.400 giờ; reserve 20% = 280 giờ; effective = 1.120 giờ = 140 pds. WBS 4 + 15 + 20 + 60 + 17 + 10 = 126 pds, còn 14 pds chưa gán. Mỗi iteration 20 effective pds phù hợp capacity hai tuần. Không có căn cứ chỉ từ thay đổi 16 → 15 unit để giảm effort: U08 nhận thêm trách nhiệm, U03 nhận Settings.

Giữ lịch 14 tuần và named roles làm baseline quản lý hiện có; cần re-estimate theo 51 story/73 UC và trách nhiệm mới, đặc biệt U03/U08/U09–U16. Các con số UC tăng không chứng minh effort tăng cùng tỷ lệ vì có thay đổi mức phân tách catalog. Chưa có actual effort/weekly board trong nguồn đọc, nên không kết luận team trễ hoặc lịch khả thi.

Mapping đề xuất để sửa bảng hiện có:

| Iteration | Unit | Điều chỉnh deliverable |
|---|---|---|
| 1, tuần 6–7 | U03/U02/U01/U04; U07 nếu đủ providers và capacity | Foundation, Settings ownership/contracts, quyền tài nguyên, file/job/audit và enrollment theo Chủ nhiệm môn; G1. |
| 2, tuần 8–9 | U05/U06/U07/U12/U08; U09/U13 có thể mở theo providers | Upload/extraction và summary request contract; Subject bank/rubric; payment/Settings integration; bài môn/copy/version/publication; G2. AI thật còn có thể tích hợp qua C sau, không nghiệm thu giả là AI thật. |
| 3, tuần 10–11 | Phần còn lại U09/U11/U14/U15/U13/U16 | Bốn dạng assignment + quiz luyện tập, group document, grading, AI/Code và reporting; G3/G4 theo readiness. |

Đây là đề xuất đồng bộ mapping, không cam kết lịch mới hay approval. Waves là nhóm tích hợp, không barrier; tối đa năm unit active, mỗi unit có implementer/reviewer và estimate trên weekly board. RACI bỏ U10, cập nhật U03/U08/U06 và giữ named assignments cho đến khi nhóm quyết định phân công thực thi.

## Các phần còn phù hợp

- Một Spring Boot modular monolith, Next.js, worker riêng, PostgreSQL/pgvector, Redis/RabbitMQ, Shared Drive, PayOS, Gemini/Judge0.
- Brevo/Mailpit, SSE có authorization, Docker Compose/VPS/Nginx, commit-SHA tags/Flyway và logs/healthchecks đã phù hợp nguồn mới đọc.
- AI không tự publish hoặc chốt điểm GRADED; Student Practice result riêng, ngoài Gradebook; private files, payment idempotency, PR review và traceability.
- Giới hạn tối đa năm unit và direct-provider scheduling; đường phụ thuộc dài U02/U03 → U01 → U04 → U05 hoặc U06 → U08 → U09 → U11 hoặc U14 → U15 → U16.

## Lệch nguồn trong repo

[execution-plan.md](execution-plan.md) còn các đoạn tự nhận hiện hành 40 UC/49 story/16 unit, template, simulation, payment entitlement nối access control và multi-zone. Một số phần ghi historical rõ; các phần còn lại không nên dùng làm authoritative cho revision 09/10. Cần đồng bộ riêng để Report 2 không bị kéo về baseline cũ. State/current contract/unit map cùng requirement revision mới là cơ sở review này.

## Thứ tự cập nhật đề xuất

1. Chốt scope 73/51/15 và sửa I.1, acronym, metric 9, R01, tên/ownership units.
2. Sửa iteration, waves/gates/RACI; bỏ U10, bổ sung Settings và bài môn/copy/version.
3. Sửa credit học liệu, authorization/bank/quiz và test acceptance tại gate; re-estimate weekly board.
4. Ghi change log mới với nội dung và người sửa thực tế; giữ version/approvals lịch sử, cập nhật links baseline và đồng bộ execution-plan riêng.

Review đọc tài liệu, không kiểm runtime hoặc xác nhận gate đã đạt. Security authorization/privacy/validation và bounded resiliency được đưa vào đề xuất kiểm chứng; không có thay đổi code để chứng nhận compliance runtime. Property-Based Testing disabled theo state. Markdown/relative links của báo cáo được kiểm trước handoff.
