"use client";

import { subjectNavEntries } from "@/features/identity/slots";
import { createElement } from "react";
import { teacherClassTabs, studentClassSections, subjectSummarySections } from "@/features/academics/slots";
import { assignmentMaterialSections } from "@/features/assignmentwork/slots";
import { quizMaterialSections } from "@/features/quizpractice/slots";
import { ClassMaterialsTab } from "./MaterialList";
import { ClassMaterialsCard, MaterialLinkList, MaterialSummary } from "./MaterialSections";

subjectNavEntries.register({ key: "manager-materials", order: 50, title: "Học liệu", icon: "folder_open", path: "/manager/materials" });
teacherClassTabs.register({ key: "u13-materials", order: 50, component: ({ classId, tab }) => tab === "materials" ? createElement(ClassMaterialsTab, { classId }) : null });
studentClassSections.register({ key: "u13-materials", order: 10, component: ClassMaterialsCard });
subjectSummarySections.register({ key: "u13-materials", order: 30, component: MaterialSummary });
assignmentMaterialSections.register({ key: "u13-materials", order: 30, component: MaterialLinkList });
quizMaterialSections.register({ key: "u13-materials", order: 30, component: MaterialLinkList });
