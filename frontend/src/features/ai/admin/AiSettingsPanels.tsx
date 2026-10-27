"use client";

import { useEffect, useState } from "react";
import { PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { AdminMcpTab } from "./AdminMcpPage";
import { InstitutionalKeysSection } from "./InstitutionalKeysSection";
import { QuotaPolicySection, TeacherQuotaTable } from "./QuotaSections";
import { SharedUsageTab } from "./SharedUsageTab";

type Tab = "usage" | "settings" | "mcp";

const TABS: TabItem<Tab>[] = [
  { key: "usage", label: "Usage" },
  { key: "settings", label: "Cài đặt" },
  { key: "mcp", label: "Agent MCP" },
];

/**
 * Trang "Kết nối AI" mở từ Setting List (UC 14, 15, 32, 33): tab Usage mở trước; tab Cài đặt gồm khóa của trường, hạn
 * mức chung và mức riêng trong một thẻ; tab Agent MCP là agent của chính Admin. Mở từ thẻ hạn mức thì vào tab Cài đặt và cuộn tới phần hạn mức.
 */
function AiSettingsPage({ focusQuota = false, initialTab = "usage" }: { focusQuota?: boolean; initialTab?: Tab }) {
  const [tab, setTab] = useState<Tab>(focusQuota ? "settings" : initialTab);
  useEffect(() => {
    if (focusQuota) document.getElementById("ai-quota")?.scrollIntoView?.({ block: "start" });
  }, [focusQuota]);

  return (
    <>
      <PageHeader
        title="Kết nối AI"
        backHref="/admin/settings"
        breadcrumb={[{ label: "Cài đặt", href: "/admin/settings" }, { label: "Kết nối AI" }]}
      />
      <Tabs items={TABS} value={tab} onChange={setTab} label="Kết nối AI" />
      {tab === "usage" && <SharedUsageTab />}
      {tab === "mcp" && <AdminMcpTab />}
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

/** Đăng ký cho mục "Kết nối AI" trên Setting List; {@code ?tab=mcp} mở thẳng tab Agent MCP (đường dẫn cũ /admin/mcp). */
export function AiConnectionsPanel({ tab }: { tab?: string }) {
  return <AiSettingsPage initialTab={TABS.some((t) => t.key === tab) ? (tab as Tab) : "usage"} />;
}

/** Đăng ký cho mục hạn mức AI giảng viên: cùng trang, tab Cài đặt, cuộn tới phần hạn mức. */
export function TeacherQuotaPanel() {
  return <AiSettingsPage focusQuota />;
}
