"use client";

import Link from "next/link";
import { Logo } from "@/shared/ui/Logo";
import styles from "./error-page.module.css";

export function ErrorPage({
  code,
  title,
  description,
  retry,
}: {
  code: string;
  title: string;
  description: string;
  retry?: () => void;
}) {
  return (
    <main className={styles.page}>
      <Link className={styles.brand} href="/" aria-label="Nền tảng học tập - Trang chủ">
        <Logo />
      </Link>
      <section className={styles.content} aria-labelledby="error-title">
        <span className={styles.code}>{code}</span>
        <h1 id="error-title">{title}</h1>
        <p>{description}</p>
        <div className={styles.actions}>
          {retry && <button className={styles.primary} type="button" onClick={retry}>Thử lại</button>}
          <Link className={retry ? styles.secondary : styles.primary} href="/">Về trang chủ</Link>
        </div>
      </section>
      <footer className={styles.footer}>© 2026 Nền tảng học tập</footer>
    </main>
  );
}
