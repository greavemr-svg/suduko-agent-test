# ADR 0002: Saved game format and persistence

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-sudoku-mvp`

## Context
The spec needs one save slot that survives the app being closed or its process
being killed at any time (A5, AC15-AC17). The game is saved after every change.
On load, the saved data is untrusted input and must be validated; if it is
missing, unreadable or invalid the app starts a new game without crashing
(AC18). If a write fails, the session continues and the next successful save
captures the latest state (NFR). Restoring may add at most 1 s to start-up.
The stored data is exactly: puzzle (givens), solution, player-entered digits,
solved flag. No personal data. Android auto-backup may include it.

The format is hard to reverse once installs exist: a later release must read
it or migrate it, so it is recorded here.

## Options considered
1. **One JSON file in `Context.filesDir`, written atomically with the platform
   `android.util.AtomicFile`, encoded with kotlinx.serialization** (chosen).
   - Pros: atomic replace (a kill mid-write leaves the previous complete save);
     no extra storage library (AtomicFile is in the platform since API 17);
     human-readable for debugging; strict decoding into a DTO, then a separate
     pure-Kotlin validator; trivially versioned.
   - Cons: we write our own small write queue (a conflated channel) instead of
     getting one from a library.
2. **Jetpack DataStore (Proto or typed with a custom serializer).**
   - Pros: atomic writes, built-in corruption handler, coroutine API.
   - Cons: an extra dependency (and Proto adds protobuf tooling) for one tiny
     record; its single-instance-per-file rule complicates "fresh process"
     relaunch tests.
3. **SharedPreferences.**
   - Pros: no dependency.
   - Cons: `apply()` is asynchronous with weak guarantees on kill; multiple keys
     would not be written atomically together, so a kill could leave entries
     from one game with the puzzle of another.
4. **Room / SQLite.**
   - Pros: robust, migrations built in.
   - Cons: heavy for a single row; adds annotation processing (KSP) and a schema.
5. **Do nothing (no save)** — violates AC15-AC17.

## Decision
Option 1.

File: `<filesDir>/saved_game.json`. Schema version 1:

```json
{
  "version": 1,
  "givens":   "<81 chars, '1'-'9' at given cells, '0' elsewhere>",
  "solution": "<81 chars, '1'-'9'>",
  "entries":  "<81 chars, '1'-'9' for player-entered digits, '0' for empty or given cells>",
  "solved": false
}
```

Cells are in row-major order (index = row * 9 + column, both 0-based).
Mistakes and selection are not stored: mistakes are derived from entries and
solution on load (so AC15's "same cells shown as mistakes" holds by
construction), and selection is in-memory only.

Load rules (all must pass, else the save is treated as absent, deleted, and a
new game is generated; nothing is thrown to the UI):
- File size at most 4 KiB before decoding.
- Strict JSON decoding into the DTO (unknown keys and missing keys rejected).
- `version == 1`.
- Each grid string is exactly 81 characters from the allowed set.
- `solution` is a valid Sudoku (each row, column and box holds 1-9 once).
- Every non-zero `givens` cell equals `solution` at that cell, the givens count
  is 36-46 (spec A1), and the givens have exactly one solution (checked with
  the app's solver, a few milliseconds).
- `entries` is `'0'` at every given cell.
- `solved` equals "every non-given cell's entry equals the solution".

Write rules:
- Every state change hands the latest full snapshot to a `GameSaver`, which
  holds a conflated channel consumed by one coroutine in the application scope
  (not `viewModelScope`, so leaving the screen does not cancel the last save).
  Writes run on an injected IO dispatcher. Only the newest snapshot is written.
- A write uses `AtomicFile.startWrite()` / `finishWrite()`; on `IOException` it
  calls `failWrite()`, logs a message with no game content, and keeps running.

## Consequences
- Easier: validation is pure Kotlin and fully unit-testable; relaunch tests
  only need a new repository instance pointed at the same directory.
- Harder: any future change to the format must bump `version` and add a
  reader for version 1 (or accept that old saves are discarded as invalid,
  which the load rules already handle safely).
- Reversal: delete the file and the code. Since the only consequence of an
  unreadable save is a new game, a format change can never crash existing
  installs.

## For new dependencies
- kotlinx.serialization (runtime `kotlinx-serialization-json` and the Kotlin
  serialization compiler plugin). Covered in ADR 0001's dependency table:
  Apache-2.0, maintained by JetBrains, no known vulnerabilities, small and
  reflection-free.
- `android.util.AtomicFile`: platform API, no dependency.
