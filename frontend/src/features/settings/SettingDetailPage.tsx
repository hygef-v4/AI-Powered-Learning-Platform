"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { getSetting } from "./api";
import { SettingValueForm } from "./SettingValueForm";
import type { SettingItem } from "./types";
import { formatSettingValue } from "./validation";

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
      .catch(() => setError("Không tải được cài đặt"));
  }, [settingKey]);

  useEffect(load, [load]);

  return (
    <main className="mx-auto max-w-2xl p-6">
      <Link className="text-sm text-blue-700 hover:underline" href="/admin/settings">
        ← Cài đặt hệ thống
      </Link>
      {error && <p className="mt-4 text-red-700">{error}</p>}
      {item && (
        <>
          <h1 className="mt-2 text-2xl font-semibold">{item.name}</h1>
          <p className="mt-1 text-slate-600">{item.description}</p>
          <p className="mt-3 text-sm">
            Giá trị hiện hành: <strong>{formatSettingValue(item)}</strong>
            {item.updatedBy && ` — sửa bởi ${item.updatedBy}`}
          </p>
          <SettingValueForm key={item.version} item={item} onSaved={setItem} onReload={load} />
        </>
      )}
    </main>
  );
}
