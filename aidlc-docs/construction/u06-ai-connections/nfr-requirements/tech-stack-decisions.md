# U06 AI Connections — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Gọi provider | Spring `RestClient` gọi REST API trực tiếp của Gemini, OpenAI, Anthropic; ba adapter cùng interface | Không cần SDK riêng từng hãng; chuẩn hóa usage và lỗi một chỗ |
| Kết quả có cấu trúc | JSON schema gửi kèm yêu cầu (structured output của từng provider) và kiểm lại bằng thư viện JSON schema validator | Không tin kết quả AI |
| Mã hóa key | JCA `AES/GCM/NoPadding`, khóa 256 bit từ biến môi trường | Có sẵn trong JDK |
| MCP server | MCP Java SDK (`io.modelcontextprotocol.sdk`), transport Streamable HTTP trên Spring MVC | SDK chính thức, transport cho server từ xa |
| Giới hạn lời gọi MCP | Đếm trong Redis theo credential và phút | Redis đã có trong hạ tầng |
| Lưu trữ | PostgreSQL + Spring Data JPA, 7 bảng `ai_*`, `mcp_credentials` | Như các unit khác |
| Embedding, RAG | Không dùng trong MVP | RAG là phần mở rộng |
| Test | JUnit 5; HTTP server giả cho adapter; Testcontainers cho integration test do tester viết | Theo NFR-004 |
