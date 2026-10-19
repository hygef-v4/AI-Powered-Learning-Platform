"use client";

import { ErrorPage } from "./ErrorPage";
import styles from "./error-page.module.css";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="vi">
      <body className={styles.globalBody}>
        <ErrorPage
          code="500"
          title="Đã xảy ra lỗi"
          description="Hệ thống chưa thể hoàn tất yêu cầu. Bạn có thể thử lại hoặc quay về trang chủ."
          retry={reset}
        />
      </body>
    </html>
  );
}
