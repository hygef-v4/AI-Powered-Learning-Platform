"use client";

import { useId, useRef, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, Select, TextInput } from "@/shared/ui";
import { describeError } from "../model/labels";
import type { QuizDetail, QuizRow } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = {
  onClose: () => void;
  onCreate: (title: string) => Promise<QuizDetail>;
  onCreated: (quiz: QuizDetail) => void;
  /** Quiz của lớp còn copy được từ lớp khác cùng môn: chọn một quiz thì bỏ qua ô tên và copy luôn. */
  copy?: { load: () => Promise<QuizRow[]>; run: (sourceId: string) => Promise<QuizDetail> };
};

/** Popup "Quiz mới" (F6, F11) gộp một form: nhập tên để tạo quiz trống, hoặc chọn quiz ở lớp khác để copy. */
export function CreateQuizDialog({ onClose, onCreate, onCreated, copy }: Props) {
  const id = useId();
  const [title, setTitle] = useState("");
  const [sources, setSources] = useState<QuizRow[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const loading = useRef(false);
  const copying = !!sourceId;
  const invalid = !title.trim() || title.trim().length > 200;

  const loadSources = () => {
    if (!copy || sources || loading.current) return;
    loading.current = true;
    copy.load().then(setSources, (failure: unknown) => setError(describeError(failure))).finally(() => { loading.current = false; });
  };

  const submit = async () => {
    if (!copying && invalid) {
      setError(msg("quizzes.components.CreateQuizDialog.tenQuizTu1Den200"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onCreated(copying && copy ? await copy.run(sourceId) : await onCreate(title.trim()));
    } catch (failure) {
      setError(describeError(failure));
      setBusy(false);
    }
  };

  return (
    <Dialog open onClose={onClose} dismissible={!busy} title={msg("quizzes.components.CreateQuizDialog.quizMoi")}
      footer={
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="create-quiz-submit">
          {copying ? "Copy quiz" : msg("quizzes.components.CreateQuizDialog.taoQuiz")}
          {!busy && <Icon name="arrow_forward" size={15} />}
        </Button>
      }>
      <div className={copying ? "opacity-45 transition-opacity" : "transition-opacity"}>
        <Field label={msg("quizzes.components.CreateQuizDialog.tenQuiz")} htmlFor={`${id}-title`} required={!copying}>
          <TextInput id={`${id}-title`} value={title} maxLength={200} disabled={copying} placeholder={msg("quizzes.components.CreateQuizDialog.onTapChuong3")}
            onChange={(event) => setTitle(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") void submit(); }} data-testid="create-quiz-title" />
        </Field>
      </div>
      {copy && (
        <Select aria-label={msg("quizzes.components.CreateQuizDialog.copyQuizTuLopKhac")} value={sourceId} disabled={busy} data-testid="class-quizzes-copy"
          onFocus={loadSources} onMouseDown={loadSources} onChange={(event) => setSourceId(event.target.value)}>
          <option value="">{sources && sources.length === 0 ? msg("quizzes.components.CreateQuizDialog.khongCoQuizOLopKhac") : msg("quizzes.components.CreateQuizDialog.copyTuLopKhac")}</option>
          {sources?.map((source) => <option key={source.id} value={source.id}>{source.title} · {source.itemCount} {msg("quizzes.components.CreateQuizDialog.cau")}</option>)}
        </Select>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
