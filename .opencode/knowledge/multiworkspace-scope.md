# Multiworkspace Scope Guidance

Apply this guidance in repositories or folders that are part of a `MultiWorkspace*`
layout.

## Default scope

- Do not search across all workspaces by default.
- Prefer this scope order:
  1. explicitly named workspace/version
  2. current workspace
  3. nearest matching module in the current workspace
  4. other workspaces only when the user asks for comparison/porting or local
     evidence points there

## Starting points by task type

- Rendering/UI bug:
  - start from the screen, renderer, render data, or menu class tied to the
    symptom
- Inventory/slot interaction bug:
  - start from the menu, slot, packet, or client screen handling the interaction
- Build/performance/config task:
  - start from root Gradle files and workspace-level config before submodule
    build files
- Test failure:
  - start from the failing test and nearest production code only
- Command/skill/agent behavior:
  - start from `.opencode`, `ai-config`, commands, agents, skills, and
    knowledge files
- Version/regression comparison:
  - start from the current implementation, then compare the matching file/class
    in the reference version

## Expansion rules

- If you expand beyond the current workspace, state that briefly and explain why.
- If a prompt names a version but not a module, infer the narrowest likely module
  first rather than scanning the whole workspace.
- For cross-version comparison, compare the matching file/class first before
  broad package-level search.

## Ask before broad multiworkspace exploration

Ask one targeted clarifying question before broad multiworkspace exploration if
both are true:

1. more than one workspace/module is a plausible target
2. choosing wrong would likely trigger wide cross-workspace search or rework

Good triggers:

- more than one workspace version is plausible
- the prompt could target shared code or a specific mod module
- the task could be design-only, diagnose-only, implementation, or review
- no likely anchor file/class/package is identifiable from the prompt

When asking, include:

- recommended default workspace/module/anchor
- the main plausible alternative
- what exploration would broaden if not clarified

## Avoidable exploration patterns

Avoid these unless already narrowed by evidence:

- scanning all modules when the issue appears local to one module
- searching all workspaces before checking the current one
- reading many build files before checking root build wiring
- reading broad knowledge trees when the task is about one command or workflow
- repeating broad searches after the user corrected workspace, module, or task
  mode

## User corrections

If the user corrects scope or intent, tighten the search immediately.

Treat corrections like these as narrowing instructions:

- wrong workspace
- wrong module
- wrong screen/feature
- design only, not implementation
- diagnose only, not fix yet

Do not respond to those by re-exploring broadly again.
