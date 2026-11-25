import type { CSSProperties, ReactNode } from "react";
import Link from "next/link";
import type { Locale } from "@/i18n/locales";
import { LogoMark, INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";
import { SITE_BRAND_FULL, SITE_HOST } from "@/shared/siteMetadata";
import { cx } from "@/shared/ui/cx";
import styles from "../landing.module.css";
import { AssignVisual, DiagramVisual, DraftVisual, GradeVisual, GroupVisual } from "../components/FeatureVisuals";
import { GlossyClassBlock, GlossyDrop, GlossyPen, GlossyRing, GlossyStar, GlossyTriangle } from "../components/GlossyShapes";
import { FeatureRows, FloatingNav, LanguageSwitch, PillPile, ThemeSwitch, type Pill } from "../components/interactive";
import { Doodle, InkReveal, PointerScene } from "../components/motion";
import { SignedInRedirect } from "../components/SignedInRedirect";
import { UmlActor } from "../components/UmlActor";

type Translate = (key: string) => string;

const K = "landing.pages.LandingPage.";

const FOCUS_RING = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus";
const SECTIONS = ["about", "features", "benefits", "faq"] as const;
const ROLES = ["students", "lecturers", "subjectManagers"] as const;
// Mỗi tính năng có một vật thể bóng riêng cạnh phần chữ và một màu mực cho nét gạch chân vẽ tay.
const FEATURES = [
  { key: "assign", Visual: AssignVisual, ink: INK_ORANGE, object: <GlossyClassBlock uid="feature-assign" tone="lilac" /> },
  { key: "diagrams", Visual: DiagramVisual, ink: INK_TEAL, object: <GlossyTriangle uid="feature-diagrams" tone="blue" /> },
  { key: "aiGrading", Visual: GradeVisual, ink: INK_ORANGE, object: <GlossyStar uid="feature-grading" tone="orange" /> },
  { key: "aiDrafting", Visual: DraftVisual, ink: INK_TEAL, object: <GlossyDrop uid="feature-drafting" tone="teal" /> },
  { key: "groupWork", Visual: GroupVisual, ink: INK_ORANGE, object: <GlossyRing uid="feature-group" tone="teal" /> },
];
const PILLS: { key: string; tone: Pill["tone"] }[] = [
  { key: "aiGrading", tone: "orange" },
  { key: "aiDrafting", tone: "paper" },
  { key: "diagrams", tone: "teal" },
  { key: "assign", tone: "lilac" },
  { key: "groupWork", tone: "blue" },
  { key: "quiz", tone: "orange" },
  { key: "materials", tone: "teal" },
  { key: "feedback", tone: "paper" },
  { key: "fast", tone: "lilac" },
  { key: "smart", tone: "blue" },
  { key: "easy", tone: "orange" },
  { key: "together", tone: "paper" },
  { key: "flexible", tone: "teal" },
  { key: "fair", tone: "lilac" },
  { key: "visual", tone: "orange" },
  { key: "anywhere", tone: "blue" },
  { key: "automated", tone: "paper" },
  { key: "secure", tone: "teal" },
];
const FAQS = ["what", "grading", "drafting", "diagrams", "groups", "account"] as const;

/** Câu hỏi thường gặp dùng chung cho phần hỏi đáp trên trang và dữ liệu cấu trúc FAQPage. */
export function landingFaqs(t: Translate) {
  return FAQS.map((key) => ({ question: t(`${K}faq.${key}.q`), answer: t(`${K}faq.${key}.a`) }));
}

/**
 * Trang chủ công khai (SEO-FR1), dựng sẵn phía máy chủ để máy tìm kiếm đọc được đủ chữ. Người đã đăng nhập được
 * {@link SignedInRedirect} đưa thẳng vào dashboard. `t` là hàm dịch theo ngôn ngữ của lượt tải trang; mọi chữ của các
 * thành phần chạy trên trình duyệt đều đi qua props để không lệch ngôn ngữ giữa máy chủ và trình duyệt.
 *
 * Nhịp trang xen kẽ tối và sáng: bảng vẽ tối có vật thể bóng (hero), câu tuyên bố sáng (giới thiệu), các tính năng sáng,
 * dải lợi ích tối có nhãn rơi, hỏi đáp sáng, rồi khép lại bằng bảng vẽ tối. Phần tối luôn dùng bảng màu tối qua
 * `data-theme="dark"` cục bộ, phần sáng theo theme của người xem.
 */
export function LandingPage({
  t,
  locale,
  structuredData,
  nonce,
}: {
  t: Translate;
  locale: Locale;
  structuredData: string;
  nonce?: string;
}) {
  const tr = (key: string) => t(K + key);
  const navItems = SECTIONS.map((id) => ({ id, label: tr(`nav.${id}`) }));
  return (
    <div className="flex min-h-dvh flex-col bg-shell text-body">
      <SignedInRedirect />
      <script type="application/ld+json" nonce={nonce} dangerouslySetInnerHTML={{ __html: structuredData }} />
      <FloatingNav items={navItems} label={tr("nav.label")} />

      <Hero tr={tr} locale={locale} />

      <main className="flex-1">
        <About tr={tr} />
        <Features tr={tr} />
        <Benefits tr={tr} />
        <Faq tr={tr} />
      </main>

      <Closing tr={tr} navItems={navItems} />
    </div>
  );
}

function Wordmark({ className }: { className?: string }) {
  return (
    <span className={cx("inline-flex items-center gap-2.5", className)}>
      <LogoMark size={34} />
      <span className="text-[21px] leading-none font-extrabold tracking-[-0.03em]">
        ALAP<span style={{ color: INK_TEAL }}>Edu</span>
      </span>
    </span>
  );
}

/** Đặt một vật thể bóng: vị trí, kích thước, độ sâu khi nghiêng theo con trỏ và nhịp trôi riêng. */
function Float({
  className,
  depth,
  duration,
  delay = 0,
  tilt = 6,
  enter = 0,
  children,
}: {
  className: string;
  depth: number;
  duration: number;
  delay?: number;
  tilt?: number;
  enter?: number;
  children: ReactNode;
}) {
  const style = { "--depth": depth, "--dur": `${duration}s`, "--delay": `${delay}s`, "--tilt": `${tilt}deg`, "--enter": `${enter}s` } as CSSProperties;
  return (
    <div className={cx(styles.object, className)} style={style}>
      <div className={styles.enter}>
        <div className={styles.float}>{children}</div>
      </div>
    </div>
  );
}

function Hero({ tr, locale }: { tr: Translate; locale: Locale }) {
  return (
    <div data-theme="dark" className="drafting-board relative overflow-hidden text-body">
      <header className="absolute inset-x-0 top-0 z-30">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-5 py-5 sm:px-8">
          <Link href="/" aria-label={tr("home")} className={cx("rounded-lg text-ink", FOCUS_RING)}>
            <Wordmark />
          </Link>
          <div className="flex items-center gap-2 sm:gap-3">
            <ThemeSwitch toLight={tr("theme.toLight")} toDark={tr("theme.toDark")} />
            <LanguageSwitch
              locale={locale}
              label={tr("language")}
              options={[
                { code: "vi", short: "VI", name: "Tiếng Việt" },
                { code: "en", short: "EN", name: "English" },
              ]}
            />
            <Link
              href="/login"
              className={cx("inline-flex h-10 items-center rounded-full bg-[#eef2f7] px-4 text-[14px] font-bold whitespace-nowrap sm:px-5 text-[#132031] transition-colors hover:bg-white", FOCUS_RING)}
            >
              {tr("signIn")}
            </Link>
          </div>
        </div>
      </header>

      <PointerScene className="relative flex min-h-[100svh] items-center justify-center px-5 pt-28 pb-24 sm:px-8">
        <Float className="top-[11%] left-[-16%] w-[42vw] max-w-[360px] sm:top-[14%] sm:left-[3%] sm:w-[28vw]" depth={1.2} duration={10} tilt={-5}>
          <GlossyRing uid="hero-ring" tone="teal" />
        </Float>
        <Float className="bottom-[9%] left-[5%] hidden w-[8vw] max-w-[110px] min-w-[64px] sm:block" depth={2.2} duration={8} delay={-2} tilt={14} enter={0.15}>
          <GlossyPen uid="hero-pen" />
        </Float>
        <Float className="top-[10%] right-[6%] hidden w-[18vw] max-w-[250px] sm:block" depth={1.5} duration={11} delay={-4} tilt={8} enter={0.1}>
          <GlossyTriangle uid="hero-triangle" tone="blue" />
        </Float>
        <Float className="right-[-12%] bottom-[2%] w-[30vw] max-w-[230px] sm:right-[7%] sm:bottom-[6%] sm:w-[15vw]" depth={1.9} duration={9} delay={-1} tilt={-9} enter={0.2}>
          <GlossyClassBlock uid="hero-block" tone="lilac" />
        </Float>
        <Float className="bottom-[12%] left-[20%] hidden w-[8vw] max-w-[120px] lg:block" depth={2.6} duration={7} delay={-3} tilt={18} enter={0.3}>
          <GlossyStar uid="hero-star" tone="orange" />
        </Float>
        <Float className="top-[15%] left-[36%] hidden w-[3.6vw] max-w-[52px] md:block" depth={3} duration={6} delay={-2} enter={0.35}>
          <GlossyDrop uid="hero-drop-a" tone="orange" />
        </Float>
        <Float className="top-[44%] right-[22%] hidden w-[2.8vw] max-w-[42px] md:block" depth={3.2} duration={7} delay={-5} enter={0.4}>
          <GlossyDrop uid="hero-drop-b" tone="teal" />
        </Float>

        <div className="relative z-10 flex max-w-5xl flex-col items-center gap-7 text-center">
          <h1 className="flex flex-col items-center gap-6">
            <span className="animate-rise rounded-full border border-white/15 bg-[#132031]/70 px-4 py-1.5 text-[14px] font-semibold text-[#c5cfdd] backdrop-blur-sm">
              {SITE_BRAND_FULL}: {tr("hero.brandLine")}
            </span>
            <span className="text-[clamp(44px,7.4vw,108px)] leading-[0.98] font-extrabold tracking-[-0.045em] text-balance text-ink">
              <span className="animate-rise block" style={{ animationDelay: "0.08s" }}>{tr("hero.line1")}</span>{" "}
              <span className="animate-rise relative inline-block" style={{ animationDelay: "0.18s" }}>
                {tr("hero.line2")}
                <Doodle kind="circle" color={INK_ORANGE} width={4} delay={0.9} className="pointer-events-none absolute inset-x-[-7%] inset-y-[-22%] h-[144%] w-[114%]" />
              </span>
            </span>
          </h1>
          <p className="animate-rise max-w-[40rem] text-[17px] leading-8 text-[#c5cfdd] sm:text-[19px]" style={{ animationDelay: "0.3s" }}>
            {tr("hero.intro")}
          </p>
          <div className="animate-rise flex flex-wrap items-center justify-center gap-3" style={{ animationDelay: "0.42s" }}>
            <Link
              href="/login"
              className={cx("group inline-flex h-13 items-center gap-2 rounded-full bg-[#e8833a] px-7 text-[16px] font-bold text-[#1d1005] shadow-[0_14px_30px_rgb(232_131_58/0.3)] transition-[background-color,translate] hover:-translate-y-0.5 hover:bg-[#f0944f]", FOCUS_RING)}
            >
              {tr("hero.cta")}
              <ArrowIcon className="transition-transform group-hover:translate-x-0.5" />
            </Link>
            <a href="#features" className={cx("inline-flex h-13 items-center rounded-full border border-white/20 px-6 text-[16px] font-semibold text-ink transition-colors hover:bg-white/10", FOCUS_RING)}>
              {tr("hero.seeFeatures")}
            </a>
          </div>
        </div>

        <div aria-hidden="true" className={cx(styles.chip, "absolute top-[47%] left-[4%] z-10 hidden items-center gap-3 rounded-2xl border border-white/10 bg-[#202b39]/85 px-4 py-3 shadow-[0_18px_40px_rgb(0_0_0/0.35)] backdrop-blur-md xl:flex")} style={{ "--enter": "1.1s", "--delay": "-2s" } as CSSProperties}>
          <span className="inline-flex size-9 items-center justify-center rounded-xl text-[13px] font-extrabold text-[#1d1005]" style={{ background: INK_ORANGE }}>AI</span>
          <span className="flex flex-col text-left">
            <span className="text-[12px] text-[#a2afc0]">{tr("hero.chipAi")}</span>
            <span className="text-[15px] font-bold text-ink">{tr("hero.chipScore")}</span>
          </span>
        </div>
        <div aria-hidden="true" className={cx(styles.chip, "absolute top-[52%] right-[3%] z-10 hidden items-center gap-3 rounded-2xl border border-white/10 bg-[#202b39]/85 px-4 py-3 shadow-[0_18px_40px_rgb(0_0_0/0.35)] backdrop-blur-md xl:flex")} style={{ "--enter": "1.35s", "--delay": "-4s" } as CSSProperties}>
          <span className="flex -space-x-2">
            {[INK_TEAL, INK_ORANGE, "#6f93c2"].map((color) => (
              <i key={color} className="size-7 rounded-full border-2 border-[#202b39]" style={{ background: color }} />
            ))}
          </span>
          <span className="text-[13px] font-semibold text-ink">{tr("hero.chipGroup")}</span>
        </div>
      </PointerScene>
    </div>
  );
}

function About({ tr }: { tr: Translate }) {
  return (
    <section id="about" aria-labelledby="about-title" className="relative scroll-mt-4 overflow-hidden">
      <Doodle kind="sun" color={INK_TEAL} width={4} className="absolute top-14 right-[5%] size-24 md:top-20 md:size-32" />
      <Doodle kind="swirl" color={INK_ORANGE} width={4} delay={0.3} className="absolute bottom-12 left-[3%] hidden h-14 w-36 md:block" />
      <div className="relative mx-auto w-full max-w-6xl px-5 pt-28 pb-24 sm:px-8 md:pt-36 md:pb-32">
        <h2 id="about-title" className="sr-only">{tr("about.title")}</h2>
        <InkReveal
          text={tr("about.statement")}
          className="max-w-5xl text-[clamp(28px,4.2vw,56px)] leading-[1.14] font-bold tracking-[-0.03em] text-balance"
        />
        <ul className="mt-20 grid gap-10 border-t border-line pt-10 md:grid-cols-3 md:gap-12">
          {ROLES.map((key) => (
            <li key={key} className="flex gap-5">
              <UmlActor className="h-14 w-9 shrink-0 text-primary-ink" />
              <div className="flex flex-col gap-1.5">
                <h3 className="text-[18px] font-bold text-ink">{tr(`role.${key}.title`)}</h3>
                <p className="text-[15px] leading-7">{tr(`role.${key}.body`)}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function Features({ tr }: { tr: Translate }) {
  return (
    <section id="features" aria-labelledby="features-title" className="scroll-mt-4 bg-sunken">
      <div className="mx-auto w-full max-w-7xl px-5 py-24 sm:px-8 md:py-32">
        <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-end">
          <InkReveal
            as="h2"
            id="features-title"
            text={tr("features.title")}
            off="var(--c-faint)"
            className="max-w-3xl text-[clamp(32px,4.6vw,60px)] leading-[1.05] font-extrabold tracking-[-0.04em] text-balance"
          />
          <Link href="/login" className={cx("group inline-flex shrink-0 items-center gap-2 rounded-full border border-line-strong px-5 py-3 text-[15px] font-bold text-ink transition-colors hover:bg-surface", FOCUS_RING)}>
            {tr("features.cta")}
            <ArrowIcon className="transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>

        <FeatureRows className="mt-16 flex flex-col gap-6 md:mt-20 md:gap-4">
          {FEATURES.map(({ key, Visual, ink, object }, index) => (
            <article key={key} className="grid items-center gap-8 py-6 lg:min-h-[78vh] lg:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)] lg:gap-16">
              <div className={index % 2 ? "lg:order-2" : undefined}>
                <Visual tr={tr} />
              </div>
              <div className="relative flex max-w-md flex-col gap-5 lg:pt-16">
                <div aria-hidden="true" className={cx(styles.reveal, styles.featureObject, "absolute -top-6 right-0 w-20 sm:w-24 lg:top-0 lg:right-[-4%] lg:w-28")} style={{ "--d": "0.05s" } as CSSProperties}>
                  <div className={styles.float} style={{ "--dur": `${7 + index}s`, "--tilt": `${index % 2 ? -10 : 10}deg` } as CSSProperties}>{object}</div>
                </div>
                <h3 className={cx(styles.reveal, "relative max-w-[85%] text-[clamp(28px,3vw,40px)] leading-[1.08] font-extrabold tracking-[-0.035em] text-ink")} style={{ "--d": "0.1s" } as CSSProperties}>
                  <span className="relative inline-block">
                    {tr(`feature.${key}.title`)}
                    <Doodle kind="underline" color={ink} width={5} delay={0.3} className="absolute inset-x-0 -bottom-3 h-4 w-full" />
                  </span>
                </h3>
                <p className={cx(styles.reveal, "text-[18px] leading-8")} style={{ "--d": "0.2s" } as CSSProperties}>{tr(`feature.${key}.body`)}</p>
                <ul className="flex flex-wrap gap-2 pt-1">
                  {tr(`feature.${key}.tags`).split("|").map((tag, tagIndex) => (
                    <li
                      key={tag}
                      className={cx(styles.reveal, "inline-flex items-center gap-2 rounded-full border border-line-strong bg-surface px-3.5 py-1.5 text-[13px] font-semibold text-ink shadow-[var(--shadow-lift)]")}
                      style={{ "--d": `${0.3 + tagIndex * 0.08}s` } as CSSProperties}
                    >
                      <i aria-hidden="true" className="size-1.5 rounded-full" style={{ background: tagIndex === 1 ? INK_TEAL : ink }} />
                      {tag}
                    </li>
                  ))}
                </ul>
              </div>
            </article>
          ))}
        </FeatureRows>
      </div>
    </section>
  );
}

function Benefits({ tr }: { tr: Translate }) {
  return (
    <section id="benefits" aria-labelledby="benefits-title" data-theme="dark" className="drafting-board scroll-mt-4 overflow-hidden text-body">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center px-5 pt-24 pb-16 text-center sm:px-8 md:pt-32">
        <h2 id="benefits-title" className="max-w-4xl text-[clamp(32px,4.8vw,62px)] leading-[1.06] font-extrabold tracking-[-0.04em] text-balance text-ink">
          {tr("benefits.titleStart")}{" "}
          <span className="relative inline-block">
            {tr("benefits.titleMark")}
            <Doodle kind="underline" color={INK_ORANGE} width={5} className="absolute inset-x-0 -bottom-3 h-4 w-full" />
          </span>
        </h2>
        <p className="mt-6 max-w-2xl text-[17px] leading-8 text-[#c5cfdd]">{tr("benefits.body")}</p>
        <div className="mt-12 w-full max-w-5xl md:mt-16">
          <PillPile label={tr("benefits.listLabel")} pills={PILLS.map(({ key, tone }) => ({ label: tr(`benefits.pill.${key}`), tone }))} />
        </div>
      </div>
    </section>
  );
}

function Faq({ tr }: { tr: Translate }) {
  return (
    <section id="faq" aria-labelledby="faq-title" className="scroll-mt-4 border-t border-line bg-sunken">
      <div className="mx-auto grid w-full max-w-6xl gap-12 px-5 py-24 sm:px-8 md:py-32 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)] lg:gap-20">
        <div className="lg:sticky lg:top-16 lg:self-start">
          <h2 id="faq-title" className="text-[clamp(30px,3.6vw,46px)] leading-[1.08] font-extrabold tracking-[-0.035em] text-balance text-ink">
            {tr("faqTitle")}
          </h2>
          <p className="mt-5 max-w-sm text-[16px] leading-8">{tr("faqBody")}</p>
        </div>
        <div className="flex flex-col">
          {FAQS.map((key, index) => (
            <details key={key} name="landing-faq" open={index === 0} className="group border-b border-line-strong">
              <summary className={cx("flex cursor-pointer list-none items-center justify-between gap-6 py-6 text-left text-[18px] font-bold text-ink [&::-webkit-details-marker]:hidden", FOCUS_RING)}>
                <h3>{tr(`faq.${key}.q`)}</h3>
                <span aria-hidden="true" className="inline-flex size-9 shrink-0 items-center justify-center rounded-full border border-line-strong text-muted transition-transform duration-300 group-open:rotate-45">
                  <svg viewBox="0 0 16 16" className="size-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M8 2v12M2 8h12" /></svg>
                </span>
              </summary>
              <p className="max-w-2xl pb-7 text-[16px] leading-8">{tr(`faq.${key}.a`)}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

function Closing({ tr, navItems }: { tr: Translate; navItems: { id: string; label: string }[] }) {
  return (
    <footer data-theme="dark" className="drafting-board overflow-hidden text-body">
      <PointerScene className="relative mx-auto w-full max-w-7xl px-5 pt-28 pb-12 sm:px-8 md:pt-36">
        <Float className="top-[8%] right-[8%] hidden w-[20vw] max-w-[260px] sm:block" depth={1.6} duration={9} tilt={-10}>
          <GlossyStar uid="cta-star" tone="lilac" />
        </Float>
        <Float className="top-[36%] right-[24%] hidden w-[14vw] max-w-[190px] lg:block" depth={2.2} duration={10} delay={-3} tilt={8}>
          <GlossyRing uid="cta-ring" tone="teal" />
        </Float>
        <div className="relative z-10 flex max-w-3xl flex-col items-start gap-8">
          <h2 className="text-[clamp(36px,5.6vw,76px)] leading-[1.04] font-extrabold tracking-[-0.045em] text-balance text-ink">
            {tr("closing.start")}{" "}
            <span className="relative inline-block">
              {tr("closing.mark")}
              <Doodle kind="underline" color={INK_TEAL} width={5} className="absolute inset-x-0 -bottom-2 h-4 w-full" />
            </span>{" "}
            <span className="relative inline-block">
              {tr("closing.circle")}
              <Doodle kind="circle" color={INK_ORANGE} width={4} delay={0.4} className="pointer-events-none absolute inset-x-[-10%] inset-y-[-24%] h-[148%] w-[120%]" />
            </span>
          </h2>
          <p className="max-w-xl text-[17px] leading-8 text-[#c5cfdd]">{tr("closing.body")}</p>
          <Link href="/login" className={cx("group inline-flex h-13 items-center gap-2 rounded-full bg-[#eef2f7] px-7 text-[16px] font-bold text-[#132031] transition-[background-color,translate] hover:-translate-y-0.5 hover:bg-white", FOCUS_RING)}>
            {tr("closing.cta")}
            <ArrowIcon className="transition-transform group-hover:translate-x-0.5" />
          </Link>
          <p className="max-w-md text-[13px] leading-6 text-[#a2afc0]">{tr("accountNote")}</p>
        </div>

        <div className="relative z-10 mt-28 flex flex-col gap-8 border-t border-white/10 pt-8 pb-16 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-col gap-2">
            <Wordmark className="text-ink" />
            <span className="text-[13px] text-[#a2afc0]">{tr("footer.tagline")}</span>
          </div>
          <nav aria-label={tr("footer.label")}>
            <ul className="flex flex-wrap gap-x-6 gap-y-2 text-[14px] font-semibold">
              {navItems.map(({ id, label }) => (
                <li key={id}><a href={`#${id}`} className={cx("rounded text-[#c5cfdd] hover:text-white", FOCUS_RING)}>{label}</a></li>
              ))}
              <li><Link href="/privacy" className={cx("rounded text-[#c5cfdd] hover:text-white", FOCUS_RING)}>{tr("privacy")}</Link></li>
            </ul>
          </nav>
          <span className="text-[13px] text-[#a2afc0]">© 2026 {SITE_BRAND_FULL} · {SITE_HOST}</span>
        </div>
      </PointerScene>
    </footer>
  );
}

function ArrowIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" className={cx("size-5", className)} fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 10h12M11 5l5 5-5 5" />
    </svg>
  );
}
