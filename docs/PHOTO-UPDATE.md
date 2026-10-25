# Updated background photo set — 3.99

The photos/ folder contains 83 originals: 81 JPEG and 2 HEIC files. All are landscape. Originals were preserved.

.github/tools/prepare_tv_photos.py applies EXIF orientation, converts HEIC, and creates JPEG copies fitted within 1920×1080 (quality 87) under photos/tv/. Derived names use a content hash, so spaces and non-ASCII filenames cannot break Android resource URLs. photos/index.json was rebuilt from the actual originals, removing obsolete entries and retaining known credits. Each entry includes its source filename and original dimensions.

Original total: 166,652,958 bytes. TV copies: 27,878,475 bytes.

Both launcher and screensaver carry the collection in their APKs. The screensaver intercepts its photos/index.json and photos/tv/*.jpg requests from bundled assets instead of contacting the upstream repository. The launcher uses these JPEGs as its bundled wallpapers and skips the unrelated upstream collection. The screensaver rotates the full local set rather than limiting it to five upstream photos for four days. Existing rotation intervals stay unchanged.

To prepare a later update:

    PYTHONPATH=optimize/run/photo-tools python .github/tools/prepare_tv_photos.py
    python src/build.py

Build and install both Android apps using the existing local-signing workflow. pillow-heif is needed only for preparing HEIC originals, not on the television. The helper dependency was installed locally under optimize/run/photo-tools.

A contact sheet for inspection is saved under optimize/run/shots/updated-photo-contact-sheet.jpg.

