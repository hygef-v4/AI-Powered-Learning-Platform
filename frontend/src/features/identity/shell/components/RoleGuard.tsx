"use client";

import { useRouter } from "next/navigation";
import { ErrorPage } from "@/app/ErrorPage";
import { FullPageStatus } from "@/shared/ui/FullPageStatus";
import { useEffect, type ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { loginPathFor, useSession } from "../../hooks/session";

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
    return <ErrorPage kind="unavailable" />;
  }
  if (!allowed) {
    return <FullPageStatus>Đang tải...</FullPageStatus>;
  }
  return <>{children}</>;
}
