package com.wiryadinata.stmlarkam.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore document in the `angkatan` collection.
 *
 * Fields (as stored in Firestore):
 *  - document id      -> [id]
 *  - `nama_angkatan`  -> [namaAngkatan] (e.g. "@51")
 *
 * All properties are `var` with defaults so Firestore can deserialize via the
 * no-arg constructor path.
 */
data class Angkatan(
    @DocumentId
    var id: String = "",

    @get:PropertyName("nama_angkatan")
    @set:PropertyName("nama_angkatan")
    var namaAngkatan: String = ""
)
