package com.wiryadinata.stmlarkam.data.repository

import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.DetailIzin
import com.wiryadinata.stmlarkam.data.model.RekapKelas
import com.wiryadinata.stmlarkam.data.model.SesiLarkam
import com.wiryadinata.stmlarkam.data.model.SesiStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [LarkamRepository] used as a fallback when Firebase is not configured
 * (no google-services.json). Lets the whole app run — including the timer flow and
 * saving a session — without any backend. Data lives only for the process lifetime.
 *
 * Seeded with the default angkatan and a couple of sample sessions so the Home screen
 * is not empty during a demo.
 */
class LocalLarkamRepository : LarkamRepository {

    private val angkatan = MutableStateFlow(
        listOf(
            Angkatan(id = "a51", namaAngkatan = "@51"),
            Angkatan(id = "a52", namaAngkatan = "@52"),
            Angkatan(id = "a53", namaAngkatan = "@53")
        )
    )

    private val sessions = MutableStateFlow(sampleSessions())
    private var counter = 0

    override fun observeAngkatan(): Flow<List<Angkatan>> = angkatan.asStateFlow()

    override fun observeSesi(angkatanId: String?): Flow<List<SesiLarkam>> =
        sessions.map { list ->
            val filtered = if (angkatanId.isNullOrBlank()) {
                list
            } else {
                list.filter { it.angkatanId == angkatanId }
            }
            filtered.sortedByDescending { it.tanggal }
        }

    override suspend fun createSesi(sesi: SesiLarkam): String {
        val id = "local-${counter++}"
        sessions.update { it + sesi.copy(idSesi = id) }
        return id
    }

    override suspend fun updateSesi(idSesi: String, status: String, rekapKelas: List<RekapKelas>) {
        sessions.update { list ->
            list.map { sesi ->
                if (sesi.idSesi == idSesi) {
                    sesi.copy(status = status, rekapKelas = rekapKelas)
                } else {
                    sesi
                }
            }
        }
    }

    override suspend fun ensureAngkatanSeeded() {
        // Already seeded in-memory.
    }

    private fun sampleSessions(): List<SesiLarkam> = listOf(
        SesiLarkam(
            idSesi = "sample-1",
            tanggal = 1_722_556_800_000L, // 2 Aug 2024
            angkatanId = "a51",
            status = SesiStatus.SELESAI,
            rekapKelas = listOf(
                RekapKelas(
                    namaKelas = "XII TOI A",
                    totalSiswa = 30,
                    totalHadir = 28,
                    totalIzin = 2,
                    detailIzin = listOf(
                        DetailIzin("Andi", "Sakit"),
                        DetailIzin("Budi", "Izin keluarga")
                    )
                ),
                RekapKelas(
                    namaKelas = "XII IOP A",
                    totalSiswa = 32,
                    totalHadir = 31,
                    totalIzin = 1,
                    detailIzin = listOf(DetailIzin("Citra", "Lomba"))
                )
            )
        ),
        SesiLarkam(
            idSesi = "sample-2",
            tanggal = 1_723_161_600_000L, // 9 Aug 2024
            angkatanId = "a52",
            status = SesiStatus.SELESAI,
            rekapKelas = listOf(
                RekapKelas(
                    namaKelas = "XI TOI B",
                    totalSiswa = 29,
                    totalHadir = 27,
                    totalIzin = 2,
                    detailIzin = listOf(
                        DetailIzin("Dewi", "Sakit"),
                        DetailIzin("Eka", "Izin")
                    )
                )
            )
        )
    )
}
