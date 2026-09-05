# Sleep Timer Tile

An Android app whose entire purpose is a **Quick Settings tile**. Tap the tile,
a countdown starts; when it expires the phone pauses whatever media is playing
and locks/turns off the screen. Built for falling asleep to TV shows without
having to open an app first.

- **Package:** `com.jwilgeroth.sleeptimertile`
- **Min SDK:** 26 (Android 8.0). **Target/compile SDK:** 37.
- **UI:** Jetpack Compose (settings screen only). Kotlin, Gradle Kotlin DSL.
- **Distribution:** GitHub Releases → installed via Obtainium. Not on Google Play.
- **No network.** No INTERNET permission, no analytics, no crash reporting.

---

## Where things live

| What | File |
|------|------|
| The tile (its logic) | `app/src/main/java/com/jwilgeroth/sleeptimertile/SleepTimerTileService.kt` |
| Tile icon | `app/src/main/res/drawable/ic_tile_sleep.xml` |
| Placeholder / (later) settings screen | `app/src/main/java/com/jwilgeroth/sleeptimertile/MainActivity.kt` |
| Manifest (declares the tile) | `app/src/main/AndroidManifest.xml` |
| App module build config | `app/build.gradle.kts` |
| Dependency versions | `gradle/libs.versions.toml` |

## How a Quick Settings tile works (the one-paragraph version)

A `TileService` is a service the **system** binds to, but only while the tile is
visible in the pulled-down shade. You never start it; the OS does. That's why
the app can sit idle consuming nothing. The tile is declared in the manifest
with `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"` (meaning
"only the OS may bind me") and an intent-filter for
`android.service.quicksettings.action.QS_TILE` (how Android discovers it's a
tile). Because the tile is only "live" when the shade is open, you can only
redraw its label/state inside `onStartListening()`.

## Build & run (device)

1. Open the project in Android Studio.
2. Plug in the phone with USB debugging enabled; pick it in the device dropdown.
3. Press **Run** (▶). To add the tile: pull down the shade fully → pencil/Edit →
   drag **Sleep Timer** into the active tiles.

Command line (once the Gradle wrapper is present):
```
./gradlew installDebug     # build + install the debug APK to the connected phone
```

---

## Build phases (progress log)

- [x] **Phase 1 — Prove the pipeline.** Tile appears in Quick Settings; tapping
      it shows a toast.
- [x] **Phase 2 — Timer + screen off.** Exact alarm + Device Admin `lockNow()`.
      Tile toggles idle/active. Also added a custom ongoing countdown
      notification (big Chronometer, moon branding) and a moon launcher icon.
- [x] **Phase 3 — Media pause.** Media-key fallback (no permission) + optional
      Notification Listener path for precise media-session pause.
- [x] **Phase 4 — Settings screen.** Duration picker, permission status, toggles,
      10-second test button, persisted with DataStore. Also: long-pressing the
      tile opens a floating preset picker (`DurationPickerActivity`, registered
      for `QS_TILE_PREFERENCES`) that starts a timer of the chosen length.
- [ ] **Phase 5 — Polish.** Countdown in the tile label, Samsung battery deep
      links, foldable layout check.
- [ ] **Phase 6 — Release pipeline.** Signing keystore, GitHub Actions signed
      release on tag, Obtainium instructions.

## Releasing a new version

1. Bump the version in `app/build.gradle.kts` — increase **`versionCode`** by 1
   (Android refuses to install an update whose versionCode isn't higher) and set
   a human `versionName` (e.g. `"1.1"`).
2. Commit, then tag and push:
   ```
   git commit -am "Release 1.1"
   git tag v1.1
   git push && git push --tags
   ```
3. Pushing the `v1.1` tag triggers `.github/workflows/release.yml`, which builds
   a **signed** APK and attaches it to a new GitHub Release.
4. Obtainium (pointed at this repo) sees the new release and offers the update.

### Signing
- The keystore is `release-keystore.jks` — **git-ignored, back it up yourself.**
  Losing it means you can never update the app again.
- Local release build: `./gradlew :app:assembleRelease` (reads
  `keystore.properties`).
- CI reads the key from GitHub **secrets**: `RELEASE_KEYSTORE_BASE64` (the .jks
  base64-encoded), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`.

## Design decisions worth remembering

- **Exact alarms via `USE_EXACT_ALARM`, not `SCHEDULE_EXACT_ALARM`** (planned for
  Phase 2). Since this app is sideloaded (never Google Play), `USE_EXACT_ALARM`
  is auto-granted at install with no special-access settings page. The alarm —
  not a coroutine/handler/foreground loop — is the source of truth for firing,
  so it survives Doze.
- **The tile's live-ticking countdown label is partly cosmetic.** A tile can
  only be redrawn while the shade is open (`onStartListening`). The alarm still
  fires exactly on time regardless.
- **Turning the screen off requires Device Admin** (`force-lock` + `lockNow()`).
  There is no lighter non-root API. ⚠️ **Device Admin must be disabled before the
  app can be uninstalled** — Settings → Security → Device admin apps.
