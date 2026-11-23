import Link from "next/link";
import { ButtonLink } from "@/shared/ui/Button";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";
import { SITE_BRAND } from "@/shared/siteMetadata";
import { DiagramIllustration } from "../components/DiagramIllustration";
import { SignedInRedirect } from "../components/SignedInRedirect";

type Translate = (key: string) => string;

const K = "landing.pages.LandingPage.";

const FEATURES: { icon: IconName; key: string }[] = [
  { icon: "edit", key: "diagrams" },
  { icon: "sparkle", key: "aiGrading" },
  { icon: "groups", key: "groupWork" },
  { icon: "quiz", key: "quizzes" },
  { icon: "book", key: "materials" },
  { icon: "extension", key: "mcp" },
];

const ROLES: { icon: IconName; key: string }[] = [
  { icon: "school", key: "students" },
  { icon: "person", key: "lecturers" },
  { icon: "crown", key: "subjectManagers" },
];

const STEPS = ["publish", "submit", "review"] as const;

/**
 * Trang chủ công khai (SEO-FR1): dựng sẵn phía máy chủ để máy tìm kiếm đọc được. Người đã đăng nhập được
 * {@link SignedInRedirect} đưa thẳng vào dashboard. `t` là hàm dịch theo ngôn ngữ của lượt tải trang.
 */
export function LandingPage({ t, structuredData, nonce }: { t: Translate; structuredData: string; nonce?: string }) {
  const tr = (key: string) => t(K + key);
  return (
    <div className="flex min-h-dvh flex-col bg-shell text-body">
      <SignedInRedirect />
      <script type="application/ld+json" nonce={nonce} dangerouslySetInnerHTML={{ __html: structuredData }} />

      <header className="border-b border-line bg-shell/90">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <Link href="/" aria-label={tr("home")} className="rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus">
            <Logo />
          </Link>
          <ButtonLink href="/login" variant="primary">{tr("signIn")}</ButtonLink>
        </div>
      </header>

      <main className="flex-1">
        <section className="paper-grid border-b border-line">
          <div className="mx-auto grid w-full max-w-6xl items-center gap-10 px-4 py-14 sm:px-6 md:grid-cols-[1.1fr_1fr] md:py-20">
            <div className="flex flex-col gap-5">
              <p className="text-[13px] font-semibold tracking-wide text-primary-ink uppercase">{SITE_BRAND} · {tr("eyebrow")}</p>
              <h1 className="font-display text-[32px] leading-[1.15] tracking-[-0.02em] text-ink sm:text-[44px]">{tr("headline")}</h1>
              <p className="max-w-xl text-[16px] leading-7">{tr("intro")}</p>
              <div className="flex flex-wrap items-center gap-3 pt-1">
                <ButtonLink href="/login" variant="primary" className="h-11 px-5 text-[14px]">{tr("signIn")}</ButtonLink>
                <a
                  href="#features"
                  className="inline-flex h-11 items-center rounded-lg px-4 text-[14px] font-semibold text-ink underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus"
                >
                  {tr("seeFeatures")}
                </a>
              </div>
              <p className="text-[13px] text-muted">{tr("accountNote")}</p>
            </div>
            <div className="rounded-2xl border border-line-strong bg-surface p-4 shadow-[var(--shadow-float)] sm:p-6">
              <DiagramIllustration className="h-auto w-full" />
            </div>
          </div>
        </section>

        <section id="features" aria-labelledby="features-title" className="mx-auto w-full max-w-6xl scroll-mt-6 px-4 py-14 sm:px-6 md:py-20">
          <h2 id="features-title" className="font-display text-[26px] leading-tight text-ink sm:text-[32px]">{tr("featuresTitle")}</h2>
          <p className="mt-3 max-w-2xl text-[15px] leading-7">{tr("featuresIntro")}</p>
          <ul className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ icon, key }) => (
              <li key={key} className="flex flex-col gap-3 rounded-xl border border-line bg-surface p-5">
                <span className="inline-flex size-10 items-center justify-center rounded-lg bg-primary-soft text-primary-ink">
                  <Icon name={icon} size={20} />
                </span>
                <h3 className="text-[16px] font-semibold text-ink">{tr(`feature.${key}.title`)}</h3>
                <p className="text-[14px] leading-6">{tr(`feature.${key}.body`)}</p>
              </li>
            ))}
          </ul>
        </section>

        <section aria-labelledby="steps-title" className="border-y border-line bg-sunken">
          <div className="mx-auto w-full max-w-6xl px-4 py-14 sm:px-6 md:py-20">
            <h2 id="steps-title" className="font-display text-[26px] leading-tight text-ink sm:text-[32px]">{tr("stepsTitle")}</h2>
            <ol className="mt-8 grid gap-4 md:grid-cols-3">
              {STEPS.map((key, index) => (
                <li key={key} className="flex gap-4 rounded-xl border border-line bg-surface p-5">
                  <span className="inline-flex size-9 shrink-0 items-center justify-center rounded-full bg-primary font-semibold text-on-primary">
                    {index + 1}
                  </span>
                  <div className="flex flex-col gap-1.5">
                    <h3 className="text-[16px] font-semibold text-ink">{tr(`step.${key}.title`)}</h3>
                    <p className="text-[14px] leading-6">{tr(`step.${key}.body`)}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section aria-labelledby="roles-title" className="mx-auto w-full max-w-6xl px-4 py-14 sm:px-6 md:py-20">
          <h2 id="roles-title" className="font-display text-[26px] leading-tight text-ink sm:text-[32px]">{tr("rolesTitle")}</h2>
          <ul className="mt-8 grid gap-4 md:grid-cols-3">
            {ROLES.map(({ icon, key }) => (
              <li key={key} className="flex flex-col gap-3 rounded-xl border border-line bg-surface p-5">
                <span className="inline-flex items-center gap-2 text-[16px] font-semibold text-ink">
                  <Icon name={icon} size={20} className="text-primary-ink" />
                  {tr(`role.${key}.title`)}
                </span>
                <p className="text-[14px] leading-6">{tr(`role.${key}.body`)}</p>
              </li>
            ))}
          </ul>
        </section>

        <section className="px-4 pb-16 sm:px-6">
          <div className="mx-auto flex w-full max-w-6xl flex-col items-start gap-4 rounded-2xl bg-primary px-6 py-10 text-on-primary sm:flex-row sm:items-center sm:justify-between sm:px-10">
            <div className="flex flex-col gap-2">
              <h2 className="font-display text-[24px] leading-tight">{tr("ctaTitle")}</h2>
              <p className="text-[15px] opacity-90">{tr("ctaBody")}</p>
            </div>
            <Link
              href="/login"
              className="inline-flex h-11 items-center rounded-lg bg-surface px-5 text-[14px] font-semibold text-ink transition-opacity hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-on-primary"
            >
              {tr("signIn")}
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-line">
        <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-6 text-[13px] text-muted sm:px-6">
          <span>© 2026 {SITE_BRAND}</span>
          <Link href="/privacy" className="underline-offset-4 hover:text-ink hover:underline">{tr("privacy")}</Link>
        </div>
      </footer>
    </div>
  );
}
