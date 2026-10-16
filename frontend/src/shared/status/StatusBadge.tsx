type Tone = "neutral" | "progress" | "success" | "error";

const TONES: Record<Tone, string> = {
  neutral: "bg-slate-100 text-slate-700",
  progress: "bg-blue-100 text-blue-800",
  success: "bg-green-100 text-green-800",
  error: "bg-red-100 text-red-800",
};

/** Nhãn trạng thái; nhãn tiếng Việt do unit truyền vào. */
export function StatusBadge({ label, tone = "neutral" }: { label: string; tone?: Tone }) {
  return <span className={`rounded px-2 py-0.5 text-xs font-medium ${TONES[tone]}`}>{label}</span>;
}
