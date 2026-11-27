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

- **Speed:** automatic. Tap the title screen to start at 60 km/h; the car
  speeds up as the run goes on, up to 140 km/h.
- **Steer:** drag anywhere on the screen.
- **Siren:** tap the police car.
- After a crash, tap to drive again. Your best distance is saved.
- **Music:** the ♪ button on the title and crash screens turns it on or off.

### Getting the APK

Every push to `master` publishes a signed release APK as a GitHub Release.
The latest one is always at:

**https://github.com/gabrielgaraujo/highway-patrol/releases/latest/download/highway-patrol.apk**

Open it on your phone to install, or to update over an earlier release.
(You'll need to allow installing apps from your browser or Files app.)
To publish a release from another branch, run the **Android** workflow by hand
from the Actions tab.

Releases are signed with a private key kept in two repository secrets,
`SIGNING_KEYSTORE_BASE64` (the base64 keystore) and `SIGNING_PASSWORD`.
Keep a backup of the keystore: if it's lost, new releases can't update an
installed copy. Pull requests and other pushes also build the release APK and
attach it to the workflow run as an artifact.

### Music

`assets/sounds/music.ogg` is an original chiptune loop rendered by
[`tools/make_music.py`](tools/make_music.py) (needs numpy and ffmpeg). Edit the
chords, melody or tempo there and run `python3 tools/make_music.py` to re-render.

### Building locally

Open `android/` in Android Studio, or with the Android SDK installed:

```sh
cd android
./gradlew testDebugUnitTest assembleDebug  # debug build, signed with android/app/debug.keystore
adb install app/build/outputs/apk/debug/app-debug.apk
```
