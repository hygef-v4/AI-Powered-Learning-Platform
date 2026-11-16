"use client";

import Link from "next/link";
import { useCallback, useEffect, useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button, ButtonLink, Card, cx, Dialog, Field, Icon, IconButton, LockedInput, PageHeader, Section, Select, showToast, TextInput } from "@/shared/ui";
import { getMyGroup, renameGroup, submitLeaderRequest } from "../api";
import { describeFormation } from "../model/proposals";
import { GroupProposalSection, useMyProposal } from "./GroupProposalSection";
import type { MyGroupView } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { initials } from "@/shared/layout/navigation";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import { msg } from "@/i18n/msg";

function useMyGroup(classId: string) {
  const [view, setView] = useState<MyGroupView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const reload = useCallback(
    () => getMyGroup(classId).then(setView, () => setError(msg("groups.components.MyGroup.khongTaiDuocNhomCuaBan"))),
    [classId],
  );
  useEffect(() => {
    let active = true;
    getMyGroup(classId).then(
      (loaded) => {
        if (active) setView(loaded);
      },
      () => {
        if (active) setError(msg("groups.components.MyGroup.khongTaiDuocNhomCuaBan"));
      },
    );
    return () => {
      active = false;
    };
  }, [classId]);
  return { view, error, reload };
}

/**
 * Thẻ Nhóm của tôi trên trang lớp của sinh viên, dưới thẻ Giảng viên: ba dòng Nhóm, Thành viên, Trưởng nhóm; dưới nhãn Trưởng
 * nhóm là chữ cái đầu, tên đậm, email nhạt như thẻ Giảng viên. Giãn hết phần cao còn lại của cột phải.
 */
export function MyGroupCard({ classId }: { classId: string }) {
  const { view } = useMyGroup(classId);
  const leader = view?.members.find((m) => m.leader);
  return (
    <Section
      title={msg("groups.components.MyGroup.nhomCuaToi")}
      className="flex-1"
      actions={view?.groupId ? <ButtonLink href={`/classes/${classId}/group`} size="sm">{msg("groups.components.MyGroup.moNhom")}</ButtonLink> : undefined}
    >
      {!view ? (
        <p role="status" className="text-[13px] text-muted">{msg("groups.components.MyGroup.dangTaiNhom")}</p>
      ) : view.groupId ? (
        <dl className="flex flex-col gap-2.5 text-[13px]">
          <div className="flex items-center justify-between gap-3"><dt className="text-muted">{msg("groups.components.MyGroup.nhom")}</dt><dd className="truncate font-semibold text-ink">{view.name}</dd></div>
          <div className="flex items-center justify-between gap-3"><dt className="text-muted">{msg("groups.components.MyGroup.thanhVien")}</dt><dd className="text-ink">{view.members.length}</dd></div>
          <div className="flex flex-col gap-2">
            <dt className="text-muted">{msg("groups.components.MyGroup.truongNhom")}</dt>
            <dd>
              {leader ? (
                <div className="flex items-center gap-3">
                  <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent-soft font-semibold text-accent">{initials(leader.name ?? "?")}</span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-ink">{leader.name ?? msg("groups.components.MyGroup.chuaRoTen")}</p>
                    <PersonSubline studentCode={leader.studentCode} email={leader.email} />
                  </div>
                </div>
              ) : <span className="text-muted">{msg("groups.components.MyGroup.chuaCo")}</span>}
            </dd>
          </div>
        </dl>
      ) : (
        <ProposalSummary classId={classId} />
      )}
    </Section>
  );
}

/** Thẻ Nhóm của tôi khi chưa có nhóm: đợt tự chia nhóm, đề xuất đang tham gia, số lời mời, lối vào trang Nhóm của tôi. */
function ProposalSummary({ classId }: { classId: string }) {
  const { view } = useMyProposal(classId);
  const proposal = view?.proposal;
  const invitations = view?.invitations.length ?? 0;
  const active = !!view && (view.formation.open || !!proposal || invitations > 0);
  return (
    <div className="flex flex-col gap-2.5 text-[13px]">
      <p className="text-muted">{msg("groups.components.MyGroup.banChuaCoNhomTrongLop")}</p>
      {view?.formation.enabled && <p className="text-ink">{msg("groups.components.MyGroup.tuChiaNhom")}{describeFormation(view.formation)}</p>}
      {proposal && (
        <p className="text-ink">
          {msg("groups.components.MyGroup.deXuat")}<span className="font-semibold">{proposal.name}</span> · {proposal.members.length} {msg("groups.components.MyGroup.thanhVien2")}</p>
      )}
      {invitations > 0 && <p className="text-ink">{msg("groups.components.MyGroup.banCo")}{invitations} {msg("groups.components.MyGroup.loiMoiVaoNhom")}</p>}
      {active && (
        <ButtonLink href={`/classes/${classId}/group`} size="sm" className="self-start" data-testid="my-group-open-proposal">
          {msg("groups.components.MyGroup.moNhomCuaToi")}</ButtonLink>
      )}
    </div>
  );
}

/**
 * Trang Nhóm của tôi (s03, UC 54). Thẻ nhóm: tên nhóm (trưởng nhóm sửa ngay tại chỗ, không cần giảng viên duyệt), số thành
 * viên và link sang bài nhóm trên trang Bài tập; bên dưới mỗi thành viên một thẻ đứng, trưởng nhóm có nhãn trên avatar.
 * Thành viên nào cũng gửi được yêu cầu đổi trưởng nhóm; đang có yêu cầu chờ thì nút khóa và ghi Đang chờ duyệt.
 */
export function MyGroupPage({ classId }: { classId: string }) {
  const { view, error, reload } = useMyGroup(classId);
  const [requesting, setRequesting] = useState(false);
  const reloadGroup = useCallback(() => void reload(), [reload]);

  if (error && !view) return <PageLoadError>{error}</PageLoadError>;
  if (!view) return <PageLoading />;
  const open = view.classStatus === "OPEN";

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title={msg("groups.components.MyGroup.nhomCuaToi")}
        actions={
          view.groupId && open ? (
            <Button icon={view.pendingRequest ? "schedule" : "swap_horiz"} onClick={() => setRequesting(true)} disabled={view.pendingRequest}
              data-testid="leader-request-open">
              {view.pendingRequest ? msg("groups.components.MyGroup.dangChoDuyet") : msg("groups.components.MyGroup.yeuCauDoiTruongNhom")}
            </Button>
          ) : undefined
        }
      />
      {!view.groupId ? (
        <GroupProposalSection classId={classId} onGrouped={reloadGroup} />
      ) : (
        <Card as="section" padding="none" className="flex flex-col gap-5 p-6" aria-label={msg("groups.components.MyGroup.thanhVienNhom")}>
          <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
            <div className="min-w-0 flex-1">
              <GroupName view={view} editable={view.amLeader && open} onRenamed={() => void reload()} />
              <p className="mt-0.5 text-[13px] text-muted">{view.members.length} {msg("groups.components.MyGroup.thanhVien2")}</p>
            </div>
            <Link href={`/my-assignments?classId=${encodeURIComponent(classId)}&mode=GROUP`} data-testid="group-works-link"
              className="inline-flex shrink-0 items-center gap-1.5 rounded-md px-2 py-1.5 text-[13px] font-semibold text-primary-ink transition-colors hover:bg-primary-soft">
              {msg("groups.components.MyGroup.baiNhom")}<Icon name="arrow_forward" size={15} />
            </Link>
          </div>
          <ul className="grid grid-cols-[repeat(auto-fill,minmax(156px,1fr))] gap-3">
            {view.members.map((m) => <MemberCard key={m.accountId} member={m} />)}
          </ul>
        </Card>
      )}
      {requesting && view.groupId && (
        <LeaderChangeRequestDialog
          view={view}
          onClose={() => setRequesting(false)}
          onSent={() => {
            setRequesting(false);
            void reload();
          }}
        />
      )}
    </div>
  );
}

/** Tên nhóm; trưởng nhóm bấm bút để sửa ngay trên dòng: Enter lưu, Esc thôi. */
function GroupName({ view, editable, onRenamed }: { view: MyGroupView; editable: boolean; onRenamed: () => void }) {
  const [draft, setDraft] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cancel = () => { setDraft(null); setError(null); };
  const save = async () => {
    const name = (draft ?? "").trim();
    if (!name || name.length > 100) { setError(msg("groups.components.MyGroup.tenNhomCan1100Ky")); return; }
    if (name === view.name) { cancel(); return; }
    setBusy(true);
    setError(null);
    try {
      await renameGroup(view.groupId!, name);
      setDraft(null);
      showToast(msg("groups.components.MyGroup.daDoiTenNhom"));
      onRenamed();
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? msg("groups.components.MyGroup.khongDoiDuocTenNhom")) : msg("groups.components.MyGroup.khongDoiDuocTenNhom"));
    } finally {
      setBusy(false);
    }
  };

  if (draft === null) {
    return (
      <div className="flex min-w-0 items-center gap-1">
        <h2 className="min-w-0 text-xl font-bold tracking-[-0.01em] text-ink [overflow-wrap:anywhere]">{view.name}</h2>
        {editable && <IconButton icon="edit" label={msg("groups.components.MyGroup.suaTenNhom")} onClick={() => setDraft(view.name ?? "")} data-testid="group-rename" />}
      </div>
    );
  }
  return (
    <form className="flex max-w-md flex-col gap-1.5" onSubmit={(event) => { event.preventDefault(); void save(); }}>
      <div className="flex items-center gap-2">
        <TextInput aria-label={msg("groups.components.MyGroup.tenNhom")} value={draft} maxLength={100} disabled={busy} invalid={!!error} autoFocus
          onChange={(event) => { setDraft(event.target.value); setError(null); }}
          onKeyDown={(event) => { if (event.key === "Escape") { event.preventDefault(); cancel(); } }} data-testid="group-rename-input" />
        <Button type="submit" variant="primary" size="sm" disabled={busy}>{msg("groups.components.MyGroup.luu")}</Button>
        <Button type="button" size="sm" disabled={busy} onClick={cancel}>{msg("groups.components.MyGroup.huy")}</Button>
      </div>
      {error && <p role="alert" className="text-xs text-danger">{error}</p>}
    </form>
  );
}

/** Thẻ đứng của một thành viên: nhãn Trưởng nhóm (ô trống ở người khác cho avatar thẳng hàng), avatar, tên, email. */
function MemberCard({ member }: { member: MyGroupView["members"][number] }) {
  return (
    <li data-testid="group-member"
      className={cx("flex aspect-[3/4] min-w-0 flex-col items-center justify-center gap-2.5 rounded-xl border px-3 py-5 text-center",
        member.leader ? "border-warning/50 bg-warning-soft/35" : "border-line bg-surface")}>
      <span className={cx("inline-flex h-5 items-center gap-1 rounded-full px-2.5 text-xs font-semibold", member.leader ? "bg-warning-soft text-warning" : "invisible")}>
        {member.leader && <><Icon name="crown" size={13} filled />{msg("groups.components.MyGroup.truongNhom")}</>}
      </span>
      <span aria-hidden="true"
        className={cx("flex size-16 shrink-0 items-center justify-center rounded-full text-lg font-bold",
          member.leader ? "bg-warning-soft text-warning" : "bg-primary-soft text-primary-ink")}>
        {initials(member.name ?? member.email ?? "?")}
      </span>
      <div className="flex min-w-0 flex-col gap-0.5">
        <p className="text-sm font-semibold text-ink [overflow-wrap:anywhere]">{member.name ?? msg("groups.components.MyGroup.chuaRoTen")}</p>
        <PersonSubline studentCode={member.studentCode} email={member.email} />
      </div>
    </li>
  );
}

/** Popup Yêu cầu đổi trưởng nhóm (s04, BR-U12-10): Trưởng nhóm hiện tại và Trưởng nhóm mới (tùy chọn); không gửi lý do. */
export function LeaderChangeRequestDialog({ view, onClose, onSent }: { view: MyGroupView; onClose: () => void; onSent: () => void }) {
  const fieldId = useId();
  const [proposed, setProposed] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const leader = view.members.find((m) => m.leader);
  const send = async () => {
    setBusy(true);
    setError(null);
    try {
      await submitLeaderRequest(view.groupId!, proposed || null);
      onSent();
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? msg("groups.components.MyGroup.khongGuiDuocYeuCau")) : msg("groups.components.MyGroup.khongGuiDuocYeuCau"));
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title={msg("groups.components.MyGroup.yeuCauDoiTruongNhom")}
      icon="swap_horiz"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button variant="primary" onClick={() => void send()} disabled={busy}>
            {msg("groups.components.MyGroup.guiYeuCau")}</Button>
        </>
      }
    >
      <Field label={msg("groups.components.MyGroup.truongNhomHienTai")} htmlFor={`${fieldId}-current`}>
        <LockedInput id={`${fieldId}-current`} value={leader ? (leader.name ?? leader.email ?? "") : msg("groups.components.MyGroup.chuaCo")} />
      </Field>
      <Field label={msg("groups.components.MyGroup.truongNhomMoi")} htmlFor={`${fieldId}-p`} error={error}>
        <Select id={`${fieldId}-p`} value={proposed} onChange={(e) => setProposed(e.target.value)}>
          <option value="">{msg("groups.components.MyGroup.khongDeXuat")}</option>
          {view.members.filter((m) => !m.leader).map((m) => (
            <option key={m.accountId} value={m.accountId}>
              {m.name ?? m.email}
            </option>
          ))}
        </Select>
      </Field>
    </Dialog>
  );
}
