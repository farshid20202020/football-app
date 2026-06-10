package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String,
    val elo: Double,
    val attackRating: Double,  // Normalized 1.0 - 99.0
    val defenseRating: Double, // Normalized 1.0 - 99.0
    val formString: String,    // Comma-separated (W,D,L,W,W...)
    val squadStrengthScore: Double, // 1 to 10
    val tacticalStyle: String,  // Possessive, High Press, Counter Attack, Low Block
    val formation: String      // 4-3-3, 3-5-2, 4-2-3-1 etc.
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeTeamLogo: String,
    val awayTeamLogo: String,
    val status: String,       // UPCOMING, LIVE, FINISHED
    val homeScore: Int,
    val awayScore: Int,
    val minute: Int,
    val leagueName: String,
    val matchTime: String,    // HH:MM
    val stadium: String,
    val referee: String,
    val weather: String,       // Sunny, Rain, Wind, Snow
    val restDaysHome: Int,
    val restDaysAway: Int,
    val stadiumIntensity: Double, // 1.0 - 5.0
    val refereeStrictness: Double // 1.0 - 5.0
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val name: String,
    val position: String, // GK, DEF, MID, FWD
    val impactRating: Double, // 1.0 - 10.0
    val fitness: Double,      // 0% - 100%
    val isInjured: Boolean
)

@Entity(tableName = "match_stats")
data class MatchStatsEntity(
    @PrimaryKey val matchId: String,
    val homeXG: Double,
    val awayXG: Double,
    val homeShots: Int,
    val awayShots: Int,
    val homePossession: Int,  // % (e.g. 55)
    val awayPossession: Int,
    val homePPDA: Double,     // Passes allowed Per Defensive Action (lower = more press)
    val awayPPDA: Double,
    val homeDangerousAttacks: Int,
    val awayDangerousAttacks: Int,
    val homeCards: Int,
    val awayCards: Int
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey val matchId: String,
    val winProb: Double,      // 0.0 - 1.0
    val drawProb: Double,     // 0.0 - 1.0
    val lossProb: Double,     // 0.0 - 1.0
    val over05: Double,
    val over15: Double,
    val over25: Double,
    val over35: Double,
    val bttsProb: Double,
    val topScoresJson: String,  // Top 5 scorelines sorted by likelihood (e.g. "[{\"score\":\"1-1\",\"prob\":0.12}]")
    val confidence: Double,     // 0.0 - 1.0
    val momentumIndex: Double,  // -100.0 to +100.0 (Home vs Away momentum shift)
    val uncertaintyIndicator: String, // Low, Medium, High based on statistical variance
    val preLiveDriftJson: String     // Drift history data for visualization: "[{\"min\":0,\"h\":45,\"d\":25,\"a\":30}]"
)

@Entity(tableName = "live_events")
data class LiveEventEntity(
    @PrimaryKey val id: String,
    val matchId: String,
    val minute: Int,
    val type: String,       // GOAL, SHOT, DANGEROUS_ATTACK, CARD, SUB
    val team: String,       // HOME, AWAY, NONE
    val detail: String,     // Goal scorer, card receiver, etc.
    val xGValue: Double     // Event xG change
)
