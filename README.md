# suduko-agent-test

A simple, fully offline Sudoku game for Android phones (API 26+, portrait only),
built by a governed team of Claude Code agents. Product and stack details are in
[`docs/project.md`](docs/project.md); how the agents work is in
[`docs/governance/HOW-TO.md`](docs/governance/HOW-TO.md).

To start or continue work: `claude --agent orchestrator`.

## Requirements
- JDK 17
- Android SDK with platform 36 (set `ANDROID_HOME`, or `sdk.dir` in `local.properties`)

## Commands
Run from the repository root (Git Bash on Windows, or bash).

| Purpose | Command |
|---|---|
| Build | `./gradlew --no-daemon assembleDebug assembleRelease` |
| Lint (Android Lint and ktlint) | `./gradlew --no-daemon lint` |
| Auto-format | `./gradlew --no-daemon ktlintFormat` |
| Unit tests (JVM and Robolectric) | `./gradlew --no-daemon testDebugUnitTest` |
| Coverage | `./gradlew --no-daemon koverXmlReportDebug koverHtmlReportDebug` (reports in `app/build/reports/kover/`) |
| Everything CI runs | `bash scripts/ci.sh` |

## Release builds
The version is set by `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`.
A release is signed only when `SUDOKU_KEYSTORE_FILE`, `SUDOKU_KEYSTORE_PASSWORD`,
`SUDOKU_KEY_ALIAS` and `SUDOKU_KEY_PASSWORD` are set in the environment;
otherwise it is left unsigned. No keys are kept in the repository.
