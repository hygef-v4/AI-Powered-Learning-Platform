"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, PageHeader, Pill, Section } from "@/shared/ui";
import { activateQuestion, createQuestion, deleteQuestion, getQuestion, openDraft, retireQuestion, updateQuestion } from "./api";
import { errorCode, errorMessage, STATUS_LABELS, STATUS_TONES } from "./labels";
import { McqEditor } from "./McqEditor";
import { QuestionMetaPanel, QuestionVersionPanel } from "./QuestionSidePanels";
import { emptyForm, formOf, toContent, validateForm, type FormErrors, type QuestionForm } from "./questionForm";
import type { QuestionDetail, Violation } from "./types";

type Props = { id: string; subjectId?: string | null };
type Confirm = "retire" | "delete" | null;

const listHref = (subjectId?: string | null) => `/manager/questions${subjectId ? `?subjectId=${encodeURIComponent(subjectId)}` : ""}`;

/**
 * Màn #20 Question Detail (UC 46, 47): tạo câu (id "new"), sửa bản nháp, sửa câu đang dùng bằng bản nháp mới
 * (BR-U10-04), kích hoạt, ngưng, xóa nháp (BR-U10-05).
 */
export function QuestionDetailPage({ id, subjectId }: Props) {
  const router = useRouter();
  const creating = id === "new";
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [form, setForm] = useState<QuestionForm>(emptyForm);
  const [errors, setErrors] = useState<FormErrors>({});
  const [violations, setViolations] = useState<Violation[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notice, setNotice] = useState<{ tone: "success" | "danger" | "warning"; text: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<Confirm>(null);

  const show = useCallback((next: QuestionDetail) => {
    setDetail(next);
    setForm(formOf(next.question));
    setErrors({});
    setViolations([]);
  }, []);

  const [reloadKey, setReloadKey] = useState(0);
  const reload = () => setReloadKey((key) => key + 1);

  useEffect(() => {
    if (creating) return;
    let active = true;
    getQuestion(id)
      .then((next) => {
        if (!active) return;
        setLoadError(null);
        show(next);
      })
      .catch((error: unknown) => {
        if (active) setLoadError(errorMessage(error));
      });
    return () => {
      active = false;
    };
  }, [creating, id, reloadKey, show]);

  const question = detail?.question;
  const ownerSubject = question?.subjectId ?? subjectId ?? null;
  const dirty = useMemo(() => !question || JSON.stringify(form) !== JSON.stringify(formOf(question)), [form, question]);
  const readOnly = question?.status === "RETIRED";

  const fail = (error: unknown) => {
    if (error instanceof ApiError && error.problem.code === "QUESTION_INVALID") {
      setViolations((error.problem as { violations?: Violation[] }).violations ?? []);
      setNotice({ tone: "danger", text: "Nội dung câu hỏi chưa hợp lệ." });
      return;
    }
    setNotice({ tone: "danger", text: errorMessage(error) });
    if (errorCode(error) === "QUESTION_VERSION_CONFLICT") reload();
  };

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setNotice(null);
    try {
      await action();
    } catch (error) {
      fail(error);
    } finally {
      setBusy(false);
    }
  };

  const save = () => {
    const found = validateForm(form);
    setErrors(found);
    setViolations([]);
    if (Object.keys(found).length > 0) return;
    const content = toContent(form);
    void run(async () => {
      if (creating) {
        if (!subjectId) throw new Error("missing subject");
        const created = await createQuestion(subjectId, content);
        router.replace(`/manager/questions/${created.question.id}`);
        return;
      }
      if (!question) return;
      if (question.status === "DRAFT") {
        show(await updateQuestion(question.id, content, question.version));
        setNotice({ tone: "success", text: "Đã lưu bản nháp." });
        return;
      }
      // Câu đang dùng: mở bản nháp mới rồi lưu nội dung vào đó; quiz đã phát hành vẫn dùng phiên bản đã ghim.
      const draft = await openDraft(question.id);
      await updateQuestion(draft.question.id, content, draft.question.version);
      router.push(`/manager/questions/${draft.question.id}`);
    });
  };

  const activate = () => question && void run(async () => {
    show(await activateQuestion(question.id, question.version));
    setNotice({ tone: "success", text: "Đã kích hoạt câu hỏi." });
  });

  const confirmAction = () => question && void run(async () => {
    if (confirm === "retire") {
      show(await retireQuestion(question.id, question.version));
      setNotice({ tone: "success", text: "Đã ngưng dùng câu hỏi." });
    } else if (confirm === "delete") {
      await deleteQuestion(question.id, question.version);
      router.push(listHref(question.subjectId));
    }
    setConfirm(null);
  });

  if (creating && !subjectId) {
    return <Alert tone="warning" title="Chưa chọn môn">Hãy mở Ngân hàng câu hỏi và chọn môn trước khi thêm câu.</Alert>;
  }
  if (loadError) {
    return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={reload}>Thử lại</Button>}>{loadError}</Alert>;
  }
  if (!creating && !question) return <p role="status" className="py-10 text-center text-sm text-muted">Đang tải câu hỏi…</p>;

  return (
    <>
      <PageHeader
        backHref={listHref(ownerSubject)}
        breadcrumb={[{ label: "Câu hỏi", href: listHref(ownerSubject) }, { label: creating ? "Câu hỏi mới" : "Câu hỏi" }]}
        title={creating ? "Câu hỏi mới" : "Câu hỏi"}
        subtitle={question && <span className="inline-flex items-center gap-2">
          <Pill tone={STATUS_TONES[question.status]}>{STATUS_LABELS[question.status]}</Pill>Phiên bản {question.versionNo}
        </span>}
        actions={<div className="flex flex-wrap gap-2">
          {question?.status === "DRAFT" && <Button variant="ghost" disabled={busy} onClick={() => setConfirm("delete")} data-testid="question-detail-delete">Xóa</Button>}
          {question?.status === "ACTIVE" && <Button disabled={busy} onClick={() => setConfirm("retire")} data-testid="question-detail-retire">Ngưng</Button>}
          {question?.status === "DRAFT" && (
            <Button disabled={busy || dirty} title={dirty ? "Lưu thay đổi trước khi kích hoạt" : undefined} onClick={activate} data-testid="question-detail-activate">Kích hoạt</Button>
          )}
          {!readOnly && <Button variant="primary" icon="save" disabled={busy || !dirty} onClick={save} data-testid="question-detail-save">Lưu thay đổi</Button>}
        </div>}
      />
      {notice && <Alert tone={notice.tone} role={notice.tone === "danger" ? "alert" : "status"}>{notice.text}</Alert>}
      {violations.length > 0 && (
        <Alert tone="danger">
          <ul className="list-disc pl-4">{violations.map((v, i) => <li key={i}>{v.message}</li>)}</ul>
        </Alert>
      )}
      {question?.status === "ACTIVE" && (
        <Alert tone="info">Đang dùng trong {detail?.usedInQuizzes ?? 0} quiz.</Alert>
      )}
      {readOnly && <Alert tone="warning">Câu đã ngưng dùng.</Alert>}
      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Section title="Nội dung">
          <McqEditor value={form} onChange={setForm} errors={errors} readOnly={readOnly} />
        </Section>
        <div className="flex flex-col gap-4">
          <QuestionMetaPanel value={form} onChange={setForm} errors={errors} readOnly={readOnly} />
          {detail && <QuestionVersionPanel detail={detail} />}
        </div>
      </div>
      <Dialog open={confirm !== null} onClose={() => setConfirm(null)} tone={confirm === "delete" ? "danger" : "warning"}
        title={confirm === "delete" ? "Xóa câu nháp?" : "Ngưng dùng câu hỏi?"}
        footer={<>
          <Button variant={confirm === "delete" ? "danger" : "primary"} disabled={busy} onClick={confirmAction} data-testid="question-detail-confirm">
            {confirm === "delete" ? "Xóa" : "Ngưng dùng"}
          </Button>
        </>}>
      </Dialog>
    </>
  );
}
