import type { Metadata } from "next";
import Link from "next/link";
import { translateOnServer, loadServerRichTranslator, loadServerTranslator } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.privacy.page.privacyPolicy") };
}

/** Trang công khai, không cần đăng nhập: chính sách quyền riêng tư (Google yêu cầu khi publish OAuth app). */
export default async function PrivacyPage() {
  const t = await loadServerTranslator();
  // Câu có phần in đậm: <b>…</b> trong tệp ngôn ngữ thành <strong>.
  const rich = await loadServerRichTranslator();
  const renderBold = (key: string) => rich(key, { b: (chunks) => <strong className="font-semibold text-ink">{chunks}</strong> });
  const siteName = t("shared.siteMetadata.learningPlatform");
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-3xl flex-col gap-6 px-4 py-12">
      <header className="flex flex-col gap-2">
        {/* Chân trang chỉ có ở màn đăng nhập nên nút quay lại về trang đăng nhập (yêu cầu 2026-11-16). */}
        <Link href="/login" className="text-[13px] font-semibold text-primary-ink underline underline-offset-4">
          ← {t("identity.otp.components.OtpFlow.backToSignIn")}
        </Link>
        <h1 className="font-display text-[28px] leading-tight text-ink">{t("app.privacy.page.privacyPolicy")}</h1>
        <p className="text-[13px] text-muted">{t("app.privacy.page.lastUpdated15112026")}</p>
      </header>

      <article className="flex flex-col gap-5 text-[14.5px] leading-7 text-body">
        <p>
          {siteName} {renderBold("app.privacy.page.isATeachingAndLearning")}</p>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.googleDriveAccess")}</h2>
          <p>
            {t("app.privacy.page.theSystemsGoogleIntegrationOnly")}<code className="rounded bg-sunken px-1 py-0.5 font-mono text-[13px]">drive.file</code>{t("app.privacy.page.theSystemCanOnlySee")}<strong>{t("app.privacy.page.theFilesAndFoldersIt")}</strong>{renderBold("app.privacy.page.inTheMyDriveOf")}
          </p>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.dataTheSystemStores")}</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>{renderBold("app.privacy.page.filesUploadedByUsersStored")}</li>
            <li>{renderBold("app.privacy.page.accountInformationNeededToOperate")}</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.whatTheSystemDoesNot")}</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>{renderBold("app.privacy.page.itDoesNotSellOr")}</li>
            <li>{renderBold("app.privacy.page.itDoesNotUseUsers")}</li>
            <li>{renderBold("app.privacy.page.googleCredentialsLiveOnlyIn")}</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.contact")}</h2>
          <p>
            {renderBold("app.privacy.page.questionsAboutThisPolicyContact")}{" "}
            <a href="https://github.com/hygef-v4/AI-Powered-Learning-Platform" target="_blank" rel="noreferrer"
              className="text-primary-ink underline underline-offset-4">
              {t("app.privacy.page.sourceRepository")}</a>.
          </p>
        </section>
      </article>
    </main>
  );
}
