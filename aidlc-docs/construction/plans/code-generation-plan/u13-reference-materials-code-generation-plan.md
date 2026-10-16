# U13 Reference Materials — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Thiết kế: [business-logic-model](../../u13-reference-materials/functional-design/business-logic-model.md), [business-rules](../../u13-reference-materials/functional-design/business-rules.md), [domain-entities](../../u13-reference-materials/functional-design/domain-entities.md), [frontend-components](../../u13-reference-materials/functional-design/frontend-components.md), [NFR](../../u13-reference-materials/nfr-requirements/nfr-requirements.md), [NFR design](../../u13-reference-materials/nfr-design/nfr-design-patterns.md), [infrastructure](../../u13-reference-materials/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/materials/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../materials/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/materials/slots.ts` | Slot đã khai báo: `learningMaterialSections` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/materials/V20260925_1900__create_material_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `Material`, `MaterialVersion` và enum; repository chỉ dùng trong package.
- [ ] **Bước 3 — Quản lý:** `MaterialService` (F2, F4, P1).
- [ ] **Bước 4 — Xem:** `MaterialQueryService`, `MaterialAccessPolicy` (F1, F3, F5, P3).
- [ ] **Bước 5 — Trích chữ:** `MaterialTextExtractJobHandler`, `MaterialTextExtractor`, thêm `MATERIAL_TEXT_EXTRACT` vào loại việc của U03 (F6, P2).
- [ ] **Bước 6 — Nguồn cho AI:** `ReferenceMaterialService` (cài `IReferenceMaterial` của U06, thay adapter rỗng), `MaterialMcpTools` (tool `read_authorized_material`) (F7).
- [ ] **Bước 7 — API:** controller theo domain-entities mục 4; `contracts/openapi/materials.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 8 — Frontend:** `SubjectMaterialListPage` (đăng ký `DashboardNavEntry`), `MaterialDetailPage`, `ClassMaterialsTab` (`TeacherClassTab`), `ClassMaterialsCard` (`StudentClassSection`), `LearningMaterialPage` với slot `LearningMaterialSection`; `MaterialLinkList` cài slot `AssignmentMaterialSection` (U08), `QuizMaterialSection` (U11).
- [ ] **Bước 9 — Unit test (người code viết):** quyền theo phạm vi (Chủ nhiệm môn, giảng viên, người học, Admin); link YouTube hợp lệ và không hợp lệ (playlist, domain khác); thay phiên bản giữ phiên bản cũ; lưu trữ ẩn với người học; trích chữ `EXTRACTED`, `NO_TEXT`; `extractText` kiểm lại quyền và cắt theo trần; tool MCP từ chối ngoài quyền; component frontend. Integration test do tester viết.
- [ ] **Bước 10 — Review:** đối chiếu UC 58–62, story, BR-U13 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Tải tệp qua token; token hết hạn bị từ chối.
- Thay phiên bản khi job trích chữ phiên bản cũ đang chạy: kết quả gắn đúng phiên bản.
- U07, U10 dùng `extractText` với tài liệu ngoài quyền: bị từ chối.
- Phân quyền 4 role trên mọi endpoint, gồm người học lớp khác cùng môn mở tài liệu của lớp.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
