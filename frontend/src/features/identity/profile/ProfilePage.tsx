"use client";

import { useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { PageHeader } from "@/shared/ui/PageHeader";
import { useSession } from "../session";
import { PASSWORD_CHANGED_MESSAGE, PasswordChangeDialog } from "./PasswordChangeDialog";
import { ProfileForm } from "./ProfileForm";

/** Màn User Profile (UC 06, 07; design `c05`), mở popup Password Change (UC 05, `c06`). */
export function ProfilePage() {
  const session = useSession();
  const [changing, setChanging] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  if (session.status !== "authenticated") return null;

  return (
    <div className="mx-auto flex w-full max-w-[760px] flex-col gap-5">
      <PageHeader title="Hồ sơ" />
      {notice && (
        <Alert tone="success" role="status">
          {notice}
        </Alert>
      )}
      <ProfileForm
        key={session.profile.accountId}
        profile={session.profile}
        onSaved={() => void session.reload()}
        onChangePassword={() => {
          setNotice(null);
          setChanging(true);
        }}
      />
      {changing && (
        <PasswordChangeDialog
          onClose={() => setChanging(false)}
          onChanged={() => {
            setChanging(false);
            setNotice(PASSWORD_CHANGED_MESSAGE);
          }}
        />
      )}
    </div>
  );
}
