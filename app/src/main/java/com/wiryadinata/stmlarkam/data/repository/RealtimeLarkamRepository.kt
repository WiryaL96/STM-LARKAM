package com.wiryadinata.stmlarkam.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.RekapKelas
import com.wiryadinata.stmlarkam.data.model.SesiLarkam
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase **Realtime Database**-backed [LarkamRepository].
 *
 * Tree layout:
 * ```
 * angkatan/
 *   <pushId>/ { nama_angkatan }
 * sesi_larkam/
 *   <pushId>/ { tanggal, angkatan_id, status, rekap_kelas: [ ... ] }
 * ```
 *
 * Real-time reads wrap a [ValueEventListener] in a [callbackFlow]; writes use a small
 * [awaitResult] helper so we don't need the coroutines-play-services artifact. Record
 * ids are the push keys, injected onto the deserialized objects from `snapshot.key`.
 */
class RealtimeLarkamRepository(
    db: FirebaseDatabase = FirebaseDatabase.getInstance()
) : LarkamRepository {

    private val angkatanRef: DatabaseReference = db.getReference(NODE_ANGKATAN)
    private val sesiRef: DatabaseReference = db.getReference(NODE_SESI)

    override fun observeAngkatan(): Flow<List<Angkatan>> = callbackFlow {
        val query = angkatanRef.orderByChild(FIELD_NAMA_ANGKATAN)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(Angkatan::class.java)?.apply { id = child.key.orEmpty() }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun observeSesi(angkatanId: String?): Flow<List<SesiLarkam>> = callbackFlow {
        // Filter server-side by angkatan when requested; sort by date client-side so no
        // extra composite index is needed (orderByChild + equalTo already covers it).
        val query: Query = if (angkatanId.isNullOrBlank()) {
            sesiRef
        } else {
            sesiRef.orderByChild(FIELD_ANGKATAN_ID).equalTo(angkatanId)
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { child ->
                        child.getValue(SesiLarkam::class.java)?.apply { idSesi = child.key.orEmpty() }
                    }
                    .sortedByDescending { it.tanggal }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun createSesi(sesi: SesiLarkam): String {
        val ref = sesiRef.push()
        val id = ref.key ?: error("Realtime Database returned a null push key")
        // idSesi is @Exclude'd from serialization; the push key IS the id.
        ref.setValue(sesi.copy(idSesi = id)).awaitResult()
        return id
    }

    override suspend fun updateSesi(idSesi: String, status: String, rekapKelas: List<RekapKelas>) {
        // updateChildren serializes the RekapKelas POJOs (incl. waktu_mulai_timer +
        // status_timer) and writes status + rekap_kelas in one atomic update.
        sesiRef.child(idSesi)
            .updateChildren(
                mapOf(
                    FIELD_STATUS to status,
                    FIELD_REKAP_KELAS to rekapKelas
                )
            )
            .awaitResult()
    }

    override suspend fun ensureAngkatanSeeded() {
        val existing = angkatanRef.get().awaitResult()
        if (existing.hasChildren()) return
        DEFAULT_ANGKATAN.forEach { nama ->
            angkatanRef.push().setValue(mapOf(FIELD_NAMA_ANGKATAN to nama)).awaitResult()
        }
    }

    /**
     * Suspends until the [Task] completes, without pulling in play-services-tasks-ktx.
     *
     * A [WRITE_TIMEOUT_MS] guard turns an unreachable database (wrong URL/region, or the
     * database not created yet) into a clear error instead of an endless "menyimpan data"
     * spinner — with persistence on, the write itself stays queued and flushes once the
     * connection is fixed.
     */
    private suspend fun <T> Task<T>.awaitResult(): T = try {
        withTimeout(WRITE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
                addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
                addOnCanceledListener { cont.cancel() }
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw IOException(
            "Timeout menyimpan ke Realtime Database. Cek koneksi internet dan URL/region " +
                "database di google-services.json.",
            e
        )
    }

    companion object {
        /** Fail a pending read/write after this long so the UI can show an error + retry. */
        private const val WRITE_TIMEOUT_MS = 15_000L

        const val NODE_ANGKATAN = "angkatan"
        const val NODE_SESI = "sesi_larkam"

        private const val FIELD_NAMA_ANGKATAN = "nama_angkatan"
        private const val FIELD_ANGKATAN_ID = "angkatan_id"
        private const val FIELD_STATUS = "status"
        private const val FIELD_REKAP_KELAS = "rekap_kelas"

        private val DEFAULT_ANGKATAN = listOf("@51", "@52", "@53")
    }
}
