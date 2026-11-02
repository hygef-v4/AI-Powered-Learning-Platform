"use client";

import { useState } from "react";
import { PageHeader } from "@/shared/ui/PageHeader";
import { showToast } from "@/shared/ui/Toast";
import { useSession } from "../../hooks/session";
import { PASSWORD_CHANGED_MESSAGE, PasswordChangeDialog } from "../components/PasswordChangeDialog";
import { ProfileForm } from "../components/ProfileForm";

/** Màn User Profile (UC 06, 07; design `c05`), mở popup Password Change (UC 05, `c06`). */
export function ProfilePage() {
  const session = useSession();
  const [changing, setChanging] = useState(false);
  if (session.status !== "authenticated") return null;

  return (
    <div className="mx-auto flex w-full max-w-[760px] flex-col gap-5">
      <PageHeader title="Hồ sơ" />
      <ProfileForm
        key={session.profile.accountId}
        profile={session.profile}
        onSaved={() => void session.reload()}
        onChangePassword={() => setChanging(true)}
      />
      {changing && (
        <PasswordChangeDialog
          onClose={() => setChanging(false)}
          onChanged={() => {
            setChanging(false);
            showToast(PASSWORD_CHANGED_MESSAGE);
          }}
        />
      )}
    </div>
  );
}
