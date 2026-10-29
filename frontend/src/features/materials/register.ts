"use client";

import { contextNavEntries, subjectNavEntries } from "@/features/identity/slots";
import { createElement } from "react";
import { teacherClassInfoFields, teacherClassTabs, studentClassSections } from "@/features/academics/slots";
import { assignmentMaterialSections } from "@/features/assignmentwork/slots";
import { quizMaterialSections } from "@/features/quizpractice/slots";
import { ClassMaterialsTab } from "./MaterialList";
import { moduleContext } from "./moduleNav";
import { ClassMaterialCountField, ClassMaterialsCard, MaterialLinkList } from "./MaterialSections";

// Trang xem, sửa một học liệu: sidebar là danh sách module (trước sidebar của lớp, của môn).
contextNavEntries.register({ key: "material-modules", order: 5, roles: ["SUBJECT_MANAGER", "TEACHER", "STUDENT"], resolve: moduleContext });
subjectNavEntries.register({ key: "manager-materials", order: 30, title: "Học liệu", icon: "folder_open", path: "/manager/materials" });
teacherClassInfoFields.register({ key: "u13-material-count", order: 10, component: ClassMaterialCountField });
teacherClassTabs.register({ key: "u13-materials", order: 50, component: ({ classId, tab }) => tab === "materials" ? createElement(ClassMaterialsTab, { classId }) : null });
studentClassSections.register({ key: "u13-materials", order: 10, component: ClassMaterialsCard });
assignmentMaterialSections.register({ key: "u13-materials", order: 30, component: MaterialLinkList });
quizMaterialSections.register({ key: "u13-materials", order: 30, component: MaterialLinkList });
