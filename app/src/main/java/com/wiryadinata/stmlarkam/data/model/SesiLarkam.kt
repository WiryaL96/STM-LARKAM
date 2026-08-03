package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Status values for a Larkam session, as stored in the `status` field.
 */
object SesiStatus {
    const val BERJALAN = "BERJALAN"
    const val SELESAI = "SELESAI"
}

/**
 * Firestore document in the `sesi_larkam` collection.
 *
 * Firestore field mapping:
 *  - document id    -> [idSesi]
 *  - `tanggal`      -> [tanggal] (epoch milliseconds)
 *  - `angkatan_id`  -> [angkatanId]
 *  - `status`       -> [status]  (see [SesiStatus])
 *  - `rekap_kelas`  -> [rekapKelas] (array of [RekapKelas])
 */
data class SesiLarkam(
    @DocumentId
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
