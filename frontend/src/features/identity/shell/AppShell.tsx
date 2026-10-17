"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { BrandLogo } from "../components/BrandLogo";
import { useSession } from "../session";
import { headerWidgets } from "../slots";
import { AccountMenu } from "./AccountMenu";

/** Khung sau đăng nhập: thanh trên có vùng HeaderWidget (U14 gắn chuông) và menu tài khoản. */
export function AppShell({ children }: { children: ReactNode }) {
  const session = useSession();
  const router = useRouter();
  if (session.status !== "authenticated") return <>{children}</>;

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-slate-200 bg-white px-6">
        <Link href={session.profile.homePath} aria-label="Trang chính">
          <BrandLogo />
        </Link>
        <div className="flex items-center gap-3">
          <SlotOutlet slot={headerWidgets} props={{}} />
          <AccountMenu
            profile={session.profile}
            onSignedOut={() => {
              session.clear();
              router.replace("/login");
            }}
          />
        </div>
      </header>
      {children}
    </div>
  );
}
