package net.runelite.client.plugins.flavorscapegears;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import net.runelite.standalone.FlavorScapeCommands;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresetSlotInputTest {
    private final List<String> commands=new ArrayList<>();
    private final Canvas canvas=new Canvas();
    private final FlavorScapeGearsPlugin plugin=new FlavorScapeGearsPlugin(){
        @Override void command(String command){commands.add(command);}
    };
    private KeyListener keys;
    private MouseAdapter mouse;
    private int tab=2,inputType;
    private Field field(String name)throws Exception {Field f=FlavorScapeGearsPlugin.class.getDeclaredField(name);f.setAccessible(true);return f;}
    @Before public void setup()throws Exception {
        field("client").set(plugin,Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args)->{
            if(method.getName().equals("getGameState"))return GameState.LOGGED_IN;
            if(method.getName().equals("getVar"))return args[0]==VarClientInt.INTERFACE_TAB?tab:inputType;
            return null;
        }));
        field("open").setBoolean(plugin,true);
        field("panel").set(plugin,new Rectangle(0,0,198,360));
        keys=(KeyListener)field("input").get(plugin);
        mouse=(MouseAdapter)field("input").get(plugin);
    }
    private KeyEvent press(int code,int modifiers){
        KeyEvent event=new KeyEvent(canvas,KeyEvent.KEY_PRESSED,0,modifiers,code,KeyEvent.CHAR_UNDEFINED);
        keys.keyPressed(event);return event;
    }
    private void release(int code){keys.keyReleased(new KeyEvent(canvas,KeyEvent.KEY_RELEASED,0,0,code,KeyEvent.CHAR_UNDEFINED));}
    @Test public void f1ThroughF12LoadMatchingSlotsAndSuppressKeyRepeat()throws Exception {
        for(int slot=0;slot<12;slot++){
            assertTrue(press(KeyEvent.VK_F1+slot,0).isConsumed());
            press(KeyEvent.VK_F1+slot,0);
            assertEquals(slot+1,commands.size());
            assertEquals("gear "+(slot+1),commands.get(slot));
            assertEquals(slot,field("selected").getInt(plugin));
            release(KeyEvent.VK_F1+slot);
        }
        press(KeyEvent.VK_F12,0);assertEquals(13,commands.size());
    }
    @Test public void normalFunctionKeysRemainAvailableOutsideTheVisiblePanel()throws Exception {
        field("open").setBoolean(plugin,false);assertFalse(press(KeyEvent.VK_F1,0).isConsumed());
        field("open").setBoolean(plugin,true);tab=3;assertFalse(press(KeyEvent.VK_F1,0).isConsumed());
        tab=2;inputType=7;assertFalse(press(KeyEvent.VK_F1,0).isConsumed());
        inputType=0;assertFalse(press(KeyEvent.VK_F1,InputEvent.SHIFT_DOWN_MASK).isConsumed());
        field("panel").set(plugin,new Rectangle());assertFalse(press(KeyEvent.VK_F1,0).isConsumed());
        assertTrue(commands.isEmpty());
    }
    @Test public void bothDefaultAndTwelfthRowsCanBeSavedAndLoaded()throws Exception {
        Class<?> row=Class.forName(FlavorScapeGearsPlugin.class.getName()+"$Row");
        Constructor<?> constructor=row.getDeclaredConstructor(int.class,String.class);constructor.setAccessible(true);
        field("rows").set(plugin,Arrays.asList(constructor.newInstance(0,"Default|-1|ANCIENT|Stats"),constructor.newInstance(11,"Saved|-1|MODERN|Stats")));
        for(int i=0;i<2;i++){
            int y=24+i*116+90;
            mouse.mousePressed(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,20,y,1,false,MouseEvent.BUTTON1));
            mouse.mousePressed(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,160,y,1,false,MouseEvent.BUTTON1));
        }
        assertEquals(Arrays.asList("storegear 1","gear 1","storegear 12","gear 12"),commands);
        mouse.mousePressed(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,0,40,347,1,false,MouseEvent.BUTTON1));
        assertEquals("savegear 12",commands.get(4));
    }
    @Test public void protocolPublishesTwelveSlotsOnlyAfterEndAndIgnoresOldExtraRows(){
        int group=32766<<16;
        FlavorScapeCommands.clear();FlavorScapeCommands.receive(group,"FS_GEARS_BEGIN");
        FlavorScapeCommands.receive(group|12,"Twelfth|-1|MODERN|Stats");
        FlavorScapeCommands.receive(group|17,"Legacy extra slot");
        assertNull(FlavorScapeCommands.previews()[11]);
        FlavorScapeCommands.receive(group|18,"FS_GEARS_END");
        assertEquals(12,FlavorScapeCommands.previews().length);
        assertTrue(FlavorScapeCommands.previews()[11].startsWith("Twelfth|"));
        FlavorScapeCommands.clear();
    }
}
