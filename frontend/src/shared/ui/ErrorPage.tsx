"use client";

import Link from "next/link";
import { useTheme } from "@/shared/theme";
import { ErrorMark, type ErrorKind } from "@/shared/ui/ErrorMark";
import { Icon } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";
import styles from "../styles/error-page.module.css";
import { msg } from "@/i18n/msg";

/** Chữ của từng trang lỗi: chỉ nói lỗi gì, không hướng dẫn thêm; `code` chỉ có ở lỗi mang mã HTTP. */
const COPY: Record<ErrorKind, { code?: string; title: string; description: string }> = {
  "not-found": {
    code: "404",
    title: msg("shared.ui.ErrorPage.pageNotFound"),
    description: msg("shared.ui.ErrorPage.theLinkDoesNotExist"),
  },
  unavailable: {
    code: "503",
    title: msg("shared.ui.ErrorPage.theSystemIsBusy"),
    description: msg("shared.ui.ErrorPage.theServerHasNotResponded"),
  },
  offline: {
    title: msg("shared.ui.ErrorPage.networkConnectionLost"),
    description: msg("shared.ui.ErrorPage.yourDeviceIsOffline"),
  },
  generic: {
    title: msg("shared.ui.ErrorPage.somethingWentWrong"),
    description: msg("shared.ui.ErrorPage.theSystemCouldNotComplete"),
  },
};

/**
 * Trang lỗi cả màn: logo nền tảng và nút đổi giao diện sáng/tối ở hai góc trên, giữa trang là hình riêng của lỗi (bút và nghiên), mã lỗi nhỏ, tiêu đề, một dòng
 * giải thích và nút. Không tìm thấy trang chỉ có Về trang chủ (nút trắng); các lỗi khác có thêm Tải lại trang (`retry`,
 * mặc định tải lại cả trang) làm nút chính.
 */
export function ErrorPage({ kind, retry }: { kind: ErrorKind; retry?: () => void }) {
  const copy = COPY[kind];
  const reloadable = kind !== "not-found";
  const [theme, toggleTheme] = useTheme();
  return (
    <main className={styles.page}>
      <div className={styles.top}>
        <Link className={styles.brand} href="/" aria-label={msg("shared.ui.ErrorPage.learningPlatformHome")}>
          <Logo />
        </Link>
        <button
          className={styles.themeToggle}
          type="button"
          onClick={toggleTheme}
          aria-label={theme === "dark" ? msg("shared.ui.ErrorPage.switchToLightMode") : msg("shared.ui.ErrorPage.switchToDarkMode")}
        >
          <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
        </button>
      </div>
      <section className={styles.content} aria-labelledby="error-title">
        <ErrorMark kind={kind} />
        {copy.code && <span className={styles.code}>{msg("shared.ui.ErrorPage.error")}{copy.code}</span>}
        <h1 id="error-title">{copy.title}</h1>
        <p>{copy.description}</p>
        <div className={styles.actions}>
          {reloadable && (
            <button className={styles.primary} type="button" onClick={retry ?? (() => window.location.reload())}>
              {msg("shared.ui.ErrorPage.reloadPage")}</button>
          )}
          <Link className={styles.secondary} href="/">
            {msg("shared.ui.ErrorPage.goToHomePage")}</Link>
        </div>
      </section>
      <footer className={styles.footer}>{msg("shared.ui.ErrorPage.n2026LearningPlatform")}</footer>
    </main>
  );
}
