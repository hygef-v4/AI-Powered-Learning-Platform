# U06 AI Connections — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| AI Connection | `AiConnectionPage` | 31, 32 | Student, Teacher, Subject Manager |
| AI Usage History | `AiUsageHistoryPage` | 33 | Student, Teacher, Subject Manager |
| Setting List, Setting Detail (phần API AI của trường, quota AI giảng viên) | `AiConnectionsPanel`, `TeacherQuotaPanel` | 14, 15, 33 | Admin |
| Popup chọn nguồn AI (nhúng ở màn của unit khác) | `AiSourcePicker`, `AiTaskStatus` | UC của U07–U10 | Theo màn chứa |

AI Connection mở từ menu tài khoản (Post-Login) qua `AccountMenuEntry` của U01; AI Usage History mở từ AI Connection.

## 2. Cây component

```
app/ai/connection/            AiConnectionPage     AI Connection
  PersonalApiTab                                   API cá nhân theo provider
    ApiKeyDialog                                   thêm, thay key, chọn model
    ApiSetupGuide                                  hướng dẫn lấy key từng provider
  McpAgentTab                                      danh sách credential MCP
    McpCredentialDialog                            tạo, hiện token một lần
    McpSetupGuide                                  hướng dẫn theo ứng dụng agent đã kiểm chứng
app/ai/usage/                 AiUsageHistoryPage   AI Usage History
  QuotaSummaryCard                                 quota chung còn lại (giảng viên)
  UsageTable                                       bản ghi kèm nhãn số liệu
shared/ai/
  AiSourcePicker                                   popup chọn nguồn, unit khác nhúng
  AiTaskStatus                                     theo dõi task tới khi kết thúc
settings/ (đăng ký vào settingsSectionRegistry của U03)
  AiConnectionsPanel                               API chung: key, model, kiểm tra, tắt, bật, thu hồi
  TeacherQuotaPanel                                mức mặc định, trần, mức riêng, tab Usage
shell/ (đăng ký vào AccountMenuEntry của U01)
  aiConnectionMenuEntry                            mục AI Connection
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `PersonalApiTab`, `ApiKeyDialog` | `GET /api/v1/ai/providers`, `GET`, `PUT`, `DELETE /api/v1/me/ai-connections/...`, `POST ...:test` | Key chỉ nhập, không hiện lại; hiện 4 ký tự cuối, model, lần kiểm tra cuối; Kiểm tra có cảnh báo chi phí; Thu hồi có xác nhận |
| `McpAgentTab`, `McpCredentialDialog` | `GET`, `POST`, `DELETE /api/v1/me/mcp-credentials` | Token hiện một lần, có nút sao chép; danh sách hiện nhãn, quyền, hạn, lần dùng cuối |
| `AiUsageHistoryPage` | `GET /api/v1/me/ai-usage`, `GET /api/v1/ai/sources` | Lọc thời gian, nguồn; số không có hiện "Không có số liệu" |
| `AiConnectionsPanel` | `/api/v1/admin/ai/connections/...` | Như `PersonalApiTab`, thêm Tắt, Bật; `409` báo tải lại |
| `TeacherQuotaPanel` | `/api/v1/admin/ai/quota-policy`, `/teacher-quotas`, `/usage-summary` | Form mức mặc định và trần; bảng mức riêng có tìm kiếm; tab Usage |
| `AiSourcePicker` | `GET /api/v1/ai/sources` | Liệt kê nguồn dùng được và lý do không dùng được; hiện bên chịu chi phí, quota còn lại, ước tính; người dùng phải chọn, không có mặc định tự chuyển |
| `AiTaskStatus` | `GET /api/v1/ai/tasks/{id}`, `POST ...:cancel` | Hỏi mỗi 2 giây; hiện chờ, đang chạy, xong, lỗi, không rõ kết quả; Hủy khi còn chờ |

Route `/ai/*` chỉ cho Student, Teacher, Subject Manager (dùng `RoleGuard` của U01); panel Settings nằm trong route `/admin/*` của U03. Backend vẫn kiểm quyền thật.
