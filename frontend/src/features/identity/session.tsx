"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { ApiError, setUnauthenticatedHandler } from "@/shared/api/client";
import { clearCurrentUser } from "@/shared/auth/useCurrentUser";
import { getProfile } from "./api";
import type { Profile } from "./types";

/** Trang không cần đăng nhập; hết phiên ở đây thì không chuyển hướng. */
export const PUBLIC_PATHS = ["/login", "/activate", "/reset-password"];

export function isPublicPath(pathname: string) {
  return PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
}

/** `/login?next=...` để đăng nhập lại xong quay về đúng trang đang mở. */
export function loginPathFor(location: { pathname: string; search: string }) {
  const current = location.pathname + location.search;
  return current === "/" || isPublicPath(location.pathname) ? "/login" : `/login?next=${encodeURIComponent(current)}`;
}

/**
 * Chỉ nhận đường dẫn nội bộ (bắt đầu bằng một `/`, không phải `//` hay `/\`) để `next` không bị dùng chuyển hướng ra
 * trang ngoài; trang công khai cũng bỏ qua. Không hợp lệ thì `null` (về homePath).
 */
export function safeNextPath(next: string | null): string | null {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) return null;
  const pathname = next.split(/[?#]/)[0] ?? "";
  return isPublicPath(pathname) ? null : next;
}

type SessionState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "unavailable" }
  | { status: "authenticated"; profile: Profile };

type SessionContextValue = SessionState & {
  reload: () => Promise<void>;
  clear: () => void;
};

const SessionContext = createContext<SessionContextValue | null>(null);

async function loadSession(): Promise<SessionState> {
  try {
    return { status: "authenticated", profile: await getProfile() };
  } catch (error) {
    const status = error instanceof ApiError ? error.problem.status : 0;
    return { status: status === 401 || status === 403 ? "anonymous" : "unavailable" };
  }
}

/** Tải hồ sơ một lần để biết role và homePath (F15 bước 4). */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SessionState>({ status: "loading" });

  const router = useRouter();

  const reload = useCallback(() => loadSession().then(setState), []);

  const clear = useCallback(() => setState({ status: "anonymous" }), []);

  useEffect(() => {
    setUnauthenticatedHandler(() => {
      clearCurrentUser();
      if (!isPublicPath(window.location.pathname)) router.replace(loginPathFor(window.location));
    });
    let active = true;
    loadSession().then((next) => {
      if (active) setState(next);
    });
    return () => {
      active = false;
      setUnauthenticatedHandler(null);
    };
  }, [router]);

  const value = useMemo(() => ({ ...state, reload, clear }), [state, reload, clear]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const value = useContext(SessionContext);
  if (!value) throw new Error("useSession phải nằm trong SessionProvider");
  return value;
}
