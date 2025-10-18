package com.example.travel_planning.ui

import Place
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    place: Place?,
    onBackClick: () -> Unit,
    onAddToRoute: (Place) -> Unit
) {
    val listState = rememberLazyListState()

    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset < 10 || listState.firstVisibleItemIndex == 0
        }
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
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        val painter = rememberAsyncImagePainter(
                            model = "file:///android_asset/${place.image_filename}"
                        )
                        Image(
                            painter = painter,
                            contentDescription = place.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(place.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(place.category,
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
                                Icon(Icons.Default.LocationOn, contentDescription = "Время работы", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Время работы", style = MaterialTheme.typography.bodySmall)
                                    Text(place.work_time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }

                            Divider(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                            )

                            Text("Описание",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(place.description,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showButton,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddToRoute(place) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp),
                            shape = RoundedCornerShape(35.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Добавить в маршрут", style = MaterialTheme.typography.titleMedium)
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

val place = Place(
    id = "1",
    name = "Эрмитаж",
    category = "Музей",
    address= "адрес",
    description= "описание",
    image_filename = "",
    work_time=""
)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaceDetailScreenPreview() {
    MaterialTheme {
        PlaceDetailScreen(
            place = place,
            onBackClick = { },
            onAddToRoute = { }
        )
    }
}
@Composable
fun PlaceDetailScreenEmptyPreview() {
    MaterialTheme {
        PlaceDetailScreen(
            place = null,
            onBackClick = { },
            onAddToRoute = { }
        )
    }
}