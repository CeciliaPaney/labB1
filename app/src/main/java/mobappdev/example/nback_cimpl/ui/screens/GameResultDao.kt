package mobappdev.example.nback_cimpl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Insert
    suspend fun insertGameResult(gameResult: GameResult)

    @Query("SELECT * FROM game_results ORDER BY score DESC")
    fun getAllGameResults(): Flow<List<GameResult>>
}
