package io.ruin.model.item;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoinCurrencyTest {
    @org.junit.BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }
    @Test public void coinItemsNormalizeInConstructorsAndTransforms() {
        for (int id : new int[]{995, 996, 6964}) {
            Item award = new Item(id, 500);
            assertEquals(13307, award.getId());
            assertEquals(500, award.copy().getAmount());
            award.setId(id);
            assertEquals(13307, award.getId());
        }
        assertEquals(6529, new Item(6529, 12).getId()); // Tokkul stays an activity token.
    }

    @Test public void gsonSavedCoinsNormalizeWhenContainerInitializes() {
        ItemContainer inventory = io.ruin.api.utils.JsonUtils.GSON_EXPOSE.fromJson("{items:[{id:995,amount:500}]}", ItemContainer.class);
        inventory.init(28, false);
        assertEquals(13307, inventory.get(0).getId());
        assertEquals(500, inventory.get(0).getAmount());
    }
}
