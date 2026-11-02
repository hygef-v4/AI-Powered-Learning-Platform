"use client";

import { ErrorPage } from "./ErrorPage";

/** Lỗi khi vẽ trang: mất mạng thì báo mất kết nối, còn lại là trang lỗi chung. */
export default function Error({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const offline = typeof navigator !== "undefined" && navigator.onLine === false;
  return <ErrorPage kind={offline ? "offline" : "generic"} retry={reset} />;
}
