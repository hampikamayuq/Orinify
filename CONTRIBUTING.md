# Contributing to Orinify

Orinify is a personal fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic),
maintained by one person, mostly with AI-assisted development (Claude Code). It has no
store listing and no formal contributor process — but issues and PRs are still welcome if
something is broken or worth adding.

## The short version

1. **Open an issue first for anything non-trivial**, so the change is agreed before the
   code exists. A typo fix or an obvious bug fix doesn't need one.
2. **Branch from `main`.** There is no separate `dev`/release split here.
3. **Follow the code around you.** Kotlin official conventions, Compose single-source-of-truth,
   Clean Architecture layer rules — match what the neighbouring files already do. See
   `CLAUDE.md` for this repo's own architecture notes and recurring gotchas.
4. **Fill in the PR template.**
5. **Translations**: this fork inherited 26 locales from upstream's Crowdin project, but
   has no Crowdin project of its own — a string added here ships untranslated until
   someone translates it directly in the repo. PRs editing string files are fine.

## AI policy

AI assistance, including AI-*driven* work, is fine here — this fork itself is built that
way, commit trailers and all. The bar is the same either way: a human (the maintainer, for
now) reviews what lands and is responsible for it. If you're contributing AI-generated
code, say so and make sure you've actually read it; a PR nobody can explain doesn't get
merged regardless of who or what wrote it.

## Code of conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
