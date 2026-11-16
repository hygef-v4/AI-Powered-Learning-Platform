"use client";

import { Button, DateInput, Field, Section, Select, TextInput } from "@/shared/ui";
import { ACTION_LABELS, OBJECT_TYPE_LABELS, RESULT_LABELS } from "../model/labels";
import type { AuditFilterForm, AuditResult } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = {
  value: AuditFilterForm;
  onChange: (value: AuditFilterForm) => void;
  onClear: () => void;
};

export const DATE_ORDER_ERROR = msg("audit.components.AuditFilters.ngayBatDauPhaiTruocNgay");

/**
 * Bộ lọc màn Audit Log (artboard a13): đổi ô nào lọc ngay ô đó, không có nút Lọc. Hàng trên 4 ô bằng nhau (người
 * thực hiện theo một phần email hay tên, hành động, đối tượng, kết quả); hàng dưới là khoảng thời gian và Xóa lọc.
 */
export function AuditFilters({ value, onChange, onClear }: Props) {
  const setField = <K extends keyof AuditFilterForm>(key: K, next: AuditFilterForm[K]) => onChange({ ...value, [key]: next });
  const dateInvalid = value.fromDate !== "" && value.toDate !== "" && value.fromDate > value.toDate;

  return (
    <Section className="p-4">
      <div className="flex flex-col gap-3" data-testid="audit-filters-form">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label={msg("audit.components.AuditFilters.nguoiThucHien")} htmlFor="audit-actor">
            <TextInput
              id="audit-actor"
              type="search"
              icon="person"
              placeholder="email@fpt.edu.vn"
              value={value.actorEmail}
              onChange={(e) => setField("actorEmail", e.target.value)}
              data-testid="audit-filters-actor-input"
            />
          </Field>
          <Field label={msg("audit.components.AuditFilters.hanhDong")} htmlFor="audit-action">
            <Select id="audit-action" value={value.action} onChange={(e) => setField("action", e.target.value)} data-testid="audit-filters-action-select">
              <option value="">{msg("audit.components.AuditFilters.tatCa")}</option>
              {Object.entries(ACTION_LABELS).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={msg("audit.components.AuditFilters.doiTuong")} htmlFor="audit-object-type">
            <Select
              id="audit-object-type"
              value={value.objectType}
              onChange={(e) => onChange({ ...value, objectType: e.target.value, objectId: "" })}
              data-testid="audit-filters-object-type-select"
            >
              <option value="">{msg("audit.components.AuditFilters.tatCa")}</option>
              {Object.entries(OBJECT_TYPE_LABELS).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={msg("audit.components.AuditFilters.ketQua")} htmlFor="audit-result">
            <Select
              id="audit-result"
              value={value.result}
              onChange={(e) => setField("result", e.target.value as AuditResult | "")}
              data-testid="audit-filters-result-select"
            >
              <option value="">{msg("audit.components.AuditFilters.tatCa")}</option>
              {(Object.keys(RESULT_LABELS) as AuditResult[]).map((code) => (
                <option key={code} value={code}>
                  {RESULT_LABELS[code].label}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <Field label={msg("audit.components.AuditFilters.thoiGian")} htmlFor="audit-from" error={dateInvalid ? DATE_ORDER_ERROR : null}>
            <div className="flex items-center gap-2">
              <DateInput
                id="audit-from"
                aria-label={msg("audit.components.AuditFilters.tuNgay")}
                value={value.fromDate}
                invalid={dateInvalid}
                onChange={(date) => setField("fromDate", date)}
                data-testid="audit-filters-from-input"
              />
              <span aria-hidden="true" className="text-muted">–</span>
              <DateInput
                aria-label={msg("audit.components.AuditFilters.denNgay")}
                value={value.toDate}
                invalid={dateInvalid}
                onChange={(date) => setField("toDate", date)}
                data-testid="audit-filters-to-input"
              />
            </div>
          </Field>
          <Button type="button" variant="danger" icon="filter_alt_off" onClick={onClear} data-testid="audit-filters-clear-button">
            {msg("audit.components.AuditFilters.xoaLoc")}</Button>
        </div>
      </div>
    </Section>
  );
}
