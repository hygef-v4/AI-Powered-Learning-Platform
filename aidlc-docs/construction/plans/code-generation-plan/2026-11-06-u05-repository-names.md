# Bỏ tiền tố Jdbc khỏi repository U05 — 2026-11-06

Yêu cầu: “[Image #1] chỉ cần để tên repository như bthg là được k cần thêm prefix JDBC đâu”. Áp dụng cho ba repository U05 trong ảnh, giữ JDBC và interface hiện hữu; không mở rộng rename sang unit khác.

- [x] Rà caller, constructor, method reference, test, bean name/qualifier và tài liệu hiện hành; không có cấu hình injection theo tên cũ.
- [x] Đổi ba concrete class/file thành AcademicsReadRepository, CourseClassRepository, EnrollmentRepository; cập nhật test/caller và field test liên quan.
- [x] Bổ sung quy ước tên repository trong AI-DLC, cập nhật current U05 code summary; giữ evidence lịch sử.
- [x] Test Academics/Architecture: 200 đạt, zero failures/errors/skips, BUILD SUCCESS. Đối chiếu 5 file Java với snapshot chỉ khác đúng phép thay tên đã duyệt; SQL/transaction/API giữ nguyên, không còn reference tên cũ trong source, diff whitespace đạt.

Không cần test mới cho rename thuần; dùng các test hiện có bảo vệ wiring và behavior. Không restart ứng dụng, commit hoặc push trong yêu cầu này. Log/snapshot dùng `.tmp/repository-names-20261106`.

Kết quả: biên dịch toàn bộ 718 main + 246 test source files; focused run `Academics*,ArchitectureTest` đạt 200 test. Class output chỉ còn ba tên mới, không còn Jdbc-prefixed U05 repository classes. Không chạy lại frontend/full backend hoặc runtime acceptance cho rename thuần; không lấy test totals cũ làm kết quả mới. Giữ security/resiliency scope và PBT disabled.
