package net.runelite.client.plugins.flavorscapegears;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.ui.FontManager;
import net.runelite.standalone.FlavorScapeCommands;

@PluginDescriptor(name = "FlavorScape Gears", description = "Quest-tab gear previews, saved setups and quick loading", enabledByDefault = true)
public class FlavorScapeGearsPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private SpriteManager sprites;
    @Inject private OverlayManager overlays;
    @Inject private MouseManager mouseManager;
    @Inject private KeyManager keyManager;
    @Inject private TooltipManager tooltips;
    private final GearOverlay overlay = new GearOverlay();
    private final Input input = new Input();
    private volatile boolean open;
    private volatile boolean awaitingPresetLoad;
    private volatile int scroll;
    private volatile int selected;
    private volatile Rectangle panel = new Rectangle(), toggle = new Rectangle();
    private volatile Rectangle questFrame = new Rectangle();
    private volatile List<Row> rows = Collections.emptyList();
    private static final int ROW_HEIGHT = 116;
    private static final int ESCAPE_CLOSES_INTERFACES = 4681;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13};
    private static final String[] LABELS = {"Head", "Cape", "Neck", "Weapon", "Body", "Shield", "Legs", "Gloves", "Boots", "Ring", "Ammo"};

    @Override protected void startUp() {
        overlays.add(overlay);
        mouseManager.registerMouseListener(0, input);
        mouseManager.registerMouseWheelListener(0, input);
        keyManager.registerKeyListener(input);
    }
    @Override protected void shutDown() {
        overlays.remove(overlay);
        mouseManager.unregisterMouseListener(input);
        mouseManager.unregisterMouseWheelListener(input);
        keyManager.unregisterKeyListener(input);
        closePanel();
        panel = toggle = new Rectangle();
    }
    void command(String command) { clientThread.invokeLater(() -> FlavorScapeCommands.send(command)); }
    void closePanel() {
        open = false;
        awaitingPresetLoad = false;
        FlavorScapeCommands.takePresetLoaded();
    }
    private void togglePanel() {
        if (client.getVar(VarClientInt.INTERFACE_TAB) != 2) return;
        if (open) { closePanel(); return; }
        FlavorScapeCommands.takePresetLoaded();
        awaitingPresetLoad = false;
        open = true;
        command("gears");
    }
    private void loadPreset(String command) {
        awaitingPresetLoad = true;
        command(command);
    }
    void handlePresetLoadNotification() {
        if (FlavorScapeCommands.takePresetLoaded()) {
            if (awaitingPresetLoad) open = false;
            awaitingPresetLoad = false;
        }
    }
    private static final class Row {
        final int slot;
        final String[] data;
        Row(int slot, String text) { this.slot = slot; data = text.split("\\|", -1); }
    }
    private class GearOverlay extends Overlay {
        GearOverlay() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_WIDGETS); }
        @Override public Dimension render(Graphics2D g) {
            panel = toggle = new Rectangle();
            questFrame = new Rectangle();
            handlePresetLoadNotification();
            if (client.getGameState() != GameState.LOGGED_IN) {
                closePanel();
                rows = Collections.emptyList();
                FlavorScapeCommands.clear();
                return null;
            }
            if (client.getVar(VarClientInt.INTERFACE_TAB) != 2) {
                return null;
            }
            Widget summary = client.getWidget(720, 0);
            if (summary == null || summary.isHidden() || summary.getBounds().width <= 0) return null;
            Rectangle root = summary.getBounds();
            questFrame = new Rectangle(root);
            drawSummary(g, root);
            Widget viewportWidget = client.getViewportWidget();
            Rectangle viewport = viewportWidget == null ? new Rectangle(0, 0, root.x, client.getCanvasHeight() - 170) : viewportWidget.getBounds();
            int height = Math.min(360, viewport.height - 12);
            int x = Math.max(viewport.x + 6, Math.min(root.x - 206, viewport.x + viewport.width - 204));
            int y = Math.max(viewport.y + 6, viewport.y + (viewport.height - height) / 2);
            g.setFont(new Font("Dialog", Font.PLAIN, 11));
            // The dragon med helm is the Quest summary's gear button.
            toggle = open ? new Rectangle(x + 174, y + 3, 20, 20)
                    : new Rectangle(root.x + 4, root.y + root.height - 34, 41, 30);
            if (toggle.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
                tooltips.add(new Tooltip("Presets"));
            if (!open) return null;
            if (height < 150) return null;
            panel = new Rectangle(x, y, 198, height);
            g.setColor(new Color(51, 43, 32)); g.fill(panel);
            g.setColor(new Color(136, 113, 68)); g.draw(panel);
            List<Row> updated = new ArrayList<>();
            String[] previews = FlavorScapeCommands.previews();
            for (int i = 0; i < previews.length; i++) {
                if (previews[i] != null && previews[i].split("\\|", -1).length == 4)
                    updated.add(new Row(i, previews[i]));
            }
            rows = Collections.unmodifiableList(updated);
            g.setColor(new Color(255, 207, 100));
            long count = rows.stream().filter(r -> !r.data[0].equals("Empty slot")).count();
            g.drawString("Gear Presets " + count + "/" + rows.size(), x + 7, y + 16);
            boolean hovered = toggle.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY());
            java.awt.image.BufferedImage closeSprite = sprites.getSprite(hovered
                    ? SpriteID.WINDOW_CLOSE_BUTTON_HOVERED : SpriteID.WINDOW_CLOSE_BUTTON, 0);
            if (closeSprite != null) g.drawImage(closeSprite, toggle.x, toggle.y, toggle.width, toggle.height, null);
            else box(g, toggle, "X", false);
            int visible = height - 49;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() * ROW_HEIGHT - visible)));
            Shape oldClip = g.getClip();
            g.clipRect(x + 2, y + 24, 190, visible);
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                int ry = y + 24 + i * ROW_HEIGHT - scroll;
                if (ry + ROW_HEIGHT < y + 24 || ry > y + height - 25) continue;
                g.setColor(row.slot == selected ? new Color(84, 70, 47) : new Color(42, 36, 29));
                g.fillRect(x + 4, ry, 184, ROW_HEIGHT - 3);
                g.setColor(new Color(255, 207, 100));
                g.drawString("F" + (row.slot + 1) + ": " + row.data[0], x + 8, ry + 13);
                g.setColor(Color.LIGHT_GRAY);
                g.drawString(row.data[3], x + 8, ry + 27);
                String[] gear = row.data[1].split(",");
                for (int j = 0; j < SLOTS.length; j++) {
                    int ix = x + 8 + (j % 6) * 29, iy = ry + 32 + (j / 6) * 24;
                    g.setColor(new Color(110, 94, 66)); g.drawRect(ix, iy, 25, 22);
                    int id;
                    try { id = Integer.parseInt(gear[SLOTS[j]]); } catch (RuntimeException e) { continue; }
                    if (id >= 0) g.drawImage(items.getImage(id), ix + 1, iy + 1, 24, 20, null);
                    int mx = client.getMouseCanvasPosition().getX(), my = client.getMouseCanvasPosition().getY();
                    if (new Rectangle(ix, iy, 26, 23).contains(mx, my))
                        tooltips.add(new Tooltip(LABELS[j] + (id < 0 ? ": empty" : ": " + client.getItemDefinition(id).getName())));
                }
                box(g, new Rectangle(x + 8, ry + 81, 86, 23), "Save current", false);
                box(g, new Rectangle(x + 99, ry + 81, 40, 23), "Select", row.slot == selected);
                box(g, new Rectangle(x + 143, ry + 81, 40, 23), "Load", false);
            }
            g.setClip(oldClip);
            if (rows.size() * ROW_HEIGHT > visible) {
                int thumb = Math.max(12, visible * visible / (rows.size() * ROW_HEIGHT));
                int thumbY = y + 24 + scroll * (visible - thumb) / Math.max(1, rows.size() * ROW_HEIGHT - visible);
                g.setColor(new Color(165, 137, 83)); g.fillRect(x + 191, thumbY, 4, thumb);
            }
            box(g, new Rectangle(x + 5, y + height - 22, 184, 19), "Manage selected preset", false);
            return null;
        }
        private void drawSummary(Graphics2D g, Rectangle r) {
            int x = r.x, y = r.y, w = r.width;
            g.setColor(new Color(48, 43, 34)); g.fill(r);
            g.setFont(FontManager.getRunescapeSmallFont());
            card(g, x + 3, y + 3, w - 6, 36);
            centered(g, "Summary", x + w / 2, y + 29, new Color(255, 180, 45));
            g.setColor(new Color(168, 151, 100));
            star(g, x + w / 4, y + 12); g.setColor(new Color(194, 66, 66)); star(g, x + 3 * w / 4, y + 12);
            String[] data = FlavorScapeCommands.summary();
            card(g, x + 3, y + 41, w - 6, 66);
            centered(g, "Statistics", x + w / 2, y + 54, new Color(238, 178, 77));
            label(g, "Kills: " + data[0], x + 10, y + 70);
            label(g, "Deaths: " + data[1], x + 10, y + 84);
            label(g, "Risk: View", x + 106, y + 70);
            double kills = 0, deaths = 0;
            try { kills = Double.parseDouble(data[0]); deaths = Double.parseDouble(data[1]); } catch (NumberFormatException ignored) { }
            label(g, "K/D: " + String.format(java.util.Locale.ROOT, "%.2f", kills / Math.max(1, deaths)), x + 106, y + 84);
            label(g, "Votes: " + data[6], x + 10, y + 99);
            card(g, x + 3, y + 110, (w - 8) / 2, 55);
            card(g, x + w / 2 + 1, y + 110, (w - 8) / 2, 55);
            centered(g, "Slayer Task", x + w / 4, y + 124, new Color(238, 178, 77));
            centered(g, data[2].equals("0") ? "No task" : data[2] + " remaining", x + w / 4, y + 141, Color.YELLOW);
            centered(g, fit(g, data[3], 80), x + w / 4, y + 155, Color.YELLOW);
            centered(g, "Collections", x + 3 * w / 4, y + 124, new Color(238, 178, 77));
            centered(g, "Logged", x + 3 * w / 4, y + 140, new Color(238, 178, 77));
            centered(g, "Not tracked", x + 3 * w / 4, y + 156, Color.LIGHT_GRAY);
            card(g, x + 3, y + 168, w - 6, r.height - 205);
            centered(g, "Membership", x + w / 2, y + 182, new Color(238, 178, 77));
            label(g, "Rank: " + fit(g, data[4], 55), x + 10, y + 199);
            label(g, "Spent: $" + data[5], x + 106, y + 199);
            label(g, "Status: " + (data[5].equals("0") ? "Standard" : "Supporter"), x + 10, y + 214);
            int[] icons = {1149, 11128, 405, 9951};
            String[] names = {"Gear presets", "Achievements", "Settings", "Bestiary"};
            for (int i = 0; i < 4; i++) {
                Rectangle button = new Rectangle(x + 4 + i * 46, y + r.height - 34, 41, 30);
                card(g, button.x, button.y, button.width, button.height);
                g.drawImage(items.getImage(icons[i]), button.x + 5, button.y + 1, 30, 27, null);
                if (button.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
                    tooltips.add(new Tooltip(names[i]));
            }
        }
        private String fit(Graphics2D g, String text, int width) {
            text = text == null ? "" : text.replaceAll("<[^>]*>", "");
            while (text.length() > 1 && g.getFontMetrics().stringWidth(text) > width) text = text.substring(0, text.length() - 1);
            return text;
        }
        private void label(Graphics2D g, String text, int x, int y) { g.setColor(new Color(238, 204, 64)); g.drawString(text, x, y); }
        private void centered(Graphics2D g, String text, int x, int y, Color color) { g.setColor(color); g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y); }
        private void card(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(68, 61, 46)); g.fillRect(x, y, w, h);
            g.setColor(new Color(120, 108, 81)); g.drawRect(x, y, w, h);
            g.setColor(new Color(30, 27, 22)); g.drawLine(x + 1, y + h - 1, x + w - 1, y + h - 1);
        }
        private void star(Graphics2D g, int x, int y) {
            int[] xs = {x, x + 3, x + 8, x + 4, x + 5, x, x - 5, x - 4, x - 8, x - 3};
            int[] ys = {y - 8, y - 3, y - 2, y + 2, y + 7, y + 4, y + 7, y + 2, y - 2, y - 3};
            g.fillPolygon(xs, ys, xs.length);
        }
        private void box(Graphics2D g, Rectangle r, String label, boolean chosen) {
            g.setColor(chosen ? new Color(102, 83, 45) : new Color(67, 56, 38)); g.fill(r);
            g.setColor(new Color(158, 131, 80)); g.draw(r);
            g.setColor(new Color(255, 216, 133)); g.drawString(label, r.x + 4, r.y + r.height / 2 + 4);
        }
    }
    private class Input extends MouseAdapter implements KeyListener, MouseWheelListener {
        private boolean gearHeld, quickHeld;
        private final boolean[] presetHeld = new boolean[FlavorScapeCommands.MAX_PRESETS];
        @Override public void keyPressed(KeyEvent e) {
            int slot = e.getKeyCode() - KeyEvent.VK_F1;
            if (slot >= 0 && slot < presetHeld.length && open && !panel.isEmpty()
                    && client.getGameState() == GameState.LOGGED_IN
                    && client.getVar(VarClientInt.INTERFACE_TAB) == 2
                    && client.getVar(VarClientInt.INPUT_TYPE) == 0
                    && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown() && !e.isShiftDown()) {
                if (!presetHeld[slot]) {
                    presetHeld[slot] = true;
                    selected = slot;
                    loadPreset("gear " + (slot + 1));
                }
                e.consume();
                return;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE && open && client.getGameState() == GameState.LOGGED_IN
                    && client.getVarbitValue(client.getVarps(), ESCAPE_CLOSES_INTERFACES) == 1) {
                clientThread.invokeLater(() -> closePanel());
                e.consume();
                return;
            }
            if (!e.isControlDown() || e.isAltDown() || e.isMetaDown() || client.getGameState() != GameState.LOGGED_IN) return;
            if (e.getKeyCode() == KeyEvent.VK_G) {
                if (client.getVar(VarClientInt.INTERFACE_TAB) != 2) return;
                if (!gearHeld) { gearHeld = true; clientThread.invokeLater(() -> togglePanel()); }
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_Q) {
                if (!quickHeld) { quickHeld = true; loadPreset("lastgear"); }
                e.consume();
            }
        }
        @Override public void keyReleased(KeyEvent e) {
            int slot = e.getKeyCode() - KeyEvent.VK_F1;
            if (slot >= 0 && slot < presetHeld.length) {
                if (presetHeld[slot]) e.consume();
                presetHeld[slot] = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_G || e.getKeyCode() == KeyEvent.VK_CONTROL) gearHeld = false;
            if (e.getKeyCode() == KeyEvent.VK_Q || e.getKeyCode() == KeyEvent.VK_CONTROL) quickHeld = false;
        }
        @Override public void keyTyped(KeyEvent e) {}
        private boolean over(MouseEvent e) { return panel.contains(e.getPoint()) || toggle.contains(e.getPoint()) || questFrame.contains(e.getPoint()); }
        @Override public MouseEvent mousePressed(MouseEvent e) {
            if (e.isConsumed() || !over(e)) return e;
            // Middle mouse and Alt-left are camera controls in the native client.
            if (e.getButton() != MouseEvent.BUTTON1 || e.isAltDown() || e.isMetaDown()) return e;
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (toggle.contains(e.getPoint())) clientThread.invokeLater(() -> togglePanel());
                else if (questFrame.contains(e.getPoint())) {
                    Rectangle q = questFrame;
                    if (e.getY() >= q.y + q.height - 34) {
                        int button = (e.getX() - q.x - 4) / 46;
                        if (button == 0) clientThread.invokeLater(() -> togglePanel());
                        else if (button >= 1 && button <= 3) command("questpanel " + new String[]{"", "achievements", "settings", "bestiary"}[button]);
                    } else if (e.getY() >= q.y + 41 && e.getY() < q.y + 107) command("questpanel risk");
                }
                else {
                    Rectangle p = panel;
                    if (e.getY() >= p.y + p.height - 23) command("savegear " + (selected + 1));
                    else if (e.getY() >= p.y + 24) {
                        int index = (e.getY() - p.y - 24 + scroll) / ROW_HEIGHT;
                        List<Row> snapshot = rows;
                        if (index >= 0 && index < snapshot.size()) {
                            Row row = snapshot.get(index);
                            selected = row.slot;
                            int localY = (e.getY() - p.y - 24 + scroll) % ROW_HEIGHT;
                            if (localY >= 81 && localY <= 104 && e.getX() >= p.x + 8 && e.getX() <= p.x + 94)
                                command("storegear " + (row.slot + 1));
                            if (localY >= 81 && localY <= 104 && e.getX() >= p.x + 143)
                                loadPreset("gear " + (row.slot + 1));
                        }
                    }
                }
            }
            e.consume(); return e;
        }
        // The native handler clears its held-button state on release, including
        // drags that began outside this overlay. Never hide that release from it.
        @Override public MouseEvent mouseReleased(MouseEvent e) { return e; }
        @Override public MouseEvent mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && !e.isAltDown() && !e.isMetaDown() && over(e)) e.consume();
            return e;
        }
        @Override public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e) {
            if (panel.contains(e.getPoint())) { scroll = Math.max(0, scroll + e.getWheelRotation() * 35); e.consume(); }
            else if (questFrame.contains(e.getPoint())) e.consume();
            return e;
        }
    }
}
