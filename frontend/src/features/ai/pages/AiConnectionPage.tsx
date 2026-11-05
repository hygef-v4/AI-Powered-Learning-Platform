"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { useSession } from "@/features/identity/hooks/session";
import { McpAgentsTab } from "../components/McpAgentsTab";
import {
  addConnection,
  getConnections,
  revealConnection,
  revokeConnection,
  saveConnection,
  setConnectionEnabled,
  testConnection,
  testConnectionDraft,
} from "../api";
import { AiUsageHistoryPage } from "./AiUsageHistoryPage";
import { KeysSection, type KeysApi } from "../components/KeysSection";
import { ApiSetupGuide } from "../components/SetupGuides";

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
    { key: "usage", label: "Lịch sử sử dụng" },
    { key: "settings", label: "API cá nhân" },
    ...(mcpEnabled ? [{ key: "mcp" as const, label: "Agent MCP" }] : []),
  ];
  const [chosen, setChosen] = useUrlState<Tab>("tab", initialTab === "settings" || initialTab === "mcp" ? initialTab : "usage", ["usage", "settings", "mcp"]);
  const tab = tabs.some((t) => t.key === chosen) ? chosen : "usage";

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Kết nối AI" />
      <Tabs items={tabs} value={tab} onChange={setChosen} label="Kết nối AI" />
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
  reveal: revealConnection,
  test: testConnection,
  testDraft: testConnectionDraft,
  setEnabled: setConnectionEnabled,
  revoke: revokeConnection,
};

/**
 * Tab API cá nhân: bảng khóa cá nhân giống hệt bảng khóa của trường (nhiều khóa cho một nhà cung cấp, công tắc, kiểm tra,
 * thay, thu hồi) và hướng dẫn lấy khóa. Khi gọi AI hệ thống tự thử khóa của trường rồi khóa cá nhân (yêu cầu 2026-11-05).
 */
function PersonalKeysTab() {
  return (
    <SettingsCard>
      <KeysSection title="Khóa API cá nhân" api={PERSONAL} revokeTitle="Thu hồi khóa API?"
        loadError="Không tải được kết nối AI, thử lại sau." />
      <ApiSetupGuide />
    </SettingsCard>
  );
}
