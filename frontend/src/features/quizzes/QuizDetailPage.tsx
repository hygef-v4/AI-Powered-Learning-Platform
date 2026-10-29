"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { ArchiveItemDialog, ModuleItemDialog } from "@/features/materials/MaterialDialogs";
import { ModuleNeighbours, useModulePlace, useModuleScope } from "@/features/materials/ModulePlace";
import { notifyOutlineChanged, useOutlineChanged } from "@/features/materials/outline";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Menu, MenuItem, PageHeader, Pill, Section, showToast } from "@/shared/ui";
import { addInlineQuestion, getQuiz, reopenQuiz, publishQuiz, reviewQuiz, saveQuizItems, updateInlineQuestion, updateQuizDetails } from "./api";
import { QuizAiDraftPanel } from "./AiDraftTargets";
import { BankQuestionPicker } from "./BankQuestionPicker";
import { errorCode, errorMessage, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "./labels";
import {
  InlineQuestionDialog, QuizItemList, QuizPreviewDialog, QuizSettingsPanel, settingsFormOf, settingsInput, type DraftItem,
  type SettingsForm,
} from "./QuizDetailParts";
import type { QuizDetail, QuizItemView, Violation } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const MAX_ITEMS = 200;
type Dialogs = "picker" | "inline" | "preview" | "edit" | "archive" | null;

const draftItems = (detail: QuizDetail): DraftItem[] => detail.items.map((item) => ({ item, points: String(item.points) }));
const validPoints = (value: string) => /^\d+([.,]\d{1,2})?$/.test(value.trim()) && Number(value.replace(",", ".")) > 0
  && Number(value.replace(",", ".")) <= 9999.99;

/**
 * Màn #22 Quiz Detail (UC 48), cùng bố cục trang chi tiết học liệu: tên module nhỏ phía trên tên quiz, Sửa (cùng popup Tạo
 * học liệu) và Lưu trữ như ở danh sách học liệu; bên dưới là mô tả, câu hỏi, cấu hình rồi mục trước / sau trong danh sách
 * module (sidebar cũng là danh sách module). Quiz chưa lưu trữ sửa được tại chỗ, kể cả đang
 * mở (không tạo phiên bản mới). Giảng viên mở quiz của môn từ lớp (`classId`) thì chỉ xem.
 */
export function QuizDetailPage({ id, classId }: { id: string; classId?: string | null }) {
  const [detail, setDetail] = useState<QuizDetail | null>(null);
  const [items, setItems] = useState<DraftItem[]>([]);
  const [settings, setSettings] = useState<SettingsForm | null>(null);
  const [settingsErrors, setSettingsErrors] = useState<Record<string, string>>({});
  const [violations, setViolations] = useState<Violation[]>([]);
  const [error, setError] = useState<string | null>(null);
  // Mỗi lần chọn "Soạn bằng AI" trong nút Thêm câu hỏi thì tăng để panel AI mở popup.
  const [aiRequest, setAiRequest] = useState(0);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dialog, setDialog] = useState<Dialogs>(null);
  const [editing, setEditing] = useState<QuizItemView | null>(null);
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
  const scope = useModuleScope(quiz?.subjectId, classId ?? (quiz?.scope === "CLASS" ? quiz.classId : null));
  const place = useModulePlace(scope, (item) => item.kind === "quiz" && item.quiz.quizId === id);
  const editable = !!detail?.canManage && !!quiz && quiz.status !== "RETIRED";
  const itemsDirty = useMemo(() => !!detail && JSON.stringify(items.map((d) => [d.item.question.id, d.points]))
    !== JSON.stringify(draftItems(detail).map((d) => [d.item.question.id, d.points])), [items, detail]);
  const settingsDirty = useMemo(() => !!quiz && !!settings && JSON.stringify(settings) !== JSON.stringify(settingsFormOf(quiz)), [settings, quiz]);
  const dirty = itemsDirty || settingsDirty;

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
    show(next);
    showToast("Đã thêm câu từ đề xuất AI");
  }, [show]);

  const addFromBank = async (questionIds: string[]) => {
    if (!detail) return;
    const next = await saveQuizItems(detail.quiz.id, [
      ...detail.items.map((item) => ({ questionId: item.question.id, points: item.points })),
      ...questionIds.map((questionId) => ({ questionId, points: null })),
    ], detail.quiz.version);
    show(next);
    setDialog(null);
    showToast(`Đã thêm ${questionIds.length} câu từ ngân hàng`);
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
    showToast(action === "review" ? "Đã duyệt quiz" : "Đã phát hành quiz");
  });

  if (loadError) {
    return <PageLoadError onRetry={() => setReloadKey((k) => k + 1)}>{loadError}</PageLoadError>;
  }
  if (!detail || !quiz || !settings) return <PageLoading label="Đang tải quiz…" />;

  const free = MAX_ITEMS - detail.items.length;
  const editScope = classId ? { kind: "class" as const, classId } : quiz.scope === "CLASS" && quiz.classId
    ? { kind: "class" as const, classId: quiz.classId } : { kind: "subject" as const, subjectId: quiz.subjectId };
  const moduleQuiz = { quizId: quiz.id, title: quiz.title, status: quiz.status, scope: quiz.scope, classId: quiz.classId, canManage: detail.canManage };

  return (
    <>
      <PageHeader
        title={<><span className="min-w-0 [overflow-wrap:anywhere]">{quiz.title}</span><Pill tone={QUIZ_STATUS_TONES[quiz.status]}>{QUIZ_STATUS_LABELS[quiz.status]}</Pill></>}
        breadcrumb={place.moduleTitle ? [{ label: place.moduleTitle }, { label: quiz.title }] : undefined}
        actions={<>
          <Button icon="visibility" onClick={() => setDialog("preview")} data-testid="quiz-detail-preview">Xem trước</Button>
          {editable && dirty && <Button icon="save" disabled={busy} onClick={save} data-testid="quiz-detail-save">Lưu thay đổi</Button>}
          {detail.canManage && quiz.status === "DRAFT" && (
            <Button variant="primary" disabled={busy || dirty} title={dirty ? "Lưu thay đổi trước khi duyệt" : undefined}
              onClick={() => lifecycle("review")} data-testid="quiz-detail-review">Duyệt</Button>
          )}
          {detail.canManage && quiz.status === "REVIEWED" && (
            <Button variant="primary" disabled={busy || dirty} onClick={() => lifecycle("publish")} data-testid="quiz-detail-publish">Phát hành</Button>
          )}
          {detail.canManage && quiz.status === "RETIRED" && (
            <Button variant="primary" icon="lock_open" disabled={busy} data-testid="quiz-detail-reopen"
              onClick={() => void run(async () => { show(await reopenQuiz(quiz.id, quiz.version)); showToast("Đã mở lại quiz"); notifyOutlineChanged(); })}>Mở lại</Button>
          )}
          {editable && <>
            <Button icon="edit" disabled={busy} onClick={() => setDialog("edit")} data-testid="quiz-detail-edit">Sửa</Button>
            <Button variant="danger" icon="archive" disabled={busy} onClick={() => setDialog("archive")} data-testid="quiz-detail-archive">Lưu trữ</Button>
          </>}
        </>}
      />
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} position="top" />
      {quiz.instructions && <Section title="Mô tả"><p className="text-[14px] leading-relaxed whitespace-pre-wrap text-body">{quiz.instructions}</p></Section>}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {violations.length > 0 && (
        <Alert tone="danger" title="Cần sửa trước khi duyệt hoặc phát hành">
          <ul className="list-disc pl-4" data-testid="quiz-detail-violations">{violations.map((v, i) => <li key={i}>{v.message}</li>)}</ul>
        </Alert>
      )}
      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
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
          <QuizItemList items={items} onChange={setItems} editable={editable}
            onEditInline={(item) => { setEditing(item); setDialog("inline"); }} />
        </Section>
        <QuizSettingsPanel value={settings} onChange={setSettings} errors={settingsErrors} readOnly={!editable} />
      </div>
      {editable && (
        <QuizAiDraftPanel detail={detail} disabled={busy || dirty} onConflict={reload}
          onQuizChange={acceptedFromAi} openRequest={aiRequest} />
      )}
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} />

      {dialog === "picker" && <BankQuestionPicker quizId={quiz.id} freeSlots={free} onClose={() => setDialog(null)} onAdd={addFromBank} />}
      {dialog === "inline" && <InlineQuestionDialog initial={editing} onClose={() => { setDialog(null); setEditing(null); }} onSave={saveInline} />}
      {dialog === "preview" && <QuizPreviewDialog title={quiz.title} items={detail.items} onClose={() => setDialog(null)} />}
      {dialog === "edit" && <ModuleItemDialog scope={editScope} moduleId="" item={{ kind: "quiz", quiz: moduleQuiz }}
        onClose={() => setDialog(null)} onSaved={(message) => { setDialog(null); showToast(message); notifyOutlineChanged(); }} />}
      {dialog === "archive" && <ArchiveItemDialog item={{ quiz: moduleQuiz }} classId={classId ?? undefined} onClose={() => setDialog(null)}
        onSaved={() => { setDialog(null); showToast("Đã lưu trữ quiz", "danger"); notifyOutlineChanged(); }} />}
    </>
  );
}
