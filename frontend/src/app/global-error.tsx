"use client";

import { ErrorPage } from "./ErrorPage";
import styles from "./error-page.module.css";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="vi">
      <body className={styles.globalBody}>
        <ErrorPage kind={typeof navigator !== "undefined" && navigator.onLine === false ? "offline" : "generic"} retry={reset} />
      </body>
    </html>
  );
}
