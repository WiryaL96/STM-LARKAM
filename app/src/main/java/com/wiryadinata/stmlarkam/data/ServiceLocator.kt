package com.wiryadinata.stmlarkam.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.wiryadinata.stmlarkam.data.repository.FirestoreLarkamRepository
import com.wiryadinata.stmlarkam.data.repository.LarkamRepository
import com.wiryadinata.stmlarkam.data.repository.LocalLarkamRepository

/**
 * Minimal manual dependency container.
 *
 * Picks the data source automatically:
 *  - If a default [FirebaseFirestore] is available (i.e. google-services.json is present
 *    and Firebase initialized), it uses [FirestoreLarkamRepository].
 *  - Otherwise it falls back to [LocalLarkamRepository] so the app still runs.
 */
object ServiceLocator {

    val repository: LarkamRepository by lazy {
        runCatching { FirebaseFirestore.getInstance() }
            .fold(
                onSuccess = { firestore ->
                    Log.i(TAG, "Using FirestoreLarkamRepository (Firebase configured).")
                    FirestoreLarkamRepository(firestore)
                },
                onFailure = { error ->
                    Log.w(TAG, "Firebase not configured (${error.message}); using LocalLarkamRepository.")
                    LocalLarkamRepository()
                }
            )
    }

    private const val TAG = "Larkam"
}
