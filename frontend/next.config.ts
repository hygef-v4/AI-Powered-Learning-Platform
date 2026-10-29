import type { NextConfig } from "next";

// Production: Nginx chuyển /api/* và /mcp sang backend. Dev: Next chuyển tiếp /api/v1/* và /mcp sang BACKEND_URL.
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  distDir: process.env.NEXT_DIST_DIR ?? ".next",
  output: "standalone",
  poweredByHeader: false,
  async rewrites() {
    if (process.env.NODE_ENV === "production") return [];
    return [
      { source: "/api/v1/:path*", destination: `${backendUrl}/api/v1/:path*` },
      // MCP server của U06 (agent của người dùng gọi thẳng, không qua giao diện).
      { source: "/mcp", destination: `${backendUrl}/mcp` },
    ];
  },
};

export default nextConfig;
