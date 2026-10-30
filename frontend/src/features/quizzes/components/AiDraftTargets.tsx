"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Alert, Button, Pill, Section } from "@/shared/ui";
import {
  acceptBankDraft, acceptQuizDraft, discardBankDraft, discardQuizDraft, getBankDraft, getBankDrafts, getQuizDraft, getQuizDrafts,
  getQuizMaterials, getSubjectMaterials, requestBankDraft, requestQuizDraft,
} from "../api";
import { AiDraftDialog, type DraftTargetApi } from "./AiDrafts";
import { errorMessage, failureLabel, formatDateTime } from "../model/labels";
import type { AiDraft, AiDraftStatus, QuizDetail, SelectableMaterial } from "../model/types";

const STATUS_LABELS: Record<AiDraftStatus, string> = {
  PENDING: "AI đang soạn", READY: "Chờ chấp nhận", ACCEPTED: "Đã chấp nhận", DISCARDED: "Đã bỏ", FAILED: "Lỗi",
};
const STATUS_TONES = { PENDING: "info", READY: "warning", ACCEPTED: "success", DISCARDED: "neutral", FAILED: "danger" } as const;

/** Popup AI của Question List (F4): câu chấp nhận vào ngân hàng ở trạng thái Nháp, không tự kích hoạt. */
export function QuestionAiDraftDialog({ subjectId, onClose, onAccepted }: {
  subjectId: string;
  onClose: () => void;
  onAccepted: (count: number) => void;
}) {
  const api = useMemo<DraftTargetApi>(() => ({
    title: "Tạo nháp bằng AI",
    taskLabel: "Sinh câu hỏi · Ngân hàng câu hỏi của môn",
    acceptNote: "Câu được chấp nhận vào ngân hàng ở trạng thái Nháp; bạn kiểm tra lại trước khi kích hoạt.",
    loadMaterials: (signal) => getSubjectMaterials(subjectId, signal),
    list: (signal) => getBankDrafts(subjectId, signal),
    request: (input) => requestBankDraft(subjectId, input),
    get: (draftId) => getBankDraft(subjectId, draftId),
    accept: async (draftId, questions) => {
      await acceptBankDraft(subjectId, draftId, questions);
    },
    discard: (draftId) => discardBankDraft(subjectId, draftId),
  }), [subjectId]);
  return <AiDraftDialog api={api} onClose={onClose} onAccepted={onAccepted} />;
}

/**
 * "Đề xuất AI" trên Quiz Detail (F8, F13): đề xuất từ AI hoặc agent MCP chờ người soạn; chấp nhận thì câu thành câu riêng ở
 * cuối quiz, gửi kèm `version` mới nhất của quiz.
 */
export function QuizAiDraftPanel({ detail, onQuizChange, onConflict, disabled, openRequest = 0 }: {
  detail: QuizDetail;
  onQuizChange: (next: QuizDetail) => void;
  onConflict: () => void;
  disabled: boolean;
  /** Tăng lên mỗi lần chọn "Soạn bằng AI" trong nút Thêm câu hỏi của trang quiz: mở popup AI mới. */
  openRequest?: number;
}) {
  const quizId = detail.quiz.id;
  const quizTitle = detail.quiz.title;
  const version = useRef(detail.quiz.version);
  useEffect(() => {
    version.current = detail.quiz.version;
  }, [detail.quiz.version]);
  const [drafts, setDrafts] = useState<AiDraft[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<{ draftId: string | null } | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    getQuizDrafts(quizId, controller.signal)
      .then((list) => {
        if (controller.signal.aborted) return;
        setError(null);
        setDrafts(list);
      })
      .catch((failure: unknown) => !controller.signal.aborted && setError(errorMessage(failure)));
    return () => controller.abort();
  }, [quizId, reloadKey]);

  const api = useMemo<DraftTargetApi>(() => ({
    title: "Tạo nháp bằng AI",
    taskLabel: `Sinh câu hỏi · ${quizTitle}`,
    acceptNote: "Câu được chấp nhận thành câu riêng ở cuối quiz; AI không tự duyệt hay phát hành quiz.",
    loadMaterials: (signal) => getQuizMaterials(quizId, signal),
    list: (signal) => getQuizDrafts(quizId, signal),
    request: (input) => requestQuizDraft(quizId, input, version.current),
    get: (draftId) => getQuizDraft(quizId, draftId),
    accept: async (draftId, questions) => {
      try {
        onQuizChange(await acceptQuizDraft(quizId, draftId, questions, version.current));
      } catch (failure) {
        if ((failure as { problem?: { code?: string } }).problem?.code === "QUIZ_VERSION_CONFLICT") onConflict();
        throw failure;
      }
    },
    discard: (draftId) => discardQuizDraft(quizId, draftId),
  }), [quizId, quizTitle, onQuizChange, onConflict]);

  const close = () => {
    setOpen(null);
    setReloadKey((k) => k + 1);
  };
  // Mở popup AI khi trang quiz yêu cầu (chọn "Soạn bằng AI"); giá trị 0 là chưa yêu cầu.
  const [handled, setHandled] = useState(openRequest);
  if (openRequest !== handled) {
    setHandled(openRequest);
    if (openRequest > 0) setOpen({ draftId: null });
  }

  // Không có đề xuất nào thì không chiếm chỗ trên trang; lối tạo mới nằm trong nút Thêm câu hỏi.
  if (!error && !open && (drafts === null || drafts.length === 0)) return null;

  return (
    <Section title="Đề xuất AI">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {drafts && drafts.length > 0 && (
        <ul className="flex flex-col gap-2" data-testid="quiz-ai-draft-list">
          {drafts.map((draft) => (
            <li key={draft.id} className="flex flex-wrap items-center gap-2 text-[12.5px]">
              <Pill tone={STATUS_TONES[draft.status]}>{STATUS_LABELS[draft.status]}</Pill>
              <span>{draft.origin === "MCP" ? `Agent: ${draft.agentLabel ?? "Agent"}` : "AI"}</span>
              <span className="text-muted">
                {draft.status === "FAILED" ? failureLabel(draft.failureCode, draft.failureMessage)
                  : `${draft.questions.length || draft.requestedCount || 0} câu · ${formatDateTime(draft.createdAt)}`}
              </span>
              <Button size="sm" className="ml-auto" disabled={disabled} onClick={() => setOpen({ draftId: draft.id })}
                data-testid="quiz-ai-draft-view">Xem</Button>
            </li>
          ))}
        </ul>
      )}
      {open && (
        <AiDraftDialog api={api} initialDraftId={open.draftId} onClose={close} onAccepted={close} />
      )}
    </Section>
  );
}

/**
 * Gắn quiz với học liệu (BR-U10-11, H8): tối đa 10 tài liệu của lớp hoặc môn chứa quiz. Tài liệu đã gắn nhưng không còn
 * trong danh sách vẫn hiện để bỏ gắn.
 */
export function MaterialRefPicker({ quizId, value, onChange, readOnly }: {
  quizId: string;
  value: string[];
  onChange: (value: string[]) => void;
  readOnly: boolean;
}) {
  const [materials, setMaterials] = useState<SelectableMaterial[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getQuizMaterials(quizId, controller.signal)
      .then((list) => !controller.signal.aborted && setMaterials(list))
      .catch(() => !controller.signal.aborted && setFailed(true));
    return () => controller.abort();
  }, [quizId]);

  const known = new Set(materials?.map((m) => m.materialId));
  const missing = value.filter((id) => materials && !known.has(id));
  const toggle = (id: string) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id]);
  const shown = readOnly ? (materials ?? []).filter((m) => value.includes(m.materialId)) : materials ?? [];

  return (
    <div className="flex flex-col gap-1.5" data-testid="material-ref-picker">
      <p className="text-xs font-semibold text-body">Gắn với học liệu <span className="font-normal text-muted">· tối đa 10</span></p>
      {failed && <p className="text-[11px] text-muted">Chưa tải được danh sách học liệu; các học liệu đã gắn vẫn được giữ.</p>}
      {materials && shown.length === 0 && missing.length === 0 && (
        <p className="text-[11px] text-muted">{readOnly ? "Chưa gắn học liệu." : "Lớp hoặc môn chưa có học liệu."}</p>
      )}
      <ul className="flex flex-col gap-1">
        {shown.map((m) => {
          const checked = value.includes(m.materialId);
          return (
            <li key={m.materialId}>
              <label className="inline-flex items-center gap-2 text-[13px]">
                <input type="checkbox" checked={checked} disabled={readOnly || (!checked && value.length >= 10)}
                  onChange={() => toggle(m.materialId)} data-testid="material-ref-option" />
                {m.title}
              </label>
            </li>
          );
        })}
        {missing.map((id) => (
          <li key={id}>
            <label className="inline-flex items-center gap-2 text-[13px] text-muted">
              <input type="checkbox" checked disabled={readOnly} onChange={() => toggle(id)} />
              Học liệu không còn truy cập được
            </label>
          </li>
        ))}
      </ul>
    </div>
  );
}
