# Loại sơ đồ Context, DFD, Communication — phân tích yêu cầu (nháp)

**Ngày:** 2026-11-20
**Trạng thái:** chờ người dùng trả lời [câu hỏi](2026-11-20-diagram-types-questions.md) và GVHD duyệt phạm vi. **Chưa sửa code.**
**Nguồn:** [đối chiếu góp ý và Syllabus](2026-11-20-lecturer-feedback-syllabus.md), mục 5.

## 1. Phân tích yêu cầu

- **Yêu cầu:** hệ thống nhận đúng các loại sơ đồ mà hai môn yêu cầu: Context và DFD (SWR302: LAB, chương 12), Communication diagram (SWD392: COMET).
- **Loại:** Enhancement của U07 Assignment Authoring, có hiển thị ở U08, U09 và tài liệu dùng chung.
- **Độ phức tạp:** vừa. Không cần migration, nhưng chạm vào một quyết định đã chốt ngày 2026-10-22 (chèn sơ đồ không chọn loại, hệ thống tự đoán từ hình vẽ).
- **Độ sâu:** Standard.

## 2. Hành vi hiện tại (theo code)

1. Khi chèn, sơ đồ mới luôn có loại `OTHER` (`frontend/src/shared/document/types.ts:185`).
2. **Mỗi lần lưu**, backend đoán loại từ shape draw.io (`DiagramXml.detectType`). Đoán ra loại khác `OTHER` thì ghi đè loại gửi lên; không đoán được thì giữ loại gửi lên (`DocumentModelService.java:665-674`).
3. Loại sơ đồ được dùng ở bốn chỗ:
   - nhãn hiển thị (`DiagramViewPage`, alt của `DocumentViewer`, mô tả trong DOCX export);
   - đếm `requiredDiagrams` khi nộp. Sơ đồ `OTHER` thừa được tính bù cho loại còn thiếu (`DocumentModelService.java:201-221`);
   - văn bản gửi AI `[Sơ đồ <loại> …]` (`DocumentModelService.java:317`) và `requiredDiagrams` trong prompt chấm, prompt soạn đề và output MCP;
   - kiểm hợp lệ khi lưu: loại lạ báo `DIAGRAM_TYPE_INVALID` (`DocumentModelService.java:486-491`).
4. `requiredDiagrams` lưu trong cột jsonb `assignments.required_diagrams`, không có CHECK constraint. **Giao diện giảng viên và MCP hiện không đặt được giá trị này.** `AssignmentSettingsPanel` chỉ chuyển tiếp giá trị cũ. Muốn đặt thì phải gọi REST trực tiếp.
5. Hệ thống nhận sai loại như sau:

| Sơ đồ | Hiện nhận thành |
|---|---|
| Context diagram kiểu Wiegers (SWR302): một hình tròn hệ thống và các thực thể ngoài | `OTHER` |
| DFD (hình tròn hoặc chữ nhật bo góc, kho dữ liệu, mũi tên) | `OTHER` |
| Swimlane diagram không có hình thoi hay ký hiệu bắt đầu/kết thúc | `OTHER` (có thì thành `ACTIVITY`) |
| State-transition kiểu Wiegers không có ký hiệu bắt đầu/kết thúc | `OTHER` |
| Context diagram kiểu COMET (lớp «software system» và các lớp «external…») | `CLASS` |
| Communication diagram (COMET) | `OTHER` |

## 3. Phương án

| | A. Chỉ tự nhận diện | B. Tự nhận diện, có ô sửa tùy chọn (khuyến nghị) |
|---|---|---|
| Thao tác | Như hiện tại: không chọn gì và không sửa được | Không bắt buộc chọn. Hệ thống tự nhận diện như A; khi đoán sai, người vẽ sửa ở một ô chọn loại. Loại đã sửa tay được giữ, các lần lưu sau không đoán đè |
| Độ chính xác | Thấp với Context và DFD mức 0: cùng loại shape, chỉ khác số process. Communication phải đoán theo nhãn `1:`, `2:`… | Như A khi không sửa; đúng theo người vẽ khi đã sửa |
| Quyết định 2026-10-22 | Giữ | Giữ phần "chèn không bắt buộc chọn loại", chỉ thêm một ô sửa tùy chọn |
| Thay đổi code | `DiagramType`, `DiagramXml.detectType`, nhãn frontend và i18n | Như A, thêm ô sửa loại ở block sơ đồ, một cờ "đã sửa tay" trong nội dung block, và đổi ưu tiên ở `DocumentModelService.java:665-674` |

## 4. Ảnh hưởng tới hành vi hiện tại

### Không đổi
- Không có migration. Giá trị cũ trong jsonb vẫn hợp lệ vì chỉ thêm giá trị vào enum.
- Rubric, chấm điểm, công bố điểm, sổ điểm, quyền truy cập và MCP tool name giữ nguyên.
- Bài đã nộp giữ loại đã lưu. Việc đoán lại chỉ chạy khi lưu bản nháp.

### Có đổi
1. **Nhãn sơ đồ cũ:** với phương án A, sơ đồ đang là `OTHER` có thể đổi sang loại mới ở lần lưu kế tiếp. Nhãn hiển thị và nhãn gửi AI đổi theo. Phương án B cũng vậy, trừ sơ đồ người vẽ đã sửa tay loại.
2. **Đếm `requiredDiagrams`:** một sơ đồ trước là `OTHER` (được tính bù cho loại thiếu) nay thành `DFD` thì không bù nữa, nên bài có thể bị báo `REQUIRED_DIAGRAMS_MISSING`. Rủi ro thấp vì giao diện không đặt được `requiredDiagrams`. Cần kiểm DB xem có bài nào đang đặt giá trị khác `{}` (chưa kiểm).
3. **Hợp đồng API và MCP:** response có thể chứa giá trị mới (`CONTEXT`, `DFD`, `COMMUNICATION`). Frontend phải triển khai cùng lúc với backend. Agent MCP nhận chuỗi mới trong `requiredDiagrams`.
4. **Không rollback đơn giản được:** khi đã lưu loại mới, bản cũ sẽ báo `DIAGRAM_TYPE_INVALID` lúc lưu. `AssignmentRepository` cũng không đọc được `required_diagrams` chứa key mới, vì Jackson không ánh xạ được enum lạ.
5. **Prompt AI:** AI thấy `[Sơ đồ DFD …]` thay cho `[Sơ đồ OTHER …]`. Kết quả chấm có thể khác chút ít; nên chạy lại bộ đánh giá AI chấm sau khi đổi.

### Phạm vi file dự kiến
- Backend: `assignments/contract/DiagramType.java`, `assignments/service/DiagramXml.java`, `assignments/service/DocumentModelService.java`; test `DocumentModelServiceTest` và test của `DiagramXml`.
- Frontend: `shared/document/types.ts` (kiểu, nhãn, mặc định), editor block sơ đồ (chỉ phương án B), `i18n/messages/{vi,en,ja}.json`; test `frontend/tests/shared/document/*`.

## 5. Ngoài phạm vi

- Nhập PlantUML hoặc Mermaid.
- Ô đặt `requiredDiagrams` trên giao diện giảng viên, trừ khi chọn ở câu hỏi Q4.
- Thư viện shape COMET hoặc DFD riêng trong draw.io.
