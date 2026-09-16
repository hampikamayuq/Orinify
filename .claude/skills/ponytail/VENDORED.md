# Vendored, not installed as a plugin

These six skills are copied from [DietrichGebert/ponytail](https://github.com/DietrichGebert/ponytail)
(MIT) at commit `e3ba2aa`, into `.claude/skills/` where this repository's own skills live.

Upstream ships them as a plugin marketplace (`/plugin marketplace add DietrichGebert/ponytail`).
That route is not available from a Claude Code remote session, which has no way to add a
marketplace — but a plugin's skills are ordinary `SKILL.md` files, and a project skill directory
is read the same way. So the skills are vendored and the plugin machinery is not.

**Only the skills are here.** Upstream also ships `hooks/` — JavaScript for a status line, a mode
tracker and an activation hook, wired through `settings.json`. Those execute code on every prompt
and were deliberately left out; the skills carry the behaviour on their own. The `/ponytail lite`
persistence that the hooks provide is the one thing lost: say the level when you invoke it.

Taking an upstream update means re-copying these files, not bumping a version.
