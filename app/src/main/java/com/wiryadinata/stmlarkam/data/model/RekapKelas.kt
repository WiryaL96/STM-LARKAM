package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Status of a single class's independent 25-minute timer.
 * Stored in the `status_timer` field.
 */
object TimerStatusValue {
    const val BELUM_MULAI = "BELUM_MULAI"
    const val BERJALAN = "BERJALAN"
    const val SELESAI = "SELESAI"
}

/**
 * Recap for a single class within a Larkam session. Stored as an element of the
 * `rekap_kelas` array on a [SesiLarkam] document.
 *
 * Firestore field mapping:
 *  - `nama_kelas`         -> [namaKelas]        (e.g. "XII TOI A")
 *  - `total_siswa`        -> [totalSiswa]       (jumlah hadir target / roster yang lari)
 *  - `total_hadir`        -> [totalHadir]       (counted present via card taps)
 *  - `total_izin`         -> [totalIzin]        (jumlah tidak hadir)
 *  - `detail_izin`        -> [detailIzin]       (list of { nama, alasan })
 *  - `waktu_mulai_timer`  -> [waktuMulaiTimer]  (when THIS class's timer started; null if not started)
 *  - `status_timer`       -> [statusTimer]      (see [TimerStatusValue])
 */
data class RekapKelas(
    @get:PropertyName("nama_kelas")
    @set:PropertyName("nama_kelas")
    var namaKelas: String = "",

    @get:PropertyName("total_siswa")
    @set:PropertyName("total_siswa")
    var totalSiswa: Int = 0,

    @get:PropertyName("total_hadir")
    @set:PropertyName("total_hadir")
    var totalHadir: Int = 0,

    @get:PropertyName("total_izin")
    @set:PropertyName("total_izin")
    var totalIzin: Int = 0,

    @get:PropertyName("detail_izin")
    @set:PropertyName("detail_izin")
    var detailIzin: List<DetailIzin> = emptyList(),

    @get:PropertyName("waktu_mulai_timer")
    @set:PropertyName("waktu_mulai_timer")
    var waktuMulaiTimer: Timestamp? = null,

    @get:PropertyName("status_timer")
    @set:PropertyName("status_timer")
    var statusTimer: String = TimerStatusValue.BELUM_MULAI
)
