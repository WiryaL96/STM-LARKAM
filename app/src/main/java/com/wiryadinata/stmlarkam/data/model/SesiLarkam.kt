package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * Status values for a Larkam session, as stored in the `status` field.
 */
object SesiStatus {
    const val BERJALAN = "BERJALAN"
    const val SELESAI = "SELESAI"
}

/**
 * A record under the `sesi_larkam` node of Realtime Database.
 *
 * Realtime Database field mapping:
 *  - record key    -> [idSesi] (the push id; excluded from the written children)
 *  - `tanggal`     -> [tanggal] (epoch milliseconds)
 *  - `angkatan_id` -> [angkatanId]
 *  - `status`      -> [status]  (see [SesiStatus])
 *  - `rekap_kelas` -> [rekapKelas] (list of [RekapKelas])
 */
@IgnoreExtraProperties
data class SesiLarkam(
    @get:Exclude
    var idSesi: String = "",

    var tanggal: Long = 0L,

    @get:PropertyName("angkatan_id")
    @set:PropertyName("angkatan_id")
    var angkatanId: String = "",

    var status: String = SesiStatus.BERJALAN,

    @get:PropertyName("rekap_kelas")
    @set:PropertyName("rekap_kelas")
    var rekapKelas: List<RekapKelas> = emptyList()
)
