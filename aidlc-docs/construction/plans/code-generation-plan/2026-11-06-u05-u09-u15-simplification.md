# Kế hoạch rút gọn code U05/U09/U15, giữ nguyên hành vi

**Trạng thái:** Đã triển khai và kiểm chứng ngày 2026-11-06. [Kết quả và giới hạn](../../build-and-test/2026-11-06-u05-u09-u15-simplification-results.md).

**Mục tiêu:** Giảm phần xử lý lặp và tầng gọi không cần thiết trong backend Java U05/U09/U15, giữ cách trình bày dễ đọc sau đợt format.

**Phạm vi:** `backend/src/main/java/edu/aiplatform/{academics,grading,admindashboard}` và test liên quan. Frontend không thuộc đợt này, theo phạm vi Java của lượt rà soát trước. Không đặt mục tiêu giảm một tỷ lệ số dòng hoặc cải thiện tốc độ khi chưa có đo đạc.

**Kiến trúc:** Giữ các package/tầng hiện tại. Bỏ một helper chỉ có một caller; gom validation giống hệt nhau trong class hiện hữu; gom chuyển đổi kết quả nguồn vào reader hiện hữu của U15. Không tạo class, interface, framework hoặc dependency mới cho ba thay đổi này.

**Công nghệ:** Java 17, Spring Boot, JUnit 5, Mockito, AssertJ; Maven và bộ kiểm tra kiến trúc hiện có.

**Căn cứ:** Yêu cầu người dùng “giúp tôi lập kế hoạch tối ưu code”, trao đổi trước đó về code gọn và không đổi hành vi; [quy ước chung](../../code-conventions.md), [kết quả format](../../build-and-test/2026-11-06-u05-u09-u15-format-results.md), [kết quả merge trước đó](../../build-and-test/2026-11-06-merge-staging-u05-u09-u15.md). Tài liệu lịch sử là nguồn tham khảo; đối chiếu code hiện tại trước khi sửa.

## 1. Baseline và cách thực hiện

- [x] Đọc code, caller và test liên quan để xác định ba thay đổi cụ thể bên dưới.
- [x] Xác nhận working tree đang giữ 71 file Java đã format và tài liệu của lượt trước; không reset/stash/ghi đè chúng. HEAD khi lập kế hoạch: `e78594cb`.
- [x] Đối chiếu shared conventions, transaction, phân quyền và hướng dẫn AI-DLC; tự review kế hoạch và kiểm tra liên kết.
- [x] Người dùng duyệt phạm vi: “giúp tôi triển khai theo kế hoạch vừa duyệt”.
- [x] Chụp snapshot/hash 1.581 file source/test trong `.tmp/simplification-20261106/before` và `baseline.json`; giữ checkout `fix/u05-u09-u15` cùng diff format hiện có. Rà lại code/caller trước mỗi đợt.

Thực hiện tuần tự: baseline kiểm thử, U05, U09, U15, rồi kiểm tra tổng hợp. Mỗi đợt phải chạy kiểm tra phù hợp trước khi sang đợt tiếp theo. Cách thực hiện đề xuất là trực tiếp trong phiên làm việc; không mặc định tạo agent hoặc commit/push.

Các lựa chọn đã cân nhắc:

| Cách làm | Đánh đổi | Đề xuất |
|---|---|---|
| Chỉ format | Ít rủi ro, nhưng không giảm phần xử lý lặp; đã thực hiện | Không lặp lại |
| Refactor nhỏ trong class/package hiện tại | Phạm vi review nhỏ, có thể kiểm chứng từng thay đổi | Chọn cho kế hoạch này |
| Gộp service hoặc thêm generic CRUD/base service | Chạm nhiều caller và che khác biệt nghiệp vụ để giảm dòng | Không đưa vào phạm vi |

## 2. Ràng buộc chung

- Giữ HTTP method/URL, status, JSON field, DTO/record shape, interface `contract`, event/MCP identifier và schema database. Không đổi migration hoặc dependency.
- Giữ thứ tự kiểm quyền, validation, khóa, ghi dữ liệu, audit/event và các lệnh gọi ra ngoài. Mã lỗi, thông báo lỗi và null behavior là một phần của hành vi cần bảo vệ.
- Giữ `@Transactional`, propagation, mỗi dòng/mục một transaction, retry/idempotency, deadline/cancellation và cách xử lý lỗi nguồn.
- Không rút tên service/method về tên khó hiểu; không dồn method và `return` lên một dòng. Giữ wildcard import hiện có theo quy ước.
- Dùng test hiện hữu nếu đã bảo vệ hành vi. Chỉ thêm characterization test cho khoảng trống liên quan; chạy chúng trên production code chưa refactor trước khi sửa. Refactor giữ hành vi không đòi test phải thất bại trước.
- Chỉ bỏ code khi đã kiểm caller, method reference, reflection và wiring liên quan. Không xóa một public method vì tìm kiếm Java không thấy caller.
- Nếu lợi ích giảm lặp/điều hướng không đủ bù helper hoặc coupling mới, bỏ thay đổi đó và ghi lý do, không cố sửa đủ cả ba unit.

## 3. Đợt 1 — U05: bỏ helper cập nhật lớp chỉ có một caller

**Bằng chứng:** `AcademicsService.updateClassInfo(...)` là caller duy nhất của private `mutateClass(...)`. Helper nhận action, hàm biến đổi và audit details dù chỉ phục vụ cập nhật thông tin lớp.

**File dự kiến sửa:**

- `backend/src/main/java/edu/aiplatform/academics/service/AcademicsService.java`
- `backend/src/test/java/edu/aiplatform/academics/service/AcademicsServiceTest.java` nếu cần bổ sung bảo vệ cập nhật lớp.

**Thay đổi quyết định:** Đưa thân `mutateClass` trực tiếp vào `updateClassInfo(Actor actor, UUID id, String name, String description, String term, long version)`. Giữ lời gọi `executeCommand(actor, "CLASS_UPDATED", id, ...)`; bên trong vẫn `loadLockedClass`, `updateInfo` với `clock.instant()`, repository update, audit với `Map.of()`, rồi `mapClassView` đúng thứ tự. Xóa private `mutateClass`; không đổi chữ ký public hoặc helper transaction hiện có.

- [x] Baseline toàn phạm vi: 246 test đạt; U05 cùng ba test bổ sung đạt 175 test trên production code cũ (`u05-characterization.log`).
- [x] Ba characterization bảo vệ version/audit trong transaction, stale version trước invalid information, và không update/success audit khi thông tin sai.
- [x] Inline và xóa `mutateClass`; không còn caller trong `backend/src`.
- [x] Sau refactor U05: 175 test đạt, không lỗi/skipped (`u05-after.log`); giữ thứ tự khóa/update/audit/map.

**Giữ riêng:** `enrollStudent` và nhánh ACTIVE của `changeEnrollmentStatus` có cách xử lý kết quả/lỗi và null khác nhau. Không ép gộp. Giữ khóa tài khoản trước môn, batch mỗi dòng một transaction, audit sau rollback và thứ tự lỗi hiện hữu.

## 4. Đợt 2 — U09: gom validation kích thước batch giống nhau

**Bằng chứng:** `GradingBatchService.executeBatch`, `GradingProposalService.requestProposals` và `estimateProposalTokens` đều kiểm danh sách null/rỗng/quá 200, trả cùng mã và thông báo lỗi.

**File dự kiến sửa:**

- `backend/src/main/java/edu/aiplatform/grading/service/GradingCommands.java`
- `backend/src/main/java/edu/aiplatform/grading/service/GradingBatchService.java`
- `backend/src/main/java/edu/aiplatform/grading/service/GradingProposalService.java`
- Các test hiện hữu `GradingBatchTest.java`, `GradingProposalServiceTest.java` trong `backend/src/test/java/edu/aiplatform/grading/service/`.

**Thay đổi quyết định:** Thêm package-private static `void validateBatchItems(List<ItemVersion> items)` trong `GradingCommands`; chuyển nguyên guard hiện hữu vào đó. Ba caller gọi ở đúng vị trí cũ. Giữ giới hạn 1–200, `INVALID_BATCH` và thông báo `Batch must contain 1–200 items`. Không đổi annotation `@Size` hoặc thêm method instance vào record để tránh thay JSON shape.

- [x] Baseline bốn class U09 đã đạt trong bộ 246 case ban đầu.
- [x] Thêm 15 case tham số ở service seam cho null/0/1/200/201; 30 case U09 đạt trước khi gom guard (`u09-characterization.log`).
- [x] Thêm package-private static `validateBatchItems` trong `GradingCommands`; ba caller gọi ở vị trí cũ, không đổi annotation/DTO/vòng lặp.
- [x] U09 sau refactor: 30 case đạt, không lỗi/skipped (`u09-after.log`), bao gồm duplicate/transaction/source failure/stable reference và estimate không dispatch.

**Giữ riêng:** Không tạo generic batch executor. Không gộp finalize/publish, manual/AI, quy tắc chọn điểm mới nhất của sinh viên với phân bố điểm, hoặc điểm nhóm với member override. Không sửa scoring, grade lock/resubmission, request-ref hoặc MCP paging trong đợt này.

## 5. Đợt 3 — U15: dùng một chỗ chuyển nguồn thành Section

**Bằng chứng:** `AdminDashboardService.section(...)` và `AdminDashboardActivityService.section(...)` trùng nhau: await nguồn, từ chối unavailable/null input, map dữ liệu và chuyển lỗi mapper thành unavailable.

**File dự kiến sửa:**

- `backend/src/main/java/edu/aiplatform/admindashboard/integration/DashboardSourceReader.java`
- `backend/src/main/java/edu/aiplatform/admindashboard/service/AdminDashboardService.java`
- `backend/src/main/java/edu/aiplatform/admindashboard/service/AdminDashboardActivityService.java`
- Nếu thiếu coverage, bổ sung test vào `AdminDashboardServiceTest.java`, `AdminDashboardActivityServiceTest.java` hoặc `AdminDashboardBackupTest.java` trong `backend/src/test/java/edu/aiplatform/admindashboard/service/`.

**Thay đổi quyết định:** Thêm `<T, R> Section<R> readSection(PendingSource<T> pending, Function<T, R> mapper)` vào reader hiện hữu của U15. Chuyển nguyên phần chung vào method này, giữ `awaitSource` nằm ngoài vùng catch của mapper như hiện tại. Hai service gọi reader và bỏ hai private `section`; không tạo class/helper layer mới. Đây là phụ thuộc nội bộ cùng U15 từ reader sang `AdminDashboardViews.Section`, không phải mở rộng public contract liên unit. Review lợi ích/coupling trước khi áp dụng; nếu phải thêm adapter/layer để làm được thì bỏ đợt này.

- [x] U15 cùng `ArchitectureTest` đạt 59 case trước refactor (`u15-before.log`).
- [x] Coverage hiện hữu bảo vệ null/negative/overflow/mapping failure và partial availability; `noHistoryIsAvailableButMissingMetadataRemainsUnavailable` bảo vệ `AVAILABLE(null)`. Không thêm test trùng.
- [x] Chuyển helper vào `DashboardSourceReader.readSection`; hai service dùng chung, bỏ hai helper cũ và import không còn dùng. Không đổi submit/await order hoặc catch boundary.
- [x] U15 sau refactor cùng `ArchitectureTest`: 59 case đạt, không lỗi/skipped (`u15-after.log`); giữ cancellation/deadline và no-store.

**Giữ riêng:** Không bỏ kiểm quyền tại controller/service chỉ vì thấy lặp; thứ tự kiểm quyền và reject query phải giữ. Không thêm cache, retry hoặc gộp nguồn đọc làm các phần dashboard phụ thuộc nhau.

## 6. Điểm review bắt buộc

1. U05: lệch version và quyền/dữ liệu cùng sai phải giữ lỗi được ưu tiên, không phát success audit sau thất bại.
2. U09: null, 0, 1, 200, 201 mục; không vô tình coi deduplication là validation kích thước.
3. U09: mục trùng và AI source bị chặn; giữ thứ tự kết quả, stable request-ref, partial success và việc estimate không dispatch.
4. U15: phân biệt source null/unavailable với mapper trả null; không đổi successful empty backup thành lỗi.
5. U15: một source treo hoặc mapper lỗi không làm hỏng các phần còn lại; giữ thời hạn ban đầu và cancellation.

Các mục này thuộc test của đợt tương ứng; nếu test hiện hữu chưa bao phủ thì bổ sung trước refactor, không để thành ghi chú bỏ ngỏ.

## 7. Lệnh kiểm tra khi triển khai

Chạy từ root, với JDK 17 và Maven đã được thiết lập. Dùng cache/Byte Buddy agent và giới hạn bộ nhớ đã ghi trong tài liệu verification hiện tại nếu môi trường Windows cần; không sửa dependency/config dự án để né lỗi môi trường.

```powershell
mvn -f backend/pom.xml "-Dtest=AcademicsServiceTest,AcademicsAccessAndReadTest,AcademicsBatchTransactionTest,AcademicsControllerTest,AcademicsApiContractTest" test
mvn -f backend/pom.xml "-Dtest=GradingBatchTest,GradingProposalServiceTest,GradingCommandsTest,GradingProposalApiContractTest" test
mvn -f backend/pom.xml "-Dtest=DashboardSourceReaderTest,AdminDashboardServiceTest,AdminDashboardActivityServiceTest,AdminDashboardBackupTest,AdminDashboardAccessTest,AdminDashboardApiContractTest,AdminDashboardControllerTest,ArchitectureTest" test
mvn -f backend/pom.xml verify
powershell -NoProfile -File scripts/assert-required-backend-tests.ps1
git diff --check
```

- [x] Full backend verify đạt 1.822 test, zero failures/errors/skips, đóng gói JAR thành công; required PostgreSQL guard đạt 31 + 2 case. Lần đầu Docker chưa chạy được ghi riêng, không tính là đạt.
- [x] Đối chiếu snapshot: đúng bảy production/ba test file thay đổi; frontend/contract/config không đổi. Reviewer độc lập không phát hiện vấn đề cần sửa; `git diff --check` đạt.
- [x] Ghi test thực chạy, baseline/final log và giới hạn trong kết quả mới; cập nhật state/checklist. Không dùng số test lịch sử làm bằng chứng mới.

Không chạy frontend build/test chỉ để xác nhận thay đổi Java nội bộ. Nếu phạm vi thực tế chạm frontend hoặc API/DTO, dừng mở rộng đợt này và điều chỉnh kế hoạch/coverage trước.

## 8. Điều kiện hoàn thành và giới hạn

Hoàn thành khi đã bỏ tầng gọi đơn dụng U05, ba guard U09 dùng chung một quy tắc, U15 không còn hai bản logic chuyển Section (hoặc ghi nhận không áp dụng vì lợi ích không đủ), các kiểm tra trên đạt và diff chỉ chứa thay đổi đã chọn. Không cần cố giảm LOC nếu làm code khó đọc hơn.

Rollback từng đợt bằng patch ngược dựa trên snapshot riêng, giữ phần format có sẵn; không `git reset --hard` hoặc khôi phục toàn file từ HEAD. Commit/push/PR/deploy không thuộc kế hoạch thực thi hiện tại.

Đây là refactor đã hoàn thành, không phải báo cáo bug hoặc benchmark hiệu năng. Evidence runtime mới nằm trong tài liệu kết quả; không dùng compile-only của lượt format để chứng minh refactor. Chưa audit dead code toàn repo hoặc thay cấu trúc mọi service.

AI-DLC: tiếp tục brownfield refactor hiện hữu; requirements/planning gộp ở tài liệu này. Không cần user stories, schema, hạ tầng hay thiết kế ứng dụng mới. Giữ Security Baseline và Resiliency Baseline theo scope hiện tại; bảo vệ quyền, audit, lỗi phụ thuộc và cancellation trong test. PBT vẫn tắt theo cấu hình dự án. Triển khai/build-test hoàn thành; không có thay đổi Operations.
