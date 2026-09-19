Product

<!-- impeccable:product-schema 1 -->Platform

android

Users

Orinify is designed primarily for personal use by its owner and a small circle of people
who receive the APK directly.

There is currently no store listing, public onboarding funnel, or large-scale distribution.
The normal use case is everyday music listening on a phone, with headphones, Bluetooth,
mobile data or Wi-Fi, often while the app is running in the background.

The core job is simple:

«Play what I want without ads, and help me decide what to listen to next based on what I
already enjoy.»

Public distribution remains a future possibility rather than a current product goal.

Product Purpose

Orinify is a personal YouTube Music client for Android designed around the listener rather
than around a generic recommendation feed.

The defining feature is its local listening history.

Orinify records playback events on the device and uses them to create personalized
experiences without requiring that history to be uploaded to an Orinify server.

Success means the app can open and immediately show something meaningful from the
listener's own history.

Positioning

Orinify combines YouTube Music playback with a local personalization layer built around
the "playback_event" database.

The main personalized experiences are:

- Rediscover — tracks played at least 3 times before a 60-day cutoff and not played
  since.

- You might like — a radio generated from the listener's strongest activity during the
  previous 90 days while excluding tracks already present in local history.

- Artist mixes — personalized artist radios ordered using local play counts.

- Recently played — the listener's local history ordered from newest to oldest.

- Wrapped — listening summaries generated from local playback data.

- Monthly recaps — playlist-style summaries generated for individual months.

- Analytics — on-device listening statistics including listening time, streaks, top
  tracks, artists and albums.

- Listening context in Now Playing — the player can show information such as play count
  and when the track was last heard.

The personalization model is local-first. Most of these features operate directly on
information stored on the device.

The main navigation follows the same principle:

Home is forward-looking:

- Rediscover
- You might like
- Artist mixes

Library represents what the listener keeps and what they have already done:

- Playlists
- Favorites
- Downloads
- Recently played
- Wrapped

A personal section should have one clear home rather than appearing redundantly across
multiple tabs.

Operating Context

Orinify is currently distributed as a sideloaded Android application.

Primary package:

"dev.diego.orinify"

Development builds may use a separate debug application ID.

Build artifacts are generated through:

".github/workflows/orinify-build.yml"

Android is the primary supported product platform.

Capabilities and Constraints

- minSdk 26
- targetSdk 36
- compileSdk 37
- UI: Jetpack Compose / Compose Multiplatform
- Design system: Material 3
- Playback: Media3 / ExoPlayer
- Database: Room, schema v26
- Dependency injection: Koin
- Networking: Ktor

Desktop and JVM targets also exist in the repository, but Android is the current product
focus.

Shared code must remain compatible with those targets where applicable.

The application ships with 26 locales. Any new user-visible string must therefore be
written with localization in mind.

Local listening tracking is configurable.

When tracking is disabled, features that depend on "playback_event" no longer have
listening history available. This affects:

- Rediscover
- You might like
- Artist mixes
- Analytics
- Wrapped
- listening context shown in the player

Empty states for these features are therefore part of the normal product experience.

Orinify includes three active Now Playing designs:

- Classic
- Material 3 Expressive
- Apple Music

Player changes must account for the style or styles affected.

Two lyrics presentation styles are also active.

Android "assembleDebug" and the GitHub Actions build are the primary available build
validation paths for the Android product.

Brand Commitments

- Name: Orinify
- Package: "dev.diego.orinify"
- Deep-link scheme: "orinify://"
- Primary visual identity: violet
- Accent seed: "#AD85FD"

The Orinify mark uses a headphone-and-play symbol over a violet gradient:

"#3B1E8A → #7C3AED"

The primary accent uses HCT tone 64 to maintain stronger contrast when the accent value is
used directly over AMOLED black surfaces.

The visual identity should remain recognizably Orinify across launcher icons, player
surfaces, navigation and promotional material.

Evidence on Hand

Current product decisions are based primarily on direct use of Orinify and the listening
history generated on the owner's device.

The "playback_event" database provides the real data used by the personalized features.

There is currently:

- no analytics backend
- no formal user research
- no public install count
- no testimonials
- no large external user base

Product documentation must not invent evidence that does not exist.

Product Principles

1. The listener's history is central to Orinify.
   
   Personal playback data should be used to make the application more useful to the
   listener.

2. Listening history stays local whenever possible.
   
   Features should not require uploading the user's full listening history to an Orinify
   backend.

3. Start with the listener.
   
   When both local history and generic recommendations have useful content, the listener's
   own activity should receive priority.

4. Empty states are real product states.
   
   A lack of listening data should produce a clear explanation rather than an endless
   loading state or broken interface.

5. Keep product behavior intentional.
   
   New features should strengthen the central listening experience rather than add
   complexity without a clear purpose.

6. Respect external services and dependencies.
   
   Third-party APIs, libraries, protocols and services should retain the names, licences
   and attribution required by their respective projects.

7. Android comes first.
   
   Product decisions should optimize the Android experience while avoiding unnecessary
   breakage of shared multiplatform code.

Accessibility & Inclusion

Orinify is currently designed primarily around a dark interface.

Several immersive surfaces use artwork-derived backgrounds, so contrast must be considered
whenever text or controls are placed over dynamic imagery.

The application also supports 26 locales, meaning layouts must tolerate substantial
variation in text length.

New interfaces should avoid assuming that English text dimensions represent every
supported language.

Privacy

Orinify does not require a proprietary analytics backend for its personalized listening
features.

Listening history used by the personalization system is stored locally in
"playback_event".

Features such as:

- Rediscover
- Wrapped
- monthly recaps
- Recently played
- listening analytics
- artist rankings

can therefore be generated directly on the device.

Some online functionality necessarily communicates with external services, including
YouTube Music and optional integrations selected by the user.

These integrations should remain separate from Orinify's own local listening-history
system.

Open Obligations

If Orinify moves from personal distribution toward a public release, several areas need to
be treated as release requirements:

- Licensing — ensure the distributed application and source comply with the project's
  GPL-3.0 licence and all applicable dependency licences.

- Third-party attribution — retain notices and attribution required by libraries,
  services, APIs and other external components.

- Translations — review supported locales and validate translated strings before
  presenting broad language support as production-ready.

- Unofficial APIs — YouTube Music integration depends on interfaces that may change
  without notice. Graceful failure and maintainability are therefore product concerns.

- Release signing — public distribution requires a stable signing and release process.

- Device validation — broader distribution requires testing beyond the owner's primary
  devices.

- Privacy communication — any public release should clearly explain what data stays
  local, what external services receive requests, and which integrations are optional.

Product Definition

Orinify is not defined simply by being another way to access YouTube Music.

Its identity comes from combining music playback with a personal, local listening memory.

The service knows what music exists.

Orinify knows what you listened to.
