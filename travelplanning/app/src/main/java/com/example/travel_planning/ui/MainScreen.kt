package com.example.travel_planning.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.EditTripActivity
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.theme.TravelPlanningTheme
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
    onEditTripClick: (Long) -> Unit

) {    val lifecycleOwner = LocalLifecycleOwner.current
    var trips by remember { mutableStateOf(tripsOverride ?: listOf()) }
    var isLoading by remember { mutableStateOf(true) }
    var shouldRefresh by remember { mutableStateOf(false) }

    val loadTrips: suspend () -> Unit = {
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

    LaunchedEffect(Unit) {
        if (tripsOverride == null) {
            loadTrips()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleOwner.lifecycleScope.launch {
                    isLoading = true
                    try {
                        repository?.let { trips = it.getAllTrips() }
                    } catch (e: Exception) {
                        trips = emptyList()
                    } finally {
                        isLoading = false
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            FloatingActionButton(

                onClick = onAddTripClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
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
                        ListItemCard(
                            item = ListItem(
                                id = trip.id_.toInt(),
                                title = trip.name,
                                description = trip.notes ?: ""
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            edittrip = {
                                onEditTripClick(trip.id_)
                            },


                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пока нет поездок. Нажмите +, чтобы добавить.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListItemCard(
    item: ListItem,
    modifier: Modifier = Modifier,
    edittrip: () -> Unit

) {
    Card(Modifier
        .combinedClickable(
            onClick = edittrip,
            onLongClick = { },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                )
            }
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
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
            onEditTripClick = {}
        )
    }
}
