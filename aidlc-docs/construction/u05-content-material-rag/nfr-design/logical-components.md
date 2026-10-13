# U05 Reference Content & Announcements — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 15, 30, 33, 34, 36, 54, 55. **Primary stories:** US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. **Requirements:** FR-004/005/011/023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u05-content-material-rag` không quyết định chức năng hiện hành.

## Component responsibility

| Layer | Responsibility |
|---|---|
| Controller / MCP adapter | Deserialize/validate, authenticated actor, minimal response; gọi domain service |
| Application service | Scope/state/version, orchestration qua ports, transaction owner |
| Domain policy/model | Rules, transitions, snapshot invariants |
| Repository | Owned entities only; optimistic/locking/unique constraints |
| Outbound adapters | Timeout/retry/provenance; không quyết định học vụ |
| UI | Entry UC, form/query status, không client-only security |

## Owner contract

attach/list/download references; announcements; optional Lesson extension

## Owned data

- ReferenceMaterial: id, subjectId/classId/assignmentId scoped target, fileId(U03), title/status/version, uploaderId/timestamps.
- ClassAnnouncement: id/classId/authorId/title/content/important/sendEmail/version/status/timestamps; event chứa id/classId/flags/version, không raw confidential data.
- Lesson/embedding entities chỉ migration extension riêng nếu approved; không required relationship trong core.

## Critical paths

- Attach reference → owner/scope + U03 validation → metadata persist; view/download tái kiểm scope.
- Create important announcement → commit → U03 outbox → U16 resolves current recipients/preferences → email/in-app.
- MCP reader → resource access → file excerpt/data minimum; advanced retrieval chỉ khi extension đã chốt/provider cấu hình riêng.
