# Google Play Release Checklist

Checked against official Android and Google Play requirements on 2026-07-11.

Labels:

- `[USER VALUE]` must be supplied by the developer or account owner.
- `[PLAY CONSOLE]` must be completed in Google Play Console.
- `[VERIFY]` must be checked against the final release bundle, not only source code.

## Release identity

- [ ] `[USER VALUE]` Confirm the permanent application ID before the first upload: `com.yenyuyyy.airshare.android`.
- [ ] `[USER VALUE]` Confirm the public developer name/legal entity. It must also appear in the privacy policy: `[REQUIRED: LEGAL DEVELOPER OR COMPANY NAME]`.
- [ ] Keep `versionCode` positive, unique, and greater than every previously uploaded build. Google Play's maximum is `2100000000`.
- [ ] Keep the same application ID and signing identity for every update.
- [ ] `[PLAY CONSOLE]` Create the app and accept the Play App Signing terms.
- [ ] Back up the upload keystore, alias, and passwords outside the repository.
- [ ] Record the Play app-signing SHA-256 certificate fingerprint for any future API or App Link configuration.

## Build and bundle

- [ ] Build and upload an Android App Bundle (`.aab`), not a production APK.
- [ ] `[VERIFY]` Upload the release AAB to an internal test and resolve every Play pre-review error.
- [ ] `[VERIFY]` Confirm the final bundle targets at least Android 15 / API 35. This project currently uses `targetSdk = 35`.
- [ ] Recheck the [official target API table](https://developer.android.com/google/play/requirements/target-sdk) immediately before submission. As of 2026-07-11, the exact published minimum for new phone apps is API 35; an API 36 enforcement date is not yet listed.
- [ ] `[VERIFY]` Confirm 64-bit support. If the AAB contains any native 32-bit library, include its corresponding 64-bit ABI.
- [ ] `[VERIFY]` Confirm 16 KB page-size compatibility in Play Console. All new apps targeting Android 15+ are in scope.
- [ ] Run the release build with minification enabled and verify that QR, session-token generation, media browsing, and localization still work.

## Network and security

- [ ] Confirm the UI and privacy disclosures clearly state that local HTTP transfers are not encrypted and require a trusted private Wi-Fi network.
- [ ] Confirm the sharing token is random, scoped to one active session, and invalidated when sharing stops or the app process ends.
- [ ] Confirm no developer-operated backend, relay, analytics endpoint, ad endpoint, or crash-reporting endpoint receives user data.
- [ ] Test on an untrusted-LAN model: another device without the active token must not browse or download media.
- [ ] Make sharing state unmistakable and let the user stop the server immediately.
- [ ] Test screen lock, app backgrounding, network changes, Wi-Fi loss, process death, and token expiry.
- [ ] Current target API 35: test LAN TCP, HTTP, and service discovery with the normal `INTERNET` permission.
- [ ] Android 17 / target API 37 migration: before raising `targetSdk` to 37, declare and request `ACCESS_LOCAL_NETWORK` at runtime at the moment sharing starts, handle denial/revocation, and retest outgoing and incoming TCP plus UDP/mDNS. A system-mediated NSD picker may avoid broad LAN permission for a single selected service.

## Media permissions

- [ ] Keep broad media access limited to the user-facing core feature: browsing and sharing the user's photo/video library.
- [ ] Request `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` only in context, after explaining why the library is needed.
- [ ] Support Android 14 selected-media access and reselection with `READ_MEDIA_VISUAL_USER_SELECTED`.
- [ ] For Android 12L and lower, keep `READ_EXTERNAL_STORAGE` capped at `maxSdkVersion = 32`.
- [ ] Do not request `MANAGE_EXTERNAL_STORAGE`.
- [ ] Provide a usable reduced-access path when broad access is denied, such as selected-media access or the Android Photo Picker.
- [ ] `[PLAY CONSOLE]` Submit the photo/video permission declaration and review video described in [MEDIA_PERMISSION_DECLARATION.md](MEDIA_PERMISSION_DECLARATION.md).
- [ ] Be prepared to remove broad media permissions and use the system Photo Picker if Google determines that the persistent full-library access is not sufficiently necessary for core functionality.

## Privacy and Data Safety

- [ ] Replace every `[REQUIRED: ...]` placeholder in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
- [ ] `[USER VALUE]` Public privacy policy URL: `[REQUIRED: PUBLIC HTTPS PRIVACY POLICY URL]`.
- [ ] `[USER VALUE]` Public support URL: `[REQUIRED: PUBLIC HTTPS SUPPORT URL]`.
- [ ] `[USER VALUE]` Support email: `[REQUIRED: SUPPORT EMAIL]`.
- [ ] `[USER VALUE]` Privacy contact email: `[REQUIRED: PRIVACY EMAIL]`.
- [ ] Publish the privacy policy at an active, public, non-geofenced, non-editable HTTPS page. Do not use a PDF.
- [ ] Add a Privacy Policy link or text inside the app.
- [ ] `[PLAY CONSOLE]` Complete Data Safety using [DATA_SAFETY.md](DATA_SAFETY.md), then verify the answers against the final AAB and every bundled SDK.
- [ ] `[PLAY CONSOLE]` Declare that the app has no account creation. No account-deletion URL is required while that remains true.
- [ ] Update the privacy policy and Data Safety form before adding analytics, ads, accounts, cloud relay, remote logging, or any developer-accessible transfer service.

## App content and review

- [ ] `[PLAY CONSOLE]` Ads declaration: `No, my app does not contain ads`—confirm this remains true in the final AAB.
- [ ] `[PLAY CONSOLE]` Complete the content-rating questionnaire accurately.
- [ ] `[PLAY CONSOLE]` Select the target audience. Avoid selecting children unless the app and all SDKs are intentionally designed to comply with the Families policy.
- [ ] `[PLAY CONSOLE]` Complete App access. AirShare has no login or paywall; add reviewer instructions explaining the same-Wi-Fi browser workflow.
- [ ] Give reviewers reusable English instructions: grant media access, start sharing, open the displayed HTTP URL or QR code from another device on the same trusted LAN, browse media, download an item, then stop sharing.
- [ ] If review requires a QR code or other non-alphanumeric resource, provide it through a static URL that remains valid throughout review.
- [ ] Ensure the core workflow is not dependent on a particular country, account, one-time password, or developer-controlled device.
- [ ] `[PLAY CONSOLE]` Choose the App category and relevant tags.
- [ ] `[PLAY CONSOLE]` Complete export-law and developer policy declarations.

## Store listing

- [ ] Upload localized text from `play/listing/en-US`, `play/listing/zh-TW`, and `play/listing/ja-JP`.
- [ ] Confirm each localized title is at most 30 characters, short description at most 80, and full description at most 4,000.
- [ ] Upload a 512 × 512 32-bit PNG app icon, no larger than 1,024 KB.
- [ ] Upload a 1024 × 500 JPEG or 24-bit PNG feature graphic without alpha.
- [ ] Upload at least two accurate phone screenshots. For stronger promotional eligibility, provide at least four screenshots at 1080p or higher.
- [ ] Ensure screenshots show the current app and do not imply cloud transfer, remote transfer, accounts, or features that are not implemented.
- [ ] Add alt text for graphic assets where Play Console offers it.
- [ ] `[USER VALUE]` Add the required support email and preferably the support website to Store settings.

## Testing and rollout

- [ ] `[PLAY CONSOLE]` Determine whether the developer account is a personal account created after 2023-11-13.
- [ ] If it is, complete a closed test with at least 12 opted-in testers continuously for 14 days, then apply for production access.
- [ ] If it is a new personal account, verify access to a non-rooted physical Android 10+ device through the Play Console mobile app.
- [ ] Test on at least Android 10 / API 29, Android 13, Android 14 with partial media access, Android 15, Android 16, and Android 17.
- [ ] Review the Play pre-launch report for stability, compatibility, performance, and accessibility findings.
- [ ] Verify Traditional Chinese, English, and Japanese layouts with large font sizes and TalkBack.
- [ ] Use an internal or closed release before production and test the Play-delivered build, not only a locally installed APK.
- [ ] Start production with a controlled rollout and monitor Android vitals and user feedback.

## Announced 2026 developer verification

- [ ] `[PLAY CONSOLE]` Complete developer identity verification and inspect package-name registration status.
- [ ] Prepare before September 2026 enforcement in Brazil, Indonesia, Singapore, and Thailand. Google Play will attempt to register eligible Play package names automatically, but the account owner must confirm the resulting status.

## Official references

- [Target API level](https://developer.android.com/google/play/requirements/target-sdk)
- [Publish with Android App Bundles](https://developer.android.com/studio/publish/)
- [Play App Signing](https://developer.android.com/studio/publish/app-signing)
- [Application ID](https://developer.android.com/build/configure-app-module)
- [App versioning](https://developer.android.com/studio/publish/versioning)
- [64-bit support](https://developer.android.com/google/play/requirements/64-bit)
- [16 KB page-size support](https://developer.android.com/guide/practices/page-sizes)
- [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission)
- [Network Security Configuration](https://developer.android.com/privacy-and-security/security-config)
- [Photo/video permission policy](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Store listing assets](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Testing requirement for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Developer verification](https://support.google.com/android-developer-console/answer/16561738)
