package com.example.studyapp.ui.screen

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studyapp.data.model.TodoItem
import com.example.studyapp.notification.NotificationHelper
import com.example.studyapp.notification.TodoReminderWorker
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.util.*
import com.example.studyapp.ui.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ───────────────────────────────────────────────────────────────────

fun Long.toCalendarDay(): Triple<Int, Int, Int> {
    val c = Calendar.getInstance().apply { timeInMillis = this@toCalendarDay }
    return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
}

fun calendarDayOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel, context: Context) {
    val todos by viewModel.allTodos.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var calendarMode by remember { mutableStateOf(CalendarMode.MONTH) }
    var selectedDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val today = remember {
        Calendar.getInstance().let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
        }
    }

    val todosByDay = remember(todos) {
        todos.filter { it.dueDate != null }.groupBy { it.dueDate!!.toCalendarDay() }
    }

    val filteredTodos = remember(todos, selectedDate) {
        if (selectedDate == null) todos
        else todos.filter { it.dueDate?.toCalendarDay() == selectedDate }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScSurfaceContainerLowest)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Công việc", style = MaterialTheme.typography.headlineLarge,
                            color = ScOnSurface, fontWeight = FontWeight.Bold)
                        Text("$pendingCount việc đang chờ",
                            style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                    }
                    // Test notification button
                    IconButton(onClick = { NotificationHelper.sendTestNotification(context) }) {
                        Icon(Icons.Default.NotificationsActive, null,
                            tint = ScPrimary, modifier = Modifier.size(22.dp))
                    }
                    // Mode toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ScBackground,
                        border = BorderStroke(1.dp, ScOutlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            CalendarMode.values().forEach { mode ->
                                val selected = calendarMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selected) ScPrimary else Color.Transparent)
                                        .clickable { calendarMode = mode; selectedDate = null }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else ScOnSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Divider(color = ScOutlineVariant, thickness = 1.dp)

            // ── Calendar ──
            Surface(color = ScSurfaceContainerLowest) {
                when (calendarMode) {
                    CalendarMode.MONTH -> MonthCalendarView(todosByDay, selectedDate, today) { d ->
                        selectedDate = if (selectedDate == d) null else d
                    }
                    CalendarMode.WEEK -> WeekCalendarView(todosByDay, selectedDate, today) { d ->
                        selectedDate = if (selectedDate == d) null else d
                    }
                }
            }
            Divider(color = ScOutlineVariant, thickness = 1.dp)

            // ── Selected date chip ──
            AnimatedVisibility(visible = selectedDate != null) {
                selectedDate?.let { (y, m, d) ->
                    val label = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))
                        .format(Date(calendarDayOf(y, m, d)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScBackground)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = ScPrimaryContainer) {
                            Text(label, color = ScPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { selectedDate = null },
                            contentPadding = PaddingValues(0.dp)) {
                            Text("Xem tất cả", color = ScOutline,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── List ──
            val pending = filteredTodos.filter { !it.isCompleted }
            val completed = filteredTodos.filter { it.isCompleted }

            if (filteredTodos.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(ScBackground),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null,
                                Modifier.size(36.dp), tint = ScPrimary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (selectedDate != null) "Không có việc ngày này"
                            else "Không có việc gì!",
                            color = ScOnSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Nhấn + để thêm công việc",
                            color = ScOutline, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).background(ScBackground),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item { ScSectionLabel("Cần làm", pending.size) }
                        items(pending, key = { it.id }) { todo ->
                            TodoItemCard(todo,
                                onToggle = { viewModel.toggleComplete(todo) },
                                onEdit = { editingTodo = todo },
                                onDelete = { viewModel.deleteTodo(todo) })
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            ScSectionLabel("Đã hoàn thành", completed.size)
                        }
                        items(completed, key = { it.id }) { todo ->
                            TodoItemCard(todo,
                                onToggle = { viewModel.toggleComplete(todo) },
                                onEdit = { editingTodo = todo },
                                onDelete = { viewModel.deleteTodo(todo) })
                        }
                        item {
                            TextButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Xóa tất cả đã hoàn thành (${completed.size})",
                                    color = ScOutline,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = ScPrimary, contentColor = Color.White, shape = CircleShape
        ) { Icon(Icons.Default.Add, "Thêm việc") }
    }

    if (showAddDialog) {
        TodoAddDialog(
            preselectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onAdd = { title, priority, dueDate ->
                viewModel.addTodo(title, priority, dueDate)
                if (dueDate != null) {
                    TodoReminderWorker.schedule(context, System.currentTimeMillis(), title, dueDate)
                }
                showAddDialog = false
            }
        )
    }
    editingTodo?.let { todo ->
        TodoEditDialog(todo,
            onDismiss = { editingTodo = null },
            onSave = { updated ->
                viewModel.updateTodo(updated)
                // Reschedule if due date changed
                if (updated.dueDate != null) {
                    TodoReminderWorker.schedule(context, updated.id, updated.title, updated.dueDate)
                } else {
                    TodoReminderWorker.cancel(context, updated.id)
                }
                editingTodo = null
            })
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Dọn dẹp công việc?",
            message = "Bạn có muốn xóa tất cả ${todos.count { it.isCompleted }} công việc đã hoàn thành không?",
            iconPath = "sticky-note.png",
            confirmText = "Dọn dẹp ngay",
            onDismiss = { showClearConfirm = false },
            onConfirm = { viewModel.clearCompleted(); showClearConfirm = false }
        )
    }
}

enum class CalendarMode(val label: String) { MONTH("Tháng"), WEEK("Tuần") }

@Composable
fun ScSectionLabel(text: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge,
            color = ScOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Surface(shape = CircleShape, color = ScSurfaceContainerLow) {
            Text("$count", style = MaterialTheme.typography.labelSmall,
                color = ScOutline,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}

// ── Month Calendar ────────────────────────────────────────────────────────────

@Composable
fun MonthCalendarView(
    todosByDay: Map<Triple<Int, Int, Int>, List<TodoItem>>,
    selectedDate: Triple<Int, Int, Int>?,
    today: Triple<Int, Int, Int>,
    onDateSelected: (Triple<Int, Int, Int>) -> Unit
) {
    var displayCal by remember { mutableStateOf(Calendar.getInstance()) }
    val year = displayCal.get(Calendar.YEAR)
    val month = displayCal.get(Calendar.MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("vi")).format(displayCal.time)
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK)
    val offset = (firstDow + 5) % 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                displayCal = (displayCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) { Icon(Icons.Default.ChevronLeft, null, tint = ScOnSurfaceVariant) }
            Text(monthName, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = ScOnSurface,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            IconButton(onClick = {
                displayCal = (displayCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) { Icon(Icons.Default.ChevronRight, null, tint = ScOnSurfaceVariant) }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("T2","T3","T4","T5","T6","T7","CN").forEach { d ->
                Text(d, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = ScOutline,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        val rows = ((offset + daysInMonth) + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - offset + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val key = Triple(year, month, day)
                        CalendarDayCell(day, key == today, key == selectedDate,
                            todosByDay[key] ?: emptyList(), Modifier.weight(1f)) {
                            onDateSelected(key)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Week Calendar ─────────────────────────────────────────────────────────────

@Composable
fun WeekCalendarView(
    todosByDay: Map<Triple<Int, Int, Int>, List<TodoItem>>,
    selectedDate: Triple<Int, Int, Int>?,
    today: Triple<Int, Int, Int>,
    onDateSelected: (Triple<Int, Int, Int>) -> Unit
) {
    var weekStart by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        })
    }
    val weekLabel = remember(weekStart) {
        val end = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val fmt = SimpleDateFormat("dd/MM", Locale("vi"))
        "${fmt.format(weekStart.time)} – ${fmt.format(end.time)}"
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                weekStart = (weekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }
            }) { Icon(Icons.Default.ChevronLeft, null, tint = ScOnSurfaceVariant) }
            Text("Tuần $weekLabel", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = ScOnSurface,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            IconButton(onClick = {
                weekStart = (weekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }
            }) { Icon(Icons.Default.ChevronRight, null, tint = ScOnSurfaceVariant) }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("T2","T3","T4","T5","T6","T7","CN").forEach { d ->
                Text(d, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = ScOutline,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val dayCal = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                val key = Triple(dayCal.get(Calendar.YEAR), dayCal.get(Calendar.MONTH),
                    dayCal.get(Calendar.DAY_OF_MONTH))
                CalendarDayCell(dayCal.get(Calendar.DAY_OF_MONTH), key == today,
                    key == selectedDate, todosByDay[key] ?: emptyList(), Modifier.weight(1f)) {
                    onDateSelected(key)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Calendar Day Cell ─────────────────────────────────────────────────────────

@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    todos: List<TodoItem>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pending = todos.filter { !it.isCompleted }
    val done = todos.filter { it.isCompleted }
    val hasHigh = pending.any { it.priority == 2 }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> ScPrimary
                    isToday -> ScPrimaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isSelected -> Color.White
                isToday -> ScPrimary
                else -> ScOnSurface
            },
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
        if (todos.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (pending.isNotEmpty()) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(0.8f)
                        else if (hasHigh) ScError else ScPrimary))
                }
                if (done.isNotEmpty()) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(0.6f) else ScPrimary))
                }
                if (todos.size > 2) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(0.5f) else ScWarning))
                }
            }
        }
    }
}

// ── Todo Item Card ────────────────────────────────────────────────────────────

@Composable
fun TodoItemCard(
    todo: TodoItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (todo.priority) {
        2 -> ScError; 1 -> ScWarning; else -> ScPrimaryFixedDim
    }
    val priorityBg = when (todo.priority) {
        2 -> ScErrorContainer; 1 -> ScWarningLight; else -> ScPrimaryContainer
    }
    val isOverdue = todo.dueDate != null && !todo.isCompleted &&
            todo.dueDate < System.currentTimeMillis()
    val dueDateStr = todo.dueDate?.let {
        SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale("vi")).format(Date(it))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (todo.isCompleted) ScBackground else ScSurfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            when {
                isOverdue -> ScError.copy(0.3f)
                todo.isCompleted -> ScOutlineVariant.copy(0.5f)
                else -> ScOutlineVariant
            }
        ),
        shadowElevation = if (todo.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (todo.isCompleted) ScPrimary else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (todo.isCompleted) ScPrimary else priorityColor.copy(0.6f),
                        CircleShape
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isCompleted) {
                    Icon(Icons.Default.Check, null,
                        tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (todo.isCompleted) ScOutline else ScOnSurface,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if ((dueDateStr != null || todo.priority > 0) && !todo.isCompleted) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (dueDateStr != null) {
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = if (isOverdue) ScErrorContainer else ScBackground,
                                border = BorderStroke(1.dp, if (isOverdue) ScError.copy(0.3f) else ScOutlineVariant)) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isOverdue) Icons.Default.Warning
                                        else Icons.Default.CalendarToday,
                                        null,
                                        tint = if (isOverdue) ScError else ScOutline,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(dueDateStr, style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue) ScError else ScOutline,
                                        fontSize = 10.sp)
                                }
                            }
                        }
                        if (todo.priority > 0) {
                            Surface(shape = RoundedCornerShape(6.dp), color = priorityBg) {
                                Text(
                                    if (todo.priority == 2) "Cao" else "TB",
                                    color = priorityColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold, fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!todo.isCompleted) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, null,
                        tint = ScOutline, modifier = Modifier.size(15.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, null,
                    tint = ScError.copy(0.5f), modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ── Add Dialog ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAddDialog(
    preselectedDate: Triple<Int, Int, Int>?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var selectedDateMillis by remember {
        mutableStateOf(preselectedDate?.let { (y, m, d) -> calendarDayOf(y, m, d) })
    }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    var enableReminder by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = true
    )

    // Combine date + time into final dueDate
    val finalDueDate = selectedDateMillis?.let { dateMs ->
        Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Chọn", color = ScPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = ScOnSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = ScSurfaceContainerLowest)
        ) {
            DatePicker(state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ScSurfaceContainerLowest,
                    titleContentColor = ScOnSurface,
                    headlineContentColor = ScOnSurface,
                    weekdayContentColor = ScOnSurfaceVariant,
                    dayContentColor = ScOnSurface,
                    selectedDayContainerColor = ScPrimary,
                    todayDateBorderColor = ScPrimary,
                    todayContentColor = ScPrimary
                ))
        }
        return
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest,
                shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chọn giờ", style = MaterialTheme.typography.titleMedium,
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = ScSurfaceContainerLow,
                            clockDialSelectedContentColor = ScOnPrimary,
                            clockDialUnselectedContentColor = ScOnSurface,
                            selectorColor = ScPrimary,
                            containerColor = ScSurfaceContainerLowest,
                            timeSelectorSelectedContainerColor = ScPrimaryContainer,
                            timeSelectorUnselectedContainerColor = ScSurfaceContainerLow,
                            timeSelectorSelectedContentColor = ScPrimary,
                            timeSelectorUnselectedContentColor = ScOnSurfaceVariant
                        ))
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            showTimePicker = false
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                        ) { Text("Xác nhận", color = ScOnPrimary) }
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with gradient and icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScPrimaryContainer.copy(0.4f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = ScPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    loadAssetImage("sticky-note.png"),
                                    null,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Công việc mới",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ScOnSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Lên kế hoạch cho mục tiêu của bạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    // Input Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Tên công việc...", color = ScOutline) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScPrimary,
                            unfocusedBorderColor = ScOutlineVariant,
                            focusedContainerColor = ScSurfaceContainerLowest,
                            unfocusedContainerColor = ScSurfaceContainerLow
                        ),
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(20.dp))

                    // Priority Selection
                    Text(
                        "Mức độ ưu tiên",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScOnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(0, "Thường", ScPrimary),
                            Triple(1, "Trung bình", ScWarning),
                            Triple(2, "Quan trọng", ScError)
                        ).forEach { (p, label, color) ->
                            val isSelected = priority == p
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { priority = p },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color.copy(alpha = 0.15f) else ScSurfaceContainerLow,
                                border = if (isSelected) BorderStroke(1.5.dp, color) else null
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) color else ScOnSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Date & Time Selection
                    Text(
                        "Thời hạn & Nhắc nhở",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScOnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Date Button
                        Surface(
                            modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = ScSurfaceContainerLow,
                            border = if (selectedDateMillis != null) BorderStroke(1.dp, ScPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(loadAssetImage("clock.png"), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    selectedDateMillis?.let {
                                        SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(it))
                                    } ?: "Chọn ngày",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedDateMillis != null) ScPrimary else ScOnSurfaceVariant
                                )
                            }
                        }

                        // Time Button
                        Surface(
                            modifier = Modifier.weight(0.8f).clickable { showTimePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = ScSurfaceContainerLow,
                            border = if (selectedDateMillis != null) BorderStroke(1.dp, ScSecondary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    String.format("%02d:%02d", selectedHour, selectedMinute),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedDateMillis != null) ScSecondary else ScOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Hủy", color = ScOnSurfaceVariant)
                        }
                        Button(
                            onClick = { if (title.isNotBlank()) onAdd(title.trim(), priority, finalDueDate) },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.weight(1.5f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                        ) {
                            Image(loadAssetImage("sparkles.png"), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tạo task", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// expose reminder flag to caller via wrapper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAddDialogWithReminder(
    preselectedDate: Triple<Int, Int, Int>?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Long?, Boolean) -> Unit
) = TodoAddDialog(preselectedDate, onDismiss) { title, priority, dueDate ->
    onAdd(title, priority, dueDate, false)
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditDialog(todo: TodoItem, onDismiss: () -> Unit, onSave: (TodoItem) -> Unit) {
    var title by remember { mutableStateOf(todo.title) }
    var priority by remember { mutableStateOf(todo.priority) }
    var selectedDateMillis by remember { mutableStateOf(todo.dueDate) }
    var selectedHour by remember {
        mutableStateOf(todo.dueDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
        } ?: 8)
    }
    var selectedMinute by remember {
        mutableStateOf(todo.dueDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
        } ?: 0)
    }
    var enableReminder by remember { mutableStateOf(todo.dueDate != null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = true
    )

    val finalDueDate = selectedDateMillis?.let { dateMs ->
        Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Chọn", color = ScPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = ScOnSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = ScSurfaceContainerLowest)
        ) {
            DatePicker(state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ScSurfaceContainerLowest,
                    selectedDayContainerColor = ScPrimary,
                    todayDateBorderColor = ScPrimary,
                    todayContentColor = ScPrimary
                ))
        }
        return
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest,
                shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chọn giờ", style = MaterialTheme.typography.titleMedium,
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = ScSurfaceContainerLow,
                            selectorColor = ScPrimary,
                            timeSelectorSelectedContainerColor = ScPrimaryContainer,
                            timeSelectorSelectedContentColor = ScPrimary
                        ))
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            showTimePicker = false
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                        ) { Text("Xác nhận", color = ScOnPrimary) }
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(
                        listOf(ScTertiaryContainer.copy(0.4f), ScSurfaceContainerLowest)),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(ScTertiaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, null, tint = ScTertiary,
                                modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Chỉnh sửa công việc",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            Text("Cập nhật thông tin công việc",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    ScTextField(value = title, onValueChange = { title = it },
                        label = "Nội dung", maxLines = 3)
                    Spacer(Modifier.height(16.dp))
                    Text("Thời hạn", style = MaterialTheme.typography.labelMedium,
                        color = ScOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp,
                                if (selectedDateMillis != null) ScPrimary.copy(0.5f) else ScOutlineVariant),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null,
                                tint = if (selectedDateMillis != null) ScPrimary else ScOnSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedDateMillis?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(it))
                            } ?: "Chọn ngày",
                                color = if (selectedDateMillis != null) ScPrimary else ScOnSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        AnimatedVisibility(visible = selectedDateMillis != null) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ScSecondary.copy(0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null,
                                    tint = ScSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(String.format("%02d:%02d", selectedHour, selectedMinute),
                                    color = ScSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (selectedDateMillis != null) {
                            IconButton(onClick = { selectedDateMillis = null },
                                modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Clear, null,
                                    tint = ScOutline, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    AnimatedVisibility(visible = selectedDateMillis != null) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = if (enableReminder) ScPrimaryContainer.copy(0.3f)
                                        else ScSurfaceContainerLow,
                                border = BorderStroke(1.dp,
                                    if (enableReminder) ScPrimary.copy(0.3f) else ScOutlineVariant)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .clickable { enableReminder = !enableReminder }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, null,
                                        tint = if (enableReminder) ScPrimary else ScOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Nhắc nhở trước 30 phút",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (enableReminder) ScPrimary else ScOnSurface,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("Gửi thông báo trước khi đến hạn",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ScOnSurfaceVariant)
                                    }
                                    Switch(checked = enableReminder,
                                        onCheckedChange = { enableReminder = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ScOnPrimary,
                                            checkedTrackColor = ScPrimary,
                                            uncheckedThumbColor = ScOutline,
                                            uncheckedTrackColor = ScSurfaceContainerHigh
                                        ))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Độ ưu tiên", style = MaterialTheme.typography.labelMedium,
                        color = ScOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(0, "Bình thường", ScPrimaryFixedDim),
                            Triple(1, "Trung bình", ScWarning),
                            Triple(2, "Cao", ScError)
                        ).forEach { (value, label, color) ->
                            FilterChip(
                                selected = priority == value,
                                onClick = { priority = value },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(0.15f),
                                    selectedLabelColor = color,
                                    containerColor = ScBackground,
                                    labelColor = ScOnSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = ScOutlineVariant,
                                    selectedBorderColor = color.copy(0.4f),
                                    borderWidth = 1.dp, selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (title.isNotBlank())
                                    onSave(todo.copy(title = title.trim(), priority = priority,
                                        dueDate = finalDueDate))
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScTertiary)
                        ) {
                            Icon(Icons.Default.Save, null, tint = ScOnTertiary,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu", color = ScOnTertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
