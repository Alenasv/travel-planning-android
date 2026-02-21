package com.example.travel_planning.ui

import Place
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.AnimatedListItem
import com.example.travel_planning.utils.loadJsonListFromAssets
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.example.travel_planning.network.downloadJson
import com.example.travel_planning.utils.loadJsonListFromInternal
import java.io.File

data class ListItem(
    val id: Int,
    val title: String,
    val description: String,
)

data class GalleryItem(
    val category: String,
    val imagePath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddTripClick: () -> Unit,
    onGalleryItemClick: (String) -> Unit,
    repository: TripRepository? = null,
    tripsOverride: List<TripEntity>? = null,
    onEditTripClick: (Long) -> Unit,
    onShareTripClick: (Long) -> Unit,
    onDeleteTrips: (List<Long>) -> Unit,
    onSelectionResetCallback: ((() -> Unit) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val trips: List<TripEntity> = repository?.getAllTrips()
        ?.collectAsState(initial = emptyList())
        ?.value ?: tripsOverride ?: emptyList()

    var isLoading by remember { mutableStateOf(true) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTrips = remember { mutableStateListOf<Long>() }
    var showAddFab by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (tripsOverride == null) {
            try {
                repository?.let {
                   // trips = it.getAllTrips()
                }
            } catch (e: Exception) {
              //  trips = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedTrips.clear()
        isSelectionMode = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        repository?.let {
                           // trips = it.getAllTrips()
                        }
                    } catch (e: Exception) {
                       // trips = emptyList()
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
    var allPlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    LaunchedEffect(Unit) {
        val success = downloadJson(context, "all_places.json")
        allPlaces = if (success) {
            loadJsonListFromInternal(context, "all_places.json")
        } else {
            loadJsonListFromAssets(context, "all_places.json")//убрать
        }
        isLoading = false
    }

    val galleryItems = remember(allPlaces) {
        allPlaces
            .groupBy { it.category }
            .mapNotNull { (category, placesInCategory) ->
                placesInCategory
                    .getOrNull(0)
                    ?.image_filename
                    ?.let { image ->
                        GalleryItem(
                            category = category,
                            imagePath = image
                        )
                    }
            }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
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
                            GalleryCard(
                                item = item,
                                onClick = {
                                    onGalleryItemClick(item.category)
                                }
                            )
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
                                    if (!isSelectionMode && shouldSelect) {
                                        isSelectionMode = true
                                    }
                                    if (shouldSelect) {
                                        if (!selectedTrips.contains(trip.id_)) {
                                            selectedTrips.add(trip.id_)
                                        }
                                    } else {
                                        selectedTrips.remove(trip.id_)
                                        if (selectedTrips.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    }
                                },
                                onShareTrip = { onShareTripClick(trip.id_) }
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
fun GalleryCard(item: GalleryItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val localFile = File(context.filesDir, item.imagePath)

    Card(
        onClick = onClick,
        modifier = Modifier.size(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = if (localFile.exists()) localFile else "http://45.150.11.208:8000/static/${item.imagePath}",
                contentDescription = item.category,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = item.category,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
    onSelectTrip: (Boolean) -> Unit,
    onShareTrip: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val shareWidth = 72.dp
    val shareWidthPx = with(density) { shareWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = shareWidthPx / 2

    val isSwiping = remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val targetColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isPressed && !isSwiping.value -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        label = "cardBackground",
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (offsetX.value < -5f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(
                            topEnd = 12.dp,
                            bottomEnd = 12.dp,
                            topStart = 0.dp,
                            bottomStart = 0.dp
                        )
                    )
                    .padding(end = 16.dp)
                    .clickable(
                        enabled = !isSelectionMode && offsetX.value < -shareWidthPx / 2,
                        onClick = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                                onShareTrip()
                            }
                        }
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(shareWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(isSelectionMode) {
                    if (!isSelectionMode) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isSwiping.value = true
                                isPressed = false
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (abs(offsetX.value) > swipeThreshold) {
                                        offsetX.animateTo(-shareWidthPx)
                                    } else {
                                        offsetX.animateTo(0f)
                                    }
                                }
                                isSwiping.value = false
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-shareWidthPx, 0f)
                                coroutineScope.launch {
                                    offsetX.snapTo(newOffset)
                                }
                            }
                        )
                    }
                }
                .pointerInput(isSelectionMode, isSwiping.value, offsetX.value) {
                    if (!isSelectionMode && !isSwiping.value && offsetX.value == 0f) {
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
                    }
                }
                .then(
                    if (isSelectionMode) {
                        Modifier.clickable(
                            onClick = {
                                onSelectTrip(!isSelected)
                            }
                        )
                    } else {
                        Modifier
                    }
                ),
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
                Box(
                    modifier = Modifier.width(checkboxWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode) {
            offsetX.animateTo(0f)
        }
    }

    LaunchedEffect(offsetX.value) {
        if (offsetX.value > 0) {
            offsetX.animateTo(0f)
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
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = showAddFab && !isSelectionMode,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ),
            exit = fadeOut(animationSpec = tween(300)) +
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ),
            exit = fadeOut(animationSpec = tween(300)) +
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = onDeleteClick,
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить выбранные")
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
            onSelectionResetCallback = null,
            onGalleryItemClick = {},
            onShareTripClick = {}
        )
    }
}