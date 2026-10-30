"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, SettingsCard } from "@/shared/ui";
import { createMcpCredential, getMcpCredentials, revokeMcpCredential, updateMcpCredential } from "../api";
import { McpAgentSection } from "./McpAgentSection";
import { McpSetupGuide } from "./SetupGuides";
import type { McpCredential } from "../model/types";

/** Own MCP credentials for Student/Teacher/Subject Manager; mounted only by the authorized connection page. */
export function McpAgentsTab() {
  const [credentials, setCredentials] = useState<McpCredential[] | null>(null);
  const [error, setError] = useState(false);
  const reload = useCallback(async () => {
    try {
      setCredentials(await getMcpCredentials());
      setError(false);
    } catch {
      setError(true);
    }
  }, []);
  useEffect(() => {
    let active = true;
    getMcpCredentials().then(
      (items) => { if (active) setCredentials(items); },
      () => { if (active) setError(true); },
    );
    return () => { active = false; };
  }, []);
  return (
    <>
      {error && <Alert tone="danger" role="alert" action={<Button onClick={() => void reload()}>Tải lại</Button>}>
        Không tải được agent MCP, thử lại sau.
      </Alert>}
      {!credentials && !error && <p className="text-[13px] text-muted">Đang tải…</p>}
      {credentials && <SettingsCard>
        <McpAgentSection credentials={credentials}
          onCreate={async (label, access, days) => {
            const created = await createMcpCredential(label, access, days);
            await reload();
            return created;
          }}
          onUpdate={async (id, label, access) => { await updateMcpCredential(id, label, access); await reload(); }}
          onRevoke={async (id) => { await revokeMcpCredential(id); await reload(); }} />
        <McpSetupGuide />
      </SettingsCard>}
    </>
  );
}
