// U03 khai báo: phần cài đặt do unit khác lưu (U06: API AI chung, quota giảng viên) trong Setting Detail.

import { createRegistry, type SlotEntry } from "@/shared/slots/registry";
import type { IconName } from "@/shared/ui/Icon";
import type { Tone } from "@/shared/ui/tones";

/** Panel đăng ký theo {@code key} = sectionKey; icon, tone dùng cho thẻ trên Setting List. */
export type SettingsSectionEntry = SlotEntry<{ sectionKey: string }> & { icon?: IconName; tone?: Tone };

export const settingsSectionRegistry = createRegistry<SettingsSectionEntry>("SettingsSection");
