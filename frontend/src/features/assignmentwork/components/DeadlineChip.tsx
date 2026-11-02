"use client";

import { useState } from "react";
import { Pill } from "@/shared/ui";
import type { Tone } from "@/shared/ui/tones";

/** Nhãn hạn nộp ngắn: "Còn 18 giờ", "Còn 3 ngày", "Quá hạn"; màu theo độ gấp: dưới 1 ngày đỏ, đến 3 ngày vàng, đến 7 ngày xanh dương, xa hơn xanh lá. */
export function DeadlineChip({ closesAt }: { closesAt: string | null }) {
  // Mốc "bây giờ" chụp một lần khi vẽ để nhãn ổn định giữa các lần render.
  const [now] = useState(() => Date.now());
  if (!closesAt) return <Pill>Không hạn</Pill>;
  const left = new Date(closesAt).getTime() - now;
  const hours = Math.floor(left / 3_600_000);
  let tone: Tone = "neutral";
  let label: string;
  if (left <= 0) {
    tone = "danger";
    label = "Quá hạn";
  } else if (hours < 24) {
    tone = "danger";
    label = hours < 1 ? "Còn dưới 1 giờ" : `Còn ${hours} giờ`;
  } else {
    const days = Math.floor(hours / 24);
    tone = days <= 3 ? "warning" : days <= 7 ? "info" : "success";
    label = `Còn ${days} ngày`;
  }
  return <Pill tone={tone}>{label}</Pill>;
}
