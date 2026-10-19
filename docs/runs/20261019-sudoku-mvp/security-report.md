# Security review: 20261019-sudoku-mvp

This file holds the security agent's Stage 4 report exactly as it was returned to the orchestrator. The security agent is read-only, so the orchestrator recorded it here. Nothing has been edited.

- Agent: security (read-only)
- Branch / head: `feature/20261019-sudoku-mvp` @ 212365a, scope `origin/main...HEAD`
- Recorded: 2026-10-19 (UTC), orchestrator

---

STATUS: DONE
SCOPE: origin/main...HEAD (212365a), feature/20261019-sudoku-mvp, 87 files, all 7 task PRs (#15-#21), epic #5
SUMMARY: Critical 0, High 0, Medium 0, Low 3, Info 3

FINDINGS:
- [Low] S-01 gradle/wrapper/gradle-wrapper.properties:10 and gradle/ (no verification-metadata.xml) | evidence: distributionSha256Sum is set (dce76f55...c01f) and validateDistributionUrl=true. I could not check the hash against the official Gradle 9.8.1 value offline. gradle-wrapper.jar is committed with no checksum check. There is no gradle/verification-metadata.xml, so Maven artefact hashes are not verified. | impact: Supply-chain tampering with a dependency or the wrapper jar would not be detected locally. | fix: Owner or CI should compare the wrapper jar hash with the official Gradle published checksum (or run wrapper-validation in CI). Consider generating `gradle --write-verification-metadata sha256` later. (owner / architect)
- [Low] S-02 app/src/main/res/xml/backup_rules.xml:3 and data_extraction_rules.xml:5-10 | evidence: `<include domain="file" path="."/>` includes the whole files dir, not specifically saved_game.json. Today the files dir holds only saved_game.json (AtomicFile also writes saved_game.json.bak transiently). The ADR/task brief says backup is limited to the save file. | impact: If another file is ever added under filesDir it would be silently backed up. Restored data is untrusted but is validated on load, so there is no exploitable issue now. | fix: Use `path="saved_game.json"` (and optionally the .bak) instead of ".". (developer)
- [Low] S-03 app/build.gradle.kts:62-66 | evidence: the `benchmark` build type is signed with the debug key, uses initWith(release), and isDebuggable=false. The ADR documents it. | impact: A benchmark APK signed with the well-known debug key could be mistaken for a releasable artefact. | fix: Make sure release automation only builds `assembleRelease`/`bundleRelease`, and that G-gates never publish the benchmark variant. (owner)
- [Info] S-04 merged release manifest | evidence: the manifest contains `<uses-permission ...DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION>`, the app's own signature-level permission, added by androidx.core. There is no INTERNET and no ACCESS_NETWORK_STATE. ManifestTest AC22 explicitly allows only this one. | impact: None. It is not a grant to any other app. The spec wording "no permissions" is satisfied in substance. | fix: Optional: note it in the spec or ADR. (architect)
- [Info] S-05 FileGameRepository.kt:28-45 | evidence: on any transient IOException (not only a parse failure) load() logs and deletes the save via file.delete(). | impact: A transient read error loses the user's game. This is an availability issue, not a security one, and it fails safe (no crash, no bad state loaded). | fix: Optionally delete only when decode or validate failed. (developer)
- [Info] S-06 repo | evidence: `.claude/governance.env` is tracked in the working tree and was not touched by this branch (no diff under .claude, .github, CLAUDE.md or docs/governance). I did not open it. It is owner-managed. | impact: None observed. | fix: Owner to confirm it holds no secrets. (owner)

Input handling (checked, no findings):
- The size cap is applied on the real AtomicFile.openRead path. readBounded reads at most MAX_BYTES+1 (4097) bytes into a fixed buffer. SavedGameCodec.decode rejects anything over 4096 bytes before parsing. On API 26-29, openRead restores .bak first, and that read is then bounded the same way.
- Json is strict: isLenient=false, ignoreUnknownKeys=false, coerceInputValues=false. All DTO fields are required. SerializationException and IllegalArgumentException are caught, so decode never throws.
- Deserialization is safe: a plain @Serializable data class, no polymorphism or reflection, and no Java serialization.
- SavedGameValidator checks every rule: version; each grid exactly 81 characters in 0-9; the solution is a valid solution; givens match the solution; given count in range; a unique solution with the bound countSolutions(...,2) call; no entry on a given cell; and the solved flag equal to the computed state. Any violation returns null, the file is discarded, and the app starts a new game.
- Save writes through AtomicFile startWrite/finishWrite/failWrite. The file is in app-private storage.

Logging: Only 3 Log.w calls, all fixed strings (FileGameRepository.kt:39 and :58, GameSaver.kt:29). No exception messages and no file or game content are logged.

R8/release: isMinifyEnabled=true and isShrinkResources=true. proguard-rules.pro contains only comments, so there are no broad keep rules. A release build exists at app/build/outputs/apk/release (unsigned); no debuggable attribute appears in the merged release manifest, so it defaults to false.

Signing: reads only SUDOKU_KEYSTORE_FILE, SUDOKU_KEYSTORE_PASSWORD, SUDOKU_KEY_ALIAS and SUDOKU_KEY_PASSWORD from env vars. A partial set fails the configuration with a `check` message that names only the variable names and never echoes values. With none set, the release APK is unsigned.

DEPENDENCIES: All are pinned to exact versions in gradle/libs.versions.toml, with no ranges or "+". ADR 0001's "Dependencies" section lists every one. No audit tool such as OWASP dependency-check or osv-scanner was run or available, so CVE status relies on the ADR's "None known" plus my own knowledge. I did not run `./gradlew dependencies` or any network query.

| name | version | licence | CVEs | ADR |
|---|---|---|---|---|
| AGP com.android.application | 9.4.1 | Apache-2.0 | none known | 0001 |
| Kotlin + compose and serialization plugins | 2.4.21 | Apache-2.0 | none known | 0001 |
| Compose BOM (ui, foundation, material3) | 2026.04.01 | Apache-2.0 | none known | 0001 |
| androidx core-ktx | 1.18.0 | Apache-2.0 | none known | 0001 |
| androidx activity-compose | 1.13.0 | Apache-2.0 | none known | 0001 |
| androidx lifecycle-viewmodel | 2.9.4 | Apache-2.0 | none known | 0001 |
| kotlinx-coroutines-android and -test | 1.11.0 | Apache-2.0 | none known | 0001 |
| kotlinx-serialization-json | 1.11.0 | Apache-2.0 | none known | 0001 |
| Kover plugin | 0.9.11 | Apache-2.0 | none known | 0001 |
| ktlint-gradle plugin | 14.2.0 | MIT | none known | 0001 |
| junit (test) | 4.13.2 | EPL-1.0 | none known (CVE-2020-15250 fixed in 4.13.1) | 0001 |
| robolectric (test) | 4.17 | MIT | none known | 0001, 0003 |
| androidx test core / ext-junit (test) | 1.7.0 / 1.3.0 | Apache-2.0 | none known | 0001 |
| compose ui-test-junit4 (test) | via BOM | Apache-2.0 | none known | 0001 |
| Gradle wrapper | 9.8.1 (sha256 present) | Apache-2.0 | n/a | 0001 |

No dependency lacks an ADR. No network libraries are present.

SECRETS_SCAN: Method: `git log -p origin/main..HEAD` grepped for password, secret, token, api key, private-key headers and AKIA; `git ls-files` checked for jks, keystore, pem, key, .env and local.properties; the tree was read for fixtures. Result: no secrets, keystores or real personal data. The only hits are env-var NAMES in build.gradle.kts and docs, plus the test string "secret-looking-content" (a log-leak test fixture). .gitignore excludes *.jks, *.keystore, .env and local.properties. The only tracked file matching the name pattern is .claude/governance.env (see S-06).

MANIFEST (merged release):
- Permissions: no INTERNET and no network permissions. The only one is the app's own signature-level DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (S-04).
- Exported components: only MainActivity (exported=true, MAIN/LAUNCHER). The androidx.startup InitializationProvider is exported=false. ProfileInstallReceiver is removed by tools:node="remove" and is absent from the merged manifest. The ProfileInstallerInitializer metadata remains under the non-exported provider, which is acceptable.
- Cleartext: usesCleartextTraffic=false.
- Backup: allowBackup=true, with dataExtractionRules (cloud-backup and device-transfer) and fullBackupContent restricted to domain=file (S-02). The save holds no personal data and is validated on restore.
- Debuggable: not set, so false in release. The benchmark type is explicitly isDebuggable=false.
- No WebView and no other surface.
- Health data: none stored or processed.

VERDICT: PASS (zero open Critical/High)
