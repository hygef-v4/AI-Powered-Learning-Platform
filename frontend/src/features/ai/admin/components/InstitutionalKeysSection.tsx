"use client";

import { KeysSection, type KeysApi } from "../../components/KeysSection";
import {
  addInstitutional,
  getInstitutional,
  revokeInstitutional,
  saveInstitutional,
  setInstitutionalEnabled,
  testInstitutional,
  testInstitutionalDraft,
} from "../api";
import { msg } from "@/i18n/msg";

const API: KeysApi = {
  list: async () => (await getInstitutional()).filter((c) => c.configured && c.id),
  add: addInstitutional,
  save: saveInstitutional,
  test: testInstitutional,
  testDraft: testInstitutionalDraft,
  setEnabled: setInstitutionalEnabled,
  revoke: revokeInstitutional,
};

/** Khóa API của trường (UC 14, 15): nhiều khóa cho mỗi nhà cung cấp, cùng bảng với khóa cá nhân. */
export function InstitutionalKeysSection() {
  return <KeysSection title={msg("ai.admin.components.InstitutionalKeysSection.khoaApiCuaTruong")} api={API} revokeTitle={msg("ai.admin.components.InstitutionalKeysSection.thuHoiKhoaCuaTruong")}
    loadError={msg("ai.admin.components.InstitutionalKeysSection.khongTaiDuocKhoaApiCua")} />;
}
