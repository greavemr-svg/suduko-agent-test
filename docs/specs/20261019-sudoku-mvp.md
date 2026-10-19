# Spec: Sudoku for Android (MVP)

- Run: `20261019-sudoku-mvp`
- Epic: #5
- Status: Draft

## Problem
Casual players want a simple, offline Sudoku game on their Android phone that
they can pick up and put down without effort. The MVP must generate a fresh
easy puzzle, let the player fill in cells, show mistakes the moment they are
made so the player learns as they go, and keep their game when they leave the
app. This is the first releasable slice of a new product: it proves the core
play loop end to end on a real device, and later features build on it.

## Users and context
- Casual Sudoku players, including beginners, who want short sessions.
- Android phones running Android 8.0 (API 26) or later, used in portrait
  orientation only. Tablets, foldables in unfolded mode, and landscape are not
  targeted.
- Play may happen with no network connection (commute, flight mode).
- Sessions are often interrupted (calls, switching apps, the OS killing the
  app in the background), so progress must survive interruptions.

## Scope
**In scope**
- Generating a new easy 9x9 Sudoku puzzle that has exactly one solution.
- A 9x9 grid showing given (pre-filled) cells, visually distinct from
  player-entered cells.
- Selecting a cell and entering a digit 1-9 into it; replacing a
  player-entered digit with another digit.
- An Erase action that clears a player-entered cell.
- Live mistake highlighting: a player-entered digit that differs from the
  puzzle's unique solution is marked as a mistake as soon as it is entered.
- A "solved" message when every cell is filled and matches the solution.
- A New Game action that starts a new easy puzzle.
- One save slot: the current game is saved automatically and restored on
  relaunch.
- Portrait-only, phone-only layout for Android API 26+.

**Out of scope**
- Difficulty levels other than easy (medium, hard, expert).
- Pencil marks / candidate notes.
- Undo / redo.
- Hints, auto-solve, or revealing cells.
- Timer, scores, statistics, streaks, leaderboards.
- Mistake limits or game-over on mistakes.
- Highlighting of related cells (same row/column/box or same digit), beyond
  showing which cell is selected.
- Multiple save slots or game history.
- A confirmation prompt before New Game discards the current game.
- Settings screen, themes, dark-mode toggle (following the system theme is
  allowed but not required).
- Sound, haptics, animations.
- Accounts, sign-in, cloud sync, backups to a server.
- Analytics, crash reporting to a third party, advertising, in-app purchases.
- Tablets, landscape orientation, Wear OS, iOS, web.
- Localisation beyond English.
- Play Store listing and publishing.

## Acceptance criteria
"Easy puzzle" is defined in Assumptions (A1). "Solution" always means the
puzzle's unique solution.

| ID | Given | When | Then | Test type |
|---|---|---|---|---|
| AC1 | The app is installed and has no saved game | The player launches the app | A 9x9 grid is shown containing a newly generated easy puzzle, with given cells showing digits and all other cells empty | integration |
| AC2 | The puzzle generator | A new puzzle is generated (checked over at least 100 generations) | Every generated puzzle has exactly one solution | unit |
| AC3 | The puzzle generator | A new puzzle is generated (checked over at least 100 generations) | Every generated puzzle has between 36 and 46 given cells inclusive, every given digit equals the solution's digit in that cell, and the solution is a valid Sudoku (each row, column and 3x3 box contains 1-9 exactly once) | unit |
| AC4 | A game is shown | The player looks at the grid | Given cells are visually distinct from player-entered cells (for example by text weight or colour) | manual: start a game, enter some digits, confirm givens and entries can be told apart at a glance |
| AC5 | A game in progress | The player taps an empty, non-given cell | That cell becomes the selected cell and is visibly marked as selected; any previously selected cell is no longer marked | integration |
| AC6 | An empty non-given cell is selected | The player taps a digit 1-9 | That digit appears in the selected cell | integration |
| AC7 | A non-given cell containing a player-entered digit is selected | The player taps a different digit 1-9 | The cell shows the new digit in place of the old one | unit |
| AC8 | A given cell is selected (or the player taps a given cell) | The player taps any digit or Erase | The given cell's digit is unchanged | unit |
| AC9 | A non-given cell is selected | The player enters a digit that differs from the solution's digit for that cell | The cell is shown as a mistake immediately, without any further action by the player | integration |
| AC10 | A non-given cell is selected (empty, or showing a mistake) | The player enters the solution's digit for that cell | The cell shows that digit and is not shown as a mistake | unit |
| AC11 | A non-given cell contains a player-entered digit (correct or mistaken) and is selected | The player taps Erase | The cell becomes empty and is not shown as a mistake | integration |
| AC12 | A game where every cell except one is filled and matches the solution | The player enters the solution's digit in the last empty cell | A "solved" message is shown, and the grid no longer accepts digit entry or Erase | integration |
| AC13 | A game where every cell is filled but at least one player-entered digit differs from the solution | The grid becomes full | No "solved" message is shown, the mistaken cells are shown as mistakes, and the player can still change cells | unit |
| AC14 | A game in progress (solved or not) | The player triggers New Game | A different newly generated easy puzzle replaces the current one, with no player-entered digits, no mistakes, no selection and no "solved" message | integration |
| AC15 | A game in progress with some player-entered digits, including at least one mistake | The app is closed (including the process being killed by the system) and relaunched | The same puzzle is shown with the same player-entered digits, and the same cells are shown as mistakes | integration |
| AC16 | A solved game | The app is closed and relaunched | The solved grid is shown with the "solved" message, and the grid does not accept digit entry or Erase | integration |
| AC17 | The player started a New Game after making entries in a previous game | The app is closed and relaunched | The newer game is restored and the previous game is not recoverable (single save slot) | integration |
| AC18 | The saved game data is missing, unreadable or invalid | The player launches the app | The app does not crash and shows a newly generated easy puzzle | integration |
| AC19 | The app is running on a phone with auto-rotate on | The phone is rotated to landscape | The app stays in portrait and the game state (digits, mistakes, selection) is unchanged | manual: on an API 26+ phone or emulator with auto-rotate enabled, rotate to landscape and back, confirm layout stays portrait and nothing changes |
| AC20 | A cell is shown as a mistake | The player looks at the grid with colour perception removed (Android "Simulate colour space: Monochromacy" developer option) | The mistake is still distinguishable from a correct entry by a non-colour cue (for example a mark, underline or strike) | manual: enable monochromacy simulation, enter a wrong digit, confirm it is identifiable |
| AC21 | TalkBack is on | The player moves focus to any cell | TalkBack announces the cell's row, column, its digit or "empty", whether it is a given, and whether it is a mistake; the digit buttons, Erase and New Game are announced by name | manual: enable TalkBack on an API 26+ device/emulator, swipe through a given cell, an empty cell, a correct entry and a mistake, and the controls, and confirm each announcement |
| AC22 | The device has no network connection (flight mode) | The player launches the app, plays, starts a New Game and relaunches | Every behaviour in AC1-AC18 works, and the app's merged manifest requests no network permission (`INTERNET` absent) | integration (manifest check) + manual: repeat a play session in flight mode |
| AC23 | The app is installed on an Android 8.0 (API 26) emulator | The player launches it and plays a game to "solved" | The app installs, runs and shows the "solved" message without crashing | manual: run on an API 26 phone emulator image and complete a puzzle (a debug build may use a test path to reach a near-solved state) |

## Non-functional requirements
- Performance: a new puzzle (on first launch or New Game) is shown within
  2 seconds on a mid-range reference device or the standard API 26+ phone
  emulator; a digit entry, Erase or mistake highlight appears within 100 ms of
  the tap; restoring a saved game on relaunch adds no more than 1 second to
  app start-up.
- Accessibility: the nine cells in each grid row share the available screen
  width equally, and each grid cell's touch target is at least 24x24 dp on a
  360 dp wide portrait screen (WCAG 2.2 AA target-size minimum; see A10);
  every other interactive control (digit buttons 1-9, Erase and New Game) has
  a touch target of at least 48x48 dp; text and cell-state indicators meet
  WCAG 2.1 AA contrast (4.5:1
  for digits, 3:1 for non-text indicators); mistakes are not indicated by
  colour alone (AC20); all cells and controls have TalkBack descriptions
  (AC21); digits remain readable with the system font size set to the largest
  setting without overlapping or truncation in the grid.
- Offline / error behaviour: the app works fully offline and never requires a
  network connection (AC22). If the saved game cannot be read, a new game
  starts without a crash (AC18). If a save write fails, the current session
  continues uninterrupted; the next successful save captures the latest state.
- Security: no runtime permissions requested; no network permission; no
  components exported except the launcher activity; follows
  `docs/governance/security-standards.md` (no secrets in the repo, release
  build minified and signed with keys kept out of the repo). Saved game data is
  treated as untrusted input on load and validated (digits 1-9 or empty,
  givens consistent with the stored puzzle) before use.

## Data and privacy
No personal data is created, stored, displayed or sent. The only stored data
is the current game state (puzzle, solution, player-entered digits, solved
flag), held in the app's private on-device storage. It is retained until it is
overwritten by a New Game, or deleted when the user clears the app's data or
uninstalls the app. Nothing leaves the device: no network access, no
analytics, no third-party services. Game state contains no personal data, so
Android auto-backup may include it; this is acceptable.

## Assumptions
- A1: "Easy" means the puzzle has exactly one solution and between 36 and 46
  given cells inclusive (AC2, AC3). No solving-technique grading is required
  for the MVP.
- A2: Puzzles are generated on the device at runtime; no puzzle bank or server
  is needed. A different puzzle on each New Game is expected but repeats are
  not checked beyond AC14's "different from the current one".
- A3: Mistake detection compares each player-entered digit only with the
  unique solution (owner answer 2), not with row/column/box conflicts.
- A4: Digits are entered through an on-screen 1-9 pad plus an Erase control;
  the system keyboard is not used.
- A5: The game is saved automatically after every change (entry, Erase, New
  Game, solved) so that it survives the process being killed at any time
  (owner answer 3).
- A6: New Game is available at any time, including after solving, and
  replaces the current game without a confirmation prompt.
- A7: After a puzzle is solved, the board is read-only until New Game.
- A8: English-only text.
- A9: The app name and icon are placeholders to be finalised later; they do
  not affect any AC.
- A10: Nine grid cells at 48 dp need 432 dp, more than the 360-411 dp width of
  target phones. Owner decision on 2026-10-19 ("Cells share width"): grid
  cells share the available width equally (about 40-45 dp each on 360-411 dp
  phones) with a minimum of 24x24 dp per the WCAG 2.2 AA target-size
  criterion; the 48x48 dp minimum still applies to every other interactive
  control.

## Dependencies and risks
- Dependency: the technology stack, project commands and coverage tooling are
  not yet chosen; the architect proposes them in
  `docs/adr/0001-technology-stack.md` and the owner approves at the stack
  checkpoint before any task beyond the walking skeleton starts.
- Dependency: an Android API 26 phone emulator image (and ideally a newer
  one) must be available for the manual and integration ACs, and CI must be
  able to run the integration tests or the design must say how they are run.
- Dependency: none on external services, accounts or APIs.
- Risk: puzzle generation with a uniqueness check can be slow on low-end
  devices, threatening the 2-second target; the design should account for it.
- Risk: integration tests for process death and relaunch (AC15-AC18) can be
  flaky on emulators in CI.
- Risk: the 23 ACs must fit within 8 tasks including the walking skeleton;
  if the architect cannot fit them, scope must be split rather than tasks
  enlarged.
- Risk: manual ACs (AC4, AC19-AC21, AC23) need a human or a device run to
  evidence; the tester must record how each was checked.

## Later (product vision, not part of this run)
- Medium, hard and expert difficulty levels with technique-based grading.
- Pencil marks / candidate notes and undo/redo.
- Hints, related-cell and same-digit highlighting, optional conflict checking.
- Timer, statistics, streaks and daily puzzles.
- Settings: themes, dark mode toggle, mistake highlighting on/off.
- Tablet and landscape layouts; localisation.
- Play Store release.
