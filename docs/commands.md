# Command catalogue

`::commands` now lists the active server chat commands the current character is authorized to use. The list is alphabetized, includes aliases, removes duplicate names according to dispatch priority, and is paginated at 25 commands per page.

```text
::commands                 First page
::commands 2               Second page
::commands tele            Search accessible names containing tele
::commands 1 tele          First page of that search
::commands 1 50            Search for the numeric command name 50
::staffcommands            Accessible staff/Owner commands
```

Exact usage examples are included where existing command handlers provide them. Other entries show their actual command names; arguments may still be required or the command may open a prompt. The unimplemented donator-zone commands are explicitly labeled. Combat, location, target, cooldown, and temporary gameplay checks remain in their existing handlers.

## Permission enforcement

CommandCatalog.java supplies a common permission policy used by both the catalogue and all five execution handlers. The policy honors support/moderator/administrator inheritance, Owner membership, the exact per-command DEV exceptions, the existing primary Community Manager exclusions, and the existing heal entitlement check. Overlapping names such as master, empty, and lms use the same first-accessible-handler priority as execution.

The legacy thepassword, dice, and flower outcome-override commands now require Owner membership. Dice and flower are only listed/executable when their existing override mode is also enabled. Knowing the old override password no longer grants access to an ordinary player.

DEV mode currently grants Owner membership to every logged-in character, so those characters legitimately see an elevated command list. This change does not remove the existing DEV grants or password bypass. Production-style ordinary-player restrictions are covered separately by the permission tests.

## Keeping the list accurate

GenerateCommandCatalog.java uses the JDK Java parser at build time to index active top-level switch cases from CommandHandler.java. It indexes 330 section/name entries across 281 alias groups in the current source; overlapping names between sections are deduplicated for display. Comments and nested event/XP-mode subcommands are not mistaken for chat commands. The generated command-catalog.tsv is packaged as a server resource, so the running server does not read or parse Java source.

Adding/removing a case or alias in an existing handler updates the list on the next build. New permission categories or special account restrictions must also be added to the shared CommandCatalog policy and its tests. Rebuild/restart to activate source changes.

Server-local source files:

- main/flavorscape-server/src/main/java/io/ruin/network/incoming/handlers/CommandCatalog.java
- main/flavorscape-server/src/main/java/io/ruin/network/incoming/handlers/CommandHandler.java
- main/flavorscape-server/src/commandCatalog/java/io/ruin/tools/GenerateCommandCatalog.java
- main/flavorscape-server/src/test/java/io/ruin/network/incoming/handlers/CommandCatalogTest.java

## Verification

Verified 2026-10-25: all eight targeted tests passed with no failures. After backing up a fresh autosave and restarting locally, the actual client logged in, opened the full catalogue, searched for save, and opened the staff subset. The probe verified the resulting in-game widgets and recorded COMMANDS_SMOKE PASS in .tools/main-Client.log. Servers and the client were left running with the full catalogue open.

Run the targeted tests with JDK 21 and the existing Gradle wrapper: `:flavorscape-server:test --tests io.ruin.network.incoming.handlers.CommandCatalogTest`. Eight tests cover access boundaries, manager exclusions, Owner override restrictions, DEV alias exceptions, donor heal entitlement, search/pagination, malformed page inputs, and source parsing.

Client-only console and optional plugin commands are separate from this server-command catalogue and can depend on enabled plugins. They do not grant server privileges.

For a real-client menu check, set the local environment variable FLAVORSCAPE_VERIFY_COMMANDS=1 before starting scripts/run-local.ps1 -VerifyLogin. The optional probe submits the read-only commands menu, search, and staff-list requests through the actual game connection and verifies the resulting widgets. It does not execute the commands displayed in the list.
