# MuslimTV

Repository: https://github.com/MoSbeaa/MuslimTV — branch `main`.

## Components
- `android/launcher`: MuslimTV home screen.
- `android/app`: MuslimTV Screensaver, shared configuration, prayer and adhan settings.
- `firetv-bridge`: MuslimTV Bridge, Fire TV Home redirection and scoped adhan/popup service.
- `src` and `dist`: page sources and generated pages.
- `photos`: owner-approved original photos and optimized offline backgrounds.

## Application identities
All source packages, authorities, actions, build variables and artifact names use `muslimtv` / `MuslimTV` / `MUSLIMTV`.
- Home: `sa.muslimtv.home`
- Screensaver: `sa.muslimtv.screensaver`
- Bridge: `io.github.toolicious.homeonfire.muslimtv`

These are new Android installation identities. Existing installations need a settings transfer before changing to them; changing an APK label alone does not migrate private app data. Signing identities remain local and must stay consistent across the three components. Do not uninstall an existing installation before settings have been transferred.

The public countdown is optional, disabled by default, and accepts a locally saved title/date. No owner's personal event name or date is included.

## Build
Run `python src/build.py`, then Gradle in `android` with `:app:assembleRelease :launcher:assembleRelease`. Set `GITHUB_RUN_NUMBER` above the installed version code (current branding build: 105). The page source branch defaults to `main`. Build the bridge using `firetv-bridge/build.ps1 -NoInstall` with the same signing identity as both apps. Never commit keystores.

GitHub Actions release builds require `ANDROID_KEYSTORE_B64` and `ANDROID_KEYSTORE_PASSWORD` secrets. Do not generate a new signing key for an already installed device; all three components must continue to share the same signing identity. Existing local signing keys have not been uploaded.

## Credits
Original project author: https://github.com/tshi-dev
Home on Fire: https://github.com/toolicious/home-on-fire
Font and audio attributions remain beside their assets.
