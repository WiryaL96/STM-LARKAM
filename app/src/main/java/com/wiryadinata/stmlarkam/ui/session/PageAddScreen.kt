package com.wiryadinata.stmlarkam.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.DetailIzin
import com.wiryadinata.stmlarkam.ui.formatMmSs
import com.wiryadinata.stmlarkam.ui.theme.CardExpiredRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageAddScreen(
    initialAngkatanId: String?,
    onNavigateHome: () -> Unit,
    viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory(initialAngkatanId))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showOverlay = state.isSaving || state.savedAndDone || state.error != null

    // Auto-redirect to Home once the session has been saved.
    LaunchedEffect(state.savedAndDone) {
        if (state.savedAndDone) onNavigateHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pencatatan Larkam") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!showOverlay) {
                FinishBar(enabled = state.canFinish, onFinish = viewModel::finishSession)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showOverlay) {
                SessionOverlay(state = state, viewModel = viewModel, onNavigateHome = onNavigateHome)
            } else {
                EditingContent(state = state, viewModel = viewModel)
            }
        }
    }
}

// ---- Editing (add classes + independent timers) -------------------------------

@Composable
private fun EditingContent(state: SessionUiState, viewModel: SessionViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SectionTitle("Pilih Angkatan")
                AngkatanChips(
                    options = state.angkatanOptions,
                    selectedId = state.angkatanId,
                    onSelect = viewModel::onSelectAngkatan
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(Modifier.height(4.dp))
                SectionTitle("Tambah Kelas")
                AddKelasForm(onAdd = viewModel::addKelas)
                Spacer(Modifier.height(8.dp))
                SectionTitle(
                    if (state.cards.isEmpty()) "Belum ada kelas" else "Kelas (${state.cards.size})"
                )
                if (state.cards.isEmpty()) {
                    Text(
                        "Tambahkan kelas, lalu tekan \"Mulai Timer\" pada masing-masing kartu. " +
                            "Tiap kelas punya hitung mundur 25 menit sendiri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(state.cards, key = { it.id }) { card ->
            TimerCard(
                card = card,
                onStart = { viewModel.startTimer(card.id) },
                onStop = { viewModel.stopTimer(card.id) },
                onTap = { viewModel.onCardTap(card.id) },
                onRemove = { viewModel.removeKelas(card.id) }
            )
        }
    }
}

@Composable
private fun TimerCard(
    card: KelasCard,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    // Manually stopped classes end in a neutral color; only a real timeout is red.
    val doneColor = if (card.endedByTimeout) CardExpiredRed else MaterialTheme.colorScheme.secondary
    val borderColor = when (card.status) {
        TimerStatus.BELUM_MULAI -> MaterialTheme.colorScheme.outline
        TimerStatus.BERJALAN -> MaterialTheme.colorScheme.primary
        TimerStatus.SELESAI -> doneColor
    }
    val accentColor = if (card.isDone) doneColor else MaterialTheme.colorScheme.onSurface
    val tapModifier = if (card.isRunning) Modifier.clickable(onClick = onTap) else Modifier

    OutlinedCard(
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.height(190.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(tapModifier)
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.namaKelas,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                if (card.status == TimerStatus.BELUM_MULAI) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus kelas", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${card.totalHadir} / ${card.totalSiswa}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = "hadir • ${card.izin.size} izin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (card.status) {
                TimerStatus.BELUM_MULAI -> Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Mulai Timer")
                }

                TimerStatus.BERJALAN -> Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = formatMmSs(card.remainingMs),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        OutlinedButton(
                            onClick = onStop,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CardExpiredRed)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(2.dp))
                            Text("Stop")
                        }
                    }
                    Text(
                        "ketuk kartu untuk +1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TimerStatus.SELESAI -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (card.endedByTimeout) Icons.Filled.TimerOff else Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = doneColor
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = if (card.endedByTimeout) "WAKTU HABIS" else "DIHENTIKAN",
                        color = doneColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishBar(enabled: Boolean, onFinish: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Button(
            onClick = onFinish,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Selesai & Simpan Sesi")
        }
    }
}

// ---- Add-class form -----------------------------------------------------------

@Composable
private fun AngkatanChips(
    options: List<Angkatan>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    if (options.isEmpty()) {
        Text(
            "Memuat angkatan…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { angkatan ->
            FilterChip(
                selected = selectedId == angkatan.id,
                onClick = { onSelect(angkatan.id) },
                label = { Text(angkatan.namaAngkatan) }
            )
        }
    }
}

@Composable
private fun AddKelasForm(onAdd: (String, Int, List<DetailIzin>) -> Unit) {
    var namaKelas by remember { mutableStateOf("") }
    var jumlahText by remember { mutableStateOf("") }
    val izinList = remember { mutableStateListOf<DetailIzin>() }
    var izinNama by remember { mutableStateOf("") }
    var izinAlasan by remember { mutableStateOf("") }

    val jumlah = jumlahText.toIntOrNull() ?: 0
    val canAdd = namaKelas.isNotBlank() && jumlah > 0

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(14.dp)) {
            OutlinedTextField(
                value = namaKelas,
                onValueChange = { namaKelas = it },
                label = { Text("Nama Kelas") },
                placeholder = { Text("cth: XII TOI A") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = jumlahText,
                onValueChange = { input -> jumlahText = input.filter { it.isDigit() } },
                label = { Text("Jumlah Siswa Hadir") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text("Siswa Tidak Hadir (Izin)", style = MaterialTheme.typography.labelLarge)
            izinList.forEachIndexed { index, izin ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        "• ${izin.nama} — ${izin.alasan}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { izinList.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus izin")
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = izinNama,
                    onValueChange = { izinNama = it },
                    label = { Text("Nama") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = izinAlasan,
                    onValueChange = { izinAlasan = it },
                    label = { Text("Alasan") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (izinNama.isNotBlank()) {
                            izinList.add(DetailIzin(izinNama.trim(), izinAlasan.trim()))
                            izinNama = ""
                            izinAlasan = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah izin")
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onAdd(namaKelas, jumlah, izinList.toList())
                    namaKelas = ""
                    jumlahText = ""
                    izinList.clear()
                    izinNama = ""
                    izinAlasan = ""
                },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Tambah Kelas")
            }
        }
    }
}

// ---- Saving / result overlay --------------------------------------------------

@Composable
private fun SessionOverlay(
    state: SessionUiState,
    viewModel: SessionViewModel,
    onNavigateHome: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            when {
                state.isSaving -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Menyimpan data…")
                }

                state.error != null -> {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = CardExpiredRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Gagal menyimpan", fontWeight = FontWeight.Bold)
                    Text(
                        text = state.error,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = viewModel::retrySave,
                            colors = ButtonDefaults.buttonColors()
                        ) { Text("Coba Lagi") }
                        OutlinedButton(onClick = onNavigateHome) { Text("Kembali ke Home") }
                    }
                }

                else -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Tersimpan!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
