# SEO cho alapedu.online — Kết quả Build and Test

Ngày: 2026-11-22 · Nhánh `feat/seo` (từ `origin/staging@47837e52`) · [Kế hoạch](../../plans/code-generation-plan/2026-11-22-seo.md)

## 1. Đã thay đổi

| Yêu cầu | Thay đổi |
|---|---|
| SEO-FR1, FR2 | `/` là landing dựng phía máy chủ (`features/landing`), song ngữ en/vi. `SignedInRedirect` hỏi `/api/v1/me/profile` từ trình duyệt, có phiên thì `router.replace(homePath)`, không phiên hoặc lỗi thì giữ landing. `/post-login` giữ `PostLoginRedirect`. |
| SEO-FR3 | `app/robots.ts` (dynamic): production `Allow: /`, `Disallow: /api/`, `/post-login`, trỏ tới sitemap; khác production `Disallow: /`. **Điều chỉnh so với kế hoạch:** không chặn các trang cần đăng nhập trong robots.txt, vì bot bị chặn thì không đọc được thẻ `noindex` và không tải được CSS/JS; các trang đó bị loại nhờ `noindex` (FR5). |
| SEO-FR4 | `app/sitemap.ts` (dynamic): `/`, `/login`, `/privacy`, URL tuyệt đối theo `SITE_URL`. |
| SEO-FR5 | `robots: noindex, nofollow` ở 15 layout khu vực đăng nhập và các trang `activate`, `reset-password`, `post-login`; root layout đặt `noindex` cho mọi trang khi `SEO_INDEXABLE` khác `true`. |
| SEO-FR6 | Canonical cho `/`, `/login`, `/privacy`. |
| SEO-FR7 | JSON-LD `Organization` + `WebSite` + `SoftwareApplication` (EducationalApplication), có nonce CSP, `<` được mã hóa. |
| SEO-FR8 | `verification.google` từ `GOOGLE_SITE_VERIFICATION`. |
| SEO-FR9 | Staging nginx: `X-Robots-Tag: noindex, nofollow`. Compose staging ép `SEO_INDEXABLE=false`; compose production mặc định `true` để bản deploy đầu tiên không làm production mất index khi `.env` chưa có biến. |

Không đổi API, schema hay backend. CSP, header bảo mật và `Cache-Control: private, no-store` giữ nguyên.

## 2. Kiểm tra

| Hạng mục | Kết quả |
|---|---|
| ESLint | Đạt, 0 lỗi |
| `tsc --noEmit` | Đạt |
| Vitest toàn bộ | 159 file, 1.203/1.203 ca đạt (gồm 12 ca mới: landing, chuyển hướng, JSON-LD, robots, sitemap; và kiểm khóa en/vi khớp nhau) |
| `next build` | Biên dịch xong và in đủ bảng route (`/robots.txt`, `/sitemap.xml` là route động), sau đó lệnh dừng với `SecurityFatalError` của Next.js báo phiên bản `16.4.3` có lỗ hổng đã biết và đề nghị nâng cấp. Không chạy lại và không nâng cấp trong lượt này; cần người dùng quyết định (xem mục 4). |
| Dev server (worktree, `SEO_INDEXABLE=true`) | `robots.txt` và `sitemap.xml` trả 200 đúng nội dung; HTML `/` khi không chạy JS có title, description, canonical, thẻ xác minh Google, H1 và JSON-LD có nonce; `/classes`, `/admin`, `/post-login`, `/reset-password` có `noindex, nofollow`; `/login`, `/privacy` có canonical. |
| Giao diện | Desktop theme tối, 375 px theme sáng tiếng Việt: không tràn ngang, một H1. Không có backend nên lời gọi profile trả 500 và landing được giữ nguyên như mong đợi. Lỗi CSP inline style trong console xuất hiện y hệt ở trang `/privacy` chưa sửa, là do công cụ dev của Next, không phải do landing. |

Chưa kiểm: nhánh `SEO_INDEXABLE=false` của root layout trên trình duyệt (robots.txt nhánh này có unit test); người đã đăng nhập thật được chuyển vào dashboard (chỉ có unit test vì không chạy backend); Docker image và nginx staging thật.

## 3. Hướng dẫn Google Search Console

1. Nếu property đang xác minh bằng thẻ meta: trong Search Console → Settings → Ownership verification → HTML tag, chép phần `content="..."`; đặt `GOOGLE_SITE_VERIFICATION=<giá trị>` trong `/opt/platform/infra/.env` trên VPS. Đã xác minh bằng DNS thì để trống.
2. Không cần đặt `SEO_INDEXABLE` ở production (mặc định `true`); staging luôn `false`.
3. Sau khi deploy: mở `https://alapedu.online/robots.txt` và `/sitemap.xml` để kiểm; trong Search Console → Sitemaps gửi `https://alapedu.online/sitemap.xml`; dùng URL Inspection cho `https://alapedu.online/` rồi Request indexing.
4. Kiểm dữ liệu cấu trúc bằng Rich Results Test hoặc Schema Markup Validator.

## 4. Còn mở

- Next.js `16.4.3` bị chính lệnh build đánh dấu có lỗ hổng bảo mật; cần quyết định nâng cấp trong một thay đổi riêng.
- Googlebot không có cookie ngôn ngữ nên index bản tiếng Anh. Muốn xếp hạng cho truy vấn tiếng Việt cần URL riêng theo ngôn ngữ (ví dụ `/vi`) và hreflang — ngoài phạm vi lần này.
- Tên hiển thị trên logo và tiêu đề các trang khác vẫn là "Learning Platform"; landing và JSON-LD dùng "ALAP". Đồng bộ khi chốt nhận diện mới.
