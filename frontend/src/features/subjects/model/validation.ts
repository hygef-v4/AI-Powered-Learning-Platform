export type SubjectFieldErrors = Partial<Record<"code" | "name" | "description", string>>;
export type SubjectInformationInput = { name: string; description?: string | null };

export function normalizeSubjectCode(code: string): string {
  return code.trim().toUpperCase();
}

export function validateSubjectInformation(input: SubjectInformationInput): SubjectFieldErrors {
  const errors: SubjectFieldErrors = {};
  const name = input.name.trim();
  if (!name || name.length > 200) errors.name = "Tên môn là bắt buộc và tối đa 200 ký tự";
  if (input.description != null && input.description.length > 2000) {
    errors.description = "Mô tả môn tối đa 2.000 ký tự";
  }
  return errors;
}

export function validateCreateSubject(input: SubjectInformationInput & { code: string }): SubjectFieldErrors {
  const errors = validateSubjectInformation(input);
  if (!/^[A-Z0-9-]{1,20}$/.test(normalizeSubjectCode(input.code))) {
    errors.code = "Mã môn phải gồm 1–20 ký tự chữ hoa, số hoặc dấu -";
  }
  return errors;
}
