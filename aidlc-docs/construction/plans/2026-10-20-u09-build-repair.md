# U09 build repair

The user requested fixing the build errors reported after the clean-code refactor. Preserve U09 API, business logic and existing uncommitted work.

- [x] Reproduce Maven offline/cache failure and Turbopack child-process failure; inspect CI/build configuration and logs.
- [x] Confirm Maven artifacts are present under a different cached repository ID; prove packaging with a temporary settings profile that declares that origin, without disabling checks or changing dependency versions.
- [x] Fix frontend build prerequisites: remove build-time Google Fonts network dependency while preserving Be Vietnam Pro files/weights/ranges and fallback metrics; support an isolated build directory to avoid existing output permission failures.
- [x] Verify frontend compilation using the available supported bundler/runtime; persist only the minimal working build command if needed.
- [x] Run final backend verify/frontend checks, inspect changes and update handoff/state with exact limitations.

Evidence before production edits: Maven online build fails writing global-cache `.pom.lastUpdated`; offline build cannot associate cached plugin dependencies with central. The temporary origin profile packages the executable JAR successfully. Turbopack fails before connecting to its PostCSS child process. Webpack reaches compilation but fails resolving fonts.googleapis.com and creating an existing `.next/types` directory. Source CSS is unchanged and no CSS defect has been demonstrated.

Existing enabled security/resiliency scope is retained. Deployment, health-check and remote-service policy changes are N/A; no credentials, new network integrations or business authorization changes. Disabled PBT remains skipped.

## Changes and evidence

- Optional `scripts/maven-local-cache-settings.xml` declares the cached origin as an additional Maven Central ID, preserving the default repository and checksum policies. `mvn -o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml verify` with the existing startup test agent: **724 total / 662 passed / 62 skipped / zero failures or errors**, executable Spring Boot JAR packaged successfully. Maven 3.9.16 without the profile reproduced the same origin mismatch, so changing Maven alone does not fix it.
- `frontend/src/app/fonts.css` and `public/fonts/be-vietnam-pro` contain the existing Be Vietnam Pro WOFF2 subsets, preserving five weights, all three Unicode ranges per weight, display swap and fallback metrics. All 15 binaries hash-match the previous Next-generated font files. Root layout retains the original 10 Latin/Vietnamese preloads. Copyright/OFL notice is included, from https://github.com/google/fonts/blob/main/ofl/bevietnampro/OFL.txt.
- `build` uses supported `next build --webpack`, avoiding the reproduced Turbopack PostCSS child connection failure. Compilation, typechecking and prerendering 18 pages passed with local fonts. `NEXT_DIST_DIR` optionally selects isolated output; defaults remain `.next`. Lint/Git ignore only generated alternate output directories. Docker now copies public assets so fonts are present in its runtime stage.
- Standard Windows standalone tracing still reported EPERM during asynchronous mkdir/copy even with a fresh output directory and Node 24.19.0. A minimal 300-directory concurrent mkdir probe passed, so no universal Node filesystem defect is claimed. The same build passed with `UV_THREADPOOL_SIZE=1`. `frontend/scripts/build.mjs` now starts Next with this default only on Windows (preserving an explicit override), and invokes Webpack. The final normal `npm --prefix frontend run build` invocation passed on default Node 25.8.1, with only `NEXT_DIST_DIR=.next-verify-u09-lts` selecting scratch output. Linux CI retains its existing filesystem thread pool. No node_modules patch, dependency upgrade, disabled typecheck or disabled standalone output was applied.
- Full frontend suite after these changes: **223 passed / 45 files**.
- Final lint and typecheck passed. TypeScript configuration changes automatically added by isolated builds were restored to the pre-session file; a fresh typecheck passed afterwards. Git diff whitespace check passed.
- Standalone `server.js` started successfully and logged Ready on loopback, then was stopped. HTTP smoke requests were blocked by the sandbox socket permission (`An attempt was made to access a socket in a way forbidden by its access permissions`); this is not recorded as an application HTTP failure. Packaged login HTML contains the font class/preloads and all 15 public WOFF2 assets are present after copying static/public assets as the Dockerfile does. HTTP/browser and Docker-image verification remain unverified.

## Repeating the checks

From the repository root, use `mvn -o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml verify` when reusing this existing cache. In this sandbox, the Mockito startup agent additionally uses `-DargLine=-javaagent:<absolute path to byte-buddy-agent.jar>`; normal developer execution may use the regular JVM attachment mechanism. Normal online builds and CI do not need the optional cache settings.

For an isolated PowerShell frontend build, set `$env:NEXT_DIST_DIR='.next-local'`, then run `npm --prefix frontend run build`. Use the same environment variable when starting that output. With no override, output remains `.next`, matching the Dockerfile. Docker copies both static build output and public font assets; a Docker image build was not run in this environment.

Remaining gates: skipped database/provider integration and NFR checks from the U09 handoff remain open. This task does not change U09 grading behavior, contracts, schemas or dependency versions.
