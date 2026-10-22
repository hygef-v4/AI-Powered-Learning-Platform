# U12 — build, test và acceptance gates

Phạm vi: U12 Group Assignment (UC 53–57) trên nhánh `group-assignment`, ngày 2026-10-22. Đây là phần bổ sung Build & Test của một unit, giống `u15-verification.md`; không thay gate của unit khác và không đánh dấu Build & Test toàn dự án là xong. Xem [code-summary](../u12-group-assignment/code/code-summary.md) và [plan](../plans/code-generation-plan/u12-group-assignment-code-generation-plan.md).

## 1. Build

**Yêu cầu:** JDK 17 trở lên (máy chạy dùng JDK 21), Maven 3.9, Node 20.9 trở lên, Docker (cho PostgreSQL, Redis, RabbitMQ khi chạy thử). Không có Maven wrapper; dùng Maven cài trên máy.

```bash
mvn -B -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend test
NEXT_DIST_DIR=.next-verify-u12 npm --prefix frontend run build
```

Cùng lệnh với CI (`.github/workflows/ci.yml`). Next tự thêm đường dẫn type của thư mục build tạm vào `frontend/tsconfig.json`; khôi phục file này sau khi build (`git checkout -- frontend/tsconfig.json`).

**Kết quả mong đợi:** JAR `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`; route `/classes/[id]/group` và `/group-docs/[id]` trong bảng route của Next.

## 2. Unit test

```bash
mvn -B -f backend/pom.xml test -Dtest='edu.aiplatform.groups.**,edu.aiplatform.groupdocs.**,ArchitectureTest'
npm --prefix frontend test -- src/features/groups src/features/groupdocs
```

Test repository chạy trên PostgreSQL thật khi có biến môi trường (database đã chạy migration, user `app` và user quản trị):

```bash
export U12_TEST_JDBC_URL=jdbc:postgresql://localhost:55432/platform
export U12_TEST_JDBC_USER=app
export U12_TEST_JDBC_PASSWORD=<mật khẩu app của DB test>
export U12_TEST_ADMIN_JDBC_URL=$U12_TEST_JDBC_URL
export U12_TEST_ADMIN_USER=postgres
export U12_TEST_ADMIN_PASSWORD=<mật khẩu postgres của DB test>
mvn -B -f backend/pom.xml test -Dtest=GroupRepositoriesIntegrationTest
```

Report: `backend/target/surefire-reports`, kết quả Vitest trên console.

## 3. Integration test (tester viết)

Môi trường: `docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml up` (PostgreSQL, Redis, RabbitMQ, Mailpit, backend, worker) hoặc từng dịch vụ chạy riêng; backend profile web và worker; U07, U08 phải có trong `staging` (khi chưa có, bản tạm của U07 trả bài mẫu và mọi kiểm tài liệu đều hợp lệ).

| Kịch bản | Cách làm | Kết quả đúng |
|---|---|---|
| Hai người nhận cùng một mục | Hai sinh viên cùng nhóm gửi `POST .../sections/{id}:claim` đồng thời | Một người `200`, người kia `409 SECTION_CLAIMED_BY_OTHER` |
| Trưởng nhóm giao mục đang có người giữ | `:assign` không `replaceHolder` rồi có `replaceHolder` | Lần đầu `409`; lần sau mục đổi người giữ, bản nháp cũ bị bỏ, audit `GROUP_SECTION_RELEASED` và `GROUP_SECTION_ASSIGNED` |
| Hết hạn khi có mục đang làm | Đặt `closesAt` đã qua hơn 30 giây, chờ một vòng quét | Mục đang giữ chốt bằng bản nháp, tài liệu `CLOSED`, `submit_mode = AUTO_DEADLINE`, U09 có đánh giá nhóm |
| Ngưng giao bài | U07 gọi `onRetired` | Vòng quét kế tiếp tự nộp với `AUTO_RETIRED` |
| Gỡ thành viên đang giữ mục | Giảng viên lưu bộ nhóm bỏ người đó | Mục được nhả; nội dung đã Xong giữ tác giả; kênh SSE của người đó với tài liệu cũ đóng |
| Nhóm mới khi bài đang mở | Lưu bộ nhóm có nhóm mới | Nhóm mới có tài liệu ngay |
| Realtime hai instance | Hai backend cùng RabbitMQ; người A mở tài liệu ở instance 1, người B Xong mục ở instance 2 | A nhận sự kiện trong ≤ 2 giây (NFR-U12-01) |
| Phân quyền | Mọi endpoint với Admin, sinh viên nhóm khác, giảng viên lớp khác | `404` kèm audit `ACCESS_DENIED`; giảng viên lớp nhả khóa được nhưng không sửa, không giao, không nộp |
| U09 đọc bản nộp | Nộp rồi mở Submission Detail của U09 | U09 thấy nội dung, tác giả từng mục, mã biên nhận |

## 4. Hiệu năng (NFR-U12-01–04)

| Chỉ tiêu | Mục tiêu | Cách đo |
|---|---|---|
| Lưu nháp mục | p95 ≤ 500 ms | k6 hoặc JMeter: 50 người dùng ảo lưu nháp 10 giây một lần trong 5 phút |
| Nhận, giao, nhả, Xong | p95 ≤ 300 ms | Như trên, trộn các thao tác |
| Lưu bộ nhóm 200 sinh viên | p95 ≤ 1 giây | 20 lần `PUT .../groups` với 40 nhóm × 5 người |
| Tạo tài liệu 50 nhóm | ≤ 30 giây sau khi bài mở | Mở bài nhóm cho lớp 50 nhóm, đo tới khi đủ 50 dòng `group_documents` |
| Tự nộp | ≤ 1 phút sau hạn + 30 giây | Ghi thời điểm `submitted_at` so với `closesAt` |
| Realtime | ≤ 2 giây | Đo từ lúc Xong tới lúc client nhận sự kiện |

Chưa chạy: máy dev không có công cụ tải và U07, U08 chưa có trong `staging`.

## 5. Bảo mật

- Phân quyền theo bảng ở mục 3; kiểm `version` lệch trả `409`; dữ liệu quá giới hạn (2.000 block, 2 MB mỗi mục, 200 sự kiện hoạt động mỗi lần, lý do quá 1.000 ký tự) bị từ chối.
- Log không chứa nội dung tài liệu hay lý do yêu cầu (NFR-U12-31): kiểm log backend sau các kịch bản ở mục 3.
- Kênh SSE: mở `GET .../events` bằng tài khoản không thuộc nhóm phải nhận `404`.

## 6. Kết quả đã chạy (2026-10-22)

| Hạng mục | Kết quả |
|---|---|
| Backend U12 | 60 test pass (gồm 2 test repository trên PostgreSQL 16), `ArchitectureTest` pass |
| `mvn -B verify` toàn backend | 888 test, 33 bỏ qua, 4 lỗi; cả 4 là integration test của unit khác dừng ở migration `V20260925_1500__create_grading_tables.sql` (U09) vì thiếu bảng `assignments`; U12 không sửa file này |
| Frontend | 305 test pass (U12: 13), lint, typecheck, build pass |
| Khởi động thử | Context lên với PostgreSQL, Redis local (tắt Flyway và kiểm schema vì migration 1500); bản tạm của U12 đã tắt; API nhóm trả `401` khi chưa đăng nhập |
| Trình duyệt (API giả) | Workspace, Nhóm của tôi, tab Sinh viên, trình sửa nhóm hiển thị đúng |
| Integration, hiệu năng | Chưa chạy (tester; cần U07, U08) |

## 7. Gate còn mở

- Migration 1500 của U09 phải sửa thì `mvn verify` mới sạch và backend chạy được Flyway trên DB mới.
- Luồng đầy đủ phát hành bài nhóm → làm → nộp → chấm cần U07, U08.
- Integration test và đo hiệu năng ở mục 3, 4 do tester làm.
