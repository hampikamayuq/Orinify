# Orinify

Fork pessoal e sustentável do [InnerTune](https://github.com/z-huang/InnerTune), voltado a seleção
determinística dos melhores streams de áudio realmente disponibilizados pelo YouTube Music à sessão
autenticada.

O Orinify não falsifica entitlement Premium, não cria formatos inexistentes e não anuncia áudio
lossless quando a fonte não é lossless. A prioridade é tornar codec, bitrate, cache, download e
falhas de reprodução observáveis e previsíveis.

## Estado

- Base: `upstream/dev` no commit `bfba5ecb`.
- Versão inicial: `0.5.10-q1` (`versionCode` 2601).
- Application ID: `dev.diego.orinify`.
- Namespace legado preservado para reduzir conflitos com o upstream.
- Distribuição inicial: flavor `foss`, sem Firebase Analytics, Crashlytics ou Performance.

## Build

Requisitos: JDK 17, Android SDK 35 e Build Tools 35.0.0.

```bash
./gradlew assembleFossDebug lintFossDebug testFossDebugUnitTest
```

A release exige uma keystore externa e as variáveis descritas em
[`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md). Keystores e credenciais são ignoradas pelo Git.

## Documentação

- [Arquitetura](docs/ARCHITECTURE-Q.md)
- [Pipeline de áudio](docs/AUDIO_PIPELINE.md)
- [Detecção Premium](docs/PREMIUM_DETECTION.md)
- [Política de cache](docs/CACHE_POLICY.md)
- [Plano de testes](docs/TEST_PLAN.md)
- [Sincronização com upstream](docs/UPSTREAM_SYNC.md)
- [Segurança](docs/SECURITY.md)
- [Processo de release](docs/RELEASE_PROCESS.md)
- [Plano de melhorias](docs/IMPROVEMENT_PLAN.md)

O projeto permanece sob GPL-3.0 e mantém a atribuição e o histórico do InnerTune. Consulte
[`UPSTREAM.md`](UPSTREAM.md) e [`LICENSE`](LICENSE).

---

## Upstream project: InnerTune

<img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" height="72">

A Material 3 YouTube Music client for Android

[![Latest release](https://img.shields.io/github/v/release/z-huang/InnerTune?include_prereleases)](https://github.com/z-huang/music/releases)
[![License](https://img.shields.io/github/license/z-huang/InnerTune)](https://www.gnu.org/licenses/gpl-3.0)
[![Downloads](https://img.shields.io/github/downloads/z-huang/InnerTune/total)](https://github.com/z-huang/InnerTune/releases)
[![Translation](https://hosted.weblate.org/widget/innertune/svg-badge.svg)](https://hosted.weblate.org/engage/innertune/)

[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/z-huang/InnerTune/releases/latest)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.zionhuang.music)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.zionhuang.music)


[Compare versions](https://github.com/z-huang/InnerTune/wiki/App-Versions)

## Features

- Play songs from YT/YT Music without ads
- Background playback
- Search songs, videos, albums, and playlists from YouTube Music
- Login support
- Cache and download songs for offline playback
- Synchronized lyrics
- Lyrics translator
- Skip silence
- Audio normalization
- Adjust tempo/pitch
- Dynamic theme
- Android Auto support
- Personalized quick picks
- Discord Rich Presence support

## Screenshots

<p float="left">
  <img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="200" />
  <img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="200" />
  <img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="200" />
</p>
<p float="left">
  <img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" width="200" />
  <img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" width="200" />
</p>

> [!WARNING]
>
>If you're in a region where YouTube Music is not supported, you won't be able to use this app
***unless*** you have a proxy or VPN to connect to a YTM supported region.

## FAQ

### Q: How to scrobble music to LastFM, LibreFM, ListenBrainz or GNU FM?

Use other music scrobbler apps. I
recommend [Pano Scrobbler](https://play.google.com/store/apps/details?id=com.arn.scrobble).

### Q: Why InnerTune isn't showing in Android Auto?

1. Go to Android Auto's settings and tap multiple times on the version in the bottom to enable
   developer settings
2. In the three dots menu at the top-right of the screen, click "Developer settings"
3. Enable "Unknown sources"

## Translation

If you'd like to help translate InnerTune, check out [our project in Weblate!](https://hosted.weblate.org/engage/innertune/)

> [!TIP]
> Preferably do not use pull requests/modify files yourself, if you can. Prefer Weblate when possible. 
> Doing so may cause merging issues, and may end up becoming unmergeable.
> It's also likely that other people have also gone and helped with the translation process of the language you want to update, but it hasn't been merged yet.

[![Translation status](https://hosted.weblate.org/widget/innertune/multi-auto.svg)](https://hosted.weblate.org/engage/innertune/)

Thank you for helping make InnerTune more accessible to people *worldwide!*

## Donate

If you like InnerTune, you're welcome to send a donation. Donations will support the development,
including bug fixes and new features.

<a href="https://liberapay.com/zionhuang"><img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/assets/liberapay.png" alt="Liberapay" height="60" ></a>
<a href="https://www.buymeacoffee.com/zionhuang"><img src="https://raw.githubusercontent.com/z-huang/InnerTune/dev/assets/buymeacoffee.png" alt="Liberapay" height="60" ></a>

## Credit

I want to give credit to [vfsfitvnm/ViMusic](https://github.com/vfsfitvnm/ViMusic) for being an
example of Jetpack Compose music player. It helped me a lot on my way to learn Compose and
Android development.

## Disclaimer

This project and its contents are not affiliated with, funded, authorized, endorsed by, or in any
way associated with YouTube, Google LLC, Innertune Media Inc., or any of its affiliates and
subsidiaries.

Any trademark, service mark, trade name, or other intellectual property rights used in this project
are owned by the respective owners.
