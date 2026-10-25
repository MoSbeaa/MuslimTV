# MuslimTV

Repository: https://github.com/MoSbeaa/MuslimTV — branch `main`.

## Components
- `android/launcher`: MuslimTV home screen.
- `android/app`: MuslimTV Screensaver, shared configuration, prayer and adhan settings.
- `firetv-bridge`: MuslimTV Bridge, Fire TV Home redirection and scoped adhan/popup service.
- `src` and `dist`: page sources and generated pages.
- `photos`: owner-approved original photos and optimized offline backgrounds.

## Compatibility
Android package IDs, provider authorities, permissions, signing aliases and protocol actions remain unchanged to preserve installed settings, Home mapping and update compatibility. Legacy `urtv-*.apk` / `urtv.html` filenames remain supported by existing automation. The visible product name is MuslimTV.

## Build
Run `python src/build.py`, then Gradle in `android` with `:app:assembleRelease :launcher:assembleRelease`. Set `GITHUB_RUN_NUMBER` above the installed version code (current branding build: 104). The page source branch defaults to `main`. Build the bridge using `firetv-bridge/build.ps1 -NoInstall` with the same signing identity as both apps. Never commit keystores.

GitHub Actions release builds require `ANDROID_KEYSTORE_B64` and `ANDROID_KEYSTORE_PASSWORD` secrets. Do not generate a new signing key for an already installed device; all three components must continue to share the same signing identity. Existing local signing keys have not been uploaded.

## Credits
Original UrTV: https://github.com/tshi-dev/UrTV
Home on Fire: https://github.com/toolicious/home-on-fire
Font and audio attributions remain beside their assets.
