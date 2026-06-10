package com.example.data.engine

import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatsEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.TeamEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object PredictionEngine {

    // --- Core Analytical Weights ---
    private const val W_ELO = 0.25
    private const val W_XG = 0.20
    private const val W_FORM = 0.15
    private const val W_SQUAD = 0.10
    private const val W_TACTICAL = 0.10
    private const val W_CONTEXT = 0.10
    private const val W_H2H = 0.05
    private const val W_EXTERNAL = 0.05

    /**
     * Compute Poisson probability mass: P(k; lambda) = (lambda^k * exp(-lambda)) / k!
     */
    private fun poissonProbability(k: Int, lambda: Double): Double {
        if (lambda <= 0.0) return if (k == 0) 1.0 else 0.0
        var factorial = 1.0
        for (i in 1..k) {
            factorial *= i
        }
        return (lambda.pow(k) * exp(-lambda)) / factorial
    }

    /**
     * Executes a weighted ensemble prediction model for upcoming matches.
     */
    fun calculatePreMatchPrediction(
        match: MatchEntity,
        homeTeam: TeamEntity,
        awayTeam: TeamEntity,
        homePlayers: List<PlayerEntity>,
        awayPlayers: List<PlayerEntity>
    ): PredictionEntity {

        // 1. ELO MODEL (Home Win probability via logistic function)
        val eloDiff = homeTeam.elo - awayTeam.elo + 60.0 // 60 ELO home advantage modifier
        val pEloHome = 1.0 / (1.0 + 10.0.pow(-eloDiff / 400.0))
        val eloFactor = (pEloHome - 0.5) * 2.0 // scale to -1.0 to 1.0 range

        // 2. xG MODEL
        // Calculate expected goals trend
        val homeAttack = homeTeam.attackRating / 50.0 // normal range is ~50.0 (1.0 xG)
        val awayDefense = awayTeam.defenseRating / 50.0
        val awayAttack = awayTeam.attackRating / 50.0
        val homeDefense = homeTeam.defenseRating / 50.0

        val expectedXGHome = homeAttack * (2.0 - awayDefense) * 1.35 // 1.35 base rating
        val expectedXGAway = awayAttack * (2.0 - homeDefense) * 1.20

        val xGFactor = (expectedXGHome - expectedXGAway) / 3.0 // scale to -1.0 to 1.0 scale

        // 3. FORM MOMENTUM MODEL (Exponential Weighting)
        val homeFormScore = parseFormScore(homeTeam.formString)
        val awayFormScore = parseFormScore(awayTeam.formString)
        val formFactor = (homeFormScore - awayFormScore) / 4.0 // scale to -1.0 to 1.0

        // 4. SQUAD IMPACT MODEL
        val homeSquadScore = calculateSquadEffect(homePlayers)
        val awaySquadScore = calculateSquadEffect(awayPlayers)
        val squadFactor = (homeSquadScore - awaySquadScore) / 10.0

        // 5. TACTICAL MATCHUP ENGINE
        // Matches tactical styles (e.g. High Press beats Slow Build-up, Counter Attack beats High Press)
        val tacticalAdvantage = calculateTacticalAdvantage(homeTeam.tacticalStyle, awayTeam.tacticalStyle, homeTeam.formation, awayTeam.formation)
        val tacticalFactor = tacticalAdvantage / 2.0

        // 6. CONTEXT MODEL
        val restAdvantage = (match.restDaysHome - match.restDaysAway).coerceIn(-4, 4) * 0.1
        val travelFatigue = if (match.restDaysAway < 3) -0.15 else 0.0
        val contextFactor = restAdvantage + travelFatigue

        // 7. H2H MODEL (Home historically wins 55% against this away, say)
        val h2hFactor = 0.10 // constant for simplicity in this analytical template

        // 8. EXTERNAL FACTORS MODEL
        // Weather has dampening or accelerating effects on style, stadium intensity helps home
        val intensityAdv = (match.stadiumIntensity - 3.0) * 0.1
        val refereeFactor = (match.refereeStrictness - 3.0) * 0.05 // high strictness can disrupt build-up style
        val externalFactor = intensityAdv - refereeFactor

        // --- CALIBRATION INTERFACE ---
        // FINAL PREDICTION SCORE inside a relative scale (-1.0 to 1.0)
        val relativeAdvantage = (
            (W_ELO * eloFactor) +
            (W_XG * xGFactor) +
            (W_FORM * formFactor) +
            (W_SQUAD * squadFactor) +
            (W_TACTICAL * tacticalFactor) +
            (W_CONTEXT * contextFactor) +
            (W_H2H * h2hFactor) +
            (W_EXTERNAL * externalFactor)
        )

        // Baseline goal targets (average league goals around 2.6 per game)
        val baseHomeXG = (1.4 + relativeAdvantage * 1.5).coerceIn(0.1, 4.5)
        val baseAwayXG = (1.2 - relativeAdvantage * 1.5).coerceIn(0.1, 4.5)

        // Applying weather dampening
        val weatherDampening = if (match.weather in listOf("Rain", "Snow", "Wind")) 0.85 else 1.0
        val finalHomeExpectedG = baseHomeXG * weatherDampening
        val finalAwayExpectedG = baseAwayXG * weatherDampening

        // -- Monte Carlo Simulation (10,000 runs) --
        var homeWins = 0
        var draws = 0
        var awayWins = 0
        var bttsCount = 0
        var over05 = 0
        var over15 = 0
        var over25 = 0
        var over35 = 0

        val scorelineFreq = mutableMapOf<String, Int>()

        val rand = Random(42) // Seed for deterministic consistency

        // Run Monte Carlo simulation loops
        val simIterations = 10000
        for (i in 0 until simIterations) {
            val homeGoals = simulatePoissonSample(finalHomeExpectedG, rand)
            val awayGoals = simulatePoissonSample(finalAwayExpectedG, rand)

            if (homeGoals > awayGoals) homeWins++
            else if (homeGoals == awayGoals) draws++
            else awayWins++

            val totalGoals = homeGoals + awayGoals
            if (totalGoals > 0) over05++
            if (totalGoals > 1) over15++
            if (totalGoals > 2) over25++
            if (totalGoals > 3) over35++

            if (homeGoals > 0 && awayGoals > 0) bttsCount++

            val scoreKey = "$homeGoals-$awayGoals"
            scorelineFreq[scoreKey] = (scorelineFreq[scoreKey] ?: 0) + 1
        }

        val winProb = homeWins.toDouble() / simIterations
        val drawProb = draws.toDouble() / simIterations
        val lossProb = awayWins.toDouble() / simIterations

        // Sort scorelines by frequency
        val sortedScores = scorelineFreq.toList()
            .sortedByDescending { it.second }
            .take(5)

        val topScoresArray = JSONArray()
        for (score in sortedScores) {
            val obj = JSONObject()
            obj.put("score", score.first)
            obj.put("prob", score.second.toDouble() / simIterations)
            topScoresArray.put(obj)
        }

        // Confidence calculation based on team metrics variance
        val eloGap = kotlin.math.abs(homeTeam.elo - awayTeam.elo)
        val confidenceScore = (0.5 + (eloGap / 1000.0) + (1.0 - (drawProb * 1.5))).coerceIn(0.4, 0.95)

        // Uncertainty Indicator based on ELO closeness
        val uncertainty = when {
            eloGap < 100.0 -> "بالا (عدم قطعیت شدید)"
            eloGap < 250.0 -> "متوسط (پایداری نسبی)"
            else -> "پایین (پیش‌بینی با ثبات)"
        }

        // Initial Pre-Live Drift JSON
        val preLiveDrift = JSONArray().apply {
            val preObj = JSONObject()
            preObj.put("min", 0)
            preObj.put("h", (winProb * 100).toInt())
            preObj.put("d", (drawProb * 100).toInt())
            preObj.put("a", (lossProb * 100).toInt())
            put(preObj)
        }

        return PredictionEntity(
            matchId = match.id,
            winProb = winProb,
            drawProb = drawProb,
            lossProb = lossProb,
            over05 = over05.toDouble() / simIterations,
            over15 = over15.toDouble() / simIterations,
            over25 = over25.toDouble() / simIterations,
            over35 = over35.toDouble() / simIterations,
            bttsProb = bttsCount.toDouble() / simIterations,
            topScoresJson = topScoresArray.toString(),
            confidence = confidenceScore,
            momentumIndex = relativeAdvantage * 100.0,
            uncertaintyIndicator = uncertainty,
            preLiveDriftJson = preLiveDrift.toString()
        )
    }

    /**
     * Real-Time Bayesian live prediction updating mechanism.
     */
    fun recalculateLivePrediction(
        prePrediction: PredictionEntity,
        currentHomeScore: Int,
        currentAwayScore: Int,
        elapsedMinute: Int,
        stats: MatchStatsEntity,
        numRedCardsHome: Int = 0,
        numRedCardsAway: Int = 0
    ): PredictionEntity {
        val totalMins = 90.0
        val t = elapsedMinute / totalMins

        // Estimate expected *remaining* goals using prior prediction expectations Adjusted by events
        val preHomeXG = (prePrediction.winProb * 1.8 + prePrediction.drawProb * 0.8).coerceIn(0.1, 4.0)
        val preAwayXG = (prePrediction.lossProb * 1.8 + prePrediction.drawProb * 0.8).coerceIn(0.1, 4.0)

        // Adjust remaining expectation based on current red cards and live possession momentum
        var remainingHomeXG = preHomeXG * (1.0 - t)
        var remainingAwayXG = preAwayXG * (1.0 - t)

        // Adjust for red cards
        remainingHomeXG *= 1.0 - (numRedCardsHome * 0.25)
        remainingHomeXG *= 1.0 + (numRedCardsAway * 0.15)
        remainingAwayXG *= 1.0 - (numRedCardsAway * 0.25)
        remainingAwayXG *= 1.0 + (numRedCardsHome * 0.15)

        // Adjust for live Stats momentum (xG generated so far + possession drift)
        // Expected future performance is a Bayesian fusion of pre-match prior and live performance trends
        if (elapsedMinute > 10) {
            val liveHomeTrend = (stats.homeXG / (elapsedMinute / 90.0))
            val liveAwayTrend = (stats.awayXG / (elapsedMinute / 90.0))

            // Bayesian updating formula: Posterior index = Prior * (1 - t) + Live * t
            remainingHomeXG = (remainingHomeXG * (1.0 - t)) + (liveHomeTrend * (1.0 - t) * t)
            remainingAwayXG = (remainingAwayXG * (1.0 - t)) + (liveAwayTrend * (1.0 - t) * t)
        }

        // Ensure baseline limits
        remainingHomeXG = remainingHomeXG.coerceAtLeast(0.0)
        remainingAwayXG = remainingAwayXG.coerceAtLeast(0.0)

        // Run Mini Monte Carlo (3,000 iterations for lightning fast live performance)
        var homeWins = 0
        var draws = 0
        var awayWins = 0
        var bttsCount = 0
        var over05 = 0
        var over15 = 0
        var over25 = 0
        var over35 = 0

        val scorelineFreq = mutableMapOf<String, Int>()
        val rand = Random(123)

        val simIterations = 3000
        for (i in 0 until simIterations) {
            val remainingHomeGoals = simulatePoissonSample(remainingHomeXG, rand)
            val remainingAwayGoals = simulatePoissonSample(remainingAwayXG, rand)

            val finalHomeGoals = currentHomeScore + remainingHomeGoals
            val finalAwayGoals = currentAwayScore + remainingAwayGoals

            if (finalHomeGoals > finalAwayGoals) homeWins++
            else if (finalHomeGoals == finalAwayGoals) draws++
            else awayWins++

            val totalGoals = finalHomeGoals + finalAwayGoals
            if (totalGoals > 0) over05++
            if (totalGoals > 1) over15++
            if (totalGoals > 2) over25++
            if (totalGoals > 3) over35++

            if (finalHomeGoals > 0 && finalAwayGoals > 0) bttsCount++

            val scoreKey = "$finalHomeGoals-$finalAwayGoals"
            scorelineFreq[scoreKey] = (scorelineFreq[scoreKey] ?: 0) + 1
        }

        val winProb = homeWins.toDouble() / simIterations
        val drawProb = draws.toDouble() / simIterations
        val lossProb = awayWins.toDouble() / simIterations

        val sortedScores = scorelineFreq.toList()
            .sortedByDescending { it.second }
            .take(5)

        val topScoresArray = JSONArray()
        for (score in sortedScores) {
            val obj = JSONObject()
            obj.put("score", score.first)
            obj.put("prob", score.second.toDouble() / simIterations)
            topScoresArray.put(obj)
        }

        // Recalculate Live drift array from saved prior
        val driftArray = try {
            JSONArray(prePrediction.preLiveDriftJson)
        } catch (e: Exception) {
            JSONArray()
        }

        // Add current live minute snapshot
        val driftObj = JSONObject().apply {
            put("min", elapsedMinute)
            put("h", (winProb * 100).toInt())
            put("d", (drawProb * 100).toInt())
            put("a", (lossProb * 100).toInt())
        }
        driftArray.put(driftObj)

        // Dynamic Momentum index
        val statsGap = (stats.homeXG - stats.awayXG) * 50.0
        val liveMomentum = (prePrediction.momentumIndex * (1.0 - t) + statsGap).coerceIn(-100.0, 100.0)

        return prePrediction.copy(
            winProb = winProb,
            drawProb = drawProb,
            lossProb = lossProb,
            over05 = over05.toDouble() / simIterations,
            over15 = over15.toDouble() / simIterations,
            over25 = over25.toDouble() / simIterations,
            over35 = over35.toDouble() / simIterations,
            bttsProb = bttsCount.toDouble() / simIterations,
            topScoresJson = topScoresArray.toString(),
            momentumIndex = liveMomentum,
            preLiveDriftJson = driftArray.toString()
        )
    }

    /**
     * Poisson generator using Knuth method or exponential transform
     */
    private fun simulatePoissonSample(lambda: Double, random: Random): Int {
        val l = exp(-lambda)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= random.nextDouble()
        } while (p > l && k < 30) // Upper limit safeguard
        return k - 1
    }

    private fun parseFormScore(form: String): Double {
        val results = form.split(",")
        var multiplier = 1.0
        var total = 0.0
        for (item in results.reversed()) {
            val points = when (item.trim().uppercase()) {
                "W" -> 3.0
                "D" -> 1.0
                else -> 0.0
            }
            total += points * multiplier
            multiplier *= 0.85 // Exponential decay weighting of historical form
        }
        return total
    }

    private fun calculateSquadEffect(players: List<PlayerEntity>): Double {
        if (players.isEmpty()) return 5.0
        var fitnessWeightedImpact = 0.0
        var totalImpact = 0.0
        for (p in players) {
            val currentImpact = if (p.isInjured) 0.0 else p.impactRating * (p.fitness / 100.0)
            fitnessWeightedImpact += currentImpact
            totalImpact += 10.0
        }
        return (fitnessWeightedImpact / totalImpact) * 10.0
    }

    private fun calculateTacticalAdvantage(
        homeStyle: String, awayStyle: String,
        homeFormation: String, awayFormation: String
    ): Double {
        // High Press beats Possessive
        // Possessive beats Low Block
        // Counter Attack beats High Press
        // Low Block beats High Press
        var styleAdvantage = 0.0
        if (homeStyle == "High Press" && awayStyle == "Possessive") styleAdvantage += 0.3
        if (homeStyle == "Possessive" && awayStyle == "Low Block") styleAdvantage += 0.2
        if (homeStyle == "Counter Attack" && awayStyle == "High Press") styleAdvantage += 0.4
        if (homeStyle == "Low Block" && awayStyle == "High Press") styleAdvantage += 0.15

        if (awayStyle == "High Press" && homeStyle == "Possessive") styleAdvantage -= 0.3
        if (awayStyle == "Possessive" && homeStyle == "Low Block") styleAdvantage -= 0.2
        if (awayStyle == "Counter Attack" && homeStyle == "High Press") styleAdvantage -= 0.4
        if (awayStyle == "Low Block" && homeStyle == "High Press") styleAdvantage -= 0.15

        // Simple Formation clash
        // 3-5-2 controls midfield over 4-3-3, 4-2-3-1 neutralizes 3-5-2
        if (homeFormation == "3-5-2" && awayFormation == "4-3-3") styleAdvantage += 0.1
        if (awayFormation == "3-5-2" && homeFormation == "4-3-3") styleAdvantage -= 0.1

        return styleAdvantage
    }
}
