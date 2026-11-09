# Material visibility and formatted descriptions — backend work

Approved scope: [implementation plan](../../plans/code-generation-plan/2026-11-09-material-quiz-ui.md). This note covers backend work only; root agent owns the consolidated audit and verification report.

- [x] Add default-enabled material visibility independent of ACTIVE/ARCHIVED. Hide disabled rows from student lists; current access rules deny student direct view/token issuance and disabled sources cannot be used for new AI requests. Staff can read/manage disabled content.
- [x] Recheck MATERIAL token redemption through the owner policy before opening bytes, including grants issued before disabling. Missing/unavailable owner policy fails closed. Student grants also require the current content version; staff historical access remains authorized. File lookup is indexed.
- [x] Add bounded FormattedText alongside existing plain fields. Material uses descriptionDocument (2,000 plain characters), quiz uses instructionsDocument (5,000). Format supports paragraphs/bullets/numbered lists, alignment, indent0–6 and bold/italic/underline runs; up to100 blocks/200 runs per block/1,000 runs total. Plain text remains available for old clients. Unchanged trimmed plain text preserves existing format in legacy update requests.
- [x] Persist optional JSONB with migrations V20261109_2200 (materials) and V20261109_2201 (quizzes). Existing material rows default enabled and existing plain descriptions remain intact. Preserve formatting across quiz settings, lifecycle, clone and student-facing QuizStudentView/PracticeQuizDetail.
- [x] Convert the touched material/version adapter to JDBC as required by project policy; remove unused JPA row classes after caller search. Keep CAS/version checks and transactions; material modules and other units are not redesigned.
- [x] Red tests observed for missing enabled default, quiz trailing blank format loss and material legacy trimmed update format loss.
- [x] Clean combined backend run:533 tests/48 suites, zero failures/errors/skips; required guard passed five mandatory suites and57 traced integration cases. Log `.tmp/material-quiz-combined-clean.log`.
- [x] Subsequent quiz/student/JDBC/download regression run:104 tests, zero failures/errors/skips. Log `.tmp/material-quiz-focused-final.log`.
- [x] Final material/format/quiz model run after normalized material preservation fix:119 tests, zero failures/errors/skips. Log `.tmp/material-presentation-final.log`.
- [x] Final non-null quiz JSONB insert/update/clone PostgreSQL roundtrip suite:10 tests passed, zero failures/errors/skips. Log `.tmp/quiz-rich-jdbc-final.log`.
- [x] Backend JAR packaging with tests skipped after the verified test runs:BUILD SUCCESS, executable `backend/target/platform-backend-0.1.0-SNAPSHOT.jar` (134,309,066 bytes). Log `.tmp/material-quiz-backend-package.log`.
- [x] Required guard repeated after final focused runs:passed, saved `.tmp/material-quiz-required-guard.log`. Scoped backend diff whitespace check passed.

Do not sum overlapping test runs or call this a full-backend-suite result. PostgreSQL/Redis/RabbitMQ checks use disposable Testcontainers and test-only data. No live migration, application restart, real AI provider call, commit, push or deployment performed by this task. No frontend checks claimed here.
