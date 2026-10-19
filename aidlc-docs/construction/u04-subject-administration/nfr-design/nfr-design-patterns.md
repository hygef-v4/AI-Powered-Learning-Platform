# U04 Subject Administration — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## P1 - Đọc phạm vi không cache
- `isSubjectManager` chạy `SELECT 1 FROM subjects WHERE id = ? AND manager_id = ?` mỗi lần gọi.

## P2 - Khóa lạc quan
- `UPDATE subjects SET ..., version = version + 1 WHERE id = ? AND version = ?`; 0 dòng thì `409`.

## P3 - Gọi U05 qua contract có timeout
- `ISubjectClasses` dùng cho hiển thị với timeout1s; lỗi hiện "không khả dụng". Archive gọi thẳng `ISubjectClasses.archiveClassesOfSubject` trên cùng thread và transaction; không lấy kết quả reader async làm căn cứ archive.
- Theo Q05 đã duyệt ngày18/10/2026, tới khi U05 cung cấp implementation, placeholder báo `SUBJECT_CLASSES_UNAVAILABLE`, không giả danh sách rỗng hoặc0 lớp. Phần hiển thị chuyển lỗi thành availability UNAVAILABLE hoặc classCount=null; archive vẫn phải từ chối.
- B07 dùng executor riêng giới hạn4 worker/16 chỗ chờ, deadline1s gồm thời gian chờ trong queue; hết hạn thì cancel và bỏ task chờ. Cancel chỉ yêu cầu interrupt: U05 phải đặt query/network timeout và giải phóng tài nguyên của chính provider. Không truyền transaction/persistence context hoặc SecurityContext của caller sang worker; chỉ gọi contract đọc nội bộ sau khi đã kiểm Admin. Chưa đo tải/p95 hoặc xác nhận provider U05 thật.

## P4 - Khóa lifecycle cùng transaction (B09)

- `ISubjectMutationGuard` do U04 cài, U05 gọi trong transaction READ_COMMITTED trước mọi thao tác ghi lớp và ghi danh (thay cho advisory lock riêng của U05); khóa subject giữ đến commit/rollback. Guard không thay authorization U01.
- Archive khóa cùng row rồi mới gọi `ISubjectClasses.archiveClassesOfSubject` để U05 lưu trữ mọi lớp, cùng thread/transaction; U05 không trả lời thì trả 503 và rollback. Reopen không query/mutate lớp.
- Ngân sách lock+check tối đa1s đo bằng System.nanoTime. Repository đặt transaction-local PostgreSQL lock_timeout/statement_timeout có tính timeout chặt hơn đang tồn tại, restore sau query thành công; lỗi phải rollback. U05 tự giới hạn thời gian query. Không worker async/REQUIRES_NEW trong lifecycle.
- U04 kiểm elapsed sau provider để từ chối kết quả0 trả muộn, nhưng không thể cưỡng chế ngắt Java provider bỏ qua budget; phải xác minh provider thật bằng integration/latency evidence. Deadline không phải cam kết toàn HTTP/auth/audit/commit xong trong1s.
