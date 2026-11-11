# U12 — Sinh viên tự chia nhóm, giảng viên duyệt: kết quả Build and Test

Nhánh `feat/student-self-grouping` (worktree `../AI-Powered-Learning-Platform-self-grouping`, tách `--no-track` từ `origin/staging@e78ac7b9`), mã chưa commit. Phạm vi: FR-025, SG-01…09, US-GRP-007…010, BR-U12-70…96. Hướng dẫn build và test chung vẫn ở [build-instructions.md](build-instructions.md) và [unit-test-instructions.md](unit-test-instructions.md); tệp này chỉ ghi lần chạy của tính năng.

## Kết quả tự động

| Phần | Lệnh | Kết quả |
|---|---|---|
| Backend toàn bộ | `mvn -B verify` trong `maven:3.9-eclipse-temurin-17`, Testcontainers PostgreSQL | 2.203 test, 0 lỗi, 0 bỏ qua, `BUILD SUCCESS`, 13 phút 43 giây, xong 2026-11-11T13:37:42Z. Gồm `ArchitectureTest` và các integration test PostgreSQL bắt buộc. |
| Backend của tính năng | (trong lần chạy trên) | `ProposalRulesTest` 7, `GroupProposalRepositoryIntegrationTest` 4, `GroupFormationServiceTest` 5, `GroupProposalServiceTest` 12, `GroupProposalReviewServiceTest` 5, `GroupProposalCleanupTest` 3, `GroupProposalFlowIntegrationTest` 3, `GroupProposalControllerTest` 4, `GroupProposalNotificationTest` 7; thêm 2 test mới trong `ClassGroupServiceTest`. |
| Frontend | `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` | ESLint sạch, `tsc` sạch, Vitest 978/978 test trong 138 tệp, production build thành công. |

Migration `V20261111_1800__group_self_formation.sql` chạy trên PostgreSQL tạm của Testcontainers và trên stack thử riêng bên dưới; chưa áp lên cơ sở dữ liệu staging hay production.

## Thử trên trình duyệt (stack riêng)

Stack Docker Compose riêng tên `selfgroup` dựng từ worktree: nginx cổng 18088, backend 18080, PostgreSQL 15433, Mailpit 18025, file env với secret ngẫu nhiên mới. Stack `platform` và dữ liệu của nó không bị đụng. Lần build đầu lỗi mạng khi tải `jboss-logging` từ Maven Central ("Remote host terminated the handshake"); build lại thì qua, không liên quan mã.

Dữ liệu seed bằng SQL: 1 giảng viên, 6 sinh viên, lớp SE1801 (`OPEN`) môn SWP391. Mật khẩu thử sinh ngẫu nhiên, băm bằng `crypt()`, chỉ nằm trong scratchpad, không ghi vào repo hay tài liệu.

| Bước | Vai trò, cách thử | Kết quả |
|---|---|---|
| Bật đợt tự chia nhóm, sĩ số 2–3 | Giảng viên, trình duyệt | Hộp "Sinh viên tự chia nhóm" lưu được; khung hiện "Đang mở, nhóm từ 2 đến 3 người", "Chờ duyệt (0)", "Chưa có nhóm (6)". |
| Tạo "Nhóm A", mời Bình Trần | Sinh viên 1, trình duyệt | Đề xuất ở trạng thái Nháp, 1/3; hộp mời liệt kê 5 bạn chưa có nhóm kèm email; lời mời hiện "Đang chờ trả lời" và nút "Thu hồi". "Gửi duyệt" bị khóa khi chưa đủ 2 người. |
| Chấp nhận lời mời | Sinh viên 2, API | 200; Nhóm A thành 2/3. |
| Tạo "Nhóm B", mời sinh viên 2 (đang ở Nhóm A) | Sinh viên 3, API | Lời mời tạo được (đúng BR-U12-77). |
| Sinh viên 2 chấp nhận lời mời Nhóm B | Sinh viên 2, API | 409 `STUDENT_IN_PROPOSAL`, "Bạn đang ở một đề xuất khác, hãy rời trước" (BR-U12-78). |
| Mời sinh viên 4 với `version` cũ | Sinh viên 3, API | 409, đúng khóa `version`; gửi lại với `version` mới thì qua. |
| Sinh viên 4 chấp nhận, sinh viên 3 gửi duyệt Nhóm B | API | 200, `SUBMITTED`. |
| Gửi duyệt Nhóm A | Sinh viên 1, trình duyệt | Trạng thái "Chờ duyệt", chỉ còn nút "Rút về để sửa". |
| Duyệt Nhóm A | Giảng viên, trình duyệt | "Chờ duyệt (2)" về 1; Nhóm A thành nhóm chính thức, An Nguyễn là trưởng nhóm; "Chưa có nhóm" từ 6 về 4. |
| Từ chối Nhóm B có lý do | Giảng viên, trình duyệt | "Chờ duyệt (0)". Sinh viên 3 (API) thấy `lastRejected` với lý do "Nhóm cần đủ 3 người", `proposal` rỗng, được lập nhóm lại. |
| Thông báo trong ứng dụng | API từng tài khoản | Giảng viên: 2 `GROUP_PROPOSAL_SUBMITTED`. Sinh viên 2: 2 `GROUP_PROPOSAL_INVITED`, 1 `GROUP_PROPOSAL_APPROVED`. Sinh viên 1: 1 `GROUP_PROPOSAL_APPROVED`. Sinh viên 3: 1 `GROUP_PROPOSAL_REJECTED`. Không có thông báo "được thêm vào nhóm" trùng sau khi duyệt (BR về chống trùng). |

Ghi chú nhỏ, không chặn: danh sách "bạn mời được" không sắp theo tên (thứ tự thấy được: Giang, Bình, Chi, Hà, Dũng).

## Giới hạn

- Chưa thử: duyệt nhiều đề xuất một lần trên giao diện, hạn chót đóng đợt, rút về nháp, sinh viên rời hoặc hủy đề xuất, chia ngẫu nhiên phần còn lại, giao diện điện thoại và chế độ sáng. Các nhánh này có test tự động ở backend và frontend nhưng chưa xem trên trình duyệt.
- Không đo hiệu năng; không có hạ tầng mới. Không chạy CI, không deploy, không commit hay push.
- Stack `selfgroup` vẫn đang chạy trên máy để xem lại; có thể tắt bằng `docker compose -p selfgroup down -v`.

## Extension

Security (SECURITY-03/05/08/09/15): kiểm ở backend chặn được lời gọi API vượt qua giao diện (chấp nhận khi đang ở đề xuất khác, `version` cũ đều trả 409); kiểm sai vai trò và người ngoài lớp có trong test controller và service, chưa thử thủ công; mật khẩu và secret thử không ghi vào repo. Resiliency (RESILIENCY-10): khóa `version` và index một đề xuất hoạt động mỗi sinh viên hoạt động đúng trên PostgreSQL thật. PBT tắt. SECURITY-04/12, RESILIENCY-04/06: N/A, không đổi header, xác thực, pipeline hay health check.
