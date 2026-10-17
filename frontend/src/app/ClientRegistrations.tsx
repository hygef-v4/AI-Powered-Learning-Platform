"use client";

// Nạp phần đăng ký slot cả ở trình duyệt: component client (ví dụ SettingsSectionHost) đọc registry
// trong bundle client, còn import ở layout chỉ chạy phía server.
import "@/features/register";

export function ClientRegistrations() {
  return null;
}
