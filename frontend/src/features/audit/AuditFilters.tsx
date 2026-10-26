"use client";

import { Button, Field, Section, Select, TextInput } from "@/shared/ui";
import { ACTION_LABELS, OBJECT_TYPE_LABELS, RESULT_LABELS } from "./labels";
import type { AuditFilterForm, AuditResult } from "./types";

type Props = {
  value: AuditFilterForm;
  onChange: (value: AuditFilterForm) => void;
  onClear: () => void;
  /** Lỗi dưới ô email người thực hiện. */
  actorError?: string | null;
};

export const DATE_ORDER_ERROR = "Ngày bắt đầu phải trước ngày kết thúc.";

/**
 * Bộ lọc màn Audit Log (artboard a13): đổi ô nào lọc ngay ô đó, không có nút Lọc. Người thực hiện theo email,
 * hành động, đối tượng (loại), kết quả, thời gian.
 */
export function AuditFilters({ value, onChange, onClear, actorError }: Props) {
  const set = <K extends keyof AuditFilterForm>(key: K, next: AuditFilterForm[K]) => onChange({ ...value, [key]: next });
  const dateInvalid = value.fromDate !== "" && value.toDate !== "" && value.fromDate > value.toDate;

  return (
    <Section className="p-4">
      <div className="flex flex-col gap-3" data-testid="audit-filters-form">
        <div className="grid gap-3 md:grid-cols-3">
          <Field label="Người thực hiện" htmlFor="audit-actor" error={actorError}>
            <TextInput
              id="audit-actor"
              type="email"
              icon="person"
              placeholder="email@fpt.edu.vn"
              value={value.actorEmail}
              invalid={Boolean(actorError)}
              onChange={(e) => set("actorEmail", e.target.value)}
              data-testid="audit-filters-actor-input"
            />
          </Field>
          <Field label="Hành động" htmlFor="audit-action">
            <Select id="audit-action" value={value.action} onChange={(e) => set("action", e.target.value)} data-testid="audit-filters-action-select">
              <option value="">Tất cả</option>
              {Object.entries(ACTION_LABELS).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Đối tượng" htmlFor="audit-object-type">
            <Select
              id="audit-object-type"
              value={value.objectType}
              onChange={(e) => onChange({ ...value, objectType: e.target.value, objectId: "" })}
              data-testid="audit-filters-object-type-select"
            >
              <option value="">Tất cả</option>
              {Object.entries(OBJECT_TYPE_LABELS).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-wrap items-end gap-3">
            <div className="w-40">
              <Field label="Kết quả" htmlFor="audit-result">
                <Select
                  id="audit-result"
                  value={value.result}
                  onChange={(e) => set("result", e.target.value as AuditResult | "")}
                  data-testid="audit-filters-result-select"
                >
                  <option value="">Tất cả</option>
                  {(Object.keys(RESULT_LABELS) as AuditResult[]).map((code) => (
                    <option key={code} value={code}>
                      {RESULT_LABELS[code].label}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <Field label="Thời gian" htmlFor="audit-from" error={dateInvalid ? DATE_ORDER_ERROR : null}>
              <div className="flex items-center gap-2">
                <TextInput
                  id="audit-from"
                  type="date"
                  icon="calendar_month"
                  aria-label="Từ ngày"
                  value={value.fromDate}
                  invalid={dateInvalid}
                  onChange={(e) => set("fromDate", e.target.value)}
                  data-testid="audit-filters-from-input"
                />
                <span className="text-muted">–</span>
                <TextInput
                  type="date"
                  aria-label="Đến ngày"
                  value={value.toDate}
                  invalid={dateInvalid}
                  onChange={(e) => set("toDate", e.target.value)}
                  data-testid="audit-filters-to-input"
                />
              </div>
            </Field>
          </div>
          <Button type="button" variant="danger" icon="filter_alt_off" onClick={onClear} data-testid="audit-filters-clear-button">
            Xóa lọc
          </Button>
        </div>
      </div>
    </Section>
  );
}
