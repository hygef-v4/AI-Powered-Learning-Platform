# Review migration và persistence U04

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Lát cắt B03/B05 ngày18/10/2026 nối Subject bất biến với PostgreSQL. Code và developer checks đã xong cho adapter; phần chạy migration trong ứng dụng chung còn cần accounts migration thật U01. Chưa có service, controller, JWT hay nghiệp vụ lớp; chưa xác nhận U04 chạy end-to-end.

## Thứ tự đọc

1. [Migration](../../../../backend/src/main/resources/db/migration/subjects/V20260925_1000__create_subjects.sql): bảng, constraint và quyền app. manager_id chỉ FK tới accounts.id; không đọc role/status từ SQL U04. Kiểm tài khoản ACTIVE/SUBJECT_MANAGER thuộc service qua U01.
2. [SubjectJpaEntity](../../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/SubjectJpaEntity.java): mapping các cột; code/createdAt không cập nhật; manager là UUID, không @ManyToOne sang entity tài khoản unit khác.
3. [Repository adapter](../../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryAdapter.java): chuyển entity ↔ domain snapshot, kiểm page và pattern tìm literal, insert, conditional update.
4. [JPA repository](../../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/ISubjectJpaRepository.java): đọc câu UPDATE có WHERE version; so với test hai thread cùng version.
5. [Test PostgreSQL](../../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java): đọc các case lưu/đọc, stale update, duplicate code, transaction rollback, search và permissions.

## Version chỉ tăng một lần

Domain dùng public version từ1; Hibernate @Version ở DB bắt đầu0. Đây là mapping kỹ thuật, không có hai nguồn version độc lập:

| Thao tác | Public version | DB version |
|---|---|---|
| Tạo môn | 1 | 0 |
| Sửa tên hoặc archive thật | 2 | 1 |
| Đổi/bỏ manager thật tiếp theo | 3 | 2 |
| PUT không đổi gì, version request đúng | Giữ nguyên | Không chạy UPDATE |

Ví dụ domain đang v1, archive tạo snapshot v2. Adapter kiểm snapshot.version=expectedVersion+1, sau đó ghi DB version1 với WHERE DB version0. Hai request cùng dựa v1 thì chỉ một cập nhật được một dòng; request kia nhận false. **Service ở bước sau phải chuyển false thành409**; adapter này chưa có HTTP endpoint.

UPDATE bulk JPQL không tự áp dụng optimistic-lock check như save managed entity: vì vậy câu query tự ghi điều kiện version và nextVersion rõ ràng. Không đổi sang save/merge snapshot mới hoặc cộng thêm version lần nữa mà bỏ test tương ứng. @Version hiện hỗ trợ mapping và khởi tạo insert; bulk UPDATE do adapter kiểm bằng compare-and-set.

code/createdAt không nằm trong SET. code còn được so trong WHERE để chặn snapshot cố đổi mã. Domain requireVersion phải chạy trước khi quyết định no-op; adapter từ chối snapshot chưa tăng version thay vì gửi UPDATE giả.

## Vì sao detach đúng entity thay vì clear toàn context

Adapter trả Subject record bất biến, sau đó detach entity Subject vừa đọc. Lần đọc tiếp trong cùng transaction sẽ lấy DB mới sau bulk UPDATE. Không clear toàn EntityManager vì cùng transaction sau này có thể đang sửa account U01 khi disable/gỡ môn; clear toàn bộ có thể làm mất quản lý entity khác. Test đã kiểm read-after-write và entity không liên quan vẫn managed.

insert/update tham gia transaction của caller; nếu caller rollback, thay đổi môn cũng rollback. Đây mới là nền để service sau này ghép với audit U02; chưa có test atomic subject+audit hoặc disable account thật.

## Search và quyền DB

- Search theo mã/tên, không phân biệt hoa thường; %, _ và ! trong input được escape để mang nghĩa ký tự literal. Giá trị query được bind parameter, không nối vào SQL.
- Trang0, size1–100; sort code ổn định vì code unique. Counts theo status áp dụng cùng từ khóa trước lọc status. Lookup managerId không được nhận null để tránh biến thành truy vấn các môn chưa phân công.
- DB có unique code, check định dạng/trạng thái/version/tên và FK manager. Domain và service vẫn phải validate trước; constraint DB là lớp bảo đảm cuối khi có request đồng thời hoặc code lỗi.
- Test mô phỏng default privileges gồm DELETE như infra rồi áp dụng migration để chứng minh REVOKE DELETE thực sự có tác dụng. Môn archive/reopen bằng UPDATE, không xóa.

## Kiểm thử đã thực thi

Toàn backend: **129 test pass,0 failures/errors/skipped**, gồm20 lượt domain,15 persistence và3 rule ArchUnit. PostgreSQL thật **16.15**, Hibernate schema validate và Flyway chạy migration subjects; chạy lần hai không áp dụng lại.

Hai test concurrency dùng hai transaction/connection: cập nhật cùng version chỉ một true; tạo cùng code chỉ một thành công nhờ unique constraint. Rollback, version public/storage, FK sai, mã/status/tên raw sai, search literal/paging/counts và quyền app đều được kiểm.

Đây là developer persistence evidence, không phải sign-off độc lập của tester. Test dùng [accounts fixture](../../../../backend/src/test/resources/subjects/identity-prerequisite.sql) chỉ gồm UUID primary key để kiểm FK; không mô phỏng business role/status hoặc thay cho migration U01. Fixture nằm ngoài db/migration runtime. B03 và G1 với schema thật vẫn pending.

## Chạy lại test DB riêng

Test chỉ bật khi đặt U04_TEST_JDBC_URL. Nếu không đặt env, Maven vẫn chạy unit tests nhưng15 persistence test bị skip; không được dùng lượt đó để khẳng định PostgreSQL pass.

Khởi động Docker trước, chạy PowerShell tại repo. Container dưới đây dùng DB tạm riêng, port loopback ngẫu nhiên và mật khẩu sinh ngẫu nhiên, không dùng volume dự án:

```powershell
$u04Name = 'codex-u04-subjects-' + [Guid]::NewGuid().ToString('N').Substring(0,10)
$env:U04_TEST_DB_USER = 'u04_test'
$env:U04_TEST_DB_PASSWORD = [Guid]::NewGuid().ToString('N')
docker run --detach --rm --name $u04Name -e POSTGRES_DB=u04_subjects_test -e POSTGRES_USER=u04_test -e "POSTGRES_PASSWORD=$env:U04_TEST_DB_PASSWORD" -p '127.0.0.1::5432' postgres:16-alpine
```

Chờ `docker exec $u04Name pg_isready -U u04_test -d u04_subjects_test` báo accepting connections rồi chạy:

```powershell
$u04Port = ((docker port $u04Name 5432/tcp) -split ':')[-1].Trim()
$env:U04_TEST_JDBC_URL = "jdbc:postgresql://127.0.0.1:${u04Port}/u04_subjects_test"
try {
    mvn.cmd -f backend/pom.xml test
} finally {
    docker stop $u04Name
    Remove-Item Env:U04_TEST_JDBC_URL,Env:U04_TEST_DB_USER,Env:U04_TEST_DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Mỗi lượt test tự tạo schema có UUID và dọn schema đó; guard URL chỉ cho localhost/127.0.0.1 với database u04_subjects_test. Không trỏ test vào DB ứng dụng thật, không dùng docker compose down -v. Các báo cáo Surefire nằm trong backend/target và được Git ignore. Không đổi pom hoặc thêm Testcontainers; dùng thư viện JPA/JDBC/Flyway hiện có để tránh chồng file dependency chung của nhóm.

## Bước tiếp theo sau review

B06: cài ISubjectQuery và ISubjectScope bằng repository này; tiếp đó cung cấp guard khóa lifecycle cho U05 theo hợp đồng đã thống nhất. Archive-vs-create/reopen và disable-vs-assign là concurrency **liên unit**, chưa được chứng minh bởi hai test optimistic/unique trong lượt này. Không coi entity/adapter là đã hoàn thành scope, phân quyền hay audit.

**Cập nhật sau review:** B06 query/scope đã triển khai; xem `aidlc-docs/construction/u04-subject-administration/code/query-scope-review.md`. Suite PostgreSQL hiện có16 case vì bổ sung query/scope qua DB; các số15 case/129 tổng ở phần trên là bằng chứng lượt persistence trước. Guard/lifecycle và integration U01/U05 thật vẫn chưa hoàn thành.
