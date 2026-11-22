"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getProfile } from "@/features/identity/api";

/**
 * Landing vẫn hiện cho khách và máy tìm kiếm; ai đã đăng nhập thì chuyển ngay tới trang chủ theo role (U01 F15).
 * Cookie phiên chỉ gửi tới /api nên phải hỏi API từ trình duyệt. Không có phiên hoặc lỗi mạng thì giữ nguyên landing.
 */
export function SignedInRedirect() {
  const router = useRouter();
  useEffect(() => {
    let active = true;
    getProfile()
      .then((profile) => {
        if (active) router.replace(profile.homePath);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [router]);
  return null;
}
