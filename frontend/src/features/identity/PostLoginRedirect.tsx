"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { getProfile } from "./api";

/** Post-Login (F15): đã đăng nhập thì tới dashboard theo role, chưa thì về User Login. Dùng cho / và /post-login. */
export function PostLoginRedirect() {
  const router = useRouter();
  const [unavailable, setUnavailable] = useState(false);

  useEffect(() => {
    getProfile()
      .then((profile) => router.replace(profile.homePath))
      .catch((error: unknown) => {
        const status = error instanceof ApiError ? error.problem.status : 0;
        if (status === 401 || status === 403) router.replace("/login");
        else setUnavailable(true);
      });
  }, [router]);

  if (unavailable) {
    return (
      <p role="alert" className="p-8 text-center text-sm text-red-600">
        Hệ thống tạm bận, vui lòng tải lại trang sau.
      </p>
    );
  }
  return <p className="p-8 text-center text-sm text-slate-500">Đang chuyển trang...</p>;
}
