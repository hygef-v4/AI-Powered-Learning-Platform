# U04/U13/U14 — chuyển JPA sang JDBC: câu hỏi xác nhận

Người dùng trả lời trong chat lúc 2026-11-18T13:17:51Z: "với câu 1 tôi chỉ muốn sửa 3 unit 04, 13, 14 mà không làm ảnh hưởng đến các unit khác. Các câu 2-4 theo khuyến nghị".

Yêu cầu 2026-11-18: "giúp tôi tạo branch mới từ staging để sửa lại từ jpa sang sử dụng jdbc cho cả 3 unit". Nhánh `refactor/jdbc-u04-u13-u14` tạo từ `staging@2e179598`. Quy định áp dụng: [persistence-conventions.md](../../construction/persistence-conventions.md) và [code-conventions.md §8](../../construction/code-conventions.md).

## Hiện trạng đã kiểm

| Unit | Code còn JPA | Điểm cần giữ khi chuyển |
|---|---|---|
| U04 subjects | `ISubjectJpaRepository`, `SubjectJpaEntity`, `SubjectRepositoryAdapter`; `SubjectCommandService` bắt `org.hibernate...ConstraintViolationException` | Khóa `PESSIMISTIC_WRITE` kèm lock/statement timeout ≤ 1s; storage version = public version − 1; CAS `updateSnapshot`; nhận diện trùng mã qua constraint `uk_subjects_code`; escape `!` trong LIKE; sắp theo `code` |
| U13 materials | `MaterialModuleRepositoryAdapter` (SQL thuần chạy qua `EntityManager.createNativeQuery`) | Giữ SQL, CAS theo version, timeout 10s; `MaterialRepositoryAdapter` đã JDBC |
| U14 announcements | `Announcement` (`@Entity`), `AnnouncementRepositoryAdapter` (find, persist, JPQL) | CAS version + `status=ACTIVE`; thứ tự `created_at desc, id desc`; scope lớp rỗng trả rỗng |
| U14 notifications | `Notification`, `NotificationPreference`, `EmailDelivery`, `DeadlineReminder` (`@Entity`); `NotificationRepositoryAdapter`; `NotificationExceptionHandler` bắt `PersistenceException` | Xem rủi ro bên dưới |

**Rủi ro chính, U14 email:** `EmailDeliveryService.sendEmail`, `markEmailFailed` và `releaseDeferredEmails` đổi trạng thái `EmailDelivery` mà không gọi lưu. Hiện JPA tự ghi khi commit (dirty checking), kể cả `attempts++` khi gửi mail lỗi (`noRollbackFor = RetryableJobException`). Trong `releaseDeferredEmails`, JPA tự flush trước câu đếm `countReservedEmails`, nên hạn mức mỗi ngày tính cả email vừa chuyển sang PENDING. Khi dùng JDBC phải ghi tường minh ở mọi nhánh, đúng thứ tự, nếu không email sẽ kẹt trạng thái hoặc vượt hạn mức.

**Hạ tầng:** sau khi chuyển 3 unit, production không còn class nào dùng JPA. `pom.xml` vẫn có `spring-boot-starter-data-jpa`, `application.yml` có `spring.jpa` (`ddl-auto: validate`), transaction manager hiện là `JpaTransactionManager`. Test còn tự dựng JPA: `SubjectRepositoryIntegrationTest`, `NotificationDeliveryIntegrationTest`, `OwnerRequirementsTestConfig`, `EvaluationVersionIntegrationTest` (U09).

## Question 1
Phạm vi xử lý hạ tầng JPA sau khi chuyển code 3 unit?

A) Gỡ hẳn: bỏ `spring-boot-starter-data-jpa` và cấu hình `spring.jpa`, chuyển các test đang dựng JPA sang `DataSourceTransactionManager`, mở rộng ArchitectureTest để cấm JPA/Hibernate trên toàn backend. Transaction manager chung đổi từ `JpaTransactionManager` sang `DataSourceTransactionManager`; các repository JDBC hiện có vẫn tham gia transaction như cũ. (Khuyến nghị)

B) Chỉ chuyển code 3 unit và thêm rule ArchitectureTest cho 3 unit; giữ dependency, cấu hình và transaction manager JPA, gỡ trong một thay đổi sau.

X) Other (please describe after [Answer]: tag below)

[Answer]: X — Chỉ sửa U04, U13, U14, không ảnh hưởng unit khác: giữ dependency, cấu hình và transaction manager JPA chung (gần với B).

## Question 2
Model của U14 (`Announcement`, `Notification`, `EmailDelivery`) là class field public, có thể sửa, được service dùng trực tiếp. Cách chuyển?

A) Giữ class và field, chỉ bỏ annotation JPA. Repository map bằng `RowMapper`, thêm method ghi tường minh (ví dụ `updateEmailDelivery`) và gọi ở mọi nhánh service đang dựa vào dirty checking. Xóa `NotificationPreference`, `DeadlineReminder` vì chỉ là mapping JPA, không service nào dùng. (Khuyến nghị: thay đổi nhỏ nhất, không đổi API)

B) Đổi model sang record bất biến và viết lại service theo kiểu trả snapshot mới như U04.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Mức kiểm chứng trước và sau khi chuyển?

A) Viết test characterization trên PostgreSQL (Testcontainers, migration thật) trước khi chuyển: khóa và timeout U04, CAS/version, trùng mã môn, phân trang và escape, email (trạng thái, `attempts` khi lỗi mail, hạn mức ngày, deferred), đánh dấu đã đọc, preference upsert. Chạy cùng bộ test trước và sau; cuối cùng chạy `mvn -o -B verify` toàn backend. (Khuyến nghị, giống đợt U05/U09)

B) Chỉ chạy test hiện có và `mvn verify` toàn backend.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Cách commit trên nhánh `refactor/jdbc-u04-u13-u14`?

A) Mỗi unit một commit cục bộ (U13 → U14 → U04), cuối cùng một commit gỡ hạ tầng và cập nhật tài liệu; không push, không mở PR khi chưa được yêu cầu. (Khuyến nghị: dễ review và revert từng unit)

B) Không commit; để toàn bộ thay đổi trong working tree cho người dùng tự review và commit.

C) Một commit duy nhất sau khi hoàn tất và kiểm chứng.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Extension

Giữ cấu hình hiện hành trong `aidlc-state.md`: Security Baseline và Resiliency Baseline bật trong phạm vi đã duyệt, PBT tắt. Thay đổi này không đổi xác thực, phân quyền, API hay schema; phải giữ bind parameter (SECURITY), lock timeout, transaction timeout và trạng thái retry email (RESILIENCY).
