package net.runelite.http.api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.runelite.http.api.item.ItemStats;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TypeAdaptersTest
{
	private final Gson gson = new GsonBuilder()
		.registerTypeAdapter(ItemStats.class, TypeAdapters.ITEMSTATS)
		.create();

	@Test
	public void equipmentStatsSkipUnknownFieldsOfAnyType()
	{
		ItemStats stats = gson.fromJson("{\"equipable\":true,\"equipment\":{\"slot\":3,\"is2h\":true,"
			+ "\"futureObject\":{\"value\":1},\"futureArray\":[1,false],\"futureNull\":null,"
			+ "\"futureString\":\"value\",\"astab\":-2,\"str\":12,\"aspeed\":4}}", ItemStats.class);

		assertEquals(3, stats.getEquipment().getSlot());
		assertEquals(-2, stats.getEquipment().getAstab());
		assertEquals(12, stats.getEquipment().getStr());
		assertEquals(4, stats.getEquipment().getAspeed());
	}

	@Test(expected = JsonSyntaxException.class)
	public void equipmentStatsRejectInvalidKnownField()
	{
		gson.fromJson("{\"equipment\":{\"str\":true}}", ItemStats.class);
	}

	@Test
	public void itemStatsSkipUnknownFields()
	{
		ItemStats stats = gson.fromJson("{\"weight\":0.028,\"ge_limit\":11000}", ItemStats.class);

		assertEquals(0.028, stats.getWeight(), 0.00001);
	}
}
