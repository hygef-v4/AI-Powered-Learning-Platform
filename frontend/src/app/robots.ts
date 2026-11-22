import type { MetadataRoute } from "next";
import { SEO_INDEXABLE, buildSiteUrl } from "@/shared/siteMetadata";

// Đọc SEO_INDEXABLE lúc chạy, không đóng băng giá trị của lúc build image.
export const dynamic = "force-dynamic";

/**
 * robots.txt. Production cho bot đọc trang (kể cả CSS/JS để dựng trang) và tự loại các trang cần đăng nhập nhờ thẻ `noindex`
 * của chúng; chặn trước lời gọi API. Môi trường khác chặn toàn bộ.
 */
export default function robots(): MetadataRoute.Robots {
  if (!SEO_INDEXABLE) {
    return { rules: { userAgent: "*", disallow: "/" } };
  }
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/api/", "/post-login"] },
    sitemap: buildSiteUrl("/sitemap.xml"),
  };
}
