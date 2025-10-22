package dev.oneuiproject.oneuiexample.data.stargazers.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import kotlinx.coroutines.flow.Flow

@Dao
interface StargazersDao {
    @Query("SELECT * FROM stargazers")
    fun getAllStargazers(): Flow<List<Stargazer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stargazers: List<Stargazer>)

    @Query("DELETE FROM stargazers")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(stargazers: List<Stargazer>){
        clear()
        insertAll(stargazers)
    }

    @Query("SELECT * FROM stargazers WHERE id IN (:ids)")
    suspend fun getStargazersById(ids: IntArray): List<Stargazer>

}