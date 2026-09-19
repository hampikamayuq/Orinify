<div align="center">
<h1>Orinify</h1>
A personal, ad-free YouTube Music client for Android (and Desktop) that knows the listener —
built as a fork of <a href="https://github.com/maxrave-dev/SimpMusic">SimpMusic</a> using Compose Multiplatform.
<br>
<br>
<a href="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml"><img src="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml/badge.svg"></a>
<a href="https://github.com/hampikamayuq/Orinify/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue"></a>
<br>
<br>
<h4>Download</h4>
Debug builds are published as artifacts on every push — grab the latest one from the
<a href="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml">Actions tab</a>
(pick the most recent successful run, download <code>orinify-debug-arm64</code> for most phones).
There is no signed release and no store listing: this is a personal fork, sideloaded, that
installs alongside SimpMusic rather than replacing it (package <code>dev.diego.orinify</code>).
</div>

## What this fork adds

Every YouTube Music client can play what YouTube suggests. Orinify also builds lists from a
`playback_event` table that never leaves the device:

- **Rediscover** — tracks played at least 3 times before a 60-day cutoff and not once since
- **You might like** — radio seeded from your last 90 days of listening (the only personal
  list that touches the network)
- **Artist mixes** — one radio per artist, ranked by your own play count
- **Recently played**, **Wrapped**, monthly recap playlists, and an on-device **Analytics**
  tab (listening clock, streaks, top tracks/artists/albums), all from the same local history
- **The player's own line** — under the artist, on every Now Playing style: play count and
  when you last heard it

None of this needs your history to leave the phone, and turning off local tracking simply
empties these lists — no other part of the app changes.

## Features

- Play music from YouTube Music or YouTube for free, without ads and in the background
- Three Now Playing styles: Classic, Material 3 Expressive and Apple Music
- Ten-band equalizer with presets and AutoEq headphone profiles, plus Delay and Reverb effects
- Word-by-word Apple Music-style lyrics, romanization for 12 languages, share lyrics as an image
- High quality up to 256kbps stream (Opus or AAC) for YouTube Music Premium accounts
- Browsing Home, Charts, Podcasts, Moods & Genres with YouTube Music data
- Search everything on YouTube
- Spotify Canvas and Animated Album Art
- Crossfade with DJ-style transitions
- Listen Together: shared rooms that play in sync with friends, wire-compatible with
  [Metrolist](https://github.com/MetrolistGroup)
- AI song suggestions and AI lyrics translation (bring your own OpenAI or Gemini API key)
- Import playlists converted from Spotify and other apps
- Multi-YouTube-account support, custom local playlists synced with YouTube Music
- Caching and offline playback, download with metadata
- Sleep timer, Android Auto, Discord Rich Presence, Last.fm scrobbling
- Google Cast support
- Supports SponsorBlock and Return YouTube Dislike
- Synced lyrics from SimpMusic Lyrics, LRCLIB, Spotify (requires login) and YouTube Transcript
- Desktop app (Windows, macOS, Linux) with its own mini player

> **Note.** This is a personal, sideloaded fork with no store listing and no user base
> beyond the owner and whoever he hands the APK to. It depends on YouTube Music's hidden
> API, which can and does break; treat it accordingly.

## Desktop app

### Which file should I download?
- Windows: the `.msi` installer
- macOS: the `.dmg` (ARM and x86-64)
- Linux: the `.AppImage`

Built from the same source, using [libmpv](https://github.com/mpv-player/mpv) for playback
instead of Media3/ExoPlayer. See `CLAUDE.md` for the desktop-specific build notes
(`mpvSetupAll`, platform natives, known limitations).

## Data & Privacy

- Uses YouTube Music's hidden/unofficial API to fetch data — the same approach as most
  third-party clients, and the reason things occasionally break when YouTube changes it.
- Listening history (`playback_event`) is stored locally and never leaves the device. It is
  the only thing the personalized lists above are built from.
- No tracker, no analytics backend, no crash reporting in the FOSS build. If a logged-in
  account enables YouTube's own "Send back to Google" feature, listening activity is sent
  to Google the same way it would be from any YouTube Music client — that is YouTube's
  tracking API, not this app's.
- Spotify's Web API is used (with some tricks) for Canvas and lyrics.

## Credits

Orinify is a fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic), an open-source
project by [maxrave-dev](https://github.com/maxrave-dev), and stays under the same
GPL-3.0 licence. Nearly everything not described in "What this fork adds" above — the
architecture, the YouTube Music integration, the equalizer, Listen Together, the AI
features, and much more — is upstream's work, carried forward here.

SimpMusic itself credits:
- [InnerTune](https://github.com/z-huang/InnerTune/) for the original idea of extracting
  YouTube Music data
- [SmartTube](https://github.com/yuliskov/SmartTube) for streaming URL extraction
- [SponsorBlock](https://sponsor.ajay.app/) for sponsor-skip data
- [LRCLIB](https://lrclib.net/) for lyrics
- [Metrolist](https://github.com/MetrolistGroup) for the Listen Together wire protocol

## Legal Disclaimer & Terms of Use

### 1. Free, Open-Source & Non-Commercial
Orinify is open source, built for personal and educational use. It is not sold, and it has
no ads, premium features, subscriptions, or hidden fees.

### 2. A Custom Browser with Content Filtering
Orinify acts strictly as a specialized, third-party client. It parses YouTube and YouTube
Music's publicly available website content and APIs, and renders them in a custom
interface — no different in kind from a standard browser with an ad-blocking extension.

### 3. Support Content Creators
Please consider subscribing to [YouTube Premium](https://www.youtube.com/premium) — it is
the most direct way to support the artists and creators whose work this app streams.

### 4. No Hosting of Copyrighted Material
Orinify does not host, upload, distribute, or store any audio, video, or copyrighted media.
All content streamed through the app is served entirely from Google's/YouTube's own
servers and remains the property of its respective copyright owners.

### 5. User Responsibility
The software is provided "AS IS", without warranty of any kind. Users are responsible for
ensuring their use of this app complies with local law and the terms of service of the
platforms it accesses. Because no media is hosted here, DMCA takedown requests for audio
or video content cannot be processed by this project; legal concerns about the source code
itself can be raised via [GitHub Issues](https://github.com/hampikamayuq/Orinify/issues).

## License

GPL-3.0, inherited from [SimpMusic](https://github.com/maxrave-dev/SimpMusic). See
[LICENSE](LICENSE).
