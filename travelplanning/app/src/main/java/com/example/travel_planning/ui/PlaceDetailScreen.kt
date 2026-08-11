package com.example.travel_planning.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.travel_planning.utils.Place
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    place: Place?,
    isSelected: Boolean,
    imageFile: File?,
    onBackClick: () -> Unit,
    onAddToRouteClick: () -> Unit
) {
    val listState = rememberLazyListState()

    val showScrollButton by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset < 10 || listState.firstVisibleItemIndex == 0
        }
    }

    var showEnterAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        showEnterAnimation = true
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = { Text("Подробнее о локации", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (place != null) {
                val imageModel = imageFile ?: "https://via.placeholder.com/600x400.png?text=No+Image"

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = place.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = place.category,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Адрес", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Адрес", style = MaterialTheme.typography.bodySmall)
                                    Text(place.address, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Время работы", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Время работы", style = MaterialTheme.typography.bodySmall)
                                    Text(place.work_time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Text(
                                text = "Описание",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = place.description,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showEnterAnimation && showScrollButton,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(400)
                    ) + androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = androidx.compose.animation.core.tween(400)
                    ),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it / 2 }
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(color = MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddToRouteClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp),
                            shape = RoundedCornerShape(35.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (isSelected) "Убрать из маршрута" else "Добавить в маршрут",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Место не найдено", style = MaterialTheme.typography.bodyLarge)
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

val previewPlace = Place(
    id = "1",
    name = "Эрмитаж",
    category = "Музей",
    address = "г. Санкт-Петербург, Дворцовая наб., 34",
    description = "Один из крупнейших и самых значительных художественных и культурно-исторических музеев России и мира.",
    image_filename = "",
    work_time = "11:00 - 18:00",
    tags = listOf("музеи", "история", "искусство")
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaceDetailScreenPreview() {
    MaterialTheme {
        PlaceDetailScreen(
            place = previewPlace,
            isSelected = true,
            imageFile = null,
            onBackClick = { },
            onAddToRouteClick = { }
        )
    }
}