@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.SavedGameValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SavedGameCodecTest {
    private fun decode(text: String) = SavedGameCodec.decode(text.toByteArray(Charsets.UTF_8))

    private val grid = "0".repeat(CELL_COUNT)

    private fun json(
        version: String = "1",
        extra: String = "",
        solved: String = "false",
    ) = """{"version":$version,"givens":"$grid","solution":"$grid","entries":"$grid","solved":$solved$extra}"""

    @Test
    fun encode_writes_the_adr_schema() {
        val text = String(SavedGameCodec.encode(SaveFixtures.inProgress), Charsets.UTF_8)
        assertEquals(
            """{"version":1,"givens":"${SaveFixtures.dto.givens}","solution":"${SaveFixtures.dto.solution}",""" +
                """"entries":"${SaveFixtures.dto.entries}","solved":false}""",
            text,
        )
    }

    @Test
    fun encodeGrid_is_81_row_major_digits() {
        val text = SavedGameCodec.encodeGrid(SaveFixtures.puzzle.solution)
        assertEquals(CELL_COUNT, text.length)
        for (i in 0 until CELL_COUNT) assertEquals(SaveFixtures.puzzle.solution.get(i), text[i] - '0')
    }

    @Test
    fun AC15_entries_and_mistakes_round_trip_through_the_codec() {
        val dto = SavedGameCodec.decode(SavedGameCodec.encode(SaveFixtures.inProgress))
        val restored = SavedGameValidator.validate(assertNotNull_(dto))
        assertEquals(SaveFixtures.inProgress, restored)
        val wrong = SaveFixtures.openCells[1]
        assertEquals(true, restored?.isMistake(wrong))
    }

    @Test
    fun AC16_solved_flag_round_trips_through_the_codec() {
        val dto = assertNotNull_(SavedGameCodec.decode(SavedGameCodec.encode(SaveFixtures.solved)))
        assertEquals(true, dto.solved)
        assertEquals(true, SavedGameValidator.validate(dto)?.isSolved)
    }

    @Test
    fun AC18_decode_accepts_well_formed_json_and_rejects_everything_else() {
        assertNotNull(decode(json()))
        assertNull(decode(""))
        assertNull(decode("not json"))
        assertNull(decode("{"))
        assertNull(decode("[]"))
        assertNull(decode("null"))
        assertNull(SavedGameCodec.decode(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00)))
    }

    @Test
    fun AC18_decode_rejects_unknown_key() {
        assertNull(decode(json(extra = ""","extra":1""")))
    }

    @Test
    fun AC18_decode_rejects_each_missing_key() {
        for (key in listOf("version", "givens", "solution", "entries", "solved")) {
            val without = json().replace(Regex(""""$key":("[^"]*"|[a-z0-9]+),?"""), "").replace(",}", "}")
            assertNull(key, decode(without))
        }
    }

    @Test
    fun AC18_decode_rejects_wrong_types_and_nulls() {
        assertNull(decode(json(version = "null")))
    }

    @Test
    fun AC18_quoted_primitives_that_carry_a_wrong_value_never_reach_a_valid_state() {
        // ADR 0002 requires strict keys, not strict primitive types. The default Json coerces quoted
        // primitives, so a wrong value stays wrong and the validator is the line of defence.
        val valid = SaveFixtures.dto

        fun viaValidator(
            version: String,
            solved: String,
        ) = decode(
            """{"version":$version,"givens":"${valid.givens}","solution":"${valid.solution}",""" +
                """"entries":"${valid.entries}","solved":$solved}""",
        )?.let { SavedGameValidator.validate(it) }

        assertNull(viaValidator("\"2\"", "\"false\""))
        assertNull(viaValidator("\"0\"", "false"))
        assertNull(viaValidator("1", "\"true\""))
        assertNull(viaValidator("\"abc\"", "false"))
        assertNull(viaValidator("1.5", "false"))
        assertNull(viaValidator("1", "1"))
        assertNull(viaValidator("1", "\"yes\""))
        assertNull(viaValidator("[1]", "false"))
        assertNull(viaValidator("1", "{}"))
    }

    @Test
    fun AC18_non_string_grid_values_are_rejected() {
        val g = SaveFixtures.dto.givens
        assertNull(decode("""{"version":1,"givens":123,"solution":"$g","entries":"$g","solved":false}"""))
        assertNull(decode("""{"version":1,"givens":null,"solution":"$g","entries":"$g","solved":false}"""))
        assertNull(decode("""{"version":1,"givens":["0"],"solution":"$g","entries":"$g","solved":false}"""))
    }

    @Test
    fun AC18_size_cap_is_exactly_4_KiB() {
        assertEquals(4096, SavedGameCodec.MAX_BYTES)
        val base = json()
        assertNotNull(decode(base + " ".repeat(SavedGameCodec.MAX_BYTES - base.length)))
        assertNull(decode(base + " ".repeat(SavedGameCodec.MAX_BYTES - base.length + 1)))
    }

    @Test
    fun AC18_decode_rejects_oversize_input() {
        val padded = json() + " ".repeat(SavedGameCodec.MAX_BYTES)
        assertNull(decode(padded))
        assertEquals(true, SavedGameCodec.MAX_BYTES >= SavedGameCodec.encode(SaveFixtures.inProgress).size)
    }

    private fun assertNotNull_(dto: SavedGameDto?): SavedGameDto {
        assertNotNull(dto)
        return dto!!
    }
}
