import { apiFetch } from "@/shared/api/client";
import type { SettingItem, SettingValue, SettingsOverview } from "../model/types";

const BASE = "/api/v1/admin/settings";

export function getSettings(): Promise<SettingsOverview> {
  return apiFetch<SettingsOverview>(BASE);
}

export function getSetting(key: string): Promise<SettingItem> {
  return apiFetch<SettingItem>(`${BASE}/${encodeURIComponent(key)}`);
}

export function updateSetting(key: string, value: SettingValue, version: number): Promise<SettingItem> {
  return apiFetch<SettingItem>(`${BASE}/${encodeURIComponent(key)}`, {
    method: "PATCH",
    body: JSON.stringify({ value, version }),
  });
}
