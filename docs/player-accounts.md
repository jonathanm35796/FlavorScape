# Player account files and rights

Kronos's local player saves are readable JSON files, one per character, at:

`main/flavorscape-server/server-data/_saved/characters/<lowercase name>.json`

For example, `Sales Tax` saves as `sales tax.json`. The file contains the password hash, saved groups, stats, inventory, equipment, bank, configuration, and other character state. It is local runtime data and is excluded from GitHub with main/.

PlayerFile.java loads the character's JSON at login. Player.java restores nonempty savedGroupIds from that file. PlayerFile.save serializes fields annotated with Gson @Expose through a single-thread save worker. Saving happens during logout, approximately every minute through World's 100-tick save event, and through the administrator ::save command.

The file-login service now assigns each account a stable positive ID through `LocalAccountIds`. The registry is `.account-ids.json` beside the character files; include it when backing up or moving the character directory. IDs persist across logins/restarts and do not depend on name hashes. Registry updates use a temporary file and atomic replacement where supported. Invalid/duplicate registry IDs fail the login instead of silently reassigning identities. This fixes the former shared `-1` ID that blocked Bounty Hunter assignment.

Relevant PlayerGroup IDs:

| Group | Saved ID |
| --- | --- |
| OWNER | 100 |
| DEVELOPER | 3 |
| COMMUNITY_MANAGER | 8 |
| ADMINISTRATOR | 5 |
| MODERATOR | 4 |
| SUPPORT | 9 |
| REGISTERED | 2 |

The numeric IDs are identifiers, not a numerical permission ladder. Characters can belong to multiple groups. isAdmin() accepts OWNER, DEVELOPER, COMMUNITY_MANAGER, or ADMINISTRATOR. The first saved group determines the primary/display group; it need not be the character's only permission group.

Current DEV behavior: a new login is initialized as ADMINISTRATOR, and Player's login setup also joins OWNER. Consequently, [5,100] already includes the highest supported owner rights while retaining the administrator display group. This automatic OWNER grant was verified in Player.java, beyond the upstream README's administrator claim. Password checks are skipped in DEV.

For persistent account edits, log the character out first, back up its JSON, then change only the intended field. Editing an online character's file can be overwritten by its next save. Do not replace an existing character with a minimal JSON file to assign rights; that would lose its other saved state. When public access is introduced, the DEV grants and password bypass require a separate deliberate configuration review.
