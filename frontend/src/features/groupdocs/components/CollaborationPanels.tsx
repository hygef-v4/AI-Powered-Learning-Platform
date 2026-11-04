"use client";

import { useEffect, useState } from "react";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { DocumentViewer } from "@/shared/document";
import { Alert, Button, Dialog, Field, Pill, Section, Select, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { applyImport, getPeerReviews, getRevisions, groupDocImageUrl, previewImport, savePeerReviews } from "../api";
import type { DocumentImport, PeerGiven, PeerReviewView, RevisionKind, RevisionView, SectionView, WorkspaceView } from "../model/types";

const KIND: Record<RevisionKind, string> = {
  DONE: "Xong",
  // Dữ liệu cũ, trước khi bỏ tiếp quản (2026-11-02).
  TAKEOVER: "Bản nháp trước khi bị tiếp quản",
  IMPORT: "Nhập từ file",
};

function message(e: unknown, fallback: string): string {
  return e instanceof ApiError ? (e.problem.detail ?? fallback) : fallback;
}

function memberName(view: Pick<WorkspaceView, "members" | "viewerId">, id: string | null | undefined): string {
  if (!id) return "—";
  if (id === view.viewerId) return "Bạn";
  return view.members.find((m) => m.accountId === id)?.name ?? "Thành viên đã rời nhóm";
}

/**
 * Lịch sử mục (BR-U12-35): mỗi lần Xong, tiếp quản, nhập file là một bản; bấm để xem. Người đang giữ mục (`onRestore`)
 * đưa một bản vào bản nháp rồi bấm Xong mới đăng.
 */
export function SectionHistoryDialog({
  view,
  section,
  onRestore,
  onClose,
}: {
  view: Pick<WorkspaceView, "documentId" | "members" | "viewerId">;
  section: Pick<SectionView, "id" | "title">;
  onRestore?: (revision: RevisionView) => Promise<void>;
  onClose: () => void;
}) {
  const [revisions, setRevisions] = useState<RevisionView[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getRevisions(view.documentId, section.id).then(
      (loaded) => {
        if (!active) return;
        setRevisions(loaded);
        setSelected(loaded[0]?.id ?? null);
      },
      (e: unknown) => {
        if (active) setError(message(e, "Không tải được lịch sử mục."));
      },
    );
    return () => {
      active = false;
    };
  }, [view.documentId, section.id]);

  const current = revisions?.find((r) => r.id === selected) ?? null;
  return (
    <Dialog
      open
      size="xl"
      icon="history"
      title={`Lịch sử · ${section.title}`}
      onClose={onClose}
      dismissible={!busy}
      footer={
        onRestore && current ? (
          <Button
            variant="primary"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              setError(null);
              try {
                await onRestore(current);
                onClose();
              } catch (e) {
                setError(message(e, "Không khôi phục được bản này."));
                setBusy(false);
              }
            }}
          >
            Dùng bản này làm bản nháp
          </Button>
        ) : undefined
      }
    >
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
      {onRestore && <p className="text-xs text-muted">Khôi phục chỉ thay bản nháp của bạn; bấm Xong để đăng cho cả nhóm.</p>}
    </Dialog>
  );
}

/**
 * Trưởng nhóm nhập DOCX và/hoặc tệp Draw.io cho cả tài liệu (FB-U12-03): tiêu đề trong DOCX trùng tên mục thì nội dung
 * vào mục đó; mỗi trang Draw.io đặt tên trùng tên mục thành sơ đồ của mục. Xem trước, bỏ chọn mục không muốn, rồi áp
 * dụng. Không dùng AI nên không tốn token.
 */
export function GroupImportDialog({ view, onClose, onImported }: { view: WorkspaceView; onClose: () => void; onImported: (text: string) => void }) {
  const [docx, setDocx] = useState<File | null>(null);
  const [drawio, setDrawio] = useState<File | null>(null);
  const [preview, setPreview] = useState<DocumentImport | null>(null);
  const [chosen, setChosen] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const read = async () => {
    setBusy(true);
    setError(null);
    setPreview(null);
    try {
      const loaded = await previewImport(view.documentId, { docx, drawio });
      setPreview(loaded);
      setChosen(loaded.sections.filter((s) => !s.skipReason).map((s) => s.sectionId));
    } catch (e) {
      setError(message(e, "Không đọc được tệp."));
    } finally {
      setBusy(false);
    }
  };

  const apply = async () => {
    if (!preview) return;
    setBusy(true);
    setError(null);
    try {
      const result = await applyImport(
        view.documentId,
        preview.sections.filter((s) => chosen.includes(s.sectionId)).map((s) => ({ sectionId: s.sectionId, version: s.version, blocks: s.blocks })),
      );
      onImported(
        `Đã nhập vào ${result.applied} mục.` + (result.skipped.length ? ` Bỏ qua: ${result.skipped.join("; ")}.` : ""),
      );
      onClose();
    } catch (e) {
      setError(message(e, "Không nhập được."));
      setBusy(false);
    }
  };

  const filePicker = (label: string, accept: string, file: File | null, set: (f: File | null) => void) => (
    <span className="flex items-center gap-2">
      <label className="inline-flex w-fit cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong bg-surface px-3 py-1.5 text-xs font-semibold">
        {file ? "Chọn tệp khác" : label}
        <input
          type="file"
          accept={accept}
          className="sr-only"
          aria-label={label}
          onChange={(e) => {
            set(e.target.files?.[0] ?? null);
            setPreview(null);
            e.target.value = "";
          }}
        />
      </label>
      {file && <span className="text-xs text-muted">{file.name}</span>}
    </span>
  );

  return (
    <Dialog
      open
      size="xl"
      icon="upload_file"
      title="Nhập tài liệu nhóm từ file"
      onClose={onClose}
      dismissible={!busy}
      footer={
        preview ? (
          <Button variant="primary" disabled={busy || chosen.length === 0} onClick={() => void apply()}>
            Nhập vào {chosen.length} mục
          </Button>
        ) : (
          <Button variant="primary" disabled={busy || (!docx && !drawio)} onClick={() => void read()}>
            {busy ? "Đang đọc…" : "Xem trước"}
          </Button>
        )
      }
    >
      <Alert tone="info">
        Đặt tiêu đề trong Word trùng tên mục (ví dụ “1. Phân tích yêu cầu”) và đặt tên mỗi trang Draw.io trùng tên mục có sơ đồ.
        Hệ thống chia theo tên, không dùng AI nên không tốn token. Mục đang có người giữ hoặc đã chốt sẽ được bỏ qua.
      </Alert>
      <Field label="Tệp Word" hint="DOCX, tối đa 20 MB.">
        {filePicker("Chọn tệp DOCX", ".docx", docx, setDocx)}
      </Field>
      {view.diagramEnabled && (
        <Field label="Tệp Draw.io" hint="Mỗi trang một sơ đồ; .drawio hoặc .xml.">
          {filePicker("Chọn tệp Draw.io", ".drawio,.xml", drawio, setDrawio)}
        </Field>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {preview && (
        <div className="flex flex-col gap-3" data-testid="group-import-preview">
          {preview.sections.length === 0 ? (
            <Alert tone="warning">Không tìm thấy tiêu đề hoặc trang nào trùng tên mục.</Alert>
          ) : (
            <TableFrame className="[&_table]:table-fixed">
              <colgroup><col className="w-[44px]" /><col /><col className="w-[110px]" /><col className="w-[34%]" /></colgroup>
              <thead>
                <tr>
                  <Th>Nhập</Th>
                  <Th>Mục</Th>
                  <Th>Nội dung</Th>
                  <Th>Ghi chú</Th>
                </tr>
              </thead>
              <tbody>
                {preview.sections.map((s) => (
                  <tr key={s.sectionId}>
                    <Td>
                      <input
                        type="checkbox"
                        aria-label={`Nhập vào ${s.title}`}
                        disabled={!!s.skipReason}
                        checked={chosen.includes(s.sectionId)}
                        onChange={(e) => setChosen((c) => (e.target.checked ? [...c, s.sectionId] : c.filter((id) => id !== s.sectionId)))}
                      />
                    </Td>
                    <Td>{s.title}</Td>
                    <Td>
                      {s.blocks.length} khối
                      {s.blocks.some((b) => b.type === "DIAGRAM") && " · có sơ đồ"}
                    </Td>
                    <Td>{s.skipReason ? <Pill tone="warning">{s.skipReason}</Pill> : s.replaces ? "Thay nội dung đã Xong" : "Mục đang trống"}</Td>
                  </tr>
                ))}
              </tbody>
            </TableFrame>
          )}
          {preview.unmatched.length > 0 && (
            <Alert tone="warning" title="Không khớp mục nào, sẽ bỏ qua">
              <ul className="list-disc pl-4">
                {preview.unmatched.map((u) => (
                  <li key={u}>{u}</li>
                ))}
              </ul>
            </Alert>
          )}
          {preview.warnings.map((w, i) => (
            <Alert key={i} tone="warning">
              {w.message}
            </Alert>
          ))}
          <p className="text-xs text-muted">Mỗi mục được nhập có thêm một bản trong lịch sử, khôi phục lại được.</p>
        </div>
      )}
    </Dialog>
  );
}

/**
 * Thẻ Đánh giá chéo (FB-U12-04). Thành viên chấm từng thành viên khác 1–5 kèm nhận xét sau khi nhóm nộp; giảng viên
 * xem trung bình, quy đổi thang 10 và nhận xét. Điểm chỉ để giảng viên tham khảo khi chấm.
 */
export function PeerReviewPanel({ view }: { view: WorkspaceView }) {
  const [data, setData] = useState<PeerReviewView | null>(null);
  const [draft, setDraft] = useState<Record<string, { score: string; comment: string }>>({});
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getPeerReviews(view.documentId).then(
      (loaded) => {
        if (!active) return;
        setData(loaded);
        setDraft(Object.fromEntries(loaded.mine.map((g) => [g.revieweeId, { score: String(g.score), comment: g.comment ?? "" }])));
      },
      (e: unknown) => {
        if (active) setError(message(e, "Không tải được đánh giá chéo."));
      },
    );
    return () => {
      active = false;
    };
  }, [view.documentId]);

  if (error && !data) return <Alert tone="danger" role="alert">{error}</Alert>;
  if (!data) return <p className="text-[13px] text-muted">Đang tải…</p>;

  const others = view.members.filter((m) => m.accountId !== view.viewerId);
  const windowText = data.open
    ? data.closesAt
      ? `Đang mở, sửa được tới ${formatDateTime(data.closesAt)}.`
      : "Đang mở."
    : (data.closedReason ?? "Chưa mở.");

  if (view.role.teacher) {
    return (
      <Section title="Đánh giá chéo" icon="groups">
        <p className="text-[13px] text-body">
          {data.reviewers}/{data.members} thành viên đã đánh giá · {windowText}
        </p>
        <TableFrame className="[&_table]:min-w-[560px] [&_table]:table-fixed">
          <colgroup><col className="w-[28%]" /><col className="w-[110px]" /><col className="w-[110px]" /><col /></colgroup>
          <thead>
            <tr>
              <Th>Thành viên</Th>
              <Th>Trung bình (1–5)</Th>
              <Th>Thang 10</Th>
              <Th>Nhận xét</Th>
            </tr>
          </thead>
          <tbody>
            {data.summary.map((s) => (
              <tr key={s.accountId}>
                <Td>{memberName(view, s.accountId)}</Td>
                <Td>{s.average === null ? "—" : `${s.average.toFixed(1)} (${s.count} người)`}</Td>
                <Td>{s.outOfTen === null ? "—" : s.outOfTen.toFixed(1)}</Td>
                <Td>
                  {s.reviews.filter((r) => r.comment).length === 0 ? (
                    <span className="text-muted">—</span>
                  ) : (
                    <ul className="flex flex-col gap-1">
                      {s.reviews.filter((r) => r.comment).map((r) => (
                        <li key={r.reviewerId} className="text-xs">
                          <span className="font-semibold">{memberName(view, r.reviewerId)}</span> ({r.score}/5): {r.comment}
                        </li>
                      ))}
                    </ul>
                  )}
                </Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
        <p className="text-xs text-muted">
          Điểm đánh giá chéo chỉ để tham khảo khi chấm; thành viên không thấy đánh giá của nhau.
        </p>
      </Section>
    );
  }

  const complete = others.every((m) => draft[m.accountId]?.score);
  const save = async () => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const reviews: PeerGiven[] = others
        .filter((m) => draft[m.accountId]?.score)
        .map((m) => ({ revieweeId: m.accountId, score: Number(draft[m.accountId]!.score), comment: draft[m.accountId]!.comment || null }));
      setData(await savePeerReviews(view.documentId, reviews));
      setNotice("Đã lưu đánh giá.");
    } catch (e) {
      setError(message(e, "Không lưu được đánh giá."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Section title="Đánh giá chéo" icon="groups">
      <p className="text-[13px] text-body">
        Chấm mức đóng góp của từng thành viên khác (1 thấp – 5 cao). Chỉ giảng viên thấy đánh giá của bạn. {windowText}
      </p>
      {others.length === 0 && <p className="text-[13px] text-muted">Nhóm chỉ có mình bạn.</p>}
      <div className="flex flex-col gap-3">
        {others.map((m) => {
          const value = draft[m.accountId] ?? { score: "", comment: "" };
          const set = (next: Partial<typeof value>) => setDraft((d) => ({ ...d, [m.accountId]: { ...value, ...next } }));
          return (
            <div key={m.accountId} className="grid gap-2 rounded-xl border border-line p-3 sm:grid-cols-[180px_140px_1fr] sm:items-center">
              <span className="text-[13px] font-semibold text-ink">{m.name ?? "Thành viên"}</span>
              <Select aria-label={`Điểm cho ${m.name ?? "thành viên"}`} value={value.score} disabled={!data.open || busy}
                onChange={(e) => set({ score: e.target.value })}>
                <option value="">Chọn điểm</option>
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>
                    {n} / 5
                  </option>
                ))}
              </Select>
              <TextInput aria-label={`Nhận xét cho ${m.name ?? "thành viên"}`} placeholder="Nhận xét (không bắt buộc)" maxLength={1000}
                value={value.comment} disabled={!data.open || busy} onChange={(e) => set({ comment: e.target.value })} />
            </div>
          );
        })}
      </div>
      {notice && <Alert tone="success" role="status">{notice}</Alert>}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {data.open && others.length > 0 && (
        <div className="flex items-center gap-2">
          <Button variant="primary" disabled={busy || !complete} onClick={() => void save()}>
            Lưu đánh giá
          </Button>
          {!complete && <span className="text-xs text-muted">Chấm đủ mọi thành viên để lưu.</span>}
        </div>
      )}
    </Section>
  );
}
