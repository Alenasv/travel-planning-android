
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.remember

import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travel_planning.ui.Trip


data class Place(
    val id: String,
    val name: String,
    val address: String,
    val work_time: String,
    val category: String,
    val description: String,
    val image_filename: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToRouteScreen(
    tripId: Long,
    selectedPlacesIds: Set<String>,
    places: List<Place>,
    onBackClick: () -> Unit,
    onSaveTrip: (List<Place>) -> Unit,
    onPlaceClick: (Place) -> Unit
) {
    val selectedPlaces = remember { mutableStateListOf<Place>() }

    LaunchedEffect(selectedPlacesIds) {
        selectedPlaces.clear()
        selectedPlaces.addAll(places.filter { it.id in selectedPlacesIds })
    }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text(
                        "Выбери места",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val selected = places.filter { place -> selectedPlaces.any { it.id == place.id } }
                            onSaveTrip(selected)
                        },
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Сохранить",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Рекомендуемые места",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(places) { place ->
                    val isSelected = selectedPlaces.any { it.id == place.id }
                    PlaceCard(
                        place = place,
                        initiallySelected = isSelected,
                        onToggleSelect = { toggled ->
                            if (toggled) {
                                if (selectedPlaces.none { it.id == place.id }) {
                                    selectedPlaces.add(place)
                                }
                            } else {
                                selectedPlaces.removeAll { it.id == place.id }
                            }
                        },
                        onPlaceClick = { onPlaceClick(place) }
                    )
                }
                }
        }
    }
}

@Composable
fun PlaceCard(
    place: Place,
    initiallySelected: Boolean = false,
    onToggleSelect: (Boolean) -> Unit,
    onPlaceClick: () -> Unit
) {
    var isSelected by remember { mutableStateOf(initiallySelected) }
    LaunchedEffect(initiallySelected) {
        isSelected = initiallySelected
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(20.dp),
        onClick = onPlaceClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    model = "file:///android_asset/${place.image_filename}"
                )
                Image(
                    painter = painter,
                    contentDescription = place.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AdaptiveTitleText(place.name)
                    Text(
                        text = place.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedButton(
                    onClick = {
                        isSelected = !isSelected
                        onToggleSelect(isSelected)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 0.9.dp)
                        .height(36.dp),
                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isSelected) "Убрать" else "Добавить",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AdaptiveTitleText(
    text: String,
    minFontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val baseFontSize = MaterialTheme.typography.titleSmall.fontSize
    var fontSize by remember { mutableStateOf(baseFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontSize = fontSize),
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        onTextLayout = { result ->
            if (!readyToDraw && result.hasVisualOverflow) {
                if (fontSize > minFontSize) {
                    fontSize *= 0.9f
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
        modifier = Modifier.padding(bottom = 4.dp)
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewAddToRouteScreen() {
    val samplePlaces = listOf(
        Place("1", "Эрмитаж", "адрес", "Музей", "описание", "", ""),
        Place("2", "Петропавловская крепость", "адрес", "История", "описание", "", ""),
        Place("3", "Исаакиевский собор", "адрес", "Архитектура", "описание", "", "")
    )
    MaterialTheme {
        AddToRouteScreen(
            places = samplePlaces,
            onBackClick = {},
            onSaveTrip = { selectedPlaces: List<Place> ->
            },
            onPlaceClick = {},
            tripId = 1L,
            selectedPlacesIds = setOf("1", "3"),
        )
    }
}
