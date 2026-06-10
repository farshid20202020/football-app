@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.FootballViewModel
import org.json.JSONArray
import org.json.JSONObject

// Sports Theming Constants
val NeonGreen = Color(0xFF00E676)
val SportsSlateBg = Color(0xFF0B0C10)
val DarkGreyBg = Color(0xFF12131C)
val CardSurface = Color(0xFF1B1D2A)
val HighlightYellow = Color(0xFFFFD600)
val RedCardColor = Color(0xFFFF1744)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FootballViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Force RTL direction for a perfect Persian layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SportsSlateBg)
        ) {
            when (val screen = currentScreen) {
                is FootballViewModel.Screen.Home -> {
                    HomeScreen(viewModel = viewModel)
                }
                is FootballViewModel.Screen.Detail -> {
                    MatchDetailScreen(
                        matchId = screen.matchId,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/**
 * --- HOME SCREEN FEED ---
 */
@Composable
fun HomeScreen(viewModel: FootballViewModel) {
    val matches by viewModel.matches.collectAsState()
    var selectedLeague by remember { mutableStateOf("همه") }
    val leaguesList = listOf("همه", "لیگ برتر خلیج فارس", "لیگ قهرمانان اروپا", "لیگ برتر انگلیس")

    val filteredMatches = remember(matches, selectedLeague) {
        if (selectedLeague == "همه") matches
        else matches.filter { it.leagueName == selectedLeague }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Logo",
                            tint = NeonGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "فوتبال آنالیتیکس پرو پلاس اصلی",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreyBg
                )
            )
        },
        containerColor = SportsSlateBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // League Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leaguesList.forEach { league ->
                    FilterChip(
                        selected = selectedLeague == league,
                        onClick = { selectedLeague = league },
                        label = { Text(league) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurface,
                            labelColor = Color.LightGray,
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = Color.Black
                        ),
                        border = null
                    )
                }
            }

            // Matches List
            if (filteredMatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("مسابقه‌ای در این دسته‌بندی یافت نشد", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // LIVE Section header
                    val liveMatches = filteredMatches.filter { it.status == "LIVE" }
                    if (liveMatches.isNotEmpty()) {
                        item {
                            SectionHeader(title = "مسابقات زنده و در حال جریان", isLive = true)
                        }
                        items(liveMatches) { match ->
                            MatchFeedItem(match = match, onClick = { viewModel.selectMatch(match.id) })
                        }
                    }

                    // UPCOMING & FINISHED Header
                    val remainingMatches = filteredMatches.filter { it.status != "LIVE" }
                    if (remainingMatches.isNotEmpty()) {
                        item {
                            SectionHeader(title = "برنامه بازی‌ها و نتایج اخیر", isLive = false)
                        }
                        items(remainingMatches) { match ->
                            MatchFeedItem(match = match, onClick = { viewModel.selectMatch(match.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, isLive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(RedCardColor)
            )
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isLive) RedCardColor else Color.White
        )
    }
}

@Composable
fun MatchFeedItem(
    match: MatchEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("match_item_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // League and Venue Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.leagueName,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (match.status == "LIVE") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RedCardColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "زنده: دقیقه ${match.minute}'",
                            color = RedCardColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val statusText = if (match.status == "FINISHED") "پایان یافته" else match.matchTime
                    Text(
                        text = statusText,
                        color = if (match.status == "FINISHED") Color.Gray else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreline & Team Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Home Team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.homeTeamName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = Alignment.CenterHorizontally.let { TextAlign.Center },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Core Score or VS divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    if (match.status != "UPCOMING") {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = if (match.status == "LIVE") NeonGreen else Color.White
                        )
                    } else {
                        Text(
                            text = "خروج",
                            color = Color.Transparent,
                            fontSize = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "VS",
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Away Team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = HighlightYellow, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.awayTeamName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Stadium descriptor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                Text(
                    text = match.stadium,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * --- MATCH ANALYTICS DETAIL SCREEN ---
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String,
    viewModel: FootballViewModel
) {
    val match by viewModel.selectedMatch.collectAsState()
    val stats by viewModel.selectedStats.collectAsState()
    val prediction by viewModel.selectedPrediction.collectAsState()
    val liveEvents by viewModel.selectedLiveEvents.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("نمای کلی", "آنالیز پیشرفته", "تحلیل تاکتیکی", "موتور پیش‌بینی", "مونت کارلو", "هوشمندی زنده")

    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    val currentMatch = match!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${currentMatch.homeTeamName} مقابل ${currentMatch.awayTeamName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.goBackToFeed() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreyBg
                )
            )
        },
        containerColor = SportsSlateBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Status details Header Card
            DetailHeaderCard(currentMatch = currentMatch, viewModel = viewModel)

            // Custom Analytics Tabs Slider
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = DarkGreyBg,
                contentColor = NeonGreen,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = NeonGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = NeonGreen,
                        unselectedContentColor = Color.LightGray
                    )
                }
            }

            // Tab Views Switch Router
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> OverviewTab(currentMatch, liveEvents, stats)
                    1 -> AdvancedAnalyticsTab(stats)
                    2 -> TacticalBreakdownTab(currentMatch, stats, viewModel)
                    3 -> PredictionEngineTab(prediction)
                    4 -> SimulationLabTab(prediction)
                    5 -> LiveIntelligenceTab(currentMatch, prediction, stats, viewModel)
                }
            }
        }
    }
}

/**
 * --- DETAILS HEADER CARD ---
 */
@Composable
fun DetailHeaderCard(
    currentMatch: MatchEntity,
    viewModel: FootballViewModel
) {
    val isSimRunning by viewModel.isSimulationRunning.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentMatch.leagueName,
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentMatch.homeTeamName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }

                // Scoreline & Live indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentMatch.homeScore} - ${currentMatch.awayScore}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentMatch.status == "LIVE") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val glowAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(RedCardColor.copy(alpha = glowAlpha))
                            )
                            Text(
                                text = "دقیقه ${currentMatch.minute}'",
                                color = RedCardColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (currentMatch.status == "FINISHED") {
                        Text("پایان یافته", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Text("شروع ساعت ${currentMatch.matchTime}", color = HighlightYellow, fontSize = 12.sp)
                    }
                }

                // Away Team
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = HighlightYellow, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentMatch.awayTeamName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }

            if (isSimRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ شبیه‌ساز مونت کارلو و به روز رسانی بیزی زنده فعال است...",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * --- TAB 1: OVERVIEW (نمای کلی) ---
 */
@Composable
fun OverviewTab(
    match: MatchEntity,
    events: List<LiveEventEntity>,
    stats: MatchStatsEntity?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick stats panel
        if (stats != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("خلاصه آماری مسابقه", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // xG meter
                        MetricSlider(label = "شمارش امید گل واقعی (xG)", leftVal = String.format("%.2f", stats.homeXG), rightVal = String.format("%.2f", stats.awayXG), leftRatio = (stats.homeXG / (stats.homeXG + stats.awayXG + 0.1)).toFloat())
                        Spacer(modifier = Modifier.height(12.dp))

                        // Possession meter
                        MetricSlider(label = "مالکیت توپ", leftVal = "${stats.homePossession}%", rightVal = "${stats.awayPossession}%", leftRatio = stats.homePossession.toFloat() / 100f)
                    }
                }
            }
        }

        // Live Event Feed
        item {
            Text("جریان رویدادهای زنده بازی", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp), contentAlignment = Alignment.Center
                ) {
                    Text("در انتظار رویدادهای مسابقه...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(events) { event ->
                EventTimelineRow(event = event)
            }
        }
    }
}

@Composable
fun EventTimelineRow(event: LiveEventEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dot & Minute indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (event.type == "GOAL") NeonGreen else Color.Gray)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("${event.minute}'", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }

        // Event descriptors
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Icon
                val icon = when (event.type) {
                    "GOAL" -> Icons.Default.SportsSoccer
                    "CARD" -> Icons.Default.Warning
                    "SHOT" -> Icons.Default.DirectionsRun
                    else -> Icons.Default.Info
                }
                val iconColor = when (event.type) {
                    "GOAL" -> HighlightYellow
                    "CARD" -> RedCardColor
                    else -> NeonGreen
                }
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))

                Column {
                    Text(
                        text = event.detail,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (event.xGValue > 0.0) {
                        Text(
                            text = "افزایش امید گل: +${String.format("%.2f", event.xGValue)} xG",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * --- TAB 2: ADVANCED STATS (آنالیز پیشرفته) ---
 */
@Composable
fun AdvancedAnalyticsTab(stats: MatchStatsEntity?) {
    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("آماری برای نمایش موجود نیست", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مقایسه آمارهای کلیدی هوشمندانه (Opta Matrix)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    MetricSlider(label = "جدول شوت‌های کل", leftVal = "${stats.homeShots}", rightVal = "${stats.awayShots}", leftRatio = (stats.homeShots.toFloat() / (stats.homeShots + stats.awayShots + 0.1f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    MetricSlider(label = "حملات خطرناک", leftVal = "${stats.homeDangerousAttacks}", rightVal = "${stats.awayDangerousAttacks}", leftRatio = (stats.homeDangerousAttacks.toFloat() / (stats.homeDangerousAttacks + stats.awayDangerousAttacks + 0.1f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    // PPDA Meter (lower is better, represents intensive press)
                    val homePPDAValue = if (stats.homePPDA == 0.0) 10.0 else stats.homePPDA
                    val awayPPDAValue = if (stats.awayPPDA == 0.0) 10.0 else stats.awayPPDA
                    val ppdaHomeRatio = (awayPPDAValue / (homePPDAValue + awayPPDAValue)).toFloat() // invert since lower is aggressive
                    MetricSlider(label = "شدت پرسینگ دفاعی (PPDA)", leftVal = String.format("%.1f", stats.homePPDA), rightVal = String.format("%.1f", stats.awayPPDA), leftRatio = ppdaHomeRatio)
                    Spacer(modifier = Modifier.height(16.dp))

                    MetricSlider(label = "کارت تخلفات انضباطی", leftVal = "${stats.homeCards}", rightVal = "${stats.awayCards}", leftRatio = (stats.homeCards.toFloat() / (stats.homeCards + stats.awayCards + 0.1f)))
                }
            }
        }

        // xG Flow Chart Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("جریان زمانی امید گل (xG Flow Timeline)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("خطوط شبیه‌ساز کیفیت موقعیت‌های شوت‌زنی انباشته هر دو تیم", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    XGFlowCanvas(stats)
                }
            }
        }
    }
}

@Composable
fun XGFlowCanvas(stats: MatchStatsEntity) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        val width = size.width
        val height = size.height

        // Draw grid lines
        for (i in 1..3) {
            val gridX = width * (i / 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw curves for Home XG and Away XG
        val homeXGTarget = stats.homeXG.coerceIn(0.0, 3.5)
        val awayXGTarget = stats.awayXG.coerceIn(0.0, 3.5)

        val homePath = Path().apply {
            moveTo(0f, height)
            quadraticTo(width * 0.4f, height - (homeXGTarget.toFloat() * height * 0.15f), width, height - (homeXGTarget.toFloat() * height * 0.25f))
        }

        val awayPath = Path().apply {
            moveTo(0f, height)
            quadraticTo(width * 0.5f, height - (awayXGTarget.toFloat() * height * 0.12f), width, height - (awayXGTarget.toFloat() * height * 0.25f))
        }

        drawPath(
            path = homePath,
            color = NeonGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = awayPath,
            color = HighlightYellow,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(NeonGreen))
            Text("میزبان: ${String.format("%.2f", stats.homeXG)} xG", color = Color.White, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(HighlightYellow))
            Text("مهمان: ${String.format("%.2f", stats.awayXG)} xG", color = Color.White, fontSize = 11.sp)
        }
    }
}

/**
 * --- TAB 3: TACTICAL BREAKDOWN (تحلیل تاکتیکی) ---
 */
@Composable
fun TacticalBreakdownTab(
    match: MatchEntity,
    stats: MatchStatsEntity?,
    viewModel: FootballViewModel
) {
    val aiInsightText by viewModel.aiInsightText.collectAsState()
    val isGeneratingInsight by viewModel.isGeneratingAiInsight.collectAsState()

    val homePlayers by viewModel.homePlayers.collectAsState()
    val awayPlayers by viewModel.awayPlayers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // AI Generator trigger Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreyBg),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "هوش مصنوعی آنالیزور تاکتیکی",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "با استفاده از مدل‌های زنده زبانی گوگل جمیینی بر اساس متغیرهای دقیق مسابقه",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isGeneratingInsight) {
                        CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("درحال برقراری ارتباط امن و استخراج داده...", color = NeonGreen, fontSize = 11.sp)
                    } else {
                        Button(
                            onClick = {
                                if (stats != null) viewModel.generateAiInsightReport(match, stats)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_ai_insight_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دریافت آنالیز تاکتیکی هوش خلاق", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (aiInsightText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))
                        SelectionContainer {
                            Text(
                                text = aiInsightText,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }
            }
        }

        // Formation Clash visual presentation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تقابل آرایش ساختاری و سبک مربیان", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.homeTeamName, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(10.dp)) {
                                Text("آرایش ۴-۲-۳-۱", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.awayTeamName, color = HighlightYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(10.dp)) {
                                Text("آرایش ۳-۵-۲", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Squad line-up listing
        item {
            Text("ترکیب احتمالی و شاخص تاثیرگذاری بازیکنان", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        if (homePlayers.isEmpty() && awayPlayers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("در حال بارگذاری لیست بازیکنان...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Home players column
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(match.homeTeamName, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        homePlayers.forEach { player ->
                            PlayerImpactRow(player = player, color = NeonGreen)
                        }
                    }

                    // Away players column
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(match.awayTeamName, color = HighlightYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        awayPlayers.forEach { player ->
                            PlayerImpactRow(player = player, color = HighlightYellow)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerImpactRow(player: PlayerEntity, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = if (player.isInjured) Color.Gray else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.position} | آمادگی: ${player.fitness.toInt()}%",
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
            if (player.isInjured) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedCardColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("مصدوم", color = RedCardColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format("%.1f", player.impactRating),
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * --- TAB 4: PREDICTION ENGINE (موتور پیش‌بینی) ---
 */
@Composable
fun PredictionEngineTab(prediction: PredictionEntity?) {
    if (prediction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("پیش‌بینی‌های فعال استخراج نشده است.", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Home/Draw/Away slider
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("احتمالات برد / مساوی / باخت (تلفیقی هوش مصنوعی)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    ProbabilityBar(
                        home = prediction.winProb,
                        draw = prediction.drawProb,
                        away = prediction.lossProb
                    )
                }
            }
        }

        // BTTS & Goal Line poisson distribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("شبیه‌سازی گل و توزیع احتمال پوآسون", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    MetricSlider(label = "گلزنی هر دو تیم (BTTS)", leftVal = String.format("%.1f%%", prediction.bttsProb * 100), rightVal = String.format("%.1f%%", (1.0 - prediction.bttsProb) * 100), leftRatio = prediction.bttsProb.toFloat())
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("برآورد گل‌های نهایی (خطوط حد نصاب)", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    DoubleMetricProgress(label = "بیش از ۰.۵ گل", ratio = prediction.over05)
                    DoubleMetricProgress(label = "بیش از ۱.۵ گل", ratio = prediction.over15)
                    DoubleMetricProgress(label = "بیش از ۲.۵ گل (پیش‌فرض کارشناسان)", ratio = prediction.over25)
                    DoubleMetricProgress(label = "بیش از ۳.۵ گل", ratio = prediction.over35)
                }
            }
        }

        // Quality measures
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ضریب اطمینان مدل", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format("%.1f%%", prediction.confidence * 100),
                            color = NeonGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("میزان عدم قطعیت آماری", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = prediction.uncertaintyIndicator,
                            color = HighlightYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // IMPORTANT STRICT DISCLAIMER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(
                        text = "سلب مسئولیت قانونی: این اپلیکیشن صرفاً یک پایگاه داده ریاضی و آنالیز توصیفی فوتبال است. این نرم‌افزار به هیچ عنوان توصیه، پیشنهاد خرید یا ابزار شرط‌بندی مالی را ارائه نمی‌دهد. هرگونه اقدام احتمالی بر اساس این متغیرها کاملاً بر عهده کاربر است.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProbabilityBar(home: Double, draw: Double, away: Double) {
    val total = home + draw + away
    val homeRatio = (home / total).toFloat()
    val drawRatio = (draw / total).toFloat()
    val awayRatio = (away / total).toFloat()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(homeRatio.coerceAtLeast(0.01f))
                        .background(NeonGreen)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(drawRatio.coerceAtLeast(0.01f))
                        .background(Color.Gray)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(awayRatio.coerceAtLeast(0.01f))
                        .background(HighlightYellow)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(NeonGreen))
                Text("برد میزبان: ${String.format("%.1f%%", homeRatio * 100)}", color = Color.White, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color.Gray))
                Text("مساوی: ${String.format("%.1f%%", drawRatio * 100)}", color = Color.White, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(HighlightYellow))
                Text("برد مهمان: ${String.format("%.1f%%", awayRatio * 100)}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DoubleMetricProgress(label: String, ratio: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(180.dp))
        LinearProgressIndicator(
            progress = { ratio.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = NeonGreen,
            trackColor = Color.White.copy(alpha = 0.05f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(String.format("%.1f%%", ratio * 100), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
    }
}

@Composable
fun MetricSlider(
    label: String,
    leftVal: String,
    rightVal: String,
    leftRatio: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(leftVal, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(36.dp))
            LinearProgressIndicator(
                progress = { leftRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = NeonGreen,
                trackColor = HighlightYellow
            )
            Text(rightVal, color = HighlightYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        }
    }
}

/**
 * --- TAB 5: SIMULATION LAB (مونت کارلو شبیه‌ساز) ---
 */
@Composable
fun SimulationLabTab(prediction: PredictionEntity?) {
    if (prediction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("شبیه‌سازی مونت کارلو آماده سازی نشده است.", color = Color.Gray)
        }
        return
    }

    val topScoresList = remember(prediction.topScoresJson) {
        try {
            val arr = JSONArray(prediction.topScoresJson)
            val list = mutableListOf<ParsedScoreline>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(ParsedScoreline(obj.getString("score"), obj.getDouble("prob")))
            }
            list
        } catch (e: Exception) {
            emptyList<ParsedScoreline>()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("آزمایشگاه شبیه‌ساز مونت کارلو (50,000 تکرار)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "محاسبه احتمالات واقعی ثبت متداول‌ترین نتایج دقیق در شرایط متغیر شبیه‌ساز",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    topScoresList.forEachIndexed { index, item ->
                        ScorelineProbRow(index + 1, item.score, item.prob)
                    }
                }
            }
        }

        // Live trend momentum index modifier
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("شاخص متغیر تلاطم تهاجمی (Momentum Drift)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("انحراف از خط پایداری بر اساس شتاب تهاجمی تیم‌ها", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    val rawMomentum = prediction.momentumIndex // ranges from -100 to 100
                    val normalizedProgress = ((rawMomentum + 100.0) / 200.0).toFloat()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تمرکز به دفاع استقلال", color = HighlightYellow, fontSize = 10.sp)
                        Text("حمله پیوسته پرسپولیس", color = NeonGreen, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { normalizedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = NeonGreen,
                        trackColor = HighlightYellow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "شاخص برآیند حرکت تاکتیکی: ${String.format("%.1f", rawMomentum)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

data class ParsedScoreline(
    val score: String,
    val prob: Double
)

@Composable
fun ScorelineProbRow(rank: Int, score: String, prob: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Text("#$rank", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = score,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            modifier = Modifier.width(60.dp)
        )

        LinearProgressIndicator(
            progress = { prob.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = NeonGreen,
            trackColor = Color.White.copy(alpha = 0.05f)
        )

        Text(
            text = String.format("%.1f%%", prob * 100),
            color = NeonGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * --- TAB 6: LIVE INTELLIGENCE PANEL (پنل هوشمند زنده) ---
 */
@Composable
fun LiveIntelligenceTab(
    match: MatchEntity,
    prediction: PredictionEntity?,
    stats: MatchStatsEntity?,
    viewModel: FootballViewModel
) {
    val isSimRunning by viewModel.isSimulationRunning.collectAsState()
    val syncMetrics by viewModel.sourceSyncMetrics.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // PRE AND LIVE CONSOLE SIMULATOR WIDGET
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreyBg),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("کنسول شبیه‌ساز داده جاری (Bayesian Live Engine)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("هر ۲.۲ ثانیه معادل یک دقیقه مسابقه برای شبیه‌سازی تغییرات زنده و انحرافات احتمالی نسبت به شبیه‌ساز مونت کارلو است.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSimRunning) {
                            Button(
                                onClick = { viewModel.stopSimulation() },
                                colors = ButtonDefaults.buttonColors(containerColor = RedCardColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_simulation_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توقف شبیه‌سازی", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startLiveSimulation(match.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_simulation_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("شروع شبیه‌سازی زنده", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetMatch(match.id) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_simulation_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تنظیم مجدد", color = Color.White)
                        }
                    }
                }
            }
        }

        // Live drift trend graphical representor
        if (prediction != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("خط انحراف احتمالات (Pre vs Live Probability Drift)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        DriftTimelineGraph(prediction.preLiveDriftJson)
                    }
                }
            }
        }

        // Multi-Source Data Quality checklist
        item {
            Text("اتصال یکپارچه کلیدهای پردازش ابری داده (Multi-Source Hub)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(syncMetrics) { metric ->
            SourceSyncMetricRow(metric)
        }
    }
}

@Composable
fun DriftTimelineGraph(driftJson: String) {
    val pointsList = remember(driftJson) {
        val list = mutableListOf<DriftPoint>()
        try {
            val arr = JSONArray(driftJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DriftPoint(
                        min = obj.getInt("min"),
                        h = obj.getInt("h"),
                        d = obj.getInt("d"),
                        a = obj.getInt("a")
                    )
                )
            }
        } catch (e: Exception) {
            // Safe fallback defaults
            list.add(DriftPoint(0, 45, 25, 30))
        }
        list
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        val width = size.width
        val height = size.height

        if (pointsList.size < 2) {
            drawLine(
                color = NeonGreen,
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.5f),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        val stepX = width / (pointsList.size - 1)
        val hPointsPath = Path()
        val aPointsPath = Path()

        pointsList.forEachIndexed { index, driftPoint ->
            val curX = index * stepX

            // Home probabilities drift
            val homeY = height - (driftPoint.h / 100f * height)
            if (index == 0) hPointsPath.moveTo(curX, homeY)
            else hPointsPath.lineTo(curX, homeY)

            // Away probabilities drift
            val awayY = height - (driftPoint.a / 100f * height)
            if (index == 0) aPointsPath.moveTo(curX, awayY)
            else aPointsPath.lineTo(curX, awayY)
        }

        drawPath(path = hPointsPath, color = NeonGreen, style = Stroke(width = 2.dp.toPx()))
        drawPath(path = aPointsPath, color = HighlightYellow, style = Stroke(width = 2.dp.toPx()))
    }
}

data class DriftPoint(
    val min: Int,
    val h: Int,
    val d: Int,
    val a: Int
)

@Composable
fun SourceSyncMetricRow(metric: com.example.ui.viewmodel.SourceSyncState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (metric.status == "متصل" || metric.status == "پشتیبان زنده") NeonGreen else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )

                Column {
                    Text(metric.sourceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("تاخیر پینگ: ${metric.pingMs}ms | ضریب هماهنگی: ${metric.confidence}%", color = Color.Gray, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (metric.status == "متصل" || metric.status == "پشتیبان زنده") NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = metric.status,
                    color = if (metric.status == "متصل" || metric.status == "پشتیبان زنده") NeonGreen else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
