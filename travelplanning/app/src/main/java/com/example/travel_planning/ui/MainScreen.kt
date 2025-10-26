package com.example.travel_planning.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.AnimatedFAB
import com.example.travel_planning.utils.AnimatedListItem
import kotlinx.coroutines.launch

data class ListItem(
    val id: Int,
    val title: String,
    val description: String,
)

data class GalleryItem(
    val id: Int,
    val color: Color,
    val title: String
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddTripClick: () -> Unit,
    repository: TripRepository? = null,
    tripsOverride: List<TripEntity>? = null,
    onEditTripClick: (Long) -> Unit,
    onDeleteTrips: (List<Long>) -> Unit,
    onSelectionResetCallback: ((() -> Unit) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var trips by remember { mutableStateOf(tripsOverride ?: emptyList<TripEntity>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTrips = remember { mutableStateListOf<Long>() }
    var showAddFab by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onSelectionResetCallback?.invoke {
            selectedTrips.clear()
            isSelectionMode = false
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        showAddFab = true
    }
    LaunchedEffect(tripsOverride) {
        tripsOverride?.let {
            trips = it
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (tripsOverride == null) {
            try {
                repository?.let {
                    trips = it.getAllTrips()
                }
            } catch (e: Exception) {
                trips = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        repository?.let {
                            trips = it.getAllTrips()
                        }
                    } catch (e: Exception) {
                        trips = emptyList()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val handleDeleteClick: () -> Unit = {
        onDeleteTrips(selectedTrips.toList())
    }

    val galleryItems = remember {
        listOf(
            GalleryItem(1, Color(0xFFFF6B6B), "Летний сад"),
            GalleryItem(2, Color(0xFF4ECDC4), "Петропавловская крепость"),
            GalleryItem(3, Color(0xFF45B7D1), "Исаакиевский собор"),
            GalleryItem(4, Color(0xFF96CEB4), "Дворцовая площадь"),
            GalleryItem(5, Color(0xFFF7DC6F), "Крейсер Аврора"),
            GalleryItem(6, Color(0xFFBB8FCE), "Казанский собор")
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        floatingActionButton = {
            MainFABs(
                isSelectionMode = isSelectionMode,
                onAddClick = onAddTripClick,
                showAddFab = showAddFab,
                onDeleteClick = handleDeleteClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(7.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Мгновения Петербурга",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Создай свои маршруты и впечатления.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(galleryItems) { item ->
                            GalleryCard(item = item)
                        }
                    }
                }

                item {
                    Text(
                        text = "Мои записи",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (trips.isNotEmpty()) {
                    items(
                        items = trips,
                        key = { it.id_ }
                    ) { trip ->
                        val selected = selectedTrips.contains(trip.id_)

                        AnimatedListItem(visible = true) {
                            ListItemCard(
                                item = ListItem(trip.id_.toInt(), trip.name, trip.notes ?: ""),
                                isSelected = selected,
                                isSelectionMode = isSelectionMode,
                                onEditTrip = { onEditTripClick(trip.id_) },
                                onSelectTrip = { shouldSelect ->
                                    if (!isSelectionMode) isSelectionMode = true
                                    if (shouldSelect) {
                                        if (!selectedTrips.contains(trip.id_)) selectedTrips.add(trip.id_)
                                    } else {
                                        selectedTrips.remove(trip.id_)
                                        if (selectedTrips.isEmpty()) isSelectionMode = false
                                    }
                                }
                            )
                        }
                    }
                } else if (!isLoading) {
                    item {
                        Text(
                            text = "Пока нет поездок. Нажмите +, чтобы добавить",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
    @Composable
    fun GalleryCard(item: GalleryItem) {
        Card(
            onClick = { /* переход на карточку из гелереи */ },
            modifier = Modifier
                .size(200.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(item.color)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItemCard(
    item: ListItem,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onEditTrip: () -> Unit,
    onSelectTrip: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val targetColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        label = "cardBackground"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onLongPress = {
                        onSelectTrip(true)
                    },
                    onTap = {
                        if (isSelectionMode) {
                            onSelectTrip(!isSelected)
                        } else {
                            onEditTrip()
                        }
                    }

    )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        val checkboxWidth by animateDpAsState(
            targetValue = if (isSelectionMode) 48.dp else 0.dp,
            animationSpec = tween(300)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.width(checkboxWidth), contentAlignment = Alignment.CenterStart) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked -> onSelectTrip(checked) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

    }
}

@Composable
fun MainFABs(
    isSelectionMode: Boolean,
    showAddFab: Boolean,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedFAB(
            visible = showAddFab && !isSelectionMode,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add,
                    contentDescription = "Добавить")
            }
        }

        AnimatedFAB(
            visible = isSelectionMode,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = onDeleteClick,
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Delete,
                    contentDescription = "Удалить выбранные")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TravelPlanningTheme {
        val fakeTrips = listOf(
            TripEntity(id_ = 1, name = "Поездка в Павловск", date = "2025-10-20", notes = "Прогулка по парку"),
            TripEntity(id_ = 2, name = "Летний сад", date = "2025-10-21", notes = "Фото с цветами"),
            TripEntity(id_ = 3, name = "Петропавловская крепость", date = null, notes = null)
        )

        MainScreen(
            onAddTripClick = {},
            tripsOverride = fakeTrips,
            onEditTripClick = {},
            onDeleteTrips = {},
            onSelectionResetCallback = null
        )
    }
}
