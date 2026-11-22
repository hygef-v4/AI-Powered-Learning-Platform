import type { MetadataRoute } from "next";
import { PUBLIC_PATHS, buildSiteUrl } from "@/shared/siteMetadata";

export const dynamic = "force-dynamic";

/** sitemap.xml: chỉ các trang công khai, URL tuyệt đối theo SITE_URL. */
export default function sitemap(): MetadataRoute.Sitemap {
  return PUBLIC_PATHS.map((path) => ({
    url: buildSiteUrl(path),
    changeFrequency: "monthly",
    priority: path === "/" ? 1 : 0.5,
  }));
}
