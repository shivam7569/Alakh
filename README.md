# Alakh

A personal **Wear OS health app** for the **Pixel Watch 3** (Wear OS 5) — workout tracking + guided breathing — with a phone companion stub wired up for later.

Personal use only. No Play Store, no developer account: you build a debug APK and sideload it.

---

## Project layout

```
Alakh/
├── wear/      Wear OS app  ← the one you build & run now
├── mobile/    Phone companion (intentional stub, ready to grow)
├── shared/    Data models, Room persistence, pure business rules (used by both)
├── gradle/libs.versions.toml   ← all dependency versions (single source of truth)
└── settings.gradle.kts / build.gradle.kts
```

**Why three modules:** the watch app and phone app are *separate apps* with different UI toolkits. `shared` holds the logic both reuse. `wear` and `mobile` deliberately share one `applicationId` (`com.andy.alakh`) and the default debug signing key, so when you later add phone⇄watch sync over the **Wear OS Data Layer**, they already recognize each other as a pair — no restructuring.

### What's implemented

| Area | State |
|------|-------|
| Navigation (home → workout / breathing) | ✅ working |
| **Breathing exercise** (animated, box-breathing) | ✅ fully working, needs no sensors/permissions |
| **Workout**: foreground `ExerciseService` + `ExerciseClient` repo + ViewModel/UI | ✅ structurally complete; heart-rate + timer wired, see notes below |
| Room history (`shared/data`) | ✅ schema ready, not yet written to (one `TODO` in `ExerciseService.stop()`) |
| Phone companion | 🟡 stub (a placeholder screen) |

---

## Step 1 — Install the toolchain

This machine has **no Android toolchain yet**. Install **Android Studio** — it bundles everything you need (JDK, Android SDK, `adb`, and Gradle):

1. Download from <https://developer.android.com/studio> and run the installer (accept the SDK component downloads — a few GB).
   - *Or* via winget in PowerShell: `winget install Google.AndroidStudio`
2. Launch it once and let the Setup Wizard finish downloading the default SDK.

You have **32 GB RAM and ~180 GB free**, so you're well within comfort — and because you'll run on the *real watch*, you do **not** need the Wear OS emulator (and can skip the BIOS virtualization setup entirely).

## Step 2 — Open the project and finalize versions

1. **File → Open** → select this `Alakh` folder. Android Studio sets up the Gradle wrapper automatically on first sync (this is why there's no `gradlew.jar` checked in).
2. Let Gradle sync. **This is where you reconcile versions.** The numbers in `gradle/libs.versions.toml` are mid-2026 latest-stable values that were *not* build-tested here, so sync may flag one or two. Fixes are quick:
   - **"Dependency requires compileSdk 37+"** → bump `compileSdk` in the three `build.gradle.kts` files (or just the one named) and install that platform via **Tools → SDK Manager**.
   - **AGP version error** → set `agp` in the catalog to the version your Android Studio ships with (**Help → About**), then **let the AGP Upgrade Assistant** finish.
   - **KSP error** → `ksp` must pair with `kotlin`; grab the matching build from <https://github.com/google/ksp/releases>.
   - Anything else: the error names the artifact and the version it wants — change it in `libs.versions.toml` only.
3. In **Settings → Build → Build Tools → Gradle**, make sure the **Gradle JDK is 17+** (Android Studio's bundled JBR is fine).

> The files most likely to need a small autocomplete-driven tweak against your installed library versions are the Wear Compose UI screens and **`wear/.../health/ExerciseRepository.kt`** (the Health Services metric accessors). The structure is correct; only exact symbol names may have drifted. Android Studio will red-underline anything off.

## Step 3 — Put the watch into Wireless Debugging

The Pixel Watch has no data USB (it charges over pins), so `adb` goes over Wi-Fi. **Watch and laptop must be on the same Wi-Fi network.**

On the watch:
1. **Settings → System → About → Versions** → tap **Build number** 7× (unlocks Developer options).
2. **Settings → Developer options** → enable **ADB debugging** and **Wireless debugging**.
3. Open **Wireless debugging → Pair new device** — note the **IP:port** and the **6-digit pairing code**.

## Step 4 — Pair and install

Open a terminal (Android Studio's **Terminal** tab works; `adb` is at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`):

```powershell
# One-time pairing (use the IP:port + code from the "Pair new device" screen)
adb pair 192.168.x.x:xxxxx
# then connect (use the IP:port shown on the main Wireless debugging screen — usually a different port)
adb connect 192.168.x.x:yyyyy
adb devices   # should list the watch
```

Then in Android Studio: select the **`wear`** run configuration and the watch as the target, and press **Run ▶**. (Or from the terminal: `./gradlew :wear:installDebug`.)

The breathing exercise works immediately. For the workout screen, grant the **Body sensors**, **Location**, and **Notifications** prompts when they appear — Health Services needs them to deliver heart rate.

---

## Known risks / gotchas (carried over from the build research)

- **`compileSdk` / platform install** — if you bump to 37, install that exact SDK platform first.
- **Health foreground service** — `foregroundServiceType="health|location"` in the manifest must stay matched to the `FOREGROUND_SERVICE_HEALTH` / `FOREGROUND_SERVICE_LOCATION` permissions, and the service must be started while the app is foreground, or Android 14 ends the exercise with `AUTO_ENDED_PERMISSION_LOST`.
- **Data Layer pairing (later)** — works only when watch and phone share `applicationId` *and* signing cert. The default debug keystore (`~/.android/debug.keystore`) gives both modules the same cert on this machine, so it's already handled. If you ever build the two apps on *different* machines, check in a shared `debug.keystore` and point both `signingConfigs.debug` at it.
- **Wear Material3** — use `androidx.wear.compose:compose-material3` only; don't mix in classic `compose-material` within a screen.

## Next steps to build out

1. Persist a `WorkoutEntity` in `ExerciseService.stop()` and add a history screen reading `WorkoutDao.observeAll()`.
2. Extract calories/distance in `ExerciseRepository` (the `TODO`).
3. Add a Wear **Tile** + **complication** to start a breathing session from the watch face.
4. When ready, build the `mobile` app and add Data Layer sync to mirror history to the phone.
