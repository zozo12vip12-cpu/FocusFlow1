# Play Console checklist (verified against Google's pages in Nov 2026 — re-check before submitting)

1. **Developer account**: pay the one-time fee and complete identity verification.
   Personal accounts created after 13 Nov 2023 must run a **closed test with at least 12 testers opted in for 14 continuous days** before applying for production access.
2. **Create app** > App name, language, "App", "Free". Package name = your `applicationId`.
3. **Upload** the signed `.aab` (Test and release > Closed testing first). Use Play App Signing.
4. **Target API**: the project targets API 36 (required for new apps/updates since 31 Aug 2026).
5. **App content** (Policy > App content):
   - Privacy policy URL: the public link of `docs/privacy-policy.html`.
   - Ads: **No ads**.
   - App access: **All functionality available without special access**.
   - Content rating questionnaire: answer honestly (no violence, no user content) — an app without a rating is not allowed.
   - Target audience: choose **18+ / 13+ (not designed for children)** to avoid the Families policy.
   - Data safety: **No data collected, no data shared** (no internet permission, local storage only). Mention that data can be deleted from Settings.
   - Government / news / health / financial apps: **No**.
6. **Foreground service declaration** (App content > Foreground service permissions):
   - Type: **Media playback**.
   - Description: "The user can start calming focus sounds (generated in the app). The sound keeps playing in the background while the screen is off, with a media notification that has a Stop button. The service starts only when the user taps a sound and stops when they stop it or when a focus session ends."
   - Video (required): a 20–30 s screen recording: open app > tap a sound > lock the screen > sound continues > notification Stop button.
7. **Exact alarm declaration** (SCHEDULE_EXACT_ALARM): "Core function is a countdown timer; the user-set end of a work/break session must alert at the exact time, also in Doze. The app asks the user for the permission and falls back to inexact alarms if declined."
8. **Store listing**: texts from `listing.md`, icon `icon-512.png`, feature graphic `feature-graphic-1024x500.png`, and at least 2 real phone screenshots (take them from your phone).
9. If Play Console shows a **16 KB page size** warning, update `compose-bom` in `app/build.gradle.kts` to the latest version and rebuild.
10. Keep the upload keystore and passwords safe (a lost key is painful, although Play App Signing allows an upload-key reset).
