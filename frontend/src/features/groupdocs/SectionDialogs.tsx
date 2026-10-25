"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { formatDateTime } from "@/features/groups/format";
import { ApiError } from "@/shared/api/client";
import { DocumentEditor, DocumentViewer } from "@/shared/document";
import { Alert, Button, Dialog, Pill, Select, TableFrame, Td, Th } from "@/shared/ui";
import { assignSection, finishSection, saveDraft, submitDocument } from "./api";
import type { DocumentBlock, DocumentModel, Receipt, SectionView, WorkspaceView } from "./types";

export const AUTOSAVE_MS = 10_000;

function problemMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.problem.status === 409) return e.problem.detail ?? "Dữ liệu vừa thay đổi, hãy tải lại.";
    return e.problem.detail ?? fallback;
  }
  return fallback;
}

/**
 * Phần của mục trong khung chung: heading của phần (`partId` là id của heading) và các block của người soạn tới heading
 * kế tiếp. Không tìm thấy heading thì dựng heading theo tên mục để trình soạn vẫn có chỗ viết.
 */
export function partOf(shared: DocumentModel, section: Pick<SectionView, "partId" | "title">) {
  const blocks = shared.blocks;
  const at = blocks.findIndex((b) => b.id === section.partId);
  if (at < 0) {
    const heading: DocumentBlock = { id: section.partId, origin: "TEACHER", type: "HEADING", content: { style: "H2", text: section.title } };
    return { heading, guide: [] as DocumentBlock[] };
  }
  const next = blocks.findIndex((b, i) => i > at && b.type === "HEADING");
  return { heading: blocks[at]!, guide: blocks.slice(at + 1, next < 0 ? undefined : next) };
}

/**
 * Trình soạn mục che kín trang (s18b, BR-U12-34) dùng `DocumentEditor` chung ở chế độ người học: chỉ người giữ; heading
 * của phần là block khóa, hướng dẫn của giảng viên trong phần chỉ xem; chỉ block của người học được lưu. Tự lưu 10 giây
 * sau lần sửa cuối kèm `version` của mục; `409` báo tải lại; Xong đăng nội dung và đóng. Draw.io theo cài đặt của bài.
 */
export function SectionEditorDialog({
  documentId,
  section,
  shared,
  diagramEnabled,
  onClose,
  onChanged,
}: {
  documentId: string;
  section: SectionView;
  shared: DocumentModel;
  diagramEnabled: boolean;
  onClose: () => void;
  onChanged: () => void;
}) {
  const part = useMemo(() => partOf(shared, section), [shared, section]);
  const [doc, setDoc] = useState<DocumentModel>(() => ({
    blocks: [part.heading, ...(section.draftBlocks ?? section.publishedBlocks ?? [])],
  }));
  const [version, setVersion] = useState(section.version);
  const [savedAt, setSavedAt] = useState<string | null>(section.draftSavedAt);
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const save = useCallback(async () => {
    try {
      const saved = await saveDraft(documentId, section.id, version, doc.blocks.filter((b) => b.origin === "STUDENT"));
      setVersion(saved.version);
      setSavedAt(saved.draftSavedAt);
      setDirty(false);
      setError(null);
      return true;
    } catch (e) {
      setError(problemMessage(e, "Không lưu được bản nháp."));
      return false;
    }
  }, [documentId, section.id, doc, version]);

  useEffect(() => {
    if (!dirty) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, save]);

  const done = async () => {
    setBusy(true);
    if (dirty && !(await save())) {
      setBusy(false);
      return;
    }
    try {
      await finishSection(documentId, section.id);
      onChanged();
      onClose();
    } catch (e) {
      setError(problemMessage(e, "Không đánh dấu Xong được."));
      setBusy(false);
    }
  };

  return (
    <div role="dialog" aria-modal="true" aria-label={`Soạn ${section.title}`} className="fixed inset-0 z-50 flex flex-col bg-white">
      <div className="flex items-center gap-3 border-b border-line px-6 py-3">
        <h2 className="text-[16px] font-bold text-ink">{section.title}</h2>
        <Pill tone="info">Bạn đang giữ</Pill>
        <span className="text-xs text-muted">
          {dirty ? "Chưa lưu" : savedAt ? `Tự lưu lúc ${formatDateTime(savedAt).slice(-5)}` : "Chưa có bản nháp"} · mỗi 10 giây
        </span>
        <span className="ml-auto flex gap-2">
          <Button onClick={() => void (dirty ? save().then(onClose) : onClose())} disabled={busy}>
            Đóng
          </Button>
          <Button variant="primary" icon="check" onClick={() => void done()} disabled={busy}>
            Xong
          </Button>
        </span>
      </div>
      {error && (
        <Alert tone="danger" role="alert" className="mx-6 mt-3">
          {error}
        </Alert>
      )}
      <div className="min-h-0 flex-1 overflow-y-auto bg-shell px-6 py-4">
        <DocumentEditor
          document={doc}
          mode="student"
          diagramEnabled={diagramEnabled}
          onChange={(next) => {
            setDoc(next);
            setDirty(true);
          }}
          header={
            part.guide.length > 0 && (
              <div className="mb-3 rounded-xl bg-shell p-3">
                <p className="mb-1 text-xs font-semibold text-muted">Hướng dẫn của giảng viên cho mục này</p>
                <DocumentViewer document={{ blocks: part.guide }} />
              </div>
            )
          }
        />
      </div>
    </div>
  );
}

/** Trưởng nhóm giao mục (s18f, BR-U12-33): mục đang do người khác giữ thì cảnh báo nhả trước. */
export function AssignSectionDialog({
  view,
  section,
  onClose,
  onChanged,
}: {
  view: WorkspaceView;
  section: SectionView;
  onClose: () => void;
  onChanged: () => void;
}) {
  const [assignee, setAssignee] = useState("");
  const [error, setError] = useState<string | null>(null);
  const heldByOther = section.status === "CLAIMED";
  const holder = view.members.find((m) => m.accountId === section.holderId);
  const assign = async () => {
    try {
      await assignSection(view.documentId, section.id, assignee, heldByOther);
      onChanged();
      onClose();
    } catch (e) {
      setError(problemMessage(e, "Không giao được mục."));
    }
  };
  return (
    <Dialog
      open
      title={`Giao mục ${section.title}`}
      icon="person_add"
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose}>Hủy</Button>
          <Button variant="primary" onClick={() => void assign()} disabled={!assignee}>
            Giao mục
          </Button>
        </>
      }
    >
      <Select aria-label="Thành viên nhận mục" value={assignee} onChange={(e) => setAssignee(e.target.value)}>
        <option value="">Chọn thành viên</option>
        {view.members.filter((m) => m.accountId !== section.holderId).map((m) => (
          <option key={m.accountId} value={m.accountId}>
            {m.name ?? m.accountId}
          </option>
        ))}
      </Select>
      {heldByOther && (
        <Alert tone="warning">
          {holder?.name ?? "Một thành viên"} đang giữ mục này. Giao cho người khác sẽ nhả mục trước; bản nháp chưa Xong của
          người đang giữ sẽ bị bỏ.
        </Alert>
      )}
      <p className="text-xs text-muted">Thành viên cũng có thể tự nhận mục.</p>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Dialog>
  );
}

/** Nộp tài liệu nhóm (s18c, BR-U12-40): chỉ trưởng nhóm; mục chưa Xong không vào bản nộp. */
export function SubmitGroupDialog({
  view,
  onClose,
  onSubmitted,
}: {
  view: WorkspaceView;
  onClose: () => void;
  onSubmitted: (receipt: Receipt) => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [issues, setIssues] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const undone = view.sections.filter((s) => s.status !== "DONE");
  const name = (id: string | null) => view.members.find((m) => m.accountId === id)?.name ?? "Chưa ai nhận";
  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      onSubmitted(await submitDocument(view.documentId, view.version));
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: { message: string }[] }) : undefined;
      setIssues(problem?.issues?.map((i) => i.message) ?? []);
      setError(problemMessage(e, "Không nộp được, thử lại sau."));
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title="Nộp tài liệu nhóm?"
      icon="upload_file"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Quay lại
          </Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {undone.length ? "Vẫn nộp" : "Nộp"}
          </Button>
        </>
      }
    >
      {undone.length > 0 && (
        <>
          <Alert tone="warning">Chỉ nội dung của mục Xong được đưa vào bản nộp.</Alert>
          <TableFrame>
            <thead>
              <tr>
                <Th>Mục</Th>
                <Th>Người giữ</Th>
                <Th>Trạng thái</Th>
              </tr>
            </thead>
            <tbody>
              {undone.map((s) => (
                <tr key={s.id}>
                  <Td>{s.title}</Td>
                  <Td>{name(s.holderId)}</Td>
                  <Td>{s.status === "CLAIMED" ? "Đang làm" : "Mở"}</Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </>
      )}
      <p className="text-xs text-muted">Bạn có thể nộp lại trước hạn; mỗi lần nộp tạo một biên nhận mới.</p>
      {error && (
        <Alert tone="danger" role="alert" title={error}>
          {issues.length > 0 && (
            <ul className="list-disc pl-4">
              {issues.map((i) => (
                <li key={i}>{i}</li>
              ))}
            </ul>
          )}
        </Alert>
      )}
    </Dialog>
  );
}
