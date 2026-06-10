package com.example.data.repository

import com.example.data.engine.PredictionEngine
import com.example.data.local.FootballDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class FootballRepository(private val dao: FootballDao) {

    val matchesFlow: Flow<List<MatchEntity>> = dao.getAllMatchesFlow()
    val teamsFlow: Flow<List<TeamEntity>> = dao.getAllTeamsFlow()

    fun getMatchFlow(matchId: String): Flow<MatchEntity?> = dao.getMatchByIdFlow(matchId)
    fun getStatsFlow(matchId: String): Flow<MatchStatsEntity?> = dao.getStatsForMatchFlow(matchId)
    fun getPredictionFlow(matchId: String): Flow<PredictionEntity?> = dao.getPredictionForMatchFlow(matchId)
    fun getLiveEventsFlow(matchId: String): Flow<List<LiveEventEntity>> = dao.getLiveEventsForMatchFlow(matchId)

    suspend fun getPlayers(teamId: String): List<PlayerEntity> = dao.getPlayersForTeam(teamId)

    /**
     * Resolves conflict stats from multiple ingestion sources using a weighted trust system.
     */
    fun resolveMultiSourceStats(
        matchId: String,
        sourceData: List<SourceStatSnapshot>
    ): MatchStatsEntity {
        var weightedPossessionHome = 0.0
        var weightedHomeXG = 0.0
        var weightedAwayXG = 0.0
        var weightedHomeShots = 0.0
        var weightedAwayShots = 0.0
        var weightedHomeDangerous = 0.0
        var weightedAwayDangerous = 0.0
        var weightedHomePPDA = 0.0
        var weightedAwayPPDA = 0.0

        var totalWeight = 0.0

        for (snapshot in sourceData) {
            val weight = getSourceWeight(snapshot.sourceName)
            weightedPossessionHome += snapshot.homePossession * weight
            weightedHomeXG += snapshot.homeXG * weight
            weightedAwayXG += snapshot.awayXG * weight
            weightedHomeShots += snapshot.homeShots * weight
            weightedAwayShots += snapshot.awayShots * weight
            weightedHomeDangerous += snapshot.homeDangerousAttacks * weight
            weightedAwayDangerous += snapshot.awayDangerousAttacks * weight
            weightedHomePPDA += snapshot.homePPDA * weight
            weightedAwayPPDA += snapshot.awayPPDA * weight
            totalWeight += weight
        }

        val resolvedHomePoss = (weightedPossessionHome / totalWeight).toInt()
        val resolvedAwayPoss = 100 - resolvedHomePoss

        return MatchStatsEntity(
            matchId = matchId,
            homeXG = weightedHomeXG / totalWeight,
            awayXG = weightedAwayXG / totalWeight,
            homeShots = (weightedHomeShots / totalWeight).toInt(),
            awayShots = (weightedAwayShots / totalWeight).toInt(),
            homePossession = resolvedHomePoss,
            awayPossession = resolvedAwayPoss,
            homePPDA = weightedHomePPDA / totalWeight,
            awayPPDA = weightedAwayPPDA / totalWeight,
            homeDangerousAttacks = (weightedHomeDangerous / totalWeight).toInt(),
            awayDangerousAttacks = (weightedAwayDangerous / totalWeight).toInt(),
            homeCards = sourceData.firstOrNull()?.homeCards ?: 0,
            awayCards = sourceData.firstOrNull()?.awayCards ?: 0
        )
    }

    private fun getSourceWeight(source: String): Double {
        return when (source) {
            "API-Football" -> 0.95
            "SportMonks" -> 0.90
            "Football-Data.org" -> 0.85
            "SofaScore Parse" -> 0.80
            "FlashScore Parse" -> 0.75
            "ESPN Scraping" -> 0.70
            else -> 0.50
        }
    }

    /**
     * Seeds the local Room database with premier pre-built high-level football matchups
     * (Tehran Derby, UCL Champions clash, Premier League epic) so it represents an elite database.
     */
    suspend fun seedDatabaseIfEmpty() {
        // We look up if we have teams seeded
        val existingTeams = dao.getAllTeamsFlow().firstOrNull()
        if (!existingTeams.isNullOrEmpty()) {
            return
        }

        // --- Teams Seeding ---
        val teams = listOf(
            TeamEntity(
                id = "pers",
                name = "پرسپولیس",
                logoUrl = "https://example.com/persepolis.png",
                elo = 1680.0,
                attackRating = 82.0,
                defenseRating = 88.0,
                formString = "W,W,D,W,W",
                squadStrengthScore = 8.5,
                tacticalStyle = "Possessive",
                formation = "4-2-3-1"
            ),
            TeamEntity(
                id = "este",
                name = "استقلال",
                logoUrl = "https://example.com/esteghlal.png",
                elo = 1640.0,
                attackRating = 78.0,
                defenseRating = 91.0,
                formString = "W,D,W,L,W",
                squadStrengthScore = 8.2,
                tacticalStyle = "Low Block",
                formation = "3-5-2"
            ),
            TeamEntity(
                id = "real",
                name = "رئال مادرید",
                logoUrl = "https://example.com/realmadrid.png",
                elo = 1980.0,
                attackRating = 96.0,
                defenseRating = 92.0,
                formString = "W,W,W,D,W",
                squadStrengthScore = 9.8,
                tacticalStyle = "Counter Attack",
                formation = "4-3-1-2"
            ),
            TeamEntity(
                id = "manc",
                name = "منچستر سیتی",
                logoUrl = "https://example.com/mancity.png",
                elo = 1995.0,
                attackRating = 98.0,
                defenseRating = 90.0,
                formString = "W,W,D,W,L",
                squadStrengthScore = 9.9,
                tacticalStyle = "Possessive",
                formation = "4-1-4-1"
            ),
            TeamEntity(
                id = "ars",
                name = "آرسنال",
                logoUrl = "https://example.com/arsenal.png",
                elo = 1910.0,
                attackRating = 92.0,
                defenseRating = 94.0,
                formString = "W,L,W,W,D",
                squadStrengthScore = 9.3,
                tacticalStyle = "High Press",
                formation = "4-3-3"
            ),
            TeamEntity(
                id = "che",
                name = "چلسی",
                logoUrl = "https://example.com/chelsea.png",
                elo = 1810.0,
                attackRating = 86.0,
                defenseRating = 82.0,
                formString = "D,W,W,L,W",
                squadStrengthScore = 8.7,
                tacticalStyle = "Possessive",
                formation = "4-2-3-1"
            )
        )
        dao.insertTeams(teams)

        // --- Players Seeding ---
        val players = mutableListOf<PlayerEntity>()
        // Persepolis major players
        players.add(PlayerEntity("p1", "pers", "علیرضا بیرانوند", "GK", 8.8, 95.0, false))
        players.add(PlayerEntity("p2", "pers", "حسین کنعانی", "DEF", 8.4, 100.0, false))
        players.add(PlayerEntity("p3", "pers", "مهدی ترابی", "MID", 8.6, 92.0, false))
        players.add(PlayerEntity("p4", "pers", "امید عالیشاه", "FWD", 8.2, 85.0, false))
        players.add(PlayerEntity("p4_inj", "pers", "سعید صادقی", "MID", 7.9, 15.0, true)) // Injured

        // Esteghlal major players
        players.add(PlayerEntity("p5", "este", "سیدحسین حسینی", "GK", 8.5, 98.0, false))
        players.add(PlayerEntity("p6", "este", "روزبه چشمی", "DEF", 8.3, 100.0, false))
        players.add(PlayerEntity("p7", "este", "آرش رضاوند", "MID", 7.8, 90.0, false))
        players.add(PlayerEntity("p8", "este", "مهرداد محمدی", "FWD", 8.1, 75.0, false))

        // Real Madrid players
        players.add(PlayerEntity("p9", "real", "Thibaut Courtois", "GK", 9.5, 80.0, false))
        players.add(PlayerEntity("p10", "real", "Antonio Rüdiger", "DEF", 9.1, 100.0, false))
        players.add(PlayerEntity("p11", "real", "Jude Bellingham", "MID", 9.7, 95.0, false))
        players.add(PlayerEntity("p12", "real", "Vinícius Júnior", "FWD", 9.8, 98.0, false))

        // Manchester City players
        players.add(PlayerEntity("p13", "manc", "Ederson", "GK", 9.2, 100.0, false))
        players.add(PlayerEntity("p14", "manc", "Rúben Dias", "DEF", 9.3, 100.0, false))
        players.add(PlayerEntity("p15", "manc", "Kevin De Bruyne", "MID", 9.8, 90.0, false))
        players.add(PlayerEntity("p16", "manc", "Erling Haaland", "FWD", 9.9, 95.0, false))

        dao.insertPlayers(players)

        // --- Matches Seeding ---
        val matches = listOf(
            MatchEntity(
                id = "m_derby",
                homeTeamId = "pers",
                awayTeamId = "este",
                homeTeamName = "پرسپولیس",
                awayTeamName = "استقلال",
                homeTeamLogo = "https://example.com/pers.png",
                awayTeamLogo = "https://example.com/est.png",
                status = "LIVE",
                homeScore = 0,
                awayScore = 0,
                minute = 45,
                leagueName = "لیگ برتر خلیج فارس",
                matchTime = "19:00",
                stadium = "ورزشگاه آزادی، تهران",
                referee = "علیرضا فغانی",
                weather = "Sunny",
                restDaysHome = 6,
                restDaysAway = 5,
                stadiumIntensity = 4.8,
                refereeStrictness = 3.9
            ),
            MatchEntity(
                id = "m_ucl",
                homeTeamId = "real",
                awayTeamId = "manc",
                homeTeamName = "رئال مادرید",
                awayTeamName = "منچستر سیتی",
                homeTeamLogo = "https://example.com/real.png",
                awayTeamLogo = "https://example.com/city.png",
                status = "UPCOMING",
                homeScore = 0,
                awayScore = 0,
                minute = 0,
                leagueName = "لیگ قهرمانان اروپا",
                matchTime = "22:30",
                stadium = "Santiago Bernabéu, Madrid",
                referee = "Szymon Marciniak",
                weather = "Sunny",
                restDaysHome = 7,
                restDaysAway = 6,
                stadiumIntensity = 4.9,
                refereeStrictness = 2.8
            ),
            MatchEntity(
                id = "m_pl",
                homeTeamId = "ars",
                awayTeamId = "che",
                homeTeamName = "آرسنال",
                awayTeamName = "چلسی",
                homeTeamLogo = "https://example.com/ars.png",
                awayTeamLogo = "https://example.com/che.png",
                status = "FINISHED",
                homeScore = 3,
                awayScore = 1,
                minute = 90,
                leagueName = "لیگ برتر انگلیس",
                matchTime = "20:45",
                stadium = "Emirates Stadium, London",
                referee = "Michael Oliver",
                weather = "Rain",
                restDaysHome = 4,
                restDaysAway = 4,
                stadiumIntensity = 4.2,
                refereeStrictness = 3.2
            )
        )
        dao.insertMatches(matches)

        // Seed Pre-Live Predictions and initial stats using our simulation engine!
        for (m in matches) {
            val hTeam = teams.first { it.id == m.homeTeamId }
            val aTeam = teams.first { it.id == m.awayTeamId }
            val hPlayers = players.filter { it.teamId == m.homeTeamId }
            val aPlayers = players.filter { it.teamId == m.awayTeamId }

            // 1. Initial Stats
            val stats = if (m.status == "FINISHED") {
                MatchStatsEntity(
                    matchId = m.id,
                    homeXG = 2.45,
                    awayXG = 0.98,
                    homeShots = 17,
                    awayShots = 9,
                    homePossession = 58,
                    awayPossession = 42,
                    homePPDA = 8.2,
                    awayPPDA = 12.4,
                    homeDangerousAttacks = 64,
                    awayDangerousAttacks = 38,
                    homeCards = 1,
                    awayCards = 2
                )
            } else if (m.status == "LIVE") {
                MatchStatsEntity(
                    matchId = m.id,
                    homeXG = 0.58,
                    awayXG = 0.42,
                    homeShots = 5,
                    awayShots = 4,
                    homePossession = 51,
                    awayPossession = 49,
                    homePPDA = 9.8,
                    awayPPDA = 10.4,
                    homeDangerousAttacks = 22,
                    awayDangerousAttacks = 19,
                    homeCards = 0,
                    awayCards = 1
                )
            } else {
                MatchStatsEntity(
                    matchId = m.id,
                    homeXG = 0.0,
                    awayXG = 0.0,
                    homeShots = 0,
                    awayShots = 0,
                    homePossession = 50,
                    awayPossession = 50,
                    homePPDA = 0.0,
                    awayPPDA = 0.0,
                    homeDangerousAttacks = 0,
                    awayDangerousAttacks = 0,
                    homeCards = 0,
                    awayCards = 0
                )
            }
            dao.insertStats(stats)

            // 2. Pre-match Predictions
            val prePrediction = PredictionEngine.calculatePreMatchPrediction(
                m, hTeam, aTeam, hPlayers, aPlayers
            )

            // 3. If LIVE or FINISHED, run the live update mechanism once to create live stats
            val finalPrediction = if (m.status == "LIVE") {
                PredictionEngine.recalculateLivePrediction(
                    prePrediction, m.homeScore, m.awayScore, m.minute, stats
                )
            } else if (m.status == "FINISHED") {
                PredictionEngine.recalculateLivePrediction(
                    prePrediction, m.homeScore, m.awayScore, 90, stats
                )
            } else {
                prePrediction
            }

            dao.insertPrediction(finalPrediction)

            // 4. Seed Live Events
            if (m.status == "FINISHED") {
                val finishedEvents = listOf(
                    LiveEventEntity("fe1", m.id, 14, "GOAL", "HOME", "گل توسط بوکایو ساکا (آرسنال)", 0.45),
                    LiveEventEntity("fe2", m.id, 32, "GOAL", "HOME", "گل توسط مارتین اودگارد (آرسنال)", 0.32),
                    LiveEventEntity("fe3", m.id, 44, "CARD", "AWAY", "کارت زرد برای انزو فرناندز (چلسی)", 0.0),
                    LiveEventEntity("fe4", m.id, 55, "GOAL", "AWAY", "گل برای چلسی توسط نیکلاس جکسون", 0.52),
                    LiveEventEntity("fe5", m.id, 81, "GOAL", "HOME", "گل توسط گابریل ژسوس (آرسنال)", 0.65)
                )
                for (ev in finishedEvents) {
                    dao.insertLiveEvent(ev)
                }
            } else if (m.status == "LIVE") {
                val liveEvents = listOf(
                    LiveEventEntity("le1", m.id, 18, "SHOT", "HOME", "شوت خارج از چهارچوب توسط مهدی ترابی", 0.12),
                    LiveEventEntity("le2", m.id, 29, "CARD", "AWAY", "کارت زرد برای روزبه چشمی", 0.0),
                    LiveEventEntity("le3", m.id, 38, "SHOT", "AWAY", "شوت در چهارچوب مهار شده توسط بیرانوند", 0.28)
                )
                for (ev in liveEvents) {
                    dao.insertLiveEvent(ev)
                }
            }
        }
    }

    suspend fun insertLiveEvent(event: LiveEventEntity) {
        dao.insertLiveEvent(event)
    }

    suspend fun updateMatch(match: MatchEntity) {
        dao.insertMatches(listOf(match))
    }

    suspend fun updateStats(stats: MatchStatsEntity) {
        dao.insertStats(stats)
    }

    suspend fun updatePrediction(prediction: PredictionEntity) {
        dao.insertPrediction(prediction)
    }

    suspend fun insertMatch(match: MatchEntity) = dao.insertMatches(listOf(match))
}

data class SourceStatSnapshot(
    val sourceName: String,
    val homePossession: Int,
    val homeXG: Double,
    val awayXG: Double,
    val homeShots: Double,
    val awayShots: Double,
    val homeDangerousAttacks: Double,
    val awayDangerousAttacks: Double,
    val homePPDA: Double,
    val awayPPDA: Double,
    val homeCards: Int,
    val awayCards: Int
)
