package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.engine.GeminiAssistant
import com.example.data.engine.PredictionEngine
import com.example.data.local.FootballDatabase
import com.example.data.model.LiveEventEntity
import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatsEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.PredictionEntity
import com.example.data.repository.FootballRepository
import com.example.data.repository.SourceStatSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class FootballViewModel(application: Application) : AndroidViewModel(application) {

    private val database: FootballDatabase by lazy {
        Room.databaseBuilder(
            application,
            FootballDatabase::class.java,
            "football_analytics_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: FootballRepository by lazy {
        FootballRepository(database.footballDao())
    }

    // List of matches
    val matches: StateFlow<List<MatchEntity>> = repository.matchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected match for deep analytics tabs
    private val _selectedMatchId = MutableStateFlow<String?>(null)
    val selectedMatchId: StateFlow<String?> = _selectedMatchId.asStateFlow()

    // Active screen state. Match list vs Match details
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    sealed interface Screen {
        object Home : Screen
        data class Detail(val matchId: String) : Screen
    }

    // Flows derived dynamically from selectedMatchId
    val selectedMatch: StateFlow<MatchEntity?> = _selectedMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getMatchFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedStats: StateFlow<MatchStatsEntity?> = _selectedMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getStatsFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPrediction: StateFlow<PredictionEntity?> = _selectedMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getPredictionFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedLiveEvents: StateFlow<List<LiveEventEntity>> = _selectedMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getLiveEventsFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _homePlayers = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val homePlayers: StateFlow<List<PlayerEntity>> = _homePlayers.asStateFlow()

    private val _awayPlayers = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val awayPlayers: StateFlow<List<PlayerEntity>> = _awayPlayers.asStateFlow()

    // Interactive AI Insight generation state
    private val _aiInsightText = MutableStateFlow<String>("")
    val aiInsightText: StateFlow<String> = _aiInsightText.asStateFlow()

    private val _isGeneratingAiInsight = MutableStateFlow(false)
    val isGeneratingAiInsight: StateFlow<Boolean> = _isGeneratingAiInsight.asStateFlow()

    // Live Ingestion & Sync Simulation metrics
    private val _sourceSyncMetrics = MutableStateFlow<List<SourceSyncState>>(emptyList())
    val sourceSyncMetrics: StateFlow<List<SourceSyncState>> = _sourceSyncMetrics.asStateFlow()

    // Simulation simulation loops
    private var simulationJob: Job? = null
    val isSimulationRunning = MutableStateFlow(false)

    init {
        // Seeding database with premier matches on launch
        viewModelScope.launch {
            try {
                repository.seedDatabaseIfEmpty()
            } catch (e: Exception) {
                Log.e("FootballViewModel", "Database seeding failed", e)
            }
        }
    }

    /**
     * Navigation API
     */
    fun selectMatch(matchId: String) {
        _selectedMatchId.value = matchId
        _currentScreen.value = Screen.Detail(matchId)
        loadSquads(matchId)
        clearAiInsight()
        updateSourceSyncMetrics(matchId)
    }

    fun goBackToFeed() {
        stopSimulation()
        _selectedMatchId.value = null
        _currentScreen.value = Screen.Home
    }

    private fun loadSquads(matchId: String) {
        viewModelScope.launch {
            val match = matches.value.firstOrNull { it.id == matchId } ?: return@launch
            _homePlayers.value = repository.getPlayers(match.homeTeamId)
            _awayPlayers.value = repository.getPlayers(match.awayTeamId)
        }
    }

    /**
     * Core AI Integration: Triggers Gemini to build full-scale analytical report
     */
    fun generateAiInsightReport(match: MatchEntity, stats: MatchStatsEntity) {
        viewModelScope.launch {
            _isGeneratingAiInsight.value = true
            _aiInsightText.value = ""
            try {
                val homeTeam = database.footballDao().getTeamById(match.homeTeamId)
                val awayTeam = database.footballDao().getTeamById(match.awayTeamId)
                if (homeTeam != null && awayTeam != null) {
                    val report = GeminiAssistant.generateTacticalAnalysis(match, homeTeam, awayTeam, stats)
                    _aiInsightText.value = report
                } else {
                    _aiInsightText.value = "خطا در بارگذاری داده‌های ساختاری تیم‌ها."
                }
            } catch (e: Exception) {
                _aiInsightText.value = "خطا در ارتباط با سرورهای هوش مصنوعی: ${e.localizedMessage}"
            } finally {
                _isGeneratingAiInsight.value = false
            }
        }
    }

    private fun clearAiInsight() {
        _aiInsightText.value = ""
        _isGeneratingAiInsight.value = false
    }

    /**
     * Setup Source Ingestion metrics (API-Football, SportMonks, Football-Data.org)
     */
    private fun updateSourceSyncMetrics(matchId: String) {
        val rand = Random(matchId.hashCode())
        _sourceSyncMetrics.value = listOf(
            SourceSyncState("API-Football", "متصل", 95, rand.nextInt(400, 1200)),
            SourceSyncState("SportMonks", "متصل", 90, rand.nextInt(300, 900)),
            SourceSyncState("Football-Data.org", "متصل", 85, rand.nextInt(500, 1500)),
            SourceSyncState("SofaScore Parse", "پشتیبان زنده", 80, rand.nextInt(150, 450)),
            SourceSyncState("FlashScore Parse", "آماده‌باش", 75, rand.nextInt(120, 350)),
            SourceSyncState("ESPN Ingestion", "آماده‌باش", 70, rand.nextInt(600, 1800))
        )
    }

    /**
     * Start live simulation loop. Simulation runs completely local using Room.
     */
    fun startLiveSimulation(matchId: String) {
        if (isSimulationRunning.value) return
        isSimulationRunning.value = true

        simulationJob = viewModelScope.launch {
            val rand = Random(System.currentTimeMillis())

            while (isSimulationRunning.value) {
                val match = database.footballDao().getMatchById(matchId) ?: break
                val stats = database.footballDao().getStatsForMatch(matchId) ?: break
                val prediction = database.footballDao().getPredictionForMatch(matchId) ?: break

                // If finished, reset match status to active LIVE at min 45 to begin fresh simulation
                val currentMin = if (match.status == "FINISHED") 45 else match.minute
                val homeScore = if (match.status == "FINISHED") 0 else match.homeScore
                val awayScore = if (match.status == "FINISHED") 0 else match.awayScore

                if (currentMin >= 90) {
                    // Match has reached full time. End loop gracefully
                    stopSimulation()
                    break
                }

                val nextMin = currentMin + 1
                val checkEvent = rand.nextDouble()

                var newHomeScore = homeScore
                var newAwayScore = awayScore
                var eventTriggered: LiveEventEntity? = null

                // Live dynamic stats adjustment (shots & xG incremental addition)
                var newHomeXg = stats.homeXG
                var newAwayXg = stats.awayXG
                var newHomeShots = stats.homeShots
                var newAwayShots = stats.awayShots
                var newHomeDangerous = stats.homeDangerousAttacks
                var newAwayDangerous = stats.awayDangerousAttacks
                var newHomeCards = stats.homeCards
                var newAwayCards = stats.awayCards
                var newHomePossession = stats.homePossession

                // Dynamic possession drift
                val possDelta = rand.nextInt(-3, 4)
                newHomePossession = (newHomePossession + possDelta).coerceIn(35, 65)

                if (checkEvent < 0.25) {
                    // Random tactical event triggered!
                    val isHome = rand.nextBoolean()
                    val eventSelector = rand.nextDouble()

                    if (eventSelector < 0.40) {
                        // SHOT
                        val eventXg = rand.nextDouble(0.02, 0.45)
                        if (isHome) {
                            newHomeShots++
                            newHomeXg += eventXg
                            newHomeDangerous += rand.nextInt(1, 3)
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "SHOT",
                                team = "HOME",
                                detail = "حمله و شوت پویای پرسپولیس با امید گل ${String.format("%.2f", eventXg)}",
                                xGValue = eventXg
                            )
                        } else {
                            newAwayShots++
                            newAwayXg += eventXg
                            newAwayDangerous += rand.nextInt(1, 3)
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "SHOT",
                                team = "AWAY",
                                detail = "شوت از پشت محوطه فرار مهاجم استقلال با امید گل ${String.format("%.2f", eventXg)}",
                                xGValue = eventXg
                            )
                        }
                    } else if (eventSelector < 0.55) {
                        // GOAL!
                        val goalXg = rand.nextDouble(0.35, 0.85)
                        if (isHome) {
                            newHomeScore++
                            newHomeShots++
                            newHomeXg += goalXg
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "GOAL",
                                team = "HOME",
                                detail = "⚽ گل برای پرسپولیس! ورود توپ مستقیم به کادر بالا روی امید گل ${String.format("%.2f", goalXg)}",
                                xGValue = goalXg
                            )
                        } else {
                            newAwayScore++
                            newAwayShots++
                            newAwayXg += goalXg
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "GOAL",
                                team = "AWAY",
                                detail = "⚽ گل برای استقلال! تبدیل بی‌نقص ضدحمله روی گام‌های امید گل ${String.format("%.2f", goalXg)}",
                                xGValue = goalXg
                            )
                        }
                    } else if (eventSelector < 0.75) {
                        // DANGEROUS ATTACK
                        if (isHome) {
                            newHomeDangerous += rand.nextInt(2, 5)
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "DANGEROUS_ATTACK",
                                team = "HOME",
                                detail = "کارت مربیگری تاکتیکی: اصرار پرسپولیس به فشار تهاجمی بر خط تدافعی حریف",
                                xGValue = 0.05
                            )
                        } else {
                            newAwayDangerous += rand.nextInt(2, 5)
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "DANGEROUS_ATTACK",
                                team = "AWAY",
                                detail = "طرح ضدحمله پر سرعت ارسالی کلیدی از استقلال به جناحین نفوذی",
                                xGValue = 0.05
                            )
                        }
                    } else {
                        // CARD (Yellow card)
                        if (isHome) {
                            newHomeCards++
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "CARD",
                                team = "HOME",
                                detail = "کارت زرد صادر شده توسط داور به دلیل خطای قطع ضد حمله",
                                xGValue = 0.0
                            )
                        } else {
                            newAwayCards++
                            eventTriggered = LiveEventEntity(
                                id = "sim_ev_${System.currentTimeMillis()}",
                                matchId = matchId,
                                minute = nextMin,
                                type = "CARD",
                                team = "AWAY",
                                detail = "اخطار انضباطی جدی به بازیکن استقلال به علت رفتار ناورزشی",
                                xGValue = 0.0
                            )
                        }
                    }
                }

                // 2. Resolve stats through the multi-source broker logic!
                // We create a few conflicting source metrics
                val apiFootballSnapshot = SourceSyncStateSnapshotMapper.map(
                    "API-Football", newHomePossession, newHomeXg, newAwayXg, newHomeShots, newAwayShots, newHomeDangerous, newAwayDangerous, stats.homePPDA, stats.awayPPDA, newHomeCards, newAwayCards
                )
                val sportMonksSnapshot = SourceSyncStateSnapshotMapper.map(
                    "SportMonks", newHomePossession + rand.nextInt(-2, 3), newHomeXg + rand.nextDouble(-0.06, 0.06), newAwayXg + rand.nextDouble(-0.06, 0.06), newHomeShots + rand.nextInt(-1, 2), newAwayShots + rand.nextInt(-1, 2), newHomeDangerous + rand.nextInt(-3, 4), newAwayDangerous + rand.nextInt(-3, 4), stats.homePPDA, stats.awayPPDA, newHomeCards, newAwayCards
                )
                val sofaScoreSnapshot = SourceSyncStateSnapshotMapper.map(
                    "SofaScore Parse", newHomePossession + rand.nextInt(-1, 2), newHomeXg + rand.nextDouble(-0.03, 0.03), newAwayXg + rand.nextDouble(-0.03, 0.03), newHomeShots + 0, newAwayShots + 0, newHomeDangerous + rand.nextInt(-2, 3), newAwayDangerous + rand.nextInt(-2, 3), stats.homePPDA, stats.awayPPDA, newHomeCards, newAwayCards
                )

                val resolvedStats = repository.resolveMultiSourceStats(
                    matchId,
                    listOf(apiFootballSnapshot, sportMonksSnapshot, sofaScoreSnapshot)
                )

                // 3. Save resolved statistics
                repository.updateStats(resolvedStats)

                // 4. Save live event if any was triggered
                if (eventTriggered != null) {
                    repository.insertLiveEvent(eventTriggered)
                }

                // 5. Run Live Dynamic Bayesian prediction updates!
                val updatedPrediction = PredictionEngine.recalculateLivePrediction(
                    prediction, newHomeScore, newAwayScore, nextMin, resolvedStats, 0, 0
                )
                repository.updatePrediction(updatedPrediction)

                // 6. Update Match Score, status and minute
                val statusType = if (nextMin >= 90) "FINISHED" else "LIVE"
                val updatedMatch = match.copy(
                    homeScore = newHomeScore,
                    awayScore = newAwayScore,
                    minute = nextMin,
                    status = statusType
                )
                repository.updateMatch(updatedMatch)

                // Refresh sync metrics response times to show actively streaming network
                updateSourceSyncMetrics(matchId)

                // Simulation speed: 2.2 seconds represent 1 match minute of real intelligence
                delay(2200)
            }
        }
    }

    fun stopSimulation() {
        isSimulationRunning.value = false
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Resets the active match back to 0-0 UPCOMING or fresh starting state for simulation replay
     */
    fun resetMatch(matchId: String) {
        viewModelScope.launch {
            stopSimulation()
            val match = database.footballDao().getMatchById(matchId) ?: return@launch
            val updatedMatch = match.copy(
                status = "LIVE",
                homeScore = 0,
                awayScore = 0,
                minute = 45
            )
            repository.updateMatch(updatedMatch)

            val resetStats = MatchStatsEntity(
                matchId = matchId,
                homeXG = 0.50,
                awayXG = 0.30,
                homeShots = 4,
                awayShots = 3,
                homePossession = 50,
                awayPossession = 50,
                homePPDA = 9.5,
                awayPPDA = 11.0,
                homeDangerousAttacks = 15,
                awayDangerousAttacks = 12,
                homeCards = 0,
                awayCards = 0
            )
            repository.updateStats(resetStats)

            database.footballDao().clearLiveEventsForMatch(matchId)

            val homeTeam = database.footballDao().getTeamById(match.homeTeamId)
            val awayTeam = database.footballDao().getTeamById(match.awayTeamId)
            if (homeTeam != null && awayTeam != null) {
                val prePrediction = PredictionEngine.calculatePreMatchPrediction(
                    updatedMatch, homeTeam, awayTeam,
                    database.footballDao().getPlayersForTeam(match.homeTeamId),
                    database.footballDao().getPlayersForTeam(match.awayTeamId)
                )
                repository.updatePrediction(prePrediction)
            }
            clearAiInsight()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}

/**
 * Sync status for the Ingestion UI Panel
 */
data class SourceSyncState(
    val sourceName: String,
    val status: String,
    val confidence: Int,
    val pingMs: Int
)

object SourceSyncStateSnapshotMapper {
    fun map(
        name: String,
        homePoss: Int,
        homeXg: Double,
        awayXg: Double,
        homeShots: Int,
        awayShots: Int,
        homeDang: Int,
        awayDang: Int,
        homePpda: Double,
        awayPpda: Double,
        homeC: Int,
        awayC: Int
    ): SourceStatSnapshot {
        return SourceStatSnapshot(
            sourceName = name,
            homePossession = homePoss,
            homeXG = homeXg,
            awayXG = awayXg,
            homeShots = homeShots.toDouble(),
            awayShots = awayShots.toDouble(),
            homeDangerousAttacks = homeDang.toDouble(),
            awayDangerousAttacks = awayDang.toDouble(),
            homePPDA = homePpda,
            awayPPDA = awayPpda,
            homeCards = homeC,
            awayCards = awayC
        )
    }
}

class FootballViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FootballViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FootballViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
