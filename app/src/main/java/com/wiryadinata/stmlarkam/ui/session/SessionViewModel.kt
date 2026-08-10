package com.wiryadinata.stmlarkam.ui.session

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wiryadinata.stmlarkam.data.ServiceLocator
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.DetailIzin
import com.wiryadinata.stmlarkam.data.model.RekapKelas
import com.wiryadinata.stmlarkam.data.model.SesiLarkam
import com.wiryadinata.stmlarkam.data.model.SesiStatus
import com.wiryadinata.stmlarkam.data.repository.LarkamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Tick interval for the countdown. The display is mm:ss, so refreshing twice a second is
// smooth enough while halving the recomposition churn a 250ms tick caused.
private const val TICK_INTERVAL_MS: Long = 500L

/** State of one class's independent timer. */
enum class TimerStatus { BELUM_MULAI, BERJALAN, PAUSED, SELESAI }

/**
 * A class participating in the session, with its OWN timer state. The countdown length is
 * chosen by the user per class ([durationMs]) and supports pause/resume: [startElapsed]
 * marks when the current running segment began and [resumeRemainingMs] the time left at
 * that moment, so remaining = resumeRemainingMs - (now - startElapsed).
 */
data class KelasCard(
    val id: Int,
    val namaKelas: String,
    val totalSiswa: Int,               // jumlah hadir target (roster yang lari)
    val izin: List<DetailIzin>,        // siswa tidak hadir + alasan
    val durationMs: Long,              // durasi timer yang diinput user
    val totalHadir: Int = 0,           // dihitung live lewat ketuk kartu
    val status: TimerStatus = TimerStatus.BELUM_MULAI,
    val startElapsed: Long = 0L,       // elapsedRealtime saat segmen berjalan sekarang dimulai (monotonic)
    val startWallClock: Long = 0L,     // currentTimeMillis saat PERTAMA mulai (untuk waktu_mulai_timer)
    val resumeRemainingMs: Long = 0L,  // sisa waktu saat segmen berjalan sekarang dimulai (untuk pause/resume)
    val remainingMs: Long = 0L,        // sisa waktu terkini (di-update ticker; beku saat pause/selesai)
    val endedByTimeout: Boolean = false, // true bila SELESAI karena waktu habis
    val completedFull: Boolean = false   // true bila SELESAI karena semua siswa sudah didata (hadir penuh)
) {
    val isDone: Boolean get() = status == TimerStatus.SELESAI
    val isRunning: Boolean get() = status == TimerStatus.BERJALAN
    val isPaused: Boolean get() = status == TimerStatus.PAUSED
}

data class SessionUiState(
    val angkatanOptions: List<Angkatan> = emptyList(),
    val angkatanId: String = "",
    val cards: List<KelasCard> = emptyList(),
    val isSaving: Boolean = false,
    val savedAndDone: Boolean = false,
    val error: String? = null
) {
    val canFinish: Boolean get() = cards.isNotEmpty()
}

/**
 * Owns the Page_add screen with INDEPENDENT per-class timers:
 *  - add classes (nama, jumlah hadir, izin, durasi) in any order,
 *  - start each class's own user-defined countdown separately ([startTimer]),
 *  - pause/resume a running class timer ([pauseTimer] / [resumeTimer]),
 *  - a single ticker recomputes every running card's remaining time from its own segment,
 *  - each card flips to red (SELESAI) when ITS timer hits zero,
 *  - tapping a running card increments its attendance,
 *  - Realtime Database is synced on each start/pause/expiry (waktu_mulai_timer + status_timer)
 *    and finalized to SELESAI when the session is saved.
 */
class SessionViewModel(
    private val repository: LarkamRepository,
    initialAngkatanId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionUiState(angkatanId = initialAngkatanId.orEmpty())
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var nextCardId = 0

    // Remote persistence is serialized so the first write creates the doc and the rest update it.
    private val remoteMutex = Mutex()
    private var remoteSesiId: String? = null

    init {
        viewModelScope.launch {
            repository.observeAngkatan().collect { list ->
                _uiState.update { state ->
                    val resolved = state.angkatanId.ifBlank { list.firstOrNull()?.id.orEmpty() }
                    state.copy(angkatanOptions = list, angkatanId = resolved)
                }
            }
        }
    }

    // ---- Editing --------------------------------------------------------------

    fun onSelectAngkatan(angkatanId: String) {
        _uiState.update { it.copy(angkatanId = angkatanId) }
    }

    fun addKelas(namaKelas: String, totalSiswa: Int, izin: List<DetailIzin>, durasiMenit: Int) {
        if (namaKelas.isBlank() || totalSiswa <= 0 || durasiMenit <= 0) return
        val durationMs = durasiMenit * 60_000L
        val card = KelasCard(
            id = nextCardId++,
            namaKelas = namaKelas.trim(),
            totalSiswa = totalSiswa,
            izin = izin,
            durationMs = durationMs,
            resumeRemainingMs = durationMs,
            remainingMs = durationMs
        )
        _uiState.update { it.copy(cards = it.cards + card) }
    }

    fun removeKelas(id: Int) {
        _uiState.update { state -> state.copy(cards = state.cards.filterNot { it.id == id }) }
    }

    // ---- Per-class timer ------------------------------------------------------

    /** Starts THIS class's independent countdown using its user-defined duration. */
    fun startTimer(id: Int) {
        val now = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        var started = false
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == id && card.status == TimerStatus.BELUM_MULAI) {
                        started = true
                        card.copy(
                            status = TimerStatus.BERJALAN,
                            startElapsed = now,
                            startWallClock = wall,
                            resumeRemainingMs = card.durationMs,
                            remainingMs = card.durationMs
                        )
                    } else {
                        card
                    }
                }
            )
        }
        if (started) {
            ensureTicker()
            syncRemote() // persist waktu_mulai_timer + status_timer for this class
        }
    }

    /** Pauses THIS class's running timer, freezing its remaining time. */
    fun pauseTimer(id: Int) {
        val now = SystemClock.elapsedRealtime()
        var paused = false
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == id && card.status == TimerStatus.BERJALAN) {
                        paused = true
                        val remaining =
                            (card.resumeRemainingMs - (now - card.startElapsed)).coerceAtLeast(0L)
                        card.copy(status = TimerStatus.PAUSED, remainingMs = remaining)
                    } else {
                        card
                    }
                }
            )
        }
        if (paused) syncRemote() // persist status_timer = PAUSED for this class
    }

    /** Resumes THIS class's paused timer, continuing from its frozen remaining time. */
    fun resumeTimer(id: Int) {
        val now = SystemClock.elapsedRealtime()
        var resumed = false
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == id && card.status == TimerStatus.PAUSED) {
                        resumed = true
                        card.copy(
                            status = TimerStatus.BERJALAN,
                            startElapsed = now,
                            resumeRemainingMs = card.remainingMs
                        )
                    } else {
                        card
                    }
                }
            )
        }
        if (resumed) {
            ensureTicker()
            syncRemote() // persist status_timer = BERJALAN for this class
        }
    }

    /** Stops THIS class's timer manually (before it runs out). Freezes its remaining time. */
    fun stopTimer(id: Int) {
        var stopped = false
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == id &&
                        (card.status == TimerStatus.BERJALAN || card.status == TimerStatus.PAUSED)
                    ) {
                        stopped = true
                        // Keep remainingMs frozen at its current value; not a timeout.
                        card.copy(status = TimerStatus.SELESAI, endedByTimeout = false)
                    } else {
                        card
                    }
                }
            )
        }
        if (stopped) {
            // Persist status_timer = SELESAI for this class. The session is NOT saved
            // automatically — the user taps "Selesai & Simpan Sesi" to finalize.
            syncRemote()
        }
    }

    /**
     * Records one more present student for a running card. When the count reaches the
     * class's target ([KelasCard.totalSiswa]) — i.e. every present student has been
     * tapped in — THIS class's timer finishes automatically (hadir penuh).
     */
    fun onCardTap(id: Int) {
        var completed = false
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == id && card.isRunning && card.totalHadir < card.totalSiswa) {
                        val newHadir = card.totalHadir + 1
                        if (newHadir >= card.totalSiswa) {
                            completed = true
                            card.copy(
                                totalHadir = newHadir,
                                status = TimerStatus.SELESAI,
                                completedFull = true,
                                endedByTimeout = false
                            )
                        } else {
                            card.copy(totalHadir = newHadir)
                        }
                    } else {
                        card
                    }
                }
            )
        }
        if (completed) {
            // Persist status_timer = SELESAI for this class. The session is NOT saved
            // automatically — the user taps "Selesai & Simpan Sesi" to finalize.
            syncRemote()
        }
    }

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            var previousRunning = runningIds()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                _uiState.update { state ->
                    state.copy(
                        cards = state.cards.map { card ->
                            if (card.status != TimerStatus.BERJALAN) return@map card
                            val remaining =
                                (card.resumeRemainingMs - (now - card.startElapsed)).coerceAtLeast(0L)
                            if (remaining <= 0L) {
                                card.copy(
                                    status = TimerStatus.SELESAI,
                                    remainingMs = 0L,
                                    endedByTimeout = true
                                )
                            } else {
                                card.copy(remainingMs = remaining)
                            }
                        }
                    )
                }

                val nowRunning = runningIds()
                val justExpired = previousRunning - nowRunning
                previousRunning = nowRunning
                if (justExpired.isNotEmpty()) {
                    // Persist status_timer = SELESAI for expired classes only; the session
                    // is finalized manually via the "Selesai & Simpan Sesi" button.
                    syncRemote()
                }
                if (nowRunning.isEmpty()) break
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun runningIds(): Set<Int> =
        _uiState.value.cards.filter { it.status == TimerStatus.BERJALAN }.map { it.id }.toSet()

    // ---- Persistence ----------------------------------------------------------

    /** Finalizes the whole session (stops any running timers) and saves to the repository. */
    fun finishSession() {
        val state = _uiState.value
        if (state.isSaving || state.cards.isEmpty()) return
        tickerJob?.cancel()
        _uiState.update { current ->
            current.copy(
                isSaving = true,
                error = null,
                // Stop still-running/paused timers so their status_timer is finalized.
                cards = current.cards.map {
                    if (it.status == TimerStatus.BERJALAN || it.status == TimerStatus.PAUSED) {
                        it.copy(status = TimerStatus.SELESAI, endedByTimeout = false)
                    } else {
                        it
                    }
                }
            )
        }
        runSave()
    }

    /** Retries a failed save without losing recorded data. */
    fun retrySave() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        runSave()
    }

    private fun runSave() {
        viewModelScope.launch {
            runCatching { persistRemote(SesiStatus.SELESAI) }
                .onSuccess { _uiState.update { it.copy(isSaving = false, savedAndDone = true) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "Gagal menyimpan data")
                    }
                }
        }
    }

    /** Best-effort intermediate sync (fire-and-forget). */
    private fun syncRemote() {
        viewModelScope.launch { runCatching { persistRemote(SesiStatus.BERJALAN) } }
    }

    private suspend fun persistRemote(status: String) {
        remoteMutex.withLock {
            val id = remoteSesiId
            if (id == null) {
                remoteSesiId = repository.createSesi(buildSesi(status))
            } else {
                repository.updateSesi(id, status, buildRekap())
            }
        }
    }

    private fun buildSesi(status: String): SesiLarkam = SesiLarkam(
        tanggal = System.currentTimeMillis(),
        angkatanId = _uiState.value.angkatanId,
        status = status,
        rekapKelas = buildRekap()
    )

    private fun buildRekap(): List<RekapKelas> = _uiState.value.cards.map { card ->
        RekapKelas(
            namaKelas = card.namaKelas,
            totalSiswa = card.totalSiswa,
            totalHadir = card.totalHadir,
            totalIzin = card.izin.size,
            detailIzin = card.izin,
            waktuMulaiTimer = if (card.startWallClock > 0L) card.startWallClock else null,
            statusTimer = card.status.name
        )
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }

    companion object {
        fun factory(initialAngkatanId: String?) = viewModelFactory {
            initializer { SessionViewModel(ServiceLocator.repository, initialAngkatanId) }
        }
    }
}
