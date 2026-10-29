package io.ruin.model.map.route.routes;

import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TargetRouteTest {
    @org.junit.BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }
    @Test public void failedActionIsConsumedAndReplacementSurvives() throws Exception {
        TargetRoute route = new TargetRoute();
        Player entity = new Player() { @Override public void faceNone(boolean update) {} };
        Field action = TargetRoute.class.getDeclaredField("finishAction");
        action.setAccessible(true);
        Method finish = TargetRoute.class.getDeclaredMethod("afterMovement0", Entity.class);
        finish.setAccessible(true);
        route.target = entity;
        route.withinDistance = true;
        action.set(route, (Runnable) () -> { throw new IllegalStateException("test failure"); });
        try { finish.invoke(route, entity); fail("Expected action failure"); }
        catch (InvocationTargetException expected) { assertTrue(expected.getCause() instanceof IllegalStateException); }
        assertNull(route.target);
        assertNull(action.get(route));
        route.target = entity;
        Runnable replacement = () -> {};
        action.set(route, (Runnable) () -> { route.target = entity; try { action.set(route, replacement); } catch (Exception e) { throw new RuntimeException(e); } });
        finish.invoke(route, entity);
        assertSame(entity, route.target);
        assertSame(replacement, action.get(route));
    }
}
