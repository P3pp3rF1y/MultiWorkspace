# Performance Checklist

Use this checklist for performance-focused review findings.

## Hot-path checks

- Tick/event/render paths avoid repeated expensive work.
- World/chunk/entity scans are bounded and justified.
- Repeated calculations are cached when safe and beneficial.
- Allocation churn in loops is minimized (especially boxing and transient objects).

## Networking checks

- Payload size is proportionate to update frequency.
- Sync frequency avoids redundant packets.
- Expensive serialization/deserialization is not done more often than needed.

## Data and IO checks

- Disk/persistence writes avoid unnecessary frequency.
- Datagen/runtime data transforms avoid repeated full recompute without need.

## Severity calibration

- `blocker`: clear runaway/high-frequency path likely to cause severe lag or instability.
- `major`: high-likelihood user-visible regression in common gameplay paths.
- `minor`: measurable inefficiency with limited scope.
- `nit`: micro-optimization suggestion with low impact.
