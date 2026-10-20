# U15 — implementation contract review

Ngày 2026-10-20; **dự thảo để duyệt, chưa code**. Phạm vi UC66/KAN-80, Epic KAN-32; US-RPT-002 và phần đọc backup metadata của US-OPS-001; FR-024/038.

## Nguồn

- [Unit mapping](../../../inception/application-design/unit-of-work.md), [story map](../../../inception/application-design/unit-of-work-story-map.md), [requirements](../../../inception/requirements/requirements.md), [stories](../../../inception/user-stories/stories.md).
- [Business rules](../functional-design/business-rules.md), [logic](../functional-design/business-logic-model.md), [domain/contracts](../functional-design/domain-entities.md), [frontend 17/10](../functional-design/frontend-components.md), [NFR](../nfr-requirements/nfr-requirements.md), [patterns](../nfr-design/nfr-design-patterns.md), [infrastructure](../infrastructure-design/infrastructure-design.md).
- Java public interfaces/provider và frontend source hiện tại. Các application-design cũ ghi U15=Grading/U16=Dashboard chỉ là mapping lịch sử; U15 hiện hành là Admin Dashboard.

## Hiện trạng source

U15 backend chỉ có `.gitkeep`; frontend có index trống và NavEntry `/admin` đã được import trong `features/register.ts`. Chưa có `app/admin/page.tsx`. `app/admin/layout.tsx` đã dùng AuthenticatedLayout/RoleGuard ADMIN và shared AppShell. `HomePaths.ADMIN_HOME` vẫn `/admin/accounts`. Counts và backup/settings có implementation thật trong source; chưa chứng minh tích hợp DB/provider bằng việc chỉ đọc class.

## Contract thực tế giữ nguyên

| Owner | Interface/method | Kiểu/semantics |
|---|---|---|
| U01 | IAuthorization.authorize(Actor,String,ResourceRef) | Kiểm tài khoản hiện hành; null/deny/lỗi không được đọc data |
| U01 | IAccountLookup.countByRoleAndStatus() | Map<Role,Map<AccountStatus,Long>>; implementation trả sparse map sau query thành công |
| U04 | ISubjectQuery.countSubjectsByStatus() | Map<SubjectStatus,Long> |
| U05 | IClassAccess.countClassesByStatus(), countActiveEnrollments() | Count theo status; enrollment ACTIVE của mọi trạng thái lớp theo SQL hiện tại |
| U02 | IAudit.record(AuditEvent) | ACCESS_DENIED; DENIED persistence độc lập transaction nghiệp vụ theo contract U02 |
| U03 | IBackupStatus.latest(), list(Pageable) | Optional<BackupRunView>, Page<BackupRunView>; empty thành công khác nguồn lỗi |
| U03 | ISettings.get(SettingKey<T>) | backup.enabled:Boolean; backup.dailyAt:LocalTime; cache30s thuộc U03 |

Nguồn Java: `identity/contract`, `subjects/contract`, `academics/contract`, `audit/contract`, `backup/contract`, `settings/contract` dưới `backend/src/main/java/edu/aiplatform`. U15 không import application/domain/repository của owner khác. Consumer tự khai báo SettingKey typed theo key public, không import BackupSettingDefinitions nội bộ U03.

## C01–C07: khoảng trống và quyết định đề xuất

| ID | Tài liệu so với code | Quyết định đề xuất |
|---|---|---|
| C01 | UI cần startedAt; BackupRunView chỉ có scheduledFor/state/snapshotAt/completedAt/byteSize/safeError/purgedAt và id | Không dùng snapshotAt giả startedAt; trường chưa khả dụng. Full acceptance cần owner U03 bổ sung metadata qua public contract được duyệt |
| C02 | UI cần lần SUCCESS gần nhất, số lỗi/lỗi mới nhất7ngày; port chỉ latest/list không filter/summary | Ba dòng không đủ kết luận. Không scan vô hạn dưới deadline2s, không mặc định0; summary thiếu nguồn là UNAVAILABLE. Contract bổ sung U03 là gate full UC66 |
| C03 | BR/logic cũ nói lối tắt và Setting Detail; UI17/10 dùng menu bên, không shortcut riêng, backup CTA sang Setting List | Theo UI17/10: sidebar hiện có; CTA `/admin/settings`; không đoán deep link |
| C04 | Copy UI nói mọi bản xóa sau7ngày; BR-U03-96 và code giữ SUCCESS mới nhất dù quá7ngày | Copy theo chính sách U03 kèm ngoại lệ; chỉ purgedAt xác nhận đã dọn, không suy từ tuổi. Không đổi retention job |
| C05 | Subtitle enrollment dễ hiểu là toàn bộ count thuộc lớp OPEN | Giữ count của port; ghi metric riêng "Lớp đang mở: n", không gán mọi enrollment ACTIVE cho OPEN. Muốn lọc OPEN cần contract U05 riêng |
| C06 | U15 không cache nhưng ISettings cache30s/có stale fallback | Không bypass U03; nextPlannedAt chỉ dự kiến theo cấu hình nhận được, không khẳng định scheduler đã nhận hoặc realtime |
| C07 | NFR p95<=1s, timeout mỗi nguồn2s | Giữ cả hai; benchmark nguồn khỏe riêng, fault-injection deadline2s+overhead riêng. Không tuyên bố mọi request<=1s |

BR-U03-88: backup.enabled mặc định false, dailyAt không có mặc định; BR-U03-96: không bao giờ xóa bản SUCCESS mới nhất. Đây là chính sách U03, U15 không sở hữu lịch/retention/restore.

## Hai cách triển khai

**A — Khuyến nghị: consumer trước, handoff provider riêng.** Làm counts/latest/recent3/history/schedule bằng port hiện có; C01/C02 hiển thị chưa khả dụng. Có thể code-ready cho lát cắt đó, chưa được gọi full UC66 Done. Không đổi record/interface U03.

**B — Chờ full metadata.** Owner U03 giao public contract bổ sung có startedAt và summary chính xác, gồm ordering/time-window/absence/error semantics; cập nhật Task3/5 trước code phần Backup đầy đủ. Approval U15 không tự cấp quyền sửa provider U03.

Không chọn: đọc SQL owner, đổi constructor public âm thầm, tải toàn bộ lịch sử mỗi request hoặc dùng fixture production. Không tự tạo ticket/nhắn owner trong lượt lập plan.

## Kiến trúc đề xuất

- Một AdminDashboardService orchestration/mapping/quyền; một DashboardSourceReader nội bộ quản lý bounded executor/deadline/cancellation; DTO immutable cùng application. Không entity/repository U15, interface một implementation hoặc framework aggregation dùng chung.
- Bảy task độc lập: accounts, subjects, classes, enrollments, latestBackup, recentBackups, backupSchedule. Submit hết trước await; deadline2s từ submit gồm queue time. Pool8, queue32, AbortPolicy, không commonPool/CallerRuns; Future cancel không bảo đảm JDBC đã dừng.
- Section AVAILABLE/UNAVAILABLE; successful empty khác lỗi. Sparse count map sau query thành công điền0; null/âm/overflow không giả0. JSON không name/email/id/archiveRef/hash/key/stack trace; backup chỉ whitelist metadata safeError từ U03.
- Reuse shared UI, session/RoleGuard và apiFetch. Hai GET read-only có no-store; không polling/cache số liệu, restore/download, sửa settings hoặc nội dung học vụ cá nhân.

Đây là addendum **đề xuất**, chưa thay spec gốc/public contract. [Plan](../../plans/code-generation-plan/u15-admin-dashboard-code-generation-plan.md) và [approval questions](../../plans/code-generation-plan/u15-implementation-approval-questions.md) cần duyệt trước implementation. Lượt này không chạy test/runtime U15, không commit/push/Jira transition.
