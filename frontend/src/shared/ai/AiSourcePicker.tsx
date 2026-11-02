"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { apiFetch } from "@/shared/api/client";
import { Alert, Button, Dialog, Pill, cx } from "@/shared/ui";
import { PROVIDER_LABEL, formatTokens, masked } from "./format";
import type { SourceChoice, SourceOption } from "./types";

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
 * (BR-U06-28) và nguồn đó dùng được thì popup tự gửi bằng nguồn ấy, không bắt chọn lại; mặc định không dùng được
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
        if (active) setError("Không tải được danh sách nguồn AI, thử lại sau.");
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
      title="Chọn nguồn AI"
      icon="smart_toy"
      size="lg"
      onClose={onCancel}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onCancel} disabled={busy}>
            Hủy
          </Button>
          <Button
            variant="primary"
            disabled={!choice || busy}
            onClick={() => choice && onSubmit({ source: choice.source, provider: choice.provider })}
          >
            Gửi yêu cầu
          </Button>
        </>
      }
    >
      <p className="text-muted">{task}</p>
      {autoSent && choice && (
        <Alert tone="info" role="status">
          Đang gửi bằng nguồn mặc định: {label(choice)}.
        </Alert>
      )}
      {preferred && !preferred.available && (
        <Alert tone="warning" role="status">
          Nguồn mặc định {label(preferred)} chưa dùng được{preferred.reason ? `: ${preferred.reason}` : ""}. Chọn nguồn khác cho lần
          này.
        </Alert>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!options && !error && <p className="text-muted">Đang tải...</p>}
      {options && (
        <div role="radiogroup" aria-label="Nguồn AI" className="flex flex-col gap-2">
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
      <p className="text-[11px] text-muted">
        {preferred ? "Đổi nguồn mặc định" : "Không muốn chọn mỗi lần? Đặt nguồn mặc định"} ở{" "}
        <Link href="/ai/connection?tab=settings" className="font-semibold text-primary-ink underline">
          Kết nối AI
        </Link>
        . Agent MCP không phải nguồn ở đây; agent tự gọi công cụ của nền tảng từ ứng dụng của bạn.
      </p>
    </Dialog>
  );
}

function label(option: SourceOption) {
  return option.source === "SHARED" ? "API của trường" : `API cá nhân · ${option.provider ? PROVIDER_LABEL[option.provider] : ""}`;
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
        {option.preferred && <Pill tone="neutral">Mặc định</Pill>}
        <Pill tone={shared ? "primary" : "accent"} className="ml-auto">
          {shared ? "Trường chịu chi phí" : "Bạn chịu chi phí"}
        </Pill>
      </span>
      <span className="text-xs text-muted">
        {option.model ? `Mô hình ${option.model}` : shared ? "Mô hình do Quản trị viên cấu hình" : ""}
        {shared && option.quota && ` · còn ${formatTokens(option.quota.left)} / ${formatTokens(option.quota.limit)} token hôm nay`}
        {!shared && option.available && " · không giới hạn bởi trường"}
        {option.available && estimatedTokens !== undefined && ` · ước tính ${formatTokens(estimatedTokens)} token`}
      </span>
      {!option.available && option.reason && <span className="text-xs text-warning">{option.reason}</span>}
    </button>
  );
}
