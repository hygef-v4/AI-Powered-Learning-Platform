"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { Icon } from "@/shared/ui/Icon";
import { IconTile } from "@/shared/ui/IconTile";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Pill } from "@/shared/ui/Pill";
import { SettingsCard, SettingsGroup } from "@/shared/ui/SettingsCard";
import { getSettings } from "../api";
import { SettingRow } from "../components/SettingRow";
import { groupMeta, type SettingItem, type SettingsOverview } from "../model/types";
import { msg } from "@/i18n/msg";

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
      .catch(() => setError(msg("settings.pages.SettingListPage.couldNotLoadTheSettings")));
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
  // BR-U03-88: đặt giờ chạy trước rồi mới bật sao lưu (KAN-255).
  const hasDailyAt = typeof items.find((i) => i.key === BACKUP_DAILY_AT)?.value === "string";
  const ai = overview?.sections.find((s) => s.sectionKey === AI_SECTION);

  return (
    <>
      <PageHeader title={msg("settings.pages.SettingListPage.settings")} />
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!overview && !error && <p className="text-[13px] text-muted">{msg("settings.pages.SettingListPage.loading")}</p>}
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
                  <span className="min-w-0 flex-1 text-[15px] font-semibold text-ink">{msg("settings.pages.SettingListPage.aiSettings")}</span>
                  <Icon name="chevron_right" size={18} className="text-muted" />
                </Link>
              ) : (
                <div className="my-1 flex items-center gap-4 rounded-xl border border-line p-4 text-muted">
                  <IconTile icon="smart_toy" />
                  <span className="min-w-0 flex-1 text-[15px] font-semibold">{msg("settings.pages.SettingListPage.aiSettings")}</span>
                  <Pill tone="warning">{msg("settings.pages.SettingListPage.unavailable")}</Pill>
                </div>
              )}
            </SettingsGroup>
          )}
          {overview.groups.map((group) => (
            <SettingsGroup key={group.group} title={groupMeta(group.group).label}>
              {group.items.map((item) => (
                <SettingRow
                  // Mục có ô nhập dựng lại theo version để ô nhập nhận giá trị mới; công tắc giữ nguyên để nút gạt trượt
                  // trọn vẹn, không bị dựng lại giữa chừng khi lưu xong.
                  key={item.type === "BOOLEAN" ? item.key : `${item.key}-${item.version}`}
                  item={item}
                  disabled={item.key === BACKUP_ENABLED && !backupOn && !hasDailyAt}
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
