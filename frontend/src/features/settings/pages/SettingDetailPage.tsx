"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Section } from "@/shared/ui/Section";
import { getSetting } from "../api";
import { SettingValueForm } from "../components/SettingValueForm";
import { groupMeta, readSettingDescription, readSettingName, TYPE_LABELS, type SettingItem } from "../model/types";
import { describeConstraints, formatDateTime, formatSettingValue, isEmptyValue } from "../model/validation";
import { msg } from "@/i18n/msg";

const BACK = "/admin/settings";

/** Setting Detail cho mục khóa–giá trị (UC 14, 15). */
export function SettingDetailPage({ settingKey }: { settingKey: string }) {
  const [item, setItem] = useState<SettingItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    getSetting(settingKey)
      .then((loaded) => {
        setItem(loaded);
        setError(null);
      })
      .catch(() => setError(msg("settings.pages.SettingDetailPage.couldNotLoadTheSettings")));
  }, [settingKey]);

  useEffect(load, [load]);

  if (!item) {
    return (
      <>
        <PageHeader backHref={BACK} title={msg("settings.pages.SettingDetailPage.settings")} />
        {error ? (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        ) : (
          <p className="text-[13px] text-muted">{msg("settings.pages.SettingDetailPage.loading")}</p>
        )}
      </>
    );
  }

  const group = groupMeta(item.group);
  return (
    <>
      <PageHeader
        backHref={BACK}
        title={readSettingName(item)}
        breadcrumb={[{ label: msg("settings.pages.SettingDetailPage.settings"), href: BACK }, { label: group.label }, { label: readSettingName(item) }]}
      />
      <Section title={msg("settings.pages.SettingDetailPage.value")} description={msg("settings.pages.SettingDetailPage.type", { value: String(TYPE_LABELS[item.type]) })} className="max-w-[680px]">
        {readSettingDescription(item) && <p className="text-[13px] leading-normal text-body">{readSettingDescription(item)}</p>}
        <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-2.5 text-[13px]">
          <dt className="text-muted">{msg("settings.pages.SettingDetailPage.currentValue")}</dt>
          <dd className="min-w-0 text-ink">
            {isEmptyValue(item.value) ? (
              <span className="text-muted">{msg("settings.pages.SettingDetailPage.notConfigured")}</span>
            ) : (
              <b>{formatSettingValue(item)}</b>
            )}
          </dd>
          <dt className="text-muted">{msg("settings.pages.SettingDetailPage.validRange")}</dt>
          <dd className="min-w-0 text-ink">{describeConstraints(item)}</dd>
          <dt className="text-muted">{msg("settings.pages.SettingDetailPage.lastEdited")}</dt>
          <dd className="min-w-0 text-ink">
            {item.updatedAt
              ? `${item.updatedBy ?? msg("settings.pages.SettingDetailPage.unknown")} · ${formatDateTime(item.updatedAt)}`
              : msg("settings.pages.SettingDetailPage.notEditedUsingTheInitial")}
          </dd>
        </dl>
        <div className="h-px bg-line-soft" />
        <SettingValueForm key={item.version} item={item} onSaved={setItem} onReload={load} />
      </Section>
    </>
  );
}
