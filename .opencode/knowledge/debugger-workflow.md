# JetBrains Debugger MCP Workflow

Use this only when debugger MCP tools are available for the current OpenCode
session.

## When to prefer debugger-first investigation

- Runtime state is unclear from source alone.
- The user asks to debug, inspect variables, trace execution, or explain an
  unexpected runtime value.
- A failing test or action needs confirmation of actual control flow.

## When not to use it

- The issue is an obvious static mistake that can be fixed confidently from the
  code.
- No usable run configuration exists for the target project.

## Default workflow

1. List run configurations and choose one that can debug.
2. Set breakpoint(s) before starting the session.
3. Start the debug session.
4. Poll session status until execution pauses.
5. Use the combined session-status inspection as the primary source of stack,
   variables, and source context.
6. Evaluate expressions or inspect variables to confirm hypotheses.
7. Step or resume as needed, polling status after each execution-control call.
8. Stop the session when done.

## Working rules

- Prefer the single combined status call over separate stack/variable/source
  calls unless a narrower call is needed.
- Breakpoints and run-to-line targets use 1-based line numbers.
- File paths for debugger navigation and breakpoints must be absolute.
- After step or resume actions, assume the program is running until a later
  status poll says it is paused.
- In single-session cases, omit session id unless disambiguation is needed.
- If multiple projects are open, supply the project path.

## Native debugger caveat

- For Rust, C, C++, Go, and Swift, prefer variable inspection over method-call
  expression evaluation because LLDB/GDB expression support is limited.

## Practical patterns

- Wrong value: break where the bad value is used, inspect inputs, then move
  earlier in the call chain if needed.
- Specific iteration: use a conditional breakpoint.
- Trace without pausing: use a log breakpoint / tracepoint.
- Caller context: switch stack frame before inspecting variables.
- Test a fix hypothesis: evaluate the corrected expression before editing code.

## Launching Gradle client runs on Windows

- For Minecraft client runs that must stay open for the user to test, do not run
  `./gradlew :workspace:runClient` directly in a blocking tool call.
- On Windows, prefer launching a visible persistent `cmd` window via
  PowerShell `Start-Process` and `gradlew.bat`.
- Use this pattern:

```powershell
powershell -NoProfile -Command "Start-Process cmd.exe -ArgumentList '/k','cd /d D:\Development\MultiWorkspace26.1 && gradlew.bat :workspace:runClient' -WorkingDirectory 'D:\Development\MultiWorkspace26.1' -WindowStyle Normal"
```

- Use `gradlew.bat`, not `./gradlew`, when launching inside `cmd.exe` on
  Windows.
- Return to the user immediately after launch so the session is not blocked
  while the game stays open.

## Cleanup

- Remove temporary breakpoints if they are no longer useful.
- Stop the debug session after collecting the needed evidence.
