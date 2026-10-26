"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { PageHeader } from "@/shared/ui/PageHeader";
import { getSettings } from "./api";
import { DelegatedSectionCard } from "./DelegatedSectionCard";
import { SettingGroupTable } from "./SettingGroupTable";
import type { SettingsOverview } from "./types";

/** Setting List (UC 14). */
export function SettingListPage() {
  const [overview, setOverview] = useState<SettingsOverview | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getSettings()
      .then(setOverview)
      .catch(() => setError("Không tải được cài đặt, thử lại sau."));
  }, []);

  return (
    <>
      <PageHeader title="Cài đặt" />
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!overview && !error && <p className="text-[13px] text-muted">Đang tải…</p>}
      {overview && overview.sections.length > 0 && (
        <div className="grid items-start gap-3 md:grid-cols-2">
          {overview.sections.map((section) => (
            <DelegatedSectionCard key={section.sectionKey} section={section} />
          ))}
        </div>
      )}
      {overview?.groups.map((group) => <SettingGroupTable key={group.group} group={group} />)}
    </>
  );
}
