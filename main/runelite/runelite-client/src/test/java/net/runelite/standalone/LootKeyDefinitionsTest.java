package net.runelite.standalone;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LootKeyDefinitionsTest {
    private Client previous;
    @Before public void callbacks(){previous=ViewportMouse.client;ViewportMouse.client=mock(Client.class);when(ViewportMouse.client.getCallbacks()).thenReturn(mock(net.runelite.api.hooks.Callbacks.class));}
    @After public void restore(){ViewportMouse.client=previous;}
    @Test public void skullyCopiesAnAvailableNpcAndExposesTalkAndValue(){
        NPCDefinition base=new NPCDefinition();base.id=6773;base.models=new int[]{123};base.additionalModels=new int[]{456};
        NPCDefinition.NpcDefinition_cached.method3034(base,6773L);
        try{NPCDefinition skully=new NPCDefinition();skully.id=15000;skully.postDecode();
            assertEquals("Skully",skully.name);assertArrayEquals(new String[]{"Talk-to","Value",null,null,null},skully.actions);
            assertArrayEquals(base.models,skully.models);assertArrayEquals(base.additionalModels,skully.additionalModels);
        }finally{NPCDefinition.NpcDefinition_cached.method3035();}
    }
    @Test public void lootKeyUsesAnExistingKeyModelWithoutStacking(){
        ItemDefinition base=new ItemDefinition();base.id=13303;base.model=123;base.zoom2d=1000;
        ItemDefinition.ItemDefinition_cached.method3034(base,13303L);
        try{ItemDefinition key=new ItemDefinition();key.id=30330;key.postDecode();
            assertEquals("Loot key",key.name);assertEquals(123,key.model);assertEquals(0,key.isStackable);
            assertEquals("Check-value",key.inventoryActions[0]);assertEquals("Destroy",key.inventoryActions[4]);
        }finally{ItemDefinition.ItemDefinition_cached.method3035();}
    }
    @Test public void lootChestUsesAnExistingChestModelAndHasLootAction(){
        ObjectDefinition base=new ObjectDefinition();base.id=27290;base.modelIds=new int[]{123};
        ObjectDefinition.ObjectDefinition_cached.method3034(base,27290L);
        try{ObjectDefinition chest=new ObjectDefinition();chest.id=50005;chest.postDecode();
            assertEquals("Loot chest",chest.name);assertEquals("Loot",chest.actions[0]);assertArrayEquals(base.modelIds,chest.modelIds);
        }finally{ObjectDefinition.ObjectDefinition_cached.method3035();}
    }
}
