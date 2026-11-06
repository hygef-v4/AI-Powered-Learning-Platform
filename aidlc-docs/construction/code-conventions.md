# Quy ước mã nguồn chung

Áp dụng từ 2026-11-05 cho toàn bộ unit: Java trong `backend/src`, TypeScript/React/CSS trong `frontend/src`, các test và script do dự án quản lý. Đây là nguồn quy ước chung cho code mới và code được refactor, không chỉ U09. Không chỉnh mã sinh tự động, thư viện bên ngoài hoặc output build.

## 1. Cách áp dụng

- Bắt buộc áp dụng khi thêm hoặc đổi tên thành phần. Khi sửa nghiệp vụ, áp dụng trong phạm vi liên quan; không đổi tên hàng loạt ngoài phạm vi chỉ để đạt quy ước.
- Quy ước này không tự cho phép thay đổi API, JSON, schema database, event, MCP tool hoặc hành vi nghiệp vụ. Khi tên hiện hữu là contract, giữ tương thích hoặc xử lý migration trong một thay đổi được xác định riêng.
- Giữ cấu trúc theo nghiệp vụ và các tầng hiện tại; không thêm interface/Impl, wrapper hay dependency chỉ để đặt tên.
- Không sửa các báo cáo kiểm thử và kế hoạch lịch sử để làm chúng giống code mới.

## 2. Tên kiểu, file và package

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Java class, record, enum | `PascalCase`, diễn đạt vai trò | `EvaluationController`, `BatchResult` |
| Java interface | Tiền tố `I`, không dùng hậu tố `Port` | `IAuthorization`, `IEvaluationRepository` |
| File Java chứa public top-level type | Trùng tên type | `EvaluationController.java` |
| Package Java | Chữ thường, theo nghiệp vụ và tầng | `grading.service`, `subjects.repository` |
| React component và file component | `PascalCase` | `SubjectTable`, `SubjectTable.tsx` |
| TypeScript type/interface/enum | `PascalCase`; không áp tiền tố interface Java | `SubjectDetail`, `SubjectListQuery` |
| React hook và file hook | `use` + tên có nghĩa | `useSubjectRead.ts`, `useGradingWorkspace.ts` |
| File TypeScript tiện ích | `lowerCamelCase`, tên phản ánh nội dung | `managerSearch.ts`, `validation.ts` |
| Hằng số cố định cấp class/module | `UPPER_SNAKE_CASE` | `MAX_BATCH_SIZE` |

Giữ tên file theo framework hoặc entry point hiện có: `page.tsx`, `layout.tsx`, `route.ts`, `index.ts`, `register.ts`, `setup.ts`. CSS module đặt theo component/feature, giữ cách đặt tên của feature hiện tại; không đổi tên class CSS chỉ để đồng bộ bảng trên.

Test Java dùng tên đối tượng/chức năng với hậu tố `Test`; test frontend dùng `.test.ts` hoặc `.test.tsx` và giữ cây thư mục tương ứng trong `frontend/tests`. Migration Flyway giữ quy ước version hiện hành trong [README migration](../../backend/src/main/resources/db/migration/README.md); không đổi tên migration đã phát hành. Script và file cấu hình giữ tên mà công cụ/caller yêu cầu; file mới theo cách đặt tên cùng thư mục.

## 3. Field dependency và tham số constructor

- Field chứa service bắt buộc có hậu tố `Service`. Mặc định lấy tên kiểu service chuyển sang `lowerCamelCase`; tham số constructor dùng cùng tên với field.
- Không dùng tên chung chung như `service`, `grading`, `review`, `batch`, `grades` khi biến thực chất là dependency service.
- Dependency không phải service đặt theo đúng vai trò/type, không thêm `Service` giả. Bỏ tiền tố `I` của interface khi đặt tên biến.
- Nếu có nhiều bean cùng kiểu, thêm từ chỉ vai trò để phân biệt và giữ đúng qualifier/cơ chế chọn bean hiện tại.

| Khai báo hiện tại | Khai báo theo quy ước |
|---|---|
| `GradingService grading` | `GradingService gradingService` |
| `SubmissionReviewService review` | `SubmissionReviewService submissionReviewService` |
| `GradingBatchService batch` | `GradingBatchService gradingBatchService` |
| `GradebookService service` | `GradebookService gradebookService` |
| `GradingProposalTransactions proposals` | `GradingProposalTransactions gradingProposalTransactions` |
| `GradebookExporter exporter` | `GradebookExporter gradebookExporter` |
| `IEvaluationRepository repository` | `IEvaluationRepository evaluationRepository` |

Quy tắc áp dụng cả dependency trong service, adapter, controller và test; không chỉ field của controller.

## 4. Method và function

- Method/function thực hiện hành động bắt buộc bắt đầu bằng động từ, theo mẫu **động từ + đối tượng nghiệp vụ**, viết `lowerCamelCase`.
- Ưu tiên khoảng hai từ. Dùng thêm từ khi cần phân biệt đối tượng, phạm vi hoặc kết quả; không rút gọn làm mất nghĩa.
- Không dùng riêng danh từ như `detail`, `member`, `student`, `progress`, `distribution`; không dùng tên thiếu đối tượng như `get`, `put`, `run`, `process`, `handle`, `save` cho method nghiệp vụ mới.
- Đọc danh sách dùng `list`; đọc chi tiết dùng `load`; tìm kiếm dùng `find`; tính toán dùng `calculate` hoặc `estimate`. Ghi dữ liệu dùng động từ đúng nghiệp vụ như `create`, `save`, `update`, `finalize`, `publish`, `issue`.
- Method kiểm tra boolean dùng `is`, `has`, `can`, `should`. Kiểm tra điều kiện và ném lỗi dùng `require` hoặc `validate`. Tên phải phản ánh hành vi thực tế, không dùng `find` cho thao tác ghi.
- Tên method controller mô tả use case, không mô phỏng HTTP verb. Giữ `@GetMapping`, `@PutMapping` và URL khi chỉ refactor tên Java.

| Tên cũ | Ví dụ tên rõ nghĩa |
|---|---|
| `detail` | `loadEvaluation` |
| `save` | `saveEvaluation` |
| `member` | `saveMemberGrade` |
| `publish` | `publishGrades` |
| `submissions` | `listSubmissions` |
| `progress` | `loadSubmissionProgress` |
| `batch` | `requestProposals` |
| `estimateBatch` | `estimateGradingTokens` |
| `student` | `loadStudentGrades` |
| `get` | `loadVisibility` |
| `update` | `updateVisibility` |
| `download` khi chỉ cấp token | `issueDownloadToken` |

Các ví dụ minh họa cách đặt tên, không xác nhận code hiện tại đã được đổi tên.

### Ngoại lệ

- Constructor trùng tên class; accessor record giữ dạng `version()`, `usedProposalId()`.
- Getter/setter JavaBeans giữ `getScore()`, `setScore()`, `isEnabled()` khi cần; không bắt đổi tên vì quy tắc method nghiệp vụ.
- Override, method do framework/library quy định, Spring Data derived query và public contract giữ chữ ký cần thiết. Không đổi `equals`, `hashCode`, `toString` hoặc React/framework entry point để đạt số từ.
- React component dùng tên kiểu component; hook dùng `use...`. Callback UI dùng `handleSave`, `handleSubmit`; prop callback dùng `onSaved`, `onClose` theo vai trò.
- Test được đặt tên câu mô tả hành vi/kỳ vọng, có thể dài hơn hai từ. Không ép test thành tên CRUD chung chung.

## 5. Biến, tham số và dữ liệu

- Dùng `lowerCamelCase`, tiếng Anh và thuật ngữ nghiệp vụ thống nhất. Tên thể hiện dữ liệu đang giữ: `evaluationId`, `assignmentId`, `studentAccountId`, `publishedGrades`.
- Collection dùng số nhiều; map thể hiện quan hệ khi cần, ví dụ `evaluationsById`. Biến boolean dùng `is`, `has`, `can`, `should` hoặc trạng thái rõ như `enabled`, `late`.
- Tránh `data`, `obj`, `tmp`, `x` khi có tên nghiệp vụ rõ hơn. Cho phép `actor`, `input`, `id` trong phạm vi ngắn, chỉ có một nghĩa; phân biệt rõ khi xuất hiện nhiều ID/đầu vào.
- Không đổi tên field record/DTO hoặc JSON chỉ vì muốn tên nội bộ đẹp hơn. Giữ nguyên serialization, schema và dữ liệu lưu trữ trong refactor tên.

## 6. Trình bày và trách nhiệm

- Thân method/constructor có câu lệnh phải xuống dòng riêng; không viết cả khai báo và `return` trên một dòng. Tách các câu lệnh và các method bằng khoảng trắng phù hợp để dễ đọc.
- Ưu tiên dòng khoảng 100 ký tự: xuống dòng danh sách tham số, lời gọi nhiều đối số, biểu thức và chuỗi gọi method khi dài. Không bắt buộc tách literal, regex, SQL, schema hoặc URL chỉ để đạt giới hạn cột; giữ nội dung dữ liệu và contract.

- Java thụt 4 spaces, một câu lệnh mỗi dòng. Cho phép wildcard import (`import ...*;`, `import static ...*;`) để giữ danh sách import gọn, kể cả khi dùng kiểu từ `contract`; không bắt buộc bung wildcard hiện có thành từng dòng. Dùng import tường minh khi cần phân biệt tên trùng hoặc giúp làm rõ kiểu được sử dụng. TypeScript/JSX thụt 2 spaces; giữ formatter/lint hiện tại, ưu tiên độ rộng 100 ký tự cho code mới khi hợp lý.
- Controller nhận/kiểm tra đầu vào HTTP và gọi service; service điều phối use case; model giữ quy tắc nghiệp vụ; repository giữ truy cập dữ liệu. Unit khác sử dụng public `contract`.
- Tái sử dụng thành phần có sẵn. Chỉ comment lý do/giới hạn khó thể hiện bằng code; không thêm tầng hoặc comment lặp lại tên method.
- Giữ validation, phân quyền, transaction, xử lý lỗi, audit/event, accessibility và các side effect khi đổi tên.

## 7. Checklist khi áp dụng

- [ ] Xác định tên nào là nội bộ và tên nào thuộc contract/framework.
- [ ] Tìm toàn bộ caller, constructor, interface/implementation, method reference, test và fixture liên quan.
- [ ] Kiểm tra `@Bean`, `@Qualifier`, `@Resource`, reflection, cấu hình và injection theo tên trước khi đổi dependency/method.
- [ ] Đổi tên đồng bộ trong phạm vi; không thay URL, HTTP method, JSON, schema hoặc migration ngoài yêu cầu.
- [ ] Chạy compile/typecheck và test phù hợp với phần code đã sửa; kiểm tra lint khi liên quan frontend.
- [ ] Kiểm tra diff, cập nhật tài liệu hiện hành có tham chiếu tên cũ; báo rõ phần chưa kiểm tra.

Quy ước này được review bằng checklist; chưa có lint rule tự động bảo đảm mọi tên tuân thủ. Thay đổi chỉ tài liệu cần kiểm tra liên kết/nội dung, không cần chạy toàn bộ test ứng dụng.
