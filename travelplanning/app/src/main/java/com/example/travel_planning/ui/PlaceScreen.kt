import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.travel_planning.network.downloadImage
import com.example.travel_planning.utils.Place
import java.io.File

sealed class GridItem {

    data object AICard : GridItem()
    data class PlaceItem(val place: Place) : GridItem()
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToRouteScreen(
    selectedPlacesIds: Set<String>,
    places: List<Place>,
    onBackClick: () -> Unit,
    onSaveTrip: (List<Place>) -> Unit,
    onPlaceClick: (Place, Boolean) -> Unit,
    onTogglePlace: (String, Boolean) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAIClick: () -> Unit
)
 {
    val allCategories = remember(places) {
        listOf("Все") + places.map { it.category }.distinct()
    }
    var selectedCategoryState by remember(selectedCategory) {
        mutableStateOf(selectedCategory)
    }

    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    val filteredPlaces = remember(places, selectedCategoryState, searchQuery.text) {
        if (selectedCategoryState == "Все" && searchQuery.text.isBlank()) {
            return@remember places
        }

        var filtered = places

        if (searchQuery.text.isNotBlank()) {
            val query = searchQuery.text.lowercase()
            filtered = filtered.filter { place ->
                place.name.lowercase().contains(query)
            }
        }

        if (selectedCategoryState != "Все") {
            filtered = filtered.filter { it.category == selectedCategoryState }
        }

        filtered
    }

    val searchSuggestions = remember(places, searchQuery.text) {
        if (searchQuery.text.isBlank()) {
            emptyList()
        } else {
            places
                .map { it.name }
                .filter { it.lowercase().contains(searchQuery.text.lowercase()) }
                .distinct()
                .take(5)
        }
    }
     val isInternetAvailable = remember { mutableStateOf(true) }
    fun handleSuggestionClick(suggestion: String) {
        searchQuery = TextFieldValue(suggestion)

        val foundPlace = places.firstOrNull { it.name == suggestion }
        if (foundPlace != null) {
            val placesInSameCategory = places.filter {
                it.category == foundPlace.category &&
                        it.name.lowercase().contains(suggestion.lowercase())
            }

            if (placesInSameCategory.size == 1) {
                selectedCategoryState = foundPlace.category
                onCategorySelected(foundPlace.category)
            } else {
                selectedCategoryState = "Все"
                onCategorySelected("Все")
            }
        }
    }
     val gridItems = remember(filteredPlaces) {
         buildList {
             add(GridItem.AICard)
             addAll(filteredPlaces.map { GridItem.PlaceItem(it) })
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
                        IconButton(
                            onClick = onBackClick
                        ) {
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
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { newValue ->
                        searchQuery = newValue
                    },
                    onClearClick = {
                        searchQuery = TextFieldValue()
                        selectedCategoryState = "Все"
                        onCategorySelected("Все")
                        focusManager.clearFocus()
                    },
                    onSearch = {
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (searchQuery.text.isNotBlank() && searchSuggestions.isNotEmpty()) {
                    SearchSuggestions(
                        suggestions = searchSuggestions,
                        onSuggestionClick = ::handleSuggestionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                CategoryFilter(
                    categories = allCategories,
                    selectedCategory = selectedCategoryState,
                    onCategorySelected = { category ->
                        selectedCategoryState = category
                        onCategorySelected(category)
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (searchQuery.text.isNotBlank() || selectedCategoryState != "Все") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Найдено: ${filteredPlaces.size} мест",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.text.isNotBlank() && selectedCategoryState != "Все") {
                            TextButton(
                                onClick = {
                                    selectedCategoryState = "Все"
                                    onCategorySelected("Все")
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Показать все категории",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (filteredPlaces.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Ничего не найдено",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val message = when {
                            searchQuery.text.isNotBlank() && selectedCategoryState != "Все" -> {
                                "По запросу \"${searchQuery.text}\" в категории \"$selectedCategoryState\" ничего не найдено"
                            }
                            searchQuery.text.isNotBlank() -> {
                                "По запросу \"${searchQuery.text}\" ничего не найдено"
                            }
                            selectedCategoryState != "Все" -> {
                                "В категории \"$selectedCategoryState\" нет мест"
                            }
                            else -> {
                                "Места не найдены"
                            }
                        }

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            if (searchQuery.text.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        searchQuery = TextFieldValue()
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Text("Очистить поиск")
                                }
                            }

                            if (selectedCategoryState != "Все") {
                                TextButton(
                                    onClick = {
                                        selectedCategoryState = "Все"
                                        onCategorySelected("Все")
                                    }
                                ) {
                                    Text("Показать все категории")
                                }
                            }

                            if (searchQuery.text.isNotBlank() && selectedCategoryState != "Все") {
                                TextButton(
                                    onClick = {
                                        searchQuery = TextFieldValue()
                                        selectedCategoryState = "Все"
                                        onCategorySelected("Все")
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Text("Сбросить все фильтры")
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = gridItems,
                            key = {
                                when (it) {
                                    is GridItem.AICard -> "ai_card"
                                    is GridItem.PlaceItem -> it.place.id
                                    else -> it.hashCode().toString()
                                }
                            }
                        ) { item ->
                            when (item) {

                                is GridItem.AICard -> {
                                    AIPreferenceCard(
                                        onClick = onAIClick,
                                        isOnline = isInternetAvailable.value
                                    )
                                }

                                is GridItem.PlaceItem -> {
                                    val place = item.place
                                    val isSelected = place.id in selectedPlacesIds

                                    PlaceCard(
                                        place = place,
                                        isSelected = isSelected,
                                        onToggleSelect = { toggled ->
                                            onTogglePlace(place.id, toggled)
                                        },
                                        onPlaceClick = {
                                            onPlaceClick(place, isSelected)
                                        }
                                    )
                                }

                                else -> {
                                    Unit
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AIPreferenceCard(
    onClick: () -> Unit,
    isOnline: Boolean
) {
    val infinite = rememberInfiniteTransition(label = "ai")

    val glow by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val scale by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Card(
        onClick = if (isOnline) onClick else ({ }),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isOnline) 1f else 0.55f
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = glow * 0.35f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TypewriterText(
                    text = if (isOnline) " AI создаст маршрут" else "AI недоступен",
                    isActive = isOnline
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (isOnline)
                        "Нажми и получи маршрут"
                    else
                        "Нет интернета",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
@Composable
fun TypewriterText(
    text: String,
    isActive: Boolean
) {
    var visibleText by remember(text, isActive) { mutableStateOf("") }

    LaunchedEffect(text, isActive) {
        if (!isActive) {
            visibleText = text
            return@LaunchedEffect
        }

        visibleText = ""

        text.forEachIndexed { index, _ ->
            visibleText = text.take(index + 1)
            kotlinx.coroutines.delay(45)
        }
    }

    Text(
        text = visibleText,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: TextFieldValue,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onClearClick: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Поиск",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            androidx.compose.material3.TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(53.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        "Поиск мест...",
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                    }
                ),
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SearchSuggestions(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                TextButton(
                    onClick = { onSuggestionClick(suggestion) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }

                if (index < suggestions.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
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
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

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
            containerColor = backgroundColor,
            labelColor = textColor,
            iconColor = textColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            selected = isSelected,
            enabled = true,
            borderColor = backgroundColor,
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
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150)
    )
    val elevation = if (isSelected) 12.dp else 6.dp
    val context = LocalContext.current

    val safeFileName = place.image_filename.hashCode().toString() + ".jpg"
    val localImageFile = File(context.cacheDir, safeFileName)

    var imageModel by remember { mutableStateOf<Any>(
        "http://45.150.11.208:8000/static/${place.image_filename.removePrefix("data/")}"
    ) }

    LaunchedEffect(place.image_filename) {
        if (!localImageFile.exists() && place.image_filename.isNotEmpty()) {
            val downloadedFile = downloadImage(context, place.image_filename)
            if (downloadedFile != null) {
                imageModel = downloadedFile
            }
        } else if (localImageFile.exists()) {
            imageModel = localImageFile
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(20.dp),
        onClick = onPlaceClick,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = place.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Убрать",
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
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
                    SimpleAdaptiveText(place.name)
                    Text(
                        text = place.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleAdaptiveText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAddToRouteScreen() {
    val samplePlaces = listOf(
        Place("1", "Эрмитаж", "адрес", "10:00-18:00", "Музей", "описание", "",emptyList()),
        Place("2", "Петропавловская крепость", "адрес", "10:00-18:00", "История", "описание", "",emptyList()),
        Place("3", "Исаакиевский собор", "адрес", "10:00-18:00", "Архитектура", "описание", "",emptyList()),
        Place("4", "Кунсткамера", "адрес", "10:00-18:00", "Музей", "описание", "",emptyList()),
        Place("5", "Русский музей", "адрес", "10:00-18:00", "Музей", "описание", "",emptyList()),
        Place("6", "Мариинский театр", "адрес", "10:00-18:00", "Театр", "описание", "",emptyList()
        )
    )
    var selectedIds by remember { mutableStateOf(setOf("1", "3")) }
    MaterialTheme {
        AddToRouteScreen(
            places = samplePlaces,
            onBackClick = {},
            onSaveTrip = { selectedPlaces: List<Place> -> },
            onPlaceClick = { place, isSelected -> },
            selectedPlacesIds = setOf("1", "3"),
            onTogglePlace = { placeId, toggled ->
                selectedIds = if (toggled) selectedIds + placeId else selectedIds - placeId
            },
            selectedCategory = "Музей",
            onCategorySelected = {},
            onAIClick = {}
        )
    }
}