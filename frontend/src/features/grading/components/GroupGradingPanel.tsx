"use client";
import { useEffect, useRef, type ReactNode } from "react";
import { useWideLayout } from "@/shared/document";
import { Button, Tabs, tabId } from "@/shared/ui";
import { msg } from "@/i18n/msg";

export function GroupGradingPanel({ mode, onModeChange, open, onClose, rubric, children, selectionKey }: {
  mode: "rubric" | "members";
  onModeChange: (mode: "rubric" | "members") => void;
  open: boolean;
  onClose: () => void;
  rubric: ReactNode;
  children: ReactNode;
  selectionKey?: string | null;
}) {
  const wide = useWideLayout();
  const panel = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const element = panel.current;
    if (!element) return;
    element.scrollTop = 0;
    if (wide && element.parentElement) element.parentElement.scrollTop = 0;
  }, [mode, selectionKey, open, wide]);
  useEffect(() => {
    if (wide || !open) return;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    panel.current?.focus({ preventScroll: true });
    return () => { if (previous?.isConnected) previous.focus({ preventScroll: true }); };
  }, [wide, open]);
  return <div ref={panel} role="region" aria-label={msg("grading.components.GroupGradingPanel.chamBaiNhom")} tabIndex={-1}
    hidden={!wide && !open}
    onKeyDown={(event) => {
      if (!wide && event.key === "Escape") { event.stopPropagation(); onClose(); }
    }}
    className={wide ? "min-w-0 rounded-xl border border-line bg-surface p-3"
      : "fixed inset-x-3 bottom-3 z-40 max-h-[75dvh] overflow-y-auto rounded-xl border border-line-strong bg-surface p-3 shadow-xl"}>
    <div className="flex items-center gap-2">
      <Tabs items={[{ key: "rubric", label: msg("grading.components.GroupGradingPanel.rubricNhom") }, { key: "members", label: msg("grading.components.GroupGradingPanel.thanhVien") }]}
        value={mode} onChange={onModeChange} label={msg("grading.components.GroupGradingPanel.chamNhom")} className="min-w-0 flex-1 gap-4" panelId="group-grading-content" />
      {!wide && <Button size="sm" variant="ghost" icon="close" aria-label={msg("grading.components.GroupGradingPanel.dongKhungCham")} onClick={onClose}>
        <span className="sr-only">{msg("grading.components.GroupGradingPanel.dong")}</span>
      </Button>}
    </div>
    <div id="group-grading-content" role="tabpanel" aria-labelledby={tabId("group-grading-content", mode)} className="mt-3">
      <div hidden={mode !== "rubric"} className="space-y-3">{rubric}</div>
      <div hidden={mode !== "members"}>{children}</div>
    </div>
  </div>;
}
