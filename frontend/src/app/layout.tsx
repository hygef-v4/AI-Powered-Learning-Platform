import type { Metadata } from "next";
import type { ReactNode } from "react";
import { headers } from "next/headers";
import { connection } from "next/server";
import { SITE_URL } from "@/shared/siteMetadata";
import { LocaleGate } from "@/i18n/LocaleGate";
import { DEFAULT_LOCALE, OG_LOCALES } from "@/i18n/locales";
import { requestLocale, loadServerTranslator } from "@/i18n/server";
import { THEME_BOOT_SCRIPT } from "@/shared/themeBoot";
import { Toaster } from "@/shared/ui/Toast";
import "../shared/styles/globals.css";
import "../shared/styles/fonts.css";

/** Tên, mô tả của trang theo ngôn ngữ người dùng đã chọn. */
export async function generateMetadata(): Promise<Metadata> {
  const locale = await requestLocale();
  const t = await loadServerTranslator();
  const name = t("shared.siteMetadata.learningPlatform");
  const title = `${name} – ${t("shared.siteMetadata.teachAndLearnBetterWith")}`;
  const description = t("shared.siteMetadata.manageClassesMaterialsAssignmentsAnd");
  return {
    metadataBase: new URL(SITE_URL),
    title: { default: title, template: `%s · ${name}` },
    description,
    applicationName: name,
    openGraph: { type: "website", locale: OG_LOCALES[locale], siteName: name, title, description },
    twitter: { card: "summary_large_image", title, description },
  };
}

const PRELOAD_FONTS = [
  "/fonts/plus-jakarta-sans/normal-latin.woff2",
  "/fonts/plus-jakarta-sans/normal-vietnamese.woff2",
];

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Per-request rendering keeps framework scripts aligned with the CSP nonce.
  await connection();
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const locale = await requestLocale();
  return (
    <html lang={locale} suppressHydrationWarning>
      <head>
        {/* Đặt theme trước khi vẽ để không nháy sáng khi đang dùng giao diện tối. */}
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: THEME_BOOT_SCRIPT }} />
        {PRELOAD_FONTS.map((href) => (
          <link key={href} rel="preload" href={href} as="font" type="font/woff2" crossOrigin="anonymous" />
        ))}
      </head>
      <body>
        <LocaleGate deferred={locale !== DEFAULT_LOCALE}>{children}</LocaleGate>
        <Toaster />
      </body>
    </html>
  );
}
