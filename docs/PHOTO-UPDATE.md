# Updated background photo set — 3.99

The photos/ folder contains 83 originals: 81 JPEG and 2 HEIC files. All are landscape. Originals were preserved.

.github/tools/prepare_tv_photos.py applies EXIF orientation, converts HEIC, and creates JPEG copies fitted within 1920×1080 (quality 87) under photos/tv/. Derived names use a content hash, so spaces and non-ASCII filenames cannot break Android resource URLs. photos/index.json was rebuilt from the actual originals, removing obsolete entries and retaining known credits. Each entry includes its source filename and original dimensions.

Original total: 166,652,958 bytes. TV copies: 27,878,475 bytes.

Both launcher and screensaver carry the collection in their APKs. The screensaver intercepts its photos/index.json and photos/tv/*.jpg requests from bundled assets instead of contacting the upstream repository. The launcher uses these JPEGs as its bundled wallpapers and skips the unrelated upstream collection. The screensaver rotates the full local set rather than limiting it to five upstream photos for four days. Existing rotation intervals stay unchanged.

On the screensaver, two photo layers fade the old image to black and the new image in over roughly 2.4 seconds. The new image is assigned only to the invisible layer; the clock and information cards remain visible. Initial display waits briefly for the bundled photo index so it need not switch from a placeholder image after opening. The old minute-long photo movement and the burst preload of all 83 photos have been removed because they can force repeated WebView redraws and exhaust memory on a Fire TV Stick.

The screensaver receives the selected launcher palette when the home screen opens or its theme changes. Prayer and hadith cards, including the optional home card, use the launcher's 55% surface opacity, 30% accent border, 1 CSS-pixel border, and 26-pixel corners. The black photo gradient matches the launcher scrim. The photographs keep their original colors.

To prepare a later update:

    PYTHONPATH=optimize/run/photo-tools python .github/tools/prepare_tv_photos.py
    python src/build.py

Build and install both Android apps using the existing local-signing workflow. pillow-heif is needed only for preparing HEIC originals, not on the television. The helper dependency was installed locally under optimize/run/photo-tools.

A contact sheet for inspection is saved under optimize/run/shots/updated-photo-contact-sheet.jpg.

