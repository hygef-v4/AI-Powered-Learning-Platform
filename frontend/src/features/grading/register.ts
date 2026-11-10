"use client";
import { createElement } from "react";
import { teacherClassTabs, studentDetailSections } from "@/features/academics/slots";
import { assignmentRowActions, assignmentSettingsSections } from "@/features/assignments/slots";
import { attemptResultSources, studentAssignmentResults, studentAssignmentFeedback, studentGradeDistributions } from "@/features/assignmentwork/slots";
import { ClassGradebookTab } from "./components/ClassGradebookTab";
import { ClassProgressSection } from "./components/ClassProgressSection";
import { StudentGradesSection } from "./components/StudentGradesSection";
import { SubmissionsLink } from "./components/SubmissionsLink";
import { StudentGradeCell } from "./components/StudentGradeCell";
import { gradingApi } from "./api";
import { GradeDistributionSettings } from "./components/GradeDistributionSettings";

function TeacherGradingSection({
  classId,
  tab,
  headerActions,
}: {
  classId: string;
  tab: "info" | "students" | "materials" | "evals" | "gradebook";
  headerActions?: HTMLElement | null;
}) {
  return tab === "gradebook"
    ? createElement(ClassGradebookTab, { classId, headerActions })
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
assignmentSettingsSections.register({ key: "u09-distribution-visibility", order: 90, component: GradeDistributionSettings });
studentAssignmentResults.register({
  key: "u09-student-grade",
  order: 90,
  component: StudentGradeCell,
});
attemptResultSources.register({
  key: "u09-attempt-grade",
  order: 90,
  load: async (attemptId) => {
    const grade = await gradingApi.loadAttemptGrade(attemptId);
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

import { StudentPublishedFeedback, StudentGradeDistribution } from "./components/StudentPublishedFeedback";
studentAssignmentFeedback.register({ key: "u09-published-feedback", order: 90, component: StudentPublishedFeedback });
studentGradeDistributions.register({ key: "u09-grade-distribution", order: 90, component: StudentGradeDistribution });
