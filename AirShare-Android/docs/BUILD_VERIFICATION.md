# Build verification

Verified locally on 2026-07-11 (Asia/Taipei).

## Toolchain

- Android Studio 2026.1 at `/Applications/Android Studio.app`
- Android Studio JBR / OpenJDK 21.0.10
- Gradle 9.4.1
- Android Gradle Plugin 9.2.1
- `minSdk 29`, `compileSdk 36`, `targetSdk 35`

## Final verification command

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

Result: `BUILD SUCCESSFUL`.

## Results

- JVM unit tests: 51 passed, 0 failed, 0 skipped.
- Android Lint: 0 errors, 15 warnings. The remaining warnings are dependency/tool version notices, the intentional `targetSdk 35` selection, an upstream Bouncy Castle trust-manager detector warning, and an adaptive-icon resource qualifier warning.
- Debug APK: 39 MB, SHA-256 `d52b5726494585eb4bcd91fbc8fe40f8bab707e3aa048c6fd304beef40044861`.
- Release AAB: 4.4 MB, SHA-256 `15cbec9337c58d6fd9c14b71e09fc2824a5ed363424260c73a45b2bce25cd99a`.
- The arm64 native libraries in the AAB use `2**14` (`16 KiB`) ELF `LOAD` alignment.
- The current release AAB is intentionally unsigned because no private upload key has been supplied. Configure `keystore.properties` and rebuild before uploading to Google Play.

## Runtime test status

`adb devices -l` detected no connected device, and no local AVD is configured. Build, static, protocol, lifecycle, and packaging checks are complete; installation, browser download, permission UI, QR scanning, and background-stop behavior still need one Android 10+ physical-device pass before production submission.
