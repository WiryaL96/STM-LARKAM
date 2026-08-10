package com.wiryadinata.stmlarkam.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.DetailIzin
import com.wiryadinata.stmlarkam.data.model.KelasCatalog
import com.wiryadinata.stmlarkam.ui.formatMmSs
import com.wiryadinata.stmlarkam.ui.theme.CardExpiredRed

/** Attendance input bounds per class: minimal 1, maksimal 40 siswa. */
private const val MAX_SISWA = 40

/** Timer duration input bounds per class (minutes): minimal 1, maksimal 180. */
private const val MAX_DURASI_MENIT = 180

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
                // Class options come from the catalog for the selected angkatan, minus the
                // classes already added, so the same class can't be picked twice.
                // remember() keyed on angkatan + added names so the timer's frequent state
                // updates don't rebuild this list (and re-render the picker) on every tick.
                val namaAngkatan = state.angkatanOptions
                    .firstOrNull { it.id == state.angkatanId }?.namaAngkatan.orEmpty()
                val addedNames = state.cards.map { it.namaKelas }.toSet()
                val kelasOptions = remember(namaAngkatan, addedNames) {
                    KelasCatalog.kelasFor(namaAngkatan).filterNot { it in addedNames }
                }
                AddKelasForm(
                    angkatanKey = state.angkatanId,
                    kelasOptions = kelasOptions,
                    onAdd = viewModel::addKelas
                )
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
                onPause = { viewModel.pauseTimer(card.id) },
                onResume = { viewModel.resumeTimer(card.id) },
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
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    // Full attendance ends green (positive); a real timeout is red; manual stop is neutral.
    val doneColor = when {
        card.completedFull -> MaterialTheme.colorScheme.primary
        card.endedByTimeout -> CardExpiredRed
        else -> MaterialTheme.colorScheme.secondary
    }
    val borderColor = when (card.status) {
        TimerStatus.BELUM_MULAI -> MaterialTheme.colorScheme.outline
        TimerStatus.BERJALAN -> MaterialTheme.colorScheme.primary
        TimerStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
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
                    // Show the user-defined duration on the start button.
                    Text("Mulai ${formatMmSs(card.durationMs)}")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Pause,
                                    contentDescription = "Jeda timer",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Stop,
                                    contentDescription = "Stop timer",
                                    modifier = Modifier.size(20.dp),
                                    tint = CardExpiredRed
                                )
                            }
                        }
                    }
                    Text(
                        "ketuk kartu untuk +1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TimerStatus.PAUSED -> Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = formatMmSs(card.remainingMs),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Lanjutkan timer",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Stop,
                                    contentDescription = "Stop timer",
                                    modifier = Modifier.size(20.dp),
                                    tint = CardExpiredRed
                                )
                            }
                        }
                    }
                    Text(
                        "timer dijeda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TimerStatus.SELESAI -> {
                    val doneIcon = when {
                        card.completedFull -> Icons.Filled.CheckCircle
                        card.endedByTimeout -> Icons.Filled.TimerOff
                        else -> Icons.Filled.Stop
                    }
                    val doneLabel = when {
                        card.completedFull -> "HADIR LENGKAP"
                        card.endedByTimeout -> "WAKTU HABIS"
                        else -> "DIHENTIKAN"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            doneIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = doneColor
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = doneLabel,
                            color = doneColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddKelasForm(
    angkatanKey: String,
    kelasOptions: List<String>,
    onAdd: (String, Int, List<DetailIzin>, Int) -> Unit
) {
    var namaKelas by remember { mutableStateOf("") }
    var jumlahText by remember { mutableStateOf("") }
    var durasiText by remember { mutableStateOf("") }
    val izinList = remember { mutableStateListOf<DetailIzin>() }
    var izinNama by remember { mutableStateOf("") }
    var izinAlasan by remember { mutableStateOf("") }

    // Reset the picked class when the angkatan (hence the option set) changes.
    LaunchedEffect(angkatanKey) { namaKelas = "" }

    val jumlah = jumlahText.toIntOrNull() ?: 0
    val jumlahValid = jumlah in 1..MAX_SISWA
    val durasi = durasiText.toIntOrNull() ?: 0
    val durasiValid = durasi in 1..MAX_DURASI_MENIT
    val canAdd = namaKelas.isNotBlank() && jumlahValid && durasiValid

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(14.dp)) {
            KelasDropdown(
                options = kelasOptions,
                selected = namaKelas,
                onSelect = { namaKelas = it }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = jumlahText,
                onValueChange = { input ->
                    // Digits only, capped at MAX_SISWA (max 2 digits). Empty is allowed while
                    // typing but blocks "Tambah Kelas" via canAdd (minimal 1 siswa).
                    val digits = input.filter { it.isDigit() }.take(2)
                    jumlahText = if (digits.isEmpty()) "" else digits.toInt().coerceAtMost(MAX_SISWA).toString()
                },
                label = { Text("Jumlah Siswa Hadir (1–$MAX_SISWA)") },
                isError = jumlahText.isNotEmpty() && !jumlahValid,
                supportingText = {
                    Text(
                        if (jumlahText.isNotEmpty() && !jumlahValid) "Minimal 1 siswa"
                        else "Maksimal $MAX_SISWA siswa"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = durasiText,
                onValueChange = { input ->
                    // Digits only (max 3 digits), capped at MAX_DURASI_MENIT. Empty allowed
                    // while typing but blocks "Tambah Kelas" via canAdd (minimal 1 menit).
                    val digits = input.filter { it.isDigit() }.take(3)
                    durasiText = if (digits.isEmpty()) "" else digits.toInt().coerceAtMost(MAX_DURASI_MENIT).toString()
                },
                label = { Text("Durasi Timer / menit (1–$MAX_DURASI_MENIT)") },
                isError = durasiText.isNotEmpty() && !durasiValid,
                supportingText = {
                    Text(
                        if (durasiText.isNotEmpty() && !durasiValid) "Minimal 1 menit"
                        else "Timer hitung mundur dari durasi ini"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text(
                if (izinList.isEmpty()) "Siswa Tidak Hadir (Izin)"
                else "Siswa Tidak Hadir (Izin) • ${izinList.size}",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))

            val addIzin = {
                if (izinNama.isNotBlank()) {
                    // Prepend so the newest entry shows right under the input.
                    izinList.add(0, DetailIzin(izinNama.trim(), izinAlasan.trim()))
                    izinNama = ""
                    izinAlasan = ""
                }
            }

            // Input row FIRST so it stays put near the label as entries pile up.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = izinNama,
                    onValueChange = { izinNama = it },
                    label = { Text("Nama") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = izinAlasan,
                    onValueChange = { izinAlasan = it },
                    label = { Text("Alasan") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addIzin() }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = addIzin) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah izin")
                }
            }

            // Added entries, newest first, in a bounded scroll area so the form stays compact
            // and you always see what was just added.
            if (izinList.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    izinList.forEachIndexed { index, izin ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                "• ${izin.nama}" +
                                    if (izin.alasan.isNotBlank()) " — ${izin.alasan}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { izinList.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Hapus izin",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onAdd(namaKelas, jumlah, izinList.toList(), durasi)
                    namaKelas = ""
                    jumlahText = ""
                    durasiText = ""
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

/** Read-only dropdown that lists the predefined classes for the selected angkatan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KelasDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Pilih Kelas") },
            placeholder = { Text("cth: XII TOI A") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Tidak ada kelas tersedia") },
                    onClick = {},
                    enabled = false
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
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
