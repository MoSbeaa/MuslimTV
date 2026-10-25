# Launcher theme colors and compact prayer card — 3.97

Settings (gear icon) → ألوان الواجهة (Theme colors).

Presets: green/gold (default), blue/silver, burgundy/gold, charcoal/white. Selecting a preset saves it immediately. For custom colors, enter six-digit #RRGGBB card and accent colors, optionally preview them, then choose حفظ الألوان. Invalid input is rejected without saving. Select the default preset to reset.

Colors are stored in the launcher's existing preferences as theme_surface and theme_accent. The launcher recreates its views when the saved palette changes. Header accents, pills, shortcut cards, dock background, and the embedded prayer card follow the palette. App artwork and photographs keep their original colors. The separate full-screen screensaver keeps its existing palette.

The launcher card width is 230.5 dp, half of its previous 461 dp. Its height and position are unchanged.

Files: ThemeColors.java, ThemeActivity.java, MainActivity.java, HomeCard.java, launcher AndroidManifest.xml.
