import type { Metadata } from "next";
import { headers } from "next/headers";
import { buildStructuredData, serializeStructuredData } from "@/features/landing/structuredData";
import { LandingPage } from "@/features/landing/pages/LandingPage";
import { loadServerTranslator, requestLocale } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const t = await loadServerTranslator();
  const title = t("landing.pages.LandingPage.metaTitle");
  const description = t("landing.pages.LandingPage.metaDescription");
  return {
    title: { absolute: title },
    description,
    alternates: { canonical: "/" },
    openGraph: { title, description, url: "/" },
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
      description: t("landing.pages.LandingPage.metaDescription"),
      language: locale,
    }),
  );
  return <LandingPage t={t} structuredData={structuredData} nonce={nonce} />;
}
