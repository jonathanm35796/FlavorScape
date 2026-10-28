package io.ruin.tools;

import io.ruin.model.activities.wilderness.BountyHunterPoints;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.impl.MysteriousEmblem;
import io.ruin.model.shop.Currency;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class BountyHunterPointsTest {
    @BeforeClass public static void data() throws Exception { BountyHunterAudit.prepare(); }
    @Test public void tiersHalveExactlyAndTargetBonusAppliesEveryTime() {
        BountyHunterAudit.Hunter winner=new BountyHunterAudit.Hunter(),victim=new BountyHunterAudit.Hunter();
        for(int tier=1;tier<=10;tier++) {
            victim.inventory.set(0,new Item(MysteriousEmblem.itemId(tier)));
            long before=winner.bountyHunterPointUnits;
            BountyHunterPoints.rewardKill(winner,victim,false);
            assertEquals(100.0/Math.pow(2,10-tier),BountyHunterPoints.balance(winner)-before/512.0,0);
        }
        for(int i=0;i<2;i++){
            long before=winner.bountyHunterPointUnits;
            BountyHunterPoints.rewardKill(winner,victim,true);
            assertEquals(110L*512,winner.bountyHunterPointUnits-before);
        }
    }
    @Test public void noEmblemGivesOnePointAndSpendingPreservesFractions() {
        BountyHunterAudit.Hunter winner=new BountyHunterAudit.Hunter(),victim=new BountyHunterAudit.Hunter();
        BountyHunterPoints.rewardKill(winner,victim,false);assertEquals(512,winner.bountyHunterPointUnits);
        winner.bountyHunterPointUnits=513;
        assertEquals(0,Currency.BOUNTY_HUNTER_POINTS.getCurrencyHandler().removeCurrency(winner,-1));
        assertEquals(0,Currency.BOUNTY_HUNTER_POINTS.getCurrencyHandler().removeCurrency(winner,2));
        assertEquals(1,Currency.BOUNTY_HUNTER_POINTS.getCurrencyHandler().removeCurrency(winner,1));
        assertEquals(1,winner.bountyHunterPointUnits);
    }
    @Test public void balancesCapWithoutOverflow() {
        BountyHunterAudit.Hunter winner=new BountyHunterAudit.Hunter();
        winner.bountyHunterPointUnits=BountyHunterPoints.MAX_UNITS-1;
        assertEquals(1,BountyHunterPoints.add(winner,Long.MAX_VALUE));
        assertEquals(BountyHunterPoints.MAX_UNITS,winner.bountyHunterPointUnits);
        assertEquals(Integer.MAX_VALUE,Currency.BOUNTY_HUNTER_POINTS.getCurrencyHandler().getCurrencyCount(winner));
    }
}
