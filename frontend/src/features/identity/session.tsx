"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { ApiError, setUnauthenticatedHandler } from "@/shared/api/client";
import { getProfile } from "./api";
import type { Profile } from "./types";

/** Trang không cần đăng nhập; hết phiên ở đây thì không chuyển hướng. */
export const PUBLIC_PATHS = ["/login", "/activate", "/reset-password"];

export function isPublicPath(pathname: string) {
  return PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
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
      if (!isPublicPath(window.location.pathname)) router.replace("/login");
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
