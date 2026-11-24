"use client";

import { useCallback, useState } from "react";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { useSession } from "@/features/identity/hooks/SessionProvider";
import { McpAgentsTab } from "../components/McpAgentsTab";
import {
  addConnection,
  getConnections,
  revokeConnection,
  saveConnection,
  setConnectionEnabled,
  testConnection,
  testConnectionDraft,
} from "../api";
import { AiUsageHistoryPage } from "./AiUsageHistoryPage";
import { KeysSection, type KeysApi } from "../components/KeysSection";
import { SourceSection } from "../components/SourceSection";
import { ApiSetupGuide } from "../components/SetupGuides";
import { msg } from "@/i18n/msg";

type Tab = "usage" | "settings" | "mcp";
/**
 * Màn #6 AI Connection (UC 31–33), cùng bố cục trang Cài đặt AI của Admin: tab Usage mở trước (lịch sử của chính mình),
 * tab API cá nhân là khóa API cá nhân, tab Agent MCP của Student/Teacher/Subject Manager là agent của chính mình.
 */
export function AiConnectionPage({ initialTab }: { initialTab?: string }) {
  const session = useSession();
  const mcpEnabled = session.status === "authenticated" &&
    (session.profile.role === "STUDENT" || session.profile.role === "TEACHER" || session.profile.role === "SUBJECT_MANAGER");
  const tabs: TabItem<Tab>[] = [
    { key: "usage", label: msg("ai.pages.AiConnectionPage.usageHistory") },
    { key: "settings", label: msg("ai.pages.AiConnectionPage.personalApi") },
    ...(mcpEnabled ? [{ key: "mcp" as const, label: "Agent MCP" }] : []),
  ];
  const [chosen, setChosen] = useUrlState<Tab>("tab", initialTab === "settings" || initialTab === "mcp" ? initialTab : "usage", ["usage", "settings", "mcp"]);
  const tab = tabs.some((t) => t.key === chosen) ? chosen : "usage";

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title={msg("ai.pages.AiConnectionPage.aiConnection")} />
      <Tabs items={tabs} value={tab} onChange={setChosen} label={msg("ai.pages.AiConnectionPage.aiConnection")} />
      {tab === "usage" && <AiUsageHistoryPage mcpEnabled={mcpEnabled} />}
      {tab === "settings" && <PersonalKeysTab />}
      {tab === "mcp" && <McpAgentsTab />}
    </div>
  );
}

const PERSONAL: KeysApi = {
  list: async () => (await getConnections()).filter((c) => c.configured && c.id),
  add: addConnection,
  save: saveConnection,
  test: testConnection,
  testDraft: testConnectionDraft,
  setEnabled: setConnectionEnabled,
  revoke: revokeConnection,
};

/**
 * Tab API cá nhân: thẻ Nguồn AI dùng mặc định (một lựa chọn cho mọi tác vụ AI, yêu cầu 2026-11-23), bảng khóa cá nhân
 * (nhiều khóa cho một nhà cung cấp, kiểm tra, thay, thu hồi; không có công tắc) và hướng dẫn lấy khóa.
 */
function PersonalKeysTab() {
  const [keysVersion, setKeysVersion] = useState(0);
  const handleKeysChanged = useCallback(() => setKeysVersion((v) => v + 1), []);
  return (
    <SettingsCard>
      <SourceSection refreshKey={keysVersion} />
      <KeysSection title={msg("ai.pages.AiConnectionPage.personalApiKeys")} api={PERSONAL} revokeTitle={msg("ai.pages.AiConnectionPage.revokeApiKey")}
        loadError={msg("ai.pages.AiConnectionPage.couldNotLoadAiConnections")} allowDisable={false}
        onChanged={handleKeysChanged} />
      <ApiSetupGuide />
    </SettingsCard>
  );
}
