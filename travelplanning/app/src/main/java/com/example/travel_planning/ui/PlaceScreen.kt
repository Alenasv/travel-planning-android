import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
    onPlaceClick: (Place,Boolean) -> Unit,
    onTogglePlace: (String, Boolean) -> Unit
) {
    val allCategories = remember(places) {
        listOf("Все") + places.map { it.category }.distinct()
    }

    var selectedCategory by remember { mutableStateOf("Все") }

    val filteredPlaces = remember(places, selectedCategory) {
        if (selectedCategory == "Все") {
            places
        } else {
            places.filter { it.category == selectedCategory }
        }
    }

    BackHandler() {
        onBackClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                                val selected = places.filter { it.id in selectedPlacesIds }
                                onSaveTrip(selected)
                            }
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
            ) {
                CategoryFilter(
                    categories = allCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category -> selectedCategory = category },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(filteredPlaces) { place ->
                        val isSelected = place.id in selectedPlacesIds
                        PlaceCard(
                            place = place,
                            isSelected = isSelected,
                            onToggleSelect = { toggled -> onTogglePlace(place.id, toggled) },
                            onPlaceClick = { onPlaceClick(place,isSelected) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        categories.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))
    }
}
@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier
            .height(48.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            labelColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            iconColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = FilterChipDefaults.filterChipBorder(
            selected = isSelected,
            enabled = true,
            borderColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
            borderWidth = 2.dp,
            selectedBorderWidth = 2.dp
        )
    )
}
@Composable
fun PlaceCard(
    place: Place,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onPlaceClick: () -> Unit
) {
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
                if (isSelected) {
                    IconButton(
                        onClick = { onToggleSelect(false) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(15.dp)
                            .size(22.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Убрать",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
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
                    onClick = { onToggleSelect(!isSelected) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = if (isSelected)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isSelected) "Отменить" else "Добавить",
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
        Place("1", "Эрмитаж", "адрес", "10:00-18:00", "Музей", "описание", ""),
        Place("2", "Петропавловская крепость", "адрес", "10:00-18:00", "История", "описание", ""),
        Place("3", "Исаакиевский собор", "адрес", "10:00-18:00", "Архитектура", "описание", ""),
        Place("4", "Кунсткамера", "адрес", "10:00-18:00", "Музей", "описание", ""),
        Place("5", "Русский музей", "адрес", "10:00-18:00", "Музей", "описание", ""),
        Place("6", "Мариинский театр", "адрес", "10:00-18:00", "Театр", "описание", "")
    )
    var selectedIds by remember { mutableStateOf(setOf("1", "3")) }
    MaterialTheme {
        AddToRouteScreen(
            places = samplePlaces,
            onBackClick = {},
            onSaveTrip = { selectedPlaces: List<Place> -> },
            onPlaceClick = {place, isSelected->},
            tripId = 1L,
            selectedPlacesIds = setOf("1", "3"),
            onTogglePlace = { placeId, toggled ->
                selectedIds = if (toggled) selectedIds + placeId else selectedIds - placeId
            },
        )
    }
}