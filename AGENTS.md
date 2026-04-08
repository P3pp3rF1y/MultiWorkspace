<INSTRUCTIONS>
This workspace may inherit reusable commands, agent definitions, knowledge indexes, and tooling rules through the AGENTS.md include chain.

Always resolve the AGENTS.md include chain first so you are aware of the available discovery entrypoints and lookup rules.

Resolving the chain is for awareness and routing only:
- identify where commands, knowledge, and agent definitions are defined
- identify which index files are the entrypoints for lookup
- do not eagerly load all command, knowledge, or agent files
- load only the specific index or payload files needed for the current prompt

Before answering repo-specific capability, command, workflow, or knowledge questions, you must resolve the AGENTS.md include chain first.

See shared instructions at:
..\AGENTS.md
</INSTRUCTIONS>
