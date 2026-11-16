import { msg } from "@/i18n/msg";

// Kiểm sơ bộ phía client cùng quy tắc với PasswordPolicy của backend (BR-U01-30, 31); backend vẫn là nơi quyết định.

export type PasswordChecks = {
  minLength: boolean;
  letterAndDigit: boolean;
};

export function checkPassword(password: string): PasswordChecks {
  return {
    minLength: [...password].length >= 8,
    letterAndDigit: /\p{L}/u.test(password) && /[0-9]/.test(password),
  };
}

export function isPasswordAcceptable(password: string): boolean {
  const checks = checkPassword(password);
  return checks.minLength && checks.letterAndDigit;
}

/** Lý do backend từ chối (mã PasswordPolicy.Violation) bằng tiếng Việt. */
export const PASSWORD_VIOLATION_MESSAGES: Record<string, string> = {
  TOO_SHORT: msg("identity.model.passwordRules.matKhauCanItNhat8"),
  TOO_LONG: msg("identity.model.passwordRules.matKhauQuaDaiToiDa"),
  NEED_LETTER: msg("identity.model.passwordRules.matKhauCanCoChuCai"),
  NEED_DIGIT: msg("identity.model.passwordRules.matKhauCanCoChuSo"),
  CONTAINS_EMAIL_NAME: msg("identity.model.passwordRules.matKhauKhongDuocChuaTen"),
};
