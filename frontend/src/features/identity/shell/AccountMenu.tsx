"use client";

import Link from "next/link";
import { useState } from "react";
import { accountMenuEntries } from "../slots";
import { ROLE_LABELS, type Profile } from "../types";
import { SignOutDialog } from "./SignOutDialog";

/**
 * Menu tài khoản: tên, role, Hồ sơ, mục do unit khác đăng ký (AccountMenuEntry, ví dụ U06 AI Connection),
 * lối sang Class Dashboard cho Subject Manager, Đăng xuất (F15).
 */
export function AccountMenu({ profile, onSignedOut }: { profile: Profile; onSignedOut: () => void }) {
  const [open, setOpen] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const entries = accountMenuEntries.entries().filter((entry) => entry.roles.includes(profile.role));
  const initials = profile.displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((word) => word[0]?.toUpperCase())
    .join("");

  return (
    <div className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 rounded-lg px-2 py-1 hover:bg-slate-100"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700">
          {initials}
        </span>
        <span className="text-left">
          <span className="block text-sm font-medium text-slate-900">{profile.displayName}</span>
          <span className="block text-xs text-slate-500">{ROLE_LABELS[profile.role]}</span>
        </span>
      </button>
      {open && (
        <div role="menu" className="absolute right-0 z-40 mt-2 w-56 rounded-lg border border-slate-200 bg-white py-1 shadow-lg">
          <MenuLink href="/profile" label="Hồ sơ" />
          {profile.role === "SUBJECT_MANAGER" && <MenuLink href="/classes" label="Lớp được giao dạy" />}
          {entries.map((entry) => (
            <MenuLink key={entry.key} href={entry.href} label={entry.title} />
          ))}
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              setConfirming(true);
            }}
            className="block w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-slate-50"
          >
            Đăng xuất
          </button>
        </div>
      )}
      {confirming && <SignOutDialog onCancel={() => setConfirming(false)} onSignedOut={onSignedOut} />}
    </div>
  );
}

function MenuLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} role="menuitem" className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50">
      {label}
    </Link>
  );
}
