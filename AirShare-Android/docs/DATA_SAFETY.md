# Google Play Data Safety Working Sheet

Checked against the official [Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469) and [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311) on 2026-07-11.

This is a Play Console working sheet, not a substitute for checking the final AAB. Submit these answers only while every assumption below remains true.

## Release assumptions

- No user account or sign-in.
- No developer-operated backend, cloud relay, or cloud storage.
- No analytics, advertising, attribution, telemetry, or remote crash-reporting SDK.
- Photos, videos, thumbnails, and metadata are accessed and processed on the Android device.
- A transfer is a specific user-initiated action between the Android device and a browser on the same local network.
- Transfer content travels directly over local HTTP. It is not sent to the developer, but it is not encrypted in transit and may be observable by other parties on an untrusted network.
- The sharing token is random, session-scoped, processed locally, not sent to the developer, and discarded when sharing stops.
- Local IP addresses and connection state are processed locally and are not sent to the developer.

## Suggested Play Console answers

### Data collection and sharing

**Does your app collect or share any of the required user data types?**

Suggested answer: **No**.

Reasoning under Google's definitions:

- Media-library access that stays on the device is on-device processing and is not collected.
- Direct transfer content is a user-initiated transfer to the recipient and is not sent to the developer.
- A download is initiated by the user for an expected recipient and is not disclosed as developer sharing.
- No SDK or developer endpoint receives data from the app.

This conclusion changes if the developer can decrypt content, operates a relay, receives logs or metadata, or adds any SDK that sends data off-device.

### Security practices

If Play Console presents an encryption-in-transit question for this transfer path, answer **No** while the release uses local cleartext HTTP.

### Account creation and deletion

- **Does the app allow users to create an account?** No.
- No account-deletion URL is required while the app has no account system.
- The privacy policy must still explain retention and deletion, even when no developer-held user data exists.

## Data-type audit

| Play data type | Suggested status | Basis |
| --- | --- | --- |
| Photos | Not collected or shared | Accessed locally and transferred only by the user's action to the intended recipient; not sent to the developer. |
| Videos | Not collected or shared | Accessed locally and transferred only by the user's action to the intended recipient; not sent to the developer. |
| Files and documents | Not collected or shared | Not part of the current product; reassess if arbitrary-file sharing is added. |
| Device or other IDs | Not collected or shared | No advertising ID, persistent device identifier, or developer telemetry. |
| App interactions | Not collected or shared | No analytics SDK or backend. |
| Crash logs | Not collected or shared | No remote crash-reporting service. Local system diagnostics do not go to the developer through the app. |
| Diagnostics | Not collected or shared | Operational status remains local to the active session. |
| Approximate or precise location | Not collected or shared | No location data is requested or derived for developer use. |
| Personal information | Not collected or shared | No account, profile, name, email, or phone-number flow. |

## Final AAB verification

- [ ] Inspect the merged manifest for unexpected permissions, especially `AD_ID`, location, contacts, `QUERY_ALL_PACKAGES`, or `MANAGE_EXTERNAL_STORAGE`.
- [ ] Inspect dependencies and SDK manifests for analytics, ads, attribution, diagnostics, or network collection.
- [ ] Capture network traffic during launch, permission handling, an idle session, browsing, download, stop, and error flows. Confirm there is no developer or third-party endpoint.
- [ ] Confirm the initial page, thumbnails, and original-media downloads remain restricted by the active session token.
- [ ] Confirm the temporary token and local peer addresses are not written to remote logs.
- [ ] Confirm the privacy policy, in-app disclosure, permission declaration, and Data Safety answers describe the same release behavior.
- [ ] Submit the completed Data Safety form for every closed, open, or production track. An internal-test-only app is exempt until it moves to one of those tracks.

## Changes that require a new assessment

Update this sheet, the Play Console form, and the privacy policy before adding any of the following:

- Firebase Analytics, Crashlytics, ads, attribution, or remote logging;
- user accounts, email support embedded in the app, or cloud preferences;
- a developer-operated relay, remote-access service, link service, or cloud backup;
- a transfer path that the developer or an intermediary can decrypt;
- persistent transfer history or peer identifiers sent off-device;
- arbitrary files, contacts, installed-app discovery, or location-derived Wi-Fi scanning; or
- any new third-party SDK that transmits user or device data.

## Play Console actions requiring user values

- [ ] `[REQUIRED: PUBLIC HTTPS PRIVACY POLICY URL]`
- [ ] `[REQUIRED: PRIVACY EMAIL]`
- [ ] `[REQUIRED: LEGAL DEVELOPER OR COMPANY NAME]`
- [ ] Keep a dated copy of the submitted Data Safety answers with the release record.
