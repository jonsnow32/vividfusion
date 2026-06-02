# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

Tradeoff: These guidelines bias toward caution over speed. For trivial tasks, use judgment.

1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.
2. Simplicity First
Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

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

VividFusion / VVF (`cloud.app.vvf`) is an Android **debrid-powered media player**. It
streams and plays video/audio from HTTP(S) URLs, HLS/DASH streams, magnet links, and
torrent files. Debrid services (RealDebrid, AllDebrid, Premiumize) and subtitle
downloads (OpenSubtitles) are first-class integrations. There is **no extension/plugin
system** and **no content database** (no TMDB/Trakt/TVDB/IMDB).

> Note: a top-level `features/` directory exists on disk but is **not** an included
> Gradle module — don't treat it as live code.

## Modules

Two Gradle modules (`settings.gradle.kts`):

- `:app` — the Android application.
- `:common` — pure Kotlin library. Contains utilities (`HttpHelper`, exceptions,
  serialization), stream models (`Streamable`, `MagnetObject`, `Resolution`,
  `PremiumType`), subtitle models (`SubtitleData`), and generic media models
  (`AVPMediaItem` with `VideoItem`/`TrackItem`/`VideoCollectionItem`/`PlaybackProgress`).
  No extension interfaces here.

## Build & Run

JDK 17 required. compileSdk/targetSdk 35, minSdk 24. No product flavors.
`app/google-services.json` must be present (Firebase + Crashlytics).
`local.properties` supplies `sdk.dir`.

```bash
./gradlew assembleDebug            # build debug APK
./gradlew installDebug             # build + install on a connected device/emulator
./gradlew lint                     # Android lint
./gradlew bundleRelease            # release AAB (R8 minify + resource shrink enabled)
./gradlew :app:testDebugUnitTest   # JVM unit tests
```

## Architecture

### Core User Flow

1. User enters URL / magnet / torrent file → `NetworkStreamFragment`
2. Optionally resolve through debrid (RealDebrid / AllDebrid / Premiumize)
3. Tap "Play Now" → `PlayerFragment` (Media3/ExoPlayer)
4. Tap "Download" → `DownloadsFragment` via `HlsDownloader`/`HttpDownloader`/`TorrentDownloader`

### Dependency Injection (Hilt)

`VVFApplication` is the `@HiltAndroidApp` entry; `MainActivity` is the single activity
host. Singletons in `app/.../di/` (`AppModule`, `NetworkModule`, `DataStoreModule`,
`WorkManagerModule`) plus per-API modules in `app/.../network/di/`. `AppModule`
provides shared `MutableSharedFlow`s for `Throwable`/`Message` events. Add new
singletons here rather than constructing them ad hoc.

### Persistence

`datastore/` (`DataStore.kt`, `app/`, `account/`) — SharedPreferences-backed key/value
store. `AppDataStore` stores: URI history (`UriHistoryItem`), player settings
(`PlayerSettingItem`), download state (`DownloadData`), and video playback progress
(`PlaybackProgress`). Account credentials for debrid services live in
`datastore/account/`.

### Playback

Media3/ExoPlayer + NextLib FFmpeg decoders. Player code in `features/player/`:
- `PlayerFragment` — full-screen player UI with custom controller
- `PlayerViewModel` — player state, track selection, playback speed
- `PlayerService` — `MediaSessionService` stub (background playback not yet implemented)
- `features/player/subtitle/` — local subtitle support (SRT/VTT/ASS/TTML), offset UI
- `features/player/torrent/` — torrent streaming overlay
- `features/dialogs/` — audio/video/subtitle track selection dialogs

### Networking & Integrations

OkHttp-based (no Retrofit). `common/HttpHelper` wraps OkHttp.
`app/.../network/api/`:
- `realdebrid/`, `alldebrid/`, `premiumize/` — debrid unrestrict APIs
- `torrentserver/` — local torrent server (torrent streaming)
- (OpenSubtitles infrastructure exists in network layer, not yet wired to UI)

`app/.../network/di/` — Hilt modules for each API client.

### Downloads

`services/downloader/`: `HlsDownloader`, `HttpDownloader`, `TorrentDownloader` with a
`stateMachine/`. `DownloadData` model tracks status, progress, file path. WorkManager:
`ApkDownloader` (self-update), `BackupWorker`.

### UI

Single-activity, Fragment-based with ViewBinding.
Navigation: bottom nav with 3 tabs (manual add/show/hide in `MainFragment`):
- **Stream** (`NetworkStreamFragment`) — URL/magnet/torrent input, play history
- **Downloads** (`DownloadsFragment`) — download queue with storage visualization
- **Settings** (`SettingsRootFragment`) — general, UI, player, download, backup, about

Detail screen: `ui/detail/torrent/TorrentInfoFragment` (torrent metadata before play).
Dialogs in `ui/widget/dialog/` — account (debrid), action selection, input, exit confirm.

## Conventions

- Kotlin official code style (`kotlin.code.style=official`); match surrounding files.
- `kotlinx.serialization` + `kotlin-parcelize` for models — annotate consistently.
- KSP (not kapt) drives Hilt and Glide code generation.
- Timber is the logging facade.
- No Jetpack Navigation — fragment transactions are manual (`MainFragment.showTab`,
  `navigate` extension in `NavUtils.kt`).
- Flow observation: use `observe(viewModel.flow) { }` from `FlowUtils.kt`.
