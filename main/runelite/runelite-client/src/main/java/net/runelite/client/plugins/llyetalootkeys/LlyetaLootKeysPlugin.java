package net.runelite.client.plugins.llyetalootkeys;

import java.awt.*;
import java.awt.event.*;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.standalone.LlyetaCommands;
import net.runelite.standalone.LlyetaLootKeyState;

@PluginDescriptor(name="Llyeta Loot Keys",description="Wilderness loot-key chest and withdrawal controls",enabledByDefault=true)
public class LlyetaLootKeysPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private SpriteManager sprites;
    @Inject private OverlayManager overlays;
    @Inject private MouseManager mouse;
    @Inject private KeyManager keys;
    private final LootOverlay overlay=new LootOverlay();
    private final Input input=new Input();
    private Rectangle panel=new Rectangle();
    private String selected="";
    private int scroll,grab;
    private boolean dragging;
    private static final int WIDTH=310,HEIGHT=324,TOP=81,VIEW_HEIGHT=192,COLUMNS=6,CELL=45;
    private static final Color GOLD=new Color(255,181,45),BG=new Color(76,69,56),DARK=new Color(38,36,29);
    @Override protected void startUp(){overlays.add(overlay);mouse.registerMouseListener(0,input);mouse.registerMouseWheelListener(0,input);keys.registerKeyListener(input);}
    @Override protected void shutDown(){overlays.remove(overlay);mouse.unregisterMouseListener(input);mouse.unregisterMouseWheelListener(input);keys.unregisterKeyListener(input);LlyetaLootKeyState.clear();panel=new Rectangle();}
    void command(String command){clientThread.invokeLater(()->LlyetaCommands.send(command));}
    void detach(){LlyetaCommands.closeLootKeyWidgets();}
    private void action(String action){LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();if(state!=null)command("lootkeys "+state.token+" "+state.revision+" "+action);}
    private void close(){LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();if(state==null)return;
        action("close");clientThread.invokeLater(()->{LlyetaLootKeyState.Snapshot current=LlyetaLootKeyState.get();if(current!=null&&current.token.equals(state.token)){detach();LlyetaLootKeyState.dismiss(state.token);panel=new Rectangle();}});
    }
    private int maximum(){LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();return state==null?0:Math.max(0,((state.items.size()+COLUMNS-1)/COLUMNS)*CELL-VIEW_HEIGHT);}
    private Rectangle thumb(){int track=VIEW_HEIGHT-32,size=Math.min(track,Math.max(12,track*VIEW_HEIGHT/(VIEW_HEIGHT+maximum())));return new Rectangle(panel.x+289,panel.y+TOP+16+(maximum()==0?0:scroll*(track-size)/maximum()),16,size);}
    private void scrollTo(int value){scroll=Math.max(0,Math.min(maximum(),value));}
    private static void text(Graphics2D g,String text,int x,int y,Color color){g.setColor(Color.BLACK);g.drawString(text,x+1,y+1);g.setColor(color);g.drawString(text,x,y);}
    private static void box(Graphics2D g,int x,int y,int width,int height,Color fill){g.setColor(fill);g.fillRect(x,y,width,height);g.setColor(new Color(132,121,96));g.drawLine(x,y,x+width-1,y);g.drawLine(x,y,x,y+height-1);g.setColor(new Color(24,26,20));g.drawLine(x,y+height-1,x+width-1,y+height-1);g.drawLine(x+width-1,y,x+width-1,y+height-1);}
    static String compact(long value){if(value>=1_000_000_000L)return String.format(java.util.Locale.ROOT,"%.1fB",value/1e9).replace(".0B","B");if(value>=1_000_000L)return String.format(java.util.Locale.ROOT,"%.1fM",value/1e6).replace(".0M","M");if(value>=10_000L)return (value/1000)+"K";return Long.toString(value);}
    private void icon(Graphics2D g,int sprite,int x,int y){java.awt.image.BufferedImage image=sprites.getSprite(sprite,0);if(image!=null)g.drawImage(image,x,y,32,32,null);}

    private class LootOverlay extends Overlay {
        LootOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);}
        @Override public Dimension render(Graphics2D g){
            panel=new Rectangle();if(client.getGameState()!=GameState.LOGGED_IN){LlyetaLootKeyState.clear();return null;}
            LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();if(state==null||client.getWidget(155,0)==null)return null;
            if(!state.selected.equals(selected)){selected=state.selected;scroll=0;}scrollTo(scroll);
            Widget viewport=client.getViewportWidget();Rectangle bounds=viewport==null?new Rectangle(0,0,512,334):viewport.getBounds();
            int x=bounds.x+Math.max(0,(bounds.width-WIDTH)/2),y=bounds.y+Math.max(0,(bounds.height-HEIGHT)/2);panel=new Rectangle(x,y,WIDTH,HEIGHT);
            draw(g,state,x,y);return null;
        }
    }
    void draw(Graphics2D g,LlyetaLootKeyState.Snapshot state,int x,int y){
        box(g,x,y,WIDTH,HEIGHT,new Color(25,28,22));box(g,x+3,y+3,WIDTH-6,HEIGHT-6,BG);
        g.setFont(FontManager.getRunescapeBoldFont());String title="Wilderness Loot Keys";text(g,title,x+(WIDTH-g.getFontMetrics().stringWidth(title))/2,y+21,GOLD);
        box(g,x+284,y+8,19,20,BG);text(g,"X",x+288,y+23,GOLD);g.setFont(FontManager.getRunescapeSmallFont());
        g.setColor(DARK);g.drawLine(x+7,y+34,x+303,y+34);
        for(int i=0;i<state.keys.size();i++){LlyetaLootKeyState.Key key=state.keys.get(i);int kx=x+10+i*54;
            box(g,kx,y+39,49,37,key.token.equals(state.selected)?new Color(98,91,76):BG);
            g.drawImage(items.getImage(30330),kx+13,y+40,24,24,null);text(g,compact(key.value),kx+3,y+73,new Color(255,255,100));
        }
        box(g,x+7,y+TOP-3,282,VIEW_HEIGHT+6,new Color(68,62,50));Shape clip=g.getClip();g.clip(new Rectangle(x+9,y+TOP,278,VIEW_HEIGHT));
        for(int i=0;i<state.items.size();i++){LlyetaLootKeyState.Item item=state.items.get(i);int ix=x+13+(i%COLUMNS)*CELL,iy=y+TOP+(i/COLUMNS)*CELL-scroll;
            if(iy+40<y+TOP||iy>y+TOP+VIEW_HEIGHT)continue;
            g.drawImage(items.getImage(item.id,item.amount,false),ix+3,iy+9,32,32,null);text(g,compact(item.amount),ix,iy+10,new Color(255,255,70));
        }
        g.setClip(clip);int sx=x+289;g.setColor(new Color(2301979));g.fillRect(sx,y+TOP,16,VIEW_HEIGHT);
        java.awt.image.BufferedImage up=sprites.getSprite(SpriteID.SCROLLBAR_ARROW_UP,0),down=sprites.getSprite(SpriteID.SCROLLBAR_ARROW_DOWN,0);
        if(up!=null)g.drawImage(up,sx,y+TOP,16,16,null);if(down!=null)g.drawImage(down,sx,y+TOP+VIEW_HEIGHT-16,16,16,null);
        Rectangle thumb=thumb();box(g,thumb.x,thumb.y,thumb.width,thumb.height,new Color(91,81,62));
        g.setColor(DARK);g.drawLine(x+7,y+280,x+303,y+280);text(g,"Withdraw as:",x+18,y+294,GOLD);
        box(g,x+12,y+298,43,19,state.noted?BG:new Color(148,41,23));box(g,x+57,y+298,43,19,state.noted?new Color(148,41,23):BG);
        text(g,"Item",x+19,y+312,GOLD);text(g,"Note",x+63,y+312,GOLD);
        for(int bx:new int[]{139,213,264})box(g,x+bx,y+285,35,33,BG);
        icon(g,SpriteID.BANK_RAID_SEND_TO_INVENTORY,x+141,y+285);icon(g,SpriteID.BANK_RAID_SEND_TO_BANK,x+215,y+285);icon(g,SpriteID.BANK_RAID_SEND_TO_TRASH,x+266,y+285);
        text(g,Integer.toString(state.bankUsed),x+181,y+298,GOLD);text(g,Integer.toString(state.bankCapacity),x+181,y+312,GOLD);
        if(client!=null){net.runelite.api.Point mouse=client.getMouseCanvasPosition();if(mouse!=null){int mx=mouse.getX()-x,my=mouse.getY()-y;String hover=null;
            if(my>=285&&mx>=139&&mx<174)hover=state.canClaim?"Withdraw all to inventory":"Ironmen can only destroy loot";
            else if(my>=285&&mx>=213&&mx<248)hover=state.canClaim?"Deposit all to bank":"Ironmen can only destroy loot";
            else if(my>=285&&mx>=264&&mx<299)hover="Destroy key and contents";
            else if(mx>=12&&mx<284&&my>=TOP&&my<TOP+VIEW_HEIGHT){int row=(my-TOP+scroll)/CELL*COLUMNS+(mx-12)/CELL;if(row<state.items.size())hover=state.items.get(row).name+" - "+compact(state.items.get(row).value)+" value";}
            if(hover!=null){int width=g.getFontMetrics().stringWidth(hover)+8;int tx=Math.min(x+WIDTH-width,mouse.getX()+8),ty=Math.max(y+16,mouse.getY()-14);box(g,tx,ty-12,width,17,new Color(35,32,25));text(g,hover,tx+4,ty,GOLD);}
        }}
    }
    private class Input extends MouseAdapter implements MouseWheelListener,KeyListener {
        @Override public MouseEvent mousePressed(MouseEvent e){
            if(panel.isEmpty()||e.isAltDown()||e.getButton()==MouseEvent.BUTTON2||client!=null&&client.isMenuOpen())return e;
            if(!panel.contains(e.getPoint()))return e;LlyetaLootKeyState.Snapshot state=LlyetaLootKeyState.get();if(state==null)return e;
            int x=e.getX()-panel.x,y=e.getY()-panel.y;
            if(e.getButton()==MouseEvent.BUTTON1){
                if(x>=284&&y<31)close();
                else if(y>=298&&y<318&&x>=12&&x<100)action(x<56?"item":"note");
                else if(y>=285&&y<319&&x>=139&&x<174){if(state.canClaim)action("inventory");}
                else if(y>=285&&y<319&&x>=213&&x<248){if(state.canClaim)action("bank");}
                else if(y>=285&&y<319&&x>=264&&x<299)action("trash");
                else if(y>=39&&y<77&&x>=10&&x<280){int key=(x-10)/54;if(key<state.keys.size())action("key "+state.keys.get(key).token);}
                else if(x>=289&&y>=TOP&&y<TOP+VIEW_HEIGHT){Rectangle thumb=thumb();if(y<TOP+16)scrollTo(scroll-4);else if(y>=TOP+VIEW_HEIGHT-16)scrollTo(scroll+4);else if(thumb.contains(e.getPoint())){dragging=true;grab=e.getY()-thumb.y;}else scrollTo(scroll+(e.getY()<thumb.y?-VIEW_HEIGHT:VIEW_HEIGHT));}
                else if(x>=12&&x<284&&y>=TOP&&y<TOP+VIEW_HEIGHT){int row=(y-TOP+scroll)/CELL*COLUMNS+(x-12)/CELL;if(row<state.items.size()&&state.canClaim)action("take "+row);}
            }
            e.consume();return e;
        }
        @Override public MouseEvent mouseDragged(MouseEvent e){if(dragging){Rectangle thumb=thumb();int travel=VIEW_HEIGHT-32-thumb.height;scrollTo(travel==0?0:(int)Math.round((e.getY()-grab-panel.y-TOP-16)*(double)maximum()/travel));e.consume();}return e;}
        @Override public MouseEvent mouseReleased(MouseEvent e){dragging=false;return e;}
        @Override public MouseEvent mouseClicked(MouseEvent e){if(panel.contains(e.getPoint())&&!(client!=null&&client.isMenuOpen()))e.consume();return e;}
        @Override public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e){if(panel.contains(e.getPoint())){scrollTo(scroll+e.getWheelRotation()*45);e.consume();}return e;}
        @Override public void keyPressed(KeyEvent e){if(!panel.isEmpty()&&e.getKeyCode()==KeyEvent.VK_ESCAPE&&!(client!=null&&client.isMenuOpen())){close();e.consume();}}
        @Override public void keyTyped(KeyEvent e){}
        @Override public void keyReleased(KeyEvent e){}
    }
}
