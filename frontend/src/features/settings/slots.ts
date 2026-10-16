// U03 khai báo: phần cài đặt do unit khác lưu (U06: API AI chung, quota giảng viên) trong Setting Detail.

import { createSlot } from "@/shared/slots/registry";

export const settingsSectionRegistry = createSlot<{ sectionKey: string }>("SettingsSection");
