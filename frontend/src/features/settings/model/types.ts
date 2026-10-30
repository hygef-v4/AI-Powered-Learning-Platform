import type { IconName } from "@/shared/ui/Icon";
import type { Tone } from "@/shared/ui/tones";

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
    label: "Tệp",
    description: "Giới hạn khi tải học liệu và ảnh trong tài liệu",
    icon: "folder",
    tone: "primary",
  },
  BACKUP: {
    label: "Sao lưu",
    description: "Sao lưu cơ sở dữ liệu lên Google Drive · giữ 7 ngày (cố định)",
    icon: "backup",
    tone: "success",
  },
  ACTIVITY: {
    label: "Hoạt động làm bài",
    description: "Dữ liệu rời trang, ẩn tab khi bài bật Ghi nhận hoạt động",
    icon: "monitoring",
    tone: "warning",
  },
};

export function groupMeta(group: string): GroupMeta {
  return GROUP_META[group] ?? { label: group, description: "", icon: "tune", tone: "neutral" };
}

export const TYPE_LABELS: Record<SettingType, string> = {
  INTEGER: "số nguyên",
  BOOLEAN: "bật / tắt",
  TIME_OF_DAY: "giờ:phút",
  ENUM: "chọn một",
  ENUM_SET: "chọn nhiều",
};
