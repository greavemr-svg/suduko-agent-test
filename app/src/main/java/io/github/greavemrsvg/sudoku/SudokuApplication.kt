package io.github.greavemrsvg.sudoku

import android.app.Application

/** Application class. Creates the [AppContainer] that holds the app-wide objects. */
open class SudokuApplication : Application() {
    lateinit var container: AppContainer
        protected set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
    }

    /** Overridable so tests can supply a container with seeded randomness. */
    protected open fun createContainer(): AppContainer = AppContainer(filesDir)
}
