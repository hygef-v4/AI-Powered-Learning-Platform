# U05/U09/U15 Java formatting — 2026-11-06

## Change

Formatting only in Java production packages `academics`, `grading`, `admindashboard`. Inspected all 85 files; changed 71 (24 U05, 40 U09, 7 U15). Expanded method/constructor bodies, wrapped long signatures/calls/chains, normalized spacing and four-space indentation. Preserved each file's newline style and wildcard imports. Added explicit multiline-body guidance to the shared code conventions. [Plan](../plans/code-generation-plan/2026-11-06-u05-u09-u15-format.md).

Inline `{ return ... }` occurrences fell from 37 to zero; lines longer than 140 characters fell from 123 to 10. Remaining long lines contain regex/SQL/schema/message literals, intentionally preserved. No identifiers, comments, literals, signatures, endpoint mappings, JSON, validation, authorization, transaction annotations or business operations changed. Frontend/tests/build configuration unchanged.

## Verification

| Check | Actual result |
|---|---|
| Eclipse JDT scanner tokens before each write | Exact equality, including comments and literal spelling; all files validated before any write |
| Tokens against Git HEAD | All 85 source files equal after CRLF/LF normalization for Git comparison |
| Formatter idempotence | Second run changed zero files |
| `git diff --check` | Passed |
| Fresh production/test compilation | BUILD SUCCESS; 720 production + 246 test source files compiled using javac release 17 |

Used the existing VS Code Eclipse JDT formatter through an ignored scratch runner, not a new project dependency. The formatter requires VS Code's bundled Java 25; system Java 21 cannot load those JDT classes. Sandbox JVM startup could not read its security configuration; execution succeeded outside the sandbox. The first compilation JVM could not reserve a 512 MB heap. Final compile used JDK 17, a 192 MB heap and bounded metaspace/code cache with SerialGC.

Normal Maven `test-compile` reported classes already up to date, so verified a fresh compile using a scratch copy of the POM pointing to the original source trees and a separate `.tmp/format-compile-target` output. Original `backend/pom.xml` and `backend/target` were not cleaned or reconfigured. This check compiles tests; it does not execute them or validate test resources. Log: `.tmp/u05-u09-u15-format-fresh-compile.log`.

No runtime, browser, provider or database acceptance rerun: source tokens are unchanged. No commit, push, restart, migration or new naming lint rule. Existing security/resiliency scope retained; no runtime policy changes.
