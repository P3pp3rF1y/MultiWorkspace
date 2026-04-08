---
description: Explore draft directions for a Minecraft block texture
agent: general
subtask: true
---

Create reviewable draft directions for a 16x16 Minecraft block texture for Sophisticated Storage / Sophisticated Backpacks.

User context:
$ARGUMENTS

Workflow requirements:
- Start by identifying the texture slot, likely material balance, and any style cues from the user context
- Default to 3 draft variants unless the user explicitly requests a different count
- Derive the actual draft directions from the user's prompt and references instead of imposing fixed built-in visual themes
- Use existing project textures as references whenever possible
- For each draft direction, create or assemble a simple 16x16 reference texture/blockout that captures the structure and material balance
- Produce reviewable draft artifacts first; prefer image previews or blockout files per variant over a notes-only response
- Name draft artifacts clearly by asset and variant so the user can open them directly
- Create a companion notes file only when it adds value for prompts, references, or review guidance; do not make it the primary output when reviewable draft artifacts can be produced
- Prefer reference-first img2img exploration when a blockout/reference can be made; use txt2img only for very rough ideation when structure is still unknown
- Keep draft outputs readable and directionally distinct, but do not spend iteration budget trying to make them production quality yet
- If a user-provided reference image path is provided, incorporate it into the most appropriate draft direction
- Recommend the strongest 1-2 draft directions to continue into production refinement
- Do not move into heavy polish unless the user explicitly asks for autonomous convergence
- Prefer `dreamshaperPixelart_v10.safetensors` as the base checkpoint
- Prefer `minecraft_pixelart_v2-000006.safetensors` as the LoRA
- Prefer lower CFG / lower denoise for structured block textures
- Reject draft results with anti-aliasing, concentric rings, emblem-like symbols, or noisy center artifacts

Output format:
- Briefly summarize each draft direction
- List the generated draft artifact paths first so the user can review them immediately
- For each draft direction, describe the reference texture/blockout it uses
- Provide the exact positive prompt, negative prompt, and generation settings for each attempted pass
- Review each draft critically in terms of structure, material balance, and style fit
- Recommend which 1-2 directions should move into `/block-texture` refinement next
