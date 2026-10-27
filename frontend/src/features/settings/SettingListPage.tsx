"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { Icon } from "@/shared/ui/Icon";
import { IconTile } from "@/shared/ui/IconTile";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Pill } from "@/shared/ui/Pill";
import { SettingsCard, SettingsGroup } from "@/shared/ui/SettingsCard";
import { getSettings } from "./api";
import { SettingRow } from "./SettingRow";
import { groupMeta, type SettingItem, type SettingsOverview } from "./types";

const BACKUP_ENABLED = "backup.enabled";
const BACKUP_DAILY_AT = "backup.dailyAt";
const AI_SECTION = "ai-connections";

/** Setting List (UC 14, 15): một thẻ rộng như các trang khác, mỗi mục sửa ngay trên một dòng; nhóm AI đứng đầu. */
export function SettingListPage() {
  const [overview, setOverview] = useState<SettingsOverview | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    getSettings()
      .then((loaded) => {
        setOverview(loaded);
        setError(null);
      })
      .catch(() => setError("Không tải được cài đặt, thử lại sau."));
  }, []);

  useEffect(load, [load]);

  const replace = (saved: SettingItem) =>
    setOverview((current) =>
      current && {
        ...current,
        groups: current.groups.map((g) => ({ ...g, items: g.items.map((i) => (i.key === saved.key ? saved : i)) })),
      },
    );

  const items = overview?.groups.flatMap((g) => g.items) ?? [];
  const backupOn = items.find((i) => i.key === BACKUP_ENABLED)?.value === true;
  const ai = overview?.sections.find((s) => s.sectionKey === AI_SECTION);

  return (
    <>
      <PageHeader title="Cài đặt" />
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!overview && !error && <p className="text-[13px] text-muted">Đang tải…</p>}
      {overview && (
        <SettingsCard>
          {ai && (
            <SettingsGroup title="AI">
              {ai.available ? (
                <Link
                  href={`/admin/settings/sections/${AI_SECTION}`}
                  className="my-1 flex items-center gap-4 rounded-xl border border-line bg-canvas p-4 transition-colors hover:border-line-strong hover:bg-sunken"
                >
                  <IconTile icon="smart_toy" />
                  <span className="min-w-0 flex-1 text-[15px] font-semibold text-ink">Kết nối AI</span>
                  <Icon name="chevron_right" size={18} className="text-muted" />
                </Link>
              ) : (
                <div className="my-1 flex items-center gap-4 rounded-xl border border-line p-4 text-muted">
                  <IconTile icon="smart_toy" />
                  <span className="min-w-0 flex-1 text-[15px] font-semibold">Kết nối AI</span>
                  <Pill tone="warning">Không khả dụng</Pill>
                </div>
              )}
            </SettingsGroup>
          )}
          {overview.groups.map((group) => (
            <SettingsGroup key={group.group} title={groupMeta(group.group).label}>
              {group.items.map((item) => (
                <SettingRow
                  key={`${item.key}-${item.version}`}
                  item={item}
                  disabled={item.key === BACKUP_DAILY_AT && !backupOn}
                  onSaved={replace}
                  onConflict={load}
                />
              ))}
            </SettingsGroup>
          ))}
        </SettingsCard>
      )}
    </>
  );
}
