# U06 AI Connections — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser ----------> nginx --> backend (package ai: API, /mcp) --> postgres (ai_*, mcp_credentials)
agent (MCP client) -> nginx /mcp --> backend
backend --> RabbitMQ jobs.ai --> worker (AiTaskJobHandler) --> Gemini / OpenAI / Claude (HTTPS)
backend --> Redis (đếm lời gọi MCP)
```

**Text alternative**: Người dùng dùng trình duyệt qua Nginx tới backend để quản lý kết nối, credential MCP và xem usage. Agent của người dùng gọi `/mcp` qua Nginx tới backend. Backend lưu dữ liệu trong PostgreSQL, gửi job AI lên RabbitMQ; worker nhận job và gọi Gemini, OpenAI hoặc Claude qua HTTPS. Redis đếm số lời gọi MCP mỗi phút.
