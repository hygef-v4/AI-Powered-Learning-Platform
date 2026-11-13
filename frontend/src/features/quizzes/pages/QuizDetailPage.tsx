"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ArchiveItemDialog, ModuleItemDialog, removalAction, removalMessage } from "@/features/materials/components/MaterialDialogs";
import { ModuleNeighbours, useModulePlace, useModuleScope } from "@/features/materials/components/ModulePlace";
import { boardHref, notifyOutlineChanged, useOutlineChanged } from "@/features/materials/model/outline";
import { ApiError } from "@/shared/api/client";
import { FormattedTextView } from "@/shared/document/FormattedTextView";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, Dialog, Menu, MenuItem, PageHeader, Pill, Section, showToast } from "@/shared/ui";
import { addInlineQuestion, getQuiz, proposeQuestion, reopenQuiz, publishQuiz, saveQuizItems, updateInlineQuestion, updateQuizDetails } from "../api";
import { QuizAiDraftPanel } from "../components/AiDraftTargets";
import { BankQuestionPicker } from "../components/BankQuestionPicker";
import { DrawRulesPanel } from "../components/DrawRulesPanel";
import { errorCode, errorMessage, formatPoints, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "../model/labels";
import { quizDetailSections } from "../slots";
import {
  InlineQuestionDialog, QuizItemList, QuizPreviewDialog, QuizSettingsPanel, settingsFormOf, settingsInput, type DraftItem,
  type SettingsForm,
} from "../components/QuizDetailParts";
import type { QuizDetail, QuizItemView, Violation } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { useAuthoringGuard } from "../hooks/useAuthoringGuard";

const MAX_ITEMS = 200;
type Dialogs = "picker" | "inline" | "ai" | "preview" | "edit" | "archive" | "publish" | null;

const draftItems = (detail: QuizDetail): DraftItem[] => detail.items.map((item) => ({ item, points: String(item.points) }));
const validPoints = (value: string) => /^\d+([.,]\d{1,2})?$/.test(value.trim()) && Number(value.replace(",", ".")) > 0
  && Number(value.replace(",", ".")) <= 9999.99;

/**
 * Màn #22 Quiz Detail (UC 48), cùng bố cục trang chi tiết học liệu: tên module nhỏ phía trên tên quiz, Sửa (cùng popup Tạo
 * học liệu) và Xóa (Quiz nháp) hoặc Lưu trữ như ở danh sách học liệu. Cột trái: cách lập đề, câu hỏi rồi mô tả; cột phải: cấu hình rồi lượt làm
 * của sinh viên; cuối trang là mục trước / sau trong danh sách module (sidebar cũng là danh sách module). Quiz chưa lưu trữ sửa được tại chỗ, kể cả đang
 * mở (không tạo phiên bản mới). Giảng viên mở quiz của môn từ lớp (`classId`) thì chỉ xem. Quiz sinh đề dùng quy tắc thay cho
 * danh sách câu (BR-U10-17).
 */
export function QuizDetailPage({ id, classId }: { id: string; classId?: string | null }) {
  const router = useRouter();
  const [detail, setDetail] = useState<QuizDetail | null>(null);
  const [items, setItems] = useState<DraftItem[]>([]);
  const [settings, setSettings] = useState<SettingsForm | null>(null);
  const [settingsErrors, setSettingsErrors] = useState<Record<string, string>>({});
  const [violations, setViolations] = useState<Violation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [authorDirty, setAuthorDirty] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dialog, setDialog] = useState<Dialogs>(null);
  const [editing, setEditing] = useState<QuizItemView | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  // Mọi lần nhận Quiz mới từ máy chủ (thêm câu, lưu, tải lại…) thì lỗi phát hành cũ không còn đúng nữa.
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
  const scope = useModuleScope(quiz?.subjectId, classId ?? (quiz?.scope === "CLASS" ? quiz.classId : null));
  const place = useModulePlace(scope, (item) => item.kind === "quiz" && item.quiz.quizId === id);
  const editable = !!detail?.canManage && !!quiz && quiz.status !== "RETIRED";
  const itemsDirty = useMemo(() => !!detail && JSON.stringify(items.map((d) => [d.item.question.id, d.points]))
    !== JSON.stringify(draftItems(detail).map((d) => [d.item.question.id, d.points])), [items, detail]);
  const settingsDirty = useMemo(() => !!quiz && !!settings && JSON.stringify(settings) !== JSON.stringify(settingsFormOf(quiz)), [settings, quiz]);
  const dirty = itemsDirty || settingsDirty;
  const confirmDiscard = useAuthoringGuard(authorDirty || dirty);
  const chooseDialog = (next: Dialogs, item: QuizItemView | null = null) => {
    if (busy) return false;
    if (next === dialog && item?.question.id === editing?.question.id) return true;
    if (!confirmDiscard()) return false;
    setAuthorDirty(false);
    setEditing(item);
    setDialog(next);
    return true;
  };

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    setViolations([]);
    try {
      await action();
    } catch (failure) {
      const problem = failure instanceof ApiError ? (failure.problem as { violations?: Violation[] }) : {};
      if (problem.violations?.length) setViolations(problem.violations);
      setError(errorMessage(failure));
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
      setError(invalid);
      return;
    }
    let current = detail;
    if (input) current = await updateQuizDetails(current.quiz.id, input, current.quiz.version);
    if (itemsDirty) {
      current = await saveQuizItems(current.quiz.id, items.map((d) => ({ questionId: d.item.question.id,
        points: Number(d.points.replace(",", ".")) })), current.quiz.version);
    }
    show(current);
    showToast("Đã lưu thay đổi");
  });

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);
  useOutlineChanged(reload);
  const acceptedFromAi = useCallback((next: QuizDetail) => {
    setAuthorDirty(false);
    setDialog(null);
    show(next);
    showToast("Đã thêm câu từ đề xuất AI");
  }, [show]);

  const addFromBank = async (questionIds: string[]) => {
    if (!detail || busy) return;
    setBusy(true);
    try {
      const next = await saveQuizItems(detail.quiz.id, [
        ...detail.items.map((item) => ({ questionId: item.question.id, points: item.points })),
        ...questionIds.map((questionId) => ({ questionId, points: null })),
      ], detail.quiz.version);
      show(next);
      setDialog(null);
      showToast(`Đã thêm ${questionIds.length} câu từ ngân hàng`);
    } finally { setBusy(false); }
  };

  const saveInline = async (content: Parameters<typeof addInlineQuestion>[1]) => {
    if (!detail || busy) return;
    setBusy(true);
    try {
      show(editing
        ? await updateInlineQuestion(detail.quiz.id, editing.question.id, content, detail.quiz.version)
        : await addInlineQuestion(detail.quiz.id, content, detail.quiz.version));
    } catch (failure) {
      throw new Error(errorMessage(failure));
    } finally { setBusy(false); }
    setDialog(null);
    setEditing(null);
  };

  // Phát hành thẳng từ nháp (BR-U10-21): máy chủ vẫn kiểm đủ điều kiện, lỗi trả danh sách cần sửa.
  const publish = () => quiz && void run(async () => {
    setDialog(null);
    show(await publishQuiz(quiz.id, quiz.version));
    showToast("Đã phát hành quiz");
    notifyOutlineChanged();
  });

  // Giảng viên gửi câu riêng vào ngân hàng của môn để Chủ nhiệm môn duyệt (BR-U10-09); câu trong quiz giữ nguyên.
  const propose = (item: QuizItemView) => quiz?.classId && void run(async () => {
    await proposeQuestion(quiz.classId as string, item.question);
    showToast("Đã gửi đề xuất cho Chủ nhiệm môn");
  });

  if (loadError) {
    return <PageLoadError onRetry={() => setReloadKey((k) => k + 1)}>{loadError}</PageLoadError>;
  }
  if (!detail || !quiz || !settings) return <PageLoading label="Đang tải quiz…" />;

  const free = MAX_ITEMS - detail.items.length;
  const questionEditor = dialog === "inline" ? <InlineQuestionDialog key={editing?.question.id ?? "new"}
    inline initial={editing} onClose={() => chooseDialog(null)} onSave={saveInline} onDirtyChange={setAuthorDirty} /> : null;
  const generated = quiz.paperMode === "GENERATED";
  const questionCount = generated ? detail.drawRules.reduce((sum, r) => sum + r.questionCount, 0) : detail.items.length;
  const editScope = classId ? { kind: "class" as const, classId } : quiz.scope === "CLASS" && quiz.classId
    ? { kind: "class" as const, classId: quiz.classId } : { kind: "subject" as const, subjectId: quiz.subjectId };
  const moduleQuiz = { quizId: quiz.id, title: quiz.title, status: quiz.status, scope: quiz.scope, classId: quiz.classId, canManage: detail.canManage };
  const moduleItem = { kind: "quiz" as const, quiz: moduleQuiz };
  const removal = removalAction(moduleItem);

  return (
    <>
      <PageHeader
        title={<><span className="min-w-0 [overflow-wrap:anywhere]">{quiz.title}</span><Pill tone={QUIZ_STATUS_TONES[quiz.status]}>{QUIZ_STATUS_LABELS[quiz.status]}</Pill></>}
        breadcrumb={place.moduleTitle ? [{ label: place.moduleTitle }, { label: quiz.title }] : undefined}
        actions={<>
          {!generated && <Button icon="visibility" disabled={busy} onClick={() => chooseDialog("preview")} data-testid="quiz-detail-preview">Xem trước</Button>}
          {editable && dirty && <Button icon="save" disabled={busy || authorDirty} onClick={save} data-testid="quiz-detail-save">Lưu thay đổi</Button>}
          {detail.canManage && quiz.status === "DRAFT" && (
            <Button variant="primary" disabled={busy || dirty || authorDirty} title={dirty || authorDirty ? "Lưu thay đổi trước khi phát hành" : undefined}
              onClick={() => chooseDialog("publish")} data-testid="quiz-detail-publish">Phát hành</Button>
          )}
          {detail.canManage && quiz.status === "RETIRED" && (
            <Button variant="primary" icon="lock_open" disabled={busy} data-testid="quiz-detail-reopen"
              onClick={() => void run(async () => { show(await reopenQuiz(quiz.id, quiz.version)); showToast("Đã mở lại quiz"); notifyOutlineChanged(); })}>Mở lại</Button>
          )}
          {editable && <>
            <Button icon="edit" disabled={busy} onClick={() => chooseDialog("edit")} data-testid="quiz-detail-edit">Sửa</Button>
            <Button variant="danger" icon={removal.icon} disabled={busy} onClick={() => chooseDialog("archive")} data-testid="quiz-detail-archive">{removal.label}</Button>
          </>}
        </>}
      />
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} position="top" />
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {violations.length > 0 && (
        <Alert tone="danger" title="Cần sửa trước khi phát hành">
          <ul className="list-disc pl-4" data-testid="quiz-detail-violations">{violations.map((v, i) => <li key={i}>{v.message}</li>)}</ul>
        </Alert>
      )}
      {/* Cột phải rộng theo màn hình (bảng lượt làm cần chỗ), tối thiểu 360px. */}
      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(360px,40%)]">
        <div className="flex min-w-0 flex-col gap-4">
        {(editable || generated) && (
          <DrawRulesPanel key={quiz.version} detail={detail} editable={editable} disabled={dirty || !!dialog} onSaved={show}
            onError={(failure) => {
              const problem = failure instanceof ApiError ? (failure.problem as { violations?: Violation[] }) : {};
              setViolations(problem.violations ?? []);
              setError(errorMessage(failure));
              if (errorCode(failure) === "QUIZ_VERSION_CONFLICT") setReloadKey((k) => k + 1);
            }} />
        )}
        {!generated && <Section title="Câu hỏi"
          actions={editable && (
            <Menu align="right" icon="add" label="Thêm câu hỏi" ariaLabel="Thêm câu hỏi">
              <MenuItem icon="edit" disabled={busy || dirty || free <= 0} onClick={() => chooseDialog("inline")}
                data-testid="quiz-detail-add-inline">Soạn câu mới</MenuItem>
              <MenuItem icon="help_center" disabled={busy || dirty || free <= 0} onClick={() => chooseDialog("picker")}
                data-testid="quiz-detail-add-bank">Lấy từ ngân hàng</MenuItem>
              <MenuItem icon="sparkle" disabled={busy || dirty || free <= 0} onClick={() => chooseDialog("ai")}
                data-testid="quiz-ai-draft-open">Soạn bằng AI</MenuItem>
            </Menu>
          )}>
          {(dialog === "inline" || dialog === "picker" || dialog === "ai") && <div role="group" aria-label="Cách thêm câu hỏi" className="mb-4 flex flex-wrap gap-1 border-b border-line pb-3">
            {([["inline", "Soạn câu mới"], ["picker", "Ngân hàng"], ["ai", "Soạn bằng AI"]] as const).map(([mode, label]) =>
              <Button key={mode} size="sm" variant={dialog === mode ? "primary" : "ghost"} disabled={busy}
                aria-pressed={dialog === mode} onClick={() => chooseDialog(mode)}>{label}</Button>)}
          </div>}
          <QuizItemList items={items} onChange={setItems} editable={editable && !dialog}
            onEditInline={(item) => chooseDialog("inline", item)} editingId={editing?.question.id} editor={questionEditor}
            onPropose={quiz.scope === "CLASS" ? propose : undefined} />
          {dialog === "inline" && !editing && <div className="mt-4">{questionEditor}</div>}
          {dialog === "picker" && <BankQuestionPicker inline quizId={quiz.id} freeSlots={free}
            onClose={() => chooseDialog(null)} onAdd={addFromBank} onDirtyChange={setAuthorDirty} />}
          {editable && <QuizAiDraftPanel inline detail={detail} disabled={busy || dirty}
            onConflict={reload} onQuizChange={acceptedFromAi} requestOpen={dialog === "ai"}
            onRequestOpen={() => chooseDialog("ai")}
            onRequestClose={() => chooseDialog(null)} onDirtyChange={setAuthorDirty} onBusyChange={setBusy} />}
        </Section>}
        {quiz.instructions && <Section title="Mô tả"><FormattedTextView text={quiz.instructions} document={quiz.instructionsDocument} /></Section>}
        </div>
        <div className="flex min-w-0 flex-col gap-4">
          <QuizSettingsPanel value={settings} onChange={setSettings} errors={settingsErrors} readOnly={!editable || !!dialog} />
          <SlotOutlet slot={quizDetailSections} props={{ quizId: quiz.id, classId: classId ?? quiz.classId ?? undefined }} />
        </div>
      </div>
      {editable && generated && (
        <QuizAiDraftPanel detail={detail} disabled={busy || dirty} onConflict={reload}
          onQuizChange={acceptedFromAi} />
      )}
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} />

      {dialog === "publish" && (
        <Dialog open onClose={() => setDialog(null)} title="Phát hành quiz?" tone="warning"
          footer={<Button variant="primary" disabled={busy} onClick={publish} data-testid="quiz-detail-publish-confirm">Phát hành</Button>}>
          <p className="text-[13px]" data-testid="quiz-detail-publish-summary">
            {generated ? `Mỗi lượt làm nhận một đề mới gồm ${questionCount} câu` : `Quiz có ${questionCount} câu`}, tổng {formatPoints(detail.maxScore)} điểm. Sau khi phát hành, sinh viên của lớp hoặc môn sẽ làm được ngay.
          </p>
        </Dialog>
      )}
      {dialog === "preview" && <QuizPreviewDialog title={quiz.title} items={detail.items} onClose={() => setDialog(null)} />}
      {dialog === "edit" && <ModuleItemDialog scope={editScope} moduleId="" item={{ kind: "quiz", quiz: moduleQuiz }}
        onClose={() => setDialog(null)} onSaved={(message) => { setDialog(null); showToast(message); notifyOutlineChanged(); }} />}
      {dialog === "archive" && <ArchiveItemDialog item={{ quiz: moduleQuiz }} classId={classId ?? undefined} onClose={() => setDialog(null)}
        onSaved={(removed) => {
          setDialog(null);
          showToast(removalMessage(moduleItem), "danger");
          notifyOutlineChanged();
          if (removed) router.push(scope ? boardHref(scope) : "/manager/materials");
        }} />}
    </>
  );
}
