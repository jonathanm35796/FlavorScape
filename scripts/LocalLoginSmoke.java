import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/** Integration probe for the selected revision-184 client; uses a disposable local DEV account. */
public final class LocalLoginSmoke {
    private static final String PREFIX = "net.runelite.standalone.";
    private static long deadline;
    private static long loggedInSince;
    private static int lastState = -1;
    private static int firstTick;
    private static boolean submitted;
    private static boolean loginReported;
    private static int commandStage;
    private static long commandSent;

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
        deadline = System.currentTimeMillis() + 180_000;
        BooleanSupplier probe = () -> {
            try {
                int state = field("Client", "gameState").getInt(null);
                if (state != lastState) {
                    System.out.println("LOCAL_SMOKE state=" + state + " loadingStage=" + field("Client", "titleLoadingStage").getInt(null));
                    lastState = state;
                }
                if (state == 10 && !submitted) {
                    field("Login", "Login_username").set(null, "Localtest");
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
                        if ("1".equals(System.getenv("FLAVORSCAPE_VERIFY_COMMANDS"))) {
                            if (commandStage == 0) { sendCommand("commands"); commandStage = 1; return false; }
                            if (System.currentTimeMillis() - commandSent < 2000) return false;
                            String menu = commandMenu(clientApi, client);
                            if (commandStage == 1) {
                                if (!menu.contains("FlavorScape Commands") || !menu.contains("accessible commands"))
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
                if (System.currentTimeMillis() > deadline) {
                    System.out.println("LOCAL_SMOKE FAIL timeout at state=" + state);
                    return true;
                }
                return false;
            } catch (Throwable error) {
                System.out.println("LOCAL_SMOKE FAIL " + error);
                error.printStackTrace();
                return true;
            }
        };
        clientThread.getClass().getMethod("invokeLater", BooleanSupplier.class).invoke(clientThread, probe);
    }
}
