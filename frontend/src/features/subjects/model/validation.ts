import { msg } from "@/i18n/msg";

export type SubjectFieldErrors = Partial<Record<"code" | "name" | "description", string>>;
export type SubjectInformationInput = { name: string; description?: string | null };

export function normalizeSubjectCode(code: string): string {
  return code.trim().toUpperCase();
}

export function validateSubjectInformation(input: SubjectInformationInput): SubjectFieldErrors {
  const errors: SubjectFieldErrors = {};
  const name = input.name.trim();
  if (!name || name.length > 200) errors.name = msg("subjects.model.validation.tenMonLaBatBuocVa");
  if (input.description != null && input.description.length > 2000) {
    errors.description = msg("subjects.model.validation.moTaMonToiDa2");
  }
  return errors;
}

export function validateCreateSubject(
  input: SubjectInformationInput & { code: string },
): SubjectFieldErrors {
  const errors = validateSubjectInformation(input);
  if (!/^[A-Z0-9-]{1,20}$/.test(normalizeSubjectCode(input.code))) {
    errors.code = msg("subjects.model.validation.maMonPhaiGom120");
  }
  return errors;
}
