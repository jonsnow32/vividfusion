# CLAUDE.md

## Custom Skills

### /video-architect
Invoke with `/video-architect <question>` to activate the elite Video Player Architect persona.
Covers: ExoPlayer/Media3, HLS/DASH, adaptive bitrate, subtitle systems, Android TV, DRM, OTT architecture, performance optimization, monetization, AI recommendations.

Installed globally at `~/.claude/skills/video-architect/SKILL.md` — available in all projects without restart.
Also available project-level at `.claude/commands/video-architect.md`.

Example: `/video-architect how should I implement adaptive bitrate buffering for weak networks?`

---

## Subagents

Spawn subagents to isolate context, parallelize independent work, or offload bulk mechanical tasks. Don't spawn when the parent needs the reasoning, when synthesis requires holding things together, or when spawn overhead dominates.

Pick the cheapest model that can do the subtask well:
- Haiku: bulk mechanical work, no judgment
- Sonnet: scoped research, code exploration, in-scope synthesis
- Opus: subtasks needing real planning or tradeoffs

If a subagent realizes it needs a higher tier than itself, return to the parent.

Parent owns final output and cross-spawn synthesis. User instructions override.

## Preferred Tools

### Data Fetching

1. **WebFetch**: free, text-only, works on public pages that don't block bots.
2. **agent-browser CLI**: free, local Rust CLI + Chrome via CDP. For dynamic pages or auth walls that WebFetch can't handle. Returns the accessibility tree with element refs (@e1, @e2). ~82% fewer tokens than screenshot-based tools. Install: `npm i -g agent-browser && agent-browser install`. Use `snapshot` for AI-friendly DOM state, element refs for interaction.
3. **Notice recurring fetch patterns and propose wrapping them as dedicated tools.** When the same fetch/parse logic comes up more than once, suggest wrapping it as a named tool (e.g. a skill file or a .py script that calls `agent-browser` with the snapshot and extraction steps baked in for that source). Add the entry to `## Dedicated Tools` below and reference it by name on future calls.


This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

VividFusion / VVF (`cloud.app.vvf`) is an Android media app for browsing, streaming,
and playing movies/TV. Content sources are **not hardcoded** — they come from
**extensions** that implement a small set of client interfaces. Extensions are either
built-in or installed as separate APK plugins. The app shell owns the UI, playback
(Media3/ExoPlayer), persistence, downloads, and debrid/network integrations;
extensions supply the actual catalog, stream links, and subtitles.

It is intentionally architected after the [Echo](https://github.com/brahmkshatriya/Echo)
player's plugin model.

## Modules

Only two Gradle modules are included (`settings.gradle.kts`):

- `:app` — the Android application.
- `:common` — pure Kotlin/Android library shared with extensions. Defines the client
  interfaces, data models, settings abstraction, and network helpers (`HttpHelper`)
  that every extension compiles against.

**Treat `:common` as a public API surface.** Installed third-party plugin APKs are
compiled against it, so changing its interfaces or models (`AVPMediaItem`,
`MediaItemsContainer`, `PagedData`, the `*Client` interfaces, `ExtensionMetadata`)
can break already-installed plugins. Models/DTOs that cross the app↔extension boundary
belong here, not in `:app`.

> Note: a top-level `features/` directory exists on disk but is **not** an included
> Gradle module — don't treat it as live code.

## Build & Run

JDK 17 required. compileSdk/targetSdk 35, minSdk 24. No product flavors.
`app/google-services.json` must be present (Firebase + Crashlytics are applied).
`local.properties` supplies `sdk.dir`.

```bash
./gradlew assembleDebug            # build debug APK
./gradlew installDebug             # build + install on a connected device/emulator
./gradlew lint                     # Android lint
./gradlew bundleRelease            # release AAB (R8 minify + resource shrink enabled)
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew connectedAndroidTest     # instrumented tests on a device
```

There are currently **no test sources** (`src/test`/`src/androidTest` exist but are
empty). When adding the first test, run a single one with:
`./gradlew :app:testDebugUnitTest --tests "cloud.app.vvf.SomeTest"`.

## Architecture

### Extension / plugin system (the core abstraction)

Everything content-related flows through extensions. Build features against the client
interfaces in `:common`, not against any specific extension.

- **Client interfaces** (`common/.../clients/`) all extend `BaseClient`
  (`onInitialize` runs once at load; `onExtensionSelected` runs each time the user
  picks it; settings come via `SettingProvider`):
  - `mvdatabase/DatabaseClient` — catalog/metadata + feeds (`getHomeFeed`,
    `getMediaDetail`, `searchFeed`, `getRecommended`, …), all paged via `PagedData`.
  - `streams/StreamClient` — `loadLinks(...)` resolves playable streams.
  - `subtitles/SubtitleClient` — `loadSubtitles(...)`.
  - `provider/*` — capability interfaces the host injects into clients
    (`HttpHelperProvider`, `SettingProvider`, `MessageFlowProvider`,
    `ExtractorFlowProvider`).
- **`ExtensionType`** (`common`): `DATABASE`, `STREAM`, `SUBTITLE` — what a given
  extension advertises (one extension may advertise several).
- **`Extension<T : BaseClient>`** (`common/.../clients/BaseClient.kt`): wraps
  `ExtensionMetadata` + a lazily-`Injectable<T>` client instance.

#### Loading pipeline (`app/.../extension/`)

- **`ExtensionLoader`** is the single entry point. It combines repositories, injects
  host dependencies (`HttpHelper`, throwable/message flows, prefs) into each client,
  exposes the live extensions as flows filtered/typed per `ExtensionType`, and keeps a
  `priorityMap` (user-ordered preference per type, persisted in SharedPreferences).
- **`extension/repo/`** is the actual loading mechanism:
  - `CombinedRepository` merges built-in clients + `AppRepository` (installed APK
    plugins discovered via `PackageManager`) + `FileRepository` (loose `.apk` files in
    a folder).
  - `ExtensionParser` reads plugin metadata from an APK and instantiates its
    `BaseClient`; `DexLoader` does the class loading and extracts native libs from the
    APK for the device ABI.
- **`extension/builtIn/`**: in-app extensions implementing the same interfaces without
  being separate APKs — `local/BuiltInClient` (on-device media) and the TMDB-based
  database client (`extension/tmdb/`, `TmdbTvdbClient`).
- **`ApkDownloader` / `InstallationUtils` / `UpdateChecker`**: download, install, and
  update plugin APKs.

### Dependency injection (Hilt)

`VVFApplication` is the `@HiltAndroidApp` entry; `MainActivity` is the single activity
host. App-wide singletons live in `app/.../di/` (`AppModule`, `NetworkModule`,
`ExtensionModule`, `DataStoreModule`, `WorkManagerModule`) plus per-API modules in
`app/.../network/di/`. `AppModule` notably provides shared `MutableSharedFlow`s for
cross-cutting `Throwable`/`Message` events and a UI-update flow. Add new singletons
here rather than constructing them ad hoc.

### Persistence

`datastore/` (`DataStore.kt`, `app/`, `account/`) is the app's own key/value + account
persistence layer. Extension settings are bridged through the settings abstraction in
`:common` so plugins read/write settings without touching app internals.

### Playback

Media3/ExoPlayer (+ NextLib FFmpeg decoders for extra codecs, torrent streaming via
`torrentserver`). Player logic lives in the `app/.../features/player/` and
`features/playerManager/` **packages** (under `:app`, distinct from the orphaned
top-level `features/` dir). HLS/DASH/SmoothStreaming and subtitle rendering are
supported.

### Networking & integrations

OkHttp-based, centralized in `common`'s `HttpHelper` (injected into extensions).
`app/.../network/api/` hosts first-party service clients: debrid providers
(`realdebrid`, `alldebrid`, `premiumize`), `openSubtitle`, `trakt`, `torrentserver`,
and GitHub (for plugin repos/updates). TMDB/Trakt/TheTVDB SDKs back the built-in
database extension.

### Background work

`services/`: `downloader/` (`HlsDownloader`, `HttpDownloader`, `TorrentDownloader`
with a `stateMachine/`), plus WorkManager workers (`BackupWorker`,
`SubscriptionWorkManager`, `ApkDownloader`).

### UI

Single-activity, Fragment-based with ViewBinding (Compose enabled but used sparingly).
Feature areas under `ui/`: `main/` (home, browse, search, library, networkstream),
`detail/` (movie/show/season/episode/actor/torrent), `stream/` (stream selection),
`media/` (paged media adapters), `extension/` (manage plugins), `download/`,
`setting/`, and `widget/dialog/`.

## Conventions

- Kotlin official code style (`kotlin.code.style=official`); match surrounding files.
- `kotlinx.serialization` + `kotlin-parcelize` are used for models — annotate new model
  classes consistently with their neighbors.
- KSP (not kapt) drives Hilt and Glide code generation.
- Timber is the logging facade.
