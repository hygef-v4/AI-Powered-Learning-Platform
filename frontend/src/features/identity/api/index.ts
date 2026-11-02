import { apiFetch } from "@/shared/api/client";
import type { OtpPurpose, Profile, SessionInfo } from "../model/types";

// API U01. Phiên nằm trong cookie HttpOnly; frontend không đọc hay lưu token.

export function login(email: string, password: string) {
  return apiFetch<SessionInfo>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function logout() {
  return apiFetch<void>("/api/v1/auth/logout", { method: "POST" });
}

export function getProfile() {
  return apiFetch<Profile>("/api/v1/me/profile");
}

/** Chỉ tên và số điện thoại (BR-U01-52); số điện thoại rỗng là xóa. */
export function updateProfile(displayName: string, phoneNumber: string) {
  return apiFetch<Profile>("/api/v1/me/profile", {
    method: "PATCH",
    body: JSON.stringify({ displayName, phoneNumber }),
  });
}

/** Đổi xong các thiết bị khác bị đăng xuất, phiên hiện tại giữ nguyên (BR-U01-34). */
export function changePassword(currentPassword: string, newPassword: string) {
  return apiFetch<void>("/api/v1/me/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

const OTP_BASE: Record<OtpPurpose, string> = {
  ACTIVATION: "/api/v1/auth/activation",
  PASSWORD_RESET: "/api/v1/auth/password-reset",
};

/** Luôn trả 202 với cùng một câu, kể cả khi email không tồn tại (BR-U01-12). */
export function requestOtp(purpose: OtpPurpose, email: string) {
  return apiFetch<{ message: string }>(`${OTP_BASE[purpose]}/request`, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

/** OTP đúng thì nhận otpTicket để sang bước đặt mật khẩu (BR-U01-28). */
export function verifyOtp(purpose: OtpPurpose, email: string, otp: string) {
  return apiFetch<{ otpTicket: string }>(`${OTP_BASE[purpose]}/verify`, {
    method: "POST",
    body: JSON.stringify({ email, otp }),
  });
}

/** Kích hoạt xong backend đã tạo phiên (BR-U01-13). */
export function completeActivation(otpTicket: string, newPassword: string) {
  return apiFetch<SessionInfo>(`${OTP_BASE.ACTIVATION}/complete`, {
    method: "POST",
    body: JSON.stringify({ otpTicket, newPassword }),
  });
}

export function completePasswordReset(otpTicket: string, newPassword: string) {
  return apiFetch<void>(`${OTP_BASE.PASSWORD_RESET}/complete`, {
    method: "POST",
    body: JSON.stringify({ otpTicket, newPassword }),
  });
}
