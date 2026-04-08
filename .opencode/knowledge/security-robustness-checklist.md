# Security and Robustness Checklist

Use this checklist for security-adjacent and robustness-focused review findings.

## Trust boundaries

- Treat network payloads and client-originated input as untrusted.
- Validate payload fields before use (ranges, nullability, identity, ownership).
- Ensure server-authoritative checks exist before state mutation.

## Input and state validation

- Validate config/data-driven values before applying behavior.
- Guard against malformed or missing data in serialization/deserialization paths.
- Use explicit failure handling for invalid states, with safe fallback where appropriate.

## Crash and abuse resistance

- Avoid obvious crash vectors from bad indices, null chains, or unchecked casts.
- Ensure loop bounds and recursion cannot be externally amplified into runaway work.
- Avoid log spam or exception spam from repeatedly triggerable bad input.

## Permission and authority

- Operations with gameplay/world impact enforce correct permission/context checks.
- Client-side hints do not imply server-authorized success without server confirmation.

## Severity calibration

- `blocker`: exploitable trust-boundary issue or high-probability crash vector.
- `major`: serious robustness gap with realistic trigger path.
- `minor`: hardening opportunity with bounded impact.
- `nit`: low-impact defensive cleanup.
