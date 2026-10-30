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

export function passwordAcceptable(password: string): boolean {
  const checks = checkPassword(password);
  return checks.minLength && checks.letterAndDigit;
}

/** Lý do backend từ chối (mã PasswordPolicy.Violation) bằng tiếng Việt. */
export const PASSWORD_VIOLATION_MESSAGES: Record<string, string> = {
  TOO_SHORT: "Mật khẩu cần ít nhất 8 ký tự.",
  TOO_LONG: "Mật khẩu quá dài (tối đa 72 byte).",
  NEED_LETTER: "Mật khẩu cần có chữ cái.",
  NEED_DIGIT: "Mật khẩu cần có chữ số.",
  CONTAINS_EMAIL_NAME: "Mật khẩu không được chứa tên trong email.",
};
