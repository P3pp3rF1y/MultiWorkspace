---
description: Review one or more sessions for command, knowledge, workflow, and agent improvements
agent: general
subtask: true
---

Review the provided session material and suggest concrete improvements to the agentic setup.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/exploration-scope.md`

User context:
$ARGUMENTS

Source selection:

- Use attached exported session JSON, transcript, markdown notes, or pasted conversation content when provided.
- If both attached files and current conversation context exist, attached files are the primary source of truth.
- If multiple session files are attached, treat this as a cross-session review and prioritize repeated patterns.

Review goals:

- Detect repeated user corrections, steering, or reframing.
- Detect places where implementation or reasoning drifted because the active agent lacked domain knowledge such as Minecraft, NeoForge, mod architecture, coding patterns, repo conventions, or workflow knowledge.
- Detect repeated facts or instructions the user keeps re-stating across sessions.
- Detect cases where a reusable command, workflow, routing rule, knowledge entry, or specialist agent would likely reduce future friction.
- Detect cases where an existing command, workflow, or agent likely needs refinement rather than a brand new addition.

Evidence to inspect:

- Repeated prompts or repeated clarifications from the user.
- Agent switches or explicit user requests to use a different agent.
- Repeated implementation retries, corrections, or scope narrowing.
- Repeated missing context about Minecraft, mods, code conventions, build flow, review expectations, or tool usage.
- Repeated manual sequences that could become a command.
- Repeated reasoning failures that suggest missing knowledge or weak routing.

Decision rules:

- Prefer updating existing commands, knowledge, workflows, or agents before suggesting brand new ones.
- Prefer a knowledge entry when the gap is a reusable fact, rule, pattern, checklist, or repo-specific convention.
- Prefer a command when the gap is a repeated multi-step action or repeatable review flow.
- Prefer a workflow change when the gap is sequencing, gating, handoff, or escalation between tasks or agents.
- Prefer an agent update or new specialist agent only when repeated failures require distinct domain judgment that is not well handled by general instructions alone.
- Raise priority when the same issue appears across multiple sessions or causes implementation churn.
- Stay conservative: do not invent patterns that are not supported by the provided evidence.

Special attention:

- If the issue appears to be missing Minecraft, NeoForge, modding, or coding knowledge, say exactly what knowledge seems missing and where it should live.
- If the issue appears to be bad agent routing, specify which agent should have been used earlier and what routing trigger would have caught it.
- If the issue appears to be repeated user guidance, separate "knowledge to encode" from "workflow to enforce".

Output format:

```text
review_scope: single-session|multi-session
source_summary: <what sources were reviewed>
overall_assessment: <2-4 sentences on the biggest friction patterns>

high_confidence_findings:
  - <finding with evidence>

recommended_improvements:
  commands:
    - priority: HIGH|MEDIUM|LOW
      recommendation: <new command or update existing command>
      why: <one sentence>
      evidence: <specific repeated behavior or correction>
  knowledge:
    - priority: HIGH|MEDIUM|LOW
      recommendation: <new knowledge entry or update existing knowledge>
      why: <one sentence>
      evidence: <specific repeated behavior or correction>
  workflows:
    - priority: HIGH|MEDIUM|LOW
      recommendation: <workflow change>
      why: <one sentence>
      evidence: <specific repeated behavior or correction>
  agents:
    - priority: HIGH|MEDIUM|LOW
      recommendation: <new specialist agent or update existing agent>
      why: <one sentence>
      evidence: <specific repeated behavior or correction>
  routing_rules:
    - priority: HIGH|MEDIUM|LOW
      recommendation: <AGENTS.md or command-discovery/routing improvement>
      why: <one sentence>
      evidence: <specific repeated behavior or correction>

user_repeated_guidance_to_encode:
  - <short instruction the user keeps repeating and where to encode it>

missing_domain_knowledge:
  - domain: <minecraft|neoforge|mod-architecture|coding-patterns|repo-conventions|other>
    gap: <what seems missing>
    recommended_home: <knowledge file, agent, command, or workflow>
    evidence: <specific repeated behavior or correction>

top_next_actions:
  - <highest-value improvement to implement next>
  - <second improvement>
  - <third improvement>
```

Guardrails:

- Keep recommendations concrete and implementation-ready.
- Tie every recommendation to evidence from the reviewed material.
- Prefer a short "update existing X" recommendation over creating unnecessary new abstractions.
- If evidence is weak for a category, return an empty list for that category.
