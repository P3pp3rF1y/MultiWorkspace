# Minecraft Mod Feature Brainstorming Guide

Use this guide when exploring potential features before design/implementation.

## Brainstorming goals

- Propose ideas that are fun, understandable, and useful in normal survival gameplay.
- Keep ideas aligned with existing mod identity and module boundaries.
- Prefer incremental additions that compose with current systems over disconnected one-off mechanics.

## Gameplay evaluation lenses

- Player value: what player problem or motivation does the feature address?
- Learnability: can players discover and understand the feature without heavy documentation?
- Progression fit: where does it sit in early/mid/late game?
- Balance: is the reward proportional to cost/complexity?
- Automation surface: does it integrate with existing automation and inventories?

## Technical feasibility lenses

- Module fit: choose the smallest correct module for the feature.
- Compatibility risk: avoid breaking save/network behavior without explicit migration plan.
- Side safety: ensure no client-only dependencies leak into common/server flow.
- Datagen/content impact: account for recipes/tags/loot/model/lang work if needed.
- Portability: keep concepts portable across maintained MC 1.20+ lines.

## Workspace context reminders

- `Reliquary`: standalone content module with its own gameplay identity.
- `SophisticatedCore`: shared systems and reusable infrastructure.
- `SophisticatedBackpacks`: mobile storage and related upgrades/interactions.
- `SophisticatedStorage`: placed storage blocks and related upgrades/interactions.
- `SophisticatedItemActions`: action-oriented systems usable by mods in the workspace.
- `SophisticatedStorageInMotion`: storage-in-motion oriented extension module.
- Integration modules should stay focused on bridge behavior.

## Output expectations for idea sessions

- Provide 3-7 feature options, grouped by ambition (`small`, `medium`, `large`).
- For each option include:
  - short pitch
  - why players care
  - likely modules touched
  - main risks
  - MVP scope (smallest shippable version)
- End with a recommended top 1-2 options and why.

## Recording policy for knowledge files

- Brainstorm freely in chat, but record only confirmed direction in project knowledge files.
- Do not write speculative options to knowledge files unless the user explicitly agrees to keep them.
- Keep unresolved ideas in chat until they are accepted; then add them as concise confirmed bullets.

## Active critique policy

- Be proactively opinionated during brainstorming instead of only echoing user ideas.
- For each major idea, call out at least one potential improvement and at least one realistic risk or tradeoff.
- When raising a concern, also provide a concrete alternative or mitigation.
- Mark suggestions as either `recommended now`, `defer`, or `watch` so direction remains clear.
- Keep critiques concise and practical, focused on gameplay value, usability, balance, and implementation risk.

## Knowledge edit transparency

- When updating project knowledge files during brainstorming, always report exactly what changed in chat.
- Include changed file paths and concise bullet points for added/removed/updated decisions.
- Keep the report short, but specific enough that the user does not need to open a diff to understand the update.

## Project documentation split policy

- During early ideation, default to a single project `index.md` as the primary source of truth.
- Recommend splitting a section into a dedicated file when any trigger is met:
  - section grows beyond roughly 100 lines
  - section accumulates more than 5 unresolved decisions
  - section has been updated in 3 or more separate brainstorm/design sessions
  - section needs its own lifecycle (for example detailed balance tables or decision history)
- When a split is recommended, explicitly notify the user and include:
  - section name
  - split reason
  - proposed filename
  - note that project `index.md` should keep a concise summary plus link
