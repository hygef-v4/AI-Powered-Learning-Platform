"use client";

import { useState } from "react";
import { useSession } from "../session";
import { PASSWORD_CHANGED_MESSAGE, PasswordChangeDialog } from "./PasswordChangeDialog";
import { ProfileForm } from "./ProfileForm";

/** Màn User Profile (UC 06, 07), mở popup Password Change (UC 05). */
export function ProfilePage() {
  const session = useSession();
  const [changing, setChanging] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  if (session.status !== "authenticated") return null;

  return (
    <main className="mx-auto max-w-3xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Hồ sơ cá nhân</h1>
      {notice && (
        <p role="status" className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {notice}
        </p>
      )}
      <div className="mt-6">
        <ProfileForm
          key={session.profile.accountId}
          profile={session.profile}
          onSaved={() => void session.reload()}
          onChangePassword={() => {
            setNotice(null);
            setChanging(true);
          }}
        />
      </div>
      {changing && (
        <PasswordChangeDialog
          onClose={() => setChanging(false)}
          onChanged={() => {
            setChanging(false);
            setNotice(PASSWORD_CHANGED_MESSAGE);
          }}
        />
      )}
    </main>
  );
}
