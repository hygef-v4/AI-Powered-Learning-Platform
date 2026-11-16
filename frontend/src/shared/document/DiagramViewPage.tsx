"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { PageHeader, Pill, Section } from "@/shared/ui";
import { diagramTypeLabels, encodeSvgDataUri, type DiagramType } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

function describeError(error: unknown): string {
  if (error instanceof ApiError && error.problem.status === 404) return msg("shared.document.DiagramViewPage.khongTimThaySoDoHoac");
  if (error instanceof ApiError) return error.problem.detail ?? msg("shared.document.DiagramViewPage.khongTaiDuocSoDo");
  return msg("shared.document.DiagramViewPage.khongTaiDuocSoDo");
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
  if (error) return <PageLoadError>{describeError(error)}</PageLoadError>;
  if (!data) return <PageLoading label={msg("shared.document.DiagramViewPage.dangTaiSoDo")} />;
  const file = `data:application/xml;charset=utf-8,${encodeURIComponent(data.xml)}`;
  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={backHref} title={data.alt || diagramTypeLabels[data.diagramType]} subtitle={data.title} />
      <Section title={<><Pill tone="accent">{diagramTypeLabels[data.diagramType]}</Pill></>}
        actions={<a href={file} download="so-do.drawio" className="text-xs font-semibold text-primary-ink">{msg("shared.document.DiagramViewPage.taiTepDrawio")}</a>}>
        {data.svgPreview ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={encodeSvgDataUri(data.svgPreview)} alt={data.alt || msg("shared.document.DiagramViewPage.soDo")} className="max-w-full" />
        ) : <p className="text-sm text-muted">{msg("shared.document.DiagramViewPage.soDoChuaCoAnhXem")}</p>}
      </Section>
    </div>
  );
}
