---
description: Resolve a CurseForge project and file id conservatively for a mod dependency
agent: general
subtask: true
---

Resolve the correct CurseForge project and file id for a Minecraft mod dependency using a conservative, low-request workflow.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/curseforge-resolution.md`

User context:
$ARGUMENTS

Workflow contract:

1. Call `curseforge_helper` with `action: lookup_cache` first.
2. If cache already contains a matching project and file for the requested Minecraft version and loader, return that result immediately.
3. If the project is already known from a provided slug, project id, or CurseForge URL, skip search and go directly to the filtered files page.
4. If the project is unknown, call `curseforge_helper` with `action: build_urls`, then fetch exactly one search page with `webfetch` in `markdown` mode.
5. Parse that search page with `curseforge_helper` using `action: parse_search`.
6. If project resolution is not clearly verified, stop and ask the user instead of guessing.
7. Before each additional CurseForge request after the first one, wait about 2 seconds using a small shell sleep.
8. Fetch exactly one filtered files page with `webfetch` in `markdown` mode.
9. Parse it with `curseforge_helper` using `action: select_file`.
10. Scan only the first 20 file rows and choose first `R`, else first `B`, else first `A`, with exact Minecraft version and exact loader match required.
11. In `strict` mode, fetch the chosen file detail page and verify it with `curseforge_helper` using `action: verify_file`.
12. If anything looks unexpected at any stage, stop and ask the user.
13. On a resolved result, store the verified project and file in cache with `curseforge_helper` using `action: store_resolution`.

Required request policy:

- Never parallelize CurseForge requests.
- Keep the flow cache-first.
- Use at most one search page, one filtered files page, and one file page by default.
- Do not crawl pagination.
- Do not try to outsmart ambiguous results.

Expected output:

Return exactly this JSON object shape:

```json
{
  "status": "resolved | needs_confirmation | not_found | rate_limited | error",
  "query": {
    "input": "<original query>",
    "inputType": "<auto|name|slug|url|project-id>",
    "minecraftVersion": "<mc version>",
    "loader": "<loader>",
    "mode": "<strict|fast>"
  },
  "project": {
    "projectId": 0,
    "slug": "<slug>",
    "title": "<title>",
    "author": "<author>",
    "url": "<project url>",
    "verified": true,
    "verificationReasons": ["<reason>"]
  },
  "file": {
    "fileId": 0,
    "displayName": "<file display name>",
    "fileName": "<jar name or null>",
    "releaseType": "release|beta|alpha|null",
    "minecraftVersions": ["<mc version>"],
    "loaders": ["<loader>"],
    "url": "<file url>"
  },
  "confidence": "high | medium | low",
  "trace": ["<decision step>"],
  "alternatives": [],
  "warnings": []
}
```

Output rules:

- When confirmation is needed, set `status` to `needs_confirmation`, populate `alternatives`, and explain the unexpected result in `warnings`.
- When a value is unknown, use `null` instead of inventing it.
- Keep `trace` concise and ordered.
- Prefer exact, verified data over complete-looking but weakly inferred data.
