"use client";

import { useEffect, useState } from "react";
import { PROVIDER_LABEL, saveSourcePreference, type SourceOption, type SourcePreference } from "@/shared/ai";
import { Alert, Button, Select, SettingLine, SettingsGroup } from "@/shared/ui";
import { getSources } from "../api";

const ASK = "ASK";

function keyOf(p: SourcePreference | SourceOption): string {
  return p.source ? `${p.source}:${p.provider ?? ""}` : ASK;
}

/**
 * Nguồn AI mặc định (FB-U06-01): chọn một lần, các chức năng AI tự dùng nguồn này thay vì hỏi mỗi lần. Nguồn chưa
 * dùng được vẫn chọn được (ví dụ chưa dán khóa) và hiện lý do; lúc dùng popup báo lại, không tự đổi nguồn.
 * `reloadKey` đổi khi khóa cá nhân thay đổi để cập nhật trạng thái dùng được.
 */
export function DefaultSourceSection({ reloadKey }: { reloadKey?: unknown }) {
  const [options, setOptions] = useState<SourceOption[] | null>(null);
  const [value, setValue] = useState(ASK);
  const [saved, setSaved] = useState(ASK);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ tone: "success" | "danger"; text: string } | null>(null);

  useEffect(() => {
    let active = true;
    getSources().then(
      (loaded) => {
        if (!active) return;
        const current = loaded.find((o) => o.preferred);
        setOptions(loaded);
        setValue(current ? keyOf(current) : ASK);
        setSaved(current ? keyOf(current) : ASK);
      },
      () => {
        if (active) setMessage({ tone: "danger", text: "Không tải được danh sách nguồn AI." });
      },
    );
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const label = (o: SourceOption) => (o.source === "SHARED" ? "API của trường" : `API cá nhân · ${o.provider ? PROVIDER_LABEL[o.provider] : ""}`);
  const chosen = options?.find((o) => keyOf(o) === value);

  const save = async () => {
    setBusy(true);
    setMessage(null);
    try {
      await saveSourcePreference(chosen ? { source: chosen.source, provider: chosen.provider } : { source: null, provider: null });
      setSaved(value);
      setMessage({ tone: "success", text: chosen ? `Đã đặt ${label(chosen)} làm nguồn mặc định.` : "Đã chuyển về hỏi nguồn mỗi lần." });
    } catch {
      setMessage({ tone: "danger", text: "Không lưu được nguồn mặc định, thử lại sau." });
    } finally {
      setBusy(false);
    }
  };

  return (
    <SettingsGroup title="Nguồn AI mặc định">
      <SettingLine
        label="Khi dùng chức năng AI"
        htmlFor="ai-default-source"
        info="Chọn một lần; tạo đề, sinh câu hỏi, đề xuất chấm sẽ tự dùng nguồn này. Nguồn tạm không dùng được thì hệ thống hỏi lại cho lần đó."
        control={
          <Select
            id="ai-default-source"
            aria-label="Nguồn AI mặc định"
            value={value}
            disabled={!options || busy}
            wrapperClassName="w-64"
            onChange={(e) => setValue(e.target.value)}
          >
            <option value={ASK}>Hỏi mỗi lần</option>
            {options?.map((o) => (
              <option key={keyOf(o)} value={keyOf(o)}>
                {label(o)}
                {o.available ? "" : " (chưa dùng được)"}
              </option>
            ))}
          </Select>
        }
        action={
          value !== saved ? (
            <Button size="sm" variant="primary" disabled={busy} onClick={() => void save()}>
              Lưu
            </Button>
          ) : undefined
        }
      />
      {chosen && !chosen.available && chosen.reason && <p className="text-xs text-warning">{chosen.reason}</p>}
      {message && (
        <Alert tone={message.tone} role={message.tone === "danger" ? "alert" : "status"}>
          {message.text}
        </Alert>
      )}
    </SettingsGroup>
  );
}
