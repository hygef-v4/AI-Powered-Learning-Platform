# Chấm thành viên cạnh bài nhóm — 2026-11-13

## Thay đổi

- U09 dùng slot sidePanel optional của DocumentEditor: đúng hai cột; caller khác
  giữ CommentRail/inline comments như trước. Khung phải sticky, cuộn độc lập.
- GroupGradingPanel có hai tab Rubric nhóm và Thành viên; màn hẹp là ngăn kéo
  modeless, focus khi mở, Escape/nút đóng, không khóa bài phía sau. Form components
  vẫn mount khi ẩn ngăn kéo/tab; chỉ DOM của người đang chọn hiển thị.
- MemberContributionPanel có Select/Trước/Sau, mục phụ trách, mức, điểm và textarea
  căn cứ. Bỏ danh sách form dài ở cuối bài. Chưa lưu xuất hiện cạnh tên/option.
- sectionAuthors là nguồn duy nhất cho nhãn và liên kết hai chiều. Giữ đủ tác giả
  nếu nhiều người cùng mục; mục không xác định không tự quy cho người khác.
- Đi tới mục dùng document:show-part và highlight, hỗ trợ Cả trang/Từng phần.
  Chuyển người/tab đặt lại cuộn khung phải, không tự cuộn hoặc lọc bài bên trái.
- Draft theo từng member ID giữ khi đổi người, reload hoặc lưu người khác. Version
  mới chỉ rebase khi giá trị server không xung đột với bản gốc. Có xung đột thì giữ
  draft/version cũ, API từ chối thay vì âm thầm ghi đè điểm mới.
- Dirty bổ sung vào cảnh báo rời bài/beforeunload; autosave điểm nhóm không lưu
  điểm cá nhân. Công bố bị nhắc lưu nếu còn draft thành viên.

Không sửa backend, AI, schema, quyền, công thức điểm hoặc dữ liệu điểm thật.

## Kiểm thử tự động

| Kiểm tra | Kết quả | Log |
|---|---|---|
| RED trước sửa | 3 fail mới, 44 control pass | .tmp/group-side-panel-red.log |
| Feature ban đầu | 47/47, hai file | .tmp/group-side-panel-green.log |
| Toàn frontend | 1056/1056, 151 file, 0 unhandled error | .tmp/group-side-panel-full-tests.log |
| Kiểm cuối sau thêm case/phần cuộn-highlight | 50/50, hai file | .tmp/group-side-panel-final-cases.log |
| ESLint grading/shared file và tests | Exit 0 | .tmp/group-side-panel-lint.log |
| TypeScript toàn src/tests, cache route hợp lệ | Exit 0 | .tmp/group-side-panel-types.log |

Full suite chạy trước chỉnh cuối chỉ giới hạn ở cuộn khung/clear highlight khi chọn
người. Các chỉnh cuối có kiểm workspace/component 50 test và lint/TypeScript qua;
không báo 1059 test là một lượt full mới chưa chạy.

Case mới kiểm chọn/lưu mức không đổi điểm, giữ draft qua chuyển người/tab/drawer,
lưu một người không xóa người khác, refresh version không đổi giá trị, conflict
giữ draft, jump section, đúng tác giả/cùng mục nhiều tác giả, cảnh báo rời bài và
không công bố draft cá nhân. Shared document và các caller được chạy trong full suite.

Lượt GREEN đầu sau source có 47 assertion pass nhưng một unhandled exception do
jsdom thiếu scrollIntoView; không tính là thành công. Bổ sung mock API cuộn trong
test, đợi callback xong và khôi phục prototype sau test. Lượt typecheck đầu bắt
fixture paragraph mới dùng text legacy; sửa fixture về runs canonical rồi pass.
Normal typecheck vẫn vướng validator .next cũ đã biết; dùng config tạm trong frontend
include toàn source/tests và .next-ai-chat-files/types, không đổi config sản phẩm,
không xóa cache. Config tạm đã xóa sau kiểm chứng.

## UI local

Đăng nhập bằng teacher test đã được cho phép, mở lớp SE1933 → Chấm điểm → bài nhóm
SE_JS QA → bản nộp chung hiện có, evaluation97184dbb-8fe1-495f-87ad-fa3fddb8adde.

- 1440x1000: khung nằm đúng cạnh bài, sticky khi jump tới mục3; tác giả Student3
  được chọn từ nút ngay tại mục, liên kết ngược trỏ đúng mục REST API.
- Gõ ghi chú QA chỉ trong state trình duyệt, chuyển Student4 rồi về Student3:
  ghi chú còn nguyên; hiện Chưa lưu. Không bấm lưu/công bố/AI.
- 390x844: ngăn kéo nằm trong viewport, không tràn ngang (scrollWidth380 ≤390).
  Đóng bằng Escape rồi mở lại vẫn giữ ghi chú. Xóa lại ghi chú QA về giá trị rỗng
  ban đầu, xác nhận không còn nhãn Chưa lưu.
- Chuyển Từng phần và bấm mục phụ trách: mở đúng heading REST API và đóng ngăn kéo.
- Khôi phục viewport554x930. Ảnh đã mở kiểm tra trực quan:
  .tmp/group-grading-desktop.png và .tmp/group-grading-mobile.png (ảnh cuối không có QA text).

Ban đầu phiên cũ hết hạn sau restart; đăng nhập lại qua UI. Một ref cũ không còn
hợp lệ sau đổi chế độ xem; snapshot lại rồi dùng ref mới, không gửi lặp thao tác ghi.
Console CSP inline style warnings có sẵn như các lần kiểm trước, ngoài phạm vi này.

## Review và giới hạn

Tự đối chiếu mọi caller DocumentEditor/useGradingWorkspace/MemberContributionPanel,
slot mặc định, breakpoint1280 dùng chung, dirty state, rebase/version, hidden
components và mapping section. Security/Resiliency phạm vi hiện hữu giữ nguyên;
PBT disabled. Không thêm dependency. Không subagent, browser mock API hay dữ liệu giả
cho UI acceptance; chỉ mock dependencies trong unit tests.

Chưa chạy production build hoặc gọi API lưu điểm trên UI trong lượt này. Không
restart BE, gọi AI, thay điểm thật, commit/push/deploy. Kiểm server lưu 5 mức vẫn là
kết quả scoped backend55/55 của thay đổi trước, không chạy lại vì delta này chỉ frontend.

## Fresh local commit check

At the later explicit commit request, reran backend55/55 and frontend grading/shared
DocumentEditor/submission-history276/276 across26 files, all passing. No source
changes during delivery. Logs .tmp/grading-assessments-precommit-backend.log and
.tmp/grading-assessments-precommit-frontend.log. Data/API, UI and documentation are
separate local commits. No push/merge/deploy, production build or real grade write.
