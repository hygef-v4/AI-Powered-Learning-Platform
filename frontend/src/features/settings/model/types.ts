import type { IconName } from "@/shared/ui/Icon";
import type { Tone } from "@/shared/ui/tones";
import { msg } from "@/i18n/msg";

export type SettingType = "INTEGER" | "BOOLEAN" | "TIME_OF_DAY" | "ENUM" | "ENUM_SET";

export type SettingValue = number | boolean | string | string[] | null;

export type SettingItem = {
  key: string;
  group: string;
  name: string;
  description: string;
  type: SettingType;
  value: SettingValue;
  defaultValue: SettingValue;
  constraints: Record<string, unknown> | null;
  version: number;
  updatedBy: string | null;
  updatedAt: string | null;
};

export type SettingGroupView = {
  group: string;
  items: SettingItem[];
};

export type SettingsSection = {
  sectionKey: string;
  title: string;
  summary: string | null;
  available: boolean;
};

export type SettingsOverview = {
  groups: SettingGroupView[];
  sections: SettingsSection[];
};

export type GroupMeta = { label: string; description: string; icon: IconName; tone: Tone };

export const GROUP_META: Record<string, GroupMeta> = {
  FILES: {
    label: msg("settings.model.types.tep"),
    description: msg("settings.model.types.gioiHanKhiTaiHocLieu"),
    icon: "folder",
    tone: "primary",
  },
  BACKUP: {
    label: msg("settings.model.types.saoLuu"),
    description: msg("settings.model.types.saoLuuCoSoDuLieu"),
    icon: "backup",
    tone: "success",
  },
  ACTIVITY: {
    label: msg("settings.model.types.hoatDongLamBai"),
    description: msg("settings.model.types.duLieuRoiTrangAnTab"),
    icon: "monitoring",
    tone: "warning",
  },
};

export function groupMeta(group: string): GroupMeta {
  return GROUP_META[group] ?? { label: group, description: "", icon: "tune", tone: "neutral" };
}

export const TYPE_LABELS: Record<SettingType, string> = {
  INTEGER: msg("settings.model.types.soNguyen"),
  BOOLEAN: msg("settings.model.types.batTat"),
  TIME_OF_DAY: msg("settings.model.types.gioPhut"),
  ENUM: msg("settings.model.types.chonMot"),
  ENUM_SET: msg("settings.model.types.chonNhieu"),
};
