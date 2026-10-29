/** Cặp màu nền nhạt và chữ dùng cho nhãn, ô icon, thông báo. */
export type Tone = "neutral" | "primary" | "success" | "warning" | "danger" | "info" | "accent" | "orange";

export const SOFT_TONES: Record<Tone, string> = {
  neutral: "bg-sunken text-muted",
  primary: "bg-primary-soft text-primary-ink",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
  danger: "bg-danger-soft text-danger",
  info: "bg-info-soft text-info",
  accent: "bg-accent-soft text-accent",
  orange: "bg-orange-soft text-orange",
};
