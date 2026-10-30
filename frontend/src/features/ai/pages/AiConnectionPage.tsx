"use client";

import { useCallback, useEffect, useState } from "react";
import type { AiProvider } from "@/shared/ai";
import { Alert, Button, PageHeader, SettingsCard, Tabs, type TabItem } from "@/shared/ui";
import { useSession } from "@/features/identity/hooks/session";
import { McpAgentsTab } from "../components/McpAgentsTab";
import { getConnections, getProviders, revokeConnection, saveConnection, testConnection } from "../api";
import { AiUsageHistoryPage } from "./AiUsageHistoryPage";
import { PersonalApiSection } from "../components/PersonalApiSection";
import { ApiSetupGuide } from "../components/SetupGuides";
import type { ConnectionView, ProviderInfo, SaveConnection } from "../model/types";

type Tab = "usage" | "settings" | "mcp";
type Data = { connections: ConnectionView[]; providers: ProviderInfo[] };

const loadAll = async (): Promise<Data> => {
  const [connections, providers] = await Promise.all([getConnections(), getProviders()]);
  return { connections, providers };
};

/**
 * Màn #6 AI Connection (UC 31–33), cùng bố cục trang Cài đặt AI của Admin: tab Usage mở trước (lịch sử của chính mình),
 * tab API cá nhân là khóa API cá nhân, tab Agent MCP của Student/Teacher/Subject Manager là agent của chính mình.
 */
export function AiConnectionPage({ initialTab }: { initialTab?: string }) {
  const session = useSession();
  const mcpEnabled = session.status === "authenticated" &&
    (session.profile.role === "STUDENT" || session.profile.role === "TEACHER" || session.profile.role === "SUBJECT_MANAGER");
  const tabs: TabItem<Tab>[] = [
    { key: "usage", label: "Usage" },
    { key: "settings", label: "API cá nhân" },
    ...(mcpEnabled ? [{ key: "mcp" as const, label: "Agent MCP" }] : []),
  ];
  const [chosen, setChosen] = useState<Tab>(initialTab === "settings" || initialTab === "mcp" ? initialTab : "usage");
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

/** Tab API cá nhân: bảng khóa API cá nhân và hướng dẫn lấy khóa trong một thẻ. */
function PersonalKeysTab() {
  const [data, setData] = useState<Data | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    () =>
      loadAll().then(
        (loaded) => {
          setData(loaded);
          setError(null);
        },
        () => setError("Không tải được kết nối AI, thử lại sau."),
      ),
    [],
  );

  useEffect(() => {
    let active = true;
    loadAll().then(
      (loaded) => {
        if (active) setData(loaded);
      },
      () => {
        if (active) setError("Không tải được kết nối AI, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const save = async (provider: AiProvider, body: SaveConnection) => {
    await saveConnection(provider, body);
    await reload();
  };
  const test = async (provider: AiProvider) => {
    const result = await testConnection(provider);
    await reload();
    return result;
  };
  const revoke = async (provider: AiProvider) => {
    await revokeConnection(provider);
    await reload();
  };

  return (
    <>
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          {error}
        </Alert>
      )}
      {!data && !error && <p className="text-[13px] text-muted">Đang tải…</p>}
      {data && (
        <SettingsCard>
          <PersonalApiSection connections={data.connections} providers={data.providers} onSave={save} onTest={test} onRevoke={revoke} />
          <ApiSetupGuide />
        </SettingsCard>
      )}
    </>
  );
}
