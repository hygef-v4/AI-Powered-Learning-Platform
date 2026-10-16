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

export const GROUP_LABELS: Record<string, string> = {
  FILES: "Tệp",
  BACKUP: "Backup",
  ACTIVITY: "Hoạt động làm bài",
};
