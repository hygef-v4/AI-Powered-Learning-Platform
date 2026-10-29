"use client";
import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import { FileUploader } from "@/features/files/FileUploader";
import { cloneQuiz, copyQuizToClass, createClassQuiz, createSubjectQuiz, getClassQuizzes, getCopySources, getQuiz, getSubjectQuizzes, retireQuiz, updateQuizDetails } from "@/features/quizzes/api";
import type { QuizDetail, QuizRow } from "@/features/quizzes/types";
import { Button, Dialog, Field, Segmented, Select, TextInput } from "@/shared/ui";
import { materialApi, materialError, moduleApi, outlinePaths, validYoutube } from "./api";
import type { OutlineItem } from "./outline";
import type { Material, ModuleQuiz, ModuleScope } from "./types";

type Content = "FILE" | "YOUTUBE_LINK" | "QUIZ";
type StaffScope = Extract<ModuleScope, { kind: "subject" } | { kind: "class" }>;
const NEW_QUIZ = "";

const TEXTAREA = "min-h-24 w-full rounded-lg border border-field px-3 py-2 text-[13.5px] text-ink outline-none focus:border-focus disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted";

function quizDetails(detail: QuizDetail, title: string, instructions: string, keep = false) {
  const q = detail.quiz;
  return { title, instructions: instructions.trim() || (keep ? q.instructions : null), settings: q.settings, activityMonitoringEnabled: q.activityMonitoringEnabled,
    activityNotice: q.activityNotice, referenceMaterialIds: q.referenceMaterialIds };
}

/**
 * Popup Tạo học liệu (và Sửa, cùng popup): dòng đầu là tên, dòng hai chọn nội dung (Tệp, YouTube, Quiz) với ô riêng của
 * từng loại, dòng cuối là mô tả. Quiz tạo mới trống hoặc lấy một quiz cũ làm mẫu (câu hỏi, cấu hình chép sang quiz mới).
 * Sửa thì giữ loại nội dung; chọn tệp hay link mới là thay nội dung.
 */
type Template = QuizRow & { copy: boolean };
export function ModuleItemDialog({ scope, moduleId, item, onClose, onSaved }: {
  scope: StaffScope; moduleId: string; item?: OutlineItem; onClose: () => void; onSaved: (message: string) => void;
}) {
  const id = useId();
  const router = useRouter();
  const material = item?.kind === "material" ? item.material : null;
  const quiz = item?.kind === "quiz" ? item.quiz : null;
  const editing = !!item;
  const [content, setContent] = useState<Content>(material ? material.kind : quiz ? "QUIZ" : "FILE");
  const [title, setTitle] = useState(material?.title ?? quiz?.title ?? "");
  const [description, setDescription] = useState(material?.description ?? "");
  const [url, setUrl] = useState(material?.kind === "YOUTUBE_LINK" ? material.current.videoUrl ?? "" : "");
  const [fileRef, setFileRef] = useState<string>();
  const [quizId, setQuizId] = useState(NEW_QUIZ);
  const [quizzes, setQuizzes] = useState<Template[] | null>(null);
  const [detail, setDetail] = useState<QuizDetail | null>(null);
  const [busy, setBusy] = useState(false); const [error, setError] = useState(""); const lock = useRef(false);

  useEffect(() => {
    if (!quiz) return;
    getQuiz(quiz.quizId, scope.kind === "class" ? scope.classId : null).then((value) => {
      setDetail(value); setDescription(value.quiz.instructions ?? "");
    }, (e: unknown) => setError(materialError(e)));
  }, [quiz, scope]);
  useEffect(() => {
    if (content !== "QUIZ" || editing || quizzes) return;
    // Mẫu: quiz của môn (Chủ nhiệm môn); quiz của lớp và quiz của lớp khác cùng môn còn chép được (giảng viên).
    const load: Promise<Template[]> = scope.kind === "subject"
      ? getSubjectQuizzes(scope.subjectId, { size: 100 }).then((page) => page.items.map((row) => ({ ...row, copy: false })))
      : Promise.all([getClassQuizzes(scope.classId), getCopySources(scope.classId).catch(() => [] as QuizRow[])]).then(([own, others]) => [
        ...own.filter((row) => row.scope === "CLASS").map((row) => ({ ...row, copy: false })),
        ...others.filter((row) => !own.some((o) => o.id === row.id)).map((row) => ({ ...row, copy: true })),
      ]);
    load.then(setQuizzes, (e: unknown) => setError(materialError(e)));
  }, [content, editing, quizzes, scope]);

  function chooseQuiz(value: string) {
    setQuizId(value); setError("");
    const row = quizzes?.find((q) => q.id === value);
    if (row && !title.trim()) setTitle(row.title);
  }

  async function save() {
    if (lock.current) return;
    if (!title.trim()) { setError("Nhập tên học liệu."); return; }
    if (content === "YOUTUBE_LINK" && (!editing || url.trim() !== (material?.current.videoUrl ?? "")) && !validYoutube(url)) {
      setError("Chỉ nhận link HTTPS của một video YouTube, không nhận playlist."); return;
    }
    if (content === "FILE" && !editing && !fileRef) { setError("Chọn tệp."); return; }
    lock.current = true; setBusy(true); setError("");
    try {
      if (material) {
        let current: Material = material;
        const newUrl = content === "YOUTUBE_LINK" && url.trim() !== (material.current.videoUrl ?? "");
        if (fileRef || newUrl) current = await materialApi.replace(current, fileRef ? { fileRef } : { videoUrl: url.trim() });
        await materialApi.update(current, title.trim(), description);
        onSaved("Đã lưu thay đổi");
      } else if (quiz) {
        if (!detail) return;
        await updateQuizDetails(quiz.quizId, quizDetails(detail, title.trim(), description), detail.quiz.version);
        onSaved("Đã lưu thay đổi");
      } else if (content === "QUIZ") {
        const template = quizzes?.find((q) => q.id === quizId);
        let created: QuizDetail;
        if (template) {
          created = template.copy && scope.kind === "class" ? await copyQuizToClass(scope.classId, template.id) : await cloneQuiz(template.id);
          created = await updateQuizDetails(created.quiz.id, quizDetails(created, title.trim(), description, true), created.quiz.version);
        } else {
          created = scope.kind === "subject" ? await createSubjectQuiz(scope.subjectId, title.trim()) : await createClassQuiz(scope.classId, title.trim());
          if (description.trim()) created = await updateQuizDetails(created.quiz.id, quizDetails(created, title.trim(), description), created.quiz.version);
        }
        await moduleApi.attachQuiz(moduleId, created.quiz.id);
        router.push(scope.kind === "class" ? `/quizzes/${encodeURIComponent(created.quiz.id)}?classId=${encodeURIComponent(scope.classId)}` : `/quizzes/${encodeURIComponent(created.quiz.id)}`);
      } else {
        await materialApi.create(outlinePaths(scope).materials, { kind: content, title: title.trim(), description, orderNo: 0,
          source: content === "FILE" ? { fileRef } : { videoUrl: url.trim() }, moduleId });
        onSaved("Đã thêm học liệu");
      }
    } catch (e) { setError(materialError(e)); }
    finally { lock.current = false; setBusy(false); }
  }

  return <Dialog open size="lg" title={editing ? "Sửa học liệu" : "Tạo học liệu"} onClose={onClose} dismissible={!busy}
    footer={<Button data-testid="material-source-save" variant="primary" disabled={busy || (!!quiz && !detail)} onClick={() => void save()}>{editing ? "Lưu" : "Tạo"}</Button>}>
    <form onSubmit={(event) => { event.preventDefault(); void save(); }} noValidate className="flex flex-col gap-4">
      <Field label="Tên học liệu" htmlFor={`${id}-title`} required>
        <TextInput id={`${id}-title`} data-testid="material-source-title" maxLength={200} value={title} disabled={busy}
          onChange={e => { setTitle(e.target.value); setError(""); }} />
      </Field>
      <div className="flex flex-col gap-3">
        <span className="text-xs font-semibold text-body">Nội dung <span className="text-primary-ink">*</span></span>
        <Segmented<Content> ariaLabel="Nội dung" value={content} onChange={(value) => { if (!editing) { setContent(value); setError(""); } }}
          options={[
            { value: "FILE", label: "Tệp", disabled: editing && content !== "FILE" },
            { value: "YOUTUBE_LINK", label: "YouTube", disabled: editing && content !== "YOUTUBE_LINK" },
            { value: "QUIZ", label: "Quiz", disabled: editing && content !== "QUIZ" },
          ]} />
        {/* Ô riêng của loại nội dung đang chọn, trong một thẻ trắng ngay dưới. */}
        {!(content === "QUIZ" && editing) && <div className="rounded-xl border border-line bg-surface p-4">
          {content === "FILE" && <FileUploader purpose="MATERIAL" currentName={material?.current.fileName}
            onUploaded={result => { setFileRef(result.fileRef.value); setError(""); }} />}
          {content === "YOUTUBE_LINK" && <TextInput id={`${id}-url`} aria-label="Link YouTube" data-testid="material-source-url" type="url" icon="link" value={url}
            disabled={busy} placeholder="https://www.youtube.com/watch?v=…" onChange={e => { setUrl(e.target.value); setError(""); }} />}
          {content === "QUIZ" && <Select id={`${id}-quiz`} aria-label="Quiz" value={quizId} disabled={busy || !quizzes} onChange={(e) => chooseQuiz(e.target.value)} data-testid="module-quiz-select">
            <option value={NEW_QUIZ}>Quiz trống</option>
            {quizzes && quizzes.length > 0 && <optgroup label="Tạo từ quiz cũ">
              {quizzes.map((row) => <option key={row.id} value={row.id}>{row.title}</option>)}
            </optgroup>}
          </Select>}
        </div>}
      </div>
      <Field label="Mô tả" htmlFor={`${id}-description`}>
        <textarea id={`${id}-description`} maxLength={content === "QUIZ" ? 5000 : 2000} value={description} disabled={busy}
          onChange={e => setDescription(e.target.value)} className={TEXTAREA} />
      </Field>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </form>
  </Dialog>;
}

/** Xóa học liệu (ẩn khỏi danh sách, dữ liệu vẫn giữ) hay lưu trữ Quiz. */
export function ArchiveItemDialog({ item, classId, onClose, onSaved }: { item: { material: Material } | { quiz: ModuleQuiz }; classId?: string; onClose: () => void; onSaved: () => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState(""); const lock = useRef(false);
  const isQuiz = "quiz" in item;
  const title = isQuiz ? item.quiz.title : item.material.title;
  async function archive() {
    if (lock.current) return; lock.current = true; setBusy(true); setError("");
    try {
      if (isQuiz) {
        const detail = await getQuiz(item.quiz.quizId, classId ?? null);
        await retireQuiz(item.quiz.quizId, "", detail.quiz.version);
      } else await materialApi.archive(item.material);
      onSaved();
    } catch (e) { setError(materialError(e)); } finally { lock.current = false; setBusy(false); }
  }
  return <Dialog open title={isQuiz ? "Lưu trữ quiz?" : "Xóa học liệu?"} onClose={onClose} dismissible={!busy} tone="danger"
    footer={<Button data-testid="material-archive-confirm" variant="danger" disabled={busy} onClick={() => void archive()}>{isQuiz ? "Lưu trữ" : "Xóa"}</Button>}>
    <p>Bạn có chắc muốn {isQuiz ? "lưu trữ quiz" : "xóa học liệu"} {title} không?</p>
    {error && <p role="alert" className="text-sm text-danger">{error}</p>}
  </Dialog>;
}
