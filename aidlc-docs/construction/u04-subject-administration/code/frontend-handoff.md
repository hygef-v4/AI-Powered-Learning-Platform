# Bàn giao frontend U04 — Subject Administration

Ngày18/10/2026. Phạm vi: UC16–19, US-CAT-001 phần quản trị môn của Admin. Các màn và luồng frontend U04 đã có code và developer tests, sẵn sàng bàn giao để review/tích hợp. **Chưa phải nghiệm thu end-to-end của unit:** còn account search, RoleGuard/session/CSRF U01 và provider backend liên unit cần bàn giao thật. Không triển khai lớp/ghi danh của U05 hoặc authentication/OTP/email của U01.

## 1. Những gì đã có

| Màn/luồng | File trong repository root | Rule và tiêu chí bàn giao |
|---|---|---|
| Danh sách `/admin/subjects` | `frontend/src/app/admin/subjects/page.tsx`; `frontend/src/features/subjects/SubjectListPage.tsx`, `SubjectFilters.tsx`, `SubjectTable.tsx` | UC16/BR50: tìm code/name debounce300ms, ACTIVE/ARCHIVED + counts từ API, page/size20–100, loading/empty/error/retry. Unknown lớp khác0; contact không tra được khác chưa phân công. |
| Thêm môn a06 | `frontend/src/features/subjects/SubjectFormDialog.tsx`, `ManagerPicker.tsx`, `managerSearch.ts` | UC17/BR02/03/06/53: code chuẩn hóa/duy nhất do server kiểm, name/description bounds, optional managerId/null, duplicate dưới code, giữ draft khi lỗi, không double POST. Form thành công mới đóng/refetch. |
| Chi tiết `/admin/subjects/[id]` | `frontend/src/app/admin/subjects/[id]/page.tsx`; `frontend/src/features/subjects/SubjectDetailPage.tsx`, `SubjectInfoTab.tsx`, `ManagerCard.tsx`, `SubjectMetaCard.tsx` | UC18: params async Next16, breadcrumb/back, trạng thái/contact/meta, tab thông tin/lớp. Đổi ID remount cả draft và state, response cũ không tác động môn mới. |
| Sửa thông tin | `frontend/src/features/subjects/SubjectEditForm.tsx` | UC19/BR02/06/Q10: code readonly, sửa name/description, cancel bỏ draft, trim name/giữ description, gửi version đang hiển thị. Môn archived vẫn sửa metadata được theo quyết định Q10. |
| Đổi/bỏ chủ nhiệm a07c | `frontend/src/features/subjects/ManagerChangeDialog.tsx` | UC19/BR03/10/11: current contact, radio assign/unassign, đổi chế độ xóa lựa chọn cũ, explicit null khi bỏ, cùng version. Lookup unavailable không cản bỏ phân công. Assign thật chờ adapter U01; không cho submit assign khi chưa chọn. |
| Lưu trữ/mở lại | `frontend/src/features/subjects/SubjectStatusActions.tsx` | UC19/BR04/07: confirmation trước PUT, không optimistic success; ACTIVE→ARCHIVED hoặc ngược lại cùng version, reopen không sửa lớp. Chỉ sau server success mới refetch snapshot và thay nút. |
| Archive bị chặn a07d | `frontend/src/features/subjects/ArchiveRejectedDialog.tsx` | BR04/Q07: đúng code SUBJECT_HAS_UNARCHIVED_CLASSES409 mới mở blocker dialog; tổng từ unarchivedCount, OPEN+DRAFT, partial list không giả count, malformed/thiếu list không thành0. Bảng chỉ đọc, không link lớp/student count theo Q07. |
| Lớp của môn a07b | `frontend/src/features/subjects/SubjectClassesTab.tsx`, `SubjectClassesTable.tsx` | UC18/BR05/Q07: lazy load, status/term filters, tổng không đổi theo filter, unavailable khác verified empty. Không thêm/sửa/xóa hoặc mở chi tiết lớp. |
| Mutation/focus/errors | `frontend/src/features/subjects/subjectMutation.tsx`, `useSubjectRead.ts`; `frontend/src/shared/ui/Dialog.tsx` | Chặn submit trùng bằng ref, giữ draft khi lỗi, safe error không raw provider detail, không tự retry mutation. Dialog focus/return focus/scroll, khóa close/cancel/Escape/backdrop khi pending. |

Các wrapper API ở `frontend/src/features/subjects/api.ts`, DTO ở `types.ts`, validation ở `validation.ts`; menu dùng đăng ký cũ trong `register.ts`. Không thêm dependency, menu trùng hoặc shared auth flow mới.

## 2. Một nguồn version cho các thao tác

Detail chỉ cho mở một editor/dialog tại một thời điểm; khóa các action khác và đổi tab khi đang sửa. Mỗi command gửi đúng version từ GET detail hiện tại. Success response chứa snapshot đã lưu; frontend báo “Đã lưu”, đóng editor rồi GET lại detail/contact/counts và invalidates classes trước thao tác sau. Không dùng manager=null/classCount=null của command response làm kết luận chưa phân công/0 lớp.

Nếu GET sau success lỗi, vẫn báo command đã lưu; retry chỉ GET, không gửi lại PATCH/PUT. Nếu lỗi409 VERSION_CONFLICT/DATA_CONFLICT, giữ draft và khóa submit; người dùng phải bấm “Tải lại dữ liệu và bỏ bản nháp” hoặc Hủy. Không tự lấy version mới để ghi lại draft cũ. Lỗi503 classes không bị hiểu nhầm thành còn lớp hoặc0 lớp. Mutation401/403 báo parent che privileged detail và đóng editor; retry phải đọc lại quyền/dữ liệu. Đổi subjectId trong khi command chờ không cho callback cũ đổi UI môn mới.

## 3. Contract U04 dùng bởi frontend

| Request | Payload/query | Dữ liệu quan trọng |
|---|---|---|
| GET `/api/v1/admin/subjects` | q/status/page/size, fixed code sort | items, page, size, totalElements, statusCounts.ACTIVE/ARCHIVED; count null là unavailable |
| POST cùng path | code/name/description/managerId | SubjectDetail snapshot; không gửi actor/role từ client |
| GET `/{id}` | UUID | SubjectDetail gồm version, managerId/manager, classCount, dates |
| PATCH `/{id}` | name/description/version | Không code/status/managerId |
| PUT `/{id}/manager` | managerId UUID hoặc null, version | Bỏ phân công phải có field managerId:null |
| PUT `/{id}/status` | status ACTIVE/ARCHIVED, version | Không giả archive thành công khi thiếu provider lớp |
| GET `/{id}/classes` | UUID | availability AVAILABLE + items, hoặc UNAVAILABLE + items:null |

Lỗi dùng shared ApiError/ProblemDetail; phân nhánh theo HTTP status + code. Không coi mọi409 là archive rejection. Các code consumer chính: SUBJECT_CODE_DUPLICATE, SUBJECT_MANAGER_INVALID, SUBJECT_VERSION_CONFLICT, SUBJECT_DATA_CONFLICT, SUBJECT_HAS_UNARCHIVED_CLASSES, SUBJECT_CLASSES_UNAVAILABLE. Không render raw detail/SQL/stack trace từ provider.

## 4. Các điểm cần người nhận nối và xác nhận

| Phụ thuộc | Hiện trạng local | Việc bàn giao còn cần |
|---|---|---|
| U01 account search | `managerSearch.ts` default ném503 ngay; picker không có account giả | Xác nhận endpoint/query/paging/schema. Thiết kế dự kiến GET admin/accounts role SUBJECT_MANAGER + status ACTIVE + q, tối đa20. Cài adapter dùng shared apiFetch, map response thành ManagerCandidate; filter quyền/role/status ở server. Không đoán items/content hoặc tải toàn bộ accounts. Test assign với tài khoản thật và disable trong khi picker đang mở. |
| U01 RoleGuard/current user | Hook hiện chỉ có displayName/email; admin layout hard-code ADMIN; U04 routes không có guard giả | Owner cung cấp loading/authenticated/unauthenticated/role thật và guard dùng chung. Bọc route/layout theo contract đó; non-Admin/anonymous direct URL không hiển thị dữ liệu. Backend vẫn quyết định quyền ở từng request. |
| U01 session/JWT/CSRF | apiFetch gửi credentials:include, không tự tạo token/refresh/CSRF flow U04 | Chốt shared auth transport và CSRF header/token. SecurityConfig mặc định yêu cầu authentication/CSRF; các mutation thật có thể bị403 cho tới khi shared flow sẵn sàng. Không tắt CSRF hoặc permitAll để demo. |
| U04 backend + U01/U02/U05 | REST U04 có code; account assignment guard/U05 checker/writers/durable audit/schema còn gate | Tester chạy backend có schema/provider thật, account scope thay đổi ngay, archive race, audit transaction/dedup; fake UI hoặc mock Actor không thay bằng chứng này. |

Props searchManagers hỗ trợ inject boundary cho component test và kết nối giữa Client Components; không truyền function qua Server Component route. OTP/email vẫn thuộc U01; UC16–19 không tự tạo OTP hoặc email job.

## 5. Chạy và kiểm lại khi nhận code

Tại `frontend`, dùng package-lock hiện có; nếu chưa có dependency thì `npm.cmd ci`. Các lệnh kiểm:

```powershell
npm.cmd run test -- --maxWorkers=4
npm.cmd run typecheck
npm.cmd run lint
npm.cmd run build
```

`--maxWorkers=4` chỉ giới hạn tài nguyên chạy test, giữ nguyên assertion và timeout. Để dev, cấu hình BACKEND_URL trỏ tới backend được nhóm bàn giao rồi chạy `npm.cmd run dev`; production dùng proxy/Nginx hiện có, không nhúng fixture hoặc tài khoản mẫu vào bundle. Không sửa/ghi đè file .env của thành viên khác.

Checklist nhận bàn giao:

1. UC16: search nhanh liên tiếp, đổi status/size/page,0 kết quả, GET503/retry, manager contact unknown và count null khác0; menu/refresh cả hai URL hoạt động.
2. UC17: tạo không manager, có manager thật sau nối U01, mã trùng/sai, tên trắng, description bounds; cancel không POST, pending không gửi trùng/đóng dialog.
3. UC19 edit: save/cancel; mở hai tab cùng môn, tab A save rồi tab B save phải409, draft B giữ nguyên đến khi chọn reload. Không tự overwrite.
4. UC19 manager: A→B→null, switch assign/unassign/assign phải chọn lại, archived cũng sửa được; disable/đổi role tài khoản giữa lookup và confirm phải bị server từ chối. Session người cũ mất R2 ở request mới là integration test backend.
5. UC19 status: archive môn chỉ có lớp archived hoặc0 lớp; reject OPEN/DRAFT có đúng tổng/bảng; unavailable khác0; version conflict khác blocker; reopen giữ lớp nguyên trạng. Không báo thành công trước response.
6. Dùng Tab/Shift+Tab/Escape; pending khóa nút/close/backdrop; đóng dialog hoặc save/refetch xong trả focus về action mở. Bảng dài scroll bên trong dialog, footer dùng được.
7. Sau PUT/PATCH success, làm GET kế tiếp lỗi: phải có thông báo đã lưu và retry GET, không mutation lần nữa. Mutation401/403 phải che detail. Đổi ID lúc request cũ chờ không lẫn dữ liệu/draft.
8. Sau tích hợp U01, tester kiểm mọi role/account state, JWT/session/CSRF thật; không dùng layout hoặc fixture làm bằng chứng quyền.

## 6. Bằng chứng và giới hạn

Lượt thêm F05/F06/F08 có32 test mới:9 edit,6 manager dialog,11 status/rejection,6 detail mutation integration; baseline92, tổng124 test/24 file. Đã pass typecheck/lint/build; báo cáo cuối và các lần thử nằm ở audit/state. Một lượt chạy test song song Next dev bị timeout hai case; chạy lại sau dừng server với4 worker, không đổi assertion/timeout. Trước commit đã chạy lại Maven verify:278 test PASS,31 PostgreSQL thật trong DB fixture riêng,0 lỗi/skip; jar build thành công và container test đã dọn. Provider/auth thật vẫn là gate riêng, không suy từ fixture DB.

Browser desktop với API fixture loopback riêng đã kiểm edit success, hai phiên xung đột giữ draft rồi reload, bỏ chủ nhiệm, archive/reopen đổi trạng thái/version/nút, archive reject OPEN+DRAFT và tab lớp readonly. Đây là QA UI, không phải backend/provider/auth thật. Chưa chứng nhận mobile/cross-browser hoặc NFR p95. Shared shell hiện theo bố cục desktop có sidebar cố định; responsive tổng thể cần kiểm với owner shell trước khi nhận tiêu chí mobile.

Ảnh QA được gửi riêng, không đưa vào Git: `u04-version-conflict.png`, `u04-archived-detail.png`, `u04-archive-rejected.png`. Fixture/script/metadata/server/tab được dọn sau kiểm, không đưa vào production. Review chi tiết trước: [frontend đọc](frontend-read-review.md), [form thêm](create-form-review.md), [API backend](api-review.md).

Người dùng đã yêu cầu tạo commit/push sau khi nhận bàn giao code. Quyết định cần giữ đã chuyển sang [implementation-decisions](implementation-decisions.md); các file TEMP được loại khỏi tập thay đổi xuất bản và giữ bản sao cục bộ ignored. Không cập nhật Jira hoặc nhắn thành viên. Các checklist code local không thay acceptance của tester hoặc phê duyệt whole unit.

## 7. Phạm vi kiểm soát

SECURITY-05/15: validation/whitelist payload/errors an toàn,409 và unavailable phân biệt; SECURITY-08: server quyết định quyền, mutation401/403 che detail. SECURITY-03/09 không log secret/PII, không provider production giả hoặc default allow. SECURITY-12/04 auth/header/CSRF tích hợp còn pending và không thay ở U04. RESILIENCY-10 chặn gửi trùng, không auto retry mutation, bỏ callback/response cũ, xử lý GET fail sau commit; provider deadline/p95 chưa chứng nhận. RESILIENCY-04/06 không đổi deploy/infra/health. PBT disabled; rule ngoài subset cấu hình N/A.
