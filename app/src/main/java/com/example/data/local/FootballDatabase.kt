package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.data.model.LiveEventEntity
import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatsEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FootballDao {

    @Query("SELECT * FROM matches ORDER BY status DESC, id ASC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    fun getMatchByIdFlow(matchId: String): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("UPDATE matches SET homeScore = :homeScore, awayScore = :awayScore, minute = :minute, status = :status WHERE id = :matchId")
    suspend fun updateMatchScore(matchId: String, homeScore: Int, awayScore: Int, minute: Int, status: String)

    @Query("SELECT * FROM teams")
    fun getAllTeamsFlow(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :teamId LIMIT 1")
    suspend fun getTeamById(teamId: String): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Query("SELECT * FROM players WHERE teamId = :teamId")
    suspend fun getPlayersForTeam(teamId: String): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("SELECT * FROM match_stats WHERE matchId = :matchId LIMIT 1")
    fun getStatsForMatchFlow(matchId: String): Flow<MatchStatsEntity?>

    @Query("SELECT * FROM match_stats WHERE matchId = :matchId LIMIT 1")
    suspend fun getStatsForMatch(matchId: String): MatchStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: MatchStatsEntity)

    @Query("SELECT * FROM predictions WHERE matchId = :matchId LIMIT 1")
    fun getPredictionForMatchFlow(matchId: String): Flow<PredictionEntity?>

    @Query("SELECT * FROM predictions WHERE matchId = :matchId LIMIT 1")
    suspend fun getPredictionForMatch(matchId: String): PredictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Query("SELECT * FROM live_events WHERE matchId = :matchId ORDER BY minute DESC, id DESC")
    fun getLiveEventsForMatchFlow(matchId: String): Flow<List<LiveEventEntity>>

    @Query("SELECT * FROM live_events WHERE matchId = :matchId ORDER BY minute DESC, id DESC")
    suspend fun getLiveEventsForMatch(matchId: String): List<LiveEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveEvent(event: LiveEventEntity)

    @Query("DELETE FROM live_events WHERE matchId = :matchId")
    suspend fun clearLiveEventsForMatch(matchId: String)
}

@Database(
    entities = [
        TeamEntity::class,
        MatchEntity::class,
        PlayerEntity::class,
        MatchStatsEntity::class,
        PredictionEntity::class,
        LiveEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FootballDatabase : RoomDatabase() {
    abstract fun footballDao(): FootballDao
}
