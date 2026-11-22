import type { Metadata } from "next";
import { PostLoginRedirect } from "@/features/identity/components/PostLoginRedirect";
import { NO_INDEX } from "@/shared/siteMetadata";

export const metadata: Metadata = { robots: NO_INDEX };

export default function Page() {
  return <PostLoginRedirect />;
}
