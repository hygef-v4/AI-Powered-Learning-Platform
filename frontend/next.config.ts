import type { NextConfig } from "next";

// Production: Nginx định tuyến /api/* sang backend. Dev: Next chuyển tiếp /api/v1/* sang BACKEND_URL.
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  async rewrites() {
    if (process.env.NODE_ENV === "production") return [];
    return [{ source: "/api/v1/:path*", destination: `${backendUrl}/api/v1/:path*` }];
  },
};

export default nextConfig;
