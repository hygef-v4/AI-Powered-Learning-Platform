# U15/U03 — rà soát contract backup, 2026-10-22

Trạng thái: **D1–D7 được người dùng duyệt riêng ngày2026-10-22; triển khai và evidence theo Phase B**. Phạm vi UC66, BR-U15-20/21, FR-024/038 và C01/C02 của slice A. Nguồn: [handoff hiện tại](implementation-summary.md), [BR U15](../functional-design/business-rules.md), [BR U03](../../u03-job-and-settings/functional-design/business-rules.md), [plan chính thức](../../plans/code-generation-plan/u15-admin-dashboard-code-generation-plan.md).

## 1. Bằng chứng hiện tại

- GitHub staging = `8519bea466ecb996a97ed75b53d1a38c9adafc61` sau PR12; checkout grading-gradebook = `ae7a1b69`, tree code giống staging. Chỉ audit local có thay đổi trước planning; giữ nguyên nội dung đó.
- `backup/contract/IBackupStatus.java` chỉ có `Optional<BackupRunView> latest()` và `Page<BackupRunView> list(Pageable)`.
- `BackupRunView` có tám component: id, scheduledFor, state, snapshotAt, completedAt, byteSize, safeError, purgedAt. Chưa có startedAt.
- `BackupRunRepository.map` và domain `BackupRun` đã đọc started_at; `BackupRun.view()` bỏ trường này. Không cần thêm cột DB để cấp metadata.
- `BackupRunRepository.latest()` sắp theo scheduled_for DESC; đây là lần chạy theo lịch mới nhất, không phải lần SUCCESS mới nhất.
- Bảng backup_runs có một row/ngày theo UNIQUE scheduled_for. markRunning cập nhật started_at khi bắt đầu lại lần retry; field này phản ánh lần xử lý gần nhất, không phải tổng lịch sử từng retry. FAILED chỉ được ghi sau lỗi vĩnh viễn/hết retry.
- `AdminDashboardService` đang gán startedAt=null, latestSuccessfulAt/failuresLast7Days=UNAVAILABLE. Frontend đã có vị trí hiển thị cả ba; chưa cần thêm màn.
- OpenAPI `admin-dashboard.yaml` chưa cho phép AVAILABLE latestSuccessfulAt có data=null; frontend đang khai báo Section<string>. Phải quyết định absence trước khi nối provider.
- JobDispatcher.safe chỉ rút ngắn exception message, BackupJobHandler.onFailed cũng chỉ cắt độ dài. Do đó tên safeError chưa bảo đảm thông điệp đã loại bí mật. Read projection mới phải giới hạn thông điệp public; không phát nguyên văn stored exception ra dashboard mới.

## 2. Các phương án

| Phương án | Đánh đổi |
|---|---|
| A. Thêm field vào BackupRunView và method vào IBackupStatus | Ít kiểu mới, nhưng đổi canonical record constructor/interface đang dùng; cần sửa compatibility constructor/default method hoặc mọi implementation/stub/consumer. |
| **B. Thêm capability chỉ đọc do U03 sở hữu — đề xuất** | Giữ nguyên hai contract cũ; thêm một interface và hai record phục vụ metadata/summary chính xác. Một BackupStatusService cài cả hai interface, không thêm service layer/truy cập DB xuyên unit. |
| C. U15 duyệt list tới hết để tự tổng hợp | Không chọn: request có thể quét lịch sử không giới hạn, khó bảo đảm snapshot và deadline; ba dòng gần nhất không đủ bằng chứng. |

Phương án B là thay đổi public contract **bổ sung**, không phải refactor giữ nguyên toàn bộ interface. Cần duyệt phạm vi U03 read-provider và handoff trước implementation. Không tự nhắn owner/tạo Jira hoặc coi Epic U03 Done là đã giao đủ metadata.

## 3. Contract đề xuất cho phương án B

Các file mới thuộc `backend/src/main/java/edu/aiplatform/backup/contract/`:

```java
public interface IBackupDashboardStatus {
    Optional<BackupDashboardRunView> latestRun();
    Page<BackupDashboardRunView> history(Pageable page);
    BackupDashboardSummary summarize(Instant fromInclusive, Instant toExclusive);
}

public record BackupDashboardRunView(
    LocalDate scheduledFor, BackupState state, Instant snapshotAt,
    Instant startedAt, Instant completedAt, Long byteSize,
    String safeError, Instant purgedAt) { }

public record BackupDashboardSummary(
    Instant fromInclusive, Instant toExclusive, Instant latestSuccessfulAt,
    long failedRuns, String latestSafeError) { }
```

Không trả id, archiveRef, archiveSha256, schemaVersion/appVersion hoặc nội dung backup. Giữ nguyên IBackupStatus/BackupRunView cũ, kể cả constructor và latest/list semantics. BackupStatusService bổ sung implements IBackupDashboardStatus; không có xung đột tên/return type với port cũ.

### Semantics cần duyệt

| ID | Quyết định đề xuất |
|---|---|
| D1 — startedAt | Map đúng started_at, có thể null khi CREATED; không dùng snapshotAt/createdAt thay thế. Retry ghi lại started_at theo U03 hiện tại, không sửa job lifecycle. |
| D2 — window | U15 lấy asOf=Clock.instant() đúng một lần; truyền [asOf−Duration.ofDays(7), asOf). Đây là 168 giờ UTC trượt; chỉ format giờ hiển thị theo Asia/Ho_Chi_Minh. |
| D3 — failedRuns | Đếm row đang FAILED có completed_at trong window; không đếm CREATED/RUNNING/SUCCESS hoặc từng lần retry. latestSafeError thuộc row FAILED mới nhất **trong cùng window**, order completed_at DESC, scheduled_for DESC, id DESC. zero thành công khác query lỗi. |
| D4 — latestSuccessfulAt | completed_at lớn nhất của row SUCCESS trước toExclusive, tìm toàn lịch sử, không giới hạn 7 ngày/3 dòng. Không lọc purged_at vì đây là mốc lịch sử thành công, không cam kết archive còn tải được; snapshotAt vẫn là field riêng. |
| D5 — absence/error | latestRun empty => AVAILABLE/null; history empty => AVAILABLE/[]; query thành công chưa có SUCCESS => AVAILABLE/null; không có FAILED => AVAILABLE/{count:0,latestSafeError:null}. Lỗi/null summary/sai window/count âm => UNAVAILABLE, không giả 0. Summary lỗi không làm mất counts/latest/history khỏe. |
| D6 — safe error | U03 projection chỉ nhận allowlist exact strings: `Quá thời gian xử lý` hoặc `Hết lượt thử lại: Quá thời gian xử lý` => `Backup quá thời gian xử lý.`. Các exception text khác của FAILED => `Backup thất bại. Vui lòng liên hệ người vận hành.`; trạng thái không FAILED trả null. Summary dùng cùng mapping cho row FAILED mới nhất, kể cả stored error null. Không sửa JobDispatcher/audit/email hoặc rewrite lịch sử trong scope này. |
| D7 — HTTP | Giữ hai GET, tên field, paging/availability hiện có. Chỉ nới data của AVAILABLE latestSuccessfulAt thành string(date-time) hoặc null, đồng bộ TS Section<string|null>. Null hiển thị `Chưa có backup thành công.`; UNAVAILABLE hiển thị `Không khả dụng`. Đây là bổ sung semantics nullable cần duyệt rõ. |

`summarize` từ chối null hoặc from>=to trước SQL. Dữ liệu terminal SUCCESS/FAILED thiếu completed_at khiến summary unavailable (kiểm integrity trong cùng SQL), không âm thầm bỏ row rồi trả count0. Các truy vấn summary nằm trong một SQL statement để dùng chung snapshot, không query-loop; history giữ thứ tự theo scheduled_for như cũ. Không thêm index/migration trong lát cắt này: dữ liệu được tạo tối đa một row/ngày; đo query thực trước khi đề xuất tối ưu riêng.

### Khả dụng và giới hạn tài nguyên

- U03 sở hữu SQL/projection và transaction read-only timeout1s cho ba method mới, dùng query parameter; kiểm timeout JDBC thật trong integration. Không thay timeout/config của method/consumer khác.
- U15 thêm một source summarize vào bảy source đang có, tổng tám source; giữ pool8/queue32/deadline2s từ submit gồm queue, submit hết trước await. Lỗi summary chỉ hạ hai section tương ứng. Không cache/poll số liệu hay scan history để fallback.
- Fallback mới trong BackupContractStubs chỉ ném BACKUP_STATUS_UNAVAILABLE; không trả empty/zero thành công giả. ConditionalOnMissingBean cho provider thật thay thế; stubs-disabled integration bắt buộc dùng implementation thật. Stub cũ không đổi.
- Consumer mapping chỉ dùng backup.contract, không import BackupRunRepository/domain. Giữ ADMIN/current-session checks trước fan-out, no-store và deny audit.

## 4. Giới hạn hoàn thành

Code-ready sau khi D1–D7 được duyệt, provider+consumer+UI/tests/build đạt. Full UC66 còn real session/audit/providers/browser, Postgres query cancellation/resource release, healthy p95≤1s và fault deadline2s+overhead; tester độc lập. Docker/DB/AI test chưa chạy trong lượt planning. U07/U08 migrations nay đã có source; ghi chú cũ “chưa có bảng” không còn là gate hiện diện source, nhưng clean startup với stubs disabled vẫn cần bằng chứng.

## 5. Self-review planning

- [x] Đối chiếu C01/C02, BR-U15-20/21 và ownership U03/U15; giữ counts/schedule/retention/backup actions ngoài scope.
- [x] Chốt chữ ký/record và semantics thời gian, absence, retry, purged, lỗi public; không còn quyết định ngầm giữa provider/consumer.
- [x] Ghi rõ hai thay đổi cần duyệt: capability U03 mới và nullable success timestamp trong HTTP schema.
- [x] Map tất cả quyết định sang tasks B1–B5 của plan chính thức; xác minh file paths và links.
- [x] Người dùng duyệt addendum và tasks B1–B5 trước khi code (approval A,2026-10-22).
