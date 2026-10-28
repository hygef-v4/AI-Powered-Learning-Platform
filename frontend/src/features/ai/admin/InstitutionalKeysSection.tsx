"use client";

import { useCallback, useEffect, useState } from "react";
import { PROVIDER_LABEL, masked } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, IconButton, SettingsGroup, Switch, TableFrame, Td, Th, cx } from "@/shared/ui";
import { ApiKeyDialog } from "../ApiKeyDialog";
import { getProviders } from "../api";
import { errorMessage, shortTestResult } from "../labels";
import type { ConnectionView, ProviderInfo } from "../types";
import {
  addInstitutional,
  getInstitutional,
  revealInstitutional,
  revokeInstitutional,
  saveInstitutional,
  setInstitutionalEnabled,
  testInstitutional,
  testInstitutionalDraft,
} from "./api";

const CONFLICT = "Khóa vừa được thay đổi ở nơi khác, hãy tải lại.";
const OK = "Đạt";

/**
 * Khóa API của trường (UC 14, 15): bảng nhiều khóa, mỗi nhà cung cấp có thể có nhiều khóa. Kiểm tra, bật/tắt, thay,
 * thu hồi trên từng dòng; 409 báo tải lại.
 */
export function InstitutionalKeysSection() {
  const [connections, setConnections] = useState<ConnectionView[] | null>(null);
  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [editing, setEditing] = useState<ConnectionView | "new" | null>(null);
  const [revoking, setRevoking] = useState<ConnectionView | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    () =>
      Promise.all([getInstitutional(), getProviders()]).then(
        ([loaded, catalog]) => {
          setConnections(loaded.filter((c) => c.configured && c.id));
          setProviders(catalog);
        },
        () => setError("Không tải được khóa API của trường, thử lại sau."),
      ),
    [],
  );

  useEffect(() => {
    let active = true;
    Promise.all([getInstitutional(), getProviders()]).then(
      ([loaded, catalog]) => {
        if (!active) return;
        setConnections(loaded.filter((c) => c.configured && c.id));
        setProviders(catalog);
      },
      () => {
        if (active) setError("Không tải được khóa API của trường, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const run = async (id: string, action: () => Promise<unknown>, fallback: string) => {
    setBusy(id);
    setError(null);
    try {
      await action();
      await reload();
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(problem?.status === 409 ? CONFLICT : errorMessage(problem?.code, fallback));
    } finally {
      setBusy(null);
    }
  };

  const current = editing === "new" ? null : editing;

  return (
    <SettingsGroup
      title="Khóa API của trường"
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing("new")}>
          Thêm khóa
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          {error}
        </Alert>
      )}
      {!connections && !error && <p className="py-3 text-[13px] text-muted">Đang tải…</p>}
      {connections && (
        <TableFrame filledHead className="mt-2">
          <thead>
            <tr>
              <Th>Nhà cung cấp</Th>
              <Th>Khóa</Th>
              <Th>Mô hình</Th>
              <Th className="w-[260px]">Kiểm tra</Th>
              <Th className="w-[1%]">
                <span className="sr-only">Bật và thao tác</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {connections.length === 0 && (
              <tr>
                <Td colSpan={5} className="py-6 text-center! text-muted">
                  Chưa có khóa
                </Td>
              </tr>
            )}
            {connections.map((c) => {
              const id = c.id as string;
              const label = PROVIDER_LABEL[c.provider];
              return (
                <tr key={id}>
                  <Td className="font-semibold whitespace-nowrap">{label}</Td>
                  <Td className="font-mono text-xs whitespace-nowrap">{masked(c.keyHint)}</Td>
                  <Td className="whitespace-nowrap">{c.model}</Td>
                  <Td>
                    <div className="flex items-center justify-between gap-3">
                      {c.lastTestResult ? (
                        <span
                          title={c.lastTestResult}
                          className={cx(
                            "min-w-0 truncate rounded-md px-2 py-0.5 text-[13px] font-medium",
                            c.lastTestResult === OK ? "bg-success-soft text-success" : "bg-danger-soft text-danger",
                          )}
                        >
                          {shortTestResult(c.lastTestResult)}
                        </span>
                      ) : (
                        <span />
                      )}
                      <Button
                        size="sm"
                        className="shrink-0"
                        disabled={busy !== null}
                        onClick={() => void run(id, () => testInstitutional(id), "Không kiểm tra được kết nối.")}
                      >
                        {busy === id ? "Đang kiểm tra…" : "Kiểm tra"}
                      </Button>
                    </div>
                  </Td>
                  <Td>
                    <div className="flex items-center justify-end gap-1">
                      <Switch
                        checked={c.status === "ACTIVE"}
                        label=""
                        ariaLabel={`Bật khóa ${label} ••••${c.keyHint ?? ""}`}
                        disabled={busy !== null}
                        onChange={(enabled) =>
                          void run(id, () => setInstitutionalEnabled(id, enabled, c.version), "Không đổi được trạng thái khóa.")
                        }
                      />
                      <IconButton icon="edit" label={`Thay khóa ${label}`} disabled={busy !== null} onClick={() => setEditing(c)} />
                      <IconButton
                        icon="trash"
                        label={`Thu hồi khóa ${label}`}
                        disabled={busy !== null}
                        className="text-danger! hover:bg-danger-soft"
                        onClick={() => setRevoking(c)}
                      />
                    </div>
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </TableFrame>
      )}
      <p className="pt-2 text-[11px] text-muted">Kiểm tra có thể tính phí nhỏ.</p>
      {editing && (
        <ApiKeyDialog
          key={current ? `${current.id}-${current.version}` : "new"}
          current={current}
          initialProvider={current?.provider ?? (providers[0]?.provider ?? "GEMINI")}
          providers={providers}
          onClose={() => setEditing(null)}
          onSave={async (provider, body) => {
            if (current?.id) await saveInstitutional(current.id, body);
            else await addInstitutional(provider, body);
            setEditing(null);
            await reload();
          }}
          onReveal={current?.id ? async () => (await revealInstitutional(current.id as string)).apiKey : undefined}
          onTest={async (provider, body) => {
            const result = await testInstitutionalDraft(provider, body, current?.id);
            return { ok: result.ok, text: result.message };
          }}
        />
      )}
      <Dialog
        open={revoking !== null}
        title="Thu hồi khóa của trường?"
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <Button
            variant="danger"
            onClick={() => {
              const target = revoking;
              setRevoking(null);
              if (target?.id) void run(target.id, () => revokeInstitutional(target.id as string), "Không thu hồi được khóa.");
            }}
          >
            Thu hồi
          </Button>
        }
      >
        <p>
          Khóa {revoking ? `${PROVIDER_LABEL[revoking.provider]} ••••${revoking.keyHint ?? ""}` : ""} sẽ bị xóa khỏi hệ thống. Thao
          tác không hoàn tác được.
        </p>
      </Dialog>
    </SettingsGroup>
  );
}
