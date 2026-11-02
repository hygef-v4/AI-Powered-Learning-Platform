"use client";

import Link from "next/link";
import type { ComponentProps } from "react";

/**
 * Nút / link quay lại: luôn mở đúng trang cha đã đặt ở `href`, không quay theo lịch sử trình duyệt (yêu cầu 2026-11-02).
 * Trang cha tự khôi phục tab, bộ lọc nếu chúng nằm trong `href` (tham số URL). Trang có việc chưa lưu vẫn chặn được bằng
 * `onClick` gọi `preventDefault()`.
 */
export function BackLink(props: ComponentProps<typeof Link>) {
  return <Link {...props} />;
}
