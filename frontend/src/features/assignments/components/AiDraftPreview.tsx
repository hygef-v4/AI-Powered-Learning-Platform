import { Alert, Pill } from "@/shared/ui";
import { RunsView } from "@/shared/document/DocumentViewer";
import { alignClass } from "@/shared/document/DocumentViewer";
import type { AiDraft, AiProposalSection, AiTextFormat } from "../model/types";
import { msg } from "@/i18n/msg";

type Proposal = NonNullable<AiDraft["proposal"]>;

function FormattedRuns({ text, format = {} }: { text: string; format?: AiTextFormat }) {
  const { runs } = format;
  const style = { bold: format.bold, italic: format.italic, underline: format.underline, strike: format.strike, code: format.code, color: format.color, highlight: format.highlight };
  return <RunsView runs={(runs ?? [{ text }]).map(run => ({ ...run, ...Object.fromEntries(Object.entries(style).filter(([, value]) => value !== undefined)) }))} />;
}

function Paragraphs({ text, format }: { text: string; format?: AiTextFormat }) {
  if (format && Object.keys(format).length) return <p className={`whitespace-pre-wrap break-words text-sm leading-7 text-body ${alignClass(format.align)}`}><FormattedRuns text={text} format={format} /></p>;
  return <div className="space-y-3 text-sm leading-7 text-body">
    {text.trim().split(/\n\s*\n/).filter(Boolean).map((paragraph, index) =>
      <p key={index} className="whitespace-pre-wrap break-words">{paragraph}</p>)}
  </div>;
}

function sumPoints(section: AiProposalSection) {
  return section.rubric.reduce((sum, criterion) => sum + criterion.items.reduce(
    (itemSum, item) => itemSum + Math.round((item.points ?? 0) * 100), 0), 0) / 100;
}

/** Keep validation and questions visible before optional shared prose, even for malformed model output. */
export function AiDraftPreview({ proposal, targetPoints }: { proposal: Proposal; targetPoints?: number }) {
  const validation = proposal.validation;
  const empty = proposal.sections.length === 0;
  const invalid = empty || !validation || validation.status === "INVALID";
  const leaves = proposal.sections.filter((section, index) => {
    const next = proposal.sections[index + 1];
    return !next || next.level <= section.level;
  });
  const covered = leaves.filter((section) => section.rubric.length > 0).length;
  return <div className="space-y-5">
    <section aria-label={msg("assignments.components.AiDraftPreview.assignmentCheckResults")} className={`rounded-xl border p-4 ${invalid ? "border-danger/40 bg-danger/5" : "border-line bg-canvas"}`}>
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <Pill tone={invalid ? "danger" : validation?.status === "VALID" ? "success" : "warning"}>
          {invalid ? msg("assignments.components.AiDraftPreview.needsFixingBeforeApplying") : validation?.status === "VALID" ? msg("assignments.components.AiDraftPreview.checkPassed") : msg("assignments.components.AiDraftPreview.needsReview")}
        </Pill>
        <span>{validation?.questionCount ?? proposal.sections.filter((section) => section.level === 1).length} {msg("assignments.components.AiDraftPreview.questions")}</span>
        <span>· {covered}/{leaves.length} {msg("assignments.components.AiDraftPreview.partsWithRubric")}</span>
        <strong className="sm:ml-auto">{Number(validation?.totalPoints ?? 0).toFixed(2)}{targetPoints === undefined ? "" : ` / ${targetPoints}`} {msg("assignments.components.AiDraftPreview.points")}</strong>
      </div>
      {validation?.issues.length ? <ul className="mt-3 space-y-1 text-sm">
        {validation.issues.map((issue, index) => <li key={`${issue.code}-${index}`} className={issue.severity === "ERROR" ? "text-danger" : "text-warning"}>{issue.message}</li>)}
      </ul> : null}
      {!validation && <p className="mt-2 text-sm text-danger">{msg("assignments.components.AiDraftPreview.thisProposalHasNoCheck")}</p>}
      {empty && <p className="mt-2 text-sm font-medium text-danger">{msg("assignments.components.AiDraftPreview.theAiHasNotCreated")}</p>}
    </section>

    {proposal.title && <h3 className="text-xl font-bold leading-7 text-ink">{proposal.title}</h3>}
    {proposal.businessContext && <details open={proposal.businessContext.length <= 900} className="rounded-xl border border-line bg-canvas p-4">
      <summary className="cursor-pointer text-sm font-semibold text-ink">{msg("assignments.components.AiDraftPreview.businessContext")}<span className="font-normal text-muted">{msg("assignments.components.AiDraftPreview.notGradedClickToExpand")}</span></summary>
      <div className="mt-3 max-h-72 overflow-y-auto pr-2"><Paragraphs text={proposal.businessContext} format={proposal.businessContextFormat} /></div>
    </details>}
    {proposal.instructions && <details className="rounded-xl border border-line p-4">
      <summary className="cursor-pointer text-sm font-semibold text-ink">{msg("assignments.components.AiDraftPreview.instructionsForDoingAndSubmitting")}</summary>
      <div className="mt-3 max-h-56 overflow-y-auto pr-2"><Paragraphs text={proposal.instructions} /></div>
    </details>}

    <section aria-label={msg("assignments.components.AiDraftPreview.questionsAndRubric")} className="space-y-4">
      <h3 className="text-base font-bold text-ink">{msg("assignments.components.AiDraftPreview.questionsAndGradingCriteria")}</h3>
      {empty ? <Alert tone="warning">{msg("assignments.components.AiDraftPreview.noQuestionsToDisplayThe")}</Alert>
        : proposal.sections.map((section, index) => {
          const nextSection = proposal.sections[index + 1];
          const parent = nextSection !== undefined && nextSection.level > section.level;
          return <article key={index} className="overflow-hidden rounded-xl border border-line" aria-label={msg("assignments.components.AiDraftPreview.question", { value: String(index + 1) })}>
            <header className="flex items-start gap-3 border-b border-line bg-canvas px-4 py-3">
              <span className="rounded-md bg-primary/10 px-2 py-1 text-xs font-bold text-primary-ink">{index + 1}</span>
              <h4 className={`min-w-0 flex-1 break-words font-semibold leading-6 text-ink ${alignClass(section.titleFormat?.align)}`}><FormattedRuns text={section.title} format={section.titleFormat} /></h4>
              {!parent && <Pill>{sumPoints(section)} {msg("assignments.components.AiDraftPreview.points")}</Pill>}
            </header>
            <div className="grid lg:grid-cols-2">
              <div className="min-w-0 p-4"><p className="mb-2 text-xs font-semibold uppercase text-muted">{msg("assignments.components.AiDraftPreview.requirementsDeliverables")}</p>
                {section.guidance ? <Paragraphs text={section.guidance} format={section.guidanceFormat} /> : <p className="text-sm text-muted">{msg("assignments.components.AiDraftPreview.noRequirementDescriptionYet")}</p>}
              </div>
              <div className="min-w-0 space-y-3 border-t border-line bg-canvas/50 p-4 lg:border-t-0 lg:border-l">
                <h5 className="text-sm font-semibold text-ink">{msg("assignments.components.AiDraftPreview.rubricCriteriaAndPoints")}</h5>
                {section.rubric.length === 0 ? <p className={parent ? "text-sm text-muted" : "text-sm text-danger"}>{parent ? msg("assignments.components.AiDraftPreview.theRubricIsSetOn") : msg("assignments.components.AiDraftPreview.thisQuestionHasNoRubric")}</p>
                  : section.rubric.map((criterion, criterionIndex) => <div key={criterionIndex} className="rounded-lg border border-line bg-surface p-3">
                    <p className="text-sm font-semibold text-ink">{criterionIndex + 1}. {criterion.title}</p>
                    {criterion.description && <p className="mt-1 whitespace-pre-wrap text-xs leading-5 text-muted">{criterion.description}</p>}
                    <ul className="mt-2 divide-y divide-line">{criterion.items.map((item, itemIndex) => <li key={itemIndex} className="flex items-start justify-between gap-4 py-2 text-sm leading-6">
                      <span className="min-w-0 whitespace-pre-wrap break-words">{item.text}</span><strong className="shrink-0 text-primary-ink">{item.points ?? "—"} {msg("assignments.components.AiDraftPreview.points")}</strong>
                    </li>)}</ul>
                  </div>)}
              </div>
            </div>
          </article>;
        })}
    </section>
  </div>;
}
