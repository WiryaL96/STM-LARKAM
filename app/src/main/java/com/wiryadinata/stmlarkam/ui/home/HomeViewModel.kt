package com.wiryadinata.stmlarkam.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wiryadinata.stmlarkam.data.ServiceLocator
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.repository.LarkamRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A class history card shown on the Home screen. */
data class KelasHistoryUi(
    val namaKelas: String,
    val totalSiswa: Int,
    val totalHadir: Int,
    val totalIzin: Int,
    val angkatanId: String,
    val tanggal: Long
)

data class HomeUiState(
    val angkatanList: List<Angkatan> = emptyList(),
    val selectedAngkatanId: String? = null, // null = "Semua"
    val kelasHistory: List<KelasHistoryUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Drives the Home screen: exposes the angkatan filter, the current selection, and
 * the flattened list of class history cards (derived from finished sessions),
 * all backed by real-time Firestore streams.
 */
class HomeViewModel(private val repository: LarkamRepository) : ViewModel() {

    private val selectedAngkatanId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAngkatan(),
        selectedAngkatanId.flatMapLatest { repository.observeSesi(it) },
        selectedAngkatanId
    ) { angkatan, sessions, selected ->
        val history = sessions.flatMap { sesi ->
            sesi.rekapKelas.map { rekap ->
                KelasHistoryUi(
                    namaKelas = rekap.namaKelas,
                    totalSiswa = rekap.totalSiswa,
                    totalHadir = rekap.totalHadir,
                    totalIzin = rekap.totalIzin,
                    angkatanId = sesi.angkatanId,
                    tanggal = sesi.tanggal
                )
            }
        }
        HomeUiState(
            angkatanList = angkatan,
            selectedAngkatanId = selected,
            kelasHistory = history,
            isLoading = false
        )
    }.catch { throwable ->
        emit(HomeUiState(isLoading = false, error = throwable.message ?: "Gagal memuat data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        // Seed default angkatan on first run; ignore failures (e.g. Firebase not yet configured).
        viewModelScope.launch { runCatching { repository.ensureAngkatanSeeded() } }
    }

    fun onSelectAngkatan(angkatanId: String?) {
        selectedAngkatanId.value = angkatanId
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(ServiceLocator.repository) }
        }
    }
}
