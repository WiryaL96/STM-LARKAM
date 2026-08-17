package com.wiryadinata.stmlarkam.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wiryadinata.stmlarkam.R
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiryadinata.stmlarkam.data.model.Angkatan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: (String?) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_smkn1cimahi),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("STM LARKAM", fontWeight = FontWeight.Bold)
                            Text(
                                "Rekap Lari Kampus",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddClick(state.selectedAngkatanId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Sesi Baru") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AngkatanFilterRow(
                angkatanList = state.angkatanList,
                selectedAngkatanId = state.selectedAngkatanId,
                onSelect = viewModel::onSelectAngkatan
            )

            when {
                state.isLoading -> CenterBox { CircularProgressIndicator() }
                state.error != null -> CenterBox {
                    MessageBlock(
                        icon = { Icon(Icons.Filled.SentimentDissatisfied, null, Modifier.size(56.dp)) },
                        title = "Gagal memuat data",
                        subtitle = state.error ?: ""
                    )
                }
                state.kelasHistory.isEmpty() -> CenterBox {
                    MessageBlock(
                        icon = { Icon(Icons.Filled.Groups, null, Modifier.size(56.dp)) },
                        title = "Belum ada riwayat Larkam",
                        subtitle = "Tekan \"Sesi Baru\" untuk mulai mencatat."
                    )
                }
                else -> HistoryGrid(state.kelasHistory)
            }
        }
    }
}

@Composable
private fun AngkatanFilterRow(
    angkatanList: List<Angkatan>,
    selectedAngkatanId: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedAngkatanId == null,
            onClick = { onSelect(null) },
            label = { Text("Semua") }
        )
        angkatanList.forEach { angkatan ->
            FilterChip(
                selected = selectedAngkatanId == angkatan.id,
                onClick = { onSelect(angkatan.id) },
                label = { Text(angkatan.namaAngkatan) },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@Composable
private fun HistoryGrid(history: List<KelasHistoryUi>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(history) { item -> KelasHistoryCard(item) }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

@Composable
private fun MessageBlock(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        icon()
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
