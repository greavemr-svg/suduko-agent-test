@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Generation effort benchmark. The node budget is the hard assertion (deterministic across
 * machines); wall-clock time is recorded for information and only checked against a loose ceiling.
 */
class GenerationPerformanceTest {
    @Test
    fun generation_stays_within_node_budget_and_writes_report() {
        val nodes = LongArray(SEED_COUNT)
        val millis = DoubleArray(SEED_COUNT)
        for (seed in 0 until SEED_COUNT) {
            val solver = SudokuSolver()
            val generator = PuzzleGenerator(Random(seed), solver)
            val start = System.nanoTime()
            generator.generate()
            millis[seed] = (System.nanoTime() - start) / NANOS_PER_MILLI
            nodes[seed] = solver.totalSearchNodes
        }
        val sortedMillis = millis.sortedArray()
        val maxNodes = nodes.max()
        val json =
            """
            {
              "seeds": $SEED_COUNT,
              "nodeBudget": $NODE_BUDGET,
              "maxNodes": $maxNodes,
              "meanNodes": ${nodes.average()},
              "meanMillis": ${millis.average()},
              "p95Millis": ${sortedMillis[(SEED_COUNT * 95 / 100) - 1]},
              "maxMillis": ${sortedMillis.last()}
            }
            """.trimIndent()
        val file = File("build/perf/generation.json")
        file.absoluteFile.parentFile?.mkdirs()
        file.writeText(json + "\n")

        assertTrue("max nodes $maxNodes exceeds budget $NODE_BUDGET", maxNodes <= NODE_BUDGET)
        assertTrue("slowest generation ${sortedMillis.last()} ms exceeds ceiling", sortedMillis.last() < MILLIS_CEILING)
    }

    private companion object {
        const val SEED_COUNT = 200
        const val NODE_BUDGET = 20_000L
        const val MILLIS_CEILING = 2_000.0
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}
