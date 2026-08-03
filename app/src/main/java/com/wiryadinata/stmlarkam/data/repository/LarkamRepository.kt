package com.wiryadinata.stmlarkam.data.repository

import com.wiryadinata.stmlarkam.data.model.Angkatan
import com.wiryadinata.stmlarkam.data.model.RekapKelas
import com.wiryadinata.stmlarkam.data.model.SesiLarkam
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the Larkam data source. The production implementation is
 * [FirestoreLarkamRepository]; keeping the interface makes the ViewModels
 * testable and the data source swappable.
 */
interface LarkamRepository {

    /** Real-time stream of the available angkatan (@51, @52, @53, ...). */
    fun observeAngkatan(): Flow<List<Angkatan>>

    /**
     * Real-time stream of sessions. When [angkatanId] is null, every session is
     * returned; otherwise only sessions belonging to that angkatan.
     */
    fun observeSesi(angkatanId: String?): Flow<List<SesiLarkam>>

    /** Creates a new session document and returns its generated id. */
    suspend fun createSesi(sesi: SesiLarkam): String

    /**
     * Updates an existing session's status and class recap. Used both for intermediate
     * syncs (a class timer started/expired) and for finalizing the session (SELESAI).
     */
    suspend fun updateSesi(idSesi: String, status: String, rekapKelas: List<RekapKelas>)

    /**
     * Seeds the default angkatan (@51, @52, @53) if the collection is empty, so
     * the app is usable on a fresh Firebase project.
     */
    suspend fun ensureAngkatanSeeded()
}
