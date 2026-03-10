# AGENTS.md

## Project context
This repository contains a Minecraft mod built for Forge / NeoForge.

Prefer solutions consistent with common Minecraft modding practices and the
existing project architecture.

## Working style
- Make minimal, focused changes.
- Preserve existing style, structure, and naming conventions.
- Avoid unrelated refactors unless explicitly requested.

## Code awareness
Before modifying behavior, review nearby code and related systems to keep
changes consistent.

Be careful with areas that commonly have side effects:
- client vs server logic
- rendering
- networking
- data generation
- registries
- save compatibility

If changes could affect these systems, mention the potential impact.

## Minecraft mod architecture hints
- Client-only code must stay in client packages or client initialization.
- Do not reference client classes from common or server code.
- Follow existing project patterns for registries, networking, and data generation.
- Prefer extending existing utilities instead of introducing new frameworks.

## Source and API accuracy
Do not infer Minecraft, Forge, or NeoForge internals or APIs when the relevant
source is unavailable or unclear.

When unsure about a class, method, signature, or behavior, inspect the existing
project code first and otherwise ask for the relevant source or dependency code.