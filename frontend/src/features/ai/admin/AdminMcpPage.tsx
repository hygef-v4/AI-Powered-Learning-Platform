"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, PageHeader } from "@/shared/ui";
import { createMcpCredential, getMcpCredentials, revokeMcpCredential } from "../api";
import { McpAgentSection } from "../McpAgentSection";
import { McpSetupGuide } from "../SetupGuides";
import type { McpCredential } from "../types";

export function AdminMcpPage() {
  const [credentials, setCredentials] = useState<McpCredential[] | null>(null);
  const [error, setError] = useState(false);
  const reload = useCallback(async () => {
    try { setCredentials(await getMcpCredentials()); setError(false); }
    catch { setError(true); }
  }, []);
  useEffect(() => {
    let active = true;
    getMcpCredentials().then(
      items => { if (active) setCredentials(items); },
      () => { if (active) setError(true); },
    );
    return () => { active = false; };
  }, []);
  return <div className="flex flex-col gap-4">
    <PageHeader title="Agent MCP" subtitle="Kết nối agent để đọc báo cáo AI và tổng quan hệ thống theo quyền quản trị." />
    {error && <Alert tone="danger" role="alert" action={<Button onClick={() => void reload()}>Tải lại</Button>}>
      Không tải được danh sách agent. Thử lại sau.
    </Alert>}
    {credentials === null && !error && <p>Đang tải...</p>}
    {credentials !== null && <McpAgentSection credentials={credentials}
      onCreate={async (label, access, days) => {
        const created = await createMcpCredential(label, access, days);
        await reload();
        return created;
      }}
      onRevoke={async id => { await revokeMcpCredential(id); await reload(); }} />}
    <McpSetupGuide />
  </div>;
}
