"use client";

import { ErrorPage } from "./ErrorPage";

export default function Error({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <ErrorPage
      code="500"
      title="Đã xảy ra lỗi"
      description="Hệ thống chưa thể hoàn tất yêu cầu. Bạn có thể thử lại hoặc quay về trang chủ."
      retry={reset}
    />
  );
}
