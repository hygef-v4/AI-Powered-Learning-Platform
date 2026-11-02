# U06 AI Connections — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
