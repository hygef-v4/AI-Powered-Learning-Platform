"use client";

import { useEffect, useState } from "react";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { DocumentViewer } from "@/shared/document";
import { Alert, Dialog, cx } from "@/shared/ui";
import { getRevisions, groupDocImageUrl } from "../api";
import type { RevisionKind, RevisionView, SectionView, WorkspaceView } from "../model/types";

const KIND: Record<RevisionKind, string> = {
  DONE: "Xong",
  // Dữ liệu cũ của các tính năng đã bỏ (tiếp quản, nhập file cả tài liệu).
  TAKEOVER: "Bản nháp trước khi bị tiếp quản",
  IMPORT: "Nhập từ file",
};

function memberName(view: Pick<WorkspaceView, "members" | "viewerId">, id: string | null | undefined): string {
  if (!id) return "—";
  if (id === view.viewerId) return "Bạn";
  return view.members.find((m) => m.accountId === id)?.name ?? "Thành viên đã rời nhóm";
}

/** Lịch sử mục chỉ xem (BR-U12-35): mỗi lần bấm Xong là một bản kèm người viết và thời điểm. */
export function SectionHistoryDialog({
  view,
  section,
  onClose,
}: {
  view: Pick<WorkspaceView, "documentId" | "members" | "viewerId">;
  section: Pick<SectionView, "id" | "title">;
  onClose: () => void;
}) {
  const [revisions, setRevisions] = useState<RevisionView[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getRevisions(view.documentId, section.id).then(
      (loaded) => {
        if (!active) return;
        setRevisions(loaded);
        setSelected(loaded[0]?.id ?? null);
      },
      (e: unknown) => {
        if (active) setError(e instanceof ApiError ? (e.problem.detail ?? "Không tải được lịch sử mục.") : "Không tải được lịch sử mục.");
      },
    );
    return () => {
      active = false;
    };
  }, [view.documentId, section.id]);

  const current = revisions?.find((r) => r.id === selected) ?? null;
  return (
    <Dialog open size="xl" icon="history" title={`Lịch sử · ${section.title}`} onClose={onClose}>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!revisions && !error && <p className="text-sm text-muted">Đang tải…</p>}
      {revisions && revisions.length === 0 && <p className="text-sm text-muted">Mục chưa có bản lịch sử nào.</p>}
      {revisions && revisions.length > 0 && (
        <div className="grid gap-3 md:grid-cols-[240px_1fr]">
          <ul aria-label="Các bản lịch sử" className="flex max-h-[60vh] flex-col gap-1 overflow-y-auto">
            {revisions.map((r) => (
              <li key={r.id}>
                <button
                  type="button"
                  aria-pressed={r.id === selected}
                  onClick={() => setSelected(r.id)}
                  className={cx(
                    "flex w-full flex-col gap-0.5 rounded-lg border px-3 py-2 text-left text-xs",
                    r.id === selected ? "border-primary bg-primary-soft" : "border-line hover:bg-shell",
                  )}
                >
                  <span className="font-semibold text-ink">{formatDateTime(r.createdAt)}</span>
                  <span className="text-muted">
                    {KIND[r.kind]} · {memberName(view, r.authorId)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
          <div className="min-h-40 rounded-xl border border-line p-3">
            {current && <DocumentViewer document={{ blocks: current.blocks }} imageUrl={groupDocImageUrl(view.documentId)} empty="Bản này trống." />}
          </div>
        </div>
      )}
    </Dialog>
  );
}
