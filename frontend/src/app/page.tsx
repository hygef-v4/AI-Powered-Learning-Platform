import { PostLoginRedirect } from "@/features/identity/components/PostLoginRedirect";

// Trang gốc: đã đăng nhập thì tới dashboard theo role, chưa thì về đăng nhập (U01 F15).
export default function HomePage() {
  return <PostLoginRedirect />;
}
