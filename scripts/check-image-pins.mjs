import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const root = new URL("../", import.meta.url);
const read = path => readFileSync(new URL(path, root), "utf8").replace(/^\uFEFF/, "");
const { images } = JSON.parse(read("infra/images.lock.json"));
const pins = new Set(images.map(({ reference, digest }) => {
  assert.match(digest, /^sha256:[a-f0-9]{64}$/, reference);
  return `${reference}@${digest}`;
}));
const files = ["backend/Dockerfile", "frontend/Dockerfile", "infra/docker-compose.yml",
  "infra/docker-compose.local.yml", "infra/docker-compose.staging.yml"];
let checked = 0;
for (const file of files) {
  for (const match of read(file).matchAll(/^(?:FROM\s+|\s+image:\s+)(\S+)/gm)) {
    const image = match[1];
    if (/^platform-(backend|frontend):local$/.test(image)) continue;
    if (/^ghcr\.io\/\$\{GHCR_OWNER:-max2min\}\/platform-(backend|frontend):\$\{IMAGE_TAG:\?\}$/.test(image)) continue;
    assert(pins.has(image), `${file}: unpinned or unrecorded image ${image}`);
    checked++;
  }
}
const postgres = [...pins].find(image => image.startsWith("postgres:"));
assert(read("infra/scripts/restore-isolated.sh").includes(postgres), "restore image must match the pinned PostgreSQL image");
console.log(`Image pins verified: ${checked} references and isolated restore.`);
