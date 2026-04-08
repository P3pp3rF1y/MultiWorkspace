# Sophisticated Portals

- Status: ideation
- Working title: Sophisticated Portals
- Recording policy: this file stores only confirmed direction agreed in chat.

## Confirmed concept

- Add portals with many more options than vanilla.
- Support more ways to create and link portals.
- Support masking/hiding the portal frame.
- Support very custom portal shapes.
- Support a true 1x1 portal block for small-entity use cases ("moles").
- Support custom colors and patterns for the portal surface.
- Support portal placement where only a portal controller/main block is required.
- Do not require mandatory surrounding blocks; players can leave the area open or build around it freely.
- Support horizontal portal placement.
- Include an item-based single-use portal (or equivalent) for temporary teleport use.
- Support teleporting individual entities.
- Support a "way back" behavior to portals on the other side.

## Confirmed linking and interaction direction

- Players should need to discover destinations before linking to them, or receive a link-enabling item/permission from a player who discovered them.
- Key-item linking is part of the core direction: use key on destination portal/controller, then place key into another portal/controller slot to link.
- Portal pairs should support there-and-back transport as a first-class use case.
- Support two key types: return-paired keys (there-and-back setup) and one-way keys.
- One-way portals are created by linking only one direction (no return key installed).
- Return-paired setup flow: player crafts a paired two-way key set, binds it at portal A (one key is installed there), then uses the remaining key at portal B to complete both-way linking before first traversal.
- Return-paired setup should prevent "forgot to install return key" failure by design while keeping key consumption understandable.
- One portal should be able to switch between different destinations.
- Atlas-style destination knowledge/selection is also in scope.
- Channel-based linking is deferred as a potential future enhancement, not current core scope.
- Portal controller owns access control and is the authority for portal use permissions.
- Access modes should include at least: owner-only, team-only, and open-to-everyone.
- Permission checks apply to both source use and destination arrival.
- A linked destination should still reject travel if the traveler lacks access at teleport time.
- Access control, not key revocation, is the primary way to disable previously allowed travel.
- Portal settings are owner-controlled; for public one-way portals, owner can keep portal open but avoid adding reverse link keys.
- Target selection should use GUI for now, with radial selection as the preferred first UX.
- If destination count becomes large (for example more than about 15-20), fall back from radial-only to a paged list style selector.
- Radial should be the default view and show key destinations first.
- At larger destination counts, support favorites and prioritize them in radial.
- Provide a list view for non-favorites/remaining destinations so players do not need to browse favorites again.
- Atlas redirect is per-player and per-use only; it does not modify portal defaults.
- If multiple players enter together, each player's destination is resolved from their own atlas choice (or portal default if they made no override).
- Public one-way portal patterns are in scope (for example sending entities/mobs into a grinder destination without a return path).

## Linking technical contract (confirmed)

- Two-way key pairs carry a shared link ID imprinted when crafted.
- Link registry is world-scoped persistent data (cleared from memory on unload, reloaded from world save on load).
- Registry stores endpoint positions as dimension plus block position.
- Portal identity for linking is position-based by design.
- Link lifecycle states are `empty`, `half-bound`, and `complete`.
- When first key of a pair is used, registry records first endpoint and key state becomes `half-bound`.
- When second key is used, registry records second endpoint and link becomes `complete`.
- If one endpoint is missing/broken, portal does not activate until endpoint is valid again.
- Permission checks happen at traversal time, so permission changes can deny travel even for an already complete link.
- Return keys can stack until encoded; encoded return keys only stack with same encoded ID.
- Once a link ID is `complete`, additional key uses with that same ID are rejected.

## Confirmed portal state indicators

- `unlinked`: no active portal surface is shown in the frame.
- `one-way outbound`: normal outbound teleport works; while teleporting, player gets a clear indication that this route has no guaranteed return path.
- `one-way inbound`: arrival is allowed, but attempting to use this portal as source shows a denied traversal indication (message or immersive feedback).
- `two-way`: regular teleport visuals and behavior.
- Distance-readable icon cue is in scope as a primary indicator for one-way vs two-way behavior.
- One-way states should have a distinct default texture/color presentation.
- Players may override visual design choices; if they do, readability tradeoffs are player-owned.

## Open questions tracker

- What exact denied-access feedback style should be default (chat/actionbar/title/particles/sound), and what should be configurable?
- How should key crafting/progression be balanced across early, mid, and late game?
- How should destination discovery be granted for keys vs atlas entries (use, discovery, or both)?
- Should a non-overridable minimal icon/marker remain visible even when players fully customize portal visuals?
