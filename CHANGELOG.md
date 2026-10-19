# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-10-19

First release: an offline Android Sudoku app (Kotlin, Jetpack Compose, API 26+, portrait phones). Epic [#5](https://github.com/greavemr-svg/suduko-agent-test/issues/5). Spec: `docs/specs/20261019-sudoku-mvp.md`. Release notes: `docs/releases/v0.1.0.md`.

### Added
- Walking skeleton: Gradle build, version catalog, CI, env-only release signing (build, #8, PR #15).
- Domain model: 9x9 grid, game state and rules, live mistake detection against the solution (feat, #9, PR #16).
- Backtracking solver and easy-puzzle generator with a unique solution and 36-46 givens (feat, #10, PR #17).
- Game screen: grid, cell selection, digit pad, Erase, live mistake marking with a non-colour cue, light and dark themes, large-font support (feat, #11, PR #18).
- Solved state, New Game, and TalkBack semantics for cells and controls (feat, #12, PR #19).
- Saved-game format (version 1, JSON), validator and file repository with atomic writes (feat, #13, PR #20).
- Autosave and restore on launch, including after process death, with fallback to a new puzzle when the save is missing or invalid (feat, #14, PR #21).

### Security
- No network permission; the saved game is the only stored data and contains no personal data.
- Open Low findings carried: S-01, S-02, S-03 (see release notes).

[Unreleased]: https://github.com/greavemr-svg/suduko-agent-test/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/greavemr-svg/suduko-agent-test/releases/tag/v0.1.0
