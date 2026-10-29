package net.runelite.client.plugins.lletyagears;

import net.runelite.standalone.CustomInterfaceEdits;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatSkillActionsTest {
    @Test public void skillButtonsUseCorrectCombatNames(){
        int[] children={1,2,3,4,5,6,9};String[] names={"Attack","Strength","Defence","Ranged","Prayer","Magic","Hitpoints"};
        for(int i=0;i<children.length;i++)assertEquals("Set "+names[i]+" level",CustomInterfaceEdits.combatLevelAction(children[i]));
        for(int child:new int[]{0,7,8,10,11,12,13,14,15,16,17,18,19,20,21,22,23})assertNull(CustomInterfaceEdits.combatLevelAction(child));
    }
}
