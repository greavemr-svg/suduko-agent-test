# ADR 0001: Technology stack for the Android Sudoku app

- Status: Accepted. Proposed by the architect (run `20261019-sudoku-mvp`) and
  approved by the owner at the bootstrap stack checkpoint on 2026-10-19
  (recorded in `docs/runs/20261019-sudoku-mvp/state.json`, `gates.stack_approval`).
- Date: 2026-10-19
- Run: `20261019-sudoku-mvp`

## Context
The spec (`docs/specs/20261019-sudoku-mvp.md`) asks for:
- An Android phone app, API 26 (Android 8.0) and later, portrait only, no
  tablets, landscape, iOS or web.
- Fully offline: no network permission at all (AC22), no third-party services.
- Tiny local data: one saved game (puzzle, solution, entries, solved flag) in
  app-private storage, validated as untrusted input on load (AC15-AC18).
- CPU work on device: puzzle generation with a uniqueness check within 2 s
  (NFR), digit feedback within 100 ms.
- Accessibility: TalkBack descriptions, non-colour mistake cue, touch targets
  (grid cells share the screen width equally and are at least 24x24 dp on a
  360 dp wide portrait screen, per spec A10; digit buttons, Erase and New Game
  at least 48x48 dp), font scaling, WCAG AA contrast.
- Integration ACs (AC1, AC5, AC6, AC9, AC11, AC12, AC14-AC18) must run in CI,
  or the design must say how they run. Process-death tests on CI emulators are
  a named flakiness risk.

Constraints from the repository (owner-only, cannot be changed by agents):
- `scripts/ci.sh` runs `./gradlew --no-daemon lint testDebugUnitTest` when an
  executable `gradlew` exists.
- `.github/workflows/ci.yml` runs on `ubuntu-latest`, sets up **JDK 17**
  (Temurin) when `gradlew` exists, and has a 30-minute timeout. It does not
  start an emulator.

Maintainability: one owner, helped by agents. Mainstream, well-documented
Google-recommended tooling is preferred over niche choices. Cost: everything
must be free and open source.

## Options considered
1. **Kotlin + Jetpack Compose (Material 3), Gradle Kotlin DSL, JVM tests with
   Robolectric** (chosen).
   - Pros: Google's recommended, default stack for new Android apps; first-class
     Android Studio support; Compose makes a 9x9 grid with per-cell state and
     semantics (TalkBack) short and testable; state hoisting fits the "logic out
     of UI" standard; Compose UI tests run on the JVM under Robolectric, so
     integration ACs run inside `testDebugUnitTest` in the existing CI with no
     emulator; matches the Android/Kotlin section of
     `docs/governance/coding-standards.md` (ktlint, ViewModels, coroutines).
   - Cons: Compose debug builds are noticeably slower than release, so
     performance must be judged on a minified build; Robolectric is a simulation,
     so a small set of behaviours (real TalkBack, real rotation, real process
     kill) still need manual checks.
2. **Kotlin + Android Views (XML layouts), Espresso tests.**
   - Pros: very mature; smaller APK; Espresso well known.
   - Cons: more boilerplate for a dynamic grid (custom View or 81 TextViews);
     Google now directs new development to Compose; Espresso integration tests
     typically need an emulator, which CI does not have, so integration ACs
     would not run in CI without an owner change to `ci.yml`.
3. **Flutter (Dart).**
   - Pros: one codebase could later target iOS; good widget testing on the host.
   - Cons: iOS is explicitly out of scope; adds a second toolchain (Flutter SDK)
     that `ci.yml` does not install and `scripts/ci.sh` does not detect (owner
     would have to change both); Android accessibility and manifest control are
     one layer removed; coding standards are written for Kotlin.
4. **React Native / Expo (TypeScript).**
   - Pros: large ecosystem; `ci.sh` detects `package.json`.
   - Cons: heavier runtime for an offline puzzle; native build still needs the
     Android toolchain; the Node path in `ci.sh` would not build the APK or run
     Android lint; templates commonly pull in network-related permissions and
     dependencies that would have to be stripped for AC22.
5. **Do nothing** — not viable; the spec needs an app.

## Decision
Option 1. Details:

| Concern | Choice |
|---|---|
| Language | Kotlin (version bundled with / required by the pinned AGP) |
| UI | Jetpack Compose with Material 3, single Activity (`MainActivity`, a `ComponentActivity`), versions via the Compose BOM |
| State | AndroidX `ViewModel` exposing `StateFlow`; kotlinx.coroutines; no `GlobalScope` (an application-scoped `CoroutineScope` is created and owned by the app container) |
| Dependency injection | Manual: a small `AppContainer` created by the `Application` class. No Hilt/Dagger/Koin (not needed for a handful of objects; avoids a dependency) |
| Build | Gradle (wrapper committed, executable bit set, LF endings), Kotlin DSL, version catalog `gradle/libs.versions.toml`, Android Gradle Plugin (AGP). Single module `:app` |
| SDK levels | `minSdk 26`, `targetSdk 36`, `compileSdk 36` |
| JDK | Builds and tests must run on JDK 17 (what CI installs). Java/Kotlin bytecode target 17 |
| Unit tests | JUnit 4, kotlinx-coroutines-test |
| Integration tests | Robolectric + AndroidX Test (core, ext-junit) + Compose UI test (`ui-test-junit4`) running on the JVM inside `testDebugUnitTest` (see ADR 0003) |
| Lint | Android Lint with `abortOnError = true` and `warningsAsErrors = true`, configured in `app/lint.xml`. Only checks that depend on the outside world are disabled there (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`), so CI does not start failing when a library releases a new version. Plus ktlint via the `org.jlleitschuh.gradle.ktlint` Gradle plugin. The `lint` task is wired to depend on `ktlintCheck`, so the `lint` call in `scripts/ci.sh` runs both |
| Coverage | Kover Gradle plugin (`org.jetbrains.kotlinx.kover`), Android variant reports `koverXmlReportDebug` / `koverHtmlReportDebug`, covering JVM unit and Robolectric tests |
| Persistence | One JSON file in app-private storage, written with the platform `android.util.AtomicFile` and encoded with kotlinx.serialization (see ADR 0002). No database |
| Release build | R8 minify + resource shrinking on; signing config read only from environment variables / the user's `~/.gradle/gradle.properties`, never from the repo; unsigned if absent. A `benchmark` build type (`initWith(release)`, debug signing key, not debuggable) exists for on-device performance measurement |
| Versioning | `VERSION_NAME` / `VERSION_CODE` in the root `gradle.properties`, read by `app/build.gradle.kts` |

Version pinning rule (task 1): every library and plugin is pinned to an exact
stable version in `gradle/libs.versions.toml`; no `+`, ranges or snapshots.
Target the latest stable AGP line (AGP 9.x with built-in Kotlin, Gradle 9.1+,
JDK 17). If, during task 1, Kover or ktlint-gradle is shown not to work with
AGP 9 built-in Kotlin, the developer may instead pin the latest AGP 8.x with
the `org.jetbrains.kotlin.android` plugin and record the reason in the PR. Both
are within this decision and need no new ADR. Robolectric's simulated SDK is
pinned to 35 in `app/src/test/resources/robolectric.properties`, because
Robolectric needs JDK 21 to simulate SDK 36 and CI provides JDK 17. Selected
tests also run at `@Config(sdk = [26])` to cover the minimum SDK.

Why this beats the alternatives: it is the mainstream Android default, it runs
in the existing CI and `scripts/ci.sh` without owner changes, it lets every
automated AC (including the integration ones) run on every PR without an
emulator, and it costs nothing.

## Consequences
- Easier: one language for domain, UI and tests; integration tests and coverage
  run in CI on every PR; agents follow the existing Kotlin coding standards.
- Harder: Robolectric does not prove real-device TalkBack, rotation or process
  kill. These are covered by the manual checks the spec already requires
  (AC19, AC21, AC22, AC23), and optionally by instrumented tests run by hand on
  an emulator.
- The owner should consider (owner-only files, not required for this run):
  adding a Gradle ecosystem entry to Dependabot, adding the Gradle wrapper
  validation action to `ci.yml`, and adding the coverage command to
  `scripts/ci.sh` so CI also produces the Kover report.
- Reversal: the stack is chosen before any feature code exists. Moving off
  Compose later would mean rewriting the UI layer only. The domain layer (rules,
  solver, generator, save validation) is plain Kotlin with no Android types and
  would survive.

## For new dependencies
All are mainstream, actively maintained and free.

| Dependency | Licence | Maintenance | Known vulnerabilities | Size / performance impact |
|---|---|---|---|---|
| Kotlin stdlib, kotlinx.coroutines | Apache-2.0 | JetBrains, frequent releases | None known at time of writing | Small; standard for Kotlin Android |
| AndroidX Core, Activity Compose, Lifecycle ViewModel Compose | Apache-2.0 | Google, frequent releases | None known | Standard |
| Jetpack Compose (BOM, ui, foundation, material3) | Apache-2.0 | Google, monthly releases | None known | Adds several MB before R8; R8 shrinks release |
| kotlinx.serialization (json) + plugin | Apache-2.0 | JetBrains, regular releases | None known | Small; compile-time codegen, no reflection |
| JUnit 4 (test only) | EPL-1.0 | Stable, maintenance releases | None known | Test only |
| kotlinx-coroutines-test (test only) | Apache-2.0 | JetBrains | None known | Test only |
| Robolectric (test only) | MIT | Google-sponsored, releases several times a year; supports SDK 23-36 | None known | Test only; downloads SDK jars from Maven Central at test time (CI and dev machines, not the app) |
| AndroidX Test core / ext-junit (test only) | Apache-2.0 | Google | None known | Test only |
| Compose ui-test-junit4 (test only) | Apache-2.0 | Google | None known | Test only. `ui-test-manifest` is deliberately **not** added (it adds an exported activity to debug manifests); tests use `createAndroidComposeRule<MainActivity>()` |
| Kover Gradle plugin (build only) | Apache-2.0 | JetBrains, 0.9.x line actively released (0.9.8, March 2026) | None known | Build time only |
| ktlint Gradle plugin `org.jlleitschuh.gradle.ktlint` (build only) | MIT | Community, maintained (14.x, Nov 2025) | None known | Build time only |

Dependencies the project deliberately does not take: Hilt/Dagger/Koin, Room,
DataStore, Navigation (single screen), any analytics, crash-reporting, ads or
networking library. The exported `ProfileInstallReceiver` that
`androidx.profileinstaller` (pulled in by Compose) adds to the manifest is
removed with `tools:node="remove"`, because the spec allows no exported
component other than the launcher activity.
