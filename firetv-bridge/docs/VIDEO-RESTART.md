# Restart two MuslimTV video shortcuts (prepared, awaiting access approval)

Canonical IDs: 3ZJvLYqrUrM (Al-Kahf / Hussain Azzam), fD4uDIoCn3M (morning adhkar / Ahmad Al Nufais).

RestartVideoActivity is signature protected using the existing OPEN_AMAZON permission. It opens the canonical YouTube link in TizenTube. VideoRestart waits up to 60 seconds for that application to be in the foreground, publish a matching media ID or matching Arabic title, enter PLAYING state, and advertise ACTION_SEEK_TO. It then sends seekTo(0) exactly once and cancels. A newer request cancels older work; leaving TizenTube after entering cancels pending work. If identification fails, it does not seek anything.

Android requires enabled notification-listener access to query/control another application's active media session. PlaybackAccess implements no notification callbacks, does not read notification text or store it, and performs no network requests. Android nevertheless grants broad notification access to an enabled listener, so explicit owner approval is required before enabling this component. No notification-listener access has been enabled by preparing/building this change.

Next after approval: install both prepared APKs, enable only io.github.toolicious.homeonfire.muslimtv/.PlaybackAccess via the supported Android notification access mechanism, preserving all other listeners. Never disable another listener. Access is reversible by disabling that component.
