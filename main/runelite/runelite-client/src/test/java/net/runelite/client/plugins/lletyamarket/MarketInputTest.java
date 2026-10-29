package net.runelite.client.plugins.lletyamarket;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.KeyListener;
import net.runelite.client.callback.ClientThread;
import net.runelite.standalone.LletyaMarketState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarketInputTest {
    private LletyaMarketPlugin plugin;
    private MouseAdapter mouse;
    private KeyListener keys;
    private final Canvas canvas = new Canvas();
    private Field field(String name) throws Exception {Field f=LletyaMarketPlugin.class.getDeclaredField(name);f.setAccessible(true);return f;}
    @Before public void open() throws Exception {
        LletyaMarketState.clear();
        LletyaMarketState.receive(32765<<16,"{\"token\":\"test\"}");
        LletyaMarketState.receive((32765<<16)|1,"{\"id\":11802,\"name\":\"Dragon plateskirt\",\"price\":1000,\"category\":0}");
        LletyaMarketState.receive((32765<<16)|2,"END");
        plugin=new LletyaMarketPlugin();
        field("panel").set(plugin,new Rectangle(0,0,506,324));
        field("layout").set(plugin,new MarketLayout(LletyaMarketState.get().items,true));
        mouse=(MouseAdapter)field("input").get(plugin);
        keys=(KeyListener)field("input").get(plugin);
    }
    @After public void clear(){LletyaMarketState.clear();}
    private void click(int x,int y){mouse.mousePressed(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false,MouseEvent.BUTTON1));}
    @Test public void clickingTheSelectorAgainClosesIt() throws Exception {
        click(138,15);assertTrue(field("dropdown").getBoolean(plugin));
        click(138,15);assertFalse(field("dropdown").getBoolean(plugin));
        assertNotNull(LletyaMarketState.get());
    }
    @Test public void replacementShopCannotOpenTheRemovedMarketSelector() throws Exception {
        LletyaMarketState.get().hasMarketSelector=false;
        field("dropdown").setBoolean(plugin,true);
        click(138,15);assertFalse(field("dropdown").getBoolean(plugin));
        click(138,15);assertFalse(field("dropdown").getBoolean(plugin));
        assertNotNull(LletyaMarketState.get());
    }
    @Test public void rightClickPassesThroughToTheNativeMenu() {
        MouseEvent event=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,145,77,1,false,MouseEvent.BUTTON3);
        mouse.mousePressed(event);
        assertFalse(event.isConsumed());
    }
    @Test public void openNativeMenuReceivesLeftClicksOverShopControlsAndItems() throws Exception {
        field("client").set(plugin,Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},
            (proxy,method,args)->method.getName().equals("isMenuOpen")?true:null));
        for(int[] point:new int[][]{{145,77},{138,15},{489,15},{20,50},{490,100}}){
            MouseEvent press=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,point[0],point[1],1,false,MouseEvent.BUTTON1);
            mouse.mousePressed(press);
            assertFalse("Native selection/dismissal must receive the click",press.isConsumed());
            MouseEvent clicked=new MouseEvent(canvas,MouseEvent.MOUSE_CLICKED,0,0,point[0],point[1],1,false,MouseEvent.BUTTON1);
            mouse.mouseClicked(clicked);assertFalse(clicked.isConsumed());
        }
        assertFalse(field("dropdown").getBoolean(plugin));
        assertNotNull(LletyaMarketState.get());
    }
    @Test public void closeDetachesNativeShopAndDismissesWithoutWaitingForServer() throws Exception {
        java.util.List<String> actions=new java.util.ArrayList<>();
        plugin=new LletyaMarketPlugin(){
            @Override void command(String text){actions.add(text);}
            @Override void closeNativeShop(){
                assertNotNull("Keep covering the native windows until they detach",LletyaMarketState.get());
                actions.add("native close");
            }
        };
        field("panel").set(plugin,new Rectangle(0,0,506,324));
        mouse=(MouseAdapter)field("input").get(plugin);
        field("clientThread").set(plugin,new ClientThread(){@Override public void invokeLater(Runnable action){action.run();}});
        click(489,15);
        assertEquals(java.util.Arrays.asList("pvpmarket close test","native close"),actions);
        assertNull("No server acknowledgement is needed for local dismissal",LletyaMarketState.get());
        assertTrue(((Rectangle)field("panel").get(plugin)).isEmpty());
        LletyaMarketState.receive((32765<<16)|3,"CLOSE");
        assertNull(LletyaMarketState.get());
    }
    @Test public void nativeMenuHasBuyOptionsForTheHoveredCatalogueItem() throws Exception {
        MenuOpened menu=new MenuOpened();menu.setMenuEntries(new MenuEntry[0]);plugin.populateNativeMenu(menu,145,77);
        MenuEntry[] entries=menu.getMenuEntries();
        assertEquals(7,entries.length);
        assertEquals("Value",entries[6].getOption());assertEquals("Buy 1",entries[5].getOption());
        assertEquals("Buy 5",entries[4].getOption());assertEquals("Buy 10",entries[3].getOption());
        assertEquals("Buy X",entries[2].getOption());assertEquals("Examine",entries[1].getOption());
        for(int i=1;i<entries.length;i++)assertEquals("Dragon plateskirt",entries[i].getTarget());
        assertEquals(MenuOpcode.RUNELITE.getId(),entries[6].getOpcode());
        assertEquals(MenuOpcode.EXAMINE_ITEM.getId(),entries[1].getOpcode());
    }
    @Test public void hoverTargetUsesTheVisibleCatalogueItem() {
        MenuEntryAdded entry=new MenuEntryAdded("Value","Wrong underlying item",11802,MenuOpcode.WIDGET_TYPE_1.getId(),0,0,false);
        plugin.updateHoverTarget(entry,145,77);
        assertEquals("Dragon plateskirt",entry.getTarget());
        assertTrue(entry.isModified());
    }
    @Test public void clickingOutsideThePanelDismissesTheSelector() throws Exception {
        click(138,15);click(600,200);assertFalse(field("dropdown").getBoolean(plugin));
        assertNotNull(LletyaMarketState.get());
    }
    @Test public void escapeDismissesTheSelectorWithoutClosingTheShop() throws Exception {
        click(138,15);keys.keyPressed(new KeyEvent(canvas,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27));
        assertFalse(field("dropdown").getBoolean(plugin));assertNotNull(LletyaMarketState.get());
    }
    @Test public void scrollbarDraggingReachesBothEndsWithoutOverscroll(){
        MarketScrollBar bar=new MarketScrollBar(0,36,252,2000,500);
        assertEquals(0,bar.dragTo(-100,5));
        assertEquals(bar.maximum,bar.dragTo(1000,5));
        assertEquals(bar.maximum,bar.dragTo(bar.top+bar.travel+5,5));
        MarketScrollBar fitting=new MarketScrollBar(0,36,252,100,0);
        assertEquals(fitting.trackHeight,fitting.thumb.height);
        assertEquals(0,fitting.dragTo(1000,5));
    }
}
