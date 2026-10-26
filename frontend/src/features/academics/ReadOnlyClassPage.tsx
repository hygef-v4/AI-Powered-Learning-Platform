"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { studentClassSections, teacherClassTabs } from "./slots";
import { StudentDetailDialog } from "./ClassDialogs";
import { EnrollmentPanel } from "./EnrollmentPanel";
import { EmptyState, PageHeading, StatusBadge, Tabs } from "./ui";
import type { CourseClassView, EnrollmentView } from "./types";
import styles from "./academics.module.css";

type ClassTab = "info" | "students" | "materials" | "evals" | "gradebook";
const CLASS_TABS: readonly ClassTab[] = ["info", "students", "materials", "evals", "gradebook"];
const DESCRIPTION_PREVIEW_LIMIT = 140;

/** Tab mở sẵn theo `?tab=` (vd. nút Mở tab Sinh viên ở Kiểm tra sẵn sàng bài nhóm); trang chỉ dựng ở client sau khi tải lớp. */
function initialTab(): ClassTab {
  const wanted = typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("tab");
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
  const [tab, setTab] = useState<ClassTab>(initialTab);
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
      {audience === "teacher" && <Tabs items={tabs} value={tab} onChange={setTab} label="Nội dung lớp" />}
      {audience === "student" && (
        <div className={styles.studentLayout}>
          <div className={styles.stack}>
            <SlotOutlet slot={studentClassSections} props={{ classId: value.id }} />
          </div>
          <aside className={styles.panel}>
            <h2>Thông tin lớp</h2>
            <dl className={styles.compactReadList}>
              <dt>Giảng viên</dt>
              <dd>{value.teacher ? value.teacher.name : "Chưa phân công"}</dd>
              {value.teacher && <><dt>Email</dt><dd>{value.teacher.email}</dd></>}
              <dt>Mã lớp</dt>
              <dd className={styles.code}>{value.code}</dd>
              <dt>Học kỳ</dt>
              <dd>{value.term}</dd>
              {value.description && <><dt>Mô tả</dt><dd className={styles.descriptionText}>{value.description}</dd></>}
            </dl>
          </aside>
        </div>
      )}
      {audience === "teacher" && tab === "info" && (
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
              <dt>Môn</dt>
              <dd>
                {value.subject.code} · {value.subject.name}
              </dd>
              <dt>Mã lớp</dt>
              <dd className={styles.code}>{value.code}</dd>
              <dt>Học kỳ</dt>
              <dd>{value.term}</dd>
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
      {audience === "teacher" && tab === "students" && (
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
      {audience === "teacher" && (tab === "materials" || tab === "evals" || tab === "gradebook") && (
        <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab }} />
      )}
      {detailError && <p role="alert">{detailError}</p>}
      {detailBusy && <p role="status">Đang tải sinh viên…</p>}
      {selected && (
        <StudentDetailDialog classId={value.id} student={selected} onClose={() => setSelected(null)} />
      )}
    </>
  );
}
