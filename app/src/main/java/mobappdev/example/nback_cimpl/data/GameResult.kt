package mobappdev.example.nback_cimpl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val correctResponses: Int,
    val timestamp: Long = System.currentTimeMillis()
)
