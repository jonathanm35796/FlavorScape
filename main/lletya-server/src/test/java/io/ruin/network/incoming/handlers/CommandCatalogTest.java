package io.ruin.network.incoming.handlers;

import io.ruin.tools.GenerateCommandCatalog;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
import static io.ruin.network.incoming.handlers.CommandCatalog.*;

public class CommandCatalogTest {
    private Rights rights(boolean admin, boolean mod, boolean support, boolean owner, boolean manager, boolean dev, boolean rigging) {
        return new Rights(admin, mod, support, owner, manager, dev, rigging, admin);
    }
    private Set<String> names(Rights rights) {
        Set<String> result = new HashSet<>();
        for (Visible command : visible(rights, false, "")) assertTrue(result.add(command.name()));
        return result;
    }
    private Entry entry(Rights rights, String name) {
        return visible(rights, false, "").stream().filter(v -> v.name().equals(name)).findFirst().orElseThrow().entry();
    }

    @Test public void regularPlayersCannotDiscoverStaffCommandsOrOutcomeOverrides() {
        Rights player = rights(false, false, false, false, false, false, true);
        Set<String> names = names(player);
        assertTrue(names.containsAll(Set.of("commands", "home", "players", "empty", "level", "levels", "combatlevels")));
        assertTrue(Collections.disjoint(names, Set.of("item", "save", "ban", "kick", "staffcommands", "thepassword", "dice", "flower", "heal", "maxlvls")));
        assertEquals(Section.REGULAR, entry(player, "master").section());
        assertTrue(visible(player, true, "").isEmpty());
    }

    @Test public void supportAndModeratorRightsAreInheritedAccurately() {
        Set<String> support = names(rights(false, false, true, false, false, false, false));
        assertTrue(support.containsAll(Set.of("kick", "jail", "mute", "staffcommands")));
        assertFalse(support.contains("ban"));
        assertFalse(support.contains("maxlvls"));
        Set<String> moderator = names(rights(false, true, false, false, false, false, false));
        assertTrue(moderator.containsAll(Set.of("kick", "ban", "unlock", "teleto")));
        assertFalse(moderator.contains("save"));
        assertFalse(moderator.contains("maxlvls"));
    }

    @Test public void administrativeRolesAndManagerRestrictionsMatchDispatch() {
        Rights admin = rights(true, false, false, false, false, false, false);
        Set<String> names = names(admin);
        assertTrue(names.containsAll(Set.of("save", "item", "pickup", "lvl", "maxlvls", "update", "hp", "ban", "kick")));
        assertFalse(names.contains("thepassword"));
        assertEquals(Section.ADMINISTRATOR, entry(admin, "master").section());
        assertEquals(Section.ADMINISTRATOR, entry(admin, "maxlvls").section());
        assertEquals(Section.ADMINISTRATOR, entry(admin, "empty").section());
        assertTrue(names.size() > 200);
        Rights manager = rights(true, false, false, false, true, false, false);
        Set<String> managerNames = names(manager);
        assertFalse(managerNames.contains("update"));
        assertFalse(managerNames.contains("lvl"));
        assertFalse(managerNames.contains("giveupgrades"));
        assertTrue(managerNames.contains("save"));
        assertTrue(managerNames.contains("maxlvls"));
        assertEquals(Section.REGULAR, entry(manager, "master").section());
    }

    @Test public void ownerOverridesRequireBothOwnerMembershipAndEnabledMode() {
        Rights owner = rights(true, false, false, true, false, false, false);
        assertTrue(names(owner).contains("thepassword"));
        assertTrue(visible(owner, true, "").stream().anyMatch(v -> v.name().equals("thepassword")));
        assertFalse(names(owner).contains("dice"));
        Rights enabled = rights(true, false, false, true, false, false, true);
        assertTrue(names(enabled).containsAll(Set.of("dice", "flower")));
        Entry dice = entry(enabled, "dice");
        assertFalse(allowed(rights(false, false, false, false, false, false, true), dice, "dice"));
        Entry futureAlias = new Entry(Section.REGULAR, List.of("thepassword", "overridealias"), false, "");
        assertFalse(allowed(rights(false, false, false, false, false, false, true), futureAlias, "overridealias"));
        assertTrue(allowed(owner, futureAlias, "overridealias"));
    }

    @Test public void devExceptionsApplyToIndividualAliasesRatherThanWholeGroups() {
        Rights dev = rights(false, false, false, false, false, true, false);
        Set<String> names = names(dev);
        assertTrue(names.containsAll(Set.of("item", "pickup", "tele", "inter", "lvl")));
        assertFalse(names.contains("teleport"));
        assertFalse(names.contains("interface"));
        assertFalse(names.contains("save"));
        assertFalse(names.contains("ban"));
        assertFalse(names.contains("thepassword"));
        assertFalse(names.contains("maxlvls"));
    }

    @Test public void donorHealRestrictionAndPaginationDoNotLeakOtherCommands() {
        Rights donor = new Rights(false, false, false, false, false, false, false, true);
        assertTrue(names(donor).contains("heal"));
        List<Visible> all = visible(rights(true, false, false, true, false, false, true), false, "");
        Set<String> paged = new HashSet<>();
        for (int n = 1; n <= page(all, 1).totalPages(); n++) {
            Page page = page(all, n);
            assertTrue(page.commands().size() <= 25);
            for (Visible command : page.commands()) assertTrue(paged.add(command.name()));
        }
        assertEquals(all.size(), paged.size());
        List<Visible> bankSearch = visible(donor, false, "BAN");
        assertTrue(bankSearch.stream().anyMatch(v -> v.name().equals("bankitems")));
        assertFalse(bankSearch.stream().anyMatch(v -> v.name().equals("ban")));
        assertFalse(allowed(donor, entry(donor, "home"), "save"));
        assertTrue(page(Collections.emptyList(), 1).commands().isEmpty());
        for (int number : new int[]{0, -1, Integer.MAX_VALUE}) {
            try { page(all, number); fail("Accepted invalid page"); }
            catch (IllegalArgumentException expected) { }
        }
        Set<String> names = names(donor);
        assertFalse(names.contains("referral")); // Commented-out command.
        assertFalse(names.contains("corrupted")); // Nested event subcommand, not a chat command.
        assertFalse(names.contains("barrage"));
    }

    @Test public void syntaxIndexIgnoresCommentsAndNestedSwitchesAndKeepsAliases() throws Exception {
        Path directory = Files.createTempDirectory("lletya-command-index-");
        Path source = directory.resolve("Fixture.java");
        Path output = directory.resolve("catalog.tsv");
        String fixture = "class Fixture {\n" +
            "void handleRegular(String command) { switch(command) { // case \"fake\":\n" +
            "case \"foo\": case \"bar\": { switch(command) { case \"nested\": break; } break; } } }\n" +
            "void handleSupport(String command) { switch(command) { case \"help\": break; } }\n" +
            "void handleMod(String command) { switch(command) { case \"ban\": break; } }\n" +
            "void handleSeniorMod(String command) { switch(command) { case \"hp\": break; } }\n" +
            "void handleAdmin(String command) { switch(command) { case \"item\": case \"pickup\": { if(isCommunityManager) return; use(\"::item id\"); } } } }";
        try {
            Files.writeString(source, fixture, StandardCharsets.UTF_8);
            GenerateCommandCatalog.generate(source, output);
            List<String> rows = Files.readAllLines(output);
            assertEquals(5, rows.size());
            assertTrue(rows.contains("REGULAR\tfoo,bar\tfalse\t"));
            assertTrue(rows.contains("ADMINISTRATOR\titem,pickup\ttrue\t::item id"));
            assertFalse(String.join("\n", rows).contains("nested"));
            assertFalse(String.join("\n", rows).contains("fake"));
        } finally { Files.deleteIfExists(output); Files.deleteIfExists(source); Files.deleteIfExists(directory); }
    }

    @Test public void helpArgumentsHandleSearchWhitespaceAndOversizedPageNumbers() {
        assertEquals(new Query(1, ""), query(null));
        assertEquals(new Query(1, "tele"), query(new String[]{"tele"}));
        assertEquals(new Query(2, "tele"), query(new String[]{"", "2", " ", "tele"}));
        assertEquals(new Query(1, "50"), query(new String[]{"1", "50"}));
        try { query(new String[]{"999999999999999999999999"}); fail("Accepted overflowing page"); }
        catch (IllegalArgumentException expected) { }
    }
}
