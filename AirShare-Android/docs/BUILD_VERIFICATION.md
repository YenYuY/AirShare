# Build verification

Verified locally on 2026-07-12 (Asia/Taipei).

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

- JVM unit tests: 55 passed, 0 failed, 0 skipped, including compact, medium, expanded, large, and compact-height adaptive layout policy checks.
- Android Lint: 0 errors, 15 warnings. The remaining warnings are dependency/tool version notices, the intentional `targetSdk 35` selection, an upstream Bouncy Castle trust-manager detector warning, and an adaptive-icon resource qualifier warning.
- Debug APK: 45 MB, SHA-256 `eff4e1dae72177b5f8770797df669ed0fc9769a96fc83275960e00c7d23f30be`.
- Release AAB: 4.3 MB, SHA-256 `60060a73d99b212e254f9d3321614e40761c1fcd9aa7f88368ac2ee1b9506366`.
- Material 3 Adaptive Window Size Classes drive runtime layout changes for phone, tablet portrait, tablet landscape, short-height windows, and desktop-size windows. Four matching Android Studio previews compile in the debug source set.
- The arm64 native libraries in the AAB use `2**14` (`16 KiB`) ELF `LOAD` alignment.
- The current release AAB is intentionally unsigned because no private upload key has been supplied. Configure `keystore.properties` and rebuild before uploading to Google Play.

## Runtime test status

`adb devices -l` detected no connected device, and no local AVD is configured. Build, static, protocol, lifecycle, and packaging checks are complete; installation, browser download, permission UI, QR scanning, and background-stop behavior still need one Android 10+ physical-device pass before production submission.
