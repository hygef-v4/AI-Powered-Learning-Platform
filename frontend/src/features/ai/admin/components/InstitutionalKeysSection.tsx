"use client";

import { KeysSection, type KeysApi } from "../../components/KeysSection";
import {
  addInstitutional,
  getInstitutional,
  revealInstitutional,
  revokeInstitutional,
  saveInstitutional,
  setInstitutionalEnabled,
  testInstitutional,
  testInstitutionalDraft,
} from "../api";

const API: KeysApi = {
  list: async () => (await getInstitutional()).filter((c) => c.configured && c.id),
  add: addInstitutional,
  save: saveInstitutional,
  reveal: revealInstitutional,
  test: testInstitutional,
  testDraft: testInstitutionalDraft,
  setEnabled: setInstitutionalEnabled,
  revoke: revokeInstitutional,
};

/** Khóa API của trường (UC 14, 15): nhiều khóa cho mỗi nhà cung cấp, cùng bảng với khóa cá nhân. */
export function InstitutionalKeysSection() {
  return <KeysSection title="Khóa API của trường" api={API} revokeTitle="Thu hồi khóa của trường?"
    loadError="Không tải được khóa API của trường, thử lại sau." />;
}
