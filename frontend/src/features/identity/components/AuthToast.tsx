"use client";

import { useEffect } from "react";
import { ToastStack } from "@/shared/ui/Toast";

export type AuthNotice = { id: number; text: string; tone: "success" | "warning" };

const VISIBLE_MS = 4000;

/** Thông báo một câu ngắn nổi ở góc phải màn chưa đăng nhập, tự ẩn sau vài giây; đổi `id` để hiện lại cùng một câu. */
export function AuthToast({ notice, onDone }: { notice: AuthNotice | null; onDone: () => void }) {
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(onDone, VISIBLE_MS);
    return () => clearTimeout(timer);
  }, [notice, onDone]);

  if (!notice) return null;
  return <ToastStack items={[notice]} className="top-5" />;
}
