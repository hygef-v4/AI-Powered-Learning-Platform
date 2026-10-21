# U13 — Developer verification và bàn giao tester

Ngày 2026-10-21. Chạy từ workspace `E:/GitHub/AI-Powered-Learning-Platform`. Không cần thay DTO U06.

## Lệnh tái hiện

Backend, từ `backend/`:

```powershell
mvn -B verify
mvn -B '-Dtest=edu.aiplatform.materials.**,ArchitectureTest' package
```

Frontend, từ `frontend/`:

```powershell
npm run typecheck
npm run lint
npm test -- --maxWorkers=4
npm run build
```

Windows sandbox có thể chặn SWC canonicalize workspace, ghi Maven cache hoặc Vitest temp/IPC. Trong phiên này, cấp quyền chạy các lệnh kiểm tra và dùng process-local TEMP/TMP tại `.tmp/u13` khắc phục; không sửa dependency/config test để bỏ lỗi. Dev QA dùng Webpack; production build dùng script Webpack sẵn có của repo. Không dùng số liệu từ các lần bị ngắt/lỗi môi trường để tính pass.

## Kết quả

| Kiểm tra | Kết quả |
|---|---|
| Full backend verify | 928 total / 851 pass / 77 skip / 0 failure / 0 error; executable JAR |
| U13 + ArchitectureTest sau review cuối | 80 pass / 0 failure / 0 error / 0 skip; gồm 78 U13 + 2 architecture; executable JAR packaged |
| PDF/DOCX/PPTX thực trong JVM con | Pass; deadline cancel/close và worker race/idempotence có unit tests |
| Packaged parser smoke | Pass; chữ PDF và page count từ PropertiesLauncher trong executable JAR |
| Full frontend | 321 pass / 61 files, gồm 29 ca U13 |
| Typecheck / lint cuối | Pass; lint không có warning/error |
| Production standalone build | Pass; cả ba route U13 trả200 trên standalone, CSP/blob/YouTube/nosniff đúng |
| OpenAPI | JSON/YAML1.2 syntax và local references pass |
| Browser fixture | List/create/metadata/replace/archive/video error state pass; mobile overflow đã sửa và kiểm lại |
| Native PDF browser render | Chưa xác nhận: blob iframe nạp nhưng in-app browser để trống vùng native viewer |

Logs chỉ là bằng chứng cục bộ ignored: `.tmp/u13/backend-verify.log`, `backend-handoff.log`, `frontend-all3.log`, `typecheck-final.log`, `lint-final.log`, `build.log`. Full backend chạy trước hai sửa review cuối; focused run sau cùng kiểm phần thay đổi đó. Không cộng số test của hai lần với nhau.

## Matrix dành tester độc lập

### SEC-004 correction — 2026-10-21

- Replaced scoped static U13 headers with `frontend/src/proxy.ts` for all HTML page routes. Root layout awaits `connection()` so pages render per request; static HTML caching is intentionally replaced by request rendering for nonce correctness. API/static assets retain their existing serving paths.
- Each response uses a fresh cryptographically random 32-byte nonce. Request CSP and x-nonce are overwritten before rendering; production script-src uses nonce + strict-dynamic without unsafe-inline/eval. X-Frame-Options DENY agrees with frame-ancestors none. HSTS, nosniff and referrer headers retained. Child frame allowlist retains same-origin/blob PDF, youtube-nocookie.com and approved embed.diagrams.net.
- Stylesheets/style elements use self/nonce. The only production unsafe-inline exception is `style-src-attr`, required by existing React dynamic style attributes (for example drag/layout state); it does not allow inline scripts. Development alone permits eval for Next/React debugging and WebSockets for HMR. Do not carry these development exceptions into production.
- Focused proxy + preview suite: 6 pass. Full frontend after correction: **323 pass / 62 files**, including 2 nonce/security regression cases. Typecheck, lint and production standalone build pass. No backend source changes or backend test rerun for this correction.
- Standalone HTTP smoke: login, manager list, detail, view, 404 and repeat login all have required headers; every script tag nonce matches response CSP, nonce differs between requests, client-supplied nonce/CSP are overwritten, assets return200 and HTML is private/no-store. Local evidence: `.tmp/u13/security-smoke.log`, `security-build.log`, `security-tests-final.log`.
- Browser production: login password-toggle hydration works; U13 fixture list -> PDF -> list -> video client navigation works; YouTube player loaded (fixture video unavailable), no CSP/hydration errors in console. PDF blob iframe loads but native viewer remains blank in the in-app browser, so Chrome/Edge native render acceptance remains pending. Draw.io allowlist verified in configuration; no full editor acceptance claimed.
- Windows sandbox full-test attempt failed on temp/IPC and build on SWC canonicalization; elevated build and full test run with process-local TEMP/TMP succeeded. An additional thread-pool run was stopped after the successful full-suite evidence was available; it is not counted as a pass.
- Deployed Nginx/HTTPS/header composition remains an integration check; do not add a conflicting second CSP at the reverse proxy or cache nonce-bearing HTML publicly.

| Nhóm | Ca cần chạy trên provider/schema thật | Kỳ vọng |
|---|---|---|
| Migration | Chạy toàn chuỗi owner migrations + Hibernate validate, role app | Hai bảng/FK/indexes/checks khớp; không DELETE; không tạo schema giả cho unit khác |
| Quyền | Bốn role trên toàn endpoint và MCP, anonymous, current account disabled/đổi role | Admin denied; actor chỉ phía server; ngoài scope404 và denied audit |
| Môn/lớp | SM chỉ phụ trách môn; SM không dạy lớp; teacher dạy lớp khác; learner lớp khác cùng môn | Không đọc tài liệu riêng lớp khác; subject shared chỉ theo quyền hợp lệ |
| Lifecycle | Môn/lớp archive; learner removed; lớp DRAFT/ARCHIVED; scope đổi giữa thao tác | Write bị chặn; student chỉ OPEN enrollment; lifecycle race được kiểm có DB |
| Transaction | SUCCESS audit lỗi, insertVersion lỗi, enqueue khi rollback | Material/version/audit atomic; job không publish trước commit hoặc sau rollback |
| Concurrency | Hai replace/update/archive/reorder cùng expected version; reorder foreign/duplicate/stale | Một winner, loser409; không ghi dở hoặc vượt scope |
| Tệp | U03 purpose MATERIAL, owner/fileRef hết hạn/sai, loại/dung lượng thay Settings | Tệp sai bị từ chối; metadata từ U03; không lộ fileId |
| Download | Token5phút, tokenaccountkhác, tokenhết hạn, xem/tải bản cũ | Chặn sai actor/hết hạn; learner chỉ current; staff history theo quyền |
| Extraction | Trùng message, retry5lần, callback sau success, replace trước/đang parse, mất publish | Đúng version và CAS PENDING; sweeper khôi phục current pending; không ghi đè success |
| Parser bounds | PDF/DOCX/PPTX50MB, tài liệu thưa/chứa macro/embedded/ZIP bomb, file lỗi | Không OCR/macro; time/memory bounded; stream/process được dọn; file vẫn xem/tải khi text lỗi |
| AI | 0–10 ID, ID trái quyền trong batch, tổng maxChars, link metadata, archived | Không trả một phần text khi trái quyền; cắt đúng tổng; archived không chọn cho task mới |
| MCP | Credential U06/actor/scope thật, archived staff vs student, version thay trong lúc đọc | Không vượt quyền; version/text cùng snapshot; error có cấu trúc, text untrusted |
| UI | Manager/teacher/student, U05 summary/tab/card, U08/U11 attachments và LearningMaterialSection | Không mất classId; tài liệu môn trong tab lớp chỉ đọc; stale request không ghi đè |
| Browser | Chrome/Edge PDF native viewer, tải PDF/DOCX/PPTX, link mới, YouTube private/deleted, keyboard/focus, mobile | PDF hiện được; fallback tải; không tràn trang; dialog và conflict/retry hoạt động |
| NFR | List/token/extractText và parser dưới tải | p95 ≤300ms/200ms/1s; parser50MB≤2phút; đo và lưu evidence riêng |

Quyết định C04 giữ nguyên U06: không xác nhận version provenance trong DTO nguồn AI liên unit trước bàn giao contract mới. Matrix này không phải kết quả integration đã đạt.
