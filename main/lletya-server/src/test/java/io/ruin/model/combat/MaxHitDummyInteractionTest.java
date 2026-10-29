package io.ruin.model.combat;

import io.ruin.model.combat.special.Special;
import io.ruin.model.combat.special.melee.ArmadylGodsword;
import io.ruin.model.combat.special.melee.DragonDagger;
import io.ruin.model.entity.player.PlayerCombat;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.map.route.routes.TargetRoute;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxHitDummyInteractionTest {
    private CombatEquipmentInteractionTest.Fighter player;
    private Target dummy;

    private static class Target extends CombatEquipmentInteractionTest.Monster {
        int hits;
        Target(int id) { setId(id); }
        @Override public int hit(Hit... hits) { this.hits += hits.length; return 0; }
    }

    @BeforeClass public static void data() throws Exception {
        CombatEquipmentInteractionTest.data();
    }

    @Before public void setup() throws Exception {
        player = new CombatEquipmentInteractionTest.Fighter() {
            final io.ruin.model.activities.duelarena.Duel duel = new io.ruin.model.activities.duelarena.Duel(this);
            @Override public void faceNone(boolean delay) {}
            @Override public io.ruin.model.activities.duelarena.Duel getDuel() { return duel; }
        };
        dummy = new Target(2668);
        AttackSet set = new AttackSet();
        set.style = AttackStyle.SLASH;
        set.type = AttackType.AGGRESSIVE;
        set.attackAnimation = 0;
        field("attackSet", set);
        player.combat.weaponType = new WeaponType();
        player.combat.weaponType.attackTicks = 6;
        player.getRouteFinder().targetRoute = new TargetRoute();
        player.getRouteFinder().targetRoute.withinDistance = true;
        player.combat.setTarget(dummy);
        Config.SPECIAL_ENERGY.set(player, 1000);
    }

    private void field(String name, Object value) throws Exception {
        Field field = PlayerCombat.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(player.combat, value);
    }

    private void special(Special special) throws Exception {
        field("specialActive", special);
        Config.SPECIAL_ACTIVE.set(player, 1);
    }

    @Test public void pvpWeaponCanArmAndSpecDummyWithFullBonusesOutsideBountyHunter() {
        io.ruin.cache.ItemDef def = io.ruin.cache.ItemDef.get(23615);
        Special original = def.special;
        final int[] testedMax = {-1};
        def.special = new io.ruin.model.combat.special.melee.VestasLongsword() {
            @Override public boolean handle(io.ruin.model.entity.player.Player p, io.ruin.model.entity.Entity t,
                                            AttackStyle style, AttackType type, int max) {
                testedMax[0] = max;
                return super.handle(p,t,style,type,max);
            }
        };
        try {
            player.wildernessLevel = 0;
            player.equipment.set(3,new io.ruin.model.item.Item(23615));
            player.equipment.set(4,new io.ruin.model.item.Item(22616));
            player.equipment.recalculateBonuses();
            assertEquals(0,player.combat.getBonus(10),0);
            Config.SPECIAL_ENERGY.set(player,650);
            player.combat.toggleSpecial();
            assertEquals(1,Config.SPECIAL_ACTIVE.get(player));
            player.combat.attack();
            assertEquals(1,dummy.hits);
            assertEquals(32,testedMax[0]);
            assertEquals(650,Config.SPECIAL_ENERGY.get(player));
            assertNull(player.combat.getTarget());
            assertFalse(player.combat.isTestingMaxHitDummy());
            assertEquals(0,player.combat.getBonus(10),0);
            assertFalse(io.ruin.model.activities.wilderness.BountyHunterEquipment.canUseWeapon(player,new Target(1),false));
        } finally {
            def.special = original;
        }
    }

    @Test public void pvpDummyExceptionStillRejectsInsufficientEnergy() throws Exception {
        player.wildernessLevel = 0;
        player.equipment.set(3,new io.ruin.model.item.Item(23615));
        special(new io.ruin.model.combat.special.melee.VestasLongsword());
        Config.SPECIAL_ENERGY.set(player,240);
        player.combat.attack();
        assertEquals(0,dummy.hits);
        assertEquals(240,Config.SPECIAL_ENERGY.get(player));
        assertFalse(player.combat.isTestingMaxHitDummy());
        assertEquals(0,player.combat.getBonus(10),0);
    }

    @Test public void normalAttackStopsAfterOneSwingAndDoesNotRestoreEnergy() {
        Config.SPECIAL_ENERGY.set(player, 200);
        player.combat.attack();
        player.combat.attack();
        assertEquals(1, dummy.hits);
        assertNull(player.combat.getTarget());
        assertNull(player.combat.lastTarget);
        assertEquals(200, Config.SPECIAL_ENERGY.get(player));
    }

    @Test public void godswordSpecialRefundsItsEnergyAndStops() throws Exception {
        special(new ArmadylGodsword());
        player.combat.attack();
        assertEquals(1, dummy.hits);
        assertEquals(1000, Config.SPECIAL_ENERGY.get(player));
        assertNull(player.combat.getTarget());
        assertEquals(0, Config.SPECIAL_ACTIVE.get(player));
    }

    @Test public void unaffordableSpecialDoesNotFallBackToNormalAttack() throws Exception {
        special(new ArmadylGodsword());
        Config.SPECIAL_ENERGY.set(player, 499);
        player.combat.attack();
        player.combat.attack();
        assertEquals(0, dummy.hits);
        assertEquals(499, Config.SPECIAL_ENERGY.get(player));
        assertNull(player.combat.getTarget());
    }

    @Test public void multiHitSpecialFinishesItsHitsWithoutAnotherAttack() throws Exception {
        special(new DragonDagger());
        player.combat.attack();
        player.combat.attack();
        assertEquals(2, dummy.hits);
        assertEquals(1000, Config.SPECIAL_ENERGY.get(player));
        assertNull(player.combat.getTarget());
    }

    @Test public void dummyRefundPreservesPartialEnergyInsteadOfRefillingToFull() throws Exception {
        special(new ArmadylGodsword());
        Config.SPECIAL_ENERGY.set(player, 650);
        player.combat.attack();
        assertEquals(1, dummy.hits);
        assertEquals(650, Config.SPECIAL_ENERGY.get(player));
    }

    @Test public void ordinaryTargetsStillConsumeSpecialEnergy() throws Exception {
        Target monster = new Target(1);
        player.combat.setTarget(monster);
        special(new ArmadylGodsword());
        player.combat.attack();
        assertEquals(1, monster.hits);
        assertEquals(500, Config.SPECIAL_ENERGY.get(player));
    }

    @Test public void dummyWaitsForAttackDelayBeforePerformingItsOneAttack() {
        player.combat.delayAttack(100);
        player.combat.attack();
        assertEquals(0, dummy.hits);
        assertSame(dummy, player.combat.getTarget());
    }

    @Test public void ordinaryTargetsKeepTheirCombatTarget() {
        Target monster = new Target(1);
        player.combat.setTarget(monster);
        player.combat.attack();
        assertEquals(1, monster.hits);
        assertSame(monster, player.combat.getTarget());
    }
}
