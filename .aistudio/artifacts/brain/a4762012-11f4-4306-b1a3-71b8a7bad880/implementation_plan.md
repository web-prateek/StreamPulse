# StreamPulse: Modern M3U & M3U8 Player for Android

StreamPulse is a modern, high-performance IPTV & M3U/M3U8 streaming player and offline playlist manager for Android. It supports live HTTP Live Streaming (HLS), remote and local video playback, instant URL pasting, offline playlist storage, channel category grouping, and real-time title search across entries.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Search Bar Requirement**: Added a prominent search bar at the top of the Library view to instantly filter channels and saved playlist entries by title with real-time feedback.
> - **Video Playback Engine**: Jetpack Media3 (ExoPlayer + HLS extension) for hardware-accelerated playback of `.m3u8`, `.ts`, `.mp4`, `.mkv`, and live streaming formats.
> - **Offline Playlist Storage**: SQLite via Android Room for caching downloaded playlists, custom tags, favorite channels, and playback positions.
> - **Curated Public Presets**: Pre-configured with top open-access iptv-org playlists (News, Music, Documentary, Sports) while providing effortless "Paste M3U Link" or local `.m3u` file import.

---

### 1. Overview & Core Concept

- **What It Does**: Enables users to paste remote M3U/M3U8 URLs, load local playlist and video files, organize channels into categories and favorites, search saved entries instantly by title, and enjoy a cinema-grade video player with gesture controls and aspect ratio switching.
- **Target Audience / Persona**: Media enthusiasts, cord-cutters, international news and sports viewers, and offline media collectors who need a clean, responsive, ad-free streaming experience.
- **Key Value**: Instant stream loading, seamless parsing of extended M3U tags (`#EXTINF`, `tvg-logo`, `group-title`), zero lag filtering across thousands of channels, and reliable offline access to playlists.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Home / Now Playing & Quick Stream**:
   - Quick "Paste Stream URL" input box with auto-clipboard detection.
   - Recently watched channels carousel with progress indicators and thumbnail preview.
   - Quick-access curated channel categories.
2. **Library View with Top Search Bar**:
   - **Top Search Bar**: Sticky M3U search bar with clear button and active query indicator, filtering channel titles in real-time.
   - **Tabbed / Filter Chips**: "All Channels", "Favorites", "Categories / Groups", and "Downloaded / Local".
   - **Playlist Management**: Add new M3U URLs, import local `.m3u` files, refresh remote playlists, view channel count, and delete/archive playlists.
   - **Channel Cards**: Channel logo (via Coil or fallback badge), stream title, group tag, resolution/format badge, and one-tap favorite star.
3. **Cinema Video Player**:
   - Edge-to-edge video canvas using Media3 `PlayerView` wrapped in Compose `AndroidView`.
   - Custom overlay HUD: Play/Pause, 10s seek forward/rewind, live stream sync indicator, channel switcher drawer, aspect ratio toggle (Fit, Fill, Zoom, 16:9), and screen brightness/volume swipe gestures.
   - Error handling with automatic reconnect and clear stream error diagnostics.

#### Visual Identity & Theme
- **Aesthetic Direction**: Premium Deep Cinema Dark mode with high-contrast emerald and cyan accents.
- **Color Palette**:
  - Background: Obsidian Dark (`#0B0F17`)
  - Surface: Elevated Charcoal (`#151C28`)
  - Primary Accent: Neon Cyan (`#00E5FF`)
  - Secondary Accent: Emerald Green (`#00E676`)
  - Text Primary: Pure Crisp White (`#F0F4F8`)
  - Text Secondary: Cool Muted Slate (`#94A3B8`)
- **Typography & Hierarchy**: Clean modern sans-serif typography with distinct letter-spacing on category headers and monospaced tech pills for bitrates/resolutions.
- **Micro-Interactions & Feedback**: Smooth entry transitions, active search highlights, pulsating "LIVE" pill for ongoing broadcasts, and haptic feedback on channel bookmarking.

---

### 3. Key Product Decisions & Trade-Offs

- **Media3 ExoPlayer for HLS & Streams**:
  - *Chosen Approach*: `androidx.media3:media3-exoplayer` + `androidx.media3:media3-exoplayer-hls` + `androidx.media3:media3-ui`.
  - *Why*: Industry standard on Android for rock-solid HLS manifest parsing, adaptive bitrate switching (ABR), low latency, and robust error recovery.
  - *Alternatives Considered*: Standard Android `VideoView` lacks advanced HLS stream handling and custom headers.
- **Room Database for Local & Offline Playlists**:
  - *Chosen Approach*: Room entity schema separating `Playlist` (source URL, title, sync time) from `ChannelItem` (stream URL, title, group, logo, favorite status).
  - *Why*: Enables millisecond-fast full-text and title filtering via SQL indices even when playlists contain thousands of channels, without draining network bandwidth.
- **Optimized M3U Parser**:
  - *Chosen Approach*: Line-by-line streaming OkHttp parser that extracts `#EXTINF` metadata (`tvg-name`, `tvg-logo`, `group-title`) efficiently without OutOfMemory exceptions on massive multi-megabyte playlists.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   StreamPulse UI Layer                 │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │   Top Search Bar      │   │  Category / Filter   │  │
│  │   (Real-time Query)   │   │  Chips & Favorites   │  │
│  └───────────┬───────────┘   └───────────┬──────────┘  │
│              ▼                           ▼             │
│  ┌──────────────────────────────────────────────────┐  │
│  │     Library Screen / Channel Grid & List         │  │
│  └───────────────────────┬──────────────────────────┘  │
│                          ▼                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │     Cinema Player Screen (Media3 PlayerView)     │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────┬─────────────────────────────┘
                           │ StateFlow & Events
                           ▼
┌────────────────────────────────────────────────────────┐
│                    LibraryViewModel                    │
│  - searchFilterQuery: MutableStateFlow<String>         │
│  - activeCategory: MutableStateFlow<String?>           │
│  - filteredChannels: StateFlow<List<ChannelItem>>      │
└──────────────┬───────────────────────────┬─────────────┘
               │                           │
               ▼                           ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│     Room Database       │    │   M3uParser & Network   │
│  - PlaylistDao          │    │   - OkHttpClient        │
│  - ChannelDao           │    │   - M3uStreamParser     │
│  - ChannelEntity        │    │   - Local File Importer │
└─────────────────────────┘    └─────────────────────────┘
```

#### Data Entities:
- `PlaylistEntity`: `id`, `name`, `sourceUrl`, `isLocal`, `channelCount`, `lastUpdated`
- `ChannelEntity`: `id`, `playlistId`, `title`, `streamUrl`, `groupName`, `logoUrl`, `isFavorite`, `lastWatchedTimestamp`

#### Key Components:
- `M3uParser`: Lightweight streaming tokenizer for `#EXTM3U`, `#EXTINF:-1 tvg-id="..." tvg-logo="..." group-title="...", Channel Name`.
- `ChannelDao`: Fast title matching via `SELECT * FROM channels WHERE title LIKE '%' || :query || '%'`.
- `PlayerViewModel` & `VideoPlayer`: Media3 player lifecycle management with play/pause, seek, audio focus, and aspect ratio controls.
