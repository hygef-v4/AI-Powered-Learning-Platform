import { msg } from "@/i18n/msg";
import { formatNumber } from "@/i18n/number";

export const MAX_AI_REQUEST_LENGTH = 10_000;

export function AiRequestLength({ id, length }: { id: string; length: number }) {
  const count = { value: formatNumber(length), limit: formatNumber(MAX_AI_REQUEST_LENGTH) };
  if (length > MAX_AI_REQUEST_LENGTH) {
    return <p id={id} role="alert" className="text-xs text-danger">
      {msg("assignments.components.AiRequestLength.overLimit", {
        ...count, excess: String(length - MAX_AI_REQUEST_LENGTH),
      })}
    </p>;
  }
  return <p id={id} className="text-xs text-muted">{msg("assignments.components.AiRequestLength.count", count)}</p>;
}
