# U06 AI Connections — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `PersonalAiConnectionController`, `McpCredentialController`, `AiUsageController`, `AiTaskController` | backend | API của người dùng (F1–F5, F7, F11 bước 1, 8) |
| `AdminAiController` | backend | API Admin cho API chung, quota, tổng hợp usage (F8–F10) |
| `AiConnectionService`, `ConnectionTestService`, `KeyCipher`, `ProviderCatalog` | backend | Lưu, che, mã hóa key; kiểm tra kết nối; model cho phép |
| `GeminiAdapter`, `OpenAiAdapter`, `ClaudeAdapter` | backend, worker | Gọi provider, chuẩn hóa kết quả, usage, lỗi |
| `QuotaPolicyService`, `QuotaReservationService`, `UsageSummaryService` | backend, worker | Mức quota, giữ chỗ, quyết toán, tổng hợp (P1) |
| `AiTaskService` (cài `IAiTask`), `AiSourceService` | backend | Nhận task, kiểm nguồn, giữ chỗ, gửi job (F11) |
| `AiTaskJobHandler`, `AiTaskResultHandlerRegistry`, `AiTaskPendingSweeper`, `AiTaskTimeoutScanner` | worker | Chạy task, gọi handler, dọn task quá hạn (F12, F13) |
| `AiUsageService` | backend, worker | Ghi và đọc usage |
| `McpServerConfig`, `McpAuthenticationFilter`, `McpToolRegistry`, `McpRateLimiter` | backend | MCP server và tool (F6) |
| `AiConnectionsSection`, `TeacherQuotaSection` | backend | Tóm tắt cho Setting List (F14) |
| Repository các bảng `ai_*`, `mcp_credentials` | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
