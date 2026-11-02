package com.farhan.dayblock.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farhan.dayblock.data.DiaryEntry
import com.farhan.dayblock.data.Moods
import com.farhan.dayblock.domain.DiaryStatsCalculator
import com.farhan.dayblock.ui.theme.DiamondBlue
import com.farhan.dayblock.ui.theme.DirtBrown
import com.farhan.dayblock.ui.theme.GoldYellow
import com.farhan.dayblock.ui.theme.GrassGreen
import com.farhan.dayblock.ui.theme.GrassLight
import com.farhan.dayblock.ui.theme.StoneGrey
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.Edit),
    Entries("Entries", Icons.Filled.MenuBook),
    Calendar("Calendar", Icons.Filled.CalendarMonth),
    Stats("Stats", Icons.Filled.BarChart)
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private fun formatDate(date: LocalDate): String = date.format(dateFormatter)

private fun formatDate(dateString: String): String =
    runCatching { LocalDate.parse(dateString).format(dateFormatter) }
        .getOrElse { dateString }

/** A chunky, block-style mood selector chip whose colours and size animate. */
@Composable
private fun MoodBlock(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "moodBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "moodForeground"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = tween(200),
        label = "moodScale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scale(scale)
            .background(background, MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (selected) Color.White else color,
                    MaterialTheme.shapes.extraSmall
                )
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = foreground
        )
    }
}

fun moodColor(mood: String): Color = when (mood) {
    Moods.GREAT -> GrassGreen
    Moods.GOOD -> GrassLight
    Moods.OKAY -> GoldYellow
    Moods.LOW -> Color(0xFFEF8F4C)
    Moods.ROUGH -> DirtBrown
    else -> StoneGrey
}

@Composable
private fun PixelBlocks() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(GrassGreen, DirtBrown, StoneGrey, DiamondBlue).forEach { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayblockApp(viewModel: DiaryViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var selectedDateText by rememberSaveable {
        mutableStateOf(LocalDate.now().toString())
    }
    val selectedDate = remember(selectedDateText) {
        runCatching { LocalDate.parse(selectedDateText) }.getOrElse { LocalDate.now() }
    }
    val selectedEntry = entries.firstOrNull { it.date == selectedDateText }
    val stats = remember(entries) {
        DiaryStatsCalculator.calculate(entries, LocalDate.now())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelBlocks()
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dayblock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "One day, one block.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = stats.currentStreak > 0,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    GrassGreen.copy(alpha = 0.15f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(GrassGreen, MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${stats.currentStreak} day streak",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(230)) { width -> if (forward) width / 3 else -width / 3 } +
                        fadeIn(tween(230))) togetherWith
                        (slideOutHorizontally(tween(200)) { width -> if (forward) -width / 3 else width / 3 } +
                            fadeOut(tween(150)))
                },
                label = "tabTransition"
            ) { currentTab ->
                when (currentTab) {
                    Tab.Today -> TodayScreen(
                    entry = selectedEntry,
                    date = selectedDate,
                    onNavigateDate = { selectedDateText = it.toString() },
                    onSave = { title, body, mood, tags ->
                        viewModel.saveEntry(selectedDateText, title, body, mood, tags)
                    },
                    onDelete = { entry ->
                        viewModel.deleteEntry(entry)
                    }
                )

                Tab.Entries -> EntriesScreen(
                    entries = entries,
                    onOpen = { date ->
                        selectedDateText = date.toString()
                        tab = Tab.Today
                    },
                    onToggleStar = { viewModel.toggleStar(it) },
                    onDelete = { viewModel.deleteEntry(it) }
                )

                Tab.Calendar -> CalendarScreen(
                    entries = entries,
                    selectedDate = selectedDate,
                    onSelectDate = { selectedDateText = it.toString() },
                    onOpenEditor = { date ->
                        selectedDateText = date.toString()
                        tab = Tab.Today
                    }
                )

                    Tab.Stats -> StatsScreen(entries = entries)
                }
            }
        }
    }
}

/* ---------------------------------- Today --------------------------------- */

private val prompts = listOf(
    "What was the best small moment of today?",
    "What did you learn today, in or out of school?",
    "Describe today in exactly three sentences.",
    "What is one thing you want tomorrow-you to remember?",
    "Who made your day better, and how?",
    "What match, game or play are you still thinking about?",
    "What challenged you today, and what did you do about it?",
    "Write about a sound, smell or scene from today.",
    "What would you do again from today? What would you skip?",
    "What are you looking forward to this week?"
)

@Composable
private fun TodayScreen(
    entry: DiaryEntry?,
    date: LocalDate,
    onNavigateDate: (LocalDate) -> Unit,
    onSave: (title: String, body: String, mood: String, tags: String) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    // Kept outside key() on purpose: the editor is rebuilt when a brand-new
    // entry gets its database id, and the "Saved" note must survive that.
    var justSaved by remember(date) { mutableStateOf(false) }
    key(date.toString(), entry?.id) {
        EditorContent(
            entry = entry,
            date = date,
            justSaved = justSaved,
            onJustSavedChange = { justSaved = it },
            onNavigateDate = onNavigateDate,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@Composable
private fun EditorContent(
    entry: DiaryEntry?,
    date: LocalDate,
    justSaved: Boolean,
    onJustSavedChange: (Boolean) -> Unit,
    onNavigateDate: (LocalDate) -> Unit,
    onSave: (title: String, body: String, mood: String, tags: String) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var body by remember { mutableStateOf(entry?.body ?: "") }
    var mood by remember { mutableStateOf(entry?.mood ?: Moods.OKAY) }
    var tags by remember { mutableStateOf(entry?.tags ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2_500)
            onJustSavedChange(false)
        }
    }

    val prompt = remember(date) { prompts[date.dayOfYear % prompts.size] }
    val wordCount = remember(body) {
        body.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }
    val isToday = date == LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onNavigateDate(date.minusDays(1)) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                    }
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onNavigateDate(date.plusDays(1)) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                    }
                }
                if (!isToday) {
                    TextButton(
                        onClick = { onNavigateDate(LocalDate.now()) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Back to today")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Prompt: $prompt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "How was the day?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Moods.all.forEach { option ->
                MoodBlock(
                    label = option,
                    color = moodColor(option),
                    selected = mood == option,
                    onClick = { mood = option }
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Dear diary...") },
            placeholder = { Text(prompt) },
            minLines = 10,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags, separated by commas") },
            placeholder = { Text("school, football, family") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount words",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedVisibility(
                visible = justSaved,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Text(
                    text = "Saved on this device",
                    style = MaterialTheme.typography.labelLarge,
                    color = GrassGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(
            onClick = {
                onSave(title, body, mood, tags)
                onJustSavedChange(true)
            },
            enabled = title.isNotBlank() || body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (entry == null) "Place this day's block" else "Save changes")
        }

        if (entry != null) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete this entry")
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showDeleteDialog && entry != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this entry?") },
            text = { Text("This day's entry will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(entry)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep it") }
            }
        )
    }
}

/* --------------------------------- Entries -------------------------------- */

@Composable
private fun EntriesScreen(
    entries: List<DiaryEntry>,
    onOpen: (LocalDate) -> Unit,
    onToggleStar: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var moodFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var starredOnly by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    val filtered = remember(entries, query, moodFilter, starredOnly) {
        entries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                entry.title.contains(query, ignoreCase = true) ||
                entry.body.contains(query, ignoreCase = true) ||
                entry.tags.contains(query, ignoreCase = true)
            val matchesMood = moodFilter == null || entry.mood == moodFilter
            val matchesStar = !starredOnly || entry.starred
            matchesQuery && matchesMood && matchesStar
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search your diary") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = moodFilter == null,
                onClick = { moodFilter = null },
                label = { Text("All moods") }
            )
            Moods.all.forEach { option ->
                FilterChip(
                    selected = moodFilter == option,
                    onClick = { moodFilter = if (moodFilter == option) null else option },
                    label = { Text(option) }
                )
            }
            FilterChip(
                selected = starredOnly,
                onClick = { starredOnly = !starredOnly },
                label = { Text("Starred") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        when {
            entries.isEmpty() -> EmptyState(
                title = "No blocks placed yet",
                message = "Write your first entry from the Today tab and it will appear here."
            )

            filtered.isEmpty() -> EmptyState(
                title = "Nothing matches",
                message = "Try a different search or clear the mood filter."
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    EntryCard(
                        modifier = Modifier.animateItem(),
                        entry = entry,
                        onOpen = {
                            runCatching { LocalDate.parse(entry.date) }
                                .getOrNull()?.let(onOpen)
                        },
                        onToggleStar = { onToggleStar(entry) },
                        onDelete = { pendingDelete = entry }
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this entry?") },
            text = {
                Text(
                    "The entry for ${formatDate(target.date)} will be removed from " +
                        "this device. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(target)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep it") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryCard(
    entry: DiaryEntry,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(moodColor(entry.mood), MaterialTheme.shapes.extraSmall)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDate(entry.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleStar) {
                    Icon(
                        if (entry.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (entry.starred) "Unstar entry" else "Star entry",
                        tint = if (entry.starred) GoldYellow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = entry.title.ifBlank { "Untitled entry" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.tagList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.tagList.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = DirtBrown
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PixelBlocks()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* --------------------------------- Calendar ------------------------------- */

/** The 7-column day cells for [month]; weeks start on Monday. */
private fun monthCells(month: YearMonth): List<LocalDate?> {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val list = mutableListOf<LocalDate?>()
    repeat(offset) { list.add(null) }
    for (day in 1..month.lengthOfMonth()) list.add(month.atDay(day))
    while (list.size % 7 != 0) list.add(null)
    return list
}

@Composable
private fun CalendarScreen(
    entries: List<DiaryEntry>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpenEditor: (LocalDate) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val entryByDate = remember(entries) { entries.associateBy { it.date } }
    val selectedEntry = entryByDate[selectedDate.toString()]

    // The day grid itself is built inside the AnimatedContent below,
    // from whichever month is being shown during the transition.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = month.format(monthFormatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(220)) { width -> if (forward) width / 2 else -width / 2 } +
                    fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(180)) { width -> if (forward) -width / 2 else width / 2 } +
                        fadeOut(tween(140)))
            },
            label = "monthTransition"
        ) { shownMonth ->
            val shownCells = remember(shownMonth) { monthCells(shownMonth) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                shownCells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val hasEntry = entryByDate.containsKey(date.toString())
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val cellBackground by animateColorAsState(
                            targetValue = if (hasEntry) {
                                GrassGreen
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(180),
                            label = "cellBackground"
                        )
                        val cellBorderWidth by animateDpAsState(
                            targetValue = when {
                                isSelected -> 2.dp
                                isToday -> 1.dp
                                else -> 0.dp
                            },
                            animationSpec = tween(180),
                            label = "cellBorderWidth"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    cellBackground,
                                    MaterialTheme.shapes.small
                                )
                                .border(
                                    width = cellBorderWidth,
                                    color = when {
                                        isSelected -> DirtBrown
                                        isToday -> GrassGreen
                                        else -> Color.Transparent
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { onSelectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (hasEntry) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasEntry) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
                }
            }
        }

        Text(
            text = "Green blocks are days you wrote. Tap any day to open it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatDate(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedEntry != null) {
                    Text(
                        text = selectedEntry.title.ifBlank { "Untitled entry" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedEntry.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "No entry for this day yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { onOpenEditor(selectedDate) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedEntry == null) "Write this day" else "Open in editor")
                }
            }
        }

        Text(
            text = "Mood overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        val moodCounts = remember(entries) {
            entries.groupingBy { it.mood }.eachCount()
        }
        if (moodCounts.isEmpty()) {
            Text(
                text = "Your mood pattern will appear here after a few entries.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Moods.all.forEach { mood ->
                val count = moodCounts[mood] ?: 0
                if (count > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(moodColor(mood), MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$mood - $count ${if (count == 1) "day" else "days"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/* ---------------------------------- Stats --------------------------------- */

@Composable
private fun StatsScreen(entries: List<DiaryEntry>) {
    val stats = remember(entries) {
        DiaryStatsCalculator.calculate(entries, LocalDate.now())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = stats.totalEntries.toString(),
                label = "blocks placed",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats.currentStreak.toString(),
                label = "day streak",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = stats.entriesThisWeek.toString(),
                label = "entries this week",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats.totalWords.toString(),
                label = "words written",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Mood breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (stats.totalEntries == 0) {
            Text(
                text = "Write a few entries and your mood pattern will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Moods.all.forEach { mood ->
                val count = stats.moodCounts[mood] ?: 0
                if (count > 0) {
                    val fraction = count.toFloat() / stats.totalEntries.toFloat()
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(650),
                        label = "moodFraction"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(moodColor(mood), MaterialTheme.shapes.extraSmall)
                        )
                        Text(
                            text = mood,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(56.dp)
                        )
                        LinearProgressIndicator(
                            progress = { animatedFraction },
                            modifier = Modifier.weight(1f),
                            color = moodColor(mood)
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Private by design",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Dayblock has no account and no network permission. " +
                        "Your entries live only in this app's private storage on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Coming next: PIN and biometric lock, encrypted backup, and export.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val animatedValue by animateIntAsState(
        targetValue = value.toIntOrNull() ?: 0,
        animationSpec = tween(650),
        label = "statValue"
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GrassGreen
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
