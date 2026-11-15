# U04/U13/U14 — áp dụng quy ước mã nguồn: câu hỏi xác nhận

Yêu cầu 2026-11-15: "sửa giúp tôi" sau lần rà soát U04/U13/U14 theo [code-conventions.md](../../construction/code-conventions.md) (kết quả rà soát ghi trong `aidlc-docs/audit.md`, mục "Rà soát quy ước mã nguồn U04/U13/U14").

Đây là refactor đặt tên và trình bày, không đổi hành vi. Quy ước §1 không cho đổi URL, HTTP method, JSON, schema, event, MCP tool hay public `contract` trong thay đổi đặt tên. Code của U04 có 32 file ở unit khác dùng contract `subjects.contract`; frontend có 9 file ngoài feature dùng các feature này.

Người dùng trả lời trong chat lúc 2026-11-15T09:27:37Z: "4 câu đầu theo khuyến nghị, câu 5 thực hiện trên nhánh fix/code-conventions-u04-u13-u14" (nguyên văn trong `aidlc-docs/audit.md`).

## Question 1
Phạm vi sửa?

A) Toàn bộ vi phạm trong U04/U13/U14: backend main, test backend, frontend và test frontend; chỉ đổi tên nội bộ, giữ contract/URL/JSON. (Khuyến nghị)

B) Chỉ phần code viết sau 2026-11-05 (ngày quy ước có hiệu lực): `MaterialRepositoryAdapter`, `RecipientResolver.proposalLink`, `materialApi.update`.

C) Toàn bộ vi phạm nhưng chỉ backend (main + test), chưa sửa frontend.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Mức đổi tên method controller/service/repository?

A) Đổi cả tên danh từ/HTTP verb (`detail`, `classes`, `get`, `put`, `unread`, `versions`…) và động từ đứng một mình thiếu đối tượng (`list`, `create`, `update`, `delete`, `edit`, `save` → `listSubjects`, `createSubject`, `deleteAnnouncement`…). (Khuyến nghị; đúng mẫu "động từ + đối tượng" của §4)

B) Chỉ đổi tên danh từ/HTTP verb; giữ các động từ đứng một mình như `list`, `create`, `update`, `delete`.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Public contract của U04 (`ISubjectQuery.getSubject`, `ISubjectClasses`, `ISubjectMutationGuard.lockActive`) mà 32 file ở unit khác đang dùng?

A) Giữ nguyên tên contract; chỉ đổi tên nội bộ. (Khuyến nghị; §1 yêu cầu xử lý contract trong thay đổi riêng)

B) Đổi tên contract và sửa đồng bộ caller ở các unit khác.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Persistence JPA còn lại (U04 subjects, module học liệu của U13, toàn bộ U14)?

A) Ngoài phạm vi; giữ JPA, chuyển JDBC trong thay đổi riêng như persistence-conventions.md đã ghi. (Khuyến nghị)

B) Chuyển luôn sang JDBC trong thay đổi này.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Nhánh làm việc (hiện đang ở `staging`)?

A) Tạo nhánh mới `refactor/u04-u13-u14-conventions` từ `staging`; không commit/push khi chưa được yêu cầu. (Khuyến nghị)

B) Sửa trực tiếp trên `staging`.

X) Other (please describe after [Answer]: tag below)

[Answer]: X — Thực hiện trên nhánh `fix/code-conventions-u04-u13-u14` (đã có sẵn, cùng commit với `staging` `d5c32804`).
