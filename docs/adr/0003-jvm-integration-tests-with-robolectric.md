# ADR 0003: Run integration tests on the JVM with Robolectric

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-sudoku-mvp`

## Context
Eleven ACs are integration tests (AC1, AC5, AC6, AC9, AC11, AC12, AC14, AC15,
AC16, AC17, AC18), plus the manifest check in AC22. The spec names two risks:
CI must be able to run them (or the design must say how), and process-death /
relaunch tests (AC15-AC18) can be flaky on emulators.

CI (`.github/workflows/ci.yml`, owner-only) runs `bash scripts/ci.sh` on
`ubuntu-latest` with JDK 17 and no emulator. `scripts/ci.sh` runs
`./gradlew --no-daemon lint testDebugUnitTest`. Only tests under
`app/src/test` run in CI unchanged.

## Options considered
1. **Robolectric + Compose UI test on the JVM, in `app/src/test`** (chosen).
   - Pros: runs inside `testDebugUnitTest`, so every PR runs all automated ACs
     with no owner change to CI; deterministic (no emulator boot, animations,
     or timing); counts toward Kover coverage, so UI code meets the
     changed-lines threshold; fast.
   - Cons: simulates Android rather than running it; cannot prove real TalkBack
     speech, rotation or a real process kill.
2. **Instrumented tests (`app/src/androidTest`) on an emulator in CI.**
   - Pros: real device behaviour.
   - Cons: needs an owner change to `ci.yml` (KVM, emulator action), adds
     minutes per run against a 30-minute limit, and is the exact flakiness risk
     the spec names.
3. **Unit tests only; integration ACs manual.**
   - Pros: simplest.
   - Cons: G3.4 needs a named test for every AC; manual-only would leave
     regressions undetected between runs.

## Decision
Option 1. Rules:
- Integration tests live in `app/src/test/java/.../integration/` (or next to the
  screen they test), use `@RunWith(AndroidJUnit4::class)` and
  `createAndroidComposeRule<MainActivity>()`, and are named after the AC
  (`AC15_relaunch_restores_entries_and_mistakes`).
- Tests use a test `Application` subclass (`@Config(application = ...)`) that
  supplies a seeded `Random` and a test dispatcher, so puzzles and save timing
  are deterministic. No sleeps; synchronisation via `composeRule.waitForIdle()`
  and the test dispatcher.
- "Process death" is simulated by what actually survives a real process kill:
  only the files on disk. The test closes the `ActivityScenario` (destroying the
  activity and clearing the ViewModel), discards the app container (so no
  in-memory object survives), builds a new container over the same
  `filesDir`, and launches a new scenario. Production code is required to keep
  no game state in statics or the container; the repository re-reads the file
  on every load.
- `robolectric.properties` pins `sdk=35` (JDK 17 compatible). Selected tests
  (at least AC1 and one relaunch test) also run with `@Config(sdk = [26])`.
- Real-device behaviour is covered by the spec's manual checks (AC19-AC23),
  including a manual process-kill check with `adb shell am kill` recorded by
  the tester. Instrumented tests in `app/src/androidTest` are optional, run by
  hand with `./gradlew connectedDebugAndroidTest`, and never gate CI.

## Consequences
- Easier: all automated ACs run on every PR; no emulator flakiness in CI.
- Harder: Robolectric version upgrades can change simulated behaviour; pin it
  and upgrade deliberately.
- Reversal: the Compose test APIs are the same for instrumented tests, so tests
  can be moved to `androidTest` later with small changes if the owner adds an
  emulator job to CI.

## For new dependencies
Robolectric, AndroidX Test core / ext-junit and Compose `ui-test-junit4` (test
scope only). Licences, maintenance and vulnerability status are in ADR 0001.
