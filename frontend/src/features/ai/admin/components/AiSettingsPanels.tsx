"use client";

import { useEffect, useState } from "react";
import { PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { InstitutionalKeysSection } from "./InstitutionalKeysSection";
import { QuotaPolicySection, TeacherQuotaTable } from "./QuotaSections";
import { SharedUsageTab } from "./SharedUsageTab";

type Tab = "usage" | "settings";

const TABS: TabItem<Tab>[] = [
  { key: "usage", label: "Usage" },
  { key: "settings", label: "Cài đặt" },
];

/**
 * Cài đặt AI của Admin chỉ gồm usage, khóa chung và quota. Agent MCP thuộc Student/Teacher/Subject Manager.
 */
function AiSettingsPage({ focusQuota = false, initialTab = "usage" }: { focusQuota?: boolean; initialTab?: Tab }) {
  const [tab, setTab] = useState<Tab>(focusQuota ? "settings" : initialTab);
  useEffect(() => {
    if (focusQuota) document.getElementById("ai-quota")?.scrollIntoView?.({ block: "start" });
  }, [focusQuota]);

  return (
    <>
      <PageHeader
        title="Cài đặt AI"
        backHref="/admin/settings"
        breadcrumb={[{ label: "Cài đặt", href: "/admin/settings" }, { label: "Cài đặt AI" }]}
      />
      <Tabs items={TABS} value={tab} onChange={setTab} label="Cài đặt AI" />
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
