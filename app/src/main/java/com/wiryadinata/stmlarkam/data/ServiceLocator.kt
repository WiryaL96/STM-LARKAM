package com.wiryadinata.stmlarkam.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.wiryadinata.stmlarkam.data.repository.LarkamRepository
import com.wiryadinata.stmlarkam.data.repository.LocalLarkamRepository
import com.wiryadinata.stmlarkam.data.repository.RealtimeLarkamRepository

/**
 * Minimal manual dependency container.
 *
 * Picks the data source automatically:
 *  - If a [FirebaseDatabase] instance can be resolved (google-services.json present and
 *    Firebase initialized), it uses [RealtimeLarkamRepository].
 *  - Otherwise it falls back to [LocalLarkamRepository] so the app still runs.
 */
object ServiceLocator {

    val repository: LarkamRepository by lazy {
        runCatching { realtimeDatabase() }
            .fold(
                onSuccess = { database ->
                    // Offline cache so the app keeps working with a flaky connection. Must
                    // be set before any reference is created; ignore if already initialized.
                    runCatching { database.setPersistenceEnabled(true) }
                    Log.i(TAG, "Using RealtimeLarkamRepository (Realtime Database configured).")
                    RealtimeLarkamRepository(database)
                },
                onFailure = { error ->
                    Log.w(TAG, "Realtime Database not configured (${error.message}); using LocalLarkamRepository.")
                    LocalLarkamRepository()
                }
            )
    }

    /**
     * Resolves the Realtime Database instance.
     *
     * Prefers the URL baked into google-services.json (`FirebaseOptions.databaseUrl`). If
     * that's missing — common when the database was created AFTER downloading
     * google-services.json — it derives the project's DEFAULT instance URL from the
     * project id (works for the default US region; other regions must supply
     * `firebase_url` in google-services.json). Throws if Firebase isn't initialized at
     * all, letting the caller fall back to the local repository.
     */
    private fun realtimeDatabase(): FirebaseDatabase {
        val options = FirebaseApp.getInstance().options
        val configuredUrl = options.databaseUrl?.takeIf { it.isNotBlank() }
        if (configuredUrl != null) {
            return FirebaseDatabase.getInstance()
        }
        val projectId = options.projectId?.takeIf { it.isNotBlank() }
            ?: error("Firebase projectId missing; cannot derive a Realtime Database URL")
        val derivedUrl = "https://$projectId-default-rtdb.firebaseio.com"
        Log.w(
            TAG,
            "google-services.json has no firebase_url; falling back to default $derivedUrl. " +
                "If your database is in another region, add firebase_url to google-services.json."
        )
        return FirebaseDatabase.getInstance(derivedUrl)
    }

    private const val TAG = "Larkam"
}
