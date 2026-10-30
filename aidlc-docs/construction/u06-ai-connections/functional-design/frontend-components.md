# U06 AI Connections — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 6 | AI Connection (`c12`), popup Thêm khóa API (`c12a`), Kết nối agent mới (`c12b`), Token agent và owner reveal khi có ciphertext (`c12c`) | `AiConnectionPage`, `ApiKeyDialog`, `McpCredentialDialog`, `McpTokenDialog` | 31, 32 | Student, Teacher, Subject Manager |
| 7 | AI Usage History (`c13`) | `AiUsageHistoryPage` | 33 | Student, Teacher, Subject Manager |
| 10 | Setting Detail — API AI của trường và hạn mức: tab Khóa và hạn mức (`a14`), tab Usage (`a14c`) | `AiConnectionsPanel`, `TeacherQuotaPanel` | 14, 15, 33 | Admin |
| – | Popup Chọn nguồn AI (`c16`), nhúng ở màn của unit khác | `AiSourcePicker`, `AiTaskStatus` | UC của U07–U10 | Theo màn chứa |

AI Connection mở từ nút "Kết nối AI" trên thanh trên cùng (U06 đăng ký `AccountMenuEntry` cho Student, Teacher, Subject Manager); AI Usage History mở từ AI Connection. Trang Setting Detail của U06 mở từ hai thẻ "API AI của trường" và "Hạn mức AI giảng viên" trên Setting List (U03).

## 2. Cây component

```
app/ai/connection/            AiConnectionPage     #6 AI Connection
  AiSummaryCards                                   hạn mức của trường hôm nay (Teacher, SM), số khóa cá nhân, số agent
  PersonalApiSection                               bảng khóa theo nhà cung cấp
    ApiKeyDialog                                   c12a: thêm, thay khóa, chọn mô hình, kiểm tra
    ApiSetupGuide                                  hướng dẫn lấy khóa từng nhà cung cấp
  McpAgentSection                                  bảng credential MCP
    McpCredentialDialog                            c12b: nhãn, quyền, hạn dùng
    McpTokenDialog                                 c12c: token tạo/reveal có kiểm owner và ACTIVE
    McpSetupGuide                                  các bước cài agent và đoạn cấu hình mẫu
app/ai/usage/                 AiUsageHistoryPage   #7 AI Usage History
  UsageSummaryCards, UsageFilters, UsageTable
shared/ai/
  AiSourcePicker                                   c16: popup chọn nguồn, unit khác nhúng
  AiTaskStatus                                     theo dõi task tới khi kết thúc
settings/ (đăng ký vào settingsSectionRegistry của U03)
  AiConnectionsPanel                               #10: mở từ thẻ "API AI của trường"; đầu trang, tab Khóa và hạn mức, Usage
  TeacherQuotaPanel                                cùng trang, mở từ thẻ "Hạn mức AI giảng viên", cuộn tới phần hạn mức
    InstitutionalKeysSection                       khóa API của trường: thêm, kiểm tra, bật/tắt, thay, thu hồi
    QuotaPolicySection                             hạn mức chung mỗi giảng viên và trần toàn hệ thống
    TeacherQuotaTable                              mức riêng từng giảng viên
    SharedUsageTab                                 usage API của trường
shell/ (đăng ký vào AccountMenuEntry của U01)
  aiConnectionMenuEntry                            "Kết nối AI" trên thanh trên cùng
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `AiConnectionPage`, `AiSummaryCards` | `GET /api/v1/me/ai-connections`, `GET /api/v1/me/mcp-credentials`, `GET /api/v1/ai/sources` | "Kết nối AI", "API cá nhân và agent MCP của riêng bạn. Khóa đã lưu luôn được che và chỉ dùng cho yêu cầu của bạn."; nút Lịch sử sử dụng AI. Thẻ: Hạn mức của trường hôm nay (đã dùng / mức, đặt lại lúc 00:00; chỉ Teacher, Subject Manager), API cá nhân (số khóa đã lưu, chi phí tính vào tài khoản nhà cung cấp), Agent MCP (n / 5 còn hiệu lực) |
| `PersonalApiSection`, `ApiKeyDialog` | `GET /api/v1/ai/providers`, `GET`, `PUT`, `DELETE /api/v1/me/ai-connections/...`, `POST ...:test` | Bảng Nhà cung cấp (Gemini, OpenAI, Claude), Khóa (chỉ 4 ký tự cuối, ví dụ "••••3kQf"), Mô hình, Kiểm tra gần nhất (Đạt hoặc lỗi · thời điểm); Kiểm tra, Thay khóa, Thu hồi (có xác nhận); nhà cung cấp chưa có khóa ghi "Chưa cấu hình" kèm Thêm khóa. Popup: Nhà cung cấp* (Gemini — Google AI Studio, OpenAI — platform.openai.com, Claude — console.anthropic.com), Khóa API* (chỉ nhập, không hiện lại; "sau khi lưu chỉ hiện 4 ký tự cuối"), Mô hình*, nút Kiểm tra kết nối (kết quả, thời gian) kèm cảnh báo có thể tính phí; Hủy, Lưu khóa |
| `ApiSetupGuide` | - | "Hướng dẫn lấy khóa API": các bước cho Gemini, OpenAI, Claude |
| `McpAgentSection`, `McpCredentialDialog`, `McpTokenDialog` | `GET`, `POST`, `DELETE /api/v1/me/mcp-credentials` | "Kết nối agent riêng để dùng công cụ của nền tảng · quyền Chỉ đọc hoặc Đọc và ghi · tối đa 5 agent còn hiệu lực"; nút Kết nối agent mới. Bảng Nhãn và token (4 ký tự cuối), Quyền, Ngày tạo, Hạn dùng, Lần dùng cuối, Trạng thái (Còn hiệu lực, Hết hạn, Đã thu hồi), Thu hồi. Popup tạo: Nhãn*, Quyền* (Chỉ đọc: xem lớp, bài, học liệu, kết quả trong quyền; Đọc và ghi: thêm thao tác được làm như tạo nháp, đề xuất chấm), Hạn dùng 7 / 30 / 90 ngày (mặc định 30), "Bạn đang có n/5 agent còn hiệu lực"; Hủy, Tạo token. Popup token: "Sao chép token trước khi đóng", token kèm nút Sao chép, nhãn, quyền, hạn (ngày hết hạn); "Tôi đã sao chép, đóng" |
| `McpSetupGuide` | - | "Hướng dẫn cài agent": tạo token, dán đoạn cấu hình (URL `/mcp`, header `Authorization: Bearer`) vào cấu hình MCP của agent, khởi động lại; ứng dụng chưa kiểm chứng ghi rõ |
| `AiUsageHistoryPage` | `GET /api/v1/me/ai-usage`, `GET /api/v1/ai/sources` | Đường dẫn "Kết nối AI › Lịch sử sử dụng AI"; chọn khoảng ngày. Thẻ: Token hôm nay và Hạn mức còn lại (Teacher, Subject Manager), API cá nhân (số yêu cầu trong kỳ), Agent MCP (số lượt agent gọi tool, token do agent tự báo). Tab nguồn Tất cả, Hạn mức trường, API cá nhân, Agent MCP; lọc trạng thái. Cột Thời gian, Tác vụ (loại tác vụ, hoặc "Tool: tên tool"), Nguồn · mô hình, Token vào / ra, Nguồn số liệu (Nhà cung cấp đo, Ước tính, Agent tự báo, Không có số liệu), Trạng thái; phân trang; ghi chú không lưu tên bài, tên sinh viên, nội dung |
| `AiConnectionsPanel`, `TeacherQuotaPanel` | - | Nút quay lại; "API AI của trường và hạn mức"; đường dẫn "Cài đặt › API AI của trường"; "Mỗi lần lưu được ghi vào Nhật ký audit · chỉ Quản trị viên"; tab Khóa và hạn mức, Usage. Mở từ thẻ hạn mức thì cuộn tới phần hạn mức |
| `InstitutionalKeysSection` | `/api/v1/admin/ai/connections/...` | Nút Thêm khóa; bảng Nhà cung cấp, Khóa (4 ký tự cuối), Mô hình, Kiểm tra gần nhất, Bật (công tắc: tắt vẫn giữ khóa, không dùng cho yêu cầu mới), Kiểm tra, Thay khóa, Thu hồi; `409` báo tải lại |
| `QuotaPolicySection` | `/api/v1/admin/ai/quota-policy` | "Hạn mức chung": Mỗi giảng viên / ngày*, Trần toàn hệ thống / ngày* (token); "Một mức áp cho mọi mô hình. Hết hạn mức, giảng viên vẫn dùng được API cá nhân hoặc agent MCP của mình. Đặt lại lúc 00:00."; Lưu hạn mức |
| `TeacherQuotaTable` | `/api/v1/admin/ai/teacher-quotas` | "Mức riêng từng giảng viên": ô tìm theo tên hoặc email, nút Thêm mức riêng; bảng Giảng viên (tên, email), Token / ngày ("0 · chặn"), Sửa; "Mức riêng thay cho mức chung của giảng viên đó. Đặt 0 để chặn dùng API trường." |
| `SharedUsageTab` | `/api/v1/admin/ai/usage-summary` | 7 ngày / 30 ngày; "Chỉ tính yêu cầu dùng API của trường"; thẻ Hôm nay (trên trần), tổng token trong kỳ, số giảng viên đã dùng; biểu đồ token theo ngày; bảng theo giảng viên (token trong kỳ, so với hạn mức ngày); bảng theo nhà cung cấp và mô hình (BR-U06-41); không có nội dung, khóa, usage API cá nhân hay MCP |
| `AiSourcePicker` | `GET /api/v1/ai/sources` | "Chọn nguồn AI" kèm dòng tác vụ · đích. Mỗi nguồn một lựa chọn: API của trường (chỉ Teacher, Subject Manager; mô hình do Quản trị viên cấu hình; trường chịu chi phí; còn x / y token hôm nay; ước tính), API cá nhân theo từng nhà cung cấp đã lưu (4 ký tự cuối; bạn chịu chi phí; không giới hạn bởi trường; ước tính); nguồn không dùng được hiện mờ kèm lý do. Không chọn sẵn, mỗi yêu cầu chọn một nguồn; ghi chú "Agent MCP không phải nguồn ở đây; agent tự gọi công cụ của nền tảng từ ứng dụng của bạn."; Hủy, Gửi yêu cầu. Lỗi hay hết hạn mức không tự đổi nguồn |
| `AiTaskStatus` | `GET /api/v1/ai/tasks/{id}`, `POST ...:cancel` | Hỏi mỗi 2 giây; hiện chờ, đang chạy, xong, lỗi, không rõ kết quả; Hủy khi còn chờ |

Route `/ai/*` chỉ cho Student, Teacher, Subject Manager (dùng `RoleGuard` của U01); panel Settings nằm trong route `/admin/*` của U03. Backend vẫn kiểm quyền thật.

## AI/MCP current flow — 2026-10-30

MCP credential dành ACTIVE Student/Teacher/Subject Manager; Admin bị từ chối. Nhiều institutional keys/provider; repository chọn ACTIVE tạo sớm nhất. Owner reveal ACTIVE MCP token khi có ciphertext; Admin reveal key chung không REVOKED (kể cả DISABLED) và ghi audit. Personal key không có readback cho Admin. Staff write tools có proposal/retry/confirmation theo source, không tự finalize/publish.

Credential HTTP: GET/POST `/api/v1/me/mcp-credentials`, PATCH/DELETE `/{id}`, GET `/{id}/token`. List DTO có revealable; popup chỉnh label/access không đổi token/hạn. Admin API key reveal là luồng riêng ở AiAdminController; không dùng để đọc personal key. Negative cases: wrong owner, Admin, expired/revoked, ciphertext thiếu và account/role đổi.
