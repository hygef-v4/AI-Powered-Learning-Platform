# Review query và scope môn học U04

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

B06 triển khai ngày18/10/2026 sau khi người dùng review persistence. Lượt này cung cấp hai interface nội bộ cho U01/U05 và các unit đọc môn. Không có API HTTP mới, không sửa authorization/OTP U01 hoặc nghiệp vụ lớp U05. Guard lifecycle, audit và disable tự gỡ môn vẫn nằm ở bước sau.

## Thứ tự đọc

1. [SubjectQueryService](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectQueryService.java) và [test query](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectQueryServiceTest.java): chuyển domain snapshot sang SubjectRef, kiểm trạng thái, danh sách môn phụ trách và counts.
2. [SubjectScopeService](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectScopeService.java) và [test scope](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectScopeServiceTest.java): chỉ kiểm manager_id hiện thời, không lưu kết quả quyền vào cache.
3. [Repository adapter](../../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryAdapter.java): method isManagedBy gọi existsByIdAndManagerId của JPA; không tải toàn môn để quyết định assignment.
4. [Test wiring](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectContractsWiringTest.java): Spring chọn service thật và không tạo stub cùng interface; kiểm placeholder lớp và việc provider bean thay nó.
5. [Test PostgreSQL](../../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java): case publicQueryAndScopeReadCurrentAssignmentsThroughTheRealDatabase đi qua service → adapter → DB, đổi A→B→null và kiểm ngay lại.

## Query và scope khác nhau ở đâu

| Interface | Dữ liệu trả về | Trách nhiệm |
|---|---|---|
| ISubjectQuery.getSubject(id) | Optional SubjectRef gồm subjectId/code/name/status/managerId | Unit khác lấy thông tin môn tối thiểu; không lộ mô tả hoặc entity JPA |
| ISubjectQuery.isActive(id) | true khi môn tồn tại và ACTIVE | Hỗ trợ unit sở hữu tài nguyên chặn sửa khi môn archived |
| ISubjectQuery.listManagedSubjects(accountId) | List SubjectRef, gồm ACTIVE và ARCHIVED | U05 dựng các môn được giao; không tự lọc mất môn lịch sử |
| ISubjectQuery.countSubjectsByStatus() | Map ACTIVE/ARCHIVED, có cả số0 | Số đếm toàn hệ thống cho consumer; không mang filter search/status của UI Admin |
| ISubjectScope.isSubjectManager(accountId, subjectId) | Boolean assignment hiện tại | U01 kết hợp với session, role và trạng thái tài khoản để phân quyền R2 |
| ISubjectScope.listManagedSubjects(accountId) | List UUID, gồm cả môn ARCHIVED | U01 kiểm phân công trước đổi role; khác kiểu trả về của method cùng tên bên query |

Ví dụ môn archived vẫn manager=A: isActive=false nhưng isSubjectManager(A,id)=true. A có thể được xem lịch sử khi account/session còn hợp lệ; quyền ghi tài nguyên cấp môn bị chặn bởi kiểm trạng thái riêng của unit thực hiện thao tác. Không sửa scope thành “chỉ ACTIVE” để chặn ghi, vì sẽ vô tình chặn đọc lịch sử và làm U01 bỏ sót phân công lúc đổi role.

## Thay chủ nhiệm có hiệu lực thế nào

Mỗi lần kiểm scope đều gọi repository đọc điều kiện subjectId/managerId hiện thời. Sau transaction gán B commit, lần đọc sau của A trả false, của B trả true. Bỏ manager thì cả hai false. Không cần tạo cache, không ghi danh sách môn vào JWT và không gọi ngược IAuthorization từ SubjectScopeService.

Không có ID/null thì query trả rỗng hoặc scope=false mà không truy vấn danh sách môn chưa phân công. DB lỗi thì exception được truyền lên: U01 phải từ chối theo chính sách fail closed/503 đã chốt, không fallback allow. “DB lỗi” khác với “không có môn”; query không trả môn mẫu hay ACTIVE giả khi truy vấn thất bại.

Test dùng cùng accountId khi đọc lại; chưa chạy request có JWT thật. Vì vậy bằng chứng hiện tại xác nhận dữ liệu scope cập nhật, không thay test session revoke/current account ACTIVE hoặc HTTP authorization của U01.

## Bean thật và stub

SubjectQueryService/SubjectScopeService là @Service. Các stub có @ConditionalOnMissingBean theo interface nên tự nhường khi tìm thấy implementation thật. Test context tối thiểu đã xác nhận mỗi interface có đúng một bean thật và subjectQueryStub/subjectScopeStub không còn.

Không sửa IdentityContractStubs hay tắt tất cả stub toàn dự án. U01/U05 và các unit khác đang code song song; những phần chưa bàn giao vẫn có thể cần placeholder. Nếu bật stubs=false, hai service thật vẫn tồn tại; provider chưa có bean sẽ không được giả là đã sẵn sàng.

**Thay đổi có chủ đích theo Q05:** [SubjectsContractStubs](../../../../backend/src/main/java/edu/aiplatform/subjects/contract/stub/SubjectsContractStubs.java) hiện ném SUBJECT_CLASSES_UNAVAILABLE cho cả listClassesOfSubject/countClasses/countUnarchivedClasses khi U05 chưa giao implementation. Đây là ApiException503, không phải list rỗng hoặc0 lớp. Tên/signature contract không đổi; Javadoc cập nhật semantics.

Service Admin ở B07/B09 sau này phải bắt lỗi dependency đúng phạm vi để hiện phần lớp “không khả dụng” và chặn archive. Không bắt lỗi rồi thay count bằng0. Khi U05 cung cấp bean ISubjectClasses, placeholder sẽ nhường; wiring test mới chỉ dùng provider bean giả để kiểm cơ chế, chưa chứng minh implementation thật của U05.

## Bằng chứng kiểm tra

- Unit/wiring mới:6 test query,5 test scope,3 test bean selection/placeholder, tất cả pass.
- PostgreSQL16.15:16 case pass, gồm case mới đọc query/scope sau gán A→B→null và archive; dùng accounts UUID fixture test, không dùng schema U01 thật.
- Toàn backend144 test pass,0 failures/errors/skipped, gồm3 rule ArchUnit. Không thêm dependency hoặc sửa frontend, pom, application config, migration hay Git index trong lượt B06.
- Truy vấn scope dùng lookup theo khóa môn và managerId, không cache. Chưa đo p95≤20ms theo NFR; test hành vi không phải benchmark hoặc bằng chứng full application boot.
- Test database/container là môi trường riêng như persistence-review.md; guard URL/schema riêng vẫn áp dụng. Không có env U04_TEST_JDBC_URL thì16 case DB bị skip, không được báo là đã kiểm DB ở lượt đó.

## Phần tiếp theo cần giữ rõ

B07 là service đọc danh sách/chi tiết quản trị môn; nó vẫn phải kiểm Admin qua U01 trước khi trả dữ liệu. B08/B09/B10/B11 là command, archive guard, audit và HTTP API. Q14 disable tự gỡ môn chưa cài: scope chỉ phản ánh manager_id hiện tại; không tự sửa phân công tài khoản khi chỉ đọc quyền. Contract guard dùng chung với U05 chưa được tạo hoặc bàn giao trong lượt B06 này. U04 và gate migration U01 thật vẫn chưa hoàn thành.
