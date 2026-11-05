// Destructive fixtures: run only against an explicitly configured isolated local stack.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";
import { Session, activate, check } from "./lib.mjs";

const results = [];
const runId = Date.now().toString(36) + randomUUID().slice(0, 6);
const prefPath = "/api/v1/me/notification-preferences";
const root = fileURLToPath(new URL("../../", import.meta.url));

function expect(response, status) {
  assert.equal(response.status, status, `expected HTTP ${status}, received ${response.status}`);
  return response.data;
}

async function test(id, action) {
  try {
    await action();
    check(results, id, true);
  } catch (error) {
    // Do not serialize API bodies, cookies, OTPs, passwords or assertion actual/expected values.
    const detail = /^expected HTTP \d+, received \d+$/.test(error.message)
      ? error.message
      : error instanceof assert.AssertionError
        ? "assertion failed"
        : "request failed";
    check(results, id, false, detail);
  }
}

function config() {
  for (const key of [
    "E2E_BASE_URL",
    "E2E_MAILPIT_URL",
    "TEST_ACCOUNT_PASSWORD",
    "U01_BOOTSTRAP_ADMIN_EMAIL",
  ]) {
    if (!process.env[key]) throw new Error(`${key} must be explicitly set`);
  }
  for (const key of ["E2E_BASE_URL", "E2E_MAILPIT_URL"]) {
    const url = new URL(process.env[key]);
    if (
      !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname) ||
      !["http:", "https:"].includes(url.protocol) ||
      url.username ||
      url.password ||
      url.pathname !== "/" ||
      url.search ||
      url.hash ||
      url.port === "8088"
    ) {
      throw new Error(`${key} must be an isolated loopback origin, never port 8088`);
    }
  }
  if (process.env.E2E_RESULT_FILE) {
    const output = path.resolve(root, process.env.E2E_RESULT_FILE);
    const scratch = path.resolve(root, ".tmp") + path.sep;
    if (!output.startsWith(scratch) || !output.endsWith(".json")) {
      throw new Error("E2E_RESULT_FILE must be a JSON file inside the gitignored .tmp directory");
    }
    return output;
  }
}

async function main() {
  const password = process.env.TEST_ACCOUNT_PASSWORD;
  const adminEmail = process.env.U01_BOOTSTRAP_ADMIN_EMAIL;
  const admin = new Session("admin");
  const login = await admin.post("/api/v1/auth/login", { email: adminEmail, password });
  if (login.status !== 200) {
    await activate(adminEmail, password);
    expect(await admin.post("/api/v1/auth/login", { email: adminEmail, password }), 200);
  }
  const people = { admin: { session: admin, email: adminEmail, role: "ADMIN" } };
  for (const [key, role] of [
    ["manager", "SUBJECT_MANAGER"],
    ["teacher", "TEACHER"],
    ["student", "STUDENT"],
    ["other", "STUDENT"],
  ]) {
    const email = `e2e.${runId}.${key}@fpt.edu.vn`;
    const account = expect(
      await admin.post("/api/v1/admin/accounts", {
        email,
        displayName: `E2E ${key} ${runId}`,
        role,
      }),
      201,
    );
    await activate(email, password);
    const session = new Session(key);
    expect(await session.post("/api/v1/auth/login", { email, password }), 200);
    people[key] = { session, email, role, id: account.id ?? account.accountId };
    assert.ok(people[key].id);
  }
  check(results, "FIXTURES unique activated accounts", true);
  const { session: manager, id: managerId } = people.manager;
  const { session: teacher, id: teacherId } = people.teacher;
  const { session: student, id: studentId } = people.student;
  let subject;
  await test("U04 create", async () => {
    subject = expect(
      await admin.post("/api/v1/admin/subjects", {
        code: `E2E-${runId}`.toUpperCase(),
        name: `E2E ${runId}`,
        description: "isolated API E2E",
      }),
      201,
    );
    assert.equal(subject.status, "ACTIVE");
    assert.equal(subject.managerId, null);
  });
  assert.ok(subject?.id);
  const subjectPath = `/api/v1/admin/subjects/${subject.id}`;
  await test("U04 read and list", async () => {
    assert.equal(expect(await admin.get(subjectPath), 200).code, subject.code);
    const list = expect(await admin.get(`/api/v1/admin/subjects?q=${subject.code}`), 200);
    assert.ok(list.items.some((item) => item.id === subject.id));
  });
  await test("U04 update and stale version", async () => {
    const oldVersion = subject.version;
    const body = { name: `Updated ${runId}`, description: "updated", version: oldVersion };
    subject = expect(await admin.patch(subjectPath, body), 200);
    assert.equal(subject.name, body.name);
    assert.ok(subject.version > oldVersion);
    expect(await admin.patch(subjectPath, body), 409);
    assert.equal(expect(await admin.get(subjectPath), 200).version, subject.version);
  });
  await test("U04 assign manager", async () => {
    subject = expect(
      await admin.put(`${subjectPath}/manager`, { managerId, version: subject.version }),
      200,
    );
    assert.equal(subject.managerId, managerId);
  });
  await test("U04 archive and reopen", async () => {
    subject = expect(
      await admin.put(`${subjectPath}/status`, { status: "ARCHIVED", version: subject.version }),
      200,
    );
    assert.equal(subject.status, "ARCHIVED");
    assert.equal(subject.managerId, null);
    subject = expect(
      await admin.put(`${subjectPath}/status`, { status: "ACTIVE", version: subject.version }),
      200,
    );
    assert.equal(subject.status, "ACTIVE");
    assert.equal(subject.managerId, null);
    subject = expect(
      await admin.put(`${subjectPath}/manager`, { managerId, version: subject.version }),
      200,
    );
  });
  for (const [role, session] of [
    ["manager", manager],
    ["teacher", teacher],
    ["student", student],
  ]) {
    await test(`U04 ${role} denied admin read/write`, async () => {
      expect(await session.get(subjectPath), 403);
      expect(
        await session.patch(subjectPath, {
          name: "denied",
          description: "",
          version: subject.version,
        }),
        403,
      );
    });
  }

  // Class and enrollment authorize the student/teacher reads through the actual access policy.
  let cls = expect(
    await manager.post("/api/v1/manager/classes", {
      subjectId: subject.id,
      code: `E2E-${runId}`.toUpperCase(),
      name: `E2E ${runId}`,
      description: "isolated API E2E",
      term: "2026-1",
      teacherId,
    }),
    201,
  );
  if (cls.status !== "OPEN") {
    cls = expect(
      await manager.put(`/api/v1/manager/classes/${cls.id}/status`, {
        status: "OPEN",
        version: cls.version,
      }),
      200,
    );
  }
  expect(
    await manager.post(`/api/v1/manager/classes/${cls.id}/enrollments`, { accountId: studentId }),
    200,
  );
  const modulesPath = `/api/v1/subjects/${subject.id}/modules`;
  const materialsPath = `/api/v1/subjects/${subject.id}/materials`;
  let module, material;
  const video1 = "dQw4w9WgXcQ",
    video2 = "M7lc1UVf-VE";
  const canonical = (id) => `https://www.youtube.com/watch?v=${id}`;
  const input = (videoUrl) => ({
    kind: "YOUTUBE_LINK",
    title: "Video E2E",
    description: "",
    orderNo: 0,
    moduleId: module.id,
    source: { videoUrl },
  });
  await test("U13 create module", async () => {
    module = expect(await manager.post(modulesPath, { title: "E2E module" }), 201);
    assert.ok(expect(await manager.get(modulesPath), 200).some((item) => item.id === module.id));
  });
  assert.ok(module?.id);
  await test("U13 create video with list/index canonicalized", async () => {
    material = expect(
      await manager.post(materialsPath, input(`${canonical(video1)}&list=PL-E2E&index=2&t=3`)),
      201,
    );
    assert.equal(material.current.videoId, video1);
    assert.equal(material.current.videoUrl, canonical(video1));
    assert.equal(material.currentVersionNo, 1);
  });
  assert.ok(material?.id);
  const materialPath = `/api/v1/materials/${material.id}`;
  await test("U13 replace video with list/index canonicalized", async () => {
    material = expect(
      await manager.post(`${materialPath}/versions`, {
        version: material.version,
        source: { videoUrl: `https://youtu.be/${video2}?list=PL-E2E&index=7` },
      }),
      200,
    );
    assert.equal(material.current.videoUrl, canonical(video2));
    assert.equal(material.current.videoId, video2);
    assert.equal(material.currentVersionNo, 2);
    const stored = expect(await manager.get(materialPath), 200);
    assert.equal(stored.current.videoUrl, canonical(video2));
    assert.equal(stored.versions.length, 2);
    assert.ok(stored.versions.every((v) => v.videoUrl === canonical(v.videoId)));
    const listed = expect(await manager.get(materialsPath), 200).find(
      (item) => item.id === material.id,
    );
    assert.equal(listed.current.videoUrl, canonical(video2));
  });
  for (const [name, url] of [
    ["shorts", `https://www.youtube.com/shorts/${video1}?list=PL-E2E`],
    ["embed", `https://www.youtube.com/embed/${video1}`],
    ["live", `https://www.youtube.com/live/${video1}`],
    ["mobile", `https://m.youtube.com/watch?v=${video1}`],
    ["http", `http://www.youtube.com/watch?v=${video1}`],
    ["bare", `youtu.be/${video1}`],
  ]) {
    await test(`U13 existing ${name} URL accepted`, async () => {
      const saved = expect(await manager.post(materialsPath, input(url)), 201);
      assert.equal(saved.current.videoUrl, canonical(video1));
    });
  }
  for (const [name, url] of [
    ["playlist-only", "https://www.youtube.com/playlist?list=PL-E2E"],
    ["unsafe scheme", `javascript:alert('${video1}')`],
    ["unsafe host", `https://www.youtube.com.evil.invalid/watch?v=${video1}`],
    ["credentials", `https://user@www.youtube.com/watch?v=${video1}`],
    ["duplicate video", `${canonical(video1)}&v=${video2}`],
  ]) {
    await test(`U13 ${name} rejected on create/replace`, async () => {
      expect(await manager.post(materialsPath, input(url)), 400);
      expect(
        await manager.post(`${materialPath}/versions`, {
          version: material.version,
          source: { videoUrl: url },
        }),
        400,
      );
      assert.equal(
        expect(await manager.get(materialPath), 200).currentVersionNo,
        material.currentVersionNo,
      );
    });
  }
  await test("U13 authorized student/teacher read", async () => {
    for (const [session, prefix] of [
      [student, "/api/v1/me"],
      [teacher, "/api/v1"],
    ]) {
      const list = expect(await session.get(`${prefix}/classes/${cls.id}/materials`), 200);
      assert.equal(
        list.find((item) => item.id === material.id).current.videoUrl,
        canonical(video2),
      );
      const detail = expect(await session.get(`${materialPath}?classId=${cls.id}`), 200);
      assert.equal(detail.canManage, false);
      assert.equal(detail.current.videoUrl, canonical(video2));
    }
  });
  await test("U13 student cannot create/replace/module write", async () => {
    expect(await student.post(materialsPath, input(canonical(video1))), 404);
    expect(
      await student.post(`${materialPath}/versions`, {
        version: material.version,
        source: { videoUrl: canonical(video1) },
      }),
      404,
    );
    expect(await student.post(modulesPath, { title: "denied" }), 404);
    assert.equal(expect(await manager.get(materialPath), 200).current.videoUrl, canonical(video2));
  });
  await test("U13 unenrolled student read denied", async () => {
    expect(await people.other.session.get(`${materialPath}?classId=${cls.id}`), 404);
  });

  const keys = {
    ADMIN: ["BACKUP_FAILED"],
    SUBJECT_MANAGER: ["LEADER_CHANGE_REQUESTED", "QUESTION_REVIEWED", "QUESTION_PROPOSED"],
    TEACHER: ["LEADER_CHANGE_REQUESTED", "QUESTION_REVIEWED"],
    STUDENT: [
      "ENROLLED",
      "ASSIGNMENT_OPENED",
      "GRADE_PUBLISHED",
      "DEADLINE_REMINDER",
      "ANNOUNCEMENT_IMPORTANT",
    ],
  };
  for (const key of ["admin", "manager", "teacher", "student"]) {
    const person = people[key],
      session = person.session;
    const defaults = person.role === "STUDENT" || person.role === "ADMIN";
    await test(`U14 ${key} exact keys/defaults`, async () => {
      const prefs = expect(await session.get(prefPath), 200);
      assert.deepEqual(Object.keys(prefs).sort(), [...keys[person.role]].sort());
      assert.ok(Object.values(prefs).every((value) => value === defaults));
    });
    await test(`U14 ${key} persist and relogin`, async () => {
      const type = keys[person.role][0],
        enabled = person.role === "ADMIN" ? true : !defaults;
      assert.equal(expect(await session.put(prefPath, { [type]: enabled }), 200)[type], enabled);
      const relogin = new Session(`${key}-relogin`);
      expect(await relogin.post("/api/v1/auth/login", { email: person.email, password }), 200);
      assert.equal(expect(await relogin.get(prefPath), 200)[type], enabled);
      assert.equal(expect(await session.put(prefPath, { [type]: defaults }), 200)[type], defaults);
    });
    await test(`U14 ${key} invalid role/type rejected`, async () => {
      const before = expect(await session.get(prefPath), 200);
      const invalid = person.role === "STUDENT" ? "QUESTION_REVIEWED" : "ENROLLED";
      expect(await session.put(prefPath, { [invalid]: true }), 400);
      expect(await session.put(prefPath, { UNKNOWN_EMAIL_TYPE: true }), 400);
      expect(await session.put(prefPath, { [keys[person.role][0]]: null }), 400);
      assert.deepEqual(expect(await session.get(prefPath), 200), before);
    });
  }
  await test("U14 admin backup email mandatory", async () => {
    expect(await admin.put(prefPath, { BACKUP_FAILED: false }), 400);
    assert.deepEqual(expect(await admin.get(prefPath), 200), { BACKUP_FAILED: true });
  });
  await test("U14 account override cannot change another account", async () => {
    const other = people.other;
    const before = expect(await other.session.get(prefPath), 200);
    const spoof = `${prefPath}?accountId=${other.id}&userId=${other.id}`;
    const changed = expect(
      await student.call(
        "PUT",
        spoof,
        { ENROLLED: false },
        {
          headers: { "X-Account-Id": other.id, "X-User-Id": other.id, "X-Role": "ADMIN" },
        },
      ),
      200,
    );
    assert.equal(changed.ENROLLED, false);
    assert.equal(expect(await student.get(spoof), 200).ENROLLED, false);
    assert.deepEqual(expect(await other.session.get(prefPath), 200), before);
    expect(await student.put(prefPath, { accountId: true, ENROLLED: true }), 400);
    assert.equal(expect(await student.get(prefPath), 200).ENROLLED, false);
    assert.deepEqual(expect(await other.session.get(prefPath), 200), before);
    expect(await student.put(prefPath, { ENROLLED: true }), 200);
  });
}

let output;
try {
  output = config();
  await main();
} catch {
  check(
    results,
    "SUITE configuration or fixture setup",
    false,
    "Check isolated URLs, required environment and stack availability; sensitive error details withheld",
  );
} finally {
  const summary = {
    runId,
    passed: results.filter((r) => r.ok).length,
    failed: results.filter((r) => !r.ok).length,
    results,
  };
  if (output) {
    fs.mkdirSync(path.dirname(output), { recursive: true });
    fs.writeFileSync(output, JSON.stringify(summary, null, 2) + "\n");
  }
  console.log(
    `${summary.failed ? "FAIL" : "PASS"} U04/U13/U14: ${summary.passed} passed, ${summary.failed} failed`,
  );
  if (summary.failed) process.exitCode = 1;
}
