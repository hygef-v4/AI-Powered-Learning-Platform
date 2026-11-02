"use client";

import { useCallback } from "react";
import { DiagramViewPage } from "@/shared/document";
import { workApi } from "./api";

/** Trang xem sơ đồ của bài làm, mở từ link trong DOCX xuất từ lượt. */
export function AttemptDiagramPage({ attemptId, blockId }: { attemptId: string; blockId: string }) {
  const load = useCallback(async (signal: AbortSignal) => {
    const diagram = await workApi.diagram(attemptId, blockId, signal);
    return { title: diagram.title, diagramType: diagram.diagramType, alt: diagram.alt, xml: diagram.xml, svgPreview: diagram.svgPreview };
  }, [attemptId, blockId]);
  return <DiagramViewPage load={load} backHref={`/attempts/${encodeURIComponent(attemptId)}`} />;
}
