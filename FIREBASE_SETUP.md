# Larkam — Firebase Setup

The app uses **Firebase Realtime Database** when configured, but **also builds and runs
without it**:

- **No `google-services.json`** → the app builds and runs on a **local in-memory
  repository** (sample data + working timer/saving, data lasts only per session). The
  Google Services Gradle plugin is skipped automatically.
- **`app/google-services.json` present** → the plugin is applied and the app switches to
  the **real Realtime Database** automatically (`ServiceLocator` detects it at runtime).

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

## 2. Enable Realtime Database
1. Firebase Console → **Build → Realtime Database → Create database**.
2. Pick a location, then **Start in test mode** for development.
3. Make sure `google-services.json` contains the database URL under
   `project_info.firebase_url` (Firebase adds it once the database exists). It looks like:

   ```
   https://<project-id>-default-rtdb.firebaseio.com
   ```

   If you created the DB **after** downloading `google-services.json`, re-download the
   file so the URL is included — otherwise `FirebaseDatabase.getInstance()` throws and the
   app silently falls back to the local repository.

### Development security rules (test only)
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```
> DEV ONLY — lock this down before production.

For faster session filtering you can also index by angkatan:
```json
{
  "rules": {
    ".read": true,
    ".write": true,
    "sesi_larkam": { ".indexOn": ["angkatan_id"] },
    "angkatan": { ".indexOn": ["nama_angkatan"] }
  }
}
```

## 3. Realtime Database data model
The app reads/writes this tree (field names are snake_case in the database):

```
angkatan/
  <pushId>/
    nama_angkatan: "@51"
sesi_larkam/
  <pushId>/
    tanggal:     1722556800000        // epoch millis
    angkatan_id: "<angkatan pushId>"
    status:      "BERJALAN"|"SELESAI"
    rekap_kelas:
      0/
        nama_kelas:        "XII TOI A"
        total_siswa:       30          // jumlah hadir target / roster
        total_hadir:       28          // dihitung live via ketuk kartu
        total_izin:        2
        detail_izin:
          0/ { nama: "...", alasan: "..." }
        waktu_mulai_timer: 1722556801000   // epoch millis, null bila belum mulai
        status_timer:      "BELUM_MULAI"|"BERJALAN"|"SELESAI"
```

- The record **key** (push id) is the `id` / `idSesi`; it is not duplicated as a child.
- On first launch the app **auto-seeds** `@51`, `@52`, `@53` under `angkatan` if that node
  is empty (see `LarkamRepository.ensureAngkatanSeeded()`).
- Each class in `rekap_kelas` has its OWN independent timer (`waktu_mulai_timer` +
  `status_timer`).

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
- Firebase versions are managed by the **firebase-bom**; only
  `com.google.firebase:firebase-database` is pulled in.
