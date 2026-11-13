"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/shared/ui/Alert";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Section } from "@/shared/ui/Section";
import { getSetting } from "../api";
import { SettingValueForm } from "../components/SettingValueForm";
import { groupMeta, TYPE_LABELS, type SettingItem } from "../model/types";
import { describeConstraints, formatDateTime, formatSettingValue, isEmptyValue } from "../model/validation";

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
      .catch(() => setError("Không tải được cài đặt, thử lại sau."));
  }, [settingKey]);

  useEffect(load, [load]);

  if (!item) {
    return (
      <>
        <PageHeader backHref={BACK} title="Cài đặt" />
        {error ? (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        ) : (
          <p className="text-[13px] text-muted">Đang tải…</p>
        )}
      </>
    );
  }

  const group = groupMeta(item.group);
  return (
    <>
      <PageHeader
        backHref={BACK}
        title={item.name}
        breadcrumb={[{ label: "Cài đặt", href: BACK }, { label: group.label }, { label: item.name }]}
      />
      <Section title="Giá trị" description={`Kiểu: ${TYPE_LABELS[item.type]}`} className="max-w-[680px]">
        {item.description && <p className="text-[13px] leading-normal text-body">{item.description}</p>}
        <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-2.5 text-[13px]">
          <dt className="text-muted">Giá trị hiện hành</dt>
          <dd className="min-w-0 text-ink">
            {isEmptyValue(item.value) ? (
              <span className="text-muted">Chưa cấu hình</span>
            ) : (
              <b>{formatSettingValue(item)}</b>
            )}
          </dd>
          <dt className="text-muted">Giới hạn hợp lệ</dt>
          <dd className="min-w-0 text-ink">{describeConstraints(item)}</dd>
          <dt className="text-muted">Sửa gần nhất</dt>
          <dd className="min-w-0 text-ink">
            {item.updatedAt
              ? `${item.updatedBy ?? "Không rõ"} · ${formatDateTime(item.updatedAt)}`
              : "Chưa sửa, đang dùng giá trị khởi tạo"}
          </dd>
        </dl>
        <div className="h-px bg-line-soft" />
        <SettingValueForm key={item.version} item={item} onSaved={setItem} onReload={load} />
      </Section>
    </>
  );
}
