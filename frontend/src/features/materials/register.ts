"use client";

import { contextNavEntries, subjectNavEntries } from "@/features/identity/slots";
import { createElement } from "react";
import { teacherClassTabs, studentClassSections } from "@/features/academics/slots";
import { assignmentMaterialSections } from "@/features/assignmentwork/slots";
import { ClassMaterialsTab } from "./components/MaterialList";
import { resolveModuleContext } from "./components/ModuleNav";
import { ClassMaterialsCard, MaterialLinkList } from "./components/MaterialSections";
import { msg } from "@/i18n/msg";

// Trang xem, sửa một học liệu: sidebar là danh sách module (trước sidebar của lớp, của môn).
contextNavEntries.register({
  key: "material-modules",
  order: 5,
  roles: ["SUBJECT_MANAGER", "TEACHER", "STUDENT"],
  resolve: resolveModuleContext,
});
subjectNavEntries.register({ key: "manager-materials", order: 30, title: msg("materials.register.learningMaterial"), icon: "folder_open", path: "/manager/materials" });
teacherClassTabs.register({ key: "u13-materials", order: 50, component: ({ classId, tab }) => tab === "materials" ? createElement(ClassMaterialsTab, { classId }) : null });
studentClassSections.register({ key: "u13-materials", order: 10, component: ClassMaterialsCard });
assignmentMaterialSections.register({ key: "u13-materials", order: 30, component: MaterialLinkList });
