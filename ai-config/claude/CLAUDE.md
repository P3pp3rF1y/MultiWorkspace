# CLAUDE.md

## General approach
- Prefer minimal, localized changes over refactors.
- Follow existing patterns in the repository.

## Understanding code
- Before editing, inspect nearby usages and related classes if the behavior may depend on other files.
- Do not assume the currently open file tells the full story.

## Dependency sources
- Do not infer Minecraft, NeoForge, or dependency internals.
- If framework source is needed, ask for the exact class or method instead of guessing.

## Scope control
- Treat small bug fixes or tweaks as simple tasks and avoid long planning.
- Do not introduce new abstractions unless clearly needed.