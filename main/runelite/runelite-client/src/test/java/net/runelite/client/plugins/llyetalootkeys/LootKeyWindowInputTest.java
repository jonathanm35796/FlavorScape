package net.runelite.client.plugins.llyetalootkeys;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.standalone.LlyetaLootKeyState;
import net.runelite.standalone.LootKeySpriteFixture;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LootKeyWindowInputTest {
    private static final int BASE=32764<<16;
    private final Canvas canvas=new Canvas();
    private final java.util.List<String> commands=new ArrayList<>();
    private LlyetaLootKeysPlugin plugin;
    private MouseAdapter mouse;
    private KeyListener keys;
    private static Field field(String name)throws Exception{Field field=LlyetaLootKeysPlugin.class.getDeclaredField(name);field.setAccessible(true);return field;}
    private static String data(String token,int revision){return "{\"token\":\""+token+"\",\"revision\":"+revision+",\"selected\":\"key0\",\"noted\":false,\"canClaim\":true,\"bankUsed\":319,\"bankCapacity\":800,\"keys\":[{\"token\":\"key0\",\"value\":15500000},{\"token\":\"key1\",\"value\":1000}],\"items\":[{\"id\":199,\"name\":\"Grimy guam leaf\",\"amount\":199,\"value\":3500000},{\"id\":995,\"name\":\"Coins\",\"amount\":12000000,\"value\":12000000},{\"id\":13307,\"name\":\"Blood Money\",\"amount\":4000,\"value\":4000}]}";}
    @Before public void open()throws Exception{
        LlyetaLootKeyState.clear();LlyetaLootKeyState.receive(BASE,data("session",1));
        plugin=new LlyetaLootKeysPlugin(){@Override void command(String text){commands.add(text);}@Override void detach(){commands.add("detach");}};
        field("panel").set(plugin,new Rectangle(0,0,310,324));
        field("clientThread").set(plugin,new ClientThread(){@Override public void invokeLater(Runnable action){action.run();}});
        mouse=(MouseAdapter)field("input").get(plugin);keys=(KeyListener)field("input").get(plugin);
    }
    @After public void clear(){LlyetaLootKeyState.clear();}
    private void click(int x,int y){MouseEvent event=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false,MouseEvent.BUTTON1);mouse.mousePressed(event);assertTrue(event.isConsumed());}

    @Test public void allThreeFooterButtonsSendTheirOwnServerAction(){
        click(155,300);click(230,300);click(280,300);
        assertEquals(Arrays.asList("lootkeys session 1 inventory","lootkeys session 1 bank","lootkeys session 1 trash"),commands);
    }
    @Test public void itemNoteAndKeyTabsSendTheSelectedControl(){
        click(30,307);click(75,307);click(80,55);click(20,96);
        assertEquals(Arrays.asList("lootkeys session 1 item","lootkeys session 1 note","lootkeys session 1 key key1","lootkeys session 1 take 0"),commands);
    }
    @Test public void closeAndEscapeDetachTheHostBeforeDismissingTheWindow()throws Exception{
        click(292,18);assertEquals(Arrays.asList("lootkeys session 1 close","detach"),commands);assertNull(LlyetaLootKeyState.get());
        LlyetaLootKeyState.receive(BASE,data("next",1));field("panel").set(plugin,new Rectangle(0,0,310,324));
        keys.keyPressed(new KeyEvent(canvas,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27));assertNull(LlyetaLootKeyState.get());
    }
    @Test public void staleSnapshotsAndLateUpdatesCannotReopenDismissedKeys(){
        LlyetaLootKeyState.receive(BASE,data("session",2));LlyetaLootKeyState.receive(BASE,data("session",1));assertEquals(2,LlyetaLootKeyState.get().revision);
        LlyetaLootKeyState.dismiss("session");LlyetaLootKeyState.receive(BASE,data("session",3));assertNull(LlyetaLootKeyState.get());
        LlyetaLootKeyState.receive(BASE,data("next",1));assertEquals("next",LlyetaLootKeyState.get().token);
        LlyetaLootKeyState.receive(BASE|1,"CLOSE");assertNull(LlyetaLootKeyState.get());
    }
    @Test public void malformedPacketsDoNotReplaceTheCurrentWindow(){
        LlyetaLootKeyState.receive(BASE,"bad JSON");LlyetaLootKeyState.receive(BASE,"{\"token\":\"bad\",\"revision\":3}");
        assertEquals("session",LlyetaLootKeyState.get().token);assertFalse(LlyetaLootKeyState.receive(300<<16,"ordinary widget"));
    }
    @Test public void ironmanClaimButtonsAreDisabledWhileTrashRemainsAvailable(){
        LlyetaLootKeyState.get().canClaim=false;click(155,300);click(230,300);click(280,300);
        assertEquals(Collections.singletonList("lootkeys session 1 trash"),commands);
    }
    @Test public void scrollbarWheelAndDragStayWithinTheLootGrid()throws Exception{
        varItems(28);MouseWheelListener wheel=(MouseWheelListener)field("input").get(plugin);
        wheel.mouseWheelMoved(new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,0,0,200,160,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,3,100));assertEquals(33,field("scroll").getInt(plugin));
        field("scroll").setInt(plugin,0);click(296,101);mouse.mouseDragged(new MouseEvent(canvas,MouseEvent.MOUSE_DRAGGED,0,0,296,500,1,false,MouseEvent.BUTTON1));assertEquals(33,field("scroll").getInt(plugin));
        mouse.mouseReleased(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,0,0,296,500,1,false,MouseEvent.BUTTON1));assertFalse(field("dragging").getBoolean(plugin));
    }
    private static void varItems(int amount){LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();LlyetaLootKeyState.Item item=state.items.get(0);while(state.items.size()<amount)state.items.add(item);}

    @Test public void rendersAReviewableWindowWithNativeFontsAndButtonSprites()throws Exception{
        Assume.assumeTrue("Optional loot-window visual QA","1".equals(System.getenv("LLYETA_RENDER_LOOT_KEYS")));
        final Path assets=Paths.get("../../../.tools/loot-key-ui-assets");
        SpriteManager sprites=mock(SpriteManager.class);when(sprites.getSprite(anyInt(),eq(0))).thenAnswer(call->LootKeySpriteFixture.read(assets.resolve("sprite-"+call.getArgument(0)+".dat")));
        ItemManager items=mock(ItemManager.class);
        when(items.getImage(anyInt())).thenAnswer(call->itemImage(assets,call.getArgument(0)));
        when(items.getImage(anyInt(),anyInt(),eq(false))).thenAnswer(call->itemImage(assets,call.getArgument(0)));
        field("sprites").set(plugin,sprites);field("items").set(plugin,items);
        BufferedImage image=new BufferedImage(310,324,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=image.createGraphics();
        try{plugin.draw(graphics,LlyetaLootKeyState.get(),0,0);}finally{graphics.dispose();}
        Path output=Paths.get("../../../.tools/loot-key-window-preview.png");ImageIO.write(image,"PNG",output.toFile());assertTrue(Files.size(output)>4000);
    }
    private static AsyncBufferedImage itemImage(Path assets,int id)throws Exception{
        BufferedImage source=ImageIO.read(assets.resolve("item-"+(id==30330?13303:id)+".gif").toFile());assertNotNull(source);
        AsyncBufferedImage image=new AsyncBufferedImage(36,32,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.drawImage(source,0,0,32,32,null);g.dispose();image.loaded();return image;
    }
}
