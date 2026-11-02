# Review cấu trúc code — 2026-10-30

**What this repo does:** Nền tảng học tập cho một trường/trung tâm, gồm tài khoản, môn/lớp, Assignment, Quiz, nhóm, học liệu, AI và thông báo. Hệ thống có 66 UC, 15 unit nghiệp vụ trong một backend Spring Boot và worker dùng chung code, cùng frontend Next.js. Quy mô đối chiếu là NFR-003: dưới 1.000 tài khoản, dưới 100 người dùng đồng thời; chưa đo tải thực tế.

**Snapshot:** HEAD `5a76e428`; working tree sạch trước review. Đây là review và đề xuất; chưa sửa production code, test, dependency, database hoặc cấu hình chạy.

## Phạm vi và tài liệu đối chiếu

- Khảo sát toàn bộ cây source: 683 file Java production; 225 file Java trong test; 412 file TS/TSX production và 94 file frontend test. Backend có 20 package nghiệp vụ cho 15 unit, thêm `shared`; frontend có 18 feature.
- Khảo sát cây 332 tài liệu Markdown AI-DLC; đọc sâu state, requirements, unit ownership/dependencies, decision register, source/contract/API inventories, traceability và báo cáo audit/remediation mới nhất. Đối chiếu quy ước repository/contract trong thiết kế các unit. Không đọc từng dòng mọi bản kế hoạch và bằng chứng lịch sử.
- Đọc sâu các đường đi đại diện: frontend client/refresh → auth service; màn quản trị môn → controller/service/repository; lớp/ghi danh → quyền/transaction/repository; bắt đầu/lưu/nộp Assignment; document editor; AI draft; chấm điểm/proposal transaction; worker dispatch và notification/email. Những đường dài được kiểm theo phần liên quan, không coi đây là audit toàn bộ nhánh nghiệp vụ.
- Nguồn chính: [requirements](../requirements/requirements.md), [kiến trúc hiện hành](../application-design/application-design.md), [unit ownership](../application-design/unit-of-work.md), [phụ thuộc](../application-design/component-dependency.md), [source baseline](current-code-baseline.md), [decision register](decision-register.md), [remediation đã hoàn tất](../../construction/build-and-test/2026-10-30-remediation-student-mcp-results.md).
- Giữ quyết định hiện hành: Student MCP cho UC41 trên Assignment PRACTICE đã nộp; Admin không dùng MCP; Quiz chỉ Practice; nhóm chỉ Graded; quyền học vụ không tự cấp cho Admin.

## Should fix — Nên sửa

### 1. Có hai implementation MCP chỉ dành cho role đã bị chặn

Nguồn: [AdminAiMcpTools.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AdminAiMcpTools.java#L9), [AdminDashboardMcpTools.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardMcpTools.java#L7), [McpToolRegistry.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpToolRegistry.java#L39), [McpCredentialService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java#L45).

- **Đang làm gì:** Hai bean khai báo bốn tool chỉ cho `ADMIN`, tổng cộng 52 dòng production. Registry loại `ADMIN` và các role được xác thực MCP chỉ là Student/Teacher/Subject Manager.
- **Vấn đề:** Không role được hỗ trợ nào gọi được các tool này qua MCP. Code và positive unit test vẫn mô tả năng lực Admin MCP đã bỏ, khiến người đọc hiểu sai scope.
- **Sửa nhỏ nhất:** Bỏ hai implementation và positive test riêng đã hết đối tượng; cập nhật catalog fixture. Giữ test chặn Admin bằng một provider giả có tool Admin để tiếp tục kiểm chính sách, cùng test credential/admission và Student feedback.
- **Nếu bỏ qua:** Mỗi lần sửa báo cáo AI/dashboard vẫn phải đọc và bảo trì đường không phục vụ người dùng nào.

Đã tìm tên class, tên bốn tool và `McpInput.admin` trên cây source/test/docs, gồm tham chiếu chuỗi. Các class vẫn được Spring discover và test khởi tạo; đây là **không thể truy cập qua MCP được hỗ trợ**, không phải khẳng định chúng không có tham chiếu. `McpInput.admin` thuộc public contract nên chỉ ghi nhận còn hai caller này; không tự xóa helper đó trong đợt giữ nguyên contract.

### 2. Repository đặt trong `domain` nhưng lại trực tiếp chạy SQL

Nguồn: [AccountRepository.java](../../../backend/src/main/java/edu/aiplatform/identity/domain/AccountRepository.java#L18), [ISubjectRepository.java](../../../backend/src/main/java/edu/aiplatform/subjects/domain/ISubjectRepository.java), [SubjectRepositoryAdapter.java](../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryAdapter.java).

- **Đang làm gì:** U01/U02/U03/U06/U07/U08/U12 có tổng cộng 17 file trong `domain` import Spring JDBC. U04/U05/U09/U10/U11/U13/U14 lại có repository interface và implementation ở chỗ khác.
- **Vấn đề:** Khi tìm nơi sửa câu SQL hoặc mapping dữ liệu, cùng một vai trò repository phải tìm ở cả `domain` lẫn `infrastructure`. Tên tầng không cho biết file chứa quy tắc nghiệp vụ hay truy cập database.
- **Sửa nhỏ nhất:** Trong từng feature, gom repository interface, JDBC/JPA implementation và mapping persistence vào `repository`; giữ đối tượng và quy tắc thuần ở `model`. Giữ các interface/adapter đang có; không tạo thêm interface cho 17 concrete repository.
- **Nếu bỏ qua:** Người mới phải học hai quy ước persistence và dễ đặt SQL mới vào lớp nghiệp vụ.

### 3. Frontend đã có API/types, nhưng file cùng feature chưa chia theo việc

Nguồn: [subjects](../../../frontend/src/features/subjects), [grading](../../../frontend/src/features/grading), [academics](../../../frontend/src/features/academics), [shared API](../../../frontend/src/shared/api/client.ts).

- **Đang làm gì:** `subjects` và `grading` mỗi nơi có 34 file trực tiếp; `academics` có 31, tính cả test/CSS. Cùng thư mục chứa page, dialog, hook, DTO, validation và API.
- **Vấn đề:** Thêm hoặc sửa màn môn học phải phân biệt page, thành phần tái dùng và logic tải dữ liệu trong một danh sách phẳng. Đã có `api.ts`/`types.ts`, nên chẩn đoán “chưa có API/model” sẽ dẫn tới viết lại phần đang làm đúng.
- **Sửa nhỏ nhất:** Giữ `features/<nghiệp vụ>`; đưa file vào `api`, `model`, `pages`, `components`, `hooks` khi thực sự có nội dung. Test nằm cạnh code; `register.ts`, `slots.ts`, public exports ở root feature.
- **Nếu bỏ qua:** Feature tiếp tục dài ra và khó tìm chỗ đặt code mới dù tên file riêng lẻ khá rõ.

### 4. Document editor gom nhiều trách nhiệm có cách kiểm thử khác nhau

Nguồn: [DocumentEditor.tsx](../../../frontend/src/shared/document/DocumentEditor.tsx#L144), [tests hiện có](../../../frontend/src/shared/document/document.test.tsx).

- **Đang làm gì:** File 1.124 dòng chứa đổi kiểu block, chọn chữ/caret, undo/redo, toolbar, ảnh, bảng và các block editor. Component được dùng trong soạn đề, làm bài cá nhân/nhóm, lịch sử và chấm bài.
- **Vấn đề:** Sửa riêng hành vi bảng hoặc đổi kiểu block vẫn phải đọc qua vòng đời selection/history/upload của cả editor. Rủi ro lan sang nhiều màn; số dòng chỉ là dấu hiệu, các trách nhiệm này mới là lý do tách.
- **Sửa nhỏ nhất:** Tách trước biến đổi block thuần và nhóm block editor; giữ `DocumentEditor` điều phối. Chỉ tách hook history nếu bước đó thực sự làm thao tác undo/redo dễ theo dõi; giữ public exports `DocumentEditor`, `AutoTextarea`, `ToolbarButton`.
- **Nếu bỏ qua:** Thay đổi nhỏ ở công cụ soạn thảo đòi hỏi kiểm tra lại nhiều vùng code không liên quan.

### 5. Dialog AI soạn Assignment vừa quản lý tệp vừa quản lý vòng đời draft

Nguồn: [AiDraftDialog.tsx](../../../frontend/src/features/assignments/AiDraftDialog.tsx#L50), [upload](../../../frontend/src/features/assignments/AiDraftDialog.tsx#L155), [apply](../../../frontend/src/features/assignments/AiDraftDialog.tsx#L287), [tests](../../../frontend/src/features/assignments/AiDraftDialog.test.tsx).

- **Đang làm gì:** File 689 dòng tải policy, upload/đọc/xóa nguồn, tạo yêu cầu, poll trạng thái, áp dụng/bỏ draft và render form/preview.
- **Vấn đề:** Thay giới hạn/chính sách nguồn phải đi qua state tạo và áp dụng đề; sửa poll phải quan tâm cả việc hủy upload. Các state và cleanup thuộc hai vòng đời khác nhau.
- **Sửa nhỏ nhất:** Tách `useAiDraftSources` và `useAssignmentAiDraft`; dialog giữ bố cục và input. Giữ preview sẵn có, không tạo framework AI draft dùng chung Assignment/Quiz khi hai luồng còn khác nhau.
- **Nếu bỏ qua:** Upload và apply dễ ảnh hưởng lẫn nhau mỗi khi thay một phần giao diện.

### 6. `AcademicsService` trộn truy vấn hiển thị, vòng đời lớp và ghi danh

Nguồn: [AcademicsService.java](../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java#L21), [batch enrollment](../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java#L189), [transaction tests](../../../backend/src/test/java/edu/aiplatform/academics/application/AcademicsBatchTransactionTest.java).

- **Đang làm gì:** Một service xử lý danh sách/chi tiết, candidate lookup, tạo/sửa/mở lớp và ghi danh từng dòng; nó giữ cả transaction chung và transaction `REQUIRES_NEW` cho batch.
- **Vấn đề:** Sửa màn đọc hoặc ghi danh phải đi qua cùng lớp chứa khóa tài khoản/môn và thay đổi trạng thái lớp. Tách theo việc sẽ dễ đọc hơn việc chỉ đổi `application` thành `service`.
- **Sửa nhỏ nhất:** Sau đợt chuyển package, tách `AcademicsQueryService`, `ClassService`, `EnrollmentService`; dùng lại guard hiện có, chỉ chia sẻ collaborator nếu cần thực sự. Chuyển caller trong cùng đợt, không để facade chỉ chuyển tiếp lâu dài.
- **Nếu bỏ qua:** Thay đổi ở một UC tiếp tục chạm service của nhiều UC khác, làm review transaction khó hơn.

Không bỏ kiểm quyền lặp lại quanh khóa; một số lần đọc lại nhằm chống thay đổi quyền/trạng thái giữa transaction, không phải duplication để xóa máy móc.

### 7. Danh sách lớp/sinh viên làm thêm truy vấn theo từng dòng

Nguồn: [views và studentView](../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java#L325), [getContacts đã có](../../../backend/src/main/java/edu/aiplatform/identity/application/AccountLookupService.java#L78).

- **Đang làm gì:** `views` đã gom enrollment count, nhưng mỗi lớp lại gọi `subjects.getSubject` và `accounts.getContact`; `students` gọi contact từng sinh viên.
- **Vấn đề:** Một trang 100 lớp có teacher có thể phát sinh riêng 200 lượt đọc bổ sung để dựng DTO, kể cả nhiều dòng chung teacher/môn. Đây là kiểu N+1: lấy danh sách rồi truy vấn tiếp cho từng dòng; chưa có đo đạc để kết luận vi phạm p95.
- **Sửa nhỏ nhất:** Dùng `getContacts(Collection)` sẵn có, gom subject trùng trong phạm vi request. Giữ cách báo dependency unavailable, thứ tự kết quả và kiểm quyền; không cache quyền giữa request.
- **Nếu bỏ qua:** Mỗi lần frontend lấy tất cả các trang, số lượt đọc database tiếp tục tăng theo số dòng.

Tối ưu này là đợt riêng: giảm số lần gọi có thể thay thời điểm quan sát dữ liệu/lỗi, cần bảo vệ bằng test, không trộn vào diff chuyển file.

### 8. Feature frontend import trực tiếp nhiều chi tiết của feature khác

Nguồn: [QuizDetailPage.tsx](../../../frontend/src/features/quizzes/QuizDetailPage.tsx#L4), [MaterialDialogs.tsx](../../../frontend/src/features/materials/MaterialDialogs.tsx#L5), [useLiveOverview.ts](../../../frontend/src/features/admindashboard/useLiveOverview.ts#L5).

- **Đang làm gì:** Quiz dùng dialog/hook/outline của Materials; Materials gọi API/DTO Quiz; Admin Dashboard import từ `ai/admin/api`. Một số feature có `index.ts`, một số chưa có.
- **Vấn đề:** Chỉ di chuyển `MaterialDialogs.tsx` hoặc `ai/admin/api.ts` đã làm consumer khác phải sửa theo đường nội bộ. Quan hệ giữa hai feature có thật; đây không phải bằng chứng vòng import gây crash.
- **Sửa nhỏ nhất:** Công khai một tập export nhỏ ở owner feature cho API/type/UI thực sự dùng bên ngoài; consumer dùng đường đó. Giữ slot có nhiều provider; chưa tạo registry/interface mới chỉ để che một import.
- **Nếu bỏ qua:** Việc sắp xếp một feature tiếp tục kéo thay đổi sang feature khác mà không có ranh giới dễ nhận biết.

## Nice to have — Dọn nhỏ

### 9. README còn mô tả khung ban đầu

Nguồn: [README.md](../../../README.md#L5), [state hiện hành](../../aidlc-state.md).

- **Đang làm gì:** README nói “khung code… chưa có nghiệp vụ” và mô tả stub/giữ chỗ như trạng thái hiện tại.
- **Vấn đề:** Người mới sẽ hiểu sai độ hoàn thiện và lấy quy ước của scaffold để diễn giải code đã triển khai.
- **Sửa nhỏ nhất:** Cập nhật mô tả hiện trạng và quy ước package đã được duyệt; liên kết tới inventory/plan thay vì chép lại toàn bộ thiết kế. Xóa hai binding import mà lint báo; chỉ `Icon` là nguyên một dòng riêng.
- **Nếu bỏ qua:** Tài liệu mở đầu tiếp tục mâu thuẫn với code và AI-DLC đã đồng bộ.

## Những phần nên giữ

- Chia theo nghiệp vụ và public `contract` giữa 15 unit; một backend/worker vẫn phù hợp quy mô hiện tại.
- `shared/api/client.ts` đang gom cookie, CSRF, refresh và tải binary; `shared/ui` đã có thành phần chung. Không cần thêm HTTP wrapper hoặc UI framework.
- DTO/record lồng nhau và concrete repository dùng được; không ép mỗi record một file hoặc mỗi service một interface/Impl.
- `SubjectCommandService` tách khỏi query để transaction không chờ dữ liệu hiển thị; `GradingProposalTransactions` là bean riêng để commit trước dispatch/callback. Không gộp chúng chỉ để giảm file.
- Slots có provider từ nhiều unit là cơ chế đang phục vụ nhu cầu thật. Không xóa cả registry vì dự án không cần microservice.
- Những vấn đề worker/backup/MCP/Quiz đã sửa ở đợt remediation trước không được báo lại là lỗi hiện tại.

## Kiểm chứng lượt này

| Kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| Frontend `npm test -- --reporter=dot` | 94 file / 599 test pass; exit 0 | Có cảnh báo React `act`; test jsdom/mock, không phải E2E |
| Frontend `npm run lint` | Exit 0; 0 error, 2 unused-import warning | `ai/register.ts`, `identity/admin/AccountDetailPage.tsx` |
| Frontend `npm run typecheck` | Exit 0 | Không thay cho production build |
| Backend focused `ArchitectureTest` | Chưa chạy tới test | Maven offline cache thiếu dependency/BOM; lần đầu thiếu jsoup, sau khi chép artifact sẵn có vào cache workspace còn thiếu `netty-bom:4.2.17.Final` |
| Backend full suite/required DB guard | Không chạy lại lượt này | Báo cáo remediation trước ghi 1.611 pass/0 skip; đây là bằng chứng lịch sử, không là kết quả mới |

Log mới ở `.tmp/2026-10-30-structure-audit-{frontend-tests,lint,typecheck,architecture}.log`. Không đổi manifest/lockfile và không tải dependency mới. Backend cần baseline chạy được trước khi triển khai refactor.

**Verdict:** Giữ kiến trúc theo nghiệp vụ; chuẩn hóa tầng bên trong và dọn hai implementation MCP hết scope trước. Không có bằng chứng mới đủ để xếp một lỗi runtime vào Must fix trong phạm vi review này.

**Lean: -52 lines, -0 dependencies possible** ở production khi bỏ hai implementation Admin MCP. Chưa tính test/fixture phải sửa hoặc thêm; di chuyển/tách file không tự làm giảm LOC.

**Not checked:** Toàn bộ nhánh của 683 file Java/412 file frontend; mọi dòng tài liệu lịch sử; backend suite mới; build frontend mới; trình duyệt E2E; provider AI/Drive/SMTP thật; broker fault injection; đo tải; restore; kiểm thử xâm nhập. Báo cáo này không chứng nhận an toàn/hiệu năng hay acceptance toàn bộ 66 UC.

Đề xuất thực hiện: [kế hoạch refactor](../plans/2026-10-30-code-structure-refactor-plan.md).
