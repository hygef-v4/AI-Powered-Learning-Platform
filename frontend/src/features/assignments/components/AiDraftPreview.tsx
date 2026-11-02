import { Alert, Pill } from "@/shared/ui";
import type { AiDraft, AiProposalSection } from "../model/types";

type Proposal = NonNullable<AiDraft["proposal"]>;

function Paragraphs({ text }: { text: string }) {
  return <div className="space-y-3 text-sm leading-7 text-body">
    {text.trim().split(/\n\s*\n/).filter(Boolean).map((paragraph, index) =>
      <p key={index} className="whitespace-pre-wrap break-words">{paragraph}</p>)}
  </div>;
}

function points(section: AiProposalSection) {
  return section.rubric.reduce((sum, criterion) => sum + criterion.items.reduce(
    (itemSum, item) => itemSum + Math.round((item.points ?? 0) * 100), 0), 0) / 100;
}

/** Keep validation and questions visible before optional shared prose, even for malformed model output. */
export function AiDraftPreview({ proposal, targetPoints }: { proposal: Proposal; targetPoints: number }) {
  const validation = proposal.validation;
  const empty = proposal.sections.length === 0;
  const invalid = empty || !validation || validation.status === "INVALID";
  const leaves = proposal.sections.filter((section, index) => {
    const next = proposal.sections[index + 1];
    return !next || next.level <= section.level;
  });
  const covered = leaves.filter((section) => section.rubric.length > 0).length;
  return <div className="space-y-5">
    <section aria-label="Kết quả kiểm tra đề" className={`rounded-xl border p-4 ${invalid ? "border-danger/40 bg-danger/5" : "border-line bg-canvas"}`}>
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <Pill tone={invalid ? "danger" : validation?.status === "VALID" ? "success" : "warning"}>
          {invalid ? "Cần sửa trước khi áp dụng" : validation?.status === "VALID" ? "Kiểm tra đạt" : "Cần rà soát"}
        </Pill>
        <span>{validation?.questionCount ?? proposal.sections.filter((section) => section.level === 1).length} câu</span>
        <span>· {covered}/{leaves.length} phần có rubric</span>
        <strong className="sm:ml-auto">{Number(validation?.totalPoints ?? 0).toFixed(2)} / {targetPoints} điểm</strong>
      </div>
      {validation?.issues.length ? <ul className="mt-3 space-y-1 text-sm">
        {validation.issues.map((issue, index) => <li key={`${issue.code}-${index}`} className={issue.severity === "ERROR" ? "text-danger" : "text-warning"}>{issue.message}</li>)}
      </ul> : null}
      {!validation && <p className="mt-2 text-sm text-danger">Đề xuất chưa có kết quả kiểm tra. Hãy tạo lại bằng phiên bản hiện tại.</p>}
      {empty && <p className="mt-2 text-sm font-medium text-danger">AI chưa tạo câu hỏi và rubric. Chọn “Sửa yêu cầu và tạo lại” để tạo đề mới; bản hiện tại chưa được thêm vào bài tập.</p>}
    </section>

    {proposal.title && <h3 className="text-xl font-bold leading-7 text-ink">{proposal.title}</h3>}
    {proposal.businessContext && <details open={proposal.businessContext.length <= 900} className="rounded-xl border border-line bg-canvas p-4">
      <summary className="cursor-pointer text-sm font-semibold text-ink">Bối cảnh nghiệp vụ <span className="font-normal text-muted">· không tính điểm · bấm để mở/thu gọn</span></summary>
      <div className="mt-3 max-h-72 overflow-y-auto pr-2"><Paragraphs text={proposal.businessContext} /></div>
    </details>}
    {proposal.instructions && <details className="rounded-xl border border-line p-4">
      <summary className="cursor-pointer text-sm font-semibold text-ink">Hướng dẫn làm và nộp bài</summary>
      <div className="mt-3 max-h-56 overflow-y-auto pr-2"><Paragraphs text={proposal.instructions} /></div>
    </details>}

    <section aria-label="Câu hỏi và rubric" className="space-y-4">
      <h3 className="text-base font-bold text-ink">Câu hỏi và tiêu chí chấm</h3>
      {empty ? <Alert tone="warning">Không có câu hỏi để hiển thị. Phần bối cảnh và hướng dẫn không thay thế câu hỏi hoặc rubric.</Alert>
        : proposal.sections.map((section, index) => {
          const nextSection = proposal.sections[index + 1];
          const parent = nextSection !== undefined && nextSection.level > section.level;
          return <article key={index} className="overflow-hidden rounded-xl border border-line" aria-label={`Câu ${index + 1}`}>
            <header className="flex items-start gap-3 border-b border-line bg-canvas px-4 py-3">
              <span className="rounded-md bg-primary/10 px-2 py-1 text-xs font-bold text-primary-ink">{index + 1}</span>
              <h4 className="min-w-0 flex-1 break-words font-semibold leading-6 text-ink">{section.title}</h4>
              {!parent && <Pill>{points(section)} điểm</Pill>}
            </header>
            <div className="grid lg:grid-cols-2">
              <div className="min-w-0 p-4"><p className="mb-2 text-xs font-semibold uppercase text-muted">Yêu cầu / sản phẩm cần nộp</p>
                {section.guidance ? <Paragraphs text={section.guidance} /> : <p className="text-sm text-muted">Chưa có mô tả yêu cầu.</p>}
              </div>
              <div className="min-w-0 space-y-3 border-t border-line bg-canvas/50 p-4 lg:border-t-0 lg:border-l">
                <h5 className="text-sm font-semibold text-ink">Rubric · tiêu chí và điểm</h5>
                {section.rubric.length === 0 ? <p className={parent ? "text-sm text-muted" : "text-sm text-danger"}>{parent ? "Rubric được đặt tại các câu con." : "Câu này chưa có rubric. Cần tạo lại đề để bổ sung tiêu chí và điểm."}</p>
                  : section.rubric.map((criterion, criterionIndex) => <div key={criterionIndex} className="rounded-lg border border-line bg-surface p-3">
                    <p className="text-sm font-semibold text-ink">{criterionIndex + 1}. {criterion.title}</p>
                    {criterion.description && <p className="mt-1 whitespace-pre-wrap text-xs leading-5 text-muted">{criterion.description}</p>}
                    <ul className="mt-2 divide-y divide-line">{criterion.items.map((item, itemIndex) => <li key={itemIndex} className="flex items-start justify-between gap-4 py-2 text-sm leading-6">
                      <span className="min-w-0 whitespace-pre-wrap break-words">{item.text}</span><strong className="shrink-0 text-primary-ink">{item.points ?? "—"} điểm</strong>
                    </li>)}</ul>
                  </div>)}
              </div>
            </div>
          </article>;
        })}
    </section>
  </div>;
}
