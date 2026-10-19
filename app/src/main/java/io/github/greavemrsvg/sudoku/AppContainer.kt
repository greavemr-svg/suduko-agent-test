package io.github.greavemrsvg.sudoku

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container created by [SudokuApplication]. It owns the
 * application-scoped coroutine scope and holds no game state.
 */
class AppContainer {
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
