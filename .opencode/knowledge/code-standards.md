# Code Standards

Use these standards during coding and code review tasks.
Do not treat this file as global behavior guidance for non-coding tasks.

## 1) Prefer direct API over reflection

- Use direct calls when class, method, or enum is public in the target version.
- Use reflection only as an absolute last resort when there is proven version/API drift that cannot be handled cleanly with direct calls.
- Do not keep reflective helpers when a direct API path is available.

## 2) Avoid broad exception handling

- Do not use `catch (Exception)` for normal control flow.
- Catch specific exception types only when that failure mode is expected at runtime.
- Do not silently swallow exceptions unless fallback behavior is intentional and user-facing.

## 2a) Be strict with unchecked-exception control flow

- `try` / `catch` / `finally` is fine for checked exceptions and explicit resource or lifecycle handling.
- For unchecked exceptions, do not add `catch` or `finally` defensively unless there is a strong reason.
- When you do add one for unchecked exceptions, call it out to the user and explain why propagation is not enough and what failure it prevents.
- If that is not easy to justify, let the unchecked exception propagate.

## 3) Avoid impossible checks

- Do not add null checks for values from APIs that are not marked nullable.
- Do not add defensive branches for states that cannot occur in normal runtime.
- Remove dead fallback branches once code paths are simplified.

## 4) Prefer explicit constants over dynamic probing

- In fixed target versions, reference known constants directly.
- Avoid runtime scanning of enum names, ordinals, or reflection-based symbol discovery when stable constants are available.

## 5) Keep fallbacks intentional

- Fallback behavior should exist only for real, user-visible degradation paths.
- Fallbacks must preserve a clear user outcome and avoid masking programming errors.

## 6) Remove compatibility leftovers promptly

- When replacing compatibility code, remove now-unused helper methods, imports, and branches in the same change.
- Keep final codepath minimal and easy to audit.

## 7) Run an obviousness audit before commit

Before finalizing changes, quickly check for:

- unnecessary null checks
- broad exception catches
- dead code and unreachable branches
- reflection on public APIs
- dynamic probing where explicit constants exist

## 8) Do not leave unused code behind

- Remove unused fields, parameters, local variables, imports, and helper classes in the same change that makes them obsolete.
- If a concept is no longer used by the decision logic, remove it from DTOs/records and all call sites rather than keeping placeholders.
- Treat compiler warnings for unused members as cleanup tasks, not deferred work.

## 9) Container slot indexing rules

- Distinguish menu slot position from underlying container slot index.
- Use menu slot id (position in `menu.slots`) when calling `menu.getSlot(id)`.
- Use `Slot#getContainerSlot()` when you need index inside the backing inventory/container.
- For player inventory subsets, prefer `slot.container instanceof Inventory` and `getContainerSlot()` ranges (`0..8` hotbar, `9..35` main inventory).

## 10) Prefer primitives by default

- Use primitive numeric types (`int`, `long`, `double`, etc.) for fields,
  parameters, return types, local variables, and loop variables by default.
- Avoid boxed numeric types (`Integer`, `Long`, `Double`, etc.) unless they are
  absolutely necessary.
- Boxed numeric types are allowed only when required by nullability,
  generics/collections APIs, external API contracts, or framework constraints.
- When boxed types are required at boundaries, keep primitive types in internal
  logic whenever possible.

## 11) Avoid trivial single-use wrappers

- Do not introduce helper methods used from a single call site when they only
  wrap 1-2 obvious statements (simple assignment/call/forwarding).
- Single-use helpers are acceptable when they encapsulate non-trivial logic
  (for example: multi-step flow, branching, resource lifecycle, version/API
  adaptation, or other non-obvious intent).
- Prefer direct inline assignment/call when extraction adds indirection without
  improving clarity.

## 12) Commit message conventions

- Use conventional commit types used by this project: `chore`, `refactor`,
  `feat`, and `fix`.
- Include the matching emoji after the type prefix:
  - `chore: 🔧 ...`
  - `refactor: ♻️ ...`
  - `feat: ✨ ...`
  - `fix: 🐛 ...`
- Use imperative tense for `chore` and `refactor` summaries.
- Use past tense for `feat` and `fix` summaries, written from the player's
  perspective when possible, because these are included in player-facing
  changelogs.

## 13) Avoid diff churn from method movement

- Keep method ordering stable during edits unless method movement is explicitly
  requested or is required by the implementation.
- When changing an existing method, prefer editing it in place instead of
  deleting and re-adding it elsewhere in the file.
- Avoid incidental reordering of helpers, overrides, nested types, or fields
  when the task does not depend on that structure change.
- Treat reviewable diffs as a requirement: minimize non-functional churn so the
  semantic change stays obvious in git history and code review.

## 14) Prefer interface types for declarations

- Prefer interface or abstract contract types on the left-hand side of local
  variables, fields, parameters, and return types when the code only relies on
  the contract behavior.
- Use the narrowest abstraction that matches the need, for example `Collection`
  for iteration/size, `List` for ordered access, `Set` for uniqueness, and
  `Map` for key/value access.
- Instantiate the concrete implementation on the right-hand side, for example
  `Set<BlockPos> positions = new LinkedHashSet<>()`.
- Use a concrete type in the declaration only when the code depends on
  implementation-specific API or when the implementation choice itself is
  important to communicate.
- Do not expose stronger implementation details than necessary in internal code
  or public APIs.
