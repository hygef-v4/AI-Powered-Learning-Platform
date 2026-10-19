"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { loginPathFor, useSession } from "../session";

/**
 * Chặn route theo role để trải nghiệm tốt (BR-U01-48): chưa đăng nhập → /login, sai role → homePath của mình.
 * Chỉ là lớp giao diện; backend vẫn tự kiểm quyền mọi API.
 */
export function RoleGuard({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const session = useSession();
  const router = useRouter();
  const allowed = session.status === "authenticated" && (!roles || roles.includes(session.profile.role));

  useEffect(() => {
    if (session.status === "anonymous") router.replace(loginPathFor(window.location));
    else if (session.status === "authenticated" && !allowed) router.replace(session.profile.homePath);
  }, [session, allowed, router]);

  if (session.status === "unavailable") {
    return (
      <p role="alert" className="p-8 text-center text-sm text-red-600">
        Hệ thống tạm bận, vui lòng tải lại trang sau.
      </p>
    );
  }
  if (!allowed) {
    return <p className="p-8 text-center text-sm text-slate-500">Đang tải...</p>;
  }
  return <>{children}</>;
}
