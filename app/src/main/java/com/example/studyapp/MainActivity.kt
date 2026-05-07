package com.example.studyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.studyapp.data.db.KMAStudyDatabase
import com.example.studyapp.data.repository.FlashcardRepository
import com.example.studyapp.data.repository.NoteRepository
import com.example.studyapp.data.repository.TodoRepository
import com.example.studyapp.data.repository.UserActivityRepository
import com.example.studyapp.ui.screen.*
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.*

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Trang chủ", Icons.Filled.Home, Icons.Outlined.Home)
    object Flashcard : Screen("flashcard", "Thẻ", Icons.Filled.Style, Icons.Outlined.Style)
    object Note : Screen("note", "Ghi chú", Icons.Filled.Note, Icons.Outlined.Note)
    object Todo : Screen("todo", "Công việc", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
    object Stats : Screen("stats", "Thống kê", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Create notification channel
        com.example.studyapp.notification.NotificationHelper.createChannel(this)

        // Request notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        val db = KMAStudyDatabase.getDatabase(this)
        val flashcardRepo = FlashcardRepository(db.flashcardDeckDao(), db.flashcardDao())
        val noteRepo = NoteRepository(db.noteDao())
        val todoRepo = TodoRepository(db.todoDao())
        val userActivityRepo = UserActivityRepository(db.userActivityDao())

        setContent {
            StudyAppTheme {
                var splashDone by remember { mutableStateOf(false) }
                if (!splashDone) {
                    SplashScreen(onFinished = { splashDone = true })
                } else {
                    KMAStudyApp(this, flashcardRepo, noteRepo, todoRepo, userActivityRepo)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KMAStudyApp(
    context: android.content.Context,
    flashcardRepo: FlashcardRepository,
    noteRepo: NoteRepository,
    todoRepo: TodoRepository,
    userActivityRepo: UserActivityRepository
) {
    val navController = rememberNavController()
    val flashcardViewModel: FlashcardViewModel = viewModel(factory = FlashcardViewModelFactory(flashcardRepo))
    val noteViewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(noteRepo))
    val todoViewModel: TodoViewModel = viewModel(factory = TodoViewModelFactory(todoRepo))
    val userActivityViewModel: UserActivityViewModel = viewModel(factory = UserActivityViewModelFactory(userActivityRepo))

    // Track usage time (attendance + duration)
    LaunchedEffect(Unit) {
        // Attendance: record initial activity
        userActivityViewModel.recordActivity(1000) // 1 second for "checked in"
        
        // Loop to track active time every 30 seconds
        while (true) {
            kotlinx.coroutines.delay(30000)
            userActivityViewModel.recordActivity(30000)
        }
    }

    val bottomItems = listOf(Screen.Home, Screen.Flashcard, Screen.Note, Screen.Todo, Screen.Stats)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    val pendingCount by todoViewModel.pendingCount.collectAsState()

    var showAiChat by remember { mutableStateOf(false) }
    var aiChatEnabled by remember { mutableStateOf(true) }
    var gifShuffleKey by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = ScBackground,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Main Dock Surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = ScSurfaceContainerLowest.copy(alpha = 0.95f),
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomItems.forEachIndexed { index, screen ->
                                val isMiddle = index == 2 // Screen.Note
                                val isSelected = currentRoute == screen.route

                                if (isMiddle) {
                                    Spacer(modifier = Modifier.width(64.dp))
                                } else {
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.15f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "scale_${screen.route}"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(contentAlignment = Alignment.TopEnd) {
                                                Icon(
                                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                                    contentDescription = screen.label,
                                                    tint = if (isSelected) ScPrimary else ScOnSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                                                )
                                                if (screen == Screen.Todo && pendingCount > 0) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .offset(x = 8.dp, y = (-6).dp)
                                                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp),
                                                        shape = RoundedCornerShape(99.dp),
                                                        color = DangerRed
                                                    ) {
                                                        Text(
                                                            text = if (pendingCount > 99) "99+" else "$pendingCount",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (isSelected) {
                                                Spacer(Modifier.height(3.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(ScPrimary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Middle Button (Slightly elevated)
                    val middleScreen = Screen.Note
                    val isMiddleSelected = currentRoute == middleScreen.route
                    val middleScale by animateFloatAsState(
                        targetValue = if (isMiddleSelected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "middleScale"
                    )

                    Surface(
                        onClick = {
                            navController.navigate(middleScreen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(56.dp)
                            .scale(middleScale)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = ScPrimary,
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMiddleSelected) middleScreen.selectedIcon else middleScreen.unselectedIcon,
                                contentDescription = middleScreen.label,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220, easing = EaseOutCubic)) { it / 5 } },
            exitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180, easing = EaseInCubic)) { -it / 5 } },
            popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220, easing = EaseOutCubic)) { -it / 5 } },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180, easing = EaseInCubic)) { it / 5 } }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    flashcardViewModel = flashcardViewModel,
                    noteViewModel = noteViewModel,
                    todoViewModel = todoViewModel,
                    userActivityViewModel = userActivityViewModel,
                    onNavigateToFlashcard = { navController.navigate(Screen.Flashcard.route) },
                    onNavigateToNote = { navController.navigate(Screen.Note.route) },
                    onNavigateToTodo = { navController.navigate(Screen.Todo.route) },
                    onDeckClick = { deckId -> navController.navigate("deck_detail/$deckId") },
                    onOpenAiChat = { showAiChat = true },
                    aiChatEnabled = aiChatEnabled,
                    onToggleAiChat = { aiChatEnabled = it },
                    onShuffleGif = { gifShuffleKey++ }
                )
            }
            composable(Screen.Flashcard.route) {
                FlashcardScreen(
                    viewModel = flashcardViewModel,
                    onDeckClick = { deckId -> navController.navigate("deck_detail/$deckId") }
                )
            }
            composable(
                route = "deck_detail/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStack ->
                val deckId = backStack.arguments?.getLong("deckId") ?: return@composable
                DeckDetailScreen(
                    deckId = deckId,
                    viewModel = flashcardViewModel,
                    onBack = { navController.popBackStack() },
                    onAiGenerate = { navController.navigate("ai_generate/$deckId") }
                )
            }
            composable(
                route = "ai_generate/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStack ->
                val deckId = backStack.arguments?.getLong("deckId") ?: return@composable
                val deck by flashcardViewModel.selectedDeck.collectAsState()
                AiGenerateScreen(
                    deckId = deckId,
                    deckName = deck?.name ?: "",
                    viewModel = flashcardViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Note.route) {
                NoteScreen(viewModel = noteViewModel)
            }
            composable(Screen.Todo.route) {
                TodoScreen(viewModel = todoViewModel, context = context)
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    flashcardViewModel = flashcardViewModel,
                    noteViewModel = noteViewModel,
                    todoViewModel = todoViewModel,
                    userActivityViewModel = userActivityViewModel
                )
            }
        }
    }

    // Floating AI bubble — hiện trên mọi màn hình
    AiChatBubble(forceShow = showAiChat, onShowSheetChange = { showAiChat = it }, isEnabled = aiChatEnabled, gifShuffleKey = gifShuffleKey)
    } // end Box
}