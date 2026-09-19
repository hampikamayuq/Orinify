<div align="center">
<h1>Orinify</h1>
A personal, ad-free YouTube Music client for Android and Desktop, built from the ground up with Compose Multiplatform and designed around the listener.
<br>
<br>
<a href="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml"><img src="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml/badge.svg"></a>
<a href="https://github.com/hampikamayuq/Orinify/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue"></a>
<br>
<br>
<h4>Download</h4>
Debug builds are published as artifacts on every push — grab the latest one from the
<a href="https://github.com/hampikamayuq/Orinify/actions/workflows/orinify-build.yml">Actions tab</a>
(pick the most recent successful run, then download <code>orinify-debug-arm64</code> for most phones).There is currently no signed release and no store listing. Orinify is a personal,
independently developed application distributed primarily through sideloaded builds
(package <code>dev.diego.orinify</code>).

</div>What makes Orinify different

Most YouTube Music clients can play what YouTube recommends. Orinify goes further by
building its own listening experience around a local <code>playback_event</code> database
that stays on your device.

- Rediscover — tracks played at least 3 times before a 60-day cutoff and not played since
- You might like — radio generated from your last 90 days of listening; this is the only
  personalized list that requires a network request
- Artist mixes — one radio per artist, ranked according to your own listening history
- Recently played — a local view of your recent listening activity
- Wrapped — personalized listening summaries generated from your local history
- Monthly recaps — automatic playlist-style summaries for each month
- Analytics — on-device statistics including listening clock, streaks, top tracks,
  artists and albums
- Listening context in the player — every Now Playing style can show how many times
  you have played a track and when you last listened to it

Listening history is processed locally. Disabling local tracking simply stops and clears
the personalized listening-history features without affecting the rest of the app.

Features

- Play music from YouTube Music or YouTube for free, without ads and with background playback
- Three Now Playing styles: Classic, Material 3 Expressive and Apple Music
- Ten-band equalizer with presets and AutoEq headphone profiles
- Delay and Reverb audio effects
- Word-by-word Apple Music-style lyrics
- Lyrics romanization for 12 languages
- Share lyrics as an image
- High-quality streams up to 256 kbps using Opus or AAC for YouTube Music Premium accounts
- Browse Home, Charts, Podcasts, Moods & Genres using YouTube Music data
- Search across YouTube
- Spotify Canvas and animated album artwork
- Crossfade with DJ-style transitions
- Listen Together rooms for synchronized playback with friends
- Wire compatibility with "Metrolist" (https://github.com/MetrolistGroup)
- AI song suggestions
- AI-powered lyrics translation using your own OpenAI or Gemini API key
- Import playlists converted from Spotify and other apps
- Multiple YouTube account support
- Custom local playlists with YouTube Music synchronization
- Caching and offline playback
- Downloads with metadata
- Sleep timer
- Android Auto support
- Discord Rich Presence
- Last.fm scrobbling
- Google Cast support
- SponsorBlock support
- Return YouTube Dislike integration
- Synced lyrics from SimpMusic Lyrics, LRCLIB, Spotify (requires login) and YouTube Transcript
- Desktop application for Windows, macOS and Linux with its own mini player

«Note. Orinify is a personal, independently developed application with no store
listing and limited distribution. It relies on YouTube Music's unofficial/internal APIs,
which may change without notice and can occasionally cause features to stop working.»

Desktop app

Orinify also includes a native desktop application built from the same multiplatform
codebase.

Which file should I download?

- Windows: ".msi" installer
- macOS: ".dmg" for ARM or x86-64
- Linux: ".AppImage"

The desktop version uses
"libmpv" (https://github.com/mpv-player/mpv) for media playback instead of
Media3/ExoPlayer.

See "CLAUDE.md" for desktop-specific build information, including "mpvSetupAll",
platform-native dependencies and known limitations.

Data & Privacy

Privacy-sensitive functionality is designed to remain local whenever possible.

- Listening history is stored in the local <code>playback_event</code> database and is not
  uploaded by Orinify.
- Personalized features such as Rediscover, Wrapped, monthly recaps, listening analytics
  and artist rankings are generated from this local history.
- Orinify has no proprietary analytics backend, advertising tracker or crash-reporting
  service in the FOSS build.
- YouTube Music data is retrieved through unofficial/internal YouTube Music APIs.
- When a logged-in account has YouTube's own Send back to Google functionality enabled,
  playback activity may be submitted to Google through YouTube's tracking APIs.
- Spotify's Web API is used for supported Spotify integrations such as Canvas and lyrics.
- AI features only communicate with the provider selected by the user and require the
  user's own OpenAI or Gemini API key.

Development

Orinify is an independently developed Compose Multiplatform project targeting Android
and desktop platforms.

The application shares its core architecture and application logic across platforms while
using platform-specific playback implementations where necessary:

- Android: Media3 / ExoPlayer
- Desktop: libmpv
- UI: Compose Multiplatform
- Local personalization: on-device playback history and analytics
- Online music source: YouTube / YouTube Music
- Desktop targets: Windows, macOS and Linux

The project is developed openly on GitHub, with automated builds available through
GitHub Actions.

License

Orinify is open-source software distributed under the
"GPL-3.0 license" (https://github.com/hampikamayuq/Orinify/blob/main/LICENSE).

Third-party services, libraries and integrations referenced by the project remain subject
to their respective licenses and terms.
