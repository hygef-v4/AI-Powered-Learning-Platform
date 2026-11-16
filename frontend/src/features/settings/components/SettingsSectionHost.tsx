"use client";

import { Alert } from "@/shared/ui/Alert";
import { PageHeader } from "@/shared/ui/PageHeader";
import { settingsSectionRegistry } from "../slots";
import { msg } from "@/i18n/msg";

/**
 * Hiện panel do unit sở hữu đăng ký cho {@code sectionKey}; panel tự vẽ đầu trang (PageHeader có nút quay lại).
 * {@code tab} (tham số ?tab=) chọn tab mở trước nếu panel có tab. Không có panel thì báo không khả dụng.
 */
export function SettingsSectionHost({ sectionKey, tab }: { sectionKey: string; tab?: string }) {
  const entry = settingsSectionRegistry.entries().find((e) => e.key === sectionKey);
  if (!entry) {
    return (
      <>
        <PageHeader backHref="/admin/settings" title={msg("settings.components.SettingsSectionHost.settings")} />
        <Alert tone="warning" className="max-w-[680px]">
          {msg("settings.components.SettingsSectionHost.thisSettingsSectionIsCurrently")}</Alert>
      </>
    );
  }
  const Panel = entry.component;
  return <Panel sectionKey={sectionKey} tab={tab} />;
}
