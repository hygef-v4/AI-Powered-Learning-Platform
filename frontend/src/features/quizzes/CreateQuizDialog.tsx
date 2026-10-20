"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, TextInput } from "@/shared/ui";
import { errorMessage } from "./labels";
import type { QuizDetail } from "./types";

type Props = { onClose: () => void; onCreate: (title: string) => Promise<QuizDetail>; onCreated: (quiz: QuizDetail) => void };

/** Popup "Tạo quiz" (F6): nhập tên rồi mở Quiz Detail; cài đặt mặc định do server đặt. */
export function CreateQuizDialog({ onClose, onCreate, onCreated }: Props) {
  const id = useId();
  const [title, setTitle] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const invalid = !title.trim() || title.trim().length > 200;

  const submit = async () => {
    if (invalid) {
      setError("Tên quiz từ 1 đến 200 ký tự.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onCreated(await onCreate(title.trim()));
    } catch (failure) {
      setError(errorMessage(failure));
      setBusy(false);
    }
  };

  return (
    <Dialog open onClose={onClose} dismissible={!busy} icon="quiz" title="Tạo quiz"
      footer={<>
        <Button onClick={onClose} disabled={busy}>Hủy</Button>
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="create-quiz-submit">Tạo quiz</Button>
      </>}>
      <div className="flex flex-col gap-3">
        <Field label="Tên quiz" htmlFor={`${id}-title`} required>
          <TextInput id={`${id}-title`} value={title} maxLength={200} onChange={(event) => setTitle(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") void submit(); }} data-testid="create-quiz-title" />
        </Field>
        <p className="text-[13px] text-muted">Quiz chỉ dùng để luyện tập (Practice): không có lịch, không vào sổ điểm.</p>
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
      </div>
    </Dialog>
  );
}
