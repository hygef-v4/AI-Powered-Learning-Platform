"use client";
import { useCallback, useEffect, useId, useState } from "react";
import { AutoTextarea } from "@/shared/document";
import { Button, Field, Select, TextInput } from "@/shared/ui";
import { gradingApi } from "../api";
import { formatErrorMessage } from "../model/format";
import type { Evaluation } from "../model/types";
import { CONTRIBUTION_LEVELS, type ContributionLevel } from "../model/contributionLevels";
import { msg } from "@/i18n/msg";

type MemberPart = { partId: string; title: string };
type AssessmentValues = { score: string; reason: string; level: ContributionLevel | "" };

function readMemberValues(member: Evaluation): AssessmentValues {
  return {
    score: String(member.score ?? ""),
    reason: member.memberOverride ? member.adjustmentReason ?? "" : "",
    level: member.contributionLevel ?? "",
  };
}

function isSameAssessment(left: AssessmentValues, right: AssessmentValues) {
  return left.score === right.score && left.reason === right.reason && left.level === right.level;
}

export function MemberContributionPanel({
  root,
  members,
  names,
  onSaved,
  editable = true,
  selectedAccountId,
  onSelectMember,
  sections = {},
  onShowPart,
  onDirtyChange,
}: {
  root: Evaluation;
  members: Evaluation[];
  names: Record<string, string>;
  onSaved: () => void;
  editable?: boolean;
  selectedAccountId?: string | null;
  onSelectMember?: (accountId: string) => void;
  sections?: Record<string, MemberPart[]>;
  onShowPart?: (partId: string) => void;
  onDirtyChange?: (dirty: boolean) => void;
}) {
  const selectorId = useId();
  const [selected, setSelected] = useState("");
  const [dirtyMembers, setDirtyMembers] = useState<Record<string, boolean>>({});
  const selectedIndex = Math.max(0, members.findIndex(member => member.accountId === (selectedAccountId ?? selected)));
  const current = members[selectedIndex];
  const hasChanges = members.some(member => dirtyMembers[member.id]);
  const handleDirtyChange = useCallback((id: string, dirty: boolean) => {
    setDirtyMembers(current => current[id] === dirty ? current : { ...current, [id]: dirty });
  }, []);
  useEffect(() => { onDirtyChange?.(hasChanges); }, [hasChanges, onDirtyChange]);
  function handleSelect(accountId: string) {
    setSelected(accountId);
    onSelectMember?.(accountId);
  }
  return (
    <section id="group-member-grades" aria-labelledby="group-member-grades-title" tabIndex={-1}
      className="@container min-w-0 scroll-mt-36">
      <h2 id="group-member-grades-title" className="sr-only">
        {msg("grading.components.MemberContributionPanel.memberScores")}</h2>
      <p className="mt-1 text-xs text-muted">
        {msg("grading.components.MemberContributionPanel.groupScore")}{root.score == null ? msg("grading.components.MemberContributionPanel.notGraded") : `${Number(root.score)} / ${Number(root.maxScore)}`}.
        {" "}{msg("grading.components.MemberContributionPanel.membersReceiveTheGroupScore")}</p>
      <details className="mt-3 rounded-lg border border-line-soft px-3 py-2 text-sm">
        <summary className="cursor-pointer font-semibold text-ink">{msg("grading.components.MemberContributionPanel.howToAssess")}</summary>
        <div className="mt-2 space-y-3 text-body">
          <p>{msg("grading.components.MemberContributionPanel.level1IsTheHighest")}</p>
          <ol className="list-decimal space-y-1 pl-5">
            {CONTRIBUTION_LEVELS.map((level) => (
              <li key={level.value}><strong>{level.label}:</strong> {level.description}</li>
            ))}
          </ol>
          <p>
            {msg("grading.components.MemberContributionPanel.gradeTheGroupWorkAgainst")}</p>
          <p>
            {msg("grading.components.MemberContributionPanel.exampleGroup810A")}</p>
          <p>
            {msg("grading.components.MemberContributionPanel.saveMemberScoresThenPublish")}</p>
        </div>
      </details>
      {current && <div className="mt-3 space-y-2">
        <Field label={msg("grading.components.MemberContributionPanel.member")} htmlFor={selectorId}>
          <Select id={selectorId} value={current.accountId ?? ""} onChange={event => handleSelect(event.target.value)}>
            {members.map(member => <option key={member.id} value={member.accountId ?? ""}>
              {names[member.accountId!] ?? msg("grading.components.MemberContributionPanel.member")}{dirtyMembers[member.id] ? msg("grading.components.MemberContributionPanel.notSaved") : ""}
            </option>)}
          </Select>
        </Field>
        <div className="flex items-center justify-between gap-2">
          <Button size="sm" icon="chevron_left" aria-label={msg("grading.components.MemberContributionPanel.previousMember")} disabled={selectedIndex <= 0}
            onClick={() => handleSelect(members[selectedIndex - 1]!.accountId!)}>{msg("grading.components.MemberContributionPanel.previous")}</Button>
          <span className="text-xs text-muted">{selectedIndex + 1} / {members.length}</span>
          <Button size="sm" icon="chevron_right" aria-label={msg("grading.components.MemberContributionPanel.nextMember")} disabled={selectedIndex >= members.length - 1}
            onClick={() => handleSelect(members[selectedIndex + 1]!.accountId!)}>Sau</Button>
        </div>
      </div>}
      <ul className="mt-3 divide-y divide-line-soft border-t border-line-soft">
        {members.map((member, index) => (
          <MemberRow
            key={member.id}
            hidden={index !== selectedIndex}
            rootId={root.id}
            locked={!editable || root.state === "SUPERSEDED"}
            member={member}
            name={names[member.accountId!] ?? msg("grading.components.MemberContributionPanel.member")}
            onSaved={onSaved}
            parts={sections[member.accountId!] ?? []}
            onShowPart={onShowPart}
            onDirtyChange={handleDirtyChange}
          />
        ))}
      </ul>
    </section>
  );
}
function MemberRow({
  rootId,
  locked,
  member,
  name,
  onSaved,
  hidden,
  parts,
  onShowPart,
  onDirtyChange,
}: {
  rootId: string;
  locked: boolean;
  member: Evaluation;
  name: string;
  onSaved: () => void;
  hidden: boolean;
  parts: MemberPart[];
  onShowPart?: (partId: string) => void;
  onDirtyChange: (id: string, dirty: boolean) => void;
}) {
  const fieldId = useId();
  const [editor, setEditor] = useState(() => ({
    values: readMemberValues(member), baseline: readMemberValues(member),
    version: member.version, seenVersion: member.version,
  }));
  const dirty = !isSameAssessment(editor.values, editor.baseline);
  if (member.version !== editor.seenVersion) {
    const incoming = readMemberValues(member);
    const canRebase = !dirty || isSameAssessment(incoming, editor.baseline);
    setEditor({
      seenVersion: member.version,
      version: canRebase ? member.version : editor.version,
      baseline: canRebase ? incoming : editor.baseline,
      values: dirty ? editor.values : incoming,
    });
  }
  useEffect(() => { onDirtyChange(member.id, dirty); }, [member.id, dirty, onDirtyChange]);
  const { score, reason, level } = editor.values;
  function updateValues(patch: Partial<AssessmentValues>) {
    setEditor(current => ({ ...current, values: { ...current.values, ...patch } }));
  }
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const disabled = locked || busy || member.state === "SUPERSEDED";
  async function handleSave() {
    if (disabled) return;
    setError("");
    setBusy(true);
    try {
      const saved = await gradingApi.saveMemberGrade(rootId, member.accountId!, editor.version,
        score, reason.trim() || null, level || null);
      setEditor(current => ({ ...current, version: saved.version,
        values: readMemberValues(saved), baseline: readMemberValues(saved) }));
      onSaved();
    } catch (e) {
      setError(formatErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  if (hidden) return null;
  return (
    <li className="py-3">
      <fieldset className="min-w-0">
        <legend className="mb-2 wrap-break-word text-sm font-semibold text-ink">
          {name} {dirty && <span className="text-xs font-normal text-warning">{msg("grading.components.MemberContributionPanel.notSaved2")}</span>}
        </legend>
        <div className="mb-3 space-y-1 text-xs">
          <p className="font-semibold text-body">{msg("grading.components.MemberContributionPanel.assignedPart")}</p>
          {parts.length ? parts.map(part => <button key={part.partId} type="button"
            aria-label={msg("grading.components.MemberContributionPanel.viewPart", { title: String(part.title) })} onClick={() => onShowPart?.(part.partId)}
            className="block cursor-pointer text-left text-primary-ink underline underline-offset-2 hover:text-ink">
            {part.title}
          </button>) : <p className="text-muted">{msg("grading.components.MemberContributionPanel.noInformationAboutTheAssigned")}</p>}
        </div>
        <div className="grid items-end gap-3 @lg:grid-cols-[minmax(0,1fr)_7rem]">
          <Field label={msg("grading.components.MemberContributionPanel.contributionLevelOptional")} htmlFor={`${fieldId}-level`}>
            <Select id={`${fieldId}-level`} disabled={disabled} value={level}
              onChange={(event) => updateValues({ level: event.target.value as ContributionLevel | "" })}>
              <option value="">{msg("grading.components.MemberContributionPanel.notAssessed")}</option>
              {CONTRIBUTION_LEVELS.map((option, index) => (
                <option key={option.value} value={option.value}>{index + 1}. {option.label}</option>
              ))}
            </Select>
          </Field>
          <Field label={msg("grading.components.MemberContributionPanel.individualScore")} htmlFor={`${fieldId}-score`}>
            <TextInput
              id={`${fieldId}-score`}
              type="number"
              disabled={disabled}
              min="0"
              max={Number(member.maxScore)}
              step="0.01"
              value={score}
              onChange={(event) => updateValues({ score: event.target.value })}
            />
          </Field>
          <div className="@lg:col-span-2">
            <Field label={msg("grading.components.MemberContributionPanel.groundsForAssessment")} htmlFor={`${fieldId}-reason`}>
              <div className="rounded-lg border border-field p-2 text-sm focus-within:border-primary">
              <AutoTextarea
                id={`${fieldId}-reason`}
                value={reason}
                maxLength={2000}
                placeholder={msg("grading.components.MemberContributionPanel.assignedPartQualityOfCompletion")}
                disabled={disabled}
                onChange={(event) => updateValues({ reason: event.target.value })}
              />
              </div>
            </Field>
          </div>
          <Button
            disabled={disabled || rootId === member.id || score === ""}
            onClick={() => void handleSave()}
            className="h-10 justify-self-start"
          >
            {msg("grading.components.MemberContributionPanel.saveMemberScore")}</Button>
        </div>
        {error && <p role="alert" className="mt-2 text-xs text-danger">{error}</p>}
      </fieldset>
    </li>
  );
}
