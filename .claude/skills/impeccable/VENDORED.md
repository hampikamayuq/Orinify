# Vendored, and deliberately incomplete

Copied from [pbakaus/impeccable](https://github.com/pbakaus/impeccable) (Apache 2.0) at commit
`f2c7051`: `plugin/skills/impeccable/SKILL.md` plus all 41 reference files, into `.claude/skills/`
where this repository's own skills live. Upstream installs with `npx impeccable install`, which a
Claude Code remote session cannot run.

**Only the markdown is here — 504 KB of the 2.2 MB.** The 1.7 MB left behind is `scripts/`: a
launcher plus a self-contained binary it downloads on first run, four browser-injection scripts
(`live-browser*.js`), `modern-screenshot.umd.js`, and a font index. All of it drives a DOM, and
this app has none.

`SKILL.md` is verbatim, launcher step included, because it documents its own fallback: *"Launcher
unavailable: On refusal or failure, send a separate message before the next tool call... Then read
existing PRODUCT.md and DESIGN.md without inventing missing context, follow applicable steps 2-3,
and continue through permitted tools."* Expect that message on every invocation. Editing the step
out would be a fork of someone else's skill to maintain against every update, for a line that is
already handled.

## What works here and what does not

Impeccable calls itself design guidance for **frontend** work, and most of it is. But
`reference/audit.native.md` and `reference/adapt.native.md` route to `reference/android.md`, and
those are written against Compose: `LazyColumn` recycling, `dp` versus `sp`, 48 dp touch targets,
TalkBack traversal, Reduce Motion. That half applies to this app directly.

Dead here: `live` and `generate` (both drive a browser), the 61-rule deterministic detector and
`impeccable detect`, the edit hooks, and `doctor` (it reports drift in artifacts the launcher
writes). `audit.native.md` says so itself — *"no browser tooling or `impeccable detect` applies"*.

The commands that are pure judgement — `critique`, `layout`, `typeset`, `colorize`, `distill`,
`quieter`, `bolder`, `harden` — need no tooling at all and are the reason this is here.

Taking an upstream update means re-copying these files, not bumping a version.
