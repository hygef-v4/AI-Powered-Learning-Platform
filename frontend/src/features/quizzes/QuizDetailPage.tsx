"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, Dialog, Field, Menu, MenuItem, PageHeader, Pill, Section } from "@/shared/ui";
import {
  addInlineQuestion, cloneQuiz, createQuizVersion, deleteQuiz, getQuiz, publishQuiz, retireQuiz, reviewQuiz, saveQuizItems,
  updateInlineQuestion, updateQuizDetails,
} from "./api";
import { QuizAiDraftPanel } from "./AiDraftTargets";
import { BankQuestionPicker } from "./BankQuestionPicker";
import { errorCode, errorMessage, formatDateTime, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "./labels";
import {
  InlineQuestionDialog, QuizItemList, QuizPreviewDialog, QuizSettingsPanel, settingsFormOf, settingsInput, type DraftItem,
  type SettingsForm,
} from "./QuizDetailParts";
import { quizDetailSections } from "./slots";
import type { QuizDetail, QuizItemView, Violation } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const MAX_ITEMS = 200;
type Dialogs = "picker" | "inline" | "preview" | "retire" | "delete" | null;

const draftItems = (detail: QuizDetail): DraftItem[] => detail.items.map((item) => ({ item, points: String(item.points) }));
const validPoints = (value: string) => /^\d+([.,]\d{1,2})?$/.test(value.trim()) && Number(value.replace(",", ".")) > 0
  && Number(value.replace(",", ".")) <= 9999.99;

/**
 * Màn #22 Quiz Detail (UC 48; F7, F9–F12): soạn câu và cấu hình khi quiz còn nháp, duyệt, phát hành, ngưng, nhân bản,
 * version mới, xóa. Giảng viên mở quiz của môn từ lớp (`classId`) thì chỉ xem.
 */
export function QuizDetailPage({ id, classId }: { id: string; classId?: string | null }) {
  const router = useRouter();
  const [detail, setDetail] = useState<QuizDetail | null>(null);
  const [items, setItems] = useState<DraftItem[]>([]);
  const [settings, setSettings] = useState<SettingsForm | null>(null);
  const [settingsErrors, setSettingsErrors] = useState<Record<string, string>>({});
  const [violations, setViolations] = useState<Violation[]>([]);
  const [notice, setNotice] = useState<{ tone: "success" | "danger" | "warning"; text: string } | null>(null);
  // Mỗi lần chọn "Soạn bằng AI" trong nút Thêm câu hỏi thì tăng để panel AI mở popup.
  const [aiRequest, setAiRequest] = useState(0);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dialog, setDialog] = useState<Dialogs>(null);
  const [editing, setEditing] = useState<QuizItemView | null>(null);
  const [reason, setReason] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  // Mọi lần nhận Quiz mới từ máy chủ (thêm câu, lưu, tải lại…) thì lỗi duyệt cũ không còn đúng nữa.
  const show = useCallback((next: QuizDetail) => {
    setDetail(next);
    setViolations([]);
    setItems(draftItems(next));
    setSettings(settingsFormOf(next.quiz));
    setSettingsErrors({});
  }, []);

  useEffect(() => {
    let active = true;
    getQuiz(id, classId)
      .then((next) => {
        if (!active) return;
        setLoadError(null);
        show(next);
      })
      .catch((failure: unknown) => active && setLoadError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [id, classId, reloadKey, show]);

  const quiz = detail?.quiz;
  const editable = !!detail?.canManage && (quiz?.status === "DRAFT" || quiz?.status === "REVIEWED");
  const itemsDirty = useMemo(() => !!detail && JSON.stringify(items.map((d) => [d.item.question.id, d.points]))
    !== JSON.stringify(draftItems(detail).map((d) => [d.item.question.id, d.points])), [items, detail]);
  const settingsDirty = useMemo(() => !!quiz && !!settings && JSON.stringify(settings) !== JSON.stringify(settingsFormOf(quiz)), [settings, quiz]);
  const dirty = itemsDirty || settingsDirty;

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setNotice(null);
    setViolations([]);
    try {
      await action();
    } catch (failure) {
      const problem = failure instanceof ApiError ? (failure.problem as { violations?: Violation[] }) : {};
      if (problem.violations?.length) setViolations(problem.violations);
      setNotice({ tone: "danger", text: errorMessage(failure) });
      if (errorCode(failure) === "QUIZ_VERSION_CONFLICT") setReloadKey((k) => k + 1);
    } finally {
      setBusy(false);
    }
  };

  // Kiểm cả cấu hình lẫn điểm trước khi gọi API; lỗi phía trình duyệt báo thẳng, không ném cho `run` (chỉ dịch lỗi API).
  const save = () => detail && settings && void run(async () => {
    const { errors, input } = settingsDirty ? settingsInput(settings) : { errors: {}, input: undefined };
    setSettingsErrors(errors);
    const invalid = settingsDirty && !input ? "Cấu hình chưa hợp lệ."
      : itemsDirty && items.some((d) => !validPoints(d.points)) ? "Điểm mỗi câu phải lớn hơn 0, tối đa 2 chữ số thập phân." : null;
    if (invalid) {
      setNotice({ tone: "danger", text: invalid });
      return;
    }
    let current = detail;
    if (input) current = await updateQuizDetails(current.quiz.id, input, current.quiz.version);
    if (itemsDirty) {
      current = await saveQuizItems(current.quiz.id, items.map((d) => ({ questionId: d.item.question.id,
        points: Number(d.points.replace(",", ".")) })), current.quiz.version);
    }
    show(current);
    setNotice({ tone: "success", text: "Đã lưu quiz." });
  });

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);
  const acceptedFromAi = useCallback((next: QuizDetail) => {
    show(next);
    setNotice({ tone: "success", text: "Đã thêm câu từ đề xuất AI vào cuối quiz." });
  }, [show]);

  const addFromBank = async (questionIds: string[]) => {
    if (!detail) return;
    const next = await saveQuizItems(detail.quiz.id, [
      ...detail.items.map((item) => ({ questionId: item.question.id, points: item.points })),
      ...questionIds.map((questionId) => ({ questionId, points: null })),
    ], detail.quiz.version);
    show(next);
    setDialog(null);
    setNotice({ tone: "success", text: `Đã thêm ${questionIds.length} câu từ ngân hàng.` });
  };

  const saveInline = async (content: Parameters<typeof addInlineQuestion>[1]) => {
    if (!detail) return;
    try {
      show(editing
        ? await updateInlineQuestion(detail.quiz.id, editing.question.id, content, detail.quiz.version)
        : await addInlineQuestion(detail.quiz.id, content, detail.quiz.version));
    } catch (failure) {
      throw new Error(errorMessage(failure));
    }
    setDialog(null);
    setEditing(null);
  };

  const lifecycle = (action: "review" | "publish") => quiz && void run(async () => {
    show(action === "review" ? await reviewQuiz(quiz.id, quiz.version) : await publishQuiz(quiz.id, quiz.version));
    setNotice({ tone: "success", text: action === "review" ? "Đã duyệt quiz." : "Đã phát hành quiz." });
  });

  const more = (value: string) => quiz && void run(async () => {
    if (value === "clone") router.push(`/quizzes/${(await cloneQuiz(quiz.id)).quiz.id}`);
    if (value === "version") router.push(`/quizzes/${(await createQuizVersion(quiz.id)).quiz.id}`);
    if (value === "retire") setDialog("retire");
    if (value === "delete") setDialog("delete");
  });

  if (loadError) {
    return <PageLoadError onRetry={() => setReloadKey((k) => k + 1)}>{loadError}</PageLoadError>;
  }
  if (!detail || !quiz || !settings) return <PageLoading label="Đang tải quiz…" />;

  const backHref = classId ? `/teaching/classes/${classId}` : quiz.scope === "SUBJECT"
    ? `/manager/quizzes?subjectId=${quiz.subjectId}` : `/teaching/classes/${quiz.classId}`;
  const free = MAX_ITEMS - detail.items.length;

  return (
    <>
      <PageHeader
        backHref={backHref}
        breadcrumb={[{ label: "Quiz", href: backHref }, { label: quiz.title }]}
        title={quiz.title}
        subtitle={<span className="inline-flex flex-wrap items-center gap-2">
          <Pill tone={QUIZ_STATUS_TONES[quiz.status]}>{QUIZ_STATUS_LABELS[quiz.status]}</Pill>
          Phiên bản {quiz.versionNo} · {quiz.scope === "SUBJECT" ? "Của môn" : "Của lớp"} · Đã lưu {formatDateTime(quiz.updatedAt)}
        </span>}
        actions={<div className="flex flex-wrap items-center gap-2">
          <Button icon="visibility" onClick={() => setDialog("preview")} data-testid="quiz-detail-preview">Xem trước</Button>
          {editable && <Button icon="save" disabled={busy || !dirty} onClick={save} data-testid="quiz-detail-save">Lưu</Button>}
          {detail.canManage && quiz.status === "DRAFT" && (
            <Button variant="primary" disabled={busy || dirty} title={dirty ? "Lưu thay đổi trước khi duyệt" : undefined}
              onClick={() => lifecycle("review")} data-testid="quiz-detail-review">Duyệt</Button>
          )}
          {detail.canManage && quiz.status === "REVIEWED" && (
            <Button variant="primary" disabled={busy || dirty} onClick={() => lifecycle("publish")} data-testid="quiz-detail-publish">Phát hành</Button>
          )}
          {detail.canManage && (
            <span data-testid="quiz-detail-more">
              <Menu align="right" icon="more_horiz" ariaLabel="Thao tác khác" label="">
                {quiz.status === "OPEN" && <MenuItem icon="block" disabled={busy} onClick={() => more("retire")}>Ngưng quiz</MenuItem>}
                <MenuItem icon="copy" disabled={busy} onClick={() => more("clone")}>Nhân bản</MenuItem>
                {(quiz.status === "OPEN" || quiz.status === "RETIRED") && <MenuItem icon="history" disabled={busy} onClick={() => more("version")}>Tạo version mới</MenuItem>}
                {quiz.publishedAt === null && editable && <MenuItem icon="trash" tone="danger" disabled={busy} onClick={() => more("delete")}>Xóa quiz</MenuItem>}
              </Menu>
            </span>
          )}
        </div>}
      />
      {notice && <Alert tone={notice.tone} role={notice.tone === "danger" ? "alert" : "status"}>{notice.text}</Alert>}
      {violations.length > 0 && (
        <Alert tone="danger" title="Cần sửa trước khi duyệt hoặc phát hành">
          <ul className="list-disc pl-4" data-testid="quiz-detail-violations">{violations.map((v, i) => <li key={i}>{v.message}</li>)}</ul>
        </Alert>
      )}
      {!detail.canManage && <Alert tone="info">Quiz của môn: giảng viên lớp chỉ xem.</Alert>}
      {quiz.status === "RETIRED" && quiz.retiredReason && <Alert tone="warning">Đã ngưng: {quiz.retiredReason}</Alert>}
      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <Section title={<>Câu hỏi <span className="font-mono text-xs font-medium text-muted">{items.length}</span></>}
          actions={editable && (
            <Menu align="right" icon="add" label="Thêm câu hỏi" ariaLabel="Thêm câu hỏi">
              <MenuItem icon="edit" disabled={busy || itemsDirty || free <= 0} onClick={() => { setEditing(null); setDialog("inline"); }}
                data-testid="quiz-detail-add-inline">Soạn câu mới</MenuItem>
              <MenuItem icon="help_center" disabled={busy || itemsDirty || free <= 0} onClick={() => setDialog("picker")}
                data-testid="quiz-detail-add-bank">Lấy từ ngân hàng</MenuItem>
              <MenuItem icon="sparkle" disabled={busy || dirty} onClick={() => setAiRequest((n) => n + 1)}
                data-testid="quiz-ai-draft-open">Soạn bằng AI</MenuItem>
            </Menu>
          )}>
          {itemsDirty && <p className="text-xs font-semibold text-warning">Chưa lưu danh sách câu</p>}
          <QuizItemList items={items} onChange={setItems} editable={editable}
            onEditInline={(item) => { setEditing(item); setDialog("inline"); }} />
        </Section>
        <QuizSettingsPanel quizId={quiz.id} value={settings} onChange={setSettings} errors={settingsErrors} readOnly={!editable}
          maxScore={detail.maxScore} />
      </div>
      {editable && (
        <QuizAiDraftPanel detail={detail} disabled={busy || dirty} onConflict={reload}
          onQuizChange={acceptedFromAi} openRequest={aiRequest} />
      )}
      {detail.canManage && <SlotOutlet slot={quizDetailSections} props={{ quizId: quiz.id, classId: classId ?? quiz.classId ?? undefined }} />}

      {dialog === "picker" && <BankQuestionPicker quizId={quiz.id} freeSlots={free} onClose={() => setDialog(null)} onAdd={addFromBank} />}
      {dialog === "inline" && <InlineQuestionDialog initial={editing} onClose={() => { setDialog(null); setEditing(null); }} onSave={saveInline} />}
      {dialog === "preview" && <QuizPreviewDialog title={quiz.title} items={detail.items} onClose={() => setDialog(null)} />}
      {dialog === "retire" && (
        <Dialog open onClose={() => setDialog(null)} tone="warning" title="Ngưng quiz?"
          footer={<>
            <Button variant="primary" disabled={busy || !reason.trim()} data-testid="quiz-retire-confirm"
              onClick={() => void run(async () => {
                show(await retireQuiz(quiz.id, reason.trim(), quiz.version));
                setDialog(null);
                setNotice({ tone: "success", text: "Đã ngưng quiz." });
              })}>Ngưng quiz</Button>
          </>}>
          <p>Bạn có chắc muốn ngưng quiz này không?</p>
          <Field label="Lý do" htmlFor="quiz-retire-reason" required>
            <textarea id="quiz-retire-reason" rows={3} maxLength={1000} value={reason} onChange={(event) => setReason(event.target.value)}
              className="w-full rounded-lg border border-field bg-surface px-3 py-2 text-[13.5px] outline-none focus:border-focus focus:shadow-[0_0_0_3px_var(--c-primary-soft)]" data-testid="quiz-retire-reason" />
          </Field>
        </Dialog>
      )}
      {dialog === "delete" && (
        <Dialog open onClose={() => setDialog(null)} tone="danger" title="Xóa quiz nháp?"
          footer={<>
            <Button variant="danger" disabled={busy} data-testid="quiz-delete-confirm"
              onClick={() => void run(async () => {
                await deleteQuiz(quiz.id, quiz.version);
                router.push(backHref);
              })}>Xóa</Button>
          </>}>
          <p>Bạn có chắc muốn xóa quiz nháp này không?</p>
        </Dialog>
      )}
    </>
  );
}
