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
    label: msg("settings.model.types.files"),
    description: msg("settings.model.types.limitsWhenUploadingLearningMaterials"),
    icon: "folder",
    tone: "primary",
  },
  BACKUP: {
    label: msg("settings.model.types.backup"),
    description: msg("settings.model.types.backUpTheDatabaseTo"),
    icon: "backup",
    tone: "success",
  },
  ACTIVITY: {
    label: msg("settings.model.types.workActivity"),
    description: msg("settings.model.types.pageLeaveAndTabHide"),
    icon: "monitoring",
    tone: "warning",
  },
};

export function groupMeta(group: string): GroupMeta {
  return GROUP_META[group] ?? { label: group, description: "", icon: "tune", tone: "neutral" };
}

/** Tên và mô tả theo ngôn ngữ giao diện; backend chỉ trả một bản tiếng Việt. Khóa lạ thì dùng chữ backend trả. */
const SETTING_TEXT: Record<string, { name: string; description: string }> = {
  "files.material.maxSizeMb": {
    name: msg("settings.model.types.materialMaxSize"),
    description: msg("settings.model.types.materialMaxSizeDescription"),
  },
  "files.documentImage.maxSizeMb": {
    name: msg("settings.model.types.imageMaxSize"),
    description: msg("settings.model.types.imageMaxSizeDescription"),
  },
  "backup.enabled": {
    name: msg("settings.model.types.backupEnabled"),
    description: msg("settings.model.types.backupEnabledDescription"),
  },
  "backup.dailyAt": {
    name: msg("settings.model.types.backupDailyAt"),
    description: msg("settings.model.types.backupDailyAtDescription"),
  },
};

export function readSettingName(item: Pick<SettingItem, "key" | "name">): string {
  return SETTING_TEXT[item.key]?.name ?? item.name;
}

export function readSettingDescription(item: Pick<SettingItem, "key" | "description">): string {
  return SETTING_TEXT[item.key]?.description ?? item.description;
}

export const TYPE_LABELS: Record<SettingType, string> = {
  INTEGER: msg("settings.model.types.integer"),
  BOOLEAN: msg("settings.model.types.onOff"),
  TIME_OF_DAY: msg("settings.model.types.hourMinute"),
  ENUM: msg("settings.model.types.singleChoice"),
  ENUM_SET: msg("settings.model.types.multipleChoice"),
};
