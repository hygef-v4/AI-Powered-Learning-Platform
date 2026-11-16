"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, PageHeader, Pill, Section, Textarea } from "@/shared/ui";
import { activateQuestion, approveQuestion, createQuestion, deleteQuestion, getQuestion, openDraft, rejectQuestion, retireQuestion, updateQuestion } from "../api";
import { readErrorCode, describeError, STATUS_LABELS, STATUS_TONES } from "../model/labels";
import { McqEditor } from "../components/McqEditor";
import { QuestionMetaPanel, QuestionVersionPanel } from "../components/QuestionSidePanels";
import { SimilarQuestionsAlert } from "../components/SimilarQuestionsAlert";
import { createEmptyForm, buildFormFrom, convertToContent, validateForm, type FormErrors, type QuestionForm } from "../model/questionForm";
import type { QuestionDetail, Violation } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

type Props = { id: string; subjectId?: string | null };
type Confirm = "retire" | "delete" | "reject" | null;

const listHref = (subjectId?: string | null) => `/manager/questions${subjectId ? `?subjectId=${encodeURIComponent(subjectId)}` : ""}`;

/**
 * Màn #20 Question Detail (UC 46, 47): tạo câu (id "new"), sửa bản nháp, sửa câu đang dùng bằng bản nháp mới
 * (BR-U10-04), kích hoạt, ngưng, xóa nháp (BR-U10-05); duyệt hoặc từ chối câu giảng viên đề xuất (BR-U10-09).
 */
export function QuestionDetailPage({ id, subjectId }: Props) {
  const router = useRouter();
  const creating = id === "new";
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [form, setForm] = useState<QuestionForm>(createEmptyForm);
  const [errors, setErrors] = useState<FormErrors>({});
  const [violations, setViolations] = useState<Violation[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notice, setNotice] = useState<{ tone: "success" | "danger" | "warning"; text: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<Confirm>(null);
  const [reason, setReason] = useState("");

  const show = useCallback((next: QuestionDetail) => {
    setDetail(next);
    setForm(buildFormFrom(next.question));
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
        if (active) setLoadError(describeError(error));
      });
    return () => {
      active = false;
    };
  }, [creating, id, reloadKey, show]);

  const question = detail?.question;
  const ownerSubject = question?.subjectId ?? subjectId ?? null;
  const dirty = useMemo(() => !question || JSON.stringify(form) !== JSON.stringify(buildFormFrom(question)), [form, question]);
  const pending = question?.status === "PENDING";
  const rejected = question?.status === "REJECTED";
  const readOnly = question?.status === "RETIRED" || pending || rejected;

  const fail = (error: unknown) => {
    if (error instanceof ApiError && error.problem.code === "QUESTION_INVALID") {
      setViolations((error.problem as { violations?: Violation[] }).violations ?? []);
      setNotice({ tone: "danger", text: msg("quizzes.pages.QuestionDetailPage.noiDungCauHoiChuaHop") });
      return;
    }
    setNotice({ tone: "danger", text: describeError(error) });
    if (readErrorCode(error) === "QUESTION_VERSION_CONFLICT") reload();
  };

  const runAction = async (action: () => Promise<void>) => {
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

  const saveQuestion = () => {
    const found = validateForm(form);
    setErrors(found);
    setViolations([]);
    if (Object.keys(found).length > 0) return;
    const content = convertToContent(form);
    void runAction(async () => {
      if (creating) {
        if (!subjectId) throw new Error("missing subject");
        const created = await createQuestion(subjectId, content);
        router.replace(`/manager/questions/${created.question.id}`);
        return;
      }
      if (!question) return;
      if (question.status === "DRAFT") {
        show(await updateQuestion(question.id, content, question.version));
        setNotice({ tone: "success", text: msg("quizzes.pages.QuestionDetailPage.daLuuBanNhap") });
        return;
      }
      // Câu đang dùng: mở bản nháp mới rồi lưu nội dung vào đó; quiz đã phát hành vẫn dùng phiên bản đã ghim.
      const draft = await openDraft(question.id);
      await updateQuestion(draft.question.id, content, draft.question.version);
      router.push(`/manager/questions/${draft.question.id}`);
    });
  };

  const activate = () => question && void runAction(async () => {
    show(await activateQuestion(question.id, question.version));
    setNotice({ tone: "success", text: msg("quizzes.pages.QuestionDetailPage.daKichHoatCauHoi") });
  });

  const approve = () => question && void runAction(async () => {
    show(await approveQuestion(question.id, question.version));
    setNotice({ tone: "success", text: msg("quizzes.pages.QuestionDetailPage.daDuyetCauDaVaoNgan") });
  });

  const confirmAction = () => question && void runAction(async () => {
    if (confirm === "retire") {
      const next = await retireQuestion(question.id, question.version);
      show(next);
      setNotice(next.quizzesShortOfQuestions?.length
        ? { tone: "warning", text: msg("quizzes.pages.QuestionDetailPage.daNgungDungCauHoiCac", { value: String(next.quizzesShortOfQuestions.join(", ")) }) }
        : { tone: "success", text: msg("quizzes.pages.QuestionDetailPage.daNgungDungCauHoi") });
    } else if (confirm === "reject") {
      show(await rejectQuestion(question.id, reason.trim(), question.version));
      setReason("");
      setNotice({ tone: "success", text: msg("quizzes.pages.QuestionDetailPage.daTuChoiVaBaoCho") });
    } else if (confirm === "delete") {
      await deleteQuestion(question.id, question.version);
      router.push(listHref(question.subjectId));
    }
    setConfirm(null);
  });

  if (creating && !subjectId) {
    return <Alert tone="warning" title={msg("quizzes.pages.QuestionDetailPage.chuaChonMon")}>{msg("quizzes.pages.QuestionDetailPage.hayMoNganHangCauHoi")}</Alert>;
  }
  if (loadError) {
    return <PageLoadError onRetry={reload}>{loadError}</PageLoadError>;
  }
  if (!creating && !question) return <PageLoading label={msg("quizzes.pages.QuestionDetailPage.dangTaiCauHoi")} />;

  return (
    <>
      <PageHeader
        backHref={listHref(ownerSubject)}
        breadcrumb={[{ label: msg("quizzes.pages.QuestionDetailPage.nganHangCauHoi"), href: listHref(ownerSubject) }, { label: creating ? msg("quizzes.pages.QuestionDetailPage.cauHoiMoi") : msg("quizzes.pages.QuestionDetailPage.cauHoi") }]}
        title={creating ? msg("quizzes.pages.QuestionDetailPage.cauHoiMoi") : msg("quizzes.pages.QuestionDetailPage.cauHoi")}
        subtitle={question && <span className="inline-flex items-center gap-2">
          <Pill tone={STATUS_TONES[question.status]}>{STATUS_LABELS[question.status]}</Pill>{msg("quizzes.pages.QuestionDetailPage.phienBan")}{question.versionNo}
        </span>}
        actions={<div className="flex flex-wrap gap-2">
          {question?.status === "DRAFT" && <Button variant="ghost" disabled={busy} onClick={() => setConfirm("delete")} data-testid="question-detail-delete">{msg("quizzes.pages.QuestionDetailPage.xoa")}</Button>}
          {question?.status === "ACTIVE" && <Button disabled={busy} onClick={() => setConfirm("retire")} data-testid="question-detail-retire">{msg("quizzes.pages.QuestionDetailPage.ngung")}</Button>}
          {question?.status === "DRAFT" && (
            <Button disabled={busy || dirty} title={dirty ? msg("quizzes.pages.QuestionDetailPage.luuThayDoiTruocKhiKich") : undefined} onClick={activate} data-testid="question-detail-activate">{msg("quizzes.pages.QuestionDetailPage.kichHoat")}</Button>
          )}
          {pending && <Button disabled={busy} onClick={() => setConfirm("reject")} data-testid="question-detail-reject">{msg("quizzes.pages.QuestionDetailPage.tuChoi")}</Button>}
          {pending && <Button variant="primary" disabled={busy} onClick={approve} data-testid="question-detail-approve">{msg("quizzes.pages.QuestionDetailPage.duyetVaoNganHang")}</Button>}
          {!readOnly && <Button variant="primary" icon="save" disabled={busy || !dirty} onClick={saveQuestion} data-testid="question-detail-save">{msg("quizzes.pages.QuestionDetailPage.luuThayDoi")}</Button>}
        </div>}
      />
      {notice && <Alert tone={notice.tone} role={notice.tone === "danger" ? "alert" : "status"}>{notice.text}</Alert>}
      {violations.length > 0 && (
        <Alert tone="danger">
          <ul className="list-disc pl-4">{violations.map((v, i) => <li key={i}>{v.message}</li>)}</ul>
        </Alert>
      )}
      {question?.status === "ACTIVE" && (
        <Alert tone="info">{msg("quizzes.pages.QuestionDetailPage.dangDungTrong")}{detail?.usedInQuizzes ?? 0} quiz.</Alert>
      )}
      {question?.status === "RETIRED" && <Alert tone="warning">{msg("quizzes.pages.QuestionDetailPage.cauDaNgungDung")}</Alert>}
      {pending && <Alert tone="info">{msg("quizzes.pages.QuestionDetailPage.cauDoGiangVienDeXuat")}</Alert>}
      {rejected && <Alert tone="warning" title={msg("quizzes.pages.QuestionDetailPage.daTuChoi")}>{detail?.review?.reason ?? msg("quizzes.pages.QuestionDetailPage.khongCoLyDo")}</Alert>}
      {detail && <SimilarQuestionsAlert similar={detail.similar ?? []} />}
      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Section title={msg("quizzes.pages.QuestionDetailPage.noiDung")}>
          <McqEditor value={form} onChange={setForm} errors={errors} readOnly={readOnly} />
        </Section>
        <div className="flex flex-col gap-4">
          <QuestionMetaPanel value={form} onChange={setForm} errors={errors} readOnly={readOnly} />
          {detail && <QuestionVersionPanel detail={detail} />}
        </div>
      </div>
      <Dialog open={confirm !== null} onClose={() => setConfirm(null)} tone={confirm === "delete" ? "danger" : "warning"}
        title={confirm === "delete" ? msg("quizzes.pages.QuestionDetailPage.xoaCauNhap") : confirm === "reject" ? msg("quizzes.pages.QuestionDetailPage.tuChoiCauDeXuat") : msg("quizzes.pages.QuestionDetailPage.ngungDungCauHoi")}
        footer={<>
          <Button variant={confirm === "delete" ? "danger" : "primary"} disabled={busy || (confirm === "reject" && !reason.trim())} onClick={confirmAction} data-testid="question-detail-confirm">
            {confirm === "delete" ? msg("quizzes.pages.QuestionDetailPage.xoa") : confirm === "reject" ? msg("quizzes.pages.QuestionDetailPage.tuChoi") : msg("quizzes.pages.QuestionDetailPage.ngungDung")}
          </Button>
        </>}>
        {confirm === "reject" ? (
          <Field label={msg("quizzes.pages.QuestionDetailPage.lyDoTuChoi")} required htmlFor="question-reject-reason" hint={msg("quizzes.pages.QuestionDetailPage.giangVienThayLyDoNay")}>
            <Textarea id="question-reject-reason" rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} data-testid="question-detail-reason" />
          </Field>
        ) : <p>{confirm === "delete" ? msg("quizzes.pages.QuestionDetailPage.banCoChacMuonXoaCau") : msg("quizzes.pages.QuestionDetailPage.banCoChacMuonNgungDung")}</p>}
      </Dialog>
    </>
  );
}
