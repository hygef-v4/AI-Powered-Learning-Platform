import Link from "next/link";
import { ButtonLink } from "@/shared/ui/Button";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";
import { SITE_BRAND } from "@/shared/siteMetadata";
import { ReviewSheet } from "../components/ReviewSheet";
import { SignedInRedirect } from "../components/SignedInRedirect";
import { UmlActor } from "../components/UmlActor";

type Translate = (key: string) => string;

const K = "landing.pages.LandingPage.";

const PRIMARY_FEATURES: { icon: IconName; key: string }[] = [
  { icon: "edit", key: "diagrams" },
  { icon: "sparkle", key: "aiGrading" },
];

const MORE_FEATURES: { icon: IconName; key: string }[] = [
  { icon: "groups", key: "groupWork" },
  { icon: "quiz", key: "quizzes" },
  { icon: "book", key: "materials" },
  { icon: "extension", key: "mcp" },
];

const ROLES = ["students", "lecturers", "subjectManagers"] as const;

const STEPS = ["publish", "submit", "review"] as const;

const FOCUS_RING = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus";

/**
 * Trang chủ công khai (SEO-FR1): dựng sẵn phía máy chủ để máy tìm kiếm đọc được. Người đã đăng nhập được
 * {@link SignedInRedirect} đưa thẳng vào dashboard. `t` là hàm dịch theo ngôn ngữ của lượt tải trang.
 *
 * Phần đầu và dải kêu gọi cuối trang là "bảng vẽ" (`.drafting-board`) luôn dùng bảng màu tối qua `data-theme="dark"`
 * cục bộ, để sơ đồ và phiếu rubric trông như nhau ở cả hai theme; phần còn lại theo theme của người xem.
 */
export function LandingPage({ t, structuredData, nonce }: { t: Translate; structuredData: string; nonce?: string }) {
  const tr = (key: string) => t(K + key);
  return (
    <div className="flex min-h-dvh flex-col bg-shell text-body">
      <SignedInRedirect />
      <script type="application/ld+json" nonce={nonce} dangerouslySetInnerHTML={{ __html: structuredData }} />

      <div data-theme="dark" className="drafting-board overflow-hidden text-body">
        <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-5 sm:px-6">
          <Link href="/" aria-label={tr("home")} className={`rounded-lg ${FOCUS_RING}`}>
            <Logo />
          </Link>
          <ButtonLink href="/login" variant="secondary">{tr("signIn")}</ButtonLink>
        </header>

        <section className="mx-auto grid w-full max-w-6xl items-center gap-12 px-4 pt-8 pb-20 sm:px-6 lg:grid-cols-2 lg:gap-12 lg:pt-12 lg:pb-24">
          <div className="animate-rise flex flex-col gap-6">
            <p className="text-[14px] font-semibold text-primary-ink">{tr("eyebrow")}</p>
            <h1 className="text-[36px] leading-[1.08] font-extrabold tracking-[-0.03em] text-balance text-ink sm:text-[46px] lg:text-[50px]">
              {tr("headline")}
            </h1>
            <p className="max-w-[34rem] text-[16px] leading-7 sm:text-[17px]">{tr("intro")}</p>
            <div className="flex flex-wrap items-center gap-3 pt-1">
              <ButtonLink href="/login" variant="primary" className="h-12 px-6 text-[15px]">{tr("signIn")}</ButtonLink>
              <a
                href="#features"
                className={`inline-flex h-12 items-center rounded-lg px-4 text-[15px] font-semibold text-ink underline-offset-4 hover:underline ${FOCUS_RING}`}
              >
                {tr("seeFeatures")}
              </a>
            </div>
            <p className="max-w-[30rem] text-[13px] leading-5 text-muted">{tr("accountNote")}</p>
          </div>
          <ReviewSheet tr={tr} />
        </section>
      </div>

      <main className="flex-1">
        <section
          id="features"
          aria-labelledby="features-title"
          className="mx-auto grid w-full max-w-6xl scroll-mt-6 gap-10 px-4 py-16 sm:px-6 md:py-24 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)] lg:gap-16"
        >
          <div className="lg:sticky lg:top-10 lg:self-start">
            <h2 id="features-title" className="text-[28px] leading-tight font-extrabold tracking-[-0.02em] text-balance text-ink sm:text-[34px]">
              {tr("featuresTitle")}
            </h2>
            <p className="mt-4 max-w-md text-[15px] leading-7">{tr("featuresIntro")}</p>
          </div>

          <div className="flex flex-col gap-10">
            <ul className="grid gap-4 md:grid-cols-2">
              {PRIMARY_FEATURES.map(({ icon, key }) => (
                <li key={key} className="flex flex-col gap-4 rounded-2xl border border-line bg-surface p-6 shadow-[var(--shadow-lift)]">
                  <span className="inline-flex size-12 items-center justify-center rounded-xl bg-primary text-on-primary">
                    <Icon name={icon} size={24} />
                  </span>
                  <h3 className="text-[19px] font-bold tracking-[-0.01em] text-ink">{tr(`feature.${key}.title`)}</h3>
                  <p className="text-[15px] leading-7">{tr(`feature.${key}.body`)}</p>
                </li>
              ))}
            </ul>
            <ul className="grid gap-x-10 sm:grid-cols-2">
              {MORE_FEATURES.map(({ icon, key }) => (
                <li key={key} className="flex gap-4 border-t border-line py-6">
                  <Icon name={icon} size={22} className="mt-0.5 text-primary-ink" />
                  <div className="flex flex-col gap-1.5">
                    <h3 className="text-[16px] font-semibold text-ink">{tr(`feature.${key}.title`)}</h3>
                    <p className="text-[14px] leading-6">{tr(`feature.${key}.body`)}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section aria-labelledby="steps-title" className="border-y border-line bg-sunken">
          <div className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6 md:py-24">
            <h2 id="steps-title" className="text-[28px] leading-tight font-extrabold tracking-[-0.02em] text-ink sm:text-[34px]">
              {tr("stepsTitle")}
            </h2>
            <ol className="mt-12 grid gap-10 md:grid-cols-3 md:gap-0">
              {STEPS.map((key, index) => (
                <li key={key} className="flex gap-5 md:flex-col md:gap-5 md:pr-8">
                  <div className="flex shrink-0 flex-col items-center md:flex-row">
                    <span className="inline-flex size-11 items-center justify-center rounded-full border-2 border-primary bg-surface text-[16px] font-extrabold text-primary-ink">
                      {index + 1}
                    </span>
                    {index < STEPS.length - 1 && <StepArrow />}
                  </div>
                  <div className="flex flex-col gap-2">
                    <h3 className="text-[17px] font-bold text-ink">{tr(`step.${key}.title`)}</h3>
                    <p className="max-w-xs text-[15px] leading-7">{tr(`step.${key}.body`)}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section aria-labelledby="roles-title" className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6 md:py-24">
          <h2 id="roles-title" className="text-[28px] leading-tight font-extrabold tracking-[-0.02em] text-ink sm:text-[34px]">
            {tr("rolesTitle")}
          </h2>
          <ul className="mt-10 grid gap-8 md:grid-cols-3 md:gap-10">
            {ROLES.map((key) => (
              <li key={key} className="flex gap-5">
                <UmlActor className="h-14 w-9 shrink-0 text-primary-ink" />
                <div className="flex flex-col gap-1.5">
                  <h3 className="text-[17px] font-bold text-ink">{tr(`role.${key}.title`)}</h3>
                  <p className="text-[15px] leading-7">{tr(`role.${key}.body`)}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>

        <section className="px-4 pb-20 sm:px-6">
          <div
            data-theme="dark"
            className="drafting-board mx-auto flex w-full max-w-6xl flex-col items-start gap-6 rounded-2xl px-6 py-12 text-body sm:flex-row sm:items-center sm:justify-between sm:px-12"
          >
            <div className="flex flex-col gap-2">
              <h2 className="text-[26px] leading-tight font-extrabold tracking-[-0.02em] text-ink sm:text-[30px]">{tr("ctaTitle")}</h2>
              <p className="text-[15px]">{tr("ctaBody")}</p>
            </div>
            <ButtonLink href="/login" variant="primary" className="h-12 shrink-0 px-6 text-[15px]">{tr("signIn")}</ButtonLink>
          </div>
        </section>
      </main>

      <footer className="border-t border-line">
        <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-6 text-[13px] text-muted sm:px-6">
          <span>© 2026 {SITE_BRAND}</span>
          <Link href="/privacy" className={`rounded underline-offset-4 hover:text-ink hover:underline ${FOCUS_RING}`}>{tr("privacy")}</Link>
        </div>
      </footer>
    </div>
  );
}

/** Đường nối giữa hai bước, đầu mũi tên hở như liên kết có hướng trong UML: dọc trên màn hẹp, ngang từ `md`. */
function StepArrow() {
  return (
    <span aria-hidden="true" className="flex flex-1 flex-col items-center text-line-strong md:flex-row">
      <span className="min-h-8 w-0.5 flex-1 bg-current md:h-0.5 md:min-h-0 md:w-auto" />
      <svg viewBox="0 0 12 12" className="size-3 rotate-90 md:mr-2 md:rotate-0" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 1 L10 6 L2 11" />
      </svg>
    </span>
  );
}
