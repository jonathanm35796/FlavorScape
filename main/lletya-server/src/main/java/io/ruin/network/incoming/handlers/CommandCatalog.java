package io.ruin.network.incoming.handlers;

import io.ruin.model.World;
import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.player.PlayerGroup;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** A build-generated index; listing and dispatch consult the same access policy. */
public final class CommandCatalog {
    public enum Section {
        ADMINISTRATOR("Admin+"), SUPPORT("Support+"), MODERATOR("Moderator+"),
        SENIOR_MODERATOR("Admin+"), REGULAR("Player");
        final String label;
        Section(String label) { this.label = label; }
    }
    public record Rights(boolean administrator, boolean moderator, boolean support, boolean owner,
                         boolean communityManager, boolean development, boolean rigging, boolean healAccess) {
        static Rights from(Player p) {
            return new Rights(p.isAdmin(), p.isModerator(), p.isSupport(), p.isGroup(PlayerGroup.OWNER),
                p.getPrimaryGroup() == PlayerGroup.COMMUNITY_MANAGER, World.isDev(), p.rigging,
                p.isAdmin() || (p.isSapphire() && !p.isGroup(PlayerGroup.SAPPHIRE)));
        }
    }
    public record Entry(Section section, List<String> aliases, boolean restrictedManager, String example) {
        String usage(String name) {
            if (example.isEmpty()) return "::" + name;
            int space = example.indexOf(' ');
            return "::" + name + (space < 0 ? "" : example.substring(space));
        }
    }
    public record Visible(String name, Entry entry) { }
    public record Page(List<Visible> commands, int number, int totalPages, int totalCommands) { }
    public record Query(int page, String search) { }

    private static final int PAGE_SIZE = 25;
    private static final Set<String> OWNER_COMMANDS = Set.of("thepassword", "dice", "flower");
    // Preserve the existing DEV exception per command spelling, rather than granting all aliases.
    private static final Set<String> DEV_COMMANDS = Set.of("item", "pickup", "empty", "fi", "fitem",
        "master", "lvl", "copyinv", "copyarm", "inter", "heal", "debug", "tele", "Bank");
    private static final List<Entry> ENTRIES = load();
    private static final Map<Section, Map<String, Entry>> INDEX = index();

    private static Map<Section, Map<String, Entry>> index() {
        Map<Section, Map<String, Entry>> index = new EnumMap<>(Section.class);
        for (Section section : Section.values()) index.put(section, new HashMap<>());
        for (Entry entry : ENTRIES) for (String alias : entry.aliases) index.get(entry.section).put(alias, entry);
        return index;
    }

    private static List<Entry> load() {
        InputStream stream = CommandCatalog.class.getResourceAsStream("/command-catalog.tsv");
        if (stream == null) throw new IllegalStateException("Missing generated command catalogue; rebuild the server");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<Entry> entries = new ArrayList<>();
            for (String line; (line = reader.readLine()) != null;) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 4) throw new IllegalStateException("Invalid command catalogue row");
                entries.add(new Entry(Section.valueOf(fields[0]), List.of(fields[1].split(",")),
                    Boolean.parseBoolean(fields[2]), fields[3]));
            }
            entries.sort(Comparator.comparing(Entry::section)); // Same order as actual dispatch.
            return List.copyOf(entries);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static boolean allowed(Rights rights, Entry entry, String name) {
        if (!entry.aliases.contains(name)) return false;
        boolean sectionAccess = switch (entry.section) {
            case ADMINISTRATOR -> rights.administrator || (rights.development && DEV_COMMANDS.contains(name));
            case SUPPORT -> rights.support || rights.moderator || rights.administrator;
            case MODERATOR -> rights.moderator || rights.administrator;
            case SENIOR_MODERATOR -> rights.administrator;
            case REGULAR -> true;
        };
        if (!sectionAccess || (entry.restrictedManager && rights.communityManager)) return false;
        if (entry.aliases.contains("thepassword")) return rights.owner;
        if (entry.aliases.contains("dice") || entry.aliases.contains("flower")) return rights.owner && rights.rigging;
        return !entry.aliases.contains("heal") || rights.healAccess;
    }

    public static boolean canUse(Player player, Section section, String command) {
        Entry entry = INDEX.get(section).get(command);
        return entry != null && allowed(Rights.from(player), entry, command);
    }

    public static List<Visible> visible(Rights rights, boolean staffOnly, String search) {
        Map<String, Visible> commands = new TreeMap<>();
        for (Entry entry : ENTRIES) for (String name : entry.aliases) {
            if (allowed(rights, entry, name)) commands.putIfAbsent(name, new Visible(name, entry));
        }
        String filter = search.toLowerCase(Locale.ROOT);
        return commands.values().stream()
            .filter(v -> !staffOnly || v.entry.section != Section.REGULAR || v.entry.aliases.stream().anyMatch(OWNER_COMMANDS::contains))
            .filter(v -> v.name.contains(filter)).collect(Collectors.toList());
    }

    public static Page page(List<Visible> commands, int number) {
        int total = Math.max(1, (commands.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (number < 1 || number > total) throw new IllegalArgumentException("Choose a page from 1 to " + total + ".");
        int start = (number - 1) * PAGE_SIZE;
        return new Page(List.copyOf(commands.subList(start, Math.min(commands.size(), start + PAGE_SIZE))), number, total, commands.size());
    }

    public static Query query(String[] rawArgs) {
        String[] args = rawArgs == null ? new String[0] : Arrays.stream(rawArgs)
            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        int pageNumber = 1;
        int searchStart = 0;
        if (args.length > 0 && args[0].matches("[-+]?\\d+")) {
            try { pageNumber = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid command page number."); }
            searchStart = 1;
        }
        return new Query(pageNumber, String.join(" ", Arrays.copyOfRange(args, searchStart, args.length)));
    }

    public static void show(Player player, String[] args, boolean staffOnly) {
        Rights rights = Rights.from(player);
        Page page;
        try {
            Query query = query(args);
            page = page(visible(rights, staffOnly, query.search), query.page);
        }
        catch (IllegalArgumentException e) { player.sendMessage(e.getMessage()); return; }
        String menu = staffOnly ? "staffcommands" : "commands";
        List<String> lines = new ArrayList<>();
        lines.add("<col=800000>" + page.totalCommands + " accessible commands - page " + page.number + "/" + page.totalPages + "</col>");
        lines.add("::" + menu + " [page] [search] | Example: ::" + menu + " 1 tele");
        lines.add("Rights checked server-side; combat, location and target checks still apply.");
        lines.add("Names without a usage example may require arguments or open a prompt.");
        if (rights.development) lines.add("DEV mode: development rights and exceptions are active.");
        if (page.commands.isEmpty()) lines.add("No accessible commands match your search.");
        for (Visible command : page.commands) {
            String label = command.entry.aliases.stream().anyMatch(OWNER_COMMANDS::contains) ? "Owner" : command.entry.section.label;
            if (command.entry.aliases.contains("heal")) label = "Donator/Admin+";
            if (command.entry.section == Section.ADMINISTRATOR && !rights.administrator) label = "DEV";
            String note = command.entry.aliases.contains("dz")
                ? " - zone not implemented" : "";
            lines.add(command.entry.usage(command.name).replace('<', '[').replace('>', ']') + " (" + label + ")" + note);
        }
        player.sendScroll("Lletya Commands", lines.toArray(new String[0]));
    }
}
