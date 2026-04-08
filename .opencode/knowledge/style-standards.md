# Style Standards (Review Lens)

Use this file for style-focused review findings.

Primary references:

- `code-standards.md`
- existing local patterns near changed code

## Core checks

- Names are descriptive and consistent with module conventions.
- Method/class responsibilities remain focused; avoid unnecessary abstraction.
- Control flow is easy to follow and avoids avoidable branching complexity.
- Error handling is explicit and specific; no broad catch unless justified.
- Dead code, unused imports/members, and compatibility leftovers are removed.
- Logging and messages are actionable and not noisy.

## Finding boundaries

- Prefer `minor` or `nit` for style-only concerns.
- Escalate to `major` only if readability/maintainability risk is likely to cause defects.
- Do not classify purely stylistic disagreements as `major`.
