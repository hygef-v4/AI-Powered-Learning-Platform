"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import type { SourceChoice, SourcePreference } from "./types";

const PATH = "/api/v1/me/ai-source-preference";

export const getSourcePreference = () => apiFetch<SourcePreference>(PATH);

/** `source` null: bỏ mặc định, hỏi mỗi lần. */
export const saveSourcePreference = (preference: SourcePreference) =>
  apiFetch<SourcePreference>(PATH, { method: "PUT", body: JSON.stringify(preference) });

/** Nguồn mặc định cho màn tự chọn nguồn (không dùng popup); `null` khi chưa đặt hoặc không tải được. */
export function usePreferredSource(): SourceChoice | null {
  const [choice, setChoice] = useState<SourceChoice | null>(null);
  useEffect(() => {
    let active = true;
    getSourcePreference().then(
      (p) => {
        if (active && p?.source) setChoice({ source: p.source, provider: p.provider });
      },
      () => undefined,
    );
    return () => {
      active = false;
    };
  }, []);
  return choice;
}
