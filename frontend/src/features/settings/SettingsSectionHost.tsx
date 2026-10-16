"use client";

import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { settingsSectionRegistry } from "./slots";

/** Hiện panel do unit sở hữu đăng ký cho {@code sectionKey}; không có thì báo không khả dụng. */
export function SettingsSectionHost({ sectionKey }: { sectionKey: string }) {
  const registered = settingsSectionRegistry.entries().some((entry) => entry.key === sectionKey);
  if (!registered) {
    return <p className="p-6 text-amber-700">Phần cài đặt này hiện không khả dụng.</p>;
  }
  return (
    <main className="mx-auto max-w-3xl p-6">
      <SlotOutlet slot={{ ...settingsSectionRegistry, entries: () => settingsSectionRegistry.entries().filter((e) => e.key === sectionKey) }} props={{ sectionKey }} />
    </main>
  );
}
