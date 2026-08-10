package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * A record under the `angkatan` node of Realtime Database.
 *
 * Stored shape (JSON):
 * ```
 * "angkatan": {
 *   "<pushId>": { "nama_angkatan": "@51" }
 * }
 * ```
 *
 * [id] is the record key (the push id); it is NOT written as a child (see [Exclude])
 * and is populated from the snapshot key when reading.
 *
 * All properties are `var` with defaults so Realtime Database can deserialize via the
 * no-arg constructor + setters.
 */
@IgnoreExtraProperties
data class Angkatan(
    @get:Exclude
    var id: String = "",

    @get:PropertyName("nama_angkatan")
    @set:PropertyName("nama_angkatan")
    var namaAngkatan: String = ""
)
