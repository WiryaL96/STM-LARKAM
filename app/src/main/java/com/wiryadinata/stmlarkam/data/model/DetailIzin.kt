package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * One student who is excused (izin) from a Larkam session, stored inside a
 * [RekapKelas.detailIzin] list as `{ nama, alasan }`.
 */
@IgnoreExtraProperties
data class DetailIzin(
    var nama: String = "",
    var alasan: String = ""
)
