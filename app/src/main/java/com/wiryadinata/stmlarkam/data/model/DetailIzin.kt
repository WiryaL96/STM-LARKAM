package com.wiryadinata.stmlarkam.data.model

/**
 * One student who is excused (izin) from a Larkam session, stored inside a
 * [RekapKelas.detailIzin] array as `{ nama, alasan }`.
 */
data class DetailIzin(
    var nama: String = "",
    var alasan: String = ""
)
