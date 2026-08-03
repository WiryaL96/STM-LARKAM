# Larkam — Firebase Setup

The app uses **Cloud Firestore** when configured, but **also builds and runs without it**:

- **No `google-services.json`** → the app builds and runs on a **local in-memory
  repository** (sample data + working timer/saving, data lasts only per session). The
  Google Services Gradle plugin is skipped automatically.
- **`app/google-services.json` present** → the plugin is applied and the app switches to
  **real Cloud Firestore** automatically (`ServiceLocator` detects it at runtime).

So you can run it right now; follow the steps below only when you want real cloud sync.

## 1. Create the Firebase project
1. Go to <https://console.firebase.google.com> and **Add project** (e.g. `Larkam`).
2. In the project, **Add app → Android**.
3. **Android package name** must be exactly:

   ```
   com.wiryadinata.stmlarkam
   ```

4. Download the generated **`google-services.json`** and place it at:

   ```
   app/google-services.json
   ```

   (`app/google-services.json.example` shows the expected shape.)

## 2. Enable Cloud Firestore
1. Firebase Console → **Build → Firestore Database → Create database**.
2. Start in **test mode** for development, then click through.

### Development security rules (test only)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;   // DEV ONLY — lock this down before production
    }
  }
}
```

## 3. Firestore data model
The app reads/writes these collections (field names are snake_case in Firestore):

### `angkatan`
| field           | type   | example |
|-----------------|--------|---------|
| (document id)   | string | auto    |
| `nama_angkatan` | string | `@51`   |

> On first launch the app **auto-seeds** `@51`, `@52`, `@53` if the collection is empty
> (see `LarkamRepository.ensureAngkatanSeeded()`).

### `sesi_larkam`
| field         | type   | notes |
|---------------|--------|-------|
| (document id) | string | also stored as `idSesi` |
| `tanggal`     | number | epoch millis |
| `angkatan_id` | string | id of an `angkatan` doc |
| `status`      | string | `BERJALAN` while running, `SELESAI` when done |
| `rekap_kelas` | array  | list of objects (below) |

Each element of `rekap_kelas` (each class has its OWN independent timer):
| field               | type      | example |
|---------------------|-----------|---------|
| `nama_kelas`        | string    | `XII TOI A` |
| `total_siswa`       | number    | `30` (jumlah hadir target / roster) |
| `total_hadir`       | number    | `28` (dihitung live via ketuk kartu) |
| `total_izin`        | number    | `2` |
| `detail_izin`       | array     | `[{ "nama": "...", "alasan": "..." }]` |
| `waktu_mulai_timer` | timestamp | kapan timer kelas ini dimulai (null bila belum) |
| `status_timer`      | string    | `BELUM_MULAI` / `BERJALAN` / `SELESAI` |

## 4. Build & run
```
./gradlew :app:assembleDebug
```
Then install on a device/emulator (API 29+).

---

### Toolchain notes (bleeding edge)
- AGP **9.3.1** with **built-in Kotlin** (Kotlin **2.2.10**) — the JetBrains
  `org.jetbrains.kotlin.android` plugin is intentionally **not** applied (it conflicts).
- Jetpack Compose enabled via the `org.jetbrains.kotlin.plugin.compose` plugin (v2.2.10,
  matched to the bundled Kotlin) + `buildFeatures { compose = true }`.
- Kotlin `jvmTarget` is set in the top-level `kotlin { compilerOptions { } }` block
  (there is no `android { kotlinOptions { } }` under built-in Kotlin).
