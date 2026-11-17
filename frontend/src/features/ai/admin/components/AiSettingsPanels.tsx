"use client";

import { useEffect } from "react";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { InstitutionalKeysSection } from "./InstitutionalKeysSection";
import { QuotaPolicySection, TeacherQuotaTable } from "./QuotaSections";
import { SharedUsageTab } from "./SharedUsageTab";
import { msg } from "@/i18n/msg";

type Tab = "usage" | "settings";

const TABS: TabItem<Tab>[] = [
  { key: "usage", label: msg("ai.admin.components.AiSettingsPanels.usageHistory") },
  { key: "settings", label: msg("ai.admin.components.AiSettingsPanels.settings") },
];

/**
 * Cài đặt AI của Admin chỉ gồm usage, khóa chung và quota. Agent MCP thuộc Student/Teacher/Subject Manager.
 */
function AiSettingsPage({ focusQuota = false, initialTab = "usage" }: { focusQuota?: boolean; initialTab?: Tab }) {
  const [tab, setTab] = useUrlState<Tab>("tab", focusQuota ? "settings" : initialTab, ["usage", "settings"]);
  useEffect(() => {
    if (focusQuota) document.getElementById("ai-quota")?.scrollIntoView?.({ block: "start" });
  }, [focusQuota]);

  return (
    <>
      <PageHeader
        title={msg("ai.admin.components.AiSettingsPanels.aiSettings")}
        backHref="/admin/settings"
        breadcrumb={[{ label: msg("ai.admin.components.AiSettingsPanels.settings"), href: "/admin/settings" }, { label: msg("ai.admin.components.AiSettingsPanels.aiSettings") }]}
      />
      <Tabs items={TABS} value={tab} onChange={setTab} label={msg("ai.admin.components.AiSettingsPanels.aiSettings")} />
      {tab === "usage" && <SharedUsageTab />}
      {tab === "settings" && (
        <SettingsCard>
          <InstitutionalKeysSection />
          <QuotaPolicySection />
          <TeacherQuotaTable />
        </SettingsCard>
      )}
    </>
  );
}

/** Query tab cũ/không hợp lệ quay về Usage; không gắn component MCP trong khu vực Admin. */
export function AiConnectionsPanel({ tab }: { tab?: string }) {
  return <AiSettingsPage initialTab={TABS.some((t) => t.key === tab) ? (tab as Tab) : "usage"} />;
}

/** Đăng ký cho mục hạn mức AI giảng viên: cùng trang, tab Cài đặt, cuộn tới phần hạn mức. */
export function TeacherQuotaPanel() {
  return <AiSettingsPage focusQuota />;
}
