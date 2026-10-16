# U06 AI Connections — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Thêm hoặc thay API cá nhân | UC 31 | AI Connection (tab API cá nhân) |
| F2 Kiểm tra API cá nhân | UC 31 | AI Connection |
| F3 Thu hồi API cá nhân | UC 31 | AI Connection |
| F4 Tạo credential MCP | UC 32 | AI Connection (tab MCP Agent) |
| F5 Thu hồi credential MCP | UC 32 | AI Connection |
| F6 Agent gọi tool MCP | UC 32 | - |
| F7 Xem lịch sử sử dụng | UC 33 | AI Usage History |
| F8 Cấu hình API chung | UC 14, 15 | Setting List, Setting Detail (phần API AI của trường) |
| F9 Cấu hình quota | UC 14, 15 | Setting Detail (phần quota AI giảng viên) |
| F10 Tổng hợp usage API chung | UC 33 (Admin) | Setting Detail (phần quota AI giảng viên) |
| F11 Chọn nguồn và gửi task AI | UC của U07–U10 | Popup chọn nguồn AI |
| F12 Worker chạy task | - | - |
| F13 Dọn task quá hạn | - | - |
| F14 Tóm tắt cho Setting List | UC 14 | Setting List |

## 2. API cá nhân (Student, Teacher, Subject Manager)

### F1 - Thêm hoặc thay API cá nhân (UC 31)
1. Mở AI Connection từ menu tài khoản, tab API cá nhân; frontend gọi `GET /api/v1/me/ai-connections` và `GET /api/v1/ai/providers` (provider và model cho phép).
2. Bấm Thêm (hoặc Thay) ở một provider, nhập key, chọn model; frontend gọi `PUT /api/v1/me/ai-connections/{provider}` (kèm `version` khi thay; đổi model không cần nhập lại key).
3. Kiểm người gọi là Student, Teacher hoặc Subject Manager đang `ACTIVE`, provider hợp lệ, model nằm trong danh sách cho phép, key đúng định dạng cơ bản (BR-U06-01, 02).
4. Mã hóa key, lưu kết nối `PERSONAL` `ACTIVE` với 4 ký tự cuối; thay thì ghi đè bản mã, tăng `version`, xóa kết quả kiểm tra cũ (BR-U06-03, 06).
5. Ghi audit `AI_CONNECTION_CHANGED`; trả kết nối dạng che, gợi ý bấm Kiểm tra.

### F2 - Kiểm tra API cá nhân (UC 31)
1. Bấm Kiểm tra; UI báo có thể phát sinh chi phí nhỏ trên key; frontend gọi `POST /api/v1/me/ai-connections/{provider}:test`.
2. Kiểm chủ sở hữu và kết nối `ACTIVE`; giải mã key trong bộ nhớ.
3. Gửi yêu cầu tổng hợp cố định, tối đa 5 token ra, timeout 20 giây (BR-U06-04).
4. Lưu kết quả (`OK` hoặc lỗi đã làm sạch: key sai, hết hạn mức phía provider, model không hỗ trợ, không kết nối được) và thời điểm; có usage thì ghi bản ghi loại `CONNECTION_TEST`.
5. Ghi audit `AI_CONNECTION_TESTED`; trả kết quả.

### F3 - Thu hồi API cá nhân (UC 31)
1. Bấm Thu hồi, xác nhận; frontend gọi `DELETE /api/v1/me/ai-connections/{provider}`.
2. Kiểm chủ sở hữu; xóa bản mã, chuyển `REVOKED`; ghi audit `AI_CONNECTION_CHANGED`.
3. Task chưa gửi đi dùng kết nối này chuyển `FAILED` khi tới lượt chạy; task đã gửi vẫn chạy xong (BR-U06-05).

## 3. MCP (Student, Teacher, Subject Manager)

### F4 - Tạo credential MCP (UC 32)
1. Mở tab MCP Agent; frontend gọi `GET /api/v1/me/mcp-credentials`.
2. Bấm Tạo, nhập nhãn (tên agent), chọn quyền `READ_ONLY` hoặc `READ_WRITE` và hạn 7, 30 hoặc 90 ngày; frontend gọi `POST /api/v1/me/mcp-credentials`.
3. Kiểm role Student, Teacher hoặc Subject Manager đang `ACTIVE`; chưa có quá 5 credential còn hiệu lực (BR-U06-30).
4. Sinh token ngẫu nhiên 256 bit; lưu bản băm, 4 ký tự cuối và hạn (BR-U06-31).
5. Ghi audit `MCP_CREDENTIAL_CREATED`; trả token đúng một lần kèm địa chỉ `/mcp` và hướng dẫn cho từng ứng dụng agent đã kiểm chứng (BR-U06-50).

### F5 - Thu hồi credential MCP (UC 32)
1. Bấm Thu hồi, xác nhận; frontend gọi `DELETE /api/v1/me/mcp-credentials/{id}`.
2. Kiểm chủ sở hữu; ghi `revokedAt`; ghi audit `MCP_REVOKED`.
3. Lời gọi tool tiếp theo dùng token này bị từ chối.

### F6 - Agent gọi tool MCP (UC 32)
1. Agent gửi `POST /mcp` (JSON-RPC `initialize`, `tools/list`, `tools/call`) kèm `Authorization: Bearer`.
2. Băm token, tìm credential; không có, đã thu hồi hoặc hết hạn thì trả `401`.
3. Dựng actor từ tài khoản của credential (BR-U06-33).
4. `tools/list`: trả các tool đăng ký qua `IMcpToolProvider` mà role của actor được dùng; credential `READ_ONLY` chỉ thấy tool đọc.
5. `tools/call`: kiểm giới hạn 60 lời gọi mỗi phút; tìm tool; tool ghi cần `READ_WRITE`; kiểm tham số theo schema, không quá 64 KB.
6. Gọi `IMcpToolProvider.call` của unit sở hữu; unit này gọi `IAuthorization.authorize` với actor (U01 kiểm `ACTIVE`, role, phạm vi lớp hoặc môn) và kiểm `expectedVersion` khi ghi.
7. Cắt kết quả không quá 256 KB; ghi usage nguồn `MCP` (tên tool, kết quả; token `UNAVAILABLE` hoặc `EXTERNALLY_REPORTED`); cập nhật `lastUsedAt`; ghi audit `MCP_TOOL_CALLED`.
8. Bị từ chối thì trả lỗi JSON-RPC không lộ dữ liệu và ghi audit `ACCESS_DENIED`.

## 4. Lịch sử sử dụng

### F7 - Xem lịch sử sử dụng (UC 33)
1. Từ AI Connection mở AI Usage History; frontend gọi `GET /api/v1/me/ai-usage` kèm khoảng thời gian, nguồn, trang (tối đa 100 dòng).
2. Chỉ trả bản ghi của người gọi.
3. Mỗi dòng: thời điểm, nguồn, provider và model, loại task hoặc tên tool, trạng thái, token vào và ra kèm nhãn số liệu; không có số thì hiện "Không có số liệu", không hiện 0 (BR-U06-40).
4. Teacher, Subject Manager có thẻ quota chung còn lại hôm nay (`GET /api/v1/ai/sources`).

## 5. Admin (qua Settings của U03)

### F8 - Cấu hình API chung (UC 14, 15)
1. Admin mở Setting List; bấm phần "API AI của trường" mở `AiConnectionsPanel`; panel gọi `GET /api/v1/admin/ai/connections` (dạng che).
2. Thêm hoặc thay key, chọn model: `PUT /api/v1/admin/ai/connections/{provider}` kèm `version`; kiểm Admin; mã hóa, lưu kết nối `INSTITUTIONAL` `ACTIVE`.
3. Kiểm tra: `POST /api/v1/admin/ai/connections/{provider}:test` chạy như F2 bước 2–5; usage ghi nguồn `SHARED`, không trừ quota giảng viên.
4. Tắt hoặc bật: `PUT .../{provider}/status`; thu hồi: `DELETE .../{provider}` xóa bản mã.
5. Ghi audit `AI_CONNECTION_CHANGED`; yêu cầu AI mới dùng cấu hình hiện hành (BR-U06-05).

### F9 - Cấu hình quota (UC 14, 15)
1. Bấm phần "Quota AI giảng viên" mở `TeacherQuotaPanel`; panel gọi `GET /api/v1/admin/ai/quota-policy` và `GET /api/v1/admin/ai/teacher-quotas?q=`.
2. Đặt mức mặc định mỗi ngày và trần ngày toàn hệ thống: `PUT /api/v1/admin/ai/quota-policy` kèm `version`; số nguyên không âm.
3. Đặt mức riêng: tìm giảng viên qua `IAccountLookup.findAccounts` (role `TEACHER`, `SUBJECT_MANAGER`), gọi `PUT /api/v1/admin/ai/teacher-quotas/{accountId}` kèm `version`; bỏ mức riêng: `DELETE` để về mặc định.
4. Kiểm Admin; người được đặt phải là Teacher hoặc Subject Manager; lệch `version` thì `409`.
5. Ghi audit `QUOTA_CHANGED` (trước, sau); mức mới áp cho lần giữ chỗ tiếp theo, không thu hồi phần đã dùng.

### F10 - Tổng hợp usage API chung (UC 33, Admin)
1. Trong `TeacherQuotaPanel`, tab Usage gọi `GET /api/v1/admin/ai/usage-summary` kèm khoảng thời gian.
2. Trả tổng theo giảng viên (đã dùng hôm nay, mức áp dụng), theo ngày, theo provider và model; chỉ nguồn `SHARED`, không nội dung, không key (BR-U06-41).

## 6. Task AI cho unit khác

### F11 - Chọn nguồn và gửi task AI
1. Ở màn của unit gọi (soạn Assignment, soạn Quiz, xin phản hồi AI, đề xuất chấm), người dùng bấm nút AI; `AiSourcePicker` gọi `GET /api/v1/ai/sources`: API chung theo từng provider (chỉ Teacher, Subject Manager; kèm quota còn lại hoặc lý do không dùng được) và các kết nối cá nhân `ACTIVE`.
2. Người dùng chọn nguồn; UI hiện bên chịu chi phí và số token ước tính có nhãn "ước tính" (BR-U06-27); không tự chọn thay.
3. Frontend gọi API của unit gọi; unit này kiểm quyền trên đích, dựng prompt tối thiểu, gọi `IAiTask.submit` kèm `requestRef` (BR-U06-20).
4. U06 kiểm `requestRef`: trùng dấu vân tay thì trả task cũ, khác thì `409` (BR-U06-21).
5. Kiểm nguồn: `SHARED` chỉ Teacher, Subject Manager `ACTIVE` và kết nối chung `ACTIVE`; `PERSONAL` đúng chủ và `ACTIVE`; người gọi có ít hơn 3 task đang chờ hoặc chạy (BR-U06-11, 17).
6. `SHARED`: giữ chỗ quota (BR-U06-13); không đủ thì từ chối, báo thời điểm có cửa sổ mới.
7. Tạo task `QUEUED`, lưu prompt; sau commit gửi job `AI_TASK` qua `IJob` với `idempotencyKey` là `taskId`.
8. Trả `taskId`; `AiTaskStatus` hỏi `GET /api/v1/ai/tasks/{id}` mỗi 2 giây tới khi task kết thúc. Người gửi có thể hủy khi còn `QUEUED` (BR-U06-26).

### F12 - Worker chạy task
1. Worker nhận `AI_TASK` trên queue `jobs.ai`; khóa dòng task.
2. Task `READY` chưa chuyển cho handler: chỉ chạy lại bước 8. Task `RUNNING` (đã có mốc gửi, chưa có kết quả): chuyển `UNKNOWN`, tính giữ chỗ là đã dùng, không gọi provider. Trạng thái khác `QUEUED`: bỏ qua (BR-U06-22).
3. Kết nối không còn `ACTIVE`: `FAILED`, giải phóng giữ chỗ.
4. Ghi mốc gửi (`RUNNING`, `dispatchedAt`), commit.
5. Giải mã key, gọi adapter của provider (timeout 120 giây) với prompt, JSON schema, `maxOutputTokens`.
6. Provider trả phản hồi: lưu kết quả và usage `PROVIDER_MEASURED`; quyết toán quota bằng token thật; đúng schema thì `READY`, sai thì `FAILED` (BR-U06-14, 23).
7. Provider từ chối không tính token: `FAILED` với lỗi đã làm sạch, giải phóng giữ chỗ. Hết giờ hoặc mất kết nối sau khi gửi: `UNKNOWN`, tính giữ chỗ là đã dùng với nhãn `ESTIMATED`.
8. `READY`: gọi `IAiTaskResultHandler.onReady` của unit gọi trong transaction riêng; handler kiểm quyền và `inputVersion` rồi áp dụng (bản nháp, phản hồi, đề xuất) hoặc trả `STALE`; U06 ghi kết quả áp dụng. `FAILED`, `UNKNOWN`: gọi `onFailed`.
9. Xóa prompt và kết quả khỏi task; giữ thông tin task và usage (BR-U06-24).

### F13 - Dọn task quá hạn
1. `IPendingSweeper`: task `QUEUED` quá 5 phút được U03 gửi lại message.
2. `IScheduledScanner` chạy mỗi phút: task `QUEUED` quá 30 phút chuyển `CANCELLED`, giải phóng giữ chỗ (chưa gửi provider).
3. Task `RUNNING` quá 10 phút chuyển `UNKNOWN`, tính giữ chỗ là đã dùng.

### F14 - Tóm tắt cho Setting List (UC 14)
1. U03 gọi `summary()` của `AiConnectionsSection` và `TeacherQuotaSection` (timeout 1 giây).
2. Trả bản tóm tắt đã che: trạng thái và model từng provider của API chung; mức mặc định, trần ngày, số giảng viên có mức riêng, hoặc "Chưa cấu hình".
