# Design: Sudoku for Android (MVP)

- Run: `20261019-sudoku-mvp`
- Spec: `docs/specs/20261019-sudoku-mvp.md` (amended spec re-approved at Gate A, 2026-10-19T06:53:26Z, commit f1dd338: Accessibility NFR reworded and assumption A10 added)
- Epic: #5
- ADRs:
  - `docs/adr/0001-technology-stack.md` (Accepted; owner approval pending at the stack checkpoint)
  - `docs/adr/0002-saved-game-format.md` (Accepted)
  - `docs/adr/0003-jvm-integration-tests-with-robolectric.md` (Accepted)
- Project profile: `docs/project.md` (Status: PROPOSED)

## Approach
A single-screen, single-Activity Kotlin app built with Jetpack Compose
(ADR 0001), in three layers inside one Gradle module `:app`:

1. **Domain (plain Kotlin, no Android types).** Immutable value types for the
   grid, puzzle and game state, plus pure functions for the game rules (select,
   enter digit, erase, mistake, solved, read-only after solve). A bitmask
   backtracking solver that counts solutions up to a limit, and a generator that
   builds a random full solution and then removes cells in random order, keeping
   each removal only while the puzzle still has exactly one solution, until a
   random target of 36-46 givens is reached (spec A1). Randomness is injected
   (`kotlin.random.Random`), so tests are deterministic.
2. **Data.** One JSON save file written atomically after every change, and
   strictly validated on load (ADR 0002). Writes go through a conflated queue on
   an application-scoped coroutine, so the latest state is always the one
   written and leaving the screen never cancels a pending save.
3. **UI.** A `GameViewModel` holds the `GameState`, calls the domain rules,
   hands every new state to the saver, and exposes a `StateFlow<GameUiState>`.
   Compose renders the grid, a 1-9 digit pad, Erase and New Game, and the
   "solved" message. Each cell carries a TalkBack description. Mistakes have a
   non-colour cue.

Grid and touch-target sizing follow the spec's Accessibility NFR and
assumption A10. The nine cells in each grid row share the available screen
width equally (each row is nine cells with equal `weight`, each cell square),
so cells are about 38-45 dp on 360-411 dp wide phones. Each grid cell's touch
target must be **at least 24x24 dp on a 360 dp wide portrait screen**. The
grid never scrolls. Every other interactive control (digit buttons 1-9, Erase
and New Game) has a touch target of **at least 48x48 dp**. Both minimums are
enforced by automated tests (`TouchTargetTest`, tasks 4 and 5) and checked by
hand with Accessibility Scanner.

On launch the ViewModel loads the save off the main thread. A valid save is
restored. If the save is absent or invalid, a new puzzle is generated on
`Dispatchers.Default`. A small loading indicator shows until the grid is ready,
then the activity reports "fully drawn" for performance measurement. New Game
generates a new puzzle the same way, replaces the state and saves it.

Mistake detection compares each entry only with the unique solution (A3). The
mistake flag is derived, never stored. Selection lives only in the ViewModel.
The activity is locked to portrait, and the ViewModel keeps state across any
other configuration change (for example dark mode or font size).

Integration ACs run on the JVM with Robolectric inside `testDebugUnitTest`
(ADR 0003), so they run in the existing CI with no emulator. Manual ACs are
checked on emulators by the tester (see Test strategy).

## Components touched
All new (no existing code).

| Component / module | Change |
|---|---|
| Gradle build (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`) | New. Wrapper committed with executable bit and LF endings. Plugins: AGP, Compose compiler, kotlinx.serialization, Kover, ktlint. `lint` depends on `ktlintCheck`. Versions pinned |
| `app/build.gradle.kts`, `app/lint.xml`, `app/proguard-rules.pro` | New. `minSdk 26`, `targetSdk 36`, `compileSdk 36`; build types `debug`, `release` (R8 + resource shrinking, env-based signing), `benchmark` (`initWith(release)`, debug key, not debuggable); `testOptions.unitTests.isIncludeAndroidResources = true`; Kover config |
| `app/src/main/AndroidManifest.xml` | New. No `uses-permission`; `MainActivity` is the only exported component (launcher), `screenOrientation="portrait"`; `usesCleartextTraffic="false"`; `allowBackup="true"` (spec allows, data validated on restore); `tools:node="remove"` on `androidx.profileinstaller.ProfileInstallReceiver` |
| `SudokuApplication`, `AppContainer`, `MainActivity` | New. Manual DI; application `CoroutineScope` (SupervisorJob); `enableEdgeToEdge()`; `setContent { SudokuTheme { GameRoute() } }` |
| `domain/` | New: `Grid`, `Puzzle`, `GameState`, `GameRules`, `SudokuSolver`, `PuzzleGenerator`, `SavedGameValidator`, `GameRepository` (interface) |
| `data/` | New: `SavedGameDto`, `SavedGameCodec`, `FileGameRepository`, `GameSaver` |
| `ui/` | New: `GameViewModel`, `GameUiState`, `GameScreen`, `SudokuGrid`, `SudokuCell`, `DigitPad`, `GameActions`, `SolvedBanner`, `theme/` |
| `app/src/main/res/values/strings.xml`, `themes.xml`, launcher icon | New. All UI text and TalkBack text in resources (English only, A8); placeholder name and icon (A9) |
| `app/src/test/...` | New: unit tests, Robolectric integration tests, `TestSudokuApplication`, `robolectric.properties` (`sdk=35`) |
| `.gitignore` | Add Android/Gradle entries: `.gradle/`, `build/`, `local.properties`, `*.apk`, `*.aab`, `.idea/`, `captures/`, `.kotlin/` |
| `README.md` | How to build, test and run (commands from `docs/project.md`) |

## Interfaces and data

### Domain types (plain Kotlin)
Constants in one place (`domain/SudokuConstants.kt`): `GRID_SIZE = 9`,
`BOX_SIZE = 3`, `CELL_COUNT = 81`, `EMPTY = 0`, `MIN_GIVENS = 36`,
`MAX_GIVENS = 46`.

```kotlin
// Cells in row-major order: index = row * 9 + column (0-based).
class Grid(cells: IntArray)                 // immutable copy; 0 = empty, 1..9 = digit
  fun get(index: Int): Int
  fun with(index: Int, digit: Int): Grid
  fun isValidSolution(): Boolean            // rows, columns, boxes each 1..9 once
  val filledCount: Int

data class Puzzle(val givens: Grid, val solution: Grid)
  // init: solution valid, each non-empty given equals solution
  fun isGiven(index: Int): Boolean

data class GameState(
  val puzzle: Puzzle,
  val entries: Grid,                        // player digits only; EMPTY at givens
  val selected: Int? = null,
)
  fun digitAt(index: Int): Int              // given or entry
  fun isMistake(index: Int): Boolean        // entry != EMPTY && entry != solution
  val isSolved: Boolean                     // every non-given entry == solution
  val isFull: Boolean

object GameRules {
  fun newGame(puzzle: Puzzle): GameState                 // no entries, no selection
  fun select(state: GameState, index: Int): GameState     // any cell, incl. givens
  fun enterDigit(state: GameState, digit: Int): GameState // no-op if no selection, given, or solved
  fun erase(state: GameState): GameState                  // no-op if no selection, given, or solved
}

class SudokuSolver {
  fun countSolutions(grid: Grid, limit: Int = 2): Int
  val lastSearchNodes: Long                 // effort counter for the performance test
}

class PuzzleGenerator(private val random: Random, private val solver: SudokuSolver) {
  fun generate(): Puzzle
}

interface GameRepository {
  suspend fun load(): GameState?            // null = absent or invalid (already logged and deleted)
  suspend fun save(state: GameState)        // throws only on programmer error; IO failures handled inside
}

object SavedGameValidator {
  fun validate(dto: SavedGameDto): GameState?   // all ADR 0002 load rules
}
```

Rules that cover specific ACs:
- AC7: `enterDigit` on a non-given cell with an entry replaces it.
- AC8: `enterDigit` / `erase` leave a given cell unchanged (givens are never
  stored in `entries`).
- AC10 / AC9: `isMistake` is derived, so entering the solution digit clears
  the mistake immediately.
- AC12 / AC16 / A7: once `isSolved`, `enterDigit` and `erase` are no-ops.
- AC13: a full board with any mistake is not solved and stays editable.

### Generator
1. Fill a full grid by randomised backtracking (digit order shuffled with the
   injected `Random`).
2. Pick a target `t` uniformly in `[36, 46]`. Visit cells in random order. Clear
   each cell, and restore it if `countSolutions(limit = 2) != 1`. Stop when the
   givens count reaches `t`.
3. If the order runs out above 46 givens (not expected in practice), restart
   from step 1, at most `MAX_GENERATION_ATTEMPTS` (named constant, e.g. 20)
   times. Then throw `IllegalStateException` (fail loudly, never an invalid
   puzzle).
4. Return `Puzzle(givens, solution)`. The constructor re-checks consistency.

### Save file
`<filesDir>/saved_game.json`, schema version 1, exactly as in ADR 0002
(`version`, `givens`, `solution`, `entries`, `solved`). The JSON codec uses
`Json { ignoreUnknownKeys = false; isLenient = false }`. There are no
migrations in this run. A future format bump must read v1 or let it fall back
to a new game, which is safe by construction (AC18 path).

### ViewModel and UI state
```kotlin
sealed interface GameUiState {
  data object Loading : GameUiState
  data class Ready(
    val cells: List<CellUi>,                // 81 entries
    val isSolved: Boolean,
    val isGenerating: Boolean,              // New Game in progress; input ignored
  ) : GameUiState
}
data class CellUi(val row: Int, val column: Int, val digit: Int,
                  val isGiven: Boolean, val isMistake: Boolean, val isSelected: Boolean)

class GameViewModel(repository, saver, generator, defaultDispatcher) : ViewModel {
  val uiState: StateFlow<GameUiState>
  fun onCellTapped(index: Int); fun onDigit(digit: Int); fun onErase(); fun onNewGame()
}
```
- Every state change that alters `entries`, the puzzle or the solved flag calls
  `saver.submit(state)`. Selection-only changes are not saved (selection is not
  persisted).
- New Game regenerates if the new puzzle's givens equal the current ones (AC14
  "different"), at most 3 times.
- `ReportDrawnWhen { uiState is Ready }` in `GameRoute` (activity-compose API),
  so `adb logcat` shows a "Fully drawn" time for the performance procedure.

### Navigation and intents
One screen, no navigation library. `MainActivity` reads nothing from its
launch intent.

## Risks and mitigations
| Risk | Mitigation |
|---|---|
| **Grid cells cannot have 48 dp touch targets on phone widths** (9 x 48 = 432 dp > 360-411 dp); resolved by spec A10 and the amended Accessibility NFR | Per A10, the nine cells in each row share the available width equally (equal `weight`, square cells, no scrolling), so cells are about 38-45 dp on 360-411 dp phones, and each cell's touch target must be at least 24x24 dp on a 360 dp wide portrait screen. Cells are as large as the width allows (no outer margin beyond the safe-area insets). The digit pad, Erase and New Game are each at least 48x48 dp. Enforced by `TouchTargetTest` (Robolectric, 360 dp wide screen): task 4 adds `NFR_grid_cells_share_width_equally_and_are_at_least_24dp_at_360dp_width` and `NFR_digit_pad_and_erase_are_at_least_48dp`; task 5 adds `NFR_new_game_button_is_at_least_48dp`. Checked by hand with Accessibility Scanner on a 360 dp wide emulator (tasks 4, 5, repeat 7) |
| Generation too slow on low-end devices (2 s target) | Uniqueness checks on 36-46-given grids are cheap: a bitmask solver with most-constrained-cell-first ordering and `limit = 2` early exit. Generation runs on `Dispatchers.Default` with a loading indicator, never on the main thread. Task 3's `GenerationPerformanceTest` runs 200 seeded generations and asserts a **deterministic** effort budget (max solver nodes per puzzle, named constant set at about 10x the observed maximum), which cannot flake. It also records wall-clock p50/p95/max to `app/build/perf/generation.json` with only a loose 2000 ms per-puzzle ceiling. On-device time is measured on a minified `benchmark` build on the API 26 emulator (procedure in `docs/project.md`). Fallback if a measurement ever fails: pre-generate the next puzzle in the background after a game starts. This would be a follow-up, not built now |
| Flaky process-death / relaunch tests on CI emulators (AC15-AC18) | No emulator in CI (ADR 0003). Relaunch is simulated on the JVM by keeping only the files on disk: close the scenario, discard the app container, build a new one over the same `filesDir`, relaunch. Saves use an injected test dispatcher, so "save finished" is deterministic. No sleeps. A real process kill is checked by hand (`adb shell am kill`) and recorded by the tester |
| Pending save lost when the screen closes | `GameSaver` runs in the application scope, not `viewModelScope`. Writes are atomic (`AtomicFile`), so a kill mid-write keeps the previous complete save. A save file is about 300 bytes, so a write takes milliseconds |
| Corrupt, tampered or backed-up-and-restored save data | Strict validation (ADR 0002): size cap, strict decode, version, lengths, character set, valid solution, givens consistent and unique, solved flag consistent. On any failure the app deletes the file, logs without game content, and starts a new game (AC18) |
| Save write fails (disk full, IO error) | `failWrite()`, log, continue. The next change submits a full snapshot, so the next successful write captures the latest state (NFR). Unit-tested with a repository whose directory is unwritable |
| `gradlew` not executable on the Linux CI runner (repo authored on Windows); `ci.sh` checks `[ -x ./gradlew ]` and would fail with "No build system detected" | Task 1 commits `gradlew` with mode 100755 (`git update-index --chmod=+x gradlew`) and LF endings (`.gitattributes` already forces LF). Task 1's acceptance includes a green CI run of `bash scripts/ci.sh` |
| Robolectric needs JDK 21 to simulate SDK 36; CI has JDK 17 | `robolectric.properties` pins `sdk=35`. `compileSdk` / `targetSdk` stay 36. Selected tests also run at SDK 26 |
| Kover / ktlint-gradle compatibility with AGP 9 built-in Kotlin is unverified | ADR 0001 lets task 1 fall back to the latest AGP 8.x with the Kotlin Android plugin, with the reason in the PR |
| Lint failing over time because new library versions appear | `app/lint.xml` disables only the version-check lint rules (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`). All code checks stay errors |
| Compose debug builds are slow, so performance judged on debug would mislead | Performance is measured on the minified `benchmark` build type |
| R8 strips kotlinx.serialization classes in release | The serialization plugin ships consumer keep rules. Task 7's manual check runs the minified `benchmark` build through save, kill and relaunch |
| Largest system font truncates digits in cells | Cell digit size is the smaller of the font-scaled size and a fraction of the cell height (computed in px), so digits never overflow. Checked manually at the largest font size (task 4) |
| Edge-to-edge is enforced when targeting SDK 35+ | `enableEdgeToEdge()` plus `Scaffold` / `safeDrawing` insets so nothing sits under the system bars |
| AC14 "different puzzle" could in theory repeat | The ViewModel compares givens and regenerates (at most 3 tries). Tested with a stub generator that returns the same puzzle first |
| Exported components pulled in by libraries | `ui-test-manifest` is not added. `ProfileInstallReceiver` is removed via `tools:node="remove"`. A test asserts the only exported component is `MainActivity` |

## Security considerations
Against `docs/governance/security-standards.md`:
- **Permissions:** the manifest declares no `uses-permission`. The merged
  manifest must not contain `android.permission.INTERNET` or
  `ACCESS_NETWORK_STATE`, and no dangerous (runtime) permissions. AndroidX may
  add the app's own signature-level
  `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. This is allowed
  (not a runtime or network permission), and the test asserts that only this
  permission may appear. `usesCleartextTraffic="false"` is set anyway.
- **Exported components:** only `MainActivity` (launcher, required). Library
  receiver `ProfileInstallReceiver` removed. `InitializationProvider` stays
  unexported. A Robolectric test (task 1) enforces this on the merged manifest.
  The tester confirms on the release APK with
  `aapt2 dump permissions` / `aapt2 dump xmltree --file AndroidManifest.xml`.
- **Input validation:** the save file is untrusted (it can be restored from a
  backup or tampered with on a rooted device). It is validated per ADR 0002
  before use, including a 4 KiB size cap before parsing and strict decoding.
  On-screen input is limited to the 1-9 pad and Erase, and the domain functions
  reject out-of-range digits and indices with `require`. The launch intent is
  ignored. File paths are fixed constants and never built from input.
- **Data protection:** no personal data (spec "Data and privacy"). The save is
  in app-private `filesDir`. `allowBackup="true"` is kept deliberately: the spec
  accepts auto-backup of non-personal game state, and restored data goes through
  the same validation. Logs never include game content and do not log on the
  hot path.
- **Secrets and signing:** no keys in the repo. Release signing reads keystore
  path and passwords from environment variables or `~/.gradle/gradle.properties`
  only, and the release is unsigned if they are absent. `.gitignore` already
  excludes `*.keystore`, `*.jks` and `.env`, and task 1 adds `local.properties`.
  Release and benchmark builds are minified (R8).
- **No back doors:** no debug-only "near-solved" path is built (the spec allows
  one but does not require it). The AC23 tester solves by hand, which live
  mistake highlighting makes quick. Test helpers (`TestSudokuApplication`,
  seeded generators) live only in `app/src/test`.
- **Dependencies:** only those in ADR 0001, pinned. No networking, analytics,
  ads or crash-reporting libraries. Robolectric downloads its SDK jars at test
  time on CI and dev machines only. Owner may add Dependabot for Gradle and
  Gradle wrapper validation (owner-only files).
- **No WebView, no SQL, no shell, no cleartext.**

## Test strategy
All automated tests are in `app/src/test` and run with
`./gradlew --no-daemon testDebugUnitTest`, which is what `scripts/ci.sh` runs in
CI on every PR. Coverage comes from
`./gradlew --no-daemon koverXmlReportDebug koverHtmlReportDebug`, and
changed-lines coverage must be at least 80%. Robolectric tests count toward
coverage, so UI code is covered too. Test names start with the AC ID (or `NFR_`
for non-functional checks). Randomness is seeded and dispatchers are injected.
No real clock, network or sleeps.

### Unit tests (plain JVM, domain and data)
| Area | What is tested | ACs |
|---|---|---|
| `GameRules` | replace entry; givens immutable under digit and Erase; correct digit clears mistake; full board with a mistake is not solved and stays editable; no-ops when nothing is selected; read-only after solved; digit range rejected | AC7, AC8, AC10, AC13 (also supports AC9, AC11, AC12) |
| `Grid` / `Puzzle` | valid-solution check; given/solution consistency enforced | AC3 support |
| `SudokuSolver` | 0 / 1 / 2+ solutions on known grids; limit respected | AC2 support |
| `PuzzleGenerator` | over 100+ seeds: exactly one solution; 36-46 givens; givens equal solution; solution valid | AC2, AC3 |
| `GenerationPerformanceTest` | 200 seeds: deterministic node budget; records timings JSON | Performance NFR |
| `SavedGameCodec` + `SavedGameValidator` | round trip keeps puzzle, entries (incl. mistakes), solved flag; rejects each invalid case (oversize, malformed JSON, unknown/missing key, wrong version, wrong length, bad char, invalid solution, given != solution, given count out of range, non-unique givens, entry on a given, inconsistent solved flag) | AC15, AC16, AC18 |
| `FileGameRepository` | missing file returns null; unreadable or invalid file returns null and is deleted; write failure does not throw and the next write succeeds; atomic replace keeps the old save if a write fails | AC18, save-failure NFR |
| `GameSaver` | only the latest snapshot is written; survives a failed write | A5, save-failure NFR |
| `GameViewModel` (with fakes) | mistake shown on entry; Erase clears; New Game resets entries, selection and solved, and differs from current; saves on change, not on selection | AC9, AC11, AC14 support |

### Integration tests (Robolectric + Compose UI test, JVM)
Use `createAndroidComposeRule<MainActivity>()` with `TestSudokuApplication`
(seeded generator, test dispatcher, temp-safe `filesDir`). Cells are found by
test tags (`cell_<row>_<col>`) and the controls by their content description.
- AC1: fresh install shows 81 cells, 36-46 with digits, the rest empty. Also
  run at `@Config(sdk = [26])`.
- AC5: tap empty cell, then it is marked selected (semantics `selected = true`).
  Tap another, and the first is no longer selected.
- AC6: select empty cell, tap digit, cell shows digit.
- AC9: enter a wrong digit, and the cell is immediately reported as a mistake
  (semantic state and the mistake cue node).
- AC11: Erase on a correct entry and on a mistake leaves the cell empty, not a
  mistake.
- AC12: seeded game filled to one empty cell through the UI or a seeded save
  file; entering the last digit shows the solved message; digits and Erase then
  change nothing.
- AC14: New Game (from in-progress and from solved) gives a different puzzle,
  no entries, mistakes or selection, and no solved message.
- AC15: entries including a mistake, then relaunch (scenario closed, container
  discarded, new container over the same files, new scenario). Same puzzle,
  entries and mistakes. Also run at SDK 26.
- AC16: solved, then relaunch. Solved message shown, read-only.
- AC17: entries, then New Game, then relaunch. The newer game is shown, and
  the earlier givens and entries are gone (file contains only the new game).
- AC18: save file missing, empty, garbage, valid JSON with bad content. Each
  launch shows a new puzzle with no crash.
- AC21 (automated support): content descriptions of a given cell, an empty
  cell, a correct entry and a mistake contain row, column, digit or "empty",
  "given", and "mistake". Digit buttons, Erase and New Game have names.
- AC19 (automated part): `MainActivity`'s `ActivityInfo.screenOrientation`
  is portrait.
- AC22 (manifest part): `PackageManager` info from the merged manifest has no
  INTERNET, no ACCESS_NETWORK_STATE and no dangerous permissions, and exports
  only `MainActivity`.
- NFR support, touch targets (Accessibility NFR, A10), in
  `app/src/test/.../ui/TouchTargetTest.kt`. The class runs with
  `@Config(qualifiers = "w360dp-h640dp-port-xhdpi")`, so the screen is a
  360 dp wide portrait phone:
  - `NFR_grid_cells_share_width_equally_and_are_at_least_24dp_at_360dp_width`
    (task 4): for every `cell_<row>_<col>` node (all 81),
    `assertWidthIsAtLeast(24.dp)` and `assertHeightIsAtLeast(24.dp)`. It also
    confirms the equal share: within each row the nine cells' widths differ by
    at most 1 px (layout rounding), all 81 cells have the same width and
    height within 1 px, and the nine cells of a row together span the grid's
    width minus its box borders (no scrolling, no clipped cell).
  - `NFR_digit_pad_and_erase_are_at_least_48dp` (task 4): the 1-9 digit
    buttons and Erase each pass `assertWidthIsAtLeast(48.dp)` and
    `assertHeightIsAtLeast(48.dp)` (touch bounds).
  - `NFR_new_game_button_is_at_least_48dp` (task 5): the same 48x48 dp check
    on New Game.

### Manual checks (tester records each on the task PR)
For each manual AC the tester posts a "Manual check" comment on the task PR
with: emulator image and API level (or device), build type and commit SHA, the
exact steps, the observed result, PASS or FAIL. No screenshots containing
personal data (emulators only). Task 7 repeats all manual checks on the final
feature build, as evidence for the release checklist item "Manual checks done
and recorded".

| AC | Task | How |
|---|---|---|
| AC4 | 4 (repeat 7) | API 34+ phone emulator, debug build: start a game, enter several digits, confirm givens (bold, on-surface colour) and entries (regular weight, primary colour) are distinguishable at a glance, in light and dark system theme |
| AC20 | 4 (repeat 7) | Developer options, "Simulate colour space: Monochromacy". Enter a wrong digit and a correct digit, and confirm the mistake is identifiable by its non-colour mark (underline bar) |
| AC21 | 5 (repeat 7) | Emulator image with Google Play (includes TalkBack), API 26+. Enable TalkBack, swipe through a given cell, an empty cell, a correct entry, a mistake, all digit buttons, Erase and New Game, and record each announcement |
| AC19 | 1 (lock), 7 (with game state) | Auto-rotate on. Mid-game with a selection and a mistake, rotate to landscape and back. Layout stays portrait, and digits, mistakes and selection are unchanged |
| AC22 | 1 (manifest), 7 (session) | Flight mode on (Wi-Fi off). Launch, play including a mistake and an Erase, New Game, force-stop, relaunch, and confirm AC1-AC18 behaviours. Also run `aapt2 dump permissions` on the release APK and confirm no INTERNET |
| AC23 | 7 | API 26 phone emulator (`system-images;android-26;google_apis;x86_64`). Install the debug or benchmark build, play a full game to "solved" by hand (live mistake highlighting guides each digit), and confirm no crash and the message shows |
| Process kill (supports AC15-AC17) | 7 | Mid-game, press Home, run `adb shell am kill <applicationId>`, relaunch from the launcher, and confirm state. Repeat with Developer options "Don't keep activities" |
| Font scaling and touch targets (Accessibility NFR, A10) | 4, 5 (repeat 7) | Largest system font size (and Display size largest), confirm grid digits do not overlap or truncate. Set the emulator to a 360 dp wide portrait screen (`adb shell wm size 720x1280` and `adb shell wm density 320`; reset with `wm size reset` / `wm density reset`) and run Accessibility Scanner on the game screen. Record: grid cells each at least 24x24 dp and equal in width across a row (Scanner's default 48 dp suggestion is expected to flag cells; that is accepted per A10 only if the reported cell size is at least 24x24 dp); digit buttons 1-9, Erase and New Game each at least 48x48 dp with no touch-target finding |
| Performance (NFR) | 7 (and performance agent) | Procedure in `docs/project.md` on the minified benchmark build: first-launch puzzle at most 2 s, restore overhead at most 1 s; digit feedback looks instant (100 ms target; profile with the "Profile GPU rendering" bars if in doubt) |

## Task breakdown
Each task is one PR from `task/20261019-sudoku-mvp-<n>-<slug>` into
`feature/20261019-sudoku-mvp`. Task 1 merges before any other task starts, and
tasks 2+ start only after the owner approves the stack.

| # | Title | ACs | Likely files | Depends on | Size |
|---|---|---|---|---|---|
| 1 | Walking skeleton: Gradle/Compose scaffold, manifest hardening, lint + ktlint, Kover, Robolectric, first tests, CI green | AC19 (portrait lock, automated part), AC22 (merged-manifest check) | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.{jar,properties}`, `gradlew` (mode 755), `gradlew.bat`, `app/build.gradle.kts`, `app/lint.xml`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/io/github/greavemrsvg/sudoku/{SudokuApplication,AppContainer,MainActivity}.kt`, `.../ui/theme/*`, `app/src/main/res/values/{strings,themes}.xml`, launcher icon, `app/src/test/resources/robolectric.properties`, `app/src/test/.../ManifestTest.kt`, `.gitignore`, `README.md` | none | M |
| 2 | Domain: grid, puzzle, game state and game rules | AC7, AC8, AC10, AC13 | `domain/{SudokuConstants,Grid,Puzzle,GameState,GameRules}.kt`, `app/src/test/.../domain/{GridTest,PuzzleTest,GameRulesTest}.kt` | 1 | M |
| 3 | Domain: solver and easy-puzzle generator with performance test | AC2, AC3 | `domain/{SudokuSolver,PuzzleGenerator}.kt`, `app/src/test/.../domain/{SudokuSolverTest,PuzzleGeneratorTest,GenerationPerformanceTest}.kt` | 2 | M |
| 4 | Game screen: grid, selection, digit pad, Erase, live mistakes (in-memory) | AC1, AC4, AC5, AC6, AC9, AC11, AC20 | `ui/{GameViewModel,GameUiState,GameScreen,SudokuGrid,SudokuCell,DigitPad,GameActions}.kt`, `ui/theme/Color.kt`, `AppContainer.kt`, `MainActivity.kt`, `res/values/strings.xml`, `app/src/test/.../ui/{GameViewModelTest,GameScreenTest,TouchTargetTest}.kt`, `app/src/test/.../TestSudokuApplication.kt` | 2, 3 | M |
| 5 | Solved state, New Game, and TalkBack semantics | AC12, AC14, AC21 | `ui/{GameViewModel,GameScreen,SudokuCell,GameActions,SolvedBanner}.kt`, `res/values/strings.xml`, `app/src/test/.../ui/{GameViewModelTest,SolvedAndNewGameTest,AccessibilityTest,TouchTargetTest}.kt` | 4 | M |
| 6 | Save format: DTO, codec, validator and file repository | AC15, AC16, AC18 (data level) | `data/{SavedGameDto,SavedGameCodec,FileGameRepository,GameSaver}.kt`, `domain/{GameRepository,SavedGameValidator}.kt`, `app/build.gradle.kts` (serialization plugin + runtime), `gradle/libs.versions.toml`, `app/src/test/.../data/{SavedGameCodecTest,SavedGameValidatorTest,FileGameRepositoryTest,GameSaverTest}.kt` | 2, 3 | M |
| 7 | Autosave and restore on launch, relaunch tests, final device and offline verification | AC15, AC16, AC17, AC18, AC19 (manual), AC22 (manual session), AC23 | `ui/GameViewModel.kt`, `AppContainer.kt`, `SudokuApplication.kt`, `MainActivity.kt` (`ReportDrawnWhen`), `app/src/test/.../integration/{RelaunchTest,CorruptSaveLaunchTest}.kt`, `TestSudokuApplication.kt` | 5, 6 | M |

### Task details

**Task 1: Walking skeleton (M).**
Scope: everything in "Components touched" for the build, manifest,
application/activity shells and theme. `MainActivity` shows a placeholder
screen with the app name from resources. Kover applied. ktlint applied, and
`tasks.named("lint") { dependsOn("ktlintCheck") }` set (or the equivalent
per-variant lint task) so `scripts/ci.sh` runs both. `app/lint.xml` as in
ADR 0001. Release and benchmark build types with R8. Env-only signing.
`ProfileInstallReceiver` removed. `ManifestTest` (Robolectric) with tests
`AC22_merged_manifest_requests_no_network_permission`,
`AC22_only_launcher_activity_is_exported` and
`AC19_main_activity_is_locked_to_portrait`.
Done when: every command in `docs/project.md` (Build, Lint, Unit tests,
Coverage) succeeds locally and `bash scripts/ci.sh` passes in GitHub Actions on
the PR. `gradlew` is committed as executable (`git ls-files -s gradlew` shows
`100755`). Versions are pinned. README lists the commands.

**Task 2: Domain rules (M).**
Pure Kotlin types and `GameRules` as in "Interfaces and data". No Android
imports in `domain/`. Tests cover AC7, AC8, AC10, AC13, plus no-op cases
(no selection, solved board) and `require` failures for out-of-range digits
and indices.

**Task 3: Solver and generator (M).**
Bitmask solver (row, column and box masks; most-constrained empty cell first;
early exit at `limit`), node counter, generator as described, with
`MAX_GENERATION_ATTEMPTS`. Tests: AC2 and AC3 over at least 100 seeds each
(`AC2_every_generated_puzzle_has_exactly_one_solution`,
`AC3_generated_puzzles_have_36_to_46_consistent_givens_and_valid_solution`).
`GenerationPerformanceTest` (200 seeds, node budget constant, writes
`app/build/perf/generation.json`).

**Task 4: Game screen, in memory (M).**
`GameViewModel` with an injected generator and `Dispatchers.Default`
replacement. In this task it starts with a freshly generated game and has no
persistence yet; the saver is wired in task 7. Compose grid with thick 3x3 box
borders. Cell visuals: givens bold in on-surface colour; entries regular weight
in primary colour (AC4); selected cell has a tinted background **and** a
thicker border (AC5); mistakes use error colour **and** an underline bar under
the digit (AC20, non-colour cue). Digit sizing as in Risks (font scaling).
Grid layout per spec A10 and the Accessibility NFR: each grid row is nine
cells with equal `Modifier.weight(1f)` and `aspectRatio(1f)`, so the cells
share the available width equally (about 38 dp each at 360 dp width) and each
cell's touch target is at least 24x24 dp on a 360 dp wide portrait screen. The
grid does not scroll. Digit pad 1-9 and Erase, each at least 48x48 dp.
Colours from Material 3 light and dark schemes with dynamic colour off, so
contrast stays at least 4.5:1 for digits and 3:1 for indicators.
`TestSudokuApplication` (seeded) created here. Integration tests AC1 (also at
SDK 26), AC5, AC6, AC9, AC11. Touch-target tests in `TouchTargetTest`
(`@Config(qualifiers = "w360dp-h640dp-port-xhdpi")`):
`NFR_grid_cells_share_width_equally_and_are_at_least_24dp_at_360dp_width`
(every `cell_<row>_<col>` node passes `assertWidthIsAtLeast(24.dp)` and
`assertHeightIsAtLeast(24.dp)`, and cell widths are equal within 1 px across
each row and across the grid) and `NFR_digit_pad_and_erase_are_at_least_48dp`.
Manual: AC4, AC20, font-scale check, and the Accessibility Scanner touch-target
check on a 360 dp wide screen (cells at least 24x24 dp, pad and Erase at least
48x48 dp).

**Task 5: Solved, New Game, TalkBack (M).**
Solved message (`SolvedBanner`, text from resources, announced as a live
region) shown when `isSolved`. Input then ignored (A7). New Game button always
available (A6), no confirmation, at least 48x48 dp. It generates off the main
thread, compares with the current givens, and resets entries, selection and
solved state. Cell semantics: merged content description built from string
resources, "Row r, column c, <digit|empty>[, given][, mistake]", plus
`selected` state; digit buttons "5" (or "Digit 5"), "Erase", "New game".
Integration tests AC12, AC14 (from in-progress and from solved), AC21
(automated support), and `NFR_new_game_button_is_at_least_48dp` added to
`TouchTargetTest`. Manual: AC21 with TalkBack, and the Accessibility Scanner
touch-target check repeated with New Game present.

**Task 6: Save format and repository (M).**
Exactly ADR 0002. `FileGameRepository(directory: File, ioDispatcher)`. It
re-reads the file on every `load()` and caches nothing. On invalid data it
deletes the file, logs a fixed message, and returns `null`. `GameSaver(scope,
repository)` with a conflated channel. Unit tests for the codec, every
validator rejection, the repository (missing, corrupt, unwritable directory,
round trip incl. mistakes and solved flag) and the saver (latest-wins,
continues after failure). ACs covered at data level: AC15 (entries and
mistakes round-trip), AC16 (solved flag round-trip), AC18 (invalid data
rejected without throwing).

**Task 7: Autosave, restore, final verification (M).**
Wire `FileGameRepository` and `GameSaver` into `AppContainer` (application
scope). ViewModel: `Loading`, then `load()`. If null, generate and save. Save
after every entry, Erase, New Game and solve. `ReportDrawnWhen { Ready }`.
`TestSudokuApplication` gains a way to discard and rebuild the container over
the same `filesDir` (test-only code). Integration tests AC15 (also at SDK 26),
AC16, AC17, AC18 (missing, empty, garbage and semantically invalid files).
Manual: AC19 with game state, AC22 flight-mode session plus `aapt2` permission
dump, AC23 on the API 26 emulator, process-kill check, performance procedure,
and a re-run of AC4, AC20, AC21 and the font-scaling and touch-target check on
the final build.

Size check: no task exceeds M. Tasks 4 and 5 split the UI so that each stays
at M. Tasks 6 and 7 split persistence into a pure data layer and its app
wiring.

## Traceability check
| AC | Task(s) | Test type |
|---|---|---|
| AC1 | 4 | integration |
| AC2 | 3 | unit |
| AC3 | 3 | unit |
| AC4 | 4 (manual re-check in 7) | manual |
| AC5 | 4 | integration |
| AC6 | 4 | integration |
| AC7 | 2 | unit |
| AC8 | 2 | unit |
| AC9 | 4 | integration |
| AC10 | 2 | unit |
| AC11 | 4 | integration |
| AC12 | 5 | integration |
| AC13 | 2 | unit |
| AC14 | 5 | integration |
| AC15 | 6 (data), 7 (app) | unit + integration |
| AC16 | 6 (data), 7 (app) | unit + integration |
| AC17 | 7 | integration |
| AC18 | 6 (data), 7 (app) | unit + integration |
| AC19 | 1 (manifest lock), 7 (manual with game state) | integration + manual |
| AC20 | 4 (manual re-check in 7) | manual |
| AC21 | 5 (manual re-check in 7) | manual (+ automated semantics support) |
| AC22 | 1 (merged-manifest check), 7 (flight-mode session) | integration + manual |
| AC23 | 7 | manual |

NFR coverage notes: the Accessibility NFR touch-target minimums (grid cells at
least 24x24 dp and sharing the width equally on a 360 dp wide portrait screen,
per A10; digit buttons, Erase and New Game at least 48x48 dp) are covered by
`TouchTargetTest` (tasks 4 and 5) and the manual Accessibility Scanner check
(tasks 4, 5, repeat 7). The performance NFR is covered by
`GenerationPerformanceTest` (task 3) and the manual procedure (task 7).

Every AC (AC1-AC23) maps to at least one task. Every task (1-7) has at least
one AC. Task count: 7 (limit 8).
