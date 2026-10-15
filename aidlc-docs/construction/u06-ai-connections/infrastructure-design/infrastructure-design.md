# U06 AI Connections — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, `/mcp`), `worker` (task AI, scanner) |
| Bảng | `ai_connections`, `ai_tasks`, `ai_usage_records`, `ai_quota_policy`, `ai_teacher_quotas`, `ai_quota_windows`, `mcp_credentials` trong `postgres` |
| Migration | `db/migration/ai/V20260925_1200__create_ai_tables.sql`: 7 bảng; unique riêng phần `(owner_id, provider)` với `PERSONAL` chưa `REVOKED` và `(provider)` với `INSTITUTIONAL` chưa `REVOKED`; `ai_tasks.request_ref` unique, index `(actor_id, state)`, `(state, created_at)`; `ai_usage_records` index `(account_id, created_at)`, `(source, created_at)`; `mcp_credentials.token_hash` unique; FK tài khoản → `accounts`; chạy sau `classes` (1100) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE` trên các bảng; không `DELETE` |
| Việc nền | Job `AI_TASK` trên queue `jobs.ai` của U03 |
| Biến môi trường | `U06_AI_KEY_ENCRYPTION_KEY` (base64 32 byte, bắt buộc; thêm `U06_AI_KEY_ENCRYPTION_KEY_V2` khi đổi khóa) |
| Cấu hình | `ai.providers.{gemini,openai,claude}.models` và `maxOutputTokens` trong `application.yml` |
| Nginx | `location /mcp` tới `backend`, `proxy_buffering off`, `proxy_read_timeout 300s` |
| Mạng ra | `backend` và `worker` gọi HTTPS tới `generativelanguage.googleapis.com`, `api.openai.com`, `api.anthropic.com` |
| Redis | Bộ đếm giới hạn lời gọi MCP |
