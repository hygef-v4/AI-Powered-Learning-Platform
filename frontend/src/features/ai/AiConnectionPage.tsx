"use client";

import { useCallback, useEffect, useState, type ReactNode } from "react";
import { formatTokens, type AiProvider, type SourceOption } from "@/shared/ai";
import { Alert, Button, ButtonLink, IconTile, PageHeader } from "@/shared/ui";
import {
  createMcpCredential,
  getConnections,
  getMcpCredentials,
  getProviders,
  getSources,
  revokeConnection,
  revokeMcpCredential,
  saveConnection,
  testConnection,
} from "./api";
import { MAX_AGENTS } from "./labels";
import { McpAgentSection } from "./McpAgentSection";
import { PersonalApiSection } from "./PersonalApiSection";
import { ApiSetupGuide, McpSetupGuide } from "./SetupGuides";
import type { ConnectionView, McpCredential, ProviderInfo, SaveConnection } from "./types";

type Data = {
  connections: ConnectionView[];
  providers: ProviderInfo[];
  credentials: McpCredential[];
  sources: SourceOption[];
};

async function loadAll(): Promise<Data> {
  const [connections, providers, credentials, sources] = await Promise.all([
    getConnections(),
    getProviders(),
    getMcpCredentials(),
    getSources().catch(() => [] as SourceOption[]),
  ]);
  return { connections, providers, credentials, sources };
}

/** Màn #6 AI Connection (UC 31, 32): API cá nhân và agent MCP của riêng người dùng. */
export function AiConnectionPage() {
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
    <div className="flex flex-col gap-4">
      <PageHeader
        title="Kết nối AI"
        subtitle="API cá nhân và agent MCP của riêng bạn. Khóa đã lưu luôn được che và chỉ dùng cho yêu cầu của bạn."
        actions={
          <ButtonLink href="/ai/usage" icon="history">
            Lịch sử sử dụng AI
          </ButtonLink>
        }
      />
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          {error}
        </Alert>
      )}
      {!data && !error && <p className="text-[13px] text-muted">Đang tải...</p>}
      {data && (
        <>
          <AiSummaryCards data={data} />
          <PersonalApiSection
            connections={data.connections}
            providers={data.providers}
            onSave={save}
            onTest={test}
            onRevoke={revoke}
          />
          <ApiSetupGuide />
          <McpAgentSection
            credentials={data.credentials}
            onCreate={async (label, access, days) => {
              const created = await createMcpCredential(label, access, days);
              await reload();
              return created;
            }}
            onRevoke={async (id) => {
              await revokeMcpCredential(id);
              await reload();
            }}
          />
          <McpSetupGuide />
        </>
      )}
    </div>
  );
}

/** Thẻ tóm tắt: hạn mức của trường hôm nay (chỉ khi được dùng), số khóa cá nhân, số agent còn hiệu lực. */
export function AiSummaryCards({ data }: { data: Data }) {
  const shared = data.sources.find((s) => s.source === "SHARED");
  const showShared = shared !== undefined && shared.reason !== "Chỉ giảng viên dùng API của trường";
  const keys = data.connections.filter((c) => c.configured).length;
  const agents = data.credentials.filter((c) => c.status === "ACTIVE").length;
  return (
    <div className="grid gap-3 md:grid-cols-3">
      {showShared && (
        <Card icon="speed" title="Hạn mức của trường hôm nay">
          {shared?.quota ? (
            <>
              <p className="text-xl font-bold text-ink">
                {formatTokens(shared.quota.used)} / {formatTokens(shared.quota.limit)}
              </p>
              <p className="text-xs text-muted">token đã dùng · đặt lại lúc 00:00</p>
            </>
          ) : (
            <p className="text-xs text-muted">{shared?.reason ?? "Chưa cấu hình"}</p>
          )}
        </Card>
      )}
      <Card icon="key" title="API cá nhân">
        <p className="text-xl font-bold text-ink">{keys} khóa</p>
        <p className="text-xs text-muted">Chi phí tính vào tài khoản nhà cung cấp của bạn</p>
      </Card>
      <Card icon="smart_toy" title="Agent MCP">
        <p className="text-xl font-bold text-ink">
          {agents} / {MAX_AGENTS}
        </p>
        <p className="text-xs text-muted">agent còn hiệu lực</p>
      </Card>
    </div>
  );
}

function Card({ icon, title, children }: { icon: "speed" | "key" | "smart_toy"; title: string; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-line bg-white p-4">
      <IconTile icon={icon} tone={icon === "smart_toy" ? "accent" : "primary"} />
      <div className="min-w-0">
        <p className="text-xs font-semibold text-muted">{title}</p>
        {children}
      </div>
    </div>
  );
}
