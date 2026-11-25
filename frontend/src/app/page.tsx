import type { Metadata } from "next";
import { headers } from "next/headers";
import { buildStructuredData, serializeStructuredData } from "@/features/landing/structuredData";
import { LandingPage, landingFaqs } from "@/features/landing/pages/LandingPage";
import { OG_LOCALES } from "@/i18n/locales";
import { loadServerTranslator, requestLocale } from "@/i18n/server";
import { SITE_BRAND_FULL } from "@/shared/siteMetadata";

const K = "landing.pages.LandingPage.";

/**
 * Metadata của trang chủ. Lần đầu vào (và với máy tìm kiếm) trang dựng bằng tiếng Việt, nên tiêu đề, mô tả và từ khóa
 * chứa đúng các cụm người dùng hay tìm: alapedu, chấm điểm bằng AI, vẽ sơ đồ, giao bài, AI tạo đề, làm bài nhóm.
 */
export async function generateMetadata(): Promise<Metadata> {
  const t = await loadServerTranslator();
  const locale = await requestLocale();
  const title = t(`${K}metaTitle`);
  const description = t(`${K}metaDescription`);
  return {
    title: { absolute: title },
    description,
    keywords: t(`${K}metaKeywords`).split(",").map((keyword) => keyword.trim()),
    applicationName: SITE_BRAND_FULL,
    alternates: { canonical: "/" },
    openGraph: { title, description, url: "/", siteName: SITE_BRAND_FULL, locale: OG_LOCALES[locale] },
    twitter: { title, description },
  };
}

/** Trang chủ công khai (SEO-FR1); người đã đăng nhập được chuyển tới dashboard theo role (U01 F15). */
export default async function HomePage() {
  const t = await loadServerTranslator();
  const locale = await requestLocale();
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const structuredData = serializeStructuredData(
    buildStructuredData({
      name: t("shared.siteMetadata.learningPlatform"),
      description: t(`${K}metaDescription`),
      language: locale,
      keywords: t(`${K}metaKeywords`),
      features: ["assign", "diagrams", "aiGrading", "aiDrafting", "groupWork"].map((key) => t(`${K}feature.${key}.title`)),
      faqs: landingFaqs(t),
    }),
  );
  return <LandingPage t={t} locale={locale} structuredData={structuredData} nonce={nonce} />;
}
