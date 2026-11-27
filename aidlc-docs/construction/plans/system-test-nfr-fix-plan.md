# Kế hoạch sửa case Fail của system test NFR (Report-5.4, 2026-11-26)

Phạm vi: 15 case Fail của sheet "Report-5.4_System Test_NFRs" (Security 036, 048, 050, 051, 052, 053; Performance 001, 002, 004, 005, 009, 011, 013, 019, 020). Quyết định của người dùng: [system-test-nfr-fix-questions.md](system-test-nfr-fix-questions.md).

Nhánh: `system-test-nfr` (đã merge `origin/staging` ở 5bec14c8). Không commit hoặc push khi người dùng chưa yêu cầu. Bug Jira: project KAN, loại Bug, nhãn `system-test` và `nfr`, assign Hải Nam; mỗi lỗi một Bug, ghi Defect ID vào cột L của sheet.

Áp dụng [code-conventions.md](../code-conventions.md) và [persistence-conventions.md](../persistence-conventions.md) (Spring JDBC, không thêm JPA). Không đổi API, JSON, schema hay event, trừ khi ghi rõ dưới đây.

## Bước 1. Sửa test case và BR (không đổi code)

- [x] SEC-048: kết quả mong đợi "sau 30 lần sai, lần tiếp theo trả 401 trung tính và không kiểm mật khẩu"; đánh dấu Pass (bằng chứng cũ).
- [x] SEC-050: bỏ "and audited" trong kết quả mong đợi; đánh dấu Pass. Sửa BR-U07-70 cho khớp BR-U02-10 (bỏ "ghi audit `ACCESS_DENIED`").

## Bước 2. Nginx (SEC-036, 052, 053)

- [x] `server_tokens off` ở mọi server block.
- [x] `log_format` ẩn token trong `/api/v1/files/download/<token>` của access log.
- [x] Nâng image nginx lên bản vá mới nhất của nhánh stable, cập nhật `infra/images.lock.json` và qua `scripts/check-image-pins.mjs`.
- [x] ~~Thêm HSTS cho các location tĩnh đang thiếu (`/_next/static/*`, font, `robots.txt`, `sitemap.xml`, 404 của `/api`).~~ Không làm: template staging cố ý không gửi HSTS (ghi chú trong template); production đã có HSTS ở server block. Ghi nhận là Low có giải thích.
- [ ] Kiểm tra: `nginx -t` trong container, build lại nginx systest, chạy lại SEC-036 (tìm token trong log), SEC-053 (ZAP active trên systest). SEC-052 (ZAP baseline staging) chạy lại sau khi bản sửa lên staging.

## Bước 3. Cấu hình chung (PERF-001, 004, 005, 020)

- [x] Hikari: `maximum-pool-size: ${DB_POOL_MAX:20}` cho backend, worker 10 (profile worker); thêm vào `infra/.env.example` và compose; ghi NFR/BR liên quan.
- [x] `ScheduledScanRunner`: `fixedDelayString = "${U03_SCAN_DELAY_MS:15000}"`; sửa BR/NFR U03; test.

## Bước 4. Code chậm

- [x] PERF-009 (U09): `GradebookService.loadGradebook` đọc toàn bộ `grading_document` JSONB của mọi evaluation trong lớp (6.000 bản) rồi parse bằng Jackson. Thay bằng truy vấn chỉ lấy các trường bảng điểm cần, lọc `SUPERSEDED` và `GROUP_DOCUMENT` trong SQL. Giữ nguyên kết quả API; thêm test so sánh.
- [x] PERF-011 (U09): `GradingBatchService.executeBatch` chạy 200 giao dịch riêng, mỗi giao dịch kiểm quyền và đọc lại dữ liệu chung. Đo một item, bỏ phần kiểm và đọc lặp lại cho cả lô, giữ nguyên "lỗi một item không hoàn tác item khác".
- [x] PERF-013 (U05): `enrollStudents` khoá lại lớp và tài khoản ở từng dòng (200 giao dịch). Đo rồi giảm phần lặp lại, giữ quy tắc khoá của BR-U05.
- [ ] PERF-004 (U08), PERF-005 (U12): đo lại sau bước 3; nếu vẫn chậm thì xem kích thước body autosave và truy vấn mở tài liệu nhóm và nhận phần.
- [x] PERF-002 (U01): đo thời gian bcrypt trong container và tìm chỗ nghẽn khác; giới hạn số bcrypt chạy cùng lúc theo số CPU để request thừa xếp hàng ngắn thay vì timeout 60 s; đề xuất mục tiêu mới theo tốc độ đo được để người dùng chốt.
- [x] PERF-019 (U12): `GroupSectionEditorPage` chỉ hỏi "Leave site?" mà không lưu. Lưu bằng `keepalive` khi `pagehide`, như `AssignmentWorkspacePage` (body ≤ 64 KB, lớn hơn thì lưu thường); test frontend.

## Bước 5. Kiểm tra

- [ ] Backend: test các package đã sửa và toàn bộ `mvn verify`; frontend: test liên quan, typecheck, lint.
- [ ] Build lại systest (backend, worker, frontend, nginx) và chạy lại các case đã sửa với cùng script và giới hạn CPU, lưu bằng chứng vào "Performance - Round 2" và "Security - Round 3".
- [ ] SEC-051: sau khi người dùng đặt khoá trên VPS, kiểm tra bản backup kế tiếp có dấu hiệu `ALAPBK01`.

## Bước 6. Ghi nhận

- [x] Tạo Bug Jira cho mỗi lỗi đã sửa (SEC-036, 052/053, 051, PERF-002 nếu là lỗi, 009, 011, 013, 019, 020, pool), comment kết quả chạy lại.
- [ ] Cập nhật sheet (Status, Defect ID, Notes/Actual), báo cáo nhóm, `aidlc-state.md`, `audit.md`.

## Ngoài phạm vi

- Nâng giới hạn CPU/RAM trong compose: chờ cấu hình VPS (`nproc`, `free -h`).
- Bản dump trước deploy trong `/var/backups/platform*` không mã hoá: ghi nhận, không sửa lần này.
- 36 bản cập nhật bảo mật Ubuntu trên VPS: việc của người quản trị.

## Ghi chú thực hiện bước 4 (2026-11-26)

- PERF-009: `listGradebookEvaluations` (chỉ trường bảng điểm, chỉ sinh viên của trang đang xem) và `listSubmissionsByAssignment` (một truy vấn trạng thái cho mọi cột). Một request: 317 câu SQL / ~298 ms DB → 144 câu / 57 ms. k6 10 VU: p95 9,3 s → 659 ms (Pass).
- PERF-011: nguyên nhân thật không phải 200 giao dịch mà là mỗi mục gọi `listSubmissions`, đọc 200 lượt làm kèm `snapshot` (40.000 dòng mỗi request). `listSubmissions` giờ chỉ đọc cột trạng thái; vẫn mỗi mục một giao dịch. Sau đó: `findGradedAttemptId` (chỉ lượt của sinh viên đó), rồi `GradingService.BatchChecks` (quyền ghi điểm theo lớp, bài đã đóng và lượt được chấm của lớp đọc một lần cho cả lô; mỗi mục vẫn khoá, kiểm phiên bản và hash bài nộp). p95 finalize 7,6 s → 1,18 s, publish 5,2 s → 1,37 s (Pass).
- PERF-013: 3,40 / 3,14 / 3,01 s (Pass).
- PERF-002: 100 VU dồn cùng lúc 0% lỗi (p95 15,1 s do xếp hàng), refresh p95 24 ms; tốc độ đều 3 lượt/s p95 548 ms, 4 lượt/s p95 5,3 s. Người dùng chốt target mới (3A): 3 lượt/s p95 ≤ 1 s, lỗi < 1%; dồn 100 người lỗi < 1% (xếp hàng ≤ 30 s), refresh p95 ≤ 500 ms → Pass.
- PERF-004/005: `DocumentFiles.resolveFiles` quét lại cả tài liệu cho từng block của bản đã lưu (O(n²), 2.000 khối ≈ 1 s mỗi lần lưu) → một lượt. Người dùng chọn sửa tiếp để hết 5xx: `AttemptService.saveAttempt` làm sạch và kiểm tài liệu ngoài transaction (bước ghi khoá lượt, kiểm lại trạng thái, hạn, phiên bản); Hikari `connection-timeout` 3 s → `${DB_CONNECTION_TIMEOUT:10000}`. Đo lại: hết lỗi chờ pool; PERF-005 0% lỗi; PERF-004 còn 4,4% (503 khi kiểm phiên: lệnh Redis quá 500 ms). Nguyên nhân: container 1 GB nên JVM tự chọn SerialGC, Full GC dừng cả app tới 862 ms. `backend/Dockerfile` chạy `-XX:+UseG1GC`: 80 lần lưu song song, dừng GC lâu nhất 239 ms, không Full GC. Vẫn còn 503 kiểm phiên theo cụm lúc 100 người cùng lưu (Java 17 nên không có virtual thread, 100 luồng Tomcat tranh 2 CPU): thêm `SaveConcurrencyLimiter` (interceptor cho `PUT /api/v1/attempts/*/content`, `U08_SAVE_MAX_CONCURRENT` mặc định số CPU, chờ tối đa `U08_SAVE_QUEUE_WAIT` 30 s rồi 503). Đo nhanh (`evidence/fast-perf004.mjs`, 100 lượt lưu × 2 đợt, kể cả khi đốt CPU máy host): 100% trả 200, không lỗi kiểm phiên, 6–8 lần lưu/s. Còn chạy lại k6 đầy đủ PERF-004/005 (k6 cùng máy làm CPU host lên 100%) và chốt target độ trễ dạng throughput với người dùng. Tạm dừng 2026-11-26 theo yêu cầu người dùng để làm system test FR.
- PERF-013: nguyên nhân chính là `SubjectRepositoryAdapter.findByIdForUpdate` (U04) đọc view `pg_settings` mỗi dòng (~4 ms × 200). Đổi sang `current_setting(...)::interval`; quy tắc khoá giữ nguyên.
- PERF-002: đo 1 lần ~450 ms; 2 lần cùng lúc 3,4 lượt/s; 16 lần 1,5 lượt/s (container 2 CPU bị throttle). Thêm `ConcurrencyLimitedPasswordEncoder` (`U01_BCRYPT_MAX_CONCURRENT`, `U01_BCRYPT_QUEUE_WAIT`).
- Jira: KAN-241…KAN-250.
