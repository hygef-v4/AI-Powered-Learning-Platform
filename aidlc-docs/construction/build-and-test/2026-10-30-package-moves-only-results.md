# Chuyển file/package — kết quả hiện tại 2026-10-30

Phạm vi theo yêu cầu mới nhất: “tôi k muốn commit xóa file ấy , như v thì sẽ mất commit cũ của người khác tôi chỉ muốn chuyển file sang folder package khác để sắp xếp lại thôi”. Thực hiện trong working tree nhánh `aidlc-docs`, parent HEAD `5a76e428`; chưa stage, commit hoặc push.

## Thay đổi giữ lại

- Giữ đủ 1.501 file nguồn/tài nguyên gốc; 1.066 file chuyển đường dẫn (716 Java, 308 frontend và 42 file `.gitkeep`). File không chuyển chỉ cập nhật tham chiếu nếu cần.
- Backend giữ feature/unit, chuyển vào controller/service/repository/model/dto/config/integration phù hợp. Factory và helper package-private giữ cùng package với caller khi cần, không đổi visibility để ép vào tầng.
- Frontend giữ feature, nhóm file vào api/model/pages/components/hooks; cập nhật import, mock và đường dẫn route caller. Không thêm barrel hay tách component/hook mới.
- Khôi phục toàn bộ code gốc của AcademicsService, DocumentModelService, DocumentEditor, AiDraftDialog và Admin MCP. Hoàn tác các phần tách logic, tối ưu batch, dọn/xóa code và thêm test của bản refactor rộng.
- Giữ test/fixture gốc; chỉ đổi package/import/tham chiếu. ArchitectureTest dùng tên tầng mới thay tên tầng cũ; script required-test dùng FQN mới.
- Đồng bộ README, inventory và tài liệu Construction theo vị trí hiện tại. [Báo cáo bản rộng](2026-10-30-code-structure-refactor-results.md) được đánh dấu đã thay thế.

## Đối chiếu nội dung và Git

Đã so 908 file Java: phần thân giữ nguyên sau khi chuẩn hóa package/import và tham chiếu FQN/package cần chuyển. Đã so AST của 506 file TypeScript/TSX: chỉ khác module/import path. Tập file nguồn/tài nguyên hiện tại tương ứng 1:1 với baseline; không có file gốc bị loại bỏ.

Preview dùng index tạm, không thay index thật: Git nhận **1.066 rename**, 86 file sửa tham chiếu tại chỗ và **0 addition/deletion không ghép cặp**, với `--find-renames=20%`. Các file nhỏ có thể cần ngưỡng similarity thấp để hiển thị rename. Git lưu snapshot và suy ra rename khi so sánh; màn hình working tree có thể vẫn hiện deleted/untracked trước khi stage. Chuyển đường dẫn không xóa commit cũ; có thể tra lịch sử file bằng `git log --follow -- <đường dẫn mới>` sau khi commit.

HEAD và index thật giữ nguyên; không rewrite lịch sử hay tạo commit.

## Kiểm tra mới trên bản chỉ chuyển file

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvn clean verify` | 1.611 passed, 0 failures/errors/skips; BUILD SUCCESS |
| Required PostgreSQL suites | SubjectRepositoryIntegrationTest 31, NotificationDeliveryIntegrationTest 2; không skip |
| Frontend Vitest | 599 passed, 94 files, `--maxWorkers=2` |
| TypeScript | Passed |
| ESLint | Exit 0; hai cảnh báo unused import có sẵn: `navEntries`, `Icon` |
| Frontend production build | Passed |
| Nội dung và tài liệu hiện hành | 908 Java bodies, 506 TypeScript/TSX ASTs; 46 controller annotation sets; Markdown links/fences và `git diff --check` passed |

Log local: `.tmp/2026-10-30-moves-only-{backend,db-guard,frontend,typecheck,lint,build}.log`. Rename proof: `.tmp/2026-10-30-moves-only-git-evidence.json`. Java/TypeScript body comparison và inventory được kiểm riêng; không thêm regression test mô phỏng thao tác chuyển file.

Không đổi dependency/lockfile, public unit contract, DB schema/migration hay cấu hình hạ tầng. Test chạy bằng JDK 21 target Java 17, Node 25 và disposable DB/Redis; khác runtime CI Java 17/Node 22. Không chạy browser E2E/provider thật/load/restore acceptance; không suy ra nghiệm thu đầy đủ từ các suite này. Extension security/resiliency giữ phạm vi hiện có; PBT vẫn tắt.
