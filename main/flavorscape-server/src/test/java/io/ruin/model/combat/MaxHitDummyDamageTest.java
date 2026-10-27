package io.ruin.model.combat;

import io.ruin.data.impl.npcs.npc_combat;
import io.ruin.model.activities.miscpvm.MaxHitDummy;
import io.ruin.model.combat.special.melee.DragonClaws;
import io.ruin.model.combat.special.melee.VestasLongsword;
import io.ruin.model.entity.npc.NPC;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Uses the real dummy listener and the real special handlers, including Hit.defend. */
public class MaxHitDummyDamageTest {
    @BeforeClass public static void data() throws Exception { CombatEquipmentInteractionTest.data(); }
    private static class DummyHandler extends MaxHitDummy {
        void attach(NPC target) { npc_combat.Info info=new npc_combat.Info();info.hitpoints=2000;init(target,info); }
    }
    private static class Target extends CombatEquipmentInteractionTest.Monster {
        final List<Hit> received=new ArrayList<>();
        Target(boolean dummy) { setId(dummy?2668:1);if(dummy)new DummyHandler().attach(this); }
        @Override public int hit(Hit... hits) {
            int damage=0;for(Hit hit:hits){assertTrue(hit.defend(this));received.add(hit);damage+=hit.damage;}return damage;
        }
    }
    @Test public void vestasSpecialAlwaysReachesItsFullMaximumOnTheDummy() {
        var player=new CombatEquipmentInteractionTest.Fighter();Target target=new Target(true);
        for(int i=0;i<250;i++){
            target.received.clear();new VestasLongsword().handle(player,target,AttackStyle.SLASH,AttackType.AGGRESSIVE,40);
            assertEquals(48,target.received.get(0).damage);assertFalse(target.received.get(0).isBlocked());
        }
    }
    @Test public void clawsAlwaysUseTheMaximumLegalFourHitSequence() {
        var player=new CombatEquipmentInteractionTest.Fighter();Target target=new Target(true);
        for(int i=0;i<250;i++){
            target.received.clear();new DragonClaws().handle(player,target,AttackStyle.SLASH,AttackType.AGGRESSIVE,40);
            assertArrayEquals(new int[]{39,19,9,10},target.received.stream().mapToInt(h->h.damage).toArray());
        }
    }
    @Test public void dummyAppliesMaximumBeforeRolledDamageModifiersAndKeepsFixedHits() {
        var player=new CombatEquipmentInteractionTest.Fighter();Target target=new Target(true);
        Hit hit=new Hit(player,AttackStyle.SLASH,AttackType.AGGRESSIVE).randDamage(40)
                .boostDamage(.25).multiplyDamage(1.1).specialDamage(1.5).scaleRolledDamage(.5).addPostRollDamage(3);
        hit.block();target.hit(hit);assertEquals(82,hit.maxDamage);assertEquals(44,hit.damage);
        Hit fixed=new Hit(player).fixedDamage(7);target.hit(fixed);assertEquals(7,fixed.damage);
        Hit cannon=new Hit(player,AttackStyle.CANNON).randDamage(40);target.hit(cannon);assertEquals(0,cannon.damage);
    }
    @Test public void ordinaryTargetsKeepRandomDamageAndCanStillMiss() {
        var player=new CombatEquipmentInteractionTest.Fighter();Target target=new Target(false);
        boolean missed=false,belowMax=false;
        for(int i=0;i<250;i++){
            target.received.clear();new VestasLongsword().handle(player,target,AttackStyle.SLASH,AttackType.AGGRESSIVE,40);
            Hit hit=target.received.get(0);missed|=hit.isBlocked();belowMax|=hit.damage<48;
        }
        assertTrue(missed);assertTrue(belowMax);
    }
}
