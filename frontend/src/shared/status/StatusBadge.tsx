export type StatusTone = "neutral" | "progress" | "success" | "warning" | "error";

export type StatusLabel = { text: string; tone: StatusTone };

const TONES: Record<StatusTone, string> = {
  neutral: "bg-slate-100 text-slate-700",
  progress: "bg-blue-100 text-blue-800",
  success: "bg-green-100 text-green-800",
  warning: "bg-amber-100 text-amber-900",
  error: "bg-red-100 text-red-800",
};

type StatusBadgeProps = {
  status: string;
  /** Nhãn tiếng Việt do unit sở hữu truyền vào, ví dụ `{ SCANNING: { text: "Đang quét", tone: "progress" } }`. */
  labels: Record<string, StatusLabel>;
};

export function StatusBadge({ status, labels }: StatusBadgeProps) {
  const label = labels[status] ?? { text: status, tone: "neutral" as const };
  return (
    <span
      data-testid="status-badge"
      data-status={status}
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TONES[label.tone]}`}
    >
      {label.tone === "progress" && (
        <span className="mr-1 h-2 w-2 animate-pulse rounded-full bg-current" aria-hidden />
      )}
      {label.text}
    </span>
  );
}
