package com.example.studyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                KMAStudyApp(this, flashcardRepo, noteRepo, todoRepo, userActivityRepo)
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

    Scaffold(
        containerColor = ScBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ScSurfaceContainerLowest,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    bottomItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                BadgedBox(badge = {
                                    if (screen == Screen.Todo && pendingCount > 0) {
                                        Badge(containerColor = DangerRed) {
                                            Text("$pendingCount", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }) {
                                    Icon(
                                        if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.label
                                    )
                                }
                            },
                            label = {
                                Text(screen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ScPrimary,
                                selectedTextColor = ScPrimary,
                                unselectedIconColor = ScOnSurfaceVariant,
                                unselectedTextColor = ScOnSurfaceVariant,
                                indicatorColor = ScPrimaryContainer
                            )
                        )
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
                    onDeckClick = { deckId -> navController.navigate("deck_detail/$deckId") }
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
}