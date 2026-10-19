@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileGameRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun TestScope.repository(directory: File) = FileGameRepository(directory, StandardTestDispatcher(testScheduler))

    private fun saveFile(directory: File) = File(directory, "saved_game.json")

    @Test
    fun AC18_missing_file_loads_null_without_throwing() =
        runTest {
            val dir = temp.newFolder()
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_missing_directory_loads_null() =
        runTest {
            assertNull(repository(File(temp.root, "absent")).load())
        }

    @Test
    fun AC18_empty_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            saveFile(dir).writeText("")
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_garbage_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            saveFile(dir).writeBytes(byteArrayOf(0, 1, 2, -1, -2, 127))
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_oversize_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            saveFile(dir).writeText("x".repeat(SavedGameCodec.MAX_BYTES * 4))
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_semantically_invalid_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            val bad = SaveFixtures.dto.copy(solved = true)
            saveFile(dir).writeBytes(
                """{"version":1,"givens":"${bad.givens}","solution":"${bad.solution}","entries":"${bad.entries}","solved":true}"""
                    .toByteArray(),
            )
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_valid_save_just_over_the_size_cap_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            val valid = String(SavedGameCodec.encode(SaveFixtures.inProgress), Charsets.UTF_8)
            saveFile(dir).writeText(valid + " ".repeat(SavedGameCodec.MAX_BYTES - valid.length + 1))
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_unknown_key_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            val valid = String(SavedGameCodec.encode(SaveFixtures.inProgress), Charsets.UTF_8)
            saveFile(dir).writeText(valid.dropLast(1) + ",\"extra\":1}")
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
        }

    @Test
    fun AC18_unreadable_file_loads_null() =
        runTest {
            val dir = temp.newFolder()
            // A directory where the file should be cannot be read as a save.
            assertTrue(saveFile(dir).mkdir())
            assertNull(repository(dir).load())
        }

    @Test
    @Config(sdk = [26])
    fun AC18_valid_backup_without_a_base_file_is_recovered() =
        runTest {
            val dir = temp.newFolder()
            val valid = SavedGameCodec.encode(SaveFixtures.inProgress)
            File(dir, "saved_game.json.bak").writeBytes(valid)
            assertFalse(saveFile(dir).exists())
            assertEquals(SaveFixtures.inProgress, repository(dir).load())
        }

    @Test
    @Config(sdk = [26])
    fun AC18_oversize_backup_without_a_base_file_is_rejected_and_deleted() =
        runTest {
            val dir = temp.newFolder()
            File(dir, "saved_game.json.bak").writeText("x".repeat(SavedGameCodec.MAX_BYTES * 4))
            assertNull(repository(dir).load())
            assertFalse(saveFile(dir).exists())
            assertFalse(File(dir, "saved_game.json.bak").exists())
        }

    @Test
    fun AC18_missing_file_does_not_log() =
        runTest {
            ShadowLog.clear()
            assertNull(repository(temp.newFolder()).load())
            assertTrue(ShadowLog.getLogs().isEmpty())
        }

    @Test
    fun AC18_invalid_file_logs_the_fixed_message_only() =
        runTest {
            val dir = temp.newFolder()
            saveFile(dir).writeText("secret-looking-content")
            ShadowLog.clear()
            assertNull(repository(dir).load())
            val logs = ShadowLog.getLogs()
            assertEquals(1, logs.size)
            assertEquals("Saved game is unreadable or invalid and was discarded", logs[0].msg)
        }

    @Test
    fun AC15_entries_and_mistakes_survive_a_new_repository_instance() =
        runTest {
            val dir = temp.newFolder()
            repository(dir).save(SaveFixtures.inProgress)
            val restored = repository(dir).load()
            assertEquals(SaveFixtures.inProgress, restored)
            assertTrue(restored!!.isMistake(SaveFixtures.openCells[1]))
            assertFalse(restored.isMistake(SaveFixtures.openCells[0]))
        }

    @Test
    fun AC16_solved_flag_survives_a_new_repository_instance() =
        runTest {
            val dir = temp.newFolder()
            repository(dir).save(SaveFixtures.solved)
            assertTrue(repository(dir).load()!!.isSolved)
        }

    @Test
    fun a_second_save_replaces_the_first_single_slot() =
        runTest {
            val dir = temp.newFolder()
            val repo = repository(dir)
            repo.save(SaveFixtures.solved)
            repo.save(SaveFixtures.inProgress)
            val loaded = repo.load()
            if (System.getProperty("os.name").orEmpty().startsWith("Windows")) {
                // Robolectric's AtomicFile cannot rename over an existing file on a Windows host, so the
                // second write fails and the first save must survive intact. Linux CI asserts replacement.
                assertEquals(SaveFixtures.solved, loaded)
            } else {
                assertEquals(SaveFixtures.inProgress, loaded)
            }
        }

    @Test
    fun load_rereads_the_file_every_time() =
        runTest {
            val dir = temp.newFolder()
            val repo = repository(dir)
            repo.save(SaveFixtures.inProgress)
            assertEquals(SaveFixtures.inProgress, repo.load())
            saveFile(dir).delete()
            assertNull(repo.load())
        }

    @Test
    fun save_to_an_unwritable_directory_does_not_throw_and_next_save_succeeds() =
        runTest {
            val blocker = temp.newFile()
            val dir = File(blocker, "sub")
            val repo = repository(dir)
            repo.save(SaveFixtures.inProgress)
            assertNull(repo.load())

            assertTrue(blocker.delete())
            repo.save(SaveFixtures.inProgress)
            assertEquals(SaveFixtures.inProgress, repo.load())
        }

    @Test
    fun failed_write_keeps_the_previous_save() =
        runTest {
            val dir = temp.newFolder()
            val repo = repository(dir)
            repo.save(SaveFixtures.inProgress)

            // A directory in the place of the temporary write file makes the next write fail.
            assertTrue(File(dir, "saved_game.json.new").mkdir())
            repo.save(SaveFixtures.solved)

            assertEquals(SaveFixtures.inProgress, repo.load())
        }
}
