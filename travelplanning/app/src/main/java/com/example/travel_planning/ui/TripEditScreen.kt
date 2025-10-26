package com.example.travel_planning.ui

import Place
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.utils.DeleteConfirmationDialog


data class Trip(
    val id: Int = 0,
    val title: String = "",
    val date: String = "",
    val places: List<Place> = emptyList(),
    val notes: String = ""
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(
    trip: Trip? = null,
    tripId: Long? = null,
    repository: TripRepository?=null,
    onBackClick: () -> Unit,
    onSaveTrip: (Trip) -> Unit,
    deleteTrip: () -> Unit,
    onAddPlaceClick: (Trip) -> Unit,
    onRemovePlaceClick: (Long, String) -> Unit,
    onPlaceClick: (Place) -> Unit,

) {
    val currentTrip = trip ?: Trip()
    val isEditing = trip != null
    val places = remember(currentTrip.places) {
        mutableStateListOf<Place>().apply { addAll(currentTrip.places) }
    }

    var title by remember(currentTrip.title) { mutableStateOf(currentTrip.title) }
    var date by remember(currentTrip.date) { mutableStateOf(currentTrip.date) }
    var notes by remember(currentTrip.notes) { mutableStateOf(currentTrip.notes) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    var isTitleError by remember { mutableStateOf(false) }
    var isTouched by remember { mutableStateOf(false) }
    var showSaveError by remember { mutableStateOf(false) }

    val saveTrip = {
        isTouched = true
        isTitleError = title.isEmpty()

        if (title.isNotEmpty()) {
            val updatedTrip = Trip(
                id = trip?.id ?: 0,
                title = title,
                date = date,
                places = places,
                notes = notes
            )
            onSaveTrip(updatedTrip)
            showSaveError = false
        } else {
            showSaveError = true
        }
    }
    BackHandler() {
        onBackClick()
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
                        if (isEditing) "Редактирование" else "Новая поездка",
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
                    Row {
                        if (isEditing) {
                            IconButton(
                                onClick = { deleteTrip()},
                                enabled = title.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = if (title.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = saveTrip,
                            enabled = title.isNotEmpty()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Сохранить",
                                    tint = if (title.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                    }
                                )
                                if (showSaveError) {
                                    Text(
                                        text = "✗",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier.offset(y = (-4).dp)
                                    )
                                }
                            }
                        }
                    }
        }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (title.isEmpty() && isTouched) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Информация",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Сначала введите название",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (title.isNotEmpty()) {
                            val updatedTrip = Trip(
                                id = trip?.id ?: 0,
                                title = title,
                                date = date,
                                places = places,
                                notes = notes
                            )
                            onAddPlaceClick(updatedTrip)
                        } else {
                            isTouched = true
                            isTitleError = true
                            showSaveError = true
                        }
                    },
                    containerColor = if (title.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (title.isNotEmpty()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить место",
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Название поездки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(
                    visible = isTitleError && isTouched,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Ошибка",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Обязательно",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = "*",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (isTouched) {
                        isTitleError = it.isEmpty()
                        showSaveError = it.isEmpty()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Введите название поездки",
                        color = if (isTitleError && isTouched) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                },
                isError = isTitleError && isTouched,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isTitleError && isTouched) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    unfocusedContainerColor = if (isTitleError && isTouched) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
                supportingText = {
                    if (isTitleError && isTouched) {
                        Text(
                            text = "Введите название поездки для сохранения",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Название будет отображаться в списке поездок",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = if (isTitleError && isTouched) {
                    RoundedCornerShape(12.dp)
                } else {
                    RoundedCornerShape(8.dp)
                }
            )

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Места для посещения",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

            }

            if (places.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    places.forEachIndexed { index, place ->
                        PlaceListItem(
                            place = place,
                            onRemoveClick = {
                                if (index < places.size) {
                                    val removedPlace = places.removeAt(index)
                                    tripId?.let { id ->
                                        onRemovePlaceClick(id, removedPlace.id)
                                    }
                                }
                            },
                            onPlaceClick = { clickedPlace ->
                                onPlaceClick(clickedPlace)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Места",
                            tint = if (title.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =   "Нажмите + чтобы добавить места",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (title.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            DatePickerButton(
                selectedDate = date,
                onDateSelected = { selectedDate ->
                    date = selectedDate
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Заметка",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Ваши заметки...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Дополнительная информация...") },
                singleLine = false
            )
        }
    }
}

@Composable
fun PlaceListItem(
    place: Place,
    onRemoveClick: () -> Unit,
    onPlaceClick: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlaceClick(place) },

        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = place.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = place.work_time,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить место",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selectedDate.isEmpty()) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "icon color animation"
    )

    OutlinedButton(
        onClick = {/* календарь добавить */   },
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Календарь",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (selectedDate.isEmpty()) "Выберите дату" else selectedDate,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TripEditScreenPreview() {
    MaterialTheme {
        TripEditScreen(
            onBackClick = {},
            onSaveTrip = {},
            onAddPlaceClick = {},
            onRemovePlaceClick = { tripId, placeId -> },
            onPlaceClick = {},
            deleteTrip = {}
        )
    }
}