package com.example.travel_planning.ui

import Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.network.model.RecommendResponse
import com.example.travel_planning.network.model.RecommendedPlace
import com.example.travel_planning.utils.ClusterDto
import com.example.travel_planning.utils.ClustersResponse
import com.example.travel_planning.utils.PlaceDto

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AITripScreen(
    api: Api,
    onBack: () -> Unit,
    onGenerate: (List<String>, String?) -> Unit
) {
    val context = LocalContext.current
    val clusters = remember { mutableStateOf<List<ClusterDto>>(emptyList()) }
    val selectedInterests = remember { mutableStateListOf<String>() }
    val loading = remember { mutableStateOf(true) }

    var metroSearchText by remember { mutableStateOf("") }
    var selectedMetro by remember { mutableStateOf<String?>(null) }

    val allMetroStations = remember {
        try {
            com.example.travel_planning.utils.loadJsonListFromAssets(context, "metro_stations.json")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<String>()
        }
    }

    val filteredMetro = remember(metroSearchText, selectedMetro) {
        if (metroSearchText.isEmpty() || selectedMetro != null) emptyList()
        else allMetroStations.filter {
            it.contains(metroSearchText, ignoreCase = true)
        }.take(5)
    }

    LaunchedEffect(Unit) {
        try {
            val response = api.getClusters()
            clusters.value = response.clusters
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI генерация",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (loading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Метро", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = selectedMetro ?: metroSearchText,
                        onValueChange = {
                            selectedMetro = null
                            metroSearchText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Выберите метро...") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        trailingIcon = {
                            if (selectedMetro != null || metroSearchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    selectedMetro = null
                                    metroSearchText = ""
                                }) { Icon(Icons.Default.Close, null) }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = filteredMetro.isNotEmpty()) {
                        Card(
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                filteredMetro.forEach { station ->
                                    Text(
                                        text = station,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMetro = station
                                                metroSearchText = ""
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ваши интересы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        clusters.value.forEach { cluster ->
                            val isSelected = cluster.name in selectedInterests
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedInterests.remove(cluster.name)
                                    else selectedInterests.add(cluster.name)
                                },
                                label = { Text(cluster.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, modifier = Modifier.size(16.dp), contentDescription = null) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AiMagicGenerateButton(
                    enabled = selectedInterests.isNotEmpty(),
                    onClick = { onGenerate(selectedInterests.toList(), selectedMetro) }
                )
            }
        }
    }
}

@Composable
fun AiMagicGenerateButton(enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = ""
    )

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC), Color(0xFFFC00FF)),
        start = Offset(offset, offset),
        end = Offset(offset + 600f, offset + 600f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { onClick() },
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) gradient else SolidColor(Color.Gray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp),
                    alpha = 1.0f
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Создать  маршрут",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
