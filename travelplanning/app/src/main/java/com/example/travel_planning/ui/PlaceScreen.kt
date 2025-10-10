import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


data class Place(
    val id: Int,
    val title: String,
    val address: String,
    val category: String,
    val description: String
)

data class Trip(
    val id: Int = 0,
    val name: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToRouteScreen(
    onBackClick: () -> Unit,
    onSaveTrip: (Trip) -> Unit,
    onPlaceClick: (Place) -> Unit
) {
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
                            onSaveTrip(Trip())
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(getSamplePlaces()) { place ->
                    PlaceCard(
                        place = place,
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
    onPlaceClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(20.dp),
        onClick = onPlaceClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Картинка",
                    color = Color(0xFF9E9E9E),
                    style = MaterialTheme.typography.bodySmall
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
                    AdaptiveTitleText(place.title)

                    Text(
                        text = place.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedButton(
                    onClick = {
                        // добавить место в определенный день
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
@Composable
fun AdaptiveTitleText(text: String) {
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
                fontSize *= 0.9
            } else {
                readyToDraw = true
            }
        },
        modifier = Modifier.padding(bottom = 4.dp)
    )
}


fun getSamplePlaces(): List<Place> {
    return listOf(
        Place(
            id = 1,
            title = "Эрмитаж",
            category = "Музей",
            address= "адрес",
            description= "описание"
        ),
        Place(
            id = 2,
            title = "Петропавловская крепость",
            category = "История",
            address= "адрес",
            description= "описание"
        ),
        Place(
            id = 3,
            title = "Исаакиевский собор",
            category = "Архитектура",
            address= "адрес",
            description= "описание"
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAddToRouteScreen() {
    MaterialTheme {
        AddToRouteScreen(
            onBackClick = {},
            onSaveTrip = {},
            onPlaceClick = {}
        )
    }
}

@Composable
fun PreviewPlaceCard() {
    MaterialTheme {
        PlaceCard(
            place = Place(1, "Тестовое место", "adres","category","description"),
            onPlaceClick = {}
        )
    }
}