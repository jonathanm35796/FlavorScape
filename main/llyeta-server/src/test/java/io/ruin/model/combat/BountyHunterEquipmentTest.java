package io.ruin.model.combat;

import io.ruin.cache.ItemDef;
import io.ruin.model.activities.wilderness.BountyHunter;
import io.ruin.model.activities.wilderness.BountyHunterEquipment;
import io.ruin.model.inter.handlers.EquipmentStats;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.Item;
import io.ruin.model.skills.magic.spells.ancient.IceBarrage;
import java.lang.reflect.Method;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class BountyHunterEquipmentTest {
    private static final int[] ITEMS = {22610,22613,22616,22619,22622,22625,22628,22631,
            22634,22636,22638,22641,22644,22647,22650,22653,22656,23615,23617,23620};

    @BeforeClass public static void data() throws Exception {
        CombatEquipmentInteractionTest.data();
    }

    private static class Fighter extends CombatEquipmentInteractionTest.Fighter {
        final BountyHunter bh = new BountyHunter();
        final io.ruin.model.map.Position position = new io.ruin.model.map.Position(3090,3530,0);
        @Override public BountyHunter getBountyHunter() { return bh; }
        @Override public io.ruin.model.map.Position getPosition() { return position; }
        @Override public void faceNone(boolean delay) {}
        Fighter() {
            getPosition().set(3090,3530,0);
            wildernessLevel = 5;
        }
    }

    @Test public void allSupportedItemsKeepFullStatsOnlyAtBountyHunterLevelsOneThroughFive() {
        for (int id : ITEMS) {
            Fighter player = new Fighter();
            ItemDef def = ItemDef.get(id);
            Item worn = new Item(id);
            player.equipment.set(def.equipSlot, worn);
            for (int level : new int[]{1,5}) {
                player.wildernessLevel = level;
                player.equipment.recalculateBonuses();
                assertArrayEquals("active item " + id, def.equipBonuses, player.equipment.bonuses);
                assertEquals(def.magicDamageBonus, player.equipment.magicDamageBonus, 0);
            }
            for (int level : new int[]{0,6,20,55}) {
                player.wildernessLevel = level;
                player.equipment.recalculateBonuses();
                assertArrayEquals("inactive item " + id, new int[16], player.equipment.bonuses);
                assertEquals(0, player.equipment.magicDamageBonus, 0);
                assertSame(worn, player.equipment.get(def.equipSlot));
            }
        }
    }

    @Test public void ancientWarriorEquipmentHasValidInventoryEquipActions() {
        for (int id : ITEMS) {
            ItemDef def = ItemDef.get(id);
            assertTrue("Missing slot for " + id, def.equipSlot >= 0);
            assertTrue("Missing equip action for " + id + " " + def.name, def.equipOption >= 0);
        }
    }

    @Test public void longswordVariantLoadsItsWeaponDefinitionAndKeepsNotesUnequippable() {
        ItemDef base = ItemDef.get(22613), variant = ItemDef.get(23615);
        assertEquals(3, variant.equipSlot);
        assertEquals(2, variant.equipOption);
        assertSame(base.weaponType, variant.weaponType);
        assertNotNull(variant.weaponType);
        assertArrayEquals(base.equipReqs, variant.equipReqs);
        assertArrayEquals(base.equipBonuses, variant.equipBonuses);
        assertTrue(new io.ruin.model.combat.special.melee.VestasLongsword().accept(variant,variant.name));
        assertEquals(-1,ItemDef.get(22614).equipSlot);
        assertEquals(-1,ItemDef.get(22615).equipSlot);
        Fighter player = new Fighter();
        player.equipment.set(3,new Item(23615));
        assertTrue(BountyHunterEquipment.canUseWeapon(player,false));
        player.wildernessLevel = 6;
        assertFalse(BountyHunterEquipment.canUseWeapon(player,false));
        player.equipment.set(3,new Item(23617));
        assertFalse(BountyHunterEquipment.hasActiveItem(player,22647));
        player.wildernessLevel = 5;
        assertTrue(BountyHunterEquipment.hasActiveItem(player,22647));
    }

    @Test public void inactivePvpArmourDoesNotDegradeWhenHit() throws Exception {
        Fighter player = new Fighter();
        Item item = new Item(22616);
        Method degrade = io.ruin.model.content.PvPArmor.class.getDeclaredMethod("postDamageListener",
                io.ruin.model.entity.player.Player.class,Item.class,Hit.class);
        degrade.setAccessible(true);
        player.wildernessLevel = 6;
        degrade.invoke(null,player,item,new Hit().fixedDamage(10));
        assertFalse(io.ruin.model.item.attributes.AttributeExtensions.hasAttribute(item,
                io.ruin.model.item.attributes.AttributeTypes.CHARGES));
        player.wildernessLevel = 5;
        degrade.invoke(null,player,item,new Hit().fixedDamage(10));
        assertEquals(59999,io.ruin.model.item.attributes.AttributeExtensions.getCharges(item));
    }

    @Test public void crossingTheCapRefreshesCombatBonusesWithoutReequipping() {
        Fighter player = new Fighter();
        player.equipment.set(4,new Item(22616));
        player.equipment.set(2,new Item(6585));
        player.equipment.recalculateBonuses();
        assertEquals(45, player.combat.getBonus(EquipmentStats.SLASH_ATTACK), 0);
        player.wildernessLevel = 6;
        assertEquals(10, player.combat.getBonus(EquipmentStats.SLASH_ATTACK), 0);
        player.wildernessLevel = 5;
        assertEquals(45, player.combat.getBonus(EquipmentStats.SLASH_ATTACK), 0);
        player.bh.targeting = BountyHunter.Targeting.DISABLED;
        assertEquals(10, player.combat.getBonus(EquipmentStats.SLASH_ATTACK), 0);
    }

    @Test public void edgevilleTargetingRequiresItsConfiguredAreaAndNoTargetIsNeededToParticipate() {
        Fighter player = new Fighter();
        player.bh.targeting = BountyHunter.Targeting.EDGEVILLE;
        assertNull(player.bh.target);
        assertTrue(BountyHunterEquipment.isActive(player));
        player.getPosition().set(3200,3530,0);
        assertFalse(BountyHunterEquipment.isActive(player));
        player.bh.targeting = BountyHunter.Targeting.ALL;
        assertTrue(BountyHunterEquipment.isActive(player));
    }

    @Test public void inactiveWeaponsRejectSpecialActivationAndSpellCastingWithoutSpendingEnergy() {
        Fighter player = new Fighter();
        player.wildernessLevel = 6;
        Config.SPECIAL_ENERGY.set(player,1000);
        player.equipment.set(3,new Item(22613));
        assertFalse(BountyHunterEquipment.canUseWeapon(player,false));
        player.combat.toggleSpecial();
        assertEquals(0,Config.SPECIAL_ACTIVE.get(player));
        assertEquals(1000,Config.SPECIAL_ENERGY.get(player));
        player.equipment.set(3,new Item(22647));
        assertFalse(new IceBarrage().cast(player,new CombatEquipmentInteractionTest.Monster()));
        player.wildernessLevel = 5;
        assertTrue(BountyHunterEquipment.canUseWeapon(player,false));
        player.equipment.set(3,new Item(11802));
        player.wildernessLevel = 6;
        assertTrue(BountyHunterEquipment.canUseWeapon(player,false));
    }

    @Test public void zurielIceAccuracyAndSpearProtectionSwitchOffPastTheCap() throws Exception {
        Fighter player = new Fighter();
        player.equipment.set(3,new Item(22647));
        player.equipment.recalculateBonuses();
        Method beforeHit = IceBarrage.class.getDeclaredMethod("beforeHit",Hit.class,io.ruin.model.entity.Entity.class);
        beforeHit.setAccessible(true);
        Hit active = new Hit(player,AttackStyle.MAGIC,null).randDamage(30);
        beforeHit.invoke(new IceBarrage(),active,player);
        assertTrue(active.getModifiedAttackBonus() > active.getAttackBonus());
        player.wildernessLevel = 6;
        Hit inactive = new Hit(player,AttackStyle.MAGIC,null).randDamage(30);
        beforeHit.invoke(new IceBarrage(),inactive,player);
        assertEquals(inactive.getAttackBonus(),inactive.getModifiedAttackBonus(),0);
        player.equipment.set(3,new Item(22610));
        player.vestasSpearSpecial.delay(8);
        Hit incoming = new Hit(new CombatEquipmentInteractionTest.Fighter(),AttackStyle.SLASH).fixedDamage(20).ignoreDefence();
        incoming.defend(player);
        assertFalse(incoming.isBlocked());
        player.wildernessLevel = 5;
        Hit protectedHit = new Hit(new CombatEquipmentInteractionTest.Fighter(),AttackStyle.SLASH).fixedDamage(20).ignoreDefence();
        protectedHit.defend(player);
        assertTrue(protectedHit.isBlocked());
    }
}
