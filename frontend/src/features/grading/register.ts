"use client";
import { createElement } from "react";
import { teacherClassTabs, studentDetailSections } from "@/features/academics/slots";
import { assignmentRowActions } from "@/features/assignments/slots";
import {
  studentAssignmentResults,
  submissionResultSections,
} from "@/features/assignmentwork/slots";
import { ClassGradebookTab } from "./ClassGradebookTab";
import { ClassProgressSection } from "./ClassProgressSection";
import { StudentGradesSection } from "./StudentGradesSection";
import { SubmissionsLink } from "./SubmissionsLink";
import { StudentGradeCell } from "./StudentGradeCell";
import { StudentGradePanel } from "./StudentGradePanel";

function TeacherGradingSection({
  classId,
  tab,
}: {
  classId: string;
  tab: "info" | "students" | "materials" | "evals" | "gradebook";
}) {
  return tab === "gradebook"
    ? createElement(ClassGradebookTab, { classId })
    : tab === "info"
      ? createElement(ClassProgressSection, { classId, layout: "overview" })
      : null;
}
teacherClassTabs.register({ key: "u09-grading", order: 90, component: TeacherGradingSection });
studentDetailSections.register({
  key: "u09-student-grades",
  order: 90,
  component: StudentGradesSection,
});
assignmentRowActions.register({ key: "u09-submissions", order: 90, component: SubmissionsLink });
studentAssignmentResults.register({
  key: "u09-student-grade",
  order: 90,
  component: StudentGradeCell,
});
submissionResultSections.register({
  key: "u09-attempt-grade",
  order: 90,
  component: StudentGradePanel,
});
