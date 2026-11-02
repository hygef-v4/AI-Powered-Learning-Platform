import { Alert } from "@/shared/ui";
import type { SimilarQuestion } from "../model/types";

/** Cảnh báo câu gần giống trong ngân hàng (BR-U10-08): chỉ để người soạn và người duyệt cân nhắc, không chặn. */
export function SimilarQuestionsAlert({ similar }: { similar: SimilarQuestion[] }) {
  if (similar.length === 0) return null;
  return (
    <div data-testid="similar-questions">
      <Alert tone="warning" title="Có câu gần giống trong ngân hàng">
        <ul className="list-disc pl-4">
          {similar.map((s) => <li key={s.id}>{s.percent}%: <span className="line-clamp-2 inline whitespace-pre-wrap">{s.stem}</span></li>)}
        </ul>
      </Alert>
    </div>
  );
}
