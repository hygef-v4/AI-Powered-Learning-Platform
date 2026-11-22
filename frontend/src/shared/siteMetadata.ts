import { msg } from "@/i18n/msg";

export const SITE_NAME = msg("shared.siteMetadata.learningPlatform");
export const SITE_TAGLINE = msg("shared.siteMetadata.teachAndLearnBetterWith");
export const SITE_TITLE = `${SITE_NAME} – ${SITE_TAGLINE}`;
export const SITE_DESCRIPTION =
  msg("shared.siteMetadata.manageClassesMaterialsAssignmentsAnd");
export const SITE_URL = process.env.SITE_URL ?? "https://alapedu.online";
export const SITE_HOST = new URL(SITE_URL).host;

/** Tên thương hiệu ngắn, khớp tên miền; dùng cho dữ liệu cấu trúc và nội dung landing để tìm "ALAP" ra đúng trang. */
export const SITE_BRAND = "ALAP";
/** Chỉ production đặt `SEO_INDEXABLE=true`; local, staging và mọi môi trường khác không cho máy tìm kiếm index. */
export const SEO_INDEXABLE = process.env.SEO_INDEXABLE === "true";
/** Mã xác minh Google Search Console (thẻ meta); rỗng thì không xuất thẻ. */
export const GOOGLE_SITE_VERIFICATION = process.env.GOOGLE_SITE_VERIFICATION?.trim() || undefined;
/** Các trang công khai được phép index và có trong sitemap. Mọi trang khác cần đăng nhập. */
export const PUBLIC_PATHS = ["/", "/login", "/privacy"] as const;
/** Metadata `robots` cho trang không bao giờ được index (trang cần đăng nhập, liên kết kích hoạt/đặt lại mật khẩu). */
export const NO_INDEX = { index: false, follow: false } as const;

/** URL tuyệt đối của một đường dẫn trong trang, theo `SITE_URL`. */
export function buildSiteUrl(path: string): string {
  return new URL(path, SITE_URL).toString();
}
