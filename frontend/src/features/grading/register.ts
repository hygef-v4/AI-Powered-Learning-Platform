"use client";
import { createElement } from "react";
import { teacherClassTabs, studentDetailSections } from "@/features/academics/slots";
import { assignmentRowActions } from "@/features/assignments/slots";
import { attemptResultSources, studentAssignmentResults } from "@/features/assignmentwork/slots";
import { ClassGradebookTab } from "./ClassGradebookTab";
import { ClassProgressSection } from "./ClassProgressSection";
import { StudentGradesSection } from "./StudentGradesSection";
import { SubmissionsLink } from "./SubmissionsLink";
import { StudentGradeCell } from "./StudentGradeCell";
import { gradingApi } from "./api";

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
attemptResultSources.register({
  key: "u09-attempt-grade",
  order: 90,
  load: async (attemptId) => {
    const grade = await gradingApi.attempt(attemptId);
    return {
      score: grade.score == null ? null : Number(grade.score),
      maxScore: grade.maxScore == null ? null : Number(grade.maxScore),
      publishedAt: grade.publishedAt,
      updated: grade.updated,
      memberOverride: !!grade.memberOverride,
      feedback: grade.feedback,
      summary: grade.structuredSummary ?? null,
      items: (grade.rubricItems ?? []).map((item) => ({
        rubricId: item.rubricId, partId: item.partId, criterionTitle: item.criterionTitle, itemText: item.itemText,
        awarded: Number(item.awardedPoints), max: Number(item.maxPoints),
        evidence: item.evidence, reason: item.reason, suggestion: item.suggestion,
      })),
    };
  },
});
