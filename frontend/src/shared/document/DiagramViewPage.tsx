"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, PageHeader, Pill, Section } from "@/shared/ui";
import { diagramTypeLabels, svgDataUri, type DiagramType } from "./types";

function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.problem.status === 404) return "Không tìm thấy sơ đồ hoặc bạn không có quyền xem.";
  if (error instanceof ApiError) return error.problem.detail ?? "Không tải được sơ đồ";
  return "Không tải được sơ đồ";
}

export type DiagramData = { title: string; diagramType: DiagramType; alt: string; xml: string; svgPreview: string | null };

/** Trang xem sơ đồ mở từ link trong DOCX (BR-U07-60); trang tự kiểm quyền qua API. Tải được tệp .drawio để mở lại. */
export function DiagramViewPage({ load, backHref }: { load: (signal: AbortSignal) => Promise<DiagramData>; backHref: string }) {
  const [data, setData] = useState<DiagramData | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal).then((value) => { if (!controller.signal.aborted) setData(value); },
      (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [load]);
  if (error) return <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>;
  if (!data) return <p role="status" className="text-sm text-muted">Đang tải sơ đồ…</p>;
  const file = `data:application/xml;charset=utf-8,${encodeURIComponent(data.xml)}`;
  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={backHref} title={data.alt || diagramTypeLabels[data.diagramType]} subtitle={data.title} />
      <Section title={<><Pill tone="accent">{diagramTypeLabels[data.diagramType]}</Pill></>}
        actions={<a href={file} download="so-do.drawio" className="text-xs font-semibold text-primary">Tải tệp .drawio</a>}>
        {data.svgPreview ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={svgDataUri(data.svgPreview)} alt={data.alt || "Sơ đồ"} className="max-w-full" />
        ) : <p className="text-sm text-muted">Sơ đồ chưa có ảnh xem trước. Tải tệp .drawio để mở bằng Draw.io.</p>}
      </Section>
    </div>
  );
}
