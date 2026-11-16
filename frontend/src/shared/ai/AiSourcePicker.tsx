"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { apiFetch } from "@/shared/api/client";
import { Alert, Button, Dialog, Pill, cx } from "@/shared/ui";
import { PROVIDER_LABEL, formatTokens, masked } from "./format";
import type { SourceChoice, SourceOption } from "./types";
import { msg } from "@/i18n/msg";

type Props = {
  open: boolean;
  /** Dòng "tác vụ · đích", ví dụ "Sinh câu hỏi · Bài 3 Cấu trúc dữ liệu". */
  task: string;
  /** Token ước tính cho yêu cầu này, nếu unit gọi tính được. */
  estimatedTokens?: number;
  busy?: boolean;
  onCancel: () => void;
  onSubmit: (choice: SourceChoice) => void;
};

function keyOf(option: SourceOption) {
  return `${option.source}:${option.provider ?? ""}`;
}

/**
 * Popup Chọn nguồn AI (c16): nguồn không dùng được hiện mờ kèm lý do. Người dùng đã đặt nguồn mặc định
 * (FB-U06-01) và nguồn đó dùng được thì popup tự gửi bằng nguồn ấy, không bắt chọn lại; mặc định không dùng được
 * thì báo lý do để chọn nguồn khác cho lần này. Lỗi hay hết hạn mức không tự đổi nguồn (BR-U06-10, 11).
 */
export function AiSourcePicker({ open, task, estimatedTokens, busy, onCancel, onSubmit }: Props) {
  const [options, setOptions] = useState<SourceOption[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  // Mỗi lần mở chỉ tự gửi một lần; gửi lỗi mà popup còn mở thì người dùng chọn lại như thường.
  const [autoSent, setAutoSent] = useState(false);
  const submitRef = useRef(onSubmit);
  useEffect(() => {
    submitRef.current = onSubmit;
  }, [onSubmit]);

  useEffect(() => {
    if (!open) return;
    let active = true;
    apiFetch<SourceOption[]>("/api/v1/ai/sources").then(
      (loaded) => {
        if (!active) return;
        setOptions(loaded);
        setError(null);
        const preferred = loaded.find((option) => option.preferred && option.available);
        setSelected(preferred ? keyOf(preferred) : null);
        setAutoSent(!!preferred);
        if (preferred) submitRef.current({ source: preferred.source, provider: preferred.provider });
      },
      () => {
        if (active) setError(msg("shared.ai.AiSourcePicker.khongTaiDuocDanhSachNguon"));
      },
    );
    return () => {
      active = false;
    };
  }, [open]);

  const choice = options?.find((option) => keyOf(option) === selected && option.available);
  const preferred = options?.find((option) => option.preferred);

  return (
    <Dialog
      open={open}
      title={msg("shared.ai.AiSourcePicker.chonNguonAi")}
      icon="smart_toy"
      size="lg"
      onClose={onCancel}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onCancel} disabled={busy}>
            {msg("shared.ai.AiSourcePicker.huy")}</Button>
          <Button
            variant="primary"
            disabled={!choice || busy}
            onClick={() => choice && onSubmit({ source: choice.source, provider: choice.provider })}
          >
            {msg("shared.ai.AiSourcePicker.guiYeuCau")}</Button>
        </>
      }
    >
      <p className="text-muted">{task}</p>
      {autoSent && choice && (
        <Alert tone="info" role="status">
          {msg("shared.ai.AiSourcePicker.dangGuiBangNguonMacDinh")}{label(choice)}.
        </Alert>
      )}
      {preferred && !preferred.available && (
        <Alert tone="warning" role="status">
          {msg("shared.ai.AiSourcePicker.nguonMacDinh")}{label(preferred)} {msg("shared.ai.AiSourcePicker.chuaDungDuoc")}{preferred.reason ? `: ${preferred.reason}` : ""}{msg("shared.ai.AiSourcePicker.chonNguonKhacChoLanNay")}</Alert>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!options && !error && <p className="text-muted">{msg("shared.ai.AiSourcePicker.dangTai")}</p>}
      {options && (
        <div role="radiogroup" aria-label={msg("shared.ai.AiSourcePicker.nguonAi")} className="flex flex-col gap-2">
          {options.map((option) => (
            <SourceRow
              key={keyOf(option)}
              option={option}
              checked={selected === keyOf(option)}
              estimatedTokens={estimatedTokens}
              onSelect={() => setSelected(keyOf(option))}
            />
          ))}
        </div>
      )}
      <p className="text-xs text-muted">
        {preferred ? msg("shared.ai.AiSourcePicker.doiNguonMacDinh") : msg("shared.ai.AiSourcePicker.khongMuonChonMoiLanDat")} {msg("shared.ai.AiSourcePicker.o")}{" "}
        <Link href="/ai/connection?tab=settings" className="font-semibold text-primary-ink underline">
          {msg("shared.ai.AiSourcePicker.ketNoiAi")}</Link>
        {msg("shared.ai.AiSourcePicker.agentMcpKhongPhaiNguonO")}</p>
    </Dialog>
  );
}

function label(option: SourceOption) {
  return option.source === "SHARED" ? msg("shared.ai.AiSourcePicker.apiCuaTruong") : msg("shared.ai.AiSourcePicker.apiCaNhan", { value: String(option.provider ? PROVIDER_LABEL[option.provider] : "") });
}

function SourceRow({
  option,
  checked,
  estimatedTokens,
  onSelect,
}: {
  option: SourceOption;
  checked: boolean;
  estimatedTokens?: number;
  onSelect: () => void;
}) {
  const shared = option.source === "SHARED";
  const title = label(option);
  return (
    <button
      type="button"
      role="radio"
      aria-checked={checked}
      aria-disabled={!option.available}
      disabled={!option.available}
      onClick={onSelect}
      className={cx(
        "flex w-full flex-col gap-1 rounded-xl border px-3.5 py-3 text-left transition-colors",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus",
        checked ? "border-primary bg-primary-soft" : "border-line-strong bg-surface hover:bg-shell",
        !option.available && "cursor-not-allowed opacity-55 hover:bg-surface",
      )}
    >
      <span className="flex items-center gap-2 text-[13px] font-semibold text-ink">
        {title}
        {option.keyHint && <span className="font-normal text-muted">{masked(option.keyHint)}</span>}
        {option.preferred && <Pill tone="neutral">{msg("shared.ai.AiSourcePicker.macDinh")}</Pill>}
        <Pill tone={shared ? "primary" : "accent"} className="ml-auto">
          {shared ? msg("shared.ai.AiSourcePicker.truongChiuChiPhi") : msg("shared.ai.AiSourcePicker.banChiuChiPhi")}
        </Pill>
      </span>
      <span className="text-xs text-muted">
        {option.model ? msg("shared.ai.AiSourcePicker.moHinh", { model: String(option.model) }) : shared ? msg("shared.ai.AiSourcePicker.moHinhDoQuanTriVien") : ""}
        {shared && option.quota && msg("shared.ai.AiSourcePicker.conTokenHomNay", { value: String(formatTokens(option.quota.left)), value2: String(formatTokens(option.quota.limit)) })}
        {!shared && option.available && msg("shared.ai.AiSourcePicker.khongGioiHanBoiTruong")}
        {option.available && estimatedTokens !== undefined && msg("shared.ai.AiSourcePicker.uocTinhToken", { value: String(formatTokens(estimatedTokens)) })}
      </span>
      {!option.available && option.reason && <span className="text-xs text-warning">{option.reason}</span>}
    </button>
  );
}
