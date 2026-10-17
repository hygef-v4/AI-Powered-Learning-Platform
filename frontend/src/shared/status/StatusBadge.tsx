import { Pill } from "@/shared/ui/Pill";
import type { Tone } from "@/shared/ui/tones";

type StatusTone = "neutral" | "progress" | "success" | "warning" | "error";

const TONES: Record<StatusTone, Tone> = {
  neutral: "neutral",
  progress: "primary",
  success: "success",
  warning: "warning",
  error: "danger",
};

/** Nhãn trạng thái; nhãn tiếng Việt do unit truyền vào. */
export function StatusBadge({ label, tone = "neutral" }: { label: string; tone?: StatusTone }) {
  return <Pill tone={TONES[tone]}>{label}</Pill>;
}
