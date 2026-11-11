import type { AttemptResult, AttemptResultItem } from "../slots";
import { formatPoints } from "../model/attemptResult";

export function PublishedFeedback({ feedback, summary }: Pick<AttemptResult, "feedback" | "summary">) {
  return <div className="space-y-3 text-[13px] text-body">
    {feedback && <p className="whitespace-pre-wrap">{feedback}</p>}
    {summary && ([
      ["Điểm mạnh", summary.strengths], ["Cần cải thiện", summary.improvements], ["Bước tiếp theo", summary.nextSteps],
    ] as const).map(([title, lines]) => lines.length > 0 && <section key={title}>
      <h4 className="font-semibold text-ink">{title}</h4>
      <ul className="list-disc space-y-1 pl-4">{lines.map((line, index) => <li key={index}>{line}</li>)}</ul>
    </section>)}
  </div>;
}

export function PublishedRubricItems({ items }: { items: AttemptResultItem[] }) {
  return <ul className="mt-3 space-y-3 border-t border-line pt-3 text-xs text-body">
    {items.map((item, index) => <li key={index} className="space-y-1">
      <p className="font-semibold text-ink">{item.itemText} · {formatPoints(item.awarded)} / {formatPoints(item.max)}</p>
      {item.evidence && <blockquote className="border-l-2 border-line pl-2 whitespace-pre-wrap">{item.evidence}</blockquote>}
      {item.reason && <p className="whitespace-pre-wrap">{item.reason}</p>}
      {item.suggestion && <p className="whitespace-pre-wrap">{item.suggestion}</p>}
    </li>)}
  </ul>;
}
