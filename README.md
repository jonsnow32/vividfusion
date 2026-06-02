# VividFusion

A standalone Android media player supporting direct stream URLs, torrents, and debrid services.

## Features

- **Stream Playback** — Play HTTP/HTTPS, HLS, DASH, SmoothStreaming, and direct media URLs
- **Torrent Streaming** — Built-in torrent engine via TorrentServer
- **Debrid Integration** — Real-Debrid, AllDebrid, Premiumize
- **Downloads** — HLS, HTTP, and torrent downloads with pause/resume support
- **Subtitles** — External subtitle file support with rendering via Media3
- **Extended Codec Support** — FFmpeg decoders for formats not natively supported by Android (via NextLib)
- **Settings** — Player, UI, download, and developer options

## Requirements

- Android 7.0+ (API 24)
- Target SDK 35

## Build

JDK 17 required. `app/google-services.json` must be present (Firebase).

```bash
./gradlew assembleDebug        # debug APK
./gradlew bundleRelease        # release AAB (requires signing config)
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # lint
```

### Local signing

Copy `key.properties.example` to `key.properties` and fill in your keystore details:

```
storeFile=app/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## CI/CD

| Trigger | Workflow | Result |
|---|---|---|
| PR / push to `master` | `ci.yml` | Tests + lint + debug APK artifact |
| `git tag v*` | `release.yml` | Build + sign AAB + GitHub Release |

### Required GitHub Secrets

| Secret | Description |
|---|---|
| `GOOGLE_SERVICES_JSON` | `base64 -i app/google-services.json` |
| `RELEASE_KEYSTORE` | `base64 -i release.jks` (JKS format) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

> **Note:** If your keystore is PKCS12 format, convert to JKS first:
> ```bash
> keytool -importkeystore -srckeystore your.jks -srcstoretype PKCS12 \
>   -destkeystore release-jks.jks -deststoretype JKS -noprompt
> ```

## Tech Stack

- **Player** — Media3 / ExoPlayer + NextLib FFmpeg decoders
- **DI** — Hilt
- **Networking** — OkHttp
- **Background** — WorkManager
- **Logging** — Timber
- **Crash reporting** — Firebase Crashlytics
- **Ads** — AdMob, Meta Audience Network, IronSource, AppLovin, Unity, Vungle

## Disclaimer

This application does not host or distribute any copyrighted content. Users are responsible for ensuring their use of any streamed content complies with applicable laws.
