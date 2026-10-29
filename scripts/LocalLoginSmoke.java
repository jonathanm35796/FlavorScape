import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/** Integration probe for the selected revision-184 client; uses a disposable local DEV account. */
public final class LocalLoginSmoke {
    private static final String PREFIX = "net.runelite.standalone.";
    private static long deadline;
    private static long loggedInSince;
    private static long loginReadySince;
    private static int lastState = -1;
    private static int firstTick;
    private static boolean submitted;
    private static boolean loginReported;
    private static int commandStage;
    private static long commandSent;
    private static int homeStage;
    private static int gearStage;
    private static int questStage;
    private static int cameraStage;
    private static int autoCloseStage;
    private static int marketStage, marketItem, marketPrice, marketClickX, marketClickY;
    private static long marketBalance;
    private static String marketItemName;
    private static int spellbookStage;
    private static Object marketPlugin;
    private static boolean marketControlsVerified;
    private static int marketItemCount;
    private static String savedInventory;
    private static Object gearPlugin;
    private static int customSlot;

    private static void verifyMouseRelease(Class<?> api, Object client, java.awt.Rectangle area) throws Exception {
        java.awt.Canvas canvas = (java.awt.Canvas) api.getMethod("getCanvas").invoke(client);
        java.awt.event.MouseListener handler = (java.awt.event.MouseListener) field("MouseHandler", "MouseHandler_instance").get(null);
        Field held = field("MouseHandler", "MouseHandler_currentButtonVolatile");
        for (int button : new int[]{java.awt.event.MouseEvent.BUTTON1, java.awt.event.MouseEvent.BUTTON2, java.awt.event.MouseEvent.BUTTON3}) {
            handler.mousePressed(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 20, 20, 1, false, button));
            int expected = button == java.awt.event.MouseEvent.BUTTON2 ? 4 : button == java.awt.event.MouseEvent.BUTTON3 ? 2 : 1;
            if (held.getInt(null) != expected) throw new IllegalStateException("Native press did not register: " + button);
            handler.mouseReleased(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, area.x + 20, area.y + 120, 1, false, button));
            if (held.getInt(null) != 0) throw new IllegalStateException("Mouse button stuck after release over interface: " + button);
        }
        for (int modifiers : new int[]{0, java.awt.event.InputEvent.ALT_DOWN_MASK}) {
            int button = modifiers == 0 ? java.awt.event.MouseEvent.BUTTON2 : java.awt.event.MouseEvent.BUTTON1;
            handler.mousePressed(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), modifiers, area.x + 20, area.y + 120, 1, false, button));
            if (held.getInt(null) != 4) throw new IllegalStateException("Camera gesture was blocked over interface");
            handler.mouseReleased(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, area.x + 20, area.y + 120, 1, false, button));
            if (held.getInt(null) != 0) throw new IllegalStateException("Camera gesture remained held after release");
        }
    }

    private static void clickGearButton(Object injector, Method instance, Class<?> api, Object client) throws Exception {
        if (gearPlugin == null) {
            Object manager = instance.invoke(injector, Class.forName("net.runelite.client.plugins.PluginManager"));
            for (Object plugin : (java.util.Collection<?>) manager.getClass().getMethod("getPlugins").invoke(manager))
                if (plugin.getClass().getName().endsWith("LlyetaGearsPlugin")) gearPlugin = plugin;
        }
        if (gearPlugin == null) throw new IllegalStateException("Gears plugin missing");
        Field toggle = gearPlugin.getClass().getDeclaredField("toggle"); toggle.setAccessible(true);
        java.awt.Rectangle bounds = (java.awt.Rectangle) toggle.get(gearPlugin);
        java.awt.Canvas canvas = (java.awt.Canvas) api.getMethod("getCanvas").invoke(client);
        java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2, 1, false, java.awt.event.MouseEvent.BUTTON1);
        Object mouse = instance.invoke(injector, Class.forName("net.runelite.client.input.MouseManager"));
        mouse.getClass().getMethod("processMousePressed", java.awt.event.MouseEvent.class).invoke(mouse, click);
        if (!click.isConsumed()) throw new IllegalStateException("Gear button click was not consumed");
        commandSent = System.currentTimeMillis();
    }

    private static String inventorySnapshot(Class<?> api, Object client) throws Exception {
        Class<?> ids = Class.forName("net.runelite.api.InventoryID");
        Object container = api.getMethod("getItemContainer", ids).invoke(client, ids.getField("INVENTORY").get(null));
        Object[] items = (Object[]) Class.forName("net.runelite.api.ItemContainer").getMethod("getItems").invoke(container);
        StringBuilder result = new StringBuilder();
        for (Object item : items) result.append(item.getClass().getMethod("getId").invoke(item)).append(':').append(item.getClass().getMethod("getQuantity").invoke(item)).append(',');
        return result.toString();
    }
    private static volatile boolean frameSaved;

    private static void captureFrame(Object drawManager, String path) throws Exception {
        frameSaved=false;
        java.util.function.Consumer<java.awt.Image> capture=image->{
            try {
                java.awt.image.BufferedImage frame=new java.awt.image.BufferedImage(image.getWidth(null),image.getHeight(null),java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics g=frame.getGraphics();g.drawImage(image,0,0,null);g.dispose();
                java.io.File file=new java.io.File(path);file.getParentFile().mkdirs();javax.imageio.ImageIO.write(frame,"png",file);frameSaved=true;
            }catch(Throwable failure){frameError=failure;}
        };
        drawManager.getClass().getMethod("requestNextFrameListener",java.util.function.Consumer.class).invoke(drawManager,capture);
    }

    private static void marketClick(Object injector, Method instance, Class<?> api, Object client, int x, int y) throws Exception {
        java.awt.Canvas canvas=(java.awt.Canvas)api.getMethod("getCanvas").invoke(client);
        Object mouse=instance.invoke(injector,Class.forName("net.runelite.client.input.MouseManager"));
        java.awt.event.MouseEvent event=new java.awt.event.MouseEvent(canvas,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,java.awt.event.MouseEvent.BUTTON1);
        mouse.getClass().getMethod("processMousePressed",java.awt.event.MouseEvent.class).invoke(mouse,event);
        if(!event.isConsumed())throw new IllegalStateException("Market did not consume click");
        commandSent=System.currentTimeMillis();
    }
    private static void marketBuyMenu(Object injector,Method instance,Class<?> api,Object client,java.awt.Rectangle bounds,int row) throws Exception {
        java.awt.Canvas canvas=(java.awt.Canvas)api.getMethod("getCanvas").invoke(client);
        int x=bounds.x+marketClickX,y=bounds.y+marketClickY;
        java.awt.event.MouseListener nativeMouse=(java.awt.event.MouseListener)field("MouseHandler","MouseHandler_instance").get(null);
        nativeMouse.mousePressed(new java.awt.event.MouseEvent(canvas,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,java.awt.event.MouseEvent.BUTTON3));
        nativeMouse.mouseReleased(new java.awt.event.MouseEvent(canvas,java.awt.event.MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,x,y,1,false,java.awt.event.MouseEvent.BUTTON3));
        Object[] entries=(Object[])api.getMethod("getMenuEntries").invoke(client);
        String[] options={"Buy 1","Buy 5","Buy 10","Buy X"};
        Object selected=null;int buyRows=0;
        for(Object entry:entries){
            String option=(String)entry.getClass().getMethod("getOption").invoke(entry);
            if(option.startsWith("Buy ")){
                buyRows++;
                String target=(String)entry.getClass().getMethod("getTarget").invoke(entry);
                if(!marketItemName.equals(target))throw new IllegalStateException("Native menu target mismatch: expected "+marketItemName+" but saw "+target);
                if(options[row].equals(option))selected=entry;
            }
        }
        if(buyRows!=4||selected==null)throw new IllegalStateException("Native shop menu did not contain Buy 1/5/10/X options");
        int param0=(Integer)selected.getClass().getMethod("getParam0").invoke(selected);
        int param1=(Integer)selected.getClass().getMethod("getParam1").invoke(selected);
        int opcode=(Integer)selected.getClass().getMethod("getOpcode").invoke(selected);
        int identifier=(Integer)selected.getClass().getMethod("getIdentifier").invoke(selected);
        String target=(String)selected.getClass().getMethod("getTarget").invoke(selected);
        api.getMethod("invokeMenuAction",int.class,int.class,int.class,int.class,String.class,String.class,int.class,int.class)
                .invoke(client,param0,param1,opcode,identifier,options[row],target,x,y);
        commandSent=System.currentTimeMillis();
    }
    private static int inventoryCount(Class<?> api,Object client,int id) throws Exception {
        Class<?> ids=Class.forName("net.runelite.api.InventoryID");
        Object container=api.getMethod("getItemContainer",ids).invoke(client,ids.getField("INVENTORY").get(null));
        int count=0;for(Object item:(Object[])Class.forName("net.runelite.api.ItemContainer").getMethod("getItems").invoke(container))
            if((Integer)item.getClass().getMethod("getId").invoke(item)==id)count+=(Integer)item.getClass().getMethod("getQuantity").invoke(item);
        return count;
    }
    private static boolean chatContains(Class<?> api,Object client,String text) throws Exception {
        java.util.Map<?,?> chats=(java.util.Map<?,?>)api.getMethod("getChatLineMap").invoke(client);
        for(Object buffer:chats.values())for(Object line:(Object[])Class.forName("net.runelite.api.ChatLineBuffer").getMethod("getLines").invoke(buffer))
            if(line!=null&&String.valueOf(Class.forName("net.runelite.api.MessageNode").getMethod("getValue").invoke(line)).contains(text))return true;
        return false;
    }
    private static volatile Throwable frameError;

    private static String commandMenu(Class<?> clientApi, Object client) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int child = 2; child < 38; child++) {
            Object widget = clientApi.getMethod("getWidget", int.class, int.class).invoke(client, 119, child);
            if (widget != null) text.append(Class.forName("net.runelite.api.widgets.Widget").getMethod("getText").invoke(widget)).append('\n');
        }
        return text.toString();
    }

    private static void sendCommand(String command) throws Exception {
        call("WorldMapSection0", "method3906", String.class, command);
        commandSent = System.currentTimeMillis();
    }

    private static Field field(String type, String name) throws Exception {
        Field field = Class.forName(PREFIX + type).getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object call(String type, String name, Class<?> argumentType, Object argument) throws Exception {
        Method method = Class.forName(PREFIX + type).getDeclaredMethod(name, argumentType);
        method.setAccessible(true);
        return method.invoke(null, argument);
    }

    public static void main(String[] args) throws Exception {
        Object world = field("CustomMain", "worldType").get(null);
        for (String method : new String[]{"getGameServerAddress", "getFileServerAddress"}) {
            if (!"127.0.0.1".equals(world.getClass().getMethod(method).invoke(world))) {
                throw new IllegalStateException("Smoke login is restricted to 127.0.0.1");
            }
        }
        Class<?> runeLite = Class.forName("net.runelite.client.RuneLite");
        runeLite.getMethod("main", String[].class).invoke(null, (Object) args);
        Object injector = runeLite.getMethod("getInjector").invoke(null);
        Method instance = Class.forName("com.google.inject.Injector").getMethod("getInstance", Class.class);
        Object clientThread = instance.invoke(injector, Class.forName("net.runelite.client.callback.ClientThread"));
        Class<?> clientApi = Class.forName("net.runelite.api.Client");
        Object client = instance.invoke(injector, clientApi);
        Object drawManager = instance.invoke(injector, Class.forName("net.runelite.client.ui.DrawManager"));
        deadline = System.currentTimeMillis() + 180_000;
        BooleanSupplier probe = () -> {
            try {
                if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Local smoke probe timed out");
                int state = field("Client", "gameState").getInt(null);
                if (state != lastState) {
                    System.out.println("LOCAL_SMOKE state=" + state + " loadingStage=" + field("Client", "titleLoadingStage").getInt(null));
                    lastState = state;
                    if (state == 10) loginReadySince = System.currentTimeMillis();
                }
                if (state == 10 && !submitted && System.currentTimeMillis() - loginReadySince >= 2000) {
                    String testName = System.getenv("LLYETA_SMOKE_USERNAME");
                    field("Login", "Login_username").set(null, testName == null ? "Localtest" : testName);
                    field("Login", "Login_password").set(null, "local-only-test");
                    call("PlayerType", "method3947", boolean.class, false);
                    call("MouseRecorder", "method1207", int.class, 20);
                    submitted = true;
                    System.out.println("LOCAL_SMOKE submitted disposable localhost DEV login");
                }
                if (state == 30) {
                    int ticks = (Integer) clientApi.getMethod("getTickCount").invoke(client);
                    if (loggedInSince == 0) { loggedInSince = System.currentTimeMillis(); firstTick = ticks; }
                    if (System.currentTimeMillis() - loggedInSince >= 10_000) {
                        Object player = clientApi.getMethod("getLocalPlayer").invoke(client);
                        if (player == null || ticks <= firstTick) throw new IllegalStateException("No active player/ticks");
                        Object location = Class.forName("net.runelite.api.Actor").getMethod("getWorldLocation").invoke(player);
                        if (!loginReported) {
                            System.out.println("LOCAL_SMOKE PASS logged-in player at " + location + "; received ticks=" + (ticks - firstTick));
                            loginReported = true;
                        }
                        String homeCheck = System.getenv("LLYETA_VERIFY_HOME");
                        if ("1".equals(System.getenv("LLYETA_SMOKE_SAVE_PLAYERS"))) {
                            sendCommand("save");
                            sendCommand("update 1");
                            System.out.println("LOCAL_SAVE requested save and one-minute safe logout before local server restart");
                            return true;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_SPELLBOOKS"))) {
                            int[] varps = (int[]) clientApi.getMethod("getVarps").invoke(client);
                            int classic = (Integer) clientApi.getMethod("getVarbitValue", int[].class, int.class).invoke(client, varps, 6718);
                            if (classic != 1) throw new IllegalStateException("Login did not select the classic spellbook grid");
                            if (spellbookStage == 0) {
                                sendCommand("vb 4070 0");
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 6});
                                spellbookStage = 1; return false;
                            }
                            if (System.currentTimeMillis() - commandSent < 2000) return false;
                            int book = (spellbookStage - 1) / 2;
                            String[] names = {"standard", "ancient", "lunar"};
                            if (spellbookStage % 2 == 1) {
                                int actualBook = (Integer) clientApi.getMethod("getVarbitValue", int[].class, int.class).invoke(client, varps, 4070);
                                if (actualBook != book) throw new IllegalStateException("Spellbook switch was not acknowledged");
                                Class<?> widgetApi = Class.forName("net.runelite.api.widgets.Widget");
                                java.util.Set<Integer> columns = new java.util.TreeSet<>();
                                int visible = 0;
                                for (int child = 0; child < 184; child++) {
                                    Object widget = clientApi.getMethod("getWidget", int.class, int.class).invoke(client, 218, child);
                                    if (widget == null || (Boolean) widgetApi.getMethod("isHidden").invoke(widget)) continue;
                                    String name = (String) widgetApi.getMethod("getName").invoke(widget);
                                    if (name == null || !name.contains("<col=00ff00>")) continue;
                                    columns.add((Integer) widgetApi.getMethod("getRelativeX").invoke(widget));
                                    visible++;
                                }
                                System.out.println("SPELLBOOK_SMOKE " + names[book] + " visible=" + visible + " columns=" + columns);
                                if (columns.size() != new int[]{7, 4, 6}[book]) throw new IllegalStateException("Unexpected spell grid for " + names[book]);
                                if (visible != new int[]{70, 26, 45}[book]) throw new IllegalStateException("Spells missing from " + names[book]);
                                String directory = System.getenv("LLYETA_SPELLBOOK_SCREENSHOTS");
                                if (directory == null) throw new IllegalStateException("Set LLYETA_SPELLBOOK_SCREENSHOTS to a workspace directory");
                                captureFrame(drawManager, directory + "/" + names[book] + ".png");
                                spellbookStage++; return false;
                            }
                            if (frameError != null) throw new IllegalStateException("Spellbook frame capture failed", frameError);
                            if (!frameSaved) return false;
                            if (book == 2) {
                                System.out.println("SPELLBOOK_SMOKE PASS classic login and all three rendered grids via server book switches");
                                return true;
                            }
                            sendCommand("vb 4070 " + (book + 1));
                            spellbookStage++; return false;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_MARKET"))) {
                            Class<?> bridge=Class.forName("net.runelite.standalone.LlyetaMarketState");
                            Object snapshot=bridge.getMethod("get").invoke(null);
                            if(marketStage==0){
                                field("Client","camAngleX").setInt(null,383);field("Client","camAngleY").setInt(null,0);
                                sendCommand("empty");marketStage=-2;return false;
                            }
                            if(System.currentTimeMillis()-commandSent<2000)return false;
                            if(marketStage==-2){
                                Object warning=clientApi.getMethod("getWidget",int.class,int.class).invoke(client,229,2);
                                if(warning!=null&&!(Boolean)Class.forName("net.runelite.api.widgets.Widget").getMethod("isHidden").invoke(warning)){
                                    clientApi.getMethod("invokeMenuAction",int.class,int.class,int.class,int.class,String.class,String.class,int.class,int.class).invoke(client,-1,(229<<16)|2,30,0,"Continue","",0,0);
                                    commandSent=System.currentTimeMillis();marketStage=-1;return false;
                                }
                                marketStage=-3;return false;
                            }
                            if(marketStage==-1){
                                clientApi.getMethod("invokeMenuAction",int.class,int.class,int.class,int.class,String.class,String.class,int.class,int.class).invoke(client,1,(219<<16)|1,57,1,"Continue","",0,0);
                                commandSent=System.currentTimeMillis();marketStage=-3;return false;
                            }
                            if(marketStage==-3){
                                String inventory=inventorySnapshot(clientApi,client);
                                int free=0;for(String slot:inventory.split(","))if(slot.startsWith("-1:"))free++;
                                if(free<23)throw new IllegalStateException("Disposable market inventory needs 23 free slots: "+inventory);
                                sendCommand("item 13307 1000000000");sendCommand("tele 3094 3507 0");marketStage=1;return false;
                            }
                            if(marketStage==1){
                                java.util.List<?> npcs=(java.util.List<?>)clientApi.getMethod("getNpcs").invoke(client);
                                Object merchant=null;
                                for(Object npc:npcs)if((Integer)Class.forName("net.runelite.api.NPC").getMethod("getId").invoke(npc)==9001)merchant=npc;
                                if(merchant==null)throw new IllegalStateException("PvP shop NPC not spawned");
                                int index=(Integer)Class.forName("net.runelite.api.NPC").getMethod("getIndex").invoke(merchant);
                                clientApi.getMethod("invokeMenuAction",int.class,int.class,int.class,int.class,String.class,String.class,int.class,int.class)
                                    .invoke(client,0,0,9,index,"Trade","PvP shop",0,0);
                                commandSent=System.currentTimeMillis();marketStage=2;return false;
                            }
                            if(marketStage==2&&snapshot==null)return false;
                            if(marketPlugin==null){
                                Object manager=instance.invoke(injector,Class.forName("net.runelite.client.plugins.PluginManager"));
                                for(Object plugin:(java.util.Collection<?>)manager.getClass().getMethod("getPlugins").invoke(manager))
                                    if(plugin.getClass().getName().endsWith("LlyetaMarketPlugin"))marketPlugin=plugin;
                                if(marketPlugin==null)throw new IllegalStateException("Market plugin not loaded");
                            }
                            Field boundsField=marketPlugin.getClass().getDeclaredField("panel");boundsField.setAccessible(true);
                            java.awt.Rectangle bounds=(java.awt.Rectangle)boundsField.get(marketPlugin);
                            Field dropdownField=marketPlugin.getClass().getDeclaredField("dropdown");dropdownField.setAccessible(true);
                            if(marketStage==20){
                                if(!snapshot.getClass().getField("pvpOnly").getBoolean(snapshot))throw new IllegalStateException("PvP selector did not switch shops");
                                marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                marketClick(injector,instance,clientApi,client,bounds.x+40,bounds.y+40);marketStage=21;return false;
                            }
                            if(marketStage==21){
                                if(snapshot.getClass().getField("pvpOnly").getBoolean(snapshot))throw new IllegalStateException("General Market selector did not switch shops");
                                marketControlsVerified=true;marketStage=2;return false;
                            }
                            if(marketStage==2){
                                java.util.List<?> stock=(java.util.List<?>)snapshot.getClass().getField("items").get(snapshot);
                                if(stock.isEmpty())throw new IllegalStateException("Market opened without stock");
                                if(bounds.isEmpty())return false;
                                if(!marketControlsVerified){
                                    marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                    if(!dropdownField.getBoolean(marketPlugin))throw new IllegalStateException("Shop selector did not open");
                                    marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                    if(dropdownField.getBoolean(marketPlugin))throw new IllegalStateException("Shop selector did not toggle closed");
                                    marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                    Object keyManager=instance.invoke(injector,Class.forName("net.runelite.client.input.KeyManager"));
                                    java.awt.Canvas canvas=(java.awt.Canvas)clientApi.getMethod("getCanvas").invoke(client);
                                    keyManager.getClass().getMethod("processKeyPressed",java.awt.event.KeyEvent.class).invoke(keyManager,new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_ESCAPE,(char)27));
                                    if(dropdownField.getBoolean(marketPlugin)||bridge.getMethod("get").invoke(null)==null)throw new IllegalStateException("Escape failed to dismiss only the shop selector");
                                    marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                    marketClick(injector,instance,clientApi,client,bounds.x+bounds.width+10,bounds.y+100);
                                    if(dropdownField.getBoolean(marketPlugin))throw new IllegalStateException("Outside click did not dismiss shop selector");
                                    marketClick(injector,instance,clientApi,client,bounds.x+138,bounds.y+15);
                                    marketClick(injector,instance,clientApi,client,bounds.x+40,bounds.y+60);marketStage=20;return false;
                                }
                                Field layoutField=marketPlugin.getClass().getDeclaredField("layout");layoutField.setAccessible(true);
                                Object layout=layoutField.get(marketPlugin);
                                Object mouseManager=instance.invoke(injector,Class.forName("net.runelite.client.input.MouseManager"));
                                java.awt.Canvas scrollCanvas=(java.awt.Canvas)clientApi.getMethod("getCanvas").invoke(client);
                                Field scrollField=marketPlugin.getClass().getDeclaredField("scroll");scrollField.setAccessible(true);
                                mouseManager.getClass().getMethod("processMouseWheelMoved",java.awt.event.MouseWheelEvent.class).invoke(mouseManager,new java.awt.event.MouseWheelEvent(scrollCanvas,java.awt.event.MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,bounds.x+300,bounds.y+150,0,false,java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,3,1));
                                int scrolled=scrollField.getInt(marketPlugin);
                                if(scrolled<=0)throw new IllegalStateException("Market wheel did not scroll");
                                Method scrollbarMethod=marketPlugin.getClass().getDeclaredMethod("scrollBar");scrollbarMethod.setAccessible(true);
                                Object scrollbar=scrollbarMethod.invoke(marketPlugin);
                                Field thumbField=scrollbar.getClass().getDeclaredField("thumb");thumbField.setAccessible(true);
                                java.awt.Rectangle thumb=(java.awt.Rectangle)thumbField.get(scrollbar);
                                marketClick(injector,instance,clientApi,client,thumb.x+8,thumb.y+thumb.height/2);
                                if(scrollField.getInt(marketPlugin)!=scrolled)throw new IllegalStateException("Grabbing the scrollbar thumb jumped the catalogue");
                                mouseManager.getClass().getMethod("processMouseDragged",java.awt.event.MouseEvent.class).invoke(mouseManager,new java.awt.event.MouseEvent(scrollCanvas,java.awt.event.MouseEvent.MOUSE_DRAGGED,System.currentTimeMillis(),java.awt.event.InputEvent.BUTTON1_DOWN_MASK,thumb.x+8,bounds.y+288+thumb.height/2,0,false,java.awt.event.MouseEvent.NOBUTTON));
                                mouseManager.getClass().getMethod("processMouseReleased",java.awt.event.MouseEvent.class).invoke(mouseManager,new java.awt.event.MouseEvent(scrollCanvas,java.awt.event.MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,thumb.x+8,bounds.y+288,1,false,java.awt.event.MouseEvent.BUTTON1));
                                int maximum=(Integer)layout.getClass().getMethod("maximumScroll",int.class).invoke(layout,252);
                                if(scrollField.getInt(marketPlugin)!=maximum)throw new IllegalStateException("Scrollbar drag did not reach the catalogue bottom");
                                mouseManager.getClass().getMethod("processMouseWheelMoved",java.awt.event.MouseWheelEvent.class).invoke(mouseManager,new java.awt.event.MouseWheelEvent(scrollCanvas,java.awt.event.MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,bounds.x+300,bounds.y+150,0,false,java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,3,-1000));
                                if(scrollField.getInt(marketPlugin)!=0)throw new IllegalStateException("Market scrolled above the first item");
                                java.util.List<?> cells=(java.util.List<?>)layout.getClass().getField("cells").get(layout);
                                if(cells.isEmpty())return false;
                                int selectedIndex=-1;
                                for(int i=0;i<stock.size();i++)if("Dragon platebody".equalsIgnoreCase((String)stock.get(i).getClass().getField("name").get(stock.get(i)))){selectedIndex=i;break;}
                                if(selectedIndex<0)throw new IllegalStateException("Dragon platebody was not present in the General Market catalogue");
                                Object cell=null;for(Object candidate:cells)if(candidate.getClass().getField("itemIndex").getInt(candidate)==selectedIndex){cell=candidate;break;}
                                if(cell==null)throw new IllegalStateException("Dragon platebody has no rendered market cell");
                                java.awt.Rectangle cellBounds=(java.awt.Rectangle)cell.getClass().getField("bounds").get(cell);
                                Object item=stock.get(selectedIndex);marketItem=item.getClass().getField("id").getInt(item);marketPrice=item.getClass().getField("price").getInt(item);marketItemName=(String)item.getClass().getField("name").get(item);
                                marketClickX=127+cellBounds.x+16;marketClickY=36+cellBounds.y+16;
                                marketBalance=snapshot.getClass().getField("balance").getLong(snapshot);
                                java.awt.Canvas marketCanvas=(java.awt.Canvas)clientApi.getMethod("getCanvas").invoke(client);
                                java.awt.event.MouseMotionListener nativeMouse=(java.awt.event.MouseMotionListener)field("MouseHandler","MouseHandler_instance").get(null);
                                nativeMouse.mouseMoved(new java.awt.event.MouseEvent(marketCanvas,java.awt.event.MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,bounds.x+marketClickX,bounds.y+marketClickY,0,false));
                                captureFrame(drawManager,System.getenv("LLYETA_MARKET_SCREENSHOT"));marketStage=3;return false;
                            }
                            if(marketStage==3){if(!frameSaved)return false;marketItemCount=inventoryCount(clientApi,client,marketItem);marketClick(injector,instance,clientApi,client,bounds.x+marketClickX,bounds.y+marketClickY);marketStage=30;return false;}
                            if(marketStage==30){
                                if(snapshot.getClass().getField("balance").getLong(snapshot)!=marketBalance||inventoryCount(clientApi,client,marketItem)!=marketItemCount)throw new IllegalStateException("Left-click bought an item instead of valuing it");
                                if(!chatContains(clientApi,client,String.format(java.util.Locale.ROOT,"%,d",marketPrice)+" Blood Money each"))throw new IllegalStateException("Left-click price was not shown in general chat");
                                marketBuyMenu(injector,instance,clientApi,client,bounds,0);marketStage=4;return false;
                            }
                            if(marketStage==4){
                                if(snapshot.getClass().getField("balance").getLong(snapshot)!=marketBalance-marketPrice)throw new IllegalStateException("Market purchase did not charge the displayed price");
                                String token=(String)snapshot.getClass().getField("token").get(snapshot);
                                sendCommand("pvpmarket sell "+token+" "+marketItem+" 1");marketStage=5;return false;
                            }
                            if(marketStage==5){
                                if(snapshot.getClass().getField("balance").getLong(snapshot)!=marketBalance-marketPrice+(long)marketPrice*40/100)throw new IllegalStateException("Market buyback was not 40%");
                                marketBalance=snapshot.getClass().getField("balance").getLong(snapshot);
                                marketBuyMenu(injector,instance,clientApi,client,bounds,1);marketStage=31;return false;
                            }
                            if(marketStage==31||marketStage==32||marketStage==34){
                                int amount=marketStage==31?5:marketStage==32?10:7;
                                if(snapshot.getClass().getField("balance").getLong(snapshot)!=marketBalance-(long)marketPrice*amount)throw new IllegalStateException("Right-click buy amount was not "+amount);
                                marketBalance=snapshot.getClass().getField("balance").getLong(snapshot);
                                if(marketStage==31){marketBuyMenu(injector,instance,clientApi,client,bounds,2);marketStage=32;return false;}
                                if(marketStage==32){marketBuyMenu(injector,instance,clientApi,client,bounds,3);marketStage=33;return false;}
                                marketClick(injector,instance,clientApi,client,bounds.x+40,bounds.y+78);marketStage=6;return false;
                            }
                            if(marketStage==33){
                                Class<?> varc=Class.forName("net.runelite.api.VarClientInt");
                                int input=(Integer)clientApi.getMethod("getVar",varc).invoke(client,varc.getField("INPUT_TYPE").get(null));
                                if(input==0)throw new IllegalStateException("Buy X did not open the numeric dialogue");
                                captureFrame(drawManager,System.getenv("LLYETA_MARKET_SCREENSHOT")+".buyx.png");marketStage=35;return false;
                            }
                            if(marketStage==35){
                                if(frameError!=null)throw new IllegalStateException("Buy X capture failed",frameError);
                                if(!frameSaved)return false;
                                java.awt.Canvas canvas=(java.awt.Canvas)clientApi.getMethod("getCanvas").invoke(client);
                                java.awt.event.KeyListener nativeKeys=(java.awt.event.KeyListener)field("KeyHandler","KeyHandler_instance").get(null);
                                nativeKeys.keyTyped(new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_UNDEFINED,'7'));
                                nativeKeys.keyPressed(new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_ENTER,'\n'));
                                nativeKeys.keyReleased(new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_ENTER,'\n'));
                                commandSent=System.currentTimeMillis();marketStage=34;return false;
                            }
                            if(marketStage==6){
                                java.lang.reflect.Field selected=marketPlugin.getClass().getDeclaredField("selectedCategory");selected.setAccessible(true);
                                if(selected.getInt(marketPlugin)!=1)throw new IllegalStateException("Market did not jump to the Ranged header");
                                marketClick(injector,instance,clientApi,client,bounds.x+40,bounds.y+300);
                                java.awt.Canvas canvas=(java.awt.Canvas)clientApi.getMethod("getCanvas").invoke(client);
                                Object keys=instance.invoke(injector,Class.forName("net.runelite.client.input.KeyManager"));
                                for(char c:"dragon".toCharArray())keys.getClass().getMethod("processKeyTyped",java.awt.event.KeyEvent.class).invoke(keys,new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_UNDEFINED,c));
                                keys.getClass().getMethod("processKeyPressed",java.awt.event.KeyEvent.class).invoke(keys,new java.awt.event.KeyEvent(canvas,java.awt.event.KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_ENTER,'\n'));
                                commandSent=System.currentTimeMillis();marketStage=7;return false;
                            }
                            if(marketStage==7){
                                if(!"dragon".equals(snapshot.getClass().getField("query").get(snapshot)))throw new IllegalStateException("Market search did not reach the server");
                                marketClick(injector,instance,clientApi,client,bounds.x+480,bounds.y+14);marketStage=8;return false;
                            }
                            if(marketStage==8){if(snapshot!=null)throw new IllegalStateException("Market close failed");captureFrame(drawManager,System.getenv("LLYETA_WORKSHOP_SCREENSHOT"));marketStage=9;return false;}
                            if(frameError!=null)throw new IllegalStateException("Market capture failed",frameError);
                            if(!frameSaved)return false;
                            System.out.println("MARKET_SMOKE PASS NPC trade, dropdown dismissal, both shops, scrolling, left-click price only, right-click Buy 1/5/10/X with typed numeric dialogue, 40% buyback, category, search, close and captures");return true;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_PRESET_CLOSE"))) {
                            if (autoCloseStage == 0) {
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 2});
                                commandSent = System.currentTimeMillis(); autoCloseStage = 1; return false;
                            }
                            if (System.currentTimeMillis() - commandSent < 2000) return false;
                            if (autoCloseStage == 1) { clickGearButton(injector, instance, clientApi, client); autoCloseStage = 2; return false; }
                            if (autoCloseStage == 2) { sendCommand("gear 0"); autoCloseStage = 3; return false; }
                            Field opened = gearPlugin.getClass().getDeclaredField("open"); opened.setAccessible(true);
                            if (autoCloseStage == 3) {
                                if (!opened.getBoolean(gearPlugin)) throw new IllegalStateException("Rejected load closed the gear panel");
                                sendCommand("gear 1"); autoCloseStage = 4; return false;
                            }
                            if (opened.getBoolean(gearPlugin)) throw new IllegalStateException("Successful load left the gear panel open");
                            System.out.println("PRESET_CLOSE_SMOKE PASS successful load closes the gear panel; rejected load keeps it open");
                            return true;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_CAMERA_INPUT"))) {
                            if (cameraStage == 0) {
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 2});
                                commandSent = System.currentTimeMillis(); cameraStage = 1; return false;
                            }
                            if (System.currentTimeMillis() - commandSent < 1500) return false;
                            if (cameraStage == 1) {
                                Object widget = clientApi.getMethod("getWidget", int.class, int.class).invoke(client, 720, 0);
                                verifyMouseRelease(clientApi, client, (java.awt.Rectangle) Class.forName("net.runelite.api.widgets.Widget").getMethod("getBounds").invoke(widget));
                                clickGearButton(injector, instance, clientApi, client); cameraStage = 2; return false;
                            }
                            Field panelField = gearPlugin.getClass().getDeclaredField("panel"); panelField.setAccessible(true);
                            java.awt.Rectangle panel = (java.awt.Rectangle) panelField.get(gearPlugin);
                            if (panel.isEmpty()) throw new IllegalStateException("Gear panel did not open");
                            verifyMouseRelease(clientApi, client, panel);
                            System.out.println("CAMERA_INPUT_SMOKE PASS native left/middle/right releases reset held state over Quest summary and gear panel; middle and Alt-left camera gestures pass through");
                            return true;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_QUEST_TAB")) && questStage < 5) {
                            if (questStage == 0) {
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 3});
                                commandSent = System.currentTimeMillis(); questStage = 1; return false;
                            }
                            if (System.currentTimeMillis() - commandSent < 1500) return false;
                            if (questStage == 1 || questStage == 4) {
                                Object manager = instance.invoke(injector, Class.forName("net.runelite.client.plugins.PluginManager"));
                                for (Object plugin : (java.util.Collection<?>) manager.getClass().getMethod("getPlugins").invoke(manager))
                                    if (plugin.getClass().getName().endsWith("LlyetaGearsPlugin")) gearPlugin = plugin;
                                Field frame = gearPlugin.getClass().getDeclaredField("questFrame"); frame.setAccessible(true);
                                Field opened = gearPlugin.getClass().getDeclaredField("open"); opened.setAccessible(true);
                                if (!((java.awt.Rectangle) frame.get(gearPlugin)).isEmpty() || opened.getBoolean(gearPlugin)) throw new IllegalStateException("Quest interface leaked onto another tab");
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 2});
                                commandSent = System.currentTimeMillis(); questStage = questStage == 1 ? 2 : 5; return false;
                            }
                            if (questStage == 2) { clickGearButton(injector, instance, clientApi, client); questStage = 3; return false; }
                            if (questStage == 3) {
                                Field panelField = gearPlugin.getClass().getDeclaredField("panel"); panelField.setAccessible(true);
                                java.awt.Rectangle panel = (java.awt.Rectangle) panelField.get(gearPlugin);
                                Object viewport = clientApi.getMethod("getViewportWidget").invoke(client);
                                java.awt.Rectangle bounds = (java.awt.Rectangle) Class.forName("net.runelite.api.widgets.Widget").getMethod("getBounds").invoke(viewport);
                                if (panel.isEmpty() || !bounds.contains(panel)) throw new IllegalStateException("Gear panel is not aligned inside the game viewport: " + panel + " vs " + bounds);
                                System.out.println("QUEST_TAB_SMOKE PASS helm opens gear panel within viewport: " + panel);
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 3});
                                commandSent = System.currentTimeMillis(); questStage = 4; return false;
                            }
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_QUEST_TAB")) && questStage == 5) {
                            if (System.currentTimeMillis() - commandSent < 1500) return false;
                            clickGearButton(injector, instance, clientApi, client); questStage = 6;
                            System.out.println("QUEST_TAB_SMOKE PASS other tabs hide/close gear controls; Quest tab exposes the helm button");
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_QUEST_TAB")) && questStage >= 6 && questStage < 9) {
                            if (System.currentTimeMillis() - commandSent < 1500) return false;
                            Field frameField = gearPlugin.getClass().getDeclaredField("questFrame"); frameField.setAccessible(true);
                            java.awt.Rectangle frame = (java.awt.Rectangle) frameField.get(gearPlugin);
                            if (questStage == 6) {
                                java.awt.Canvas canvas = (java.awt.Canvas) clientApi.getMethod("getCanvas").invoke(client);
                                Object mouse = instance.invoke(injector, Class.forName("net.runelite.client.input.MouseManager"));
                                java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, frame.x + 70, frame.y + frame.height - 18, 1, false, java.awt.event.MouseEvent.BUTTON1);
                                mouse.getClass().getMethod("processMousePressed", java.awt.event.MouseEvent.class).invoke(mouse, click);
                                commandSent = System.currentTimeMillis(); questStage = 7; return false;
                            }
                            if (questStage == 7) {
                                Object journal = clientApi.getMethod("getWidget", int.class, int.class).invoke(client, 701, 0);
                                if (!frame.isEmpty() || journal == null || (Boolean) Class.forName("net.runelite.api.widgets.Widget").getMethod("isHidden").invoke(journal)) throw new IllegalStateException("Achievements button did not open the journal cleanly");
                                clientApi.getMethod("invokeMenuAction", int.class, int.class, int.class, int.class, String.class, String.class, int.class, int.class)
                                        .invoke(client, -1, (701 << 16) | 48, 57, 1, "Back", "", 0, 0);
                                commandSent = System.currentTimeMillis(); questStage = 8; return false;
                            }
                            if (frame.isEmpty()) throw new IllegalStateException("Journal Back did not restore Quest summary");
                            clickGearButton(injector, instance, clientApi, client); questStage = 9;
                            System.out.println("QUEST_TAB_SMOKE PASS summary navigation opens Achievements and Back restores the summary");
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_ROADMAP")) && gearStage < 15) {
                            if (gearStage == 0) { sendCommand("gears"); gearStage = 1; return false; }
                            if (System.currentTimeMillis() - commandSent < 2500) return false;
                            String[] previews = (String[]) Class.forName(PREFIX + "LlyetaCommands").getMethod("previews").invoke(null);
                            if (gearStage == 13) { clickGearButton(injector, instance, clientApi, client); gearStage = 14; return false; }
                            if (gearStage == 14) {
                                Field opened = gearPlugin.getClass().getDeclaredField("open"); opened.setAccessible(true);
                                if (!opened.getBoolean(gearPlugin)) throw new IllegalStateException("Gear Presets button failed to reopen interface");
                                System.out.println("GEARS_SMOKE PASS Gear Presets button reopens the interface after a successful load");
                                gearStage = 15;
                            }
                            if (gearStage == 15) return false;
                            if (previews[0] == null || !previews[5].startsWith("Max range|")) throw new IllegalStateException("Missing six server gear previews");
                            if (gearStage > 1 && gearStage < 8) {
                                int loaded = gearStage - 2;
                                int[] levels = (int[]) clientApi.getMethod("getRealSkillLevels").invoke(client);
                                int defence = loaded == 2 || loaded == 3 ? 1 : 99;
                                int prayer = loaded == 2 || loaded == 3 ? 52 : 99;
                                if (levels[1] != defence || levels[5] != prayer) throw new IllegalStateException("Preset " + loaded + " levels " + levels[1] + "/" + levels[5]);
                                Class<?> inventoryId = Class.forName("net.runelite.api.InventoryID");
                                Object equipmentId = inventoryId.getField("EQUIPMENT").get(null);
                                Object equipment = clientApi.getMethod("getItemContainer", inventoryId).invoke(client, equipmentId);
                                Object[] actual = (Object[]) Class.forName("net.runelite.api.ItemContainer").getMethod("getItems").invoke(equipment);
                                String[] expected = previews[loaded].split("\\|", -1)[1].split(",");
                                for (int slot = 0; slot < expected.length; slot++) {
                                    int id = slot < actual.length ? (Integer) actual[slot].getClass().getMethod("getId").invoke(actual[slot]) : -1;
                                    if (id != Integer.parseInt(expected[slot])) throw new IllegalStateException("Preset " + loaded + " slot " + slot + ": " + id + " != " + expected[slot]);
                                }
                                System.out.println("GEARS_SMOKE loaded " + previews[loaded].split("\\|")[0] + " equipment/stats match");
                            }
                            if (gearStage <= 6) { sendCommand("gear " + gearStage); gearStage++; return false; }
                            if (gearStage == 7) {
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 2});
                                Object keyManager = instance.invoke(injector, Class.forName("net.runelite.client.input.KeyManager"));
                                java.awt.Canvas canvas = (java.awt.Canvas) clientApi.getMethod("getCanvas").invoke(client);
                                java.awt.event.KeyEvent key = new java.awt.event.KeyEvent(canvas, java.awt.event.KeyEvent.KEY_PRESSED, System.currentTimeMillis(), java.awt.event.InputEvent.CTRL_DOWN_MASK, java.awt.event.KeyEvent.VK_G, 'G');
                                keyManager.getClass().getMethod("processKeyPressed", java.awt.event.KeyEvent.class).invoke(keyManager, key);
                                if (!key.isConsumed()) throw new IllegalStateException("Ctrl+G was not handled");
                                clientApi.getMethod("runScript", Object[].class).invoke(client, (Object) new Object[]{915, 2});
                                java.awt.event.KeyEvent quick = new java.awt.event.KeyEvent(canvas, java.awt.event.KeyEvent.KEY_PRESSED, System.currentTimeMillis(), java.awt.event.InputEvent.CTRL_DOWN_MASK, java.awt.event.KeyEvent.VK_Q, 'Q');
                                keyManager.getClass().getMethod("processKeyPressed", java.awt.event.KeyEvent.class).invoke(keyManager, quick);
                                if (!quick.isConsumed()) throw new IllegalStateException("Ctrl+Q was not handled");
                                commandSent = System.currentTimeMillis(); gearStage = 8; return false;
                            }
                            if (gearStage == 8) {
                                Object widget = clientApi.getMethod("getWidget", int.class, int.class).invoke(client, 720, 0);
                                System.out.println("GEARS_SMOKE six presets, server previews and Ctrl+G/Ctrl+Q; summary bounds=" + Class.forName("net.runelite.api.widgets.Widget").getMethod("getBounds").invoke(widget));
                                clientApi.getMethod("invokeMenuAction", int.class, int.class, int.class, int.class, String.class, String.class, int.class, int.class)
                                        .invoke(client, -1, (541 << 16) | 5, 57, 1, "Activate", "Thick Skin", 0, 0);
                                commandSent = System.currentTimeMillis(); gearStage = 9; return false;
                            }
                            int[] varps = (int[]) clientApi.getMethod("getVarps").invoke(client);
                            int prayerActive = (Integer) clientApi.getMethod("getVarbitValue", int[].class, int.class).invoke(client, varps, 4104);
                            if (gearStage == 9) {
                                if (prayerActive != 1) throw new IllegalStateException("Prayer did not activate before saving");
                                savedInventory = inventorySnapshot(clientApi, client);
                                for (int i = 6; i < previews.length; i++) if (previews[i] != null && previews[i].startsWith("Empty slot|")) { customSlot = i + 1; break; }
                                if (customSlot == 0) throw new IllegalStateException("No empty custom preset box for smoke test");
                                sendCommand("storegear " + customSlot); gearStage = 10; return false;
                            }
                            if (gearStage == 10) {
                                if (previews[customSlot - 1] == null || !previews[customSlot - 1].startsWith("Custom preset " + customSlot + "|")) throw new IllegalStateException("Custom preset did not save/sync");
                                sendCommand("gear 3"); gearStage = 11; return false;
                            }
                            if (gearStage == 11) { sendCommand("preset " + customSlot); gearStage = 12; return false; }
                            if (prayerActive != 1 || !savedInventory.equals(inventorySnapshot(clientApi, client))) throw new IllegalStateException("Custom inventory/prayer restore mismatch");
                            int[] restored = (int[]) clientApi.getMethod("getRealSkillLevels").invoke(client);
                            if (restored[1] != 99 || restored[5] != 99) throw new IllegalStateException("Custom stats did not restore");
                            System.out.println("GEARS_SMOKE PASS custom box saved and restored inventory, gear stats and active prayer");
                            Field opened = gearPlugin.getClass().getDeclaredField("open"); opened.setAccessible(true);
                            if (opened.getBoolean(gearPlugin)) throw new IllegalStateException("Saved preset load did not close the interface");
                            commandSent = System.currentTimeMillis(); gearStage = 13; return false;
                        }
                        if (homeCheck != null) {
                            if (homeStage == 0) {
                                field("Client", "camAngleX").setInt(null, 383);
                                field("Client", "camAngleY").setInt(null, 0);
                                if (!homeCheck.equals("spawn") || "1".equals(System.getenv("LLYETA_VERIFY_ROADMAP"))) sendCommand("home");
                                else commandSent = System.currentTimeMillis();
                                homeStage = 1; return false;
                            }
                            if (System.currentTimeMillis() - commandSent < 4000) return false;
                            int x = (Integer) location.getClass().getMethod("getX").invoke(location);
                            int y = (Integer) location.getClass().getMethod("getY").invoke(location);
                            int plane = (Integer) location.getClass().getMethod("getPlane").invoke(location);
                            if (x != 3087 || y != 3492 || plane != 0) throw new IllegalStateException("Edgeville home check failed: " + location);
                            if (homeStage == 1) {
                                String screenshot = System.getenv("LLYETA_HOME_SCREENSHOT");
                                if (screenshot == null) frameSaved = true;
                                else {
                                    java.util.function.Consumer<java.awt.Image> capture = image -> {
                                        try {
                                            java.awt.image.BufferedImage frame = new java.awt.image.BufferedImage(image.getWidth(null), image.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_RGB);
                                            java.awt.Graphics graphics = frame.getGraphics(); graphics.drawImage(image, 0, 0, null); graphics.dispose();
                                            java.io.File file = new java.io.File(screenshot); file.getParentFile().mkdirs();
                                            javax.imageio.ImageIO.write(frame, "png", file); frameSaved = true;
                                        } catch (Throwable error) { frameError = error; }
                                    };
                                    drawManager.getClass().getMethod("requestNextFrameListener", java.util.function.Consumer.class).invoke(drawManager, capture);
                                }
                                homeStage = 2; return false;
                            }
                            if (frameError != null) throw new IllegalStateException("Frame capture failed", frameError);
                            if (!frameSaved) return false;
                            System.out.println("HOME_SMOKE PASS mode=" + homeCheck + " Edgeville position=" + location + "; rendered frame captured");
                            return true;
                        }
                        if ("1".equals(System.getenv("LLYETA_VERIFY_COMMANDS"))) {
                            if (commandStage == 0) { sendCommand("commands"); commandStage = 1; return false; }
                            if (System.currentTimeMillis() - commandSent < 2000) return false;
                            String menu = commandMenu(clientApi, client);
                            if (commandStage == 1) {
                                if (!menu.contains("Llyeta Commands") || !menu.contains("accessible commands"))
                                    throw new IllegalStateException("Command catalogue interface did not open");
                                System.out.println("COMMANDS_SMOKE full catalogue opened");
                                sendCommand("commands 1 save"); commandStage = 2; return false;
                            }
                            if (commandStage == 2) {
                                if (!menu.contains("::save")) throw new IllegalStateException("Command search missing save");
                                sendCommand("staffcommands"); commandStage = 3; return false;
                            }
                            if (commandStage == 3) {
                                if (!menu.contains("::staffcommands [page] [search]")) throw new IllegalStateException("Staff catalogue missing");
                                sendCommand("commands");
                                System.out.println("COMMANDS_SMOKE PASS full list, search and staff alias via actual game packets/widgets");
                            }
                        }
                        return true;
                    }
                } else { loggedInSince = 0; }
                return false;
            } catch (Throwable error) {
                System.out.println("LOCAL_SMOKE FAIL " + error);
                try {
                    java.util.Map<?, ?> chats = (java.util.Map<?, ?>) clientApi.getMethod("getChatLineMap").invoke(client);
                    for (Object buffer : chats.values()) {
                        Object[] lines = (Object[]) Class.forName("net.runelite.api.ChatLineBuffer").getMethod("getLines").invoke(buffer);
                        for (Object line : lines) if (line != null) System.out.println("SMOKE_CHAT " + Class.forName("net.runelite.api.MessageNode").getMethod("getValue").invoke(line));
                    }
                } catch (Throwable ignored) {}
                error.printStackTrace();
                return true;
            }
        };
        clientThread.getClass().getMethod("invokeLater", BooleanSupplier.class).invoke(clientThread, probe);
    }
}
