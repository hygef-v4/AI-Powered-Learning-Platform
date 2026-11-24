import assert from "node:assert/strict";
import { randomBytes, randomUUID } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { BASE, MAILPIT, Session, activate, check } from "./lib.mjs";

const scratch = path.resolve(".tmp/systemtest");
const diagramDir = new URL("./fixtures/pe-diagrams/", import.meta.url);
const results = [];
const questions = [
  ["Context Diagram", "context", "OTHER", [
    ["Represent the Campus Equipment Lending system boundary", 1],
    ["Identify Borrower and Equipment Manager and label their associations", 2],
  ]],
  ["Use Case Diagram", "use-case", "USE_CASE", [
    ["Place both actors outside the system boundary", 1],
    ["Connect actors to Check Availability, Request Loan, Approve Loan and Record Return", 2],
    ["Show Request Loan including Check Availability", 1],
  ]],
  ["Conceptual ERD", "erd", "ER", [
    ["Show Borrower, Equipment, Loan Request and Return Record", 1.5],
    ["Show and label relationships with cardinalities", 1.5],
  ]],
];
const brief = "A campus lends equipment to students. A Borrower checks availability and submits a loan request for one equipment item. An Equipment Manager approves the request and records the return. Each item and borrower can appear in many requests; a completed request has one return record. Answer each question with a Draw.io diagram.";

function requireLoopback(value) {
  const url = new URL(value);
  assert.ok(["localhost", "127.0.0.1", "[::1]"].includes(url.hostname) && url.protocol === "http:"
    && !url.username && !url.password && url.pathname === "/" && !url.search && !url.hash,
  "System tests require a local test origin");
}

function requireResponse(response, label, statuses = [200]) {
  if (!statuses.includes(response.status)) {
    const code = response.data?.code;
    const safeCode = typeof code === "string" && /^[A-Z0-9_]+$/.test(code) ? ` ${code}` : "";
    throw new Error(`${label}: HTTP ${response.status}${safeCode}`);
  }
  return response.data;
}

async function openSession(email, password) {
  const session = new Session("system test");
  requireResponse(await session.post("/api/v1/auth/login", { email, password }), "Sign in");
  return session;
}

function createDiagram(index) {
  const [title, file, diagramType] = questions[index];
  return {
    id: randomUUID(),
    origin: "STUDENT",
    type: "DIAGRAM",
    content: { diagramType, alt: title, xml: fs.readFileSync(new URL(`${file}.drawio`, diagramDir), "utf8") },
  };
}

async function createAssignment(teacher, classId, submissionMode, title) {
  const assignment = requireResponse(await teacher.post(`/api/v1/classes/${classId}/assignments`, {
    title, submissionMode, gradingMode: "GRADED", diagramEnabled: true,
  }), "Create PE assignment", [200, 201]);
  const url = `/api/v1/assignments/${assignment.id}`;
  const start = requireResponse(await teacher.get(url), "Read PE assignment");
  let current = requireResponse(await teacher.put(`${url}/skeleton`, {
    version: start.version,
    skeleton: {
      blocks: [
        { id: randomUUID(), origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: brief }] } },
        ...questions.map(([text]) => ({
          id: randomUUID(), origin: "TEACHER", type: "HEADING", content: { style: "H1", text },
        })),
      ],
    },
    instructions: "Submit the three diagrams only.",
    fromDocx: false,
  }), "Save PE questions");
  current = requireResponse(await teacher.get(url), "Read PE sections");
  assert.equal(current.parts.length, questions.length);
  for (const [index, part] of current.parts.entries()) {
    const [section, , diagramType, criteria] = questions[index];
    requireResponse(await teacher.put(`${url}/parts/${part.partId ?? part.id}/rubric`, {
      version: current.version,
      criteria: [{
        criterionId: randomUUID(), title: section, description: "", kind: "DIAGRAM", diagramType,
        items: criteria.map(([text, points]) => ({ itemId: randomUUID(), text, points })),
      }],
    }), "Save diagram rubric");
    current = requireResponse(await teacher.get(url), "Read rubric revision");
  }
  current = requireResponse(await teacher.post(`${url}/review`, { version: current.version }), "Finalize PE");
  return requireResponse(await teacher.post(`${url}/publish`, {
    version: current.version, openNow: true,
    closesAt: new Date(Date.now() + 12 * 3600_000).toISOString(),
    maxAttempts: 2, confirmUngroupedStudents: true,
  }), "Publish PE");
}

async function submitIndividual(student, classId, assignmentId, partial = false) {
  const attempt = requireResponse(await student.post(`/api/v1/assignments/${assignmentId}/attempts`, { classId }),
    "Start individual PE", [200, 201]);
  const url = `/api/v1/attempts/${attempt.attemptId}`;
  let question = 0;
  const blocks = attempt.content.blocks.flatMap((block) => {
    if (block.type !== "HEADING") return [block];
    const index = question++;
    return partial && index === 2 ? [block] : [block, createDiagram(index)];
  });
  assert.equal(question, questions.length);
  const saved = requireResponse(await student.put(`${url}/content`, {
    contentVersion: attempt.contentVersion, content: { blocks },
  }), "Save individual diagrams");
  const receipt = requireResponse(await student.post(`${url}/submit`, { contentVersion: saved.contentVersion }),
    "Submit individual diagrams");
  assert.ok(receipt.receiptHash);
  return attempt.attemptId;
}

async function submitGroup(leader, member, assignmentId) {
  let locator;
  for (let attempt = 0; attempt < 25; attempt++) {
    const response = await leader.get(`/api/v1/assignments/${assignmentId}/my-group-doc`);
    locator = response.data?.documentId ?? response.data?.id;
    if (response.status === 200 && locator) break;
    await new Promise((resolve) => setTimeout(resolve, 400));
  }
  assert.ok(locator, "Group document was not provisioned");
  const url = `/api/v1/group-docs/${locator}`;
  const document = requireResponse(await leader.get(url), "Read group work");
  assert.equal(document.sections.length, questions.length);
  for (const [index, section] of document.sections.entries()) {
    const participant = index === 1 ? member : leader;
    const sectionUrl = `${url}/sections/${section.id}`;
    const claimed = requireResponse(await participant.post(`${sectionUrl}:claim`), "Claim diagram section");
    const saved = requireResponse(await participant.put(`${sectionUrl}/draft`, {
      version: claimed.version, blocks: [createDiagram(index)],
    }), "Save group diagram");
    requireResponse(await participant.post(`${sectionUrl}:done`, { version: saved.version }), "Finish diagram section");
  }
  const completed = requireResponse(await leader.get(url), "Read completed group document");
  const receipt = requireResponse(await leader.post(`${url}/submit`, { version: completed.version }), "Submit group diagrams");
  assert.ok(receipt.receiptHash);
  return locator;
}

async function closeAssignment(teacher, assignmentId) {
  const url = `/api/v1/assignments/${assignmentId}`;
  const current = requireResponse(await teacher.get(url), "Read assignment before closing");
  return requireResponse(await teacher.post(`${url}/close`, { version: current.version }), "Close PE assignment");
}

async function listEvaluations(teacher, classId, assignmentId, count) {
  for (let attempt = 0; attempt < 25; attempt++) {
    const response = requireResponse(await teacher.get(
      `/api/v1/classes/${classId}/assignments/${assignmentId}/submissions`,
    ), "Read submission evaluations");
    const rows = response.rows.filter((row) => row.evaluationId);
    if (rows.length === count) return rows;
    await new Promise((resolve) => setTimeout(resolve, 400));
  }
  throw new Error("Submitted attempts were not available for grading");
}

async function prepareBatch(teacher, classId, batchId) {
  const rows = await listEvaluations(teacher, classId, batchId, 3);
  const sample = requireResponse(await teacher.get(`/api/v1/evaluations/${rows[0].evaluationId}`), "Read batch rubric");
  const rubricScores = Object.fromEntries(sample.rubrics.map((rubric) => [
    rubric.rubricId,
    Object.fromEntries(rubric.criteria.flatMap((entry) => entry.items.map((item) => [item.itemId, item.points]))),
  ]));
  assert.equal(Object.keys(rubricScores).length, questions.length);
  const items = [];
  for (const row of rows.slice(0, 2)) {
    const saved = requireResponse(await teacher.put(`/api/v1/evaluations/${row.evaluationId}`, {
      version: row.version, checkedItemsByRubric: {}, itemScoresByRubric: rubricScores, feedback: "PE rubric checked.",
    }), "Save batch fixture draft");
    items.push({ evaluationId: row.evaluationId, version: saved.version });
  }
  items.push({ evaluationId: rows[2].evaluationId, version: rows[2].version });
  return items;
}

async function verifyBatch(teacher, classId, batchId, items) {
  const batch = requireResponse(await teacher.post("/api/v1/evaluations:publish", { items }), "Publish batch fixture");
  assert.deepEqual(batch.items.map((item) => item.outcome), ["SUCCESS", "SUCCESS", "FAILED"]);
  const grades = await listEvaluations(teacher, classId, batchId, 3);
  assert.equal(grades.filter((grade) => grade.status === "PUBLISHED").length, 2);
  assert.equal(grades.find((grade) => grade.evaluationId === items[2].evaluationId).score, null);
  check(results, "ST-WF01-031 API partial publication", true, batch.items[2].code);
}

async function runSystemTest() {
  requireLoopback(BASE);
  requireLoopback(MAILPIT);
  assert.ok(process.env.E2E_BASE_URL && process.env.E2E_MAILPIT_URL, "Set both test origins explicitly");
  const { WF_TEST_ADMIN, WF_TEST_ADMIN_PASSWORD } = process.env;
  assert.ok(WF_TEST_ADMIN && WF_TEST_ADMIN_PASSWORD, "Provide test admin credentials through environment");
  const runId = randomBytes(4).toString("hex");
  const folder = path.join(scratch, `grading-${runId}`);
  fs.mkdirSync(folder, { recursive: true });
  console.log(`Fixture outputs: ${folder}`);
  const admin = await openSession(WF_TEST_ADMIN, WF_TEST_ADMIN_PASSWORD);
  const fixturePassword = randomBytes(24).toString("base64url") + "Aa7!";
  const accounts = {};
  for (const [index, [name, role]] of [
    ["manager", "SUBJECT_MANAGER"], ["teacher", "TEACHER"],
    ["student1", "STUDENT"], ["student2", "STUDENT"], ["student3", "STUDENT"],
  ].entries()) {
    const email = `pe.${runId}.${name}@fpt.edu.vn`;
    const account = requireResponse(await admin.post("/api/v1/admin/accounts", {
      email, displayName: `PE ${name} ${runId}`, role,
      ...(role === "STUDENT" ? {
        studentCode: `PE${String((parseInt(runId, 16) + index) % 1_000_000).padStart(6, "0")}`,
      } : {}),
    }), "Create test account", [200, 201]);
    try {
      await activate(email, fixturePassword);
    } catch {
      throw new Error(`Could not activate the ${name} fixture through Mailpit`);
    }
    accounts[name] = { email, id: account.id ?? account.accountId };
  }
  const { manager, teacher, student1, student2, student3 } = Object.fromEntries(await Promise.all(
    Object.entries(accounts).map(async ([name, account]) => [name, await openSession(account.email, fixturePassword)]),
  ));
  const subject = requireResponse(await admin.post("/api/v1/admin/subjects", {
    code: `PE${runId.toUpperCase()}`, name: `PE grading ${runId}`, managerId: accounts.manager.id,
  }), "Create fixture subject", [200, 201]);
  const cls = requireResponse(await manager.post("/api/v1/manager/classes", {
    subjectId: subject.id, code: `PE-${runId.toUpperCase()}`, name: `PE Grading ${runId}`,
    startsOn: new Date().toISOString().slice(0, 10), teacherId: accounts.teacher.id,
  }), "Create fixture class", [200, 201]);
  if (cls.status !== "OPEN") requireResponse(await manager.put(`/api/v1/manager/classes/${cls.id}/status`, {
    status: "OPEN", version: cls.version,
  }), "Open fixture class");
  for (const student of [accounts.student1, accounts.student2, accounts.student3]) {
    requireResponse(await manager.post(`/api/v1/manager/classes/${cls.id}/enrollments`, { accountId: student.id }),
      "Enroll fixture student", [200, 201, 204]);
  }
  const groupsPath = `/api/v1/classes/${cls.id}/groups`;
  const groups = requireResponse(await teacher.get(groupsPath), "Read class groups");
  requireResponse(await teacher.put(groupsPath, {
    version: groups.version, groups: [{
      key: "pe", name: "PE Team", leaderId: accounts.student1.id,
      memberIds: [accounts.student1.id, accounts.student2.id, accounts.student3.id],
    }],
  }), "Set up PE team");

  const assignments = {};
  for (const [name, mode] of [
    ["Manual", "INDIVIDUAL"], ["OpenAI", "INDIVIDUAL"], ["MCP", "INDIVIDUAL"],
    ["Batch UI", "INDIVIDUAL"], ["Batch API", "INDIVIDUAL"], ["Group", "GROUP"],
  ]) {
    const assignment = await createAssignment(teacher, cls.id, mode, `Campus Equipment Lending - ${name} ${runId}`);
    assignments[name] = { id: assignment.id, title: assignment.title };
  }
  const individual = {};
  for (const [name, learners] of [
    ["Manual", [student1]], ["OpenAI", [student1]], ["MCP", [student2]],
    ["Batch UI", [student1, student2, student3]], ["Batch API", [student1, student2, student3]],
  ]) {
    const assignmentId = assignments[name].id;
    const attempts = [];
    for (const learner of learners) {
      attempts.push(await submitIndividual(learner, cls.id, assignmentId, name === "OpenAI"));
    }
    if (name !== "Manual") await closeAssignment(teacher, assignmentId);
    const rows = await listEvaluations(teacher, cls.id, assignmentId, attempts.length);
    individual[name] = rows.map((row) => ({
      evaluationId: row.evaluationId, attemptId: row.attemptId, studentId: row.student.accountId,
    }));
  }
  const groupDocumentId = await submitGroup(student1, student2, assignments.Group.id);
  const groupRows = await listEvaluations(teacher, cls.id, assignments.Group.id, 1);
  await prepareBatch(teacher, cls.id, assignments["Batch UI"].id);
  const batchItems = await prepareBatch(teacher, cls.id, assignments["Batch API"].id);
  const fixture = {
    runId, base: BASE, mailpit: MAILPIT, classId: cls.id, className: `PE Grading ${runId}`,
    accounts, assignments, individual, group: { documentId: groupDocumentId, evaluationId: groupRows[0].evaluationId },
  };
  fs.writeFileSync(path.join(folder, "test-login.private.json"), JSON.stringify({ password: fixturePassword }));
  fs.writeFileSync(path.join(folder, "fixture.json"), JSON.stringify(fixture, null, 2));
  check(results, "PE grading fixture submitted", true);
  await verifyBatch(teacher, cls.id, assignments["Batch API"].id, batchItems);
  fs.writeFileSync(path.join(folder, "api-results.json"), JSON.stringify(results, null, 2));
}

runSystemTest().catch((error) => {
  const message = error instanceof Error ? error.message : "";
  console.error(/^(?:[\w -]+: HTTP \d{3}(?: [A-Z0-9_]+)?|Could not activate the \w+ fixture through Mailpit|System tests require a local test origin|Set both test origins explicitly|Provide test admin credentials through environment|Group document was not provisioned|Submitted attempts were not available for grading)$/.test(message)
    ? message : `Grading test failed (${error?.name ?? "Error"} at ${String(error?.stack).split("\n").find((line) => line.includes("wf01-wf02-grading.mjs"))?.match(/:(\d+):\d+/)?.[1] ?? "unknown line"})`);
  process.exitCode = 1;
});
