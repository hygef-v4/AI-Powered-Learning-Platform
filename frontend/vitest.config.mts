import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/locale.setup.ts", "./tests/setup.ts"],
    include: ["tests/**/*.test.{ts,tsx}"],
    // Interaction-heavy jsdom tests take ~1 s alone but exceed 5 s when every core runs a worker.
    testTimeout: 15_000,
  },
});
