# MuslimTV Arabic Home transition

Installed on 2026-10-22 for the Fire TV Stick at 10.0.0.70.

The upstream source is https://github.com/toolicious/home-on-fire and retains its GPL-3.0-or-later license. Local source lives in M:/DEV/MyTV/HomeOnFire.

## Installed behavior

- Custom package: `io.github.toolicious.homeonfire.urtv` (MuslimTV Bridge).
- Target: `sa.urtv.home`.
- A native, static green/gold overlay displays: اللهم صلِّ وسلِّم على سيدنا محمد.
- Arabic uses Rakkas from Google Fonts; its OFL license is in `docs/Rakkas-OFL.txt`.
- Original Home on Fire remains installed for rollback, with its Accessibility service inactive.
- The user explicitly approved WRITE_SECURE_SETTINGS for the custom package after its scope and persistence were explained.
- The active Accessibility list previously contained only the original Home on Fire service; it was replaced with the custom service.

## Timing limits

Fire OS 7 defers Home-triggered app launches for up to five seconds. This customization does not extend the wait. The existing upstream overlay lifecycle dismisses the cover after MuslimTV appears and retains its ten-second fail-safe. Amazon may still flash before the Accessibility event arrives. No root exploit or firmware change was used.

## Build

Use the installed Android SDK build tools 35.0.0, platform android-34, and JAVA_HOME pointing to the local JDK. Run `./build.ps1 -NoInstall -NoBump` in HomeOnFire. The local APK is signed with the Android development key at the existing user .android/debug.keystore; that key is not copied into the repository.

Deploy using MuslimTV's `optimize/tv.py` transport. Do not activate both Accessibility services together. For rollback, select the original Home on Fire service instead of the custom service; its previously chosen MuslimTV target is preserved.

## Verification

The APK built and installed successfully. A screenshot captured two seconds after Home showed the Arabic phrase. Local proof: `M:/DEV/MyTV/MuslimTV/optimize/run/shots/urtv-transition-proof.png`. A follow-up screenshot verifies MuslimTV after the overlay dismisses.
