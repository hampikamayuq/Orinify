# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

One listener — the owner — plus a small circle he hands the APK to directly. No store
listing, no onboarding funnel, no anonymous first-run traffic. Everyone who opens it got
it from him.

The situation is ordinary daily listening: phone, headphones or Bluetooth, often on 5G,
music in the background while doing something else. The job is "play what I want without
ads, and help me find what to play next out of what I already listen to."

Publishing is a live possibility, not a plan: *"and later if it turns out good, publish
it."* That makes the obligations below real rather than theoretical, and it is why they
are recorded as facts instead of deferred.

## Product Purpose

A personal fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic) that streams
YouTube Music without ads or tracking, and that **knows the listener**. Success is that
the app opens and the first thing on screen came from this listener's own history rather
than from YouTube's recommendation shelf.

## Positioning

Every YouTube Music client can play what YouTube suggests. This one builds lists from
`playback_event`, a table that never leaves the device:

- **Rediscover** — tracks played at least 3 times before a 60-day cutoff and not once since.
- **You might like** — radio seeded from the top of the last 90 days, minus everything
  already in the history. The only list here that touches the network.
- **Artist mixes** — one radio per artist by play count over 90 days.
- **Recently played** — the history itself, newest first, one row per track; the spine of
  the Library tab and the one list no stock client can show.
- Wrapped, monthly recap playlists, and the Analytics tab, all from the same table.

Neither the upstream nor a stock client can copy this without collecting the history
server-side. The mechanism is that the history is local and stays local.

The two tabs split the history by direction: **Home is forward-looking** (Rediscover, You
might like, Artist mixes) and **Library is what you keep and what you did** (playlists,
favorites, downloads, Recently played, Wrapped). A list belongs on one tab, never both.

## Operating Context

Installed by sideload as `dev.diego.orinify` (debug builds carry `.dev`), so it sits
beside SimpMusic rather than replacing it. Delivered as a GitHub Actions artifact, built
by `.github/workflows/orinify-build.yml`, because the APK is far past any chat upload
limit. Android only in practice.

## Capabilities and Constraints

- **minSdk 26, targetSdk 36, compileSdk 37.** Jetpack Compose, Material 3, Media3/ExoPlayer,
  Room (schema v26), Koin, Ktor.
- **The Desktop app is out of scope for design, and still in the repository.** `desktopApp`
  and the JVM media stack build and must keep building; nothing designed for the phone may
  break `commonMain` for them. It cannot be run or inspected from the build container.
- **26 locales** ship, inherited from upstream's Crowdin. Every user-visible string added
  here lands untranslated until someone translates it.
- **Local tracking is a setting, and it gates the personal half of the product.** With it
  off, `playback_event` is empty and Rediscover, You might like, Artist mixes, Analytics
  and Wrapped have nothing to show. Their empty states are load-bearing, not decoration.
- **Three Now Playing styles** (Classic/Spotify, M3 Expressive, Apple Music) and two lyrics
  styles are all live. A change to the player must say which style it is changing.
- **The JVM test targets cannot be built in this container** — Maven Central resolution
  fails for artifacts the Android build does not cache. Android `assembleDebug` plus CI is
  the only gate available.

## Brand Commitments

- **Name:** Orinify. The app says so in every language as of `a131af1c`.
- **Mark:** a violet headphone-and-play glyph on a `#3B1E8A → #7C3AED` gradient, in
  `ic_launcher_foreground.xml` and its three sibling surfaces.
- **Deep link scheme:** `orinify://`.
- **Accent:** `seed = #AD85FD` — the icon's hue at HCT tone 64. Closed the icon/interface
  disagreement. The tone is set by the ~50 places `seed` is used LITERALLY over AMOLED black,
  not by the generated scheme: `PaletteStyle.TonalSpot` reads only the seed's hue, so the icon's
  own `#7C3AED` would have produced a byte-identical palette while dropping every literal accent
  to 3.7:1, under WCAG AA. Tone 64 gives 7.6:1.

## Evidence on Hand

Real, and all local: the owner's own `playback_event` history is what every personal list
renders from. There is no user research, no analytics backend, no testimonials, no install
counts — and future work must not invent any. The one screenshot of the running app in this
project's history is the Home screen at 21:40 on a Samsung device.

## Product Principles

1. **The history is the product, and it stays on the device.** Any feature that would need
   it uploaded is the wrong feature.
2. **Start from the listener, not from YouTube.** When both have something to show, the
   listener's own history goes first.
3. **An empty personal list is a state, not a bug.** It means "not enough listening yet"
   and must say so; a spinner that never resolves is the failure mode to avoid.
4. **Credit what is not ours.** This is a GPL-3 fork of someone else's work, carrying
   third-party services under their own names.
5. **Inherit upstream deliberately.** `core/` is vendored, not a submodule, so every
   divergence is a merge cost paid later. Change what serves the product; leave the rest.

## Accessibility & Inclusion

No requirement has been established with the user, and none is inferred here. Two facts
that constrain any future one: the app is dark-first with artwork-derived backgrounds on
several immersive screens, and 26 locales mean text length varies far beyond the English
that gets designed against.

## Open Obligations

These follow from *"publish it later if it turns out good"* and are unresolved today:

- **GPL-3.** The fork inherits it. Publishing means publishing this source, keeping the
  licence, and stating the changes made.
- **Attribution.** Upstream credit survives in `credit_app` and the two promo strings, on
  purpose. But commit `a131af1c` renamed *SimpMusic Lyrics* and *SimpMusic Charts* to
  *Orinify Lyrics* and *Orinify Charts* on the owner's explicit instruction, and those are
  maxrave's services, not this fork's. Private use, harmless. Published, it credits the
  wrong provider and needs reverting for those two names.
- **Translations.** 313 translated strings were renamed automatically and 100 unused keys
  deleted; no locale has been read by a speaker.
