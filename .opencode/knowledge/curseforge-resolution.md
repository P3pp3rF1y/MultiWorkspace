# CurseForge Resolution

Use this workflow when resolving a CurseForge project and file id for a Minecraft mod dependency.

## Goals

- Resolve a verified CurseForge project from a name, slug, URL, or project id.
- Resolve a matching file id for an exact Minecraft version and loader.
- Keep request volume low and stop on unexpected results instead of guessing.

## Request policy

- Prefer cache hits before any CurseForge request.
- Never parallelize CurseForge requests.
- Insert a short delay between consecutive CurseForge requests.
- Default maximum request shape:
  - 0 requests on cache hit
  - 1 search request max when the project is unknown
  - 1 filtered files page request
  - 1 file detail verification request in strict mode
- Do not paginate search or files pages by default.
- Do not inspect more than the first 20 file rows on the filtered files page.

## Preferred sources

- Search page only to identify the project when the project is unknown.
- Filtered files page as the main file selection source:
  - `.../files/all?page=1&pageSize=20&version=<mc>&gameVersionTypeId=<loaderId>&showAlphaFiles=show`
- File detail page only to verify the chosen file in strict mode.

## Loader ids

- Forge: `1`
- Fabric: `4`
- Quilt: `5`
- NeoForge: `6`

## File selection rule

- Scan only the first 20 filtered rows.
- Require exact requested Minecraft version and exact requested loader.
- Choose by release tier `R > B > A`.
- Within the chosen tier, take the first row because the page is newest-first.
- If parsing looks incomplete or contradictory, stop and ask.

## Confirmation rule

Ask the user instead of guessing whenever results are unexpected, ambiguous, incomplete, or inconsistent.

Examples:

- search results do not yield one clearly verified project
- files page does not contain an exact version + loader match
- page structure differs enough that parsing confidence drops
- file verification metadata contradicts the files list
- request budget would need to exceed the conservative default
