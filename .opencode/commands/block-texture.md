---
description: Iterate on a production-quality Minecraft block texture
agent: general
subtask: true
---

Iterate toward a production-quality 16x16 Minecraft block texture for Sophisticated Storage / Sophisticated Backpacks.

User context:
$ARGUMENTS

Workflow requirements:
- Start by identifying the texture slot and material balance from the user context
- Use a two-phase workflow by default:
  1. draft exploration phase
  2. production refinement phase after user selection
- Explore multiple distinct design directions before converging; default to 3 variants unless the user explicitly requests a different count
- Derive the actual direction themes from the user's prompt and references instead of imposing fixed built-in visual directions
- In the draft exploration phase, create rough but readable draft textures or blockouts for each direction; these do not need to be production quality yet
- In the draft exploration phase, save reviewable draft artifacts for each direction and treat them as the primary handoff for user selection
- Present the draft variants for review and let the user choose one or more directions to continue, unless the user explicitly asks for autonomous convergence
- For each direction, create or assemble a simple 16x16 reference texture/blockout that captures the intended structure and material balance
- Use those per-direction reference textures as img2img inputs; do not rely on prompt-only exploration when a reference can be made
- In the production refinement phase, iterate each selected variant independently: generate from its reference, review critically, refine/polish with another model pass when it is close, and if it drifts or stalls, step back to edit the reference texture and restart from that variant's updated reference
- When multiple selected variants are meaningfully different, continue them in parallel; when further variants would be nearly identical, continue with a single strongest pass instead of fabricating redundant options
- If a user-provided reference image path is provided, incorporate it into the most appropriate direction-specific reference instead of skipping the multi-direction exploration
- Do not stop to ask for confirmation before the first generation pass, first cleanup pass, or first reference revision; proceed with the strongest reasonable default and only ask if blocked
- Prefer `dreamshaperPixelart_v10.safetensors` as the base checkpoint
- Prefer `minecraft_pixelart_v2-000006.safetensors` as the LoRA
- Prefer lower CFG / lower denoise for structured block textures
- Reject results with anti-aliasing, concentric rings, emblem-like symbols, or noisy center artifacts
- For stone-like materials, prefer calm slab-like surfaces over scattered speckle

Output format:
- Briefly summarize each explored direction
- Clearly separate draft exploration results from later production refinement results
- List the draft or refined artifact paths first for each phase so the user can open them directly
- For each direction, describe the reference texture/blockout it will use
- If the user has not selected a direction yet, stop after presenting the drafts and recommend which 1-2 directions are strongest
- If the user has selected directions, state which direction(s) you pursued first and why
- Provide the exact positive prompt, negative prompt, and generation settings for each attempted pass
- Review each result critically, including whether you polished it further or stepped back to revise the reference texture
- End with the current best variant and the next iteration you would run if needed
