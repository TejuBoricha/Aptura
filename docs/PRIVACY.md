# Privacy

Aptura is an offline video player. It does not collect, transmit or sell personal data. There is no analytics SDK, no advertising SDK, no account system and no crash-reporting service.

## The short version

- The app does not request `android.permission.INTERNET`. It cannot open a network connection.
- Nothing is uploaded. There is no server to upload to.
- Library data, playback progress, bookmarks and settings stay in the app's private storage on your device.
- Android backup is disabled for Aptura (`android:allowBackup="false"`), so that data is not copied to a cloud backup.
- Diagnostics are local. They leave the device only when you explicitly export or share them.

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_VIDEO` (Android 13+) | Read videos already indexed by Android's MediaStore. |
| `READ_EXTERNAL_STORAGE` (Android 12 and lower) | The same access on older releases. |
| `FOREGROUND_SERVICE` | Keep playback alive while the app is in the background. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Declare the foreground service type required by Android 14+. |

Aptura does **not** request:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.MANAGE_EXTERNAL_STORAGE`
- `android.permission.POST_NOTIFICATIONS` (the media session is exempt)
- location, contacts, camera, microphone, calendar or phone state

Folder access outside MediaStore is granted one tree at a time through the system folder picker (Storage Access Framework). The app stores the URI permission you granted; it does not browse the rest of the file system.

## Data stored on the device

| Data | Where | Why |
|---|---|---|
| Video index (URI, title, size, duration, folder, format) | Room database in app-private storage | Show the library without rescanning every launch |
| Playback position, completion, last played | Room database | Resume and "Continue watching" |
| Bookmarks | Room database | Jump back to marked moments |
| Preferences (theme, speed, subtitle style, decoder, SAF folders) | Preferences DataStore | Remember your choices |
| Diagnostics events and crash reports | App-private `files/diagnostics` | Help you describe a bug without a telemetry service |
| Thumbnail cache | App cache directory | Avoid re-decoding frames |

Deleting the app removes all of it. Android backup is off, so a reinstall starts clean.

## Diagnostics

Aptura records a bounded set of local events with codes such as "scan started" or "playback failed", plus the exception class for failures. Before export, `RedactionEngine` removes:

- storage paths (`/storage/...`, `/sdcard/...`, `/data/...`);
- file names, unless you explicitly choose to include them;
- email addresses;
- IPv4 addresses.

Settings has **Export diagnostics** and **Delete diagnostic data**. Export creates a Markdown report and hands it to the share sheet you choose. Delete clears the in-memory buffer and the on-device files immediately. There is no hidden copy and no automatic upload.

## When data leaves the app

Only when you initiate it:

- **Share** a video: Android's share sheet receives the content URI and the app you pick reads it.
- **Open with** another player: the same hand-off.
- **Search subtitles on the web**: Aptura opens your browser with a search query. The query is built from the title; the browser and search engine apply their own policies. Aptura itself does not make the request.
- **Export diagnostics**: the generated report goes to the share target you choose.

## Children

Aptura has no accounts, ads, purchases or social features, and collects no personal data. It is suitable for general audiences.

## Changes

If a future version changes any of this, the change must be visible in the manifest, the dependency graph and this document. The zero-network invariant is enforced by tests and by the release build, so it cannot change silently.

Questions or corrections: open an issue or use the private reporting route in [SECURITY.md](../SECURITY.md).
