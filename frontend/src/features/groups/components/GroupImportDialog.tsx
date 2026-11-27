"use client";

import { useId, useRef, useState } from "react";
import { Alert, Button, Dialog, Pill, Select, TableFrame, Td, Th, showToast } from "@/shared/ui";
import { formatStudentLabel } from "@/shared/studentCode";
import { saveGroups } from "../api";
import { buildDraftsFrom, compareGroupName, formatPersonLabel, saveErrorText, type StudentRow } from "../model/groupDrafts";
import {
  buildGroupImport,
  describeRowIssue,
  downloadGroupTemplate,
  readGroupWorkbook,
  type GroupImportPreview,
  type ImportGroup,
} from "../model/groupWorkbook";
import type { Draft, GroupSetView } from "../model/types";
import { msg } from "@/i18n/msg";

/**
 * Popup Nhập từ Excel (BR-U12-99…103): tải mẫu điền sẵn sinh viên chưa có nhóm, chọn file, xem trước các nhóm mới kèm lỗi
 * từng dòng và cảnh báo, chọn trưởng nhóm còn thiếu, rồi lưu cả bộ nhóm một lần như chia ngẫu nhiên.
 */
export function GroupImportDialog({ classId, set, people, sizes, proposalOf, onClose, onSaved }: {
  classId: string;
  set: GroupSetView;
  people: StudentRow[];
  sizes?: { minSize: number; maxSize: number } | null;
  proposalOf?: Map<string, string>;
  onClose: () => void;
  onSaved: (next: GroupSetView) => void;
}) {
  const inputId = useId();
  const fileInput = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState("");
  const [preview, setPreview] = useState<GroupImportPreview | null>(null);
  const [leaders, setLeaders] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const drafts = buildDraftsFrom(set);
  const grouped = new Set(drafts.flatMap((d) => d.memberIds));
  const ungrouped = people
    .filter((p) => !grouped.has(p.accountId))
    .sort((a, b) => formatPersonLabel(a).localeCompare(formatPersonLabel(b), "vi"));
  const findPerson = (accountId: string) => people.find((p) => p.accountId === accountId);

  const downloadTemplate = async () => {
    setError(null);
    try {
      await downloadGroupTemplate(ungrouped, msg("groups.components.GroupImportDialog.templateFileName"));
    } catch {
      setError(msg("groups.components.GroupImportDialog.couldNotCreateTemplate"));
    }
  };

  const chooseFile = async (file: File | undefined) => {
    if (fileInput.current) fileInput.current.value = "";
    if (!file) return;
    setBusy(true);
    setError(null);
    setPreview(null);
    setLeaders({});
    try {
      const rows = await readGroupWorkbook(file);
      setFileName(file.name);
      setPreview(buildGroupImport(rows, { set, people, sizes, proposalOf }));
    } catch (cause) {
      setFileName("");
      setError(cause instanceof Error ? cause.message : msg("groups.model.groupWorkbook.couldNotRead"));
    } finally {
      setBusy(false);
    }
  };

  const leaderOf = (group: ImportGroup) => leaders[group.key] ?? group.leaderId;
  const newGroups = [...(preview?.groups ?? [])].sort(compareGroupName);
  const missingLeader = newGroups.some((g) => !leaderOf(g));
  const memberCount = newGroups.reduce((sum, g) => sum + g.memberIds.length, 0);

  const saveImport = async () => {
    if (!preview || newGroups.length === 0 || missingLeader) return;
    const next: Draft[] = [
      ...drafts,
      ...newGroups.map((g) => ({ key: g.key, id: null, name: g.name, leaderId: leaderOf(g), memberIds: g.memberIds })),
    ];
    setBusy(true);
    setError(null);
    try {
      onSaved(await saveGroups(classId, set.version, next));
      showToast(msg("groups.components.GroupImportDialog.imported", { count: String(newGroups.length) }));
    } catch (cause) {
      setError(saveErrorText(cause, next));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open title={msg("groups.components.GroupImportDialog.title")} size="lg" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button icon="upload_file" disabled={busy} onClick={() => fileInput.current?.click()}>
          {preview ? msg("groups.components.GroupImportDialog.chooseAnotherFile") : msg("groups.components.GroupImportDialog.chooseFile")}
        </Button>
        {preview && (
          <Button variant="primary" icon="save" disabled={busy || newGroups.length === 0 || missingLeader}
            onClick={() => void saveImport()} data-testid="group-import-save">
            {msg("groups.components.GroupImportDialog.saveGroups", { count: String(newGroups.length) })}
          </Button>
        )}
      </>}>
      <input ref={fileInput} id={inputId} type="file" accept=".xlsx" className="sr-only" disabled={busy}
        aria-label={msg("groups.components.GroupImportDialog.chooseFile")} data-testid="group-import-input"
        onChange={(event) => void chooseFile(event.target.files?.[0])} />
      {error && <Alert tone="danger" role="alert">{error}</Alert>}

      {!preview && (
        <div className="flex flex-col gap-3 text-[13.5px] text-body">
          <p>{msg("groups.components.GroupImportDialog.intro")}</p>
          <ol className="flex list-decimal flex-col gap-1 pl-5">
            <li>{msg("groups.components.GroupImportDialog.stepDownload", { count: String(ungrouped.length) })}</li>
            <li>{msg("groups.components.GroupImportDialog.stepFill")}</li>
            <li>{msg("groups.components.GroupImportDialog.stepUpload")}</li>
          </ol>
          <p className="text-xs text-muted">{msg("groups.components.GroupImportDialog.onlyNewGroups")}</p>
          <div>
            <Button size="sm" icon="download" onClick={() => void downloadTemplate()} data-testid="group-import-template">
              {msg("groups.components.GroupImportDialog.downloadTemplate")}
            </Button>
          </div>
        </div>
      )}

      {preview && (
        <div className="flex flex-col gap-3">
          <p className="text-[13px] text-muted">
            {msg("groups.components.GroupImportDialog.summary", {
              file: fileName,
              groups: String(newGroups.length),
              students: String(memberCount),
            })}
          </p>
          {preview.issues.length > 0 && (
            <Alert tone="warning" role="status">
              <p className="font-semibold">{msg("groups.components.GroupImportDialog.skippedRows", { count: String(preview.issues.length) })}</p>
              <ul className="mt-1 max-h-32 list-disc overflow-y-auto pl-5">
                {preview.issues.map((issue) => <li key={`${issue.row}-${issue.code}`}>{describeRowIssue(issue)}</li>)}
              </ul>
            </Alert>
          )}
          {newGroups.length === 0 ? (
            <p className="text-[13px] text-muted">{msg("groups.components.GroupImportDialog.noGroupsInFile")}</p>
          ) : (
            <div className="max-h-96 overflow-y-auto [scrollbar-color:var(--c-muted)_transparent]">
              <TableFrame filledHead className="[&_table]:table-fixed">
                <colgroup><col className="w-[160px]" /><col /><col className="w-[220px]" /></colgroup>
                <thead><tr>
                  <Th>{msg("groups.components.GroupImportDialog.group")}</Th>
                  <Th>{msg("groups.components.GroupImportDialog.members")}</Th>
                  <Th>{msg("groups.components.GroupImportDialog.leader")}</Th>
                </tr></thead>
                <tbody>
                  {newGroups.map((g) => {
                    const leaderId = leaderOf(g);
                    return (
                      <tr key={g.key}>
                        <Td className="align-top">
                          <span className="block truncate font-semibold" title={g.name}>{g.name}</span>
                          {g.sizeWarning && sizes && (
                            <Pill tone="warning">
                              {g.sizeWarning === "BELOW_MIN"
                                ? msg("groups.components.GroupImportDialog.belowMin", { min: String(sizes.minSize) })
                                : msg("groups.components.GroupImportDialog.aboveMax", { max: String(sizes.maxSize) })}
                            </Pill>
                          )}
                        </Td>
                        <Td className="align-top text-body">
                          <ul className="flex flex-col gap-0.5">
                            {g.memberIds.map((id) => {
                              const proposal = g.inProposal.find((p) => p.accountId === id);
                              return (
                                <li key={id} className="flex flex-wrap items-center gap-1.5">
                                  <span>{formatStudentLabel(findPerson(id) ?? { name: null, email: null })}</span>
                                  {id === leaderId && <Pill tone="warning">{msg("groups.components.GroupImportDialog.leader")}</Pill>}
                                  {proposal && (
                                    <span className="text-xs text-muted">
                                      {msg("groups.components.GroupImportDialog.inProposal", { name: proposal.proposalName })}
                                    </span>
                                  )}
                                </li>
                              );
                            })}
                          </ul>
                        </Td>
                        <Td className="align-top">
                          <Select aria-label={msg("groups.components.GroupImportDialog.leaderOf", { name: g.name })}
                            invalid={!leaderId} value={leaderId ?? ""} disabled={busy}
                            onChange={(event) => setLeaders((current) => ({ ...current, [g.key]: event.target.value }))}>
                            <option value="" disabled>{msg("groups.components.GroupImportDialog.chooseLeader")}</option>
                            {g.memberIds.map((id) => <option key={id} value={id}>{formatPersonLabel(findPerson(id))}</option>)}
                          </Select>
                          {!leaderId && (
                            <p className="mt-1 text-xs text-danger">
                              {g.leaderCount > 1
                                ? msg("groups.components.GroupImportDialog.multipleLeaders")
                                : msg("groups.components.GroupImportDialog.noLeader")}
                            </p>
                          )}
                        </Td>
                      </tr>
                    );
                  })}
                </tbody>
              </TableFrame>
            </div>
          )}
          {newGroups.some((g) => g.inProposal.length > 0) && (
            <p className="text-xs text-muted">{msg("groups.components.GroupImportDialog.proposalNote")}</p>
          )}
        </div>
      )}
    </Dialog>
  );
}
