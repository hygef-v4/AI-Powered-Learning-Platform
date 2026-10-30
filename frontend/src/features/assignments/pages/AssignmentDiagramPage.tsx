"use client";

import { useCallback } from "react";
import { assignmentsApi } from "../api";
import { DiagramViewPage } from "@/shared/document";

export function AssignmentDiagramPage({ id, blockId }: { id: string; blockId: string }) {
  const load = useCallback(async (signal: AbortSignal) => {
    const diagram = await assignmentsApi.diagram(id, blockId, signal);
    return { title: diagram.assignmentTitle, diagramType: diagram.diagramType, alt: diagram.alt, xml: diagram.xml, svgPreview: diagram.svgPreview };
  }, [id, blockId]);
  return <DiagramViewPage load={load} backHref={`/assignments/${encodeURIComponent(id)}`} />;
}
