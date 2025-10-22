package dev.oneuiproject.oneuiexample.data.stargazers.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.data.stargazers.StargazersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(entities = [Stargazer::class], version = 1)
abstract class StargazersDB : RoomDatabase() {
    abstract fun stargazerDao(): StargazersDao

    companion object {
        @Volatile
        private var INSTANCE: StargazersDB? = null

        fun getDatabase(context: Context,
                        scope: CoroutineScope,
                        repoProvider: () -> StargazersRepo
        ): StargazersDB =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StargazersDB::class.java,
                    "stargazer_database"
                )
                    .addCallback(
                        StargazersDatabaseCallback(scope, repoProvider)
                    )
                    .build().also { INSTANCE = it }
            }


        private class StargazersDatabaseCallback(
            private val scope: CoroutineScope,
            private val repoProvider: () -> StargazersRepo
        ) : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                fetchInitialDataIfNotYet()
            }

            private fun fetchInitialDataIfNotYet() {
                val repository = repoProvider()
                repository.apply {
                    scope.launch {
                        when (fetchStatusFlow.first()) {
                            FetchState.INITED,
                            FetchState.REFRESHED -> Unit // Do nothing, data is fresh
                            else -> refreshStargazers() // Fetch data
                        }
                    }
                }
            }
        }
    }
}