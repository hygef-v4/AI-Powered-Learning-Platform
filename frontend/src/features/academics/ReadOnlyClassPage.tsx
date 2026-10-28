"use client";

import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { teacherClassTabs } from "./slots";
import { StudentClassPage } from "./StudentClassPage";
import { StudentDetailDialog } from "./ClassDialogs";
import { EnrollmentPanel } from "./EnrollmentPanel";
import { EmptyState, PageHeading, StatusBadge, Tabs } from "./ui";
import type { CourseClassView, EnrollmentView } from "./types";
import styles from "./academics.module.css";

type ClassTab = "info" | "students" | "materials" | "evals" | "gradebook";
const CLASS_TABS: readonly ClassTab[] = ["info", "students", "materials", "evals", "gradebook"];
const DESCRIPTION_PREVIEW_LIMIT = 140;

/** Tab theo `?tab=` (sidebar của lớp, nút Mở tab Sinh viên ở Kiểm tra sẵn sàng bài nhóm); thiếu hay sai thì là Thông tin lớp. */
function tabOf(wanted: string | null | undefined): ClassTab {
  return CLASS_TABS.find((t) => t === wanted) ?? "info";
}

export function ReadOnlyClassPage({
  audience,
  value,
  students,
  onBack,
  loadStudent,
}: {
  audience: "student" | "teacher";
  value: CourseClassView;
  students?: EnrollmentView[];
  loadStudent?: (classId: string, accountId: string, signal?: AbortSignal) => Promise<EnrollmentView>;
  onBack: () => void;
}) {
  const params = useSearchParams();
  const urlTab = tabOf(params?.get("tab"));
  const [tab, setTab] = useState<ClassTab>(urlTab);
  // Bấm mục trên sidebar của lớp thì `?tab=` đổi: mở theo.
  const [seenUrlTab, setSeenUrlTab] = useState(urlTab);
  if (urlTab !== seenUrlTab) {
    setSeenUrlTab(urlTab);
    setTab(urlTab);
  }
  // Giữ tab trên URL để tải lại hoặc gửi link vẫn mở đúng tab.
  const openTab = (next: ClassTab) => {
    setTab(next);
    const url = new URL(window.location.href);
    url.searchParams.set("tab", next);
    window.history.replaceState(window.history.state, "", url);
  };
  const [selected, setSelected] = useState<EnrollmentView | null>(null);
  const [detailError, setDetailError] = useState("");
  const [detailBusy, setDetailBusy] = useState(false);
  const [groupNames, setGroupNames] = useState<{ classId: string; names: Record<string, string> } | null>(null);
  const [expandedDescClassId, setExpandedDescClassId] = useState<string | null>(null);
  const onGroupNamesLoaded = useCallback(
    (names: Record<string, string>) => setGroupNames({ classId: value.id, names }),
    [value.id],
  );
  const detailRequest = useRef<AbortController | null>(null);
  useEffect(() => () => detailRequest.current?.abort(), []);

  async function selectStudent(student: EnrollmentView) {
    detailRequest.current?.abort();
    const request = new AbortController();
    detailRequest.current = request;
    setDetailError("");
    setDetailBusy(true);
    try {
      const detail = loadStudent ? await loadStudent(value.id, student.accountId, request.signal) : student;
      if (!request.signal.aborted) setSelected(detail);
    } catch {
      if (!request.signal.aborted) setDetailError("Không tải được thông tin sinh viên.");
    } finally {
      if (!request.signal.aborted) setDetailBusy(false);
    }
  }

  const enrollmentRows = students ?? [];
  const activeStudents = enrollmentRows.filter((student) => student.status === "ACTIVE");
  const studentSummaryCount = students !== undefined ? activeStudents.length : value.activeStudentCount;
  const normalizedDescription = (value.description ?? "").trim();
  const isLongDescription = normalizedDescription.length > DESCRIPTION_PREVIEW_LIMIT;
  const isDescriptionExpanded = expandedDescClassId === value.id;

  if (audience === "student" && value.status !== "OPEN") {
    return (
      <>
        <PageHeading title="Chi tiết lớp" onBack={onBack} />
        <section className={styles.panel}>
          <EmptyState
            title="Lớp hiện không khả dụng"
          />
        </section>
      </>
    );
  }

  if (audience === "student") return <StudentClassPage value={value} tab={tab === "materials" ? "materials" : "info"} />;

  const tabs = [
    { key: "info" as const, label: "Thông tin lớp" },
    { key: "students" as const, label: "Sinh viên", count: studentSummaryCount },
    { key: "materials" as const, label: "Học liệu" },
    { key: "evals" as const, label: "Đánh giá" },
    { key: "gradebook" as const, label: "Sổ điểm" },
  ];

  return (
    <>
      <PageHeading
        title={
          <>
            {value.subject.name}
            {value.status !== "OPEN" && <StatusBadge status={value.status} />}
          </>
        }
        subtitle={`${value.subject.code} · ${value.code} · ${value.term}`}
        onBack={onBack}
      />
      {/* Màn rộng đã có các tab này trên sidebar của lớp. */}
      <div className="md:hidden"><Tabs items={tabs} value={tab} onChange={openTab} label="Nội dung lớp" /></div>
      {detailError && <p role="alert" className="text-sm text-danger">{detailError}</p>}
      {detailBusy && <p role="status" className="text-sm text-muted">Đang tải sinh viên…</p>}
      {(
        // Đổi tab thì dựng lại khung này để nội dung mới mờ vào, khớp với gạch chân đang trượt.
        <div key={tab} role="tabpanel" aria-label={tabs.find((t) => t.key === tab)?.label} className={styles.tabPanel}>
        {tab === "info" && (
          <div className={styles.teacherOverviewGrid} data-teacher-overview="true">
            <div
              data-testid="overview-students-card"
              data-class-overview-region="students"
              className={styles.overviewSummaryCard}
            >
              <p className={styles.overviewSummaryLabel}>Sinh viên</p>
              <p className={styles.overviewSummaryValue}>{studentSummaryCount}</p>
              <p className={styles.overviewSummaryMeta}>Đang học trong lớp</p>
            </div>
            <section
              data-testid="overview-metadata-panel"
              data-class-overview-region="metadata"
              className={`${styles.panel} ${styles.overviewMetadataPanel}`}
            >
              <h2>Thông tin lớp</h2>
              <dl className={styles.compactReadList}>
                <dt>Giảng viên</dt>
                <dd>{value.teacher ? `${value.teacher.name} · ${value.teacher.email}` : "Chưa phân công"}</dd>
                <dt>Trạng thái</dt>
                <dd>
                  <StatusBadge status={value.status} />
                </dd>
                <dt>Mô tả</dt>
                <dd>
                  {!normalizedDescription ? (
                    "Chưa có mô tả."
                  ) : !isLongDescription ? (
                    <span className={styles.descriptionText}>{normalizedDescription}</span>
                  ) : (
                    <div className={styles.descriptionBlock}>
                      <span className={styles.descriptionText}>
                        {isDescriptionExpanded
                          ? normalizedDescription
                          : `${normalizedDescription.slice(0, DESCRIPTION_PREVIEW_LIMIT).trimEnd()}…`}
                      </span>
                      <button
                        type="button"
                        className={styles.textButton}
                        aria-expanded={isDescriptionExpanded}
                        onClick={() => setExpandedDescClassId(isDescriptionExpanded ? null : value.id)}
                      >
                        {isDescriptionExpanded ? "Thu gọn" : "Xem đầy đủ"}
                      </button>
                    </div>
                  )}
                </dd>
              </dl>
            </section>
            <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab: "info" as const }} />
          </div>
        )}
        {tab === "students" && (
          <>
            <EnrollmentPanel
              students={activeStudents}
              groupNames={groupNames?.classId === value.id ? groupNames.names : undefined}
              onView={(student) => void selectStudent(student)}
            />
            <SlotOutlet
              slot={teacherClassTabs}
              props={{ classId: value.id, tab: "students" as const, onGroupNamesLoaded }}
            />
          </>
        )}
        {(tab === "materials" || tab === "evals" || tab === "gradebook") && (
          <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab }} />
        )}
        </div>
      )}
      {selected && (
        <StudentDetailDialog classId={value.id} student={selected} onClose={() => setSelected(null)} />
      )}
    </>
  );
}
