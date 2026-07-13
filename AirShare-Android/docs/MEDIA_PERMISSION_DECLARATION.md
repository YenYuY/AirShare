# Photo and Video Permission Declaration

Use this document to prepare the Google Play Console declaration for `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`.

Official policy basis: apps targeting Android 13+ may request broad photo/video access only when a system picker is insufficient for legitimate, persistent or frequent access required by core functionality. A custom picker by itself does not automatically qualify. See [Google Play's sensitive permission policy](https://support.google.com/googleplay/android-developer/answer/16558241) and [minimum-scope guidance](https://support.google.com/googleplay/android-developer/answer/16935362).

## Permissions in scope

- `READ_MEDIA_IMAGES` on Android 13+
- `READ_MEDIA_VIDEO` on Android 13+
- `READ_MEDIA_VISUAL_USER_SELECTED` to support Android 14 selected-media access and reselection
- `READ_EXTERNAL_STORAGE` only through Android 12L, capped at `maxSdkVersion = 32`

AirShare must not request `MANAGE_EXTERNAL_STORAGE`.

## Core-function statement

AirShare is a local photo and video sharing utility. Its primary user experience is a temporary HTTP sharing session that generates a browser-based view of the media library Android has authorized. A browser with the temporary session link on the same trusted Wi-Fi network can browse thumbnails and download original media while the user keeps the session active.

Persistent or frequent media access is used only to:

- enumerate the photo and video library for the core browsing screen;
- load thumbnails and media metadata needed for that screen;
- reflect additions and changes while the user is using the sharing feature; and
- serve an original item after the authorized browser explicitly requests it.

The app does not upload the library to a developer server, run analytics on media, use media for advertising, or access media for an unrelated background purpose.

## Proposed Play Console declaration copy

### Concise justification

> AirShare's core purpose is repeated local browsing and sharing of the user's photo/video library. During an active session, the app must enumerate authorized media, render thumbnails, and serve selected originals over a token-protected local HTTP session on a trusted Wi-Fi network. Media is never uploaded to the developer, used for ads, or accessed for unrelated purposes.

### Why a system picker is not sufficient for the released broad-access experience

> The released core flow is a persistent media-library browser shared with an authorized desktop browser, not a one-time attachment action. A transactional system picker returns only items selected during that picker interaction and does not provide the continuously browsable library view used by the active AirShare session. Without authorized library access, the advertised full-library browsing experience is unavailable.

### Data handling

> Access is foreground and user initiated. Media and thumbnails are processed on the Android device and delivered directly to the intended browser over local HTTP using a temporary session token. The local transfer is not encrypted. AirShare has no account, analytics, advertising, cloud upload, developer backend, or remote logging. The developer does not receive transfer content.

## Important review risk

Google explicitly states that having a custom picker does not automatically justify broad media permission. File-transfer and attachment workflows can often use the Android Photo Picker. The declaration should be submitted only if the production app genuinely provides persistent or frequent full-library browsing as a primary, prominently advertised feature and the review video proves why a transactional picker cannot provide that released experience.

If Google rejects this justification, remove `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`, use the Android Photo Picker for user-selected media, and update the store listing, privacy policy, permission disclosure, and Data Safety assessment before resubmission.

## Required in-app permission experience

- [ ] Explain the full-library browsing purpose immediately before requesting access.
- [ ] Request only the media types the user is trying to browse.
- [ ] Do not request permission merely on app launch before context is shown.
- [ ] Support Android 14 full, selected, and denied states without treating selected access as an empty library.
- [ ] Provide a clear reselection control for selected-media access.
- [ ] Respect denial and revocation without crashes, loops, or coercive copy.
- [ ] Offer a reduced-access route, such as selected-media access or the system Photo Picker, where technically possible.
- [ ] Stop serving media immediately when the user ends sharing or permission is revoked.
- [ ] Keep permission rationale, store listing, review video, privacy policy, and actual behavior consistent.

## Review-video script

The video supplied to Play Console should be short, narrated or captioned in English, and show the production build:

1. Launch AirShare and show the explanation of why photo/video access is needed.
2. Grant full photo and video access through the Android system dialog.
3. Show the Android ready state and the scope of media access that was granted.
4. Start a sharing session and show its temporary QR code or HTTP URL.
5. From a second device on the same Wi-Fi network, open the generated browser gallery and browse multiple pages containing both media types.
6. Download one original photo and one original video.
7. Return to the Android device and stop sharing; show that the session is no longer available.
8. Repeat or separately demonstrate selected-media and denied-permission behavior.

Do not edit the video to imply functionality that the submitted build does not provide.

## Play Console fields requiring action

- [ ] `[PLAY CONSOLE]` Select the photo and video permission use case that best matches persistent/frequent core library access.
- [ ] `[PLAY CONSOLE]` Paste an accurate version of the proposed declaration copy.
- [ ] `[REQUIRED: PUBLICLY ACCESSIBLE REVIEW VIDEO URL]`
- [ ] `[PLAY CONSOLE]` Confirm the video shows the same version code as the submitted AAB.
- [ ] `[PLAY CONSOLE]` Revisit the declaration whenever media access behavior or permissions change.
