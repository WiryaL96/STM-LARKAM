package com.wiryadinata.stmlarkam.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.RekapKelas
import com.wiryadinata.stmlarkam.data.model.SesiLarkam
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore-backed [LarkamRepository].
 *
 * Collections:
 *  - `angkatan`     : one document per angkatan.
 *  - `sesi_larkam`  : one document per Larkam session, with an embedded
 *                     `rekap_kelas` array.
 *
 * Real-time reads use [callbackFlow] wrapping `addSnapshotListener`; writes use a
 * small [awaitResult] helper so we don't need the coroutines-play-services artifact.
 */
class FirestoreLarkamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LarkamRepository {

    private val angkatanCol get() = db.collection(COL_ANGKATAN)
    private val sesiCol get() = db.collection(COL_SESI)

    override fun observeAngkatan(): Flow<List<Angkatan>> = callbackFlow {
        val registration = angkatanCol
            .orderBy(FIELD_NAMA_ANGKATAN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Angkatan::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override fun observeSesi(angkatanId: String?): Flow<List<SesiLarkam>> = callbackFlow {
        // Single equality filter only (no orderBy) so no composite index is required;
        // sorting by date is done client-side below.
        val query: Query = if (angkatanId.isNullOrBlank()) {
            sesiCol
        } else {
            sesiCol.whereEqualTo(FIELD_ANGKATAN_ID, angkatanId)
        }
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(SesiLarkam::class.java)
                .orEmpty()
                .sortedByDescending { it.tanggal }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createSesi(sesi: SesiLarkam): String {
        val ref = sesiCol.document()
        // Persist the generated id inside the document too (idSesi field).
        val toStore = sesi.copy(idSesi = ref.id)
        ref.set(toStore).awaitResult()
        return ref.id
    }

    override suspend fun updateSesi(idSesi: String, status: String, rekapKelas: List<RekapKelas>) {
        // RekapKelas is annotated with @PropertyName, so passing the objects lets the
        // Firestore mapper serialize them (incl. waktu_mulai_timer + status_timer).
        sesiCol.document(idSesi)
            .update(
                mapOf(
                    FIELD_STATUS to status,
                    FIELD_REKAP_KELAS to rekapKelas
                )
            )
            .awaitResult()
    }

    override suspend fun ensureAngkatanSeeded() {
        val existing = angkatanCol.limit(1).get().awaitResult()
        if (!existing.isEmpty) return
        val batch = db.batch()
        DEFAULT_ANGKATAN.forEach { nama ->
            val ref = angkatanCol.document()
            batch.set(ref, mapOf(FIELD_NAMA_ANGKATAN to nama))
        }
        batch.commit().awaitResult()
    }

    /** Suspends until the [Task] completes, without pulling in play-services-tasks-ktx. */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
        addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        addOnCanceledListener { cont.cancel() }
    }

    companion object {
        const val COL_ANGKATAN = "angkatan"
        const val COL_SESI = "sesi_larkam"

        private const val FIELD_NAMA_ANGKATAN = "nama_angkatan"
        private const val FIELD_ANGKATAN_ID = "angkatan_id"
        private const val FIELD_STATUS = "status"
        private const val FIELD_REKAP_KELAS = "rekap_kelas"

        private val DEFAULT_ANGKATAN = listOf("@51", "@52", "@53")
    }
}
