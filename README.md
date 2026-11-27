# Highway Patrol

Drive the police car down the highway and dodge the traffic.

## Desktop (Python)

```sh
pip install pygame
python3 highway_patrol.py
```

Arrow keys to drive, click to toggle the siren.

## Android

The Kotlin port lives in [`android/`](android/) and uses the same sprites and
sound from [`assets/`](assets/).

- **Gas / Brake:** hold the pedals at the bottom of the screen.
- **Steer:** drag anywhere else (steering gets sharper as you speed up).
- **Siren:** tap the police car.
- After a crash, tap to drive again. Your best distance is saved.

### Getting the APK

Every push that touches `android/` or `assets/` runs the **Android** GitHub
Actions workflow, which runs the unit tests and builds a debug APK. Download
it from the workflow run's **Artifacts** section
(`highway-patrol-debug-apk`), unzip it, and install `app-debug.apk` on your
phone (you'll need to allow installing apps from unknown sources).

### Building locally

Open `android/` in Android Studio, or with the Android SDK installed:

```sh
cd android
./gradlew testDebugUnitTest assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```
