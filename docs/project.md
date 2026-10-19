# Project profile

- Status: APPROVED
- Approved: 2026-10-19, by the owner at the bootstrap stack checkpoint (run `20261019-sudoku-mvp`)
<!-- The first pipeline run (bootstrap) fills this in: the architect proposes it,
     the owner approves it at the stack checkpoint. After that it changes only
     through a pipeline run or a governance/* PR. -->

## Product
- Name: Sudoku (placeholder name, spec A9)
- One-line description: A simple, fully offline Sudoku game for Android phones that generates easy puzzles, highlights mistakes as they are made, and keeps your game when you leave.
- Users: Casual Sudoku players, including beginners, playing short and often interrupted sessions.
- Platform(s): Android phones, API 26 (Android 8.0) and later, portrait only. No tablets, landscape, iOS or web.

## Stack
- Language / framework: Kotlin, Jetpack Compose (Material 3), single-Activity app, Android Gradle Plugin with Gradle Kotlin DSL and a version catalog (`gradle/libs.versions.toml`). `minSdk 26`, `targetSdk 36`, `compileSdk 36`. Builds on JDK 17.
- Key libraries: AndroidX Core / Activity Compose / Lifecycle ViewModel, Compose BOM, kotlinx.coroutines, kotlinx.serialization (JSON). Tests: JUnit 4, kotlinx-coroutines-test, Robolectric, AndroidX Test, Compose ui-test-junit4. Build tooling: Android Lint, ktlint (`org.jlleitschuh.gradle.ktlint`), Kover (`org.jetbrains.kotlinx.kover`).
- Persistence: one JSON file in app-private storage via `android.util.AtomicFile` (ADR 0002).
- Decision record: `docs/adr/0001-technology-stack.md` (also ADR 0002 saved-game format, ADR 0003 JVM integration tests)

## Commands (every agent uses exactly these)
Run from the repository root in Git Bash (Windows) or bash (CI).

| Purpose | Command |
|---|---|
| Build | `./gradlew --no-daemon assembleDebug assembleRelease` |
| Lint | `./gradlew --no-daemon lint` (Android Lint; also runs `ktlintCheck`. Auto-format with `./gradlew --no-daemon ktlintFormat`) |
| Unit tests | `./gradlew --no-daemon testDebugUnitTest` (JVM unit tests and Robolectric integration tests) |
| Coverage | `./gradlew --no-daemon koverXmlReportDebug koverHtmlReportDebug` (reports in `app/build/reports/kover/`: XML `reportDebug.xml`, HTML `htmlDebug/`) |
| Performance | `./gradlew --no-daemon testDebugUnitTest --tests "*GenerationPerformanceTest"` (JVM generation benchmark; writes `app/build/perf/generation.json`). On-device procedure: see "Performance measurement" below |
| Full CI locally | `bash scripts/ci.sh` |

- Version file(s): `gradle.properties` at the repository root (`VERSION_NAME` = SemVer, `VERSION_CODE` = integer incremented on every release), read by `app/build.gradle.kts`. Also `CHANGELOG.md`.
- Changed-lines coverage threshold: 80%

### Performance measurement (on device)
Used by the performance agent and the tester for the spec's NFRs; not run in CI.
1. Build and install the minified benchmark build on an API 26+ phone emulator:
   `./gradlew --no-daemon installBenchmark`
2. First-launch time to puzzle shown: `adb shell pm clear <applicationId>`, then
   `adb shell am start -W -n <applicationId>/.MainActivity`, then read the
   `Fully drawn` line from `adb logcat -d -s ActivityTaskManager` (the app calls
   `reportFullyDrawn` once the puzzle is visible). Target: 2000 ms.
3. Restore overhead: play a few moves, force-stop with
   `adb shell am force-stop <applicationId>`, relaunch as in step 2, and compare
   `Fully drawn` with step 2's `TotalTime`. Target: restore adds at most 1000 ms.
4. Repeat each measurement 5 times and record the median and the worst value
   in `docs/perf/<run-id>/results.json`.

## Codebase notes
Single Gradle module `:app`, package root `io.github.greavemrsvg.sudoku`
(placeholder application id, spec A9).

| Package | Contents | Rules |
|---|---|---|
| `domain` | Board / puzzle / game-state types, game rules, solver, generator, saved-game validation | Plain Kotlin. No Android imports. Randomness injected (`kotlin.random.Random`) |
| `data` | Saved-game DTO and JSON codec, `FileGameRepository` (AtomicFile), `GameSaver` (conflated write queue) | Only layer that touches files |
| `ui` | `GameViewModel`, `GameUiState`, Compose screens and components, theme | No game rules here; calls `domain` |
| root | `SudokuApplication`, `AppContainer` (manual DI), `MainActivity` | Container holds no game state |

- Tests: `app/src/test/` only (runs in CI). Integration tests use Robolectric
  and a test `Application` subclass with seeded randomness (ADR 0003).
  `app/src/androidTest/` is optional and run by hand
  (`./gradlew connectedDebugAndroidTest`); it never gates CI.
- Robolectric simulated SDK is pinned to 35 (`app/src/test/resources/robolectric.properties`) because CI uses JDK 17.
- Test names reference the AC: `AC12_last_correct_digit_shows_solved_message`.
