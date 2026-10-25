# Fire TV screensaver: 24-hour idle display

The Fire TV was configured to enter deep sleep after 1,200,000 ms (20 minutes). MuslimTV starts after 300,000 ms (5 minutes). Power-service output showed the stick was asleep when the TCL reported no HDMI signal.

Set secure sleep_timeout to 86,400,000 ms (24 hours of inactivity). Keep system screen_off_timeout at 300,000 ms so the screensaver still starts after 5 minutes. This gives approximately 23 hours 55 minutes of screensaver time before deep sleep. It is not a permanent 24/7 setting.

Original values: optimize/run/sleep-timer-before-24h.json.

Restore the original timer with the repository tool:

    optimize/venv/bin/python -I optimize/tv.py "settings put secure sleep_timeout 1200000"

This changes the Fire TV only. The TCL Roku television can still have its own power-saving or sleep timer.
