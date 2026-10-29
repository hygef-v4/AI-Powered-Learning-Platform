import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// Windows standalone tracing can race while creating shared parent directories.
// Set this before starting Next, when Node initializes its filesystem thread pool.
const env = { ...process.env };
if (process.platform === "win32") {
  env.UV_THREADPOOL_SIZE ??= "1";
}

const nextCli = fileURLToPath(new URL("../node_modules/next/dist/bin/next", import.meta.url));
const build = spawnSync(process.execPath, [nextCli, "build", "--webpack", ...process.argv.slice(2)], {
  env,
  stdio: "inherit",
});

if (build.error) {
  throw build.error;
}
process.exit(build.status ?? 1);
