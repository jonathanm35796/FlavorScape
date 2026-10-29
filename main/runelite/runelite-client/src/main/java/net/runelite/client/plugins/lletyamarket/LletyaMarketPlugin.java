package net.runelite.client.plugins.lletyamarket;

import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.SpriteID;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
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
import net.runelite.standalone.LletyaCommands;
import net.runelite.standalone.LletyaMarketState;

@PluginDescriptor(name="Lletya Market",description="Categorised PvP market with server-provided stock and prices",enabledByDefault=true)
public class LletyaMarketPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private SpriteManager sprites;
    @Inject private OverlayManager overlays;
    @Inject private MouseManager mouse;
    @Inject private KeyManager keys;
    @Inject private EventBus eventBus;
    private final MarketOverlay overlay=new MarketOverlay();
    private final Input input=new Input();
    private volatile Rectangle panel=new Rectangle();
    private volatile MarketLayout layout=new MarketLayout(java.util.Collections.emptyList(),true);
    private LletyaMarketState.Snapshot lastSnapshot;
    private int scroll,activeMenuItem=-1;
    private String activeMenuToken;
    private int selectedCategory,pendingJump=-1;
    private boolean draggingScroll;
    private int scrollGrabOffset;
    private boolean searching,dropdown;
    private String query="",lastToken="",lastView="";
    private static final String[] CATEGORIES={"Melee","Ranged","Magic","Miscellaneous","Untradeables","Barrows","Cosmetics","Resources","Tools"};
    private static final int[] ICONS={11802,11235,4675,2434,6570,4720,1050,1515,1275};
    private static final Color GOLD=new Color(255,152,31),EDGE=new Color(101,87,65),BG=new Color(62,53,41),PAPER=new Color(73,62,46);
    private static final int WIDTH=506, HEIGHT=324, GRID_X=127, GRID_Y=36, GRID_WIDTH=344, GRID_HEIGHT=252;
    @Override protected void startUp(){overlays.add(overlay);mouse.registerMouseListener(0,input);mouse.registerMouseWheelListener(0,input);keys.registerKeyListener(input);eventBus.subscribe(MenuEntryAdded.class,this,this::onMenuEntryAdded);eventBus.subscribe(MenuOpened.class,this,this::onMenuOpened);eventBus.subscribe(MenuOptionClicked.class,this,this::onMenuOptionClicked);}
    @Override protected void shutDown(){overlays.remove(overlay);mouse.unregisterMouseListener(input);mouse.unregisterMouseWheelListener(input);keys.unregisterKeyListener(input);eventBus.unregister(this);panel=new Rectangle();}
    void command(String text){clientThread.invokeLater(()->LletyaCommands.send(text));}
    void closeNativeShop(){LletyaCommands.closeMarketWidgets();}
    private void request(int category,boolean pvp){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s==null)return;scroll=0;pendingJump=query.isEmpty()?category:-1;
        String encoded=query.isEmpty()?"~":Base64.getUrlEncoder().withoutPadding().encodeToString(query.getBytes(StandardCharsets.UTF_8));
        command("pvpmarket filter "+s.token+" "+category+" "+(pvp?1:0)+" "+encoded);}
    private void buy(int id,int amount){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s!=null)command("pvpmarket buy "+s.token+" "+id+" "+amount);}
    private void itemAction(String action,int id){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s!=null)command("pvpmarket "+action+" "+s.token+" "+id);}
    private void jumpTo(int category){int offset=layout.headerOffset(category);if(offset>=0){scroll=Math.min(offset,layout.maximumScroll(GRID_HEIGHT));selectedCategory=category;}}
    private void scrollTo(int value){scroll=Math.max(0,Math.min(value,layout.maximumScroll(GRID_HEIGHT)));selectedCategory=Math.max(0,layout.categoryAt(scroll));}
    private MarketScrollBar scrollBar(){return new MarketScrollBar(panel.x+481,panel.y+GRID_Y,GRID_HEIGHT,layout.height,scroll);}
    private void close(){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s!=null){
        command("pvpmarket close "+s.token);
        clientThread.invokeLater(()->{
            LletyaMarketState.Snapshot current=LletyaMarketState.get();
            if(current==null||!s.token.equals(current.token))return;
            // Detach the native windows before removing their covering overlay.
            closeNativeShop();LletyaMarketState.dismiss(s.token);panel=new Rectangle();
        });}
        searching=false;dropdown=false;activeMenuItem=-1;activeMenuToken=null;}
    public static String price(long value){if(value>=1_000_000_000L)return compact(value,1_000_000_000L,"B");if(value>=1_000_000L)return compact(value,1_000_000L,"M");if(value>=1_000L)return compact(value,1_000L,"K");return Long.toString(value);}
    private static String compact(long value,long unit,String suffix){return String.format(java.util.Locale.ROOT,"%.1f",value/(double)unit).replaceAll("\\.0$","")+suffix;}
    private int hoveredItemIndex(int canvasX,int canvasY){if(!panel.contains(canvasX,canvasY))return -1;return layout.itemAt(canvasX-panel.x-GRID_X,canvasY-panel.y-GRID_Y+scroll);}
    private void onMenuEntryAdded(MenuEntryAdded event){
        updateHoverTarget(event,client.getMouseCanvasPosition().getX(),client.getMouseCanvasPosition().getY());
    }
    void updateHoverTarget(MenuEntryAdded event,int canvasX,int canvasY){
        if(!(event.getOption().contains("Value")||event.getOption().contains("Buy")))return;
        LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s==null)return;
        int index=hoveredItemIndex(canvasX,canvasY);
        if(index<0||index>=s.items.size())return;
        event.setTarget(s.items.get(index).name);event.setModified();
    }
    void onMenuOpened(MenuOpened event){
        if(client==null)return;
        populateNativeMenu(event,client.getMouseCanvasPosition().getX(),client.getMouseCanvasPosition().getY());
    }
    void populateNativeMenu(MenuOpened event,int mouseX,int mouseY){
        LletyaMarketState.Snapshot s=LletyaMarketState.get();
        if(s==null)return;
        int index=hoveredItemIndex(mouseX,mouseY);
        if(index<0||index>=s.items.size())return;
        LletyaMarketState.Item item=s.items.get(index);int opcode=MenuOpcode.RUNELITE.getId();
        event.setMenuEntries(new MenuEntry[]{
            new MenuEntry("Cancel","",0,MenuOpcode.CANCEL.getId(),0,0,false),
            new MenuEntry("Examine",item.name,item.id,MenuOpcode.EXAMINE_ITEM.getId(),0,0,false),
            new MenuEntry("Buy X",item.name,item.id,opcode,0,0,false),
            new MenuEntry("Buy 10",item.name,item.id,opcode,10,0,false),
            new MenuEntry("Buy 5",item.name,item.id,opcode,5,0,false),
            new MenuEntry("Buy 1",item.name,item.id,opcode,1,0,false),
            new MenuEntry("Value",item.name,item.id,opcode,0,0,false)
        });
        event.setModified();activeMenuToken=s.token;activeMenuItem=item.id;
    }
    private void onMenuOptionClicked(MenuOptionClicked event){
        if(event.getOpcode()!=MenuOpcode.RUNELITE.getId())return;
        LletyaMarketState.Snapshot s=LletyaMarketState.get();
        if(s==null||event.getIdentifier()!=activeMenuItem||activeMenuToken==null||!activeMenuToken.equals(s.token)
            ||!event.getTarget().equals(marketItemName(activeMenuItem)))return;
        switch(event.getOption()){
            case "Value":itemAction("value",activeMenuItem);break;
            case "Buy 1":buy(activeMenuItem,1);break;
            case "Buy 5":buy(activeMenuItem,5);break;
            case "Buy 10":buy(activeMenuItem,10);break;
            case "Buy X":itemAction("buyx",activeMenuItem);break;
            default:return;
        }
        event.consume();activeMenuItem=-1;activeMenuToken=null;
    }
    private String marketItemName(int id){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s==null)return "";for(LletyaMarketState.Item item:s.items)if(item.id==id)return item.name;return "";}
    private static void text(Graphics2D g,String value,int x,int y,Color color){g.setColor(Color.BLACK);g.drawString(value,x+1,y+1);g.setColor(color);g.drawString(value,x,y);}
    private static void center(Graphics2D g,String value,int x,int y,Color c){text(g,value,x-g.getFontMetrics().stringWidth(value)/2,y,c);}
    private static void box(Graphics2D g,int x,int y,int w,int h,Color fill){g.setColor(fill);g.fillRect(x,y,w,h);g.setColor(EDGE.brighter());g.drawLine(x,y,x+w-1,y);g.drawLine(x,y,x,y+h-1);g.setColor(new Color(24,23,18));g.drawLine(x,y+h-1,x+w-1,y+h-1);g.drawLine(x+w-1,y,x+w-1,y+h-1);}
    private static void frame(Graphics2D g,int x,int y,int w,int h,Color fill){
        g.setColor(new Color(20,19,15));g.fillRect(x,y,w,h);
        box(g,x+1,y+1,w-2,h-2,new Color(94,79,57));
        box(g,x+2,y+2,w-4,h-4,new Color(37,30,23));
        box(g,x+3,y+3,w-6,h-6,fill);
    }
    private static void squareButton(Graphics2D g,int x,int y,int size){
        g.setColor(new Color(18,17,13));g.fillRect(x,y,size,size);
        box(g,x+1,y+1,size-2,size-2,new Color(116,108,83));
        box(g,x+3,y+3,size-6,size-6,new Color(75,69,50));
    }
    private static void arrow(Graphics2D g,int x,int y,boolean down){
        g.setColor(new Color(20,19,14));
        g.fillPolygon(new int[]{x-4,x,x+4},down?new int[]{y-2,y+3,y-2}:new int[]{y+2,y-3,y+2},3);
    }
    private void drawScrollBar(Graphics2D g){
        MarketScrollBar bar=scrollBar();int x=bar.thumb.x,y=panel.y+GRID_Y;
        g.setColor(new Color(2301979));g.fillRect(x,y,16,GRID_HEIGHT);
        java.awt.image.BufferedImage up=sprites.getSprite(SpriteID.SCROLLBAR_ARROW_UP,0),down=sprites.getSprite(SpriteID.SCROLLBAR_ARROW_DOWN,0);
        if(up!=null)g.drawImage(up,x,y,16,16,null);else arrow(g,x+8,y+8,false);
        if(down!=null)g.drawImage(down,x,y+GRID_HEIGHT-16,16,16,null);else arrow(g,x+8,y+GRID_HEIGHT-8,true);
        Rectangle t=bar.thumb;g.setColor(new Color(5063219));g.fillRect(t.x,t.y,t.width,t.height);
        g.setColor(new Color(7759444));g.fillRect(t.x,t.y,2,t.height);g.fillRect(t.x,t.y,16,2);
        g.setColor(new Color(3353893));g.fillRect(t.x+14,t.y,2,t.height);g.fillRect(t.x,t.y+t.height-2,16,2);
    }
    private class MarketOverlay extends Overlay {
        MarketOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);}
        @Override public Dimension render(Graphics2D g){
            panel=new Rectangle();LletyaMarketState.Snapshot s=LletyaMarketState.get();
            if(client.getGameState()!=GameState.LOGGED_IN){LletyaMarketState.clear();return null;}
            Widget shop=client.getWidget(300,0);
            if(s==null||shop==null||shop.isHidden())return null;
            if(!s.hasMarketSelector)dropdown=false;
            if(!s.token.equals(lastToken)){lastToken=s.token;query=s.query==null?"":s.query;scroll=0;searching=false;activeMenuItem=-1;selectedCategory=0;pendingJump=-1;}
            String view=s.pvpOnly+":"+s.query;if(!view.equals(lastView)){lastView=view;scroll=0;activeMenuItem=-1;}
            if(lastSnapshot!=s){lastSnapshot=s;layout=new MarketLayout(s.items,s.query==null||s.query.isEmpty());if(pendingJump>=0){jumpTo(pendingJump);pendingJump=-1;}}
            scroll=Math.max(0,Math.min(scroll,layout.maximumScroll(GRID_HEIGHT)));
            Widget viewport=client.getViewportWidget();Rectangle v=viewport==null?new Rectangle(0,0,512,334):viewport.getBounds();
            int x=v.x+Math.max(0,(v.width-WIDTH)/2),y=v.y+Math.max(0,(v.height-HEIGHT)/2);panel=new Rectangle(x,y,WIDTH,HEIGHT);
            g.setFont(FontManager.getRunescapeSmallFont());
            frame(g,x,y,WIDTH,HEIGHT,BG);
            java.awt.image.BufferedImage background=sprites.getSprite(SpriteID.DIALOG_BACKGROUND,0);
            if(background!=null){Paint old=g.getPaint();g.setPaint(new TexturePaint(background,new Rectangle(x+4,y+4,background.getWidth(),background.getHeight())));g.fillRect(x+4,y+4,WIDTH-8,HEIGHT-8);g.setPaint(old);}
            g.setColor(new Color(31,25,19));g.drawLine(x+5,y+31,x+WIDTH-6,y+31);
            if(s.hasMarketSelector){
                box(g,x+8,y+7,141,19,new Color(47,45,35));text(g,s.pvpOnly?"PvP Equipment":"General Market",x+12,y+21,GOLD);
                squareButton(g,x+130,y+8,16);arrow(g,x+138,y+15,true);
            }
            g.setFont(FontManager.getRunescapeBoldFont());
            center(g,s.pvpOnly?"PvP Equipment":"General Market",x+WIDTH/2,y+22,GOLD);
            java.awt.image.BufferedImage close=sprites.getSprite(SpriteID.WINDOW_CLOSE_BUTTON,0);
            if(close!=null)g.drawImage(close,x+478,y+6,23,23,null);else{squareButton(g,x+479,y+6,21);center(g,"X",x+489,y+22,GOLD);}
            g.setFont(FontManager.getRunescapeFont());
            for(int i=0;i<CATEGORIES.length;i++){
                int cy=y+36+i*27;boolean selected=i==selectedCategory;
                box(g,x+8,cy,115,27,selected?new Color(91,70,43):BG);
                text(g,CATEGORIES[i],x+13,cy+18,GOLD);g.drawImage(items.getImage(ICONS[i]),x+98,cy+2,22,22,null);
            }
            box(g,x+8,y+284,115,32,searching?new Color(94,80,56):BG);
            String searchLabel=searching?"> "+query+(System.currentTimeMillis()/500%2==0?"|":""):query.isEmpty()?"Search":query;
            while(g.getFontMetrics().stringWidth(searchLabel)>98&&searchLabel.length()>4)searchLabel="..."+searchLabel.substring(4);
            if(query.isEmpty()&&!searching){g.setColor(new Color(151,138,106));g.fillOval(x+36,y+294,10,10);g.setColor(new Color(35,31,23));g.drawOval(x+36,y+294,10,10);g.drawLine(x+44,y+303,x+49,y+308);text(g,"Search",x+54,y+306,GOLD);}
            else text(g,searchLabel,x+14,y+306,GOLD);
            box(g,x+126,y+35,347,254,new Color(50,42,32));
            Rectangle grid=new Rectangle(x+GRID_X,y+GRID_Y,GRID_WIDTH,GRID_HEIGHT);
            Shape clip=g.getClip();g.clip(grid);
            for(MarketLayout.Section section:layout.sections){int hy=grid.y+section.y-scroll;
                if(hy+24>=grid.y&&hy<grid.y+grid.height){box(g,grid.x,hy,GRID_WIDTH,24,PAPER);g.setFont(FontManager.getRunescapeBoldFont());center(g,CATEGORIES[section.category],grid.x+GRID_WIDTH/2,hy+18,GOLD);g.setFont(FontManager.getRunescapeSmallFont());}
                if(section.count==0)center(g,"No items in this category.",x+297,hy+42,new Color(171,163,141));
            }
            g.setFont(FontManager.getRunescapeSmallFont());
            for(MarketLayout.Cell cell:layout.cells){
                int ix=grid.x+cell.bounds.x,iy=grid.y+cell.bounds.y-scroll;
                LletyaMarketState.Item item=s.items.get(cell.itemIndex);if(iy+43<grid.y||iy>grid.y+grid.height)continue;
                g.drawImage(items.getImage(item.id),ix+5,iy,32,32,null);if(item.price>0)center(g,price(item.price),ix+22,iy+40,new Color(255,236,55));
            }
            g.setClip(clip);
            drawScrollBar(g);
            text(g,s.pvpOnly?"Bounty Hunter points: "+java.math.BigDecimal.valueOf(s.bountyBalance).stripTrailingZeros().toPlainString():"Blood Money (incl. tokens): "+price(s.balance),x+130,y+303,GOLD);
            text(g,s.pvpOnly?"Prices in BH points | No buyback":"Buyback: 40%",x+130,y+316,new Color(201,188,145));
            if(s.items.isEmpty())center(g,"No items found.",x+296,y+162,new Color(190,181,152));
            if(dropdown){frame(g,x+8,y+28,141,46,BG);text(g,"General Market",x+14,y+46,GOLD);text(g,"PvP Equipment",x+14,y+66,GOLD);}
            return null;
        }
    }
    private class Input extends MouseAdapter implements MouseWheelListener,KeyListener {
        @Override public MouseEvent mousePressed(MouseEvent e){
            // The native menu owns selection and dismissal while it is open.
            if(client!=null&&client.isMenuOpen())return e;
            if(panel.isEmpty()||e.isAltDown()||e.getButton()==MouseEvent.BUTTON2)return e;
            LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s==null)return e;int x=e.getX()-panel.x,y=e.getY()-panel.y;
            if(!s.hasMarketSelector)dropdown=false;
            if(s.hasMarketSelector&&x>=8&&x<149&&y>=7&&y<28&&e.getButton()==MouseEvent.BUTTON1){dropdown=!dropdown;searching=false;e.consume();return e;}
            if(dropdown){dropdown=false;if(x>=8&&x<149&&y>=28&&y<74&&e.getButton()==MouseEvent.BUTTON1)request(0,y>=51);e.consume();return e;}
            if(!panel.contains(e.getPoint()))return e;
            if(x>=479&&y<29)close();
            else if(x>=8&&x<123&&y>=36&&y<279){searching=false;int category=Math.min(8,(y-36)/27);if(!query.isEmpty()){query="";request(category,s.pvpOnly);}else jumpTo(category);}
            else if(x<126&&y>=282)searching=true;
            else if(x>=481&&x<497&&y>=GRID_Y&&y<GRID_Y+GRID_HEIGHT){MarketScrollBar bar=scrollBar();
                if(y<GRID_Y+16)scrollTo(scroll-4);else if(y>=GRID_Y+GRID_HEIGHT-16)scrollTo(scroll+4);
                else if(bar.thumb.contains(e.getPoint())){draggingScroll=true;scrollGrabOffset=e.getY()-bar.thumb.y;}
                else scrollTo(scroll+(e.getY()<bar.thumb.y?-GRID_HEIGHT:GRID_HEIGHT));}
            else if(x>=GRID_X&&x<GRID_X+GRID_WIDTH&&y>=GRID_Y&&y<GRID_Y+GRID_HEIGHT){int index=layout.itemAt(x-GRID_X,y-GRID_Y+scroll);
                if(index>=0&&index<s.items.size()){
                    if(e.getButton()==MouseEvent.BUTTON3)return e;
                    else if(e.getButton()==MouseEvent.BUTTON1)itemAction("value",s.items.get(index).id);
                }
            }
            e.consume();return e;
        }
        @Override public MouseEvent mouseDragged(MouseEvent e){if(draggingScroll){scrollTo(scrollBar().dragTo(e.getY(),scrollGrabOffset));e.consume();}return e;}
        @Override public MouseEvent mouseReleased(MouseEvent e){draggingScroll=false;return e;}
        @Override public MouseEvent mouseClicked(MouseEvent e){if(client!=null&&client.isMenuOpen())return e;if(panel.contains(e.getPoint())&&e.getButton()==MouseEvent.BUTTON1&&!e.isAltDown())e.consume();return e;}
        @Override public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e){if(panel.contains(e.getPoint())){dropdown=false;scrollTo(scroll+e.getWheelRotation()*45);e.consume();}return e;}
        @Override public void keyPressed(KeyEvent e){if(panel.isEmpty())return;
            if(!searching&&client!=null&&client.getVar(VarClientInt.INPUT_TYPE)!=0)return;
            if(e.getKeyCode()==KeyEvent.VK_ESCAPE){if(client!=null&&client.isMenuOpen())return;if(dropdown)dropdown=false;else close();e.consume();return;}
            if(!searching)return;
            if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE&&!query.isEmpty()){query=query.substring(0,query.length()-1);e.consume();}
            if(e.getKeyCode()==KeyEvent.VK_ENTER){LletyaMarketState.Snapshot s=LletyaMarketState.get();if(s!=null)request(selectedCategory,s.pvpOnly);searching=false;e.consume();}
        }
        @Override public void keyTyped(KeyEvent e){if(!searching||panel.isEmpty())return;char c=e.getKeyChar();if(c>=32&&c<127&&query.length()<60)query+=c;e.consume();}
        @Override public void keyReleased(KeyEvent e){if(searching&&!panel.isEmpty())e.consume();}
    }
}
