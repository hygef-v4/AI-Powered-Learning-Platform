"use client";

import { useEffect, useState } from "react";
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
      .catch(() => setError("Không tải được cài đặt"));
  }, []);

  return (
    <main className="mx-auto max-w-4xl p-6">
      <h1 className="text-2xl font-semibold">Cài đặt hệ thống</h1>
      {error && <p className="mt-4 text-red-700">{error}</p>}
      {overview?.groups.map((group) => <SettingGroupTable key={group.group} group={group} />)}
      {overview && overview.sections.length > 0 && (
        <section className="mt-6 grid gap-3 sm:grid-cols-2">
          {overview.sections.map((section) => (
            <DelegatedSectionCard key={section.sectionKey} section={section} />
          ))}
        </section>
      )}
    </main>
  );
}
