# Minecraft Client Runs

Use this when a task requires launching a Minecraft client for manual in-game
testing or visual verification.

## Windows runClient workflow

- Do not run `./gradlew :workspace:runClient` in a blocking tool call when the
  user needs to keep the client open and continue chatting.
- On Windows, launch the client in a visible persistent `cmd` window via
  PowerShell `Start-Process`.
- Use `gradlew.bat`, not `./gradlew`, when launching inside `cmd.exe`.

Recommended pattern:

```powershell
powershell -NoProfile -Command "Start-Process cmd.exe -ArgumentList '/k','cd /d D:\Development\MultiWorkspace26.1 && gradlew.bat :workspace:runClient' -WorkingDirectory 'D:\Development\MultiWorkspace26.1' -WindowStyle Normal"
```

## Working rules

- Return to the user immediately after launch so the session remains usable
  while the game is open.
- Use this after fixes that require a full client reload, especially rendering,
  model, resource, or in-game interaction changes.
- If the project path differs, substitute the correct workspace path in both the
  `cd /d` portion and the `-WorkingDirectory` argument.
