# U06 AI Connections — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Kết nối API (UC 31; API chung trong UC 14, 15)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-01 | Có hai loại kết nối: `INSTITUTIONAL` (API chung, chỉ Admin cấu hình qua Settings) và `PERSONAL` (API cá nhân, chỉ chủ tài khoản Student, Teacher, Subject Manager cấu hình trong UC 31). Admin không có API cá nhân, không xem hay dùng kết nối cá nhân của người khác. | FR-012, FR-021 |
| BR-U06-02 | Provider: `GEMINI`, `OPENAI`, `CLAUDE`. Mỗi người tối đa một kết nối cá nhân còn hiệu lực cho mỗi provider; API chung tối đa một kết nối cho mỗi provider. Mỗi kết nối chọn một model trong danh sách cho phép do server cấu hình. Địa chỉ gọi provider cố định trong code; không nhận URL từ người dùng. | FR-012 |
| BR-U06-03 | Key được mã hóa trước khi lưu, kèm phiên bản khóa mã hóa; chỉ hiện 4 ký tự cuối. Không trả key hay bản mã qua API, không ghi vào log hay audit. Thay key thì ghi đè; thu hồi thì xóa bản mã và chuyển `REVOKED`. | FR-012, SEC-006 |
| BR-U06-04 | Kiểm tra kết nối gửi một yêu cầu tổng hợp cố định, tối đa 5 token ra, không dùng nội dung học vụ; UI báo trước việc này có thể phát sinh chi phí trên key. Lưu kết quả đã làm sạch và thời điểm. Kiểm tra API chung không cho Admin quyền học vụ. | FR-037, US-AIG-003 |
| BR-U06-05 | Kết nối API chung có trạng thái `ACTIVE`, `DISABLED` (công tắc tắt, giữ key), `REVOKED`. Đổi, tắt, thu hồi có hiệu lực với yêu cầu AI mới; task đã gửi tới provider vẫn chạy xong. | FR-021 |
| BR-U06-06 | Thay key hoặc model phải gửi kèm `version`; lệch thì `409`. | FR-033 |

## 2. Nguồn AI và quota API chung (US-AIC-003)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-10 | Mỗi yêu cầu AI chọn rõ một nguồn: `SHARED` (API chung) hoặc `PERSONAL` (kết nối cá nhân của chính người gọi). Lỗi hay hết quota không tự chuyển sang nguồn khác. | FR-034, FR-036 |
| BR-U06-11 | `SHARED` chỉ cho Teacher và Subject Manager đang `ACTIVE`; Student và Admin không dùng. Nguồn nào cũng không thêm quyền học vụ: unit gọi tự kiểm quyền trên tài nguyên trước khi gửi task. | FR-021, US-AIG-002 |
| BR-U06-12 | Quota tính bằng token theo ngày (Asia/Ho_Chi_Minh), không cộng dồn sang ngày sau; không có giới hạn theo giờ. Admin đặt mức mặc định mỗi ngày cho mọi giảng viên, trần ngày toàn hệ thống, và mức riêng mỗi ngày cho từng giảng viên (0 là chặn). Một mức áp cho mọi model; usage vẫn ghi provider, model riêng. Chưa đặt mức mặc định và trần thì chưa dùng được `SHARED`. | FR-034, D04 chốt 2026-10-15 |
| BR-U06-13 | Trước khi gửi, giữ chỗ ước tính (số ký tự prompt chia 2, làm tròn lên, cộng `maxOutputTokens`) trên cửa sổ ngày toàn hệ thống và cửa sổ ngày của người gọi, trong một transaction. Vượt mức nào thì từ chối, báo thời điểm có cửa sổ mới và gợi ý chọn nguồn khác. | FR-034 |
| BR-U06-14 | Provider trả phản hồi có usage: thay phần giữ chỗ bằng token thật, kể cả khi kết quả không dùng được. Provider từ chối không tính token (key sai, quá giới hạn tần suất, model không hỗ trợ): giải phóng giữ chỗ. Không biết kết quả (hết giờ hoặc mất kết nối sau khi đã gửi): tính phần giữ chỗ là đã dùng với nhãn `ESTIMATED`, không tự hoàn. | FR-034, FR-036 |
| BR-U06-15 | Token thật vượt ước tính vẫn ghi đủ; cửa sổ có thể vượt mức và yêu cầu sau bị chặn tới cửa sổ mới. | FR-034 |
| BR-U06-16 | API cá nhân và MCP không trừ quota chung. Quota không mua bán, không quy đổi ra tiền. | FR-034 |
| BR-U06-17 | Mỗi tài khoản tối đa 3 task đang chờ hoặc đang chạy, tính mọi nguồn. | Chống quá tải worker |

## 3. Task AI cho unit khác

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-20 | Unit gọi (U07, U08, U09, U10) gửi task gồm loại task, đích (`targetRef`, `inputVersion`), nguồn, prompt đã lọc, JSON schema của kết quả, `maxOutputTokens`, `requestRef`. Unit gọi tự kiểm quyền trên đích và chỉ đưa ngữ cảnh tối thiểu (không đáp án ẩn, điểm chưa công bố, dữ liệu người khác). U06 không đọc dữ liệu của unit khác. | FR-012, FR-035 |
| BR-U06-21 | `requestRef` duy nhất. Gửi lại cùng `requestRef` và cùng dấu vân tay (người gọi, loại task, đích, `inputVersion`, nguồn) thì trả task cũ; khác dấu vân tay thì `409`. | FR-034 |
| BR-U06-22 | Trạng thái task: `QUEUED → RUNNING → READY / FAILED / UNKNOWN`; `QUEUED → CANCELLED / FAILED`. Ghi mốc đã gửi trước khi gọi provider; message đến lại cho task đã có mốc gửi mà chưa có kết quả thì chuyển `UNKNOWN`, không gọi provider lần hai. | FR-036 |
| BR-U06-23 | Kết quả phải đúng JSON schema, sai thì `FAILED` (token đã dùng vẫn tính). Kết quả đúng được chuyển cho handler của unit gọi; handler kiểm lại quyền và `inputVersion` rồi mới áp dụng; đích đã đổi thì không áp dụng (`STALE`). AI không tự công bố bài, không chốt điểm. | FR-012, US-AIG-001 |
| BR-U06-24 | Prompt và kết quả chỉ giữ tới khi task kết thúc và đã chuyển cho handler; sau đó xóa, chỉ giữ thông tin task và usage. | SEC-005 |
| BR-U06-25 | Gọi provider có timeout 120 giây. U06 không tự gọi lại provider; người dùng muốn thử lại thì tạo yêu cầu mới. | FR-036 |
| BR-U06-26 | Người gửi được hủy task `QUEUED` của mình; giữ chỗ được giải phóng. | Thiết kế |
| BR-U06-27 | Trước khi gửi, UI hiện nguồn, bên chịu chi phí (trường hoặc chính người dùng), quota còn lại nếu dùng API chung và số token ước tính có nhãn "ước tính". | FR-036 |

## 4. MCP (UC 32)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-30 | Student, Teacher, Subject Manager tạo credential MCP cho agent của mình; Admin không. Credential gắn tài khoản, nhãn, quyền `READ_ONLY` hoặc `READ_WRITE`, hạn 7, 30 hoặc 90 ngày (mặc định 30); tối đa 5 credential còn hiệu lực mỗi người. | FR-035 |
| BR-U06-31 | Token chỉ hiện một lần lúc tạo; lưu bản băm SHA-256 và 4 ký tự cuối. Credential hết hiệu lực khi hết hạn, bị thu hồi, tài khoản không còn `ACTIVE`, hoặc `credentialVersion` của tài khoản đã đổi (đổi mật khẩu, đổi role); có hiệu lực ở lời gọi tiếp theo. | FR-035, BR-U01-63 |
| BR-U06-32 | MCP server dùng transport Streamable HTTP tại `/mcp`, xác thực bằng header `Authorization: Bearer`. Không dùng key provider để xác thực, không trả key cho agent. MCP không gọi AI thay agent: agent dùng mô hình của nó. | FR-012, FR-035 |
| BR-U06-33 | Mỗi lời gọi tool: xác thực credential; dựng actor từ tài khoản và `credentialVersion` lưu trong credential; tool ghi cần `READ_WRITE`; tham số đúng schema và không quá 64 KB; tối đa 60 lời gọi mỗi phút mỗi credential; unit sở hữu tool kiểm quyền qua `AuthorizationPort` và kiểm `expectedVersion` khi ghi; kết quả không quá 256 KB. | FR-035 |
| BR-U06-34 | Tool do unit sở hữu dữ liệu đăng ký, kèm role được dùng. Không có tool nộp bài chính thức, công bố điểm, khôi phục dữ liệu, đọc credential. Tham số và kết quả coi là không tin cậy. | FR-035 |
| BR-U06-35 | Usage MCP ghi tên tool, kết quả, thời điểm; token là `UNAVAILABLE`, chỉ ghi `EXTERNALLY_REPORTED` khi agent tự gửi số token trong `_meta` của lời gọi. Không ghi 0 thay cho không biết. | FR-036, US-AIC-002 |

## 5. Lịch sử sử dụng (UC 33)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-40 | Mỗi người xem usage của chính mình: thời điểm, nguồn (`SHARED`, `PERSONAL`, `MCP`), provider và model khi biết, loại task hoặc tên tool, trạng thái, token vào và ra kèm nhãn số liệu (`PROVIDER_MEASURED`, `ESTIMATED`, `EXTERNALLY_REPORTED`, `UNAVAILABLE`). Không có số thì hiện "Không có số liệu". Teacher, Subject Manager thấy quota chung còn lại hôm nay. Không hiện key, prompt hay nội dung. | FR-036, UC 33 |
| BR-U06-41 | Admin xem tổng hợp usage API chung trong phần quota của Settings: theo giảng viên (đã dùng hôm nay, mức), theo ngày, theo provider và model. Không có nội dung, key; không xem usage API cá nhân hay MCP của từng người. | FR-021, FR-036, UC 33 |

## 6. Hướng dẫn, phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U06-50 | Mỗi provider có hướng dẫn lấy key và chọn model; MCP có hướng dẫn cho từng ứng dụng agent đã thử thật (cài đặt, xác thực, thử tool, ngắt kết nối, xử lý lỗi); ứng dụng chưa thử ghi "chưa kiểm chứng". Không hướng dẫn dán key vào prompt. | FR-037 |
| BR-U06-51 | Mọi API kiểm quyền phía server: API Settings chỉ Admin; kết nối cá nhân, credential MCP, task và usage chỉ chủ sở hữu. Ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. | SEC-002 |
| BR-U06-52 | Audit: `AI_CONNECTION_CHANGED` (thêm, thay, tắt, bật, thu hồi; provider, loại, 4 ký tự cuối), `AI_CONNECTION_TESTED`, `QUOTA_CHANGED` (trước, sau), `MCP_CREDENTIAL_CREATED`, `MCP_REVOKED`, `MCP_TOOL_CALLED` (tool, đích, kết quả). Không ghi key, token, prompt. | FR-014, FR-035 |
| BR-U06-53 | U01 hoặc U03 không trả lời thì từ chối (fail closed). Lỗi AI không làm mất dữ liệu người dùng đang soạn; người dùng vẫn làm thủ công được. | BR-U01-93, US-AIG-001 |
