# Student code UI completeness Implementation Plan

**Goal:** Bổ sung MSSV tại tám nhóm vị trí đã được duyệt, giữ nguyên quyền xem và hành vi nghiệp vụ.

**Architecture:** Dùng `PersonSubline` cho dòng phụ và `formatStudentLabel` trong shared/studentCode cho nhãn một dòng. DTO chỉ thêm mã có thể rỗng cho thành viên tài liệu nhóm và trưởng nhóm lời mời. Quiz dùng MSSV của sinh viên đã chọn, không mở rộng API.

**Tech Stack:** Spring JDBC/Java 17, Next/React/TypeScript, JUnit/Mockito, Vitest/Testing Library; không thêm dependency hoặc migration.

**Spec:** [Phạm vi đã duyệt](../../../inception/requirements/2026-11-16-student-code-ui-completeness.md).

**Authorization:** Người dùng chọn “Duyệt phạm vi, tiếp tục sửa và kiểm thử”. Thực hiện các bước dưới đây trong worktree audit-staging hiện có; không hỏi lại cùng phạm vi, không commit/push.

## Ràng buộc và điểm review

- Chỉ tên người vốn đã hiện được đi kèm mã; không thay ID, thứ tự, lựa chọn, phân trang hoặc quyền đọc.
- Người trùng tên vẫn phân biệt bằng MSSV; người không có mã giữ fallback cũ. Không hiện `undefined`, `null` hoặc dấu phân cách thừa.
- Nhãn UI có MSSV không được đưa vào prompt AI, thông báo, email hoặc log/audit.
- Nhãn thành viên trong tài liệu nhóm giữ “Bạn” cho chính mình, thêm mã khi có.
- Lịch sử của người đã rời nhóm giữ fallback hiện tại, không tra cứu thêm người ngoài phạm vi.

## Bước 1 — DTO và nhãn dùng chung

- [x] RED: test `formatStudentLabel` cho trùng tên, thiếu mã, fallback email; test GroupWorkspaceService/GroupProposalService khẳng định JSON DTO có mã từ contact hiện có.
- [x] GREEN: thêm helper vào `frontend/src/shared/studentCode.ts`; thêm optional fields trong `frontend/src/features/{groups,groupdocs}/model/types.ts`; thêm trường cuối có thể null vào `GroupWorkspaceService.Member` và `GroupProposalService.InvitationView`, giữ constructor cũ nếu caller cần.
- [x] Chạy các test backend liên quan và shared/studentCode.

## Bước 2 — Nhóm và tài liệu nhóm

- [x] RED: test duyệt đề xuất, chọn trưởng nhóm/giao mục và tìm bằng MSSV; kiểm ID gửi vẫn đúng, người không có mã vẫn hiện.
- [x] GREEN: cập nhật `groups/components/{ProposalReviewList,GroupFormDialog,LeaderRequestList,MyGroup,GroupProposalSection,GroupProgressCard}.tsx`.
- [x] GREEN: cập nhật `groupdocs/model/status.ts`, `components/{SectionDialogs,SectionHistoryDialog}.tsx`; các caller GroupWorkspacePage/GroupProgressCard dùng nhãn chung nhận mã tự động.
- [x] Chạy các suite groups/groupdocs bị ảnh hưởng.

## Bước 3 — Chấm điểm và chi tiết quiz

- [x] RED: test danh sách/tiêu đề Theo sinh viên; nhãn chấm thành viên/hoạt động; hộp lượt quiz giữ mã của đúng sinh viên.
- [x] GREEN: cập nhật `grading/components/{ClassGradebookTab,GradingReviewPanels}.tsx`, `grading/pages/GradingWorkspacePage.tsx` và `quizpractice/components/TeacherQuizSections.tsx`; dùng component/label có sẵn, không đổi logic chấm.
- [x] Chạy các suite grading/quizpractice bị ảnh hưởng.

## Bước 4 — Kiểm chứng và bàn giao

- [x] ESLint file đổi, TypeScript (tách cache Next nếu cần, không sửa cấu hình gốc) và test frontend nhóm bị ảnh hưởng.
- [x] Test backend cho groupdocs/groups, rà diff và các caller; kiểm không thay dữ liệu/quyền/log/prompt.
- [x] Cập nhật kết quả, audit và aidlc-state cùng các checkbox. Ghi rõ giới hạn browser đăng nhập/runtime backend chưa restart nếu có.


## Kết quả kiểm chứng — 2026-11-16

| Kiểm tra | Lệnh/phạm vi | Kết quả |
|---|---|---|
| RED frontend | Bảy test mới của helper, nhóm, tài liệu nhóm, chấm điểm và quiz | Thất bại đúng các nhãn còn thiếu mã trước khi sửa |
| RED backend | `mvn -q -Dtest=GroupWorkspaceServiceTest,GroupProposalServiceTest test` | Hai test JSON thất bại vì thiếu `studentCode`/`leaderStudentCode` |
| Frontend regression | `npx vitest run tests/shared/studentCode.test.ts tests/features/groups tests/features/groupdocs tests/features/grading tests/features/quizpractice --reporter=dot` | 40 file, 338/338 test đạt |
| Backend regression | `mvn -q -Dtest=edu.aiplatform.groupdocs.**,edu.aiplatform.groups.** test` | 26 suite, 141/141 test đạt, không skip; có Testcontainers PostgreSQL/RabbitMQ tách biệt |
| Lint | `npx eslint` với danh sách file frontend đã đổi từ `git diff --name-only` | Exit 0 |
| TypeScript | `npx tsc --noEmit --incremental false` | Exit 0, dùng cấu hình gốc |
| Diff | `git diff --check`, rà caller và DTO constructor | Đạt; không thay schema, phân quyền, ID payload hoặc prompt/log |
| Review độc lập | Agent chỉ đọc diff và code liên quan | Không phát hiện lỗi cần sửa; reviewer không chạy lại test/browser |
| Runtime | Backend restart cùng terminal Orca, profiles local/worker; frontend hot reload | Backend `/health` UP, frontend `/login` HTTP 200 |

Nhãn có mã dùng `PersonSubline`/`formatStudentLabel`. API chỉ thêm hai trường nullable từ contact vốn đã đọc. Quiz ghép đúng MSSV theo `attemptId`; không cần thêm API detail. Bộ chọn giao mục tìm được theo MSSV và vẫn gửi đúng accountId. Test cũ với người không có mã vẫn đạt; nhãn “Bạn”, người đã rời nhóm và mục chưa có người giữ nguyên fallback.

Security: compliant trong phạm vi — không mở thêm nguồn tra cứu/quyền, không đưa mã vào AI/log, DTO chỉ dùng contact đã được phép xem. Resiliency: compliant — giữ xử lý contact lỗi và mã rỗng; không đổi ghi dữ liệu hay lỗi lưu. PBT: N/A vì disabled trong aidlc-state.

Giới hạn: chưa kiểm tra browser có đăng nhập trên màn rộng/hẹp; Orca không có tab browser/phiên phù hợp. Chưa chạy toàn bộ suite của những module ngoài phạm vi và production build. Suite frontend có cảnh báo React act/duplicate fixture keys và jsdom scroll/navigation từ các test hiện hữu, không có test thất bại.

Task 1–3: RED→GREEN; regression frontend 338/338, backend 141/141. Final: clean — review độc lập không có finding. Không commit/push; giữ ba báo cáo AUDIT chưa theo dõi.


## Browser follow-up — 2026-11-16

Người dùng yêu cầu mở browser và chạy test UI; đã đăng nhập bằng tài khoản giảng viên/sinh viên local do người dùng cung cấp. Đã kiểm tra các luồng hiện có ở 1440×900 và 390×844, gồm gradebook, chọn trưởng nhóm, chấm thành viên và lịch sử tác giả. Các tài khoản đều chưa có MSSV, nên phần hiển thị mã khác rỗng dùng fixture GET chỉ trong browser; đã gỡ sau kiểm tra, không ghi dữ liệu học tập.

Xem [báo cáo UI và bằng chứng](../../build-and-test/2026-11-16-student-code-ui-browser-results.md). Không phát hiện regression MSSV trong ca đã chạy. Giao mục chưa thử vì bài CLOSED; duyệt đề xuất/lời mời chưa có ngữ cảnh phù hợp; hộp quiz chưa thử vì 0 lượt. Có thông báo CSP inline style khi mở quiz, nguồn chưa xác định, không kết luận là lỗi MSSV. Không tính các ca thiếu dữ liệu là PASS.


## Tinh chỉnh bảng Sinh viên theo ảnh phản hồi — 2026-11-16

Người dùng yêu cầu tách MSSV khỏi email và định dạng chữ dễ đọc. Phạm vi cụ thể được chỉ trực tiếp trong ảnh: bảng Sinh viên trong ClassGroupsPanel; dùng TableFrame/Th/Td hiện có. Cột theo thứ tự MSSV, Họ tên, Email, Nhóm, Ngày vào lớp, Bảng điểm; có độ rộng tối thiểu và cuộn ngang trong bảng trên màn hẹp, tooltip cho tên/email/nhóm dài. Thiếu mã hoặc email hiện dấu gạch ngang.

- [x] Sửa ClassGroupsPanel: bỏ PersonSubline ở ô Email, thêm cột MSSV, cân độ rộng/font và đổi empty colspan thành 6. Không thay dữ liệu, tìm kiếm, ID hoặc hành vi bấm dòng/link.
- [x] Chạy suite groups hiện có, ESLint file sửa và TypeScript; không thêm test chỉ để khóa CSS/bố cục trình bày.
- [x] Kiểm tra browser bằng dữ liệu HE19 thật trên màn rộng/hẹp: ô MSSV/email riêng, header khớp dữ liệu, bảng cuộn được và trang không tràn ngang; cập nhật kết quả.


Kết quả tinh chỉnh: chỉ đổi ClassGroupsPanel.tsx; groups Vitest 9 file/50 test đạt, ESLint file đổi và TypeScript cấu hình gốc đạt. Browser dùng dữ liệu HE19 thật, không có fixture: header/row có đúng 6 cột; email không còn kèm MSSV, tìm HE190002 trả đúng 1 người, empty colSpan=6, link Bảng điểm giữ đúng UUID. 1920px hiển thị đủ bảng; 1440px/390px bảng cuộn trong khung, trang không tràn ngang. Đã thử cuộn tới nút Bảng điểm trên mobile. Ảnh và kết quả bổ sung trong báo cáo browser hiện có. Không sửa database/backend hoặc commit/push. Security/Resiliency: giữ phạm vi đọc, ID và hành vi hiện có; PBT disabled.
