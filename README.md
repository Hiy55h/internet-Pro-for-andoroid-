# Internet Pro

Kiwi Browser / Chrome-inspired Android browser prototype.

## Features
- Tab strip with multiple WebView tabs (tap to switch, long-press to close)
- Address/search bar
- Back, forward, reload, home
- Bookmarks saved locally
- DownloadManager downloads to the device's Download folder
- Download list and per-tab history
- Android TV launcher category

## Build
Open this repository in Android Studio and build the `app` module, or run:

```bash
gradle assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions also builds the debug APK on pushes to `main` and on manual workflow dispatch. The resulting APK is available under Actions → workflow run → Artifacts.

## Notes
This is a browser UI prototype using Android System WebView, not a full Chromium fork. Browser engine features and extension compatibility therefore depend on the WebView version installed on the device.
