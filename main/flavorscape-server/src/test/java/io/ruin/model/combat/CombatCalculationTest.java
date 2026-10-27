package io.ruin.model.combat;

import io.ruin.model.entity.player.*;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Equipment;
import io.ruin.model.stat.*;
import org.junit.*;
import static org.junit.Assert.*;

public class CombatCalculationTest {
    @BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }

    public static class Fighter extends Player {
        final StatList stats = new StatList();
        final Equipment equipment = new Equipment();
        final PlayerPrayer prayer = new PlayerPrayer();
        AttackType stance = AttackType.ACCURATE;
        final PlayerCombat combat = new PlayerCombat() {
            @Override public AttackType getAttackType() { return stance; }
        };
        public Fighter() {
            stats.init(this);
            equipment.init(14, false);
            combat.init(this);
            hitListener = null;
            for(StatType type : StatType.values()) stats.get(type).set(99);
        }
        @Override public StatList getStats() { return stats; }
        @Override public int getHp() { return stats.get(StatType.Hitpoints).currentLevel; }
        @Override public int getMaxHp() { return stats.get(StatType.Hitpoints).fixedLevel; }
        @Override public Equipment getEquipment() { return equipment; }
        @Override public PlayerPrayer getPrayer() { return prayer; }
        @Override public PlayerCombat getCombat() { return combat; }
        @Override public int animate(int id, int delay) { return 0; }
        @Override public void graphics(int id, int height, int delay) {}
        @Override public void publicSound(int id, int type, int delay) {}
        @Override public void sendMessage(String message) {}
        @Override public int hit(Hit... hits) {
            int total = 0;
            for(Hit hit : hits) { hit.defend(this); total += hit.damage; }
            return total;
        }
    }

    @Test public void maxHitsFloorPrayerBeforeAddingStanceAndEight() {
        Fighter player = new Fighter();
        player.equipment.bonuses[10] = 100;
        player.prayer.strengthBoost = .23;
        assertEquals(34, CombatUtils.getMaxDamage(player, AttackStyle.SLASH, AttackType.AGGRESSIVE));
        assertEquals(33, CombatUtils.getMaxDamage(player, AttackStyle.SLASH, AttackType.ACCURATE));
        player.stats.get(StatType.Strength).set(1);
        player.equipment.bonuses[10] = 0;
        player.prayer.strengthBoost = 0;
        assertEquals(1, CombatUtils.getMaxDamage(player, AttackStyle.CRUSH, AttackType.ACCURATE));
    }

    @Test public void longrangeOnlyAddsDefence() {
        Fighter player = new Fighter();
        player.equipment.bonuses[4] = 100;
        player.equipment.bonuses[11] = 100;
        assertEquals(17548, CombatUtils.getAttackBonus(player, AttackStyle.RANGED, AttackType.LONG_RANGED), 0);
        assertEquals(CombatUtils.getMaxDamage(player, AttackStyle.RANGED, AttackType.RAPID_RANGED),
                CombatUtils.getMaxDamage(player, AttackStyle.RANGED, AttackType.LONG_RANGED));
        player.stance = AttackType.LONG_RANGED;
        assertEquals(110 * 64, CombatUtils.getDefenceBonus(player, AttackStyle.RANGED), 0);
    }

    @Test public void attackRollFloorsPrayerAndClampsNegativeEquipment() {
        Fighter player = new Fighter();
        player.prayer.attackBoost = .20;
        player.equipment.bonuses[0] = 100;
        assertEquals(129 * 164, CombatUtils.getAttackBonus(player, AttackStyle.STAB, AttackType.ACCURATE), 0);
        player.equipment.bonuses[3] = -100;
        assertEquals(0, CombatUtils.getAttackBonus(player, AttackStyle.MAGIC, AttackType.ACCURATE), 0);
    }

    @Test public void spellbookAccuracyDiffersFromAccuratePoweredStaff() {
        Fighter player = new Fighter();
        assertEquals(108 * 64, CombatUtils.getAttackBonus(player, AttackStyle.MAGIC, null), 0);
        assertEquals(110 * 64, CombatUtils.getAttackBonus(player, AttackStyle.MAGIC, AttackType.ACCURATE), 0);
    }

    @Test public void playerMagicDefenceAddsWholeBaseAndStanceAfterWeighting() {
        Fighter player = new Fighter();
        player.stats.get(StatType.Defence).set(1);
        assertEquals(77 * 64, CombatUtils.getDefenceBonus(player, AttackStyle.MAGIC), 0);
        player.stance = AttackType.DEFENSIVE;
        assertEquals(80 * 64, CombatUtils.getDefenceBonus(player, AttackStyle.MAGIC), 0);
    }

    @Test public void npcMagicDefenceUsesMagicRatherThanDefence() {
        Fighter target = new Fighter();
        target.player = null; // CombatUtils uses the entity category, not its Java type.
        target.stats.get(StatType.Magic).set(1);
        assertEquals(10 * 64, CombatUtils.getDefenceBonus(target, AttackStyle.MAGIC), 0);
        assertEquals(108 * 64, CombatUtils.getDefenceBonus(target, AttackStyle.SLASH), 0);
    }

    @Test public void fixedSpecialDefenceDoesNotChangeAttackingBonus() {
        Fighter attacker = new Fighter(), target = new Fighter();
        attacker.equipment.bonuses[0] = 100;
        target.equipment.bonuses[5] = 200;
        target.equipment.bonuses[6] = 10;
        Hit hit = new Hit(attacker, AttackStyle.STAB, AttackType.ACCURATE).defendAgainst(AttackStyle.SLASH);
        assertEquals(110 * 164, hit.getAttackBonus(), 0);
        assertEquals(107 * 74, hit.getDefenceBonus(target), 0);
    }

    @Test public void voidAffectsEffectiveLevelBeforeMaxHitRounding() {
        Fighter player = new Fighter();
        player.equipment.set(0, new Item(11664));
        player.equipment.set(4, new Item(13072));
        player.equipment.set(7, new Item(13073));
        player.equipment.set(9, new Item(8842));
        player.equipment.bonuses[11] = 100;
        assertEquals(32, CombatUtils.getMaxDamage(player, AttackStyle.RANGED, AttackType.ACCURATE));
        assertEquals(121 * 64, CombatUtils.getAttackBonus(player, AttackStyle.RANGED, AttackType.ACCURATE), 0);
    }

    @Test public void dharokUsesBaseHpAndDoesNotBoostAccuracy() {
        Fighter player = new Fighter(), target = new Fighter();
        player.equipment.set(0, new Item(4880));
        player.equipment.set(4, new Item(4720));
        player.equipment.set(7, new Item(4722));
        player.equipment.set(3, new Item(4718));
        player.stats.get(StatType.Hitpoints).currentLevel = 1;
        Hit hit = new Hit(player, AttackStyle.SLASH, AttackType.AGGRESSIVE).fixedDamage(50).ignoreDefence();
        assertTrue(SetEffect.DHAROK.checkAndApply(player, target, hit));
        assertEquals(0, hit.getAttackBoost(), 0);
        hit.defend(target);
        assertEquals(98, hit.damage);
    }

    @Test public void warhammerMissDoesNotDrainAndSuccessUsesCurrentDefence() {
        Fighter player = new Fighter(), target = new Fighter() {
            @Override public int hit(Hit... hits) {
                for(Hit hit : hits) hit.block();
                return 0;
            }
        };
        new io.ruin.model.combat.special.melee.DragonWarhammer().handle(player, target, AttackStyle.CRUSH, AttackType.AGGRESSIVE, 50);
        assertEquals(99, target.stats.get(StatType.Defence).currentLevel);
        Fighter guaranteedTarget = new Fighter() {
            @Override public int hit(Hit... hits) { hits[0].damage = 10; return 10; }
        };
        guaranteedTarget.stats.get(StatType.Defence).currentLevel = 70;
        new io.ruin.model.combat.special.melee.DragonWarhammer().handle(player, guaranteedTarget, AttackStyle.CRUSH, AttackType.AGGRESSIVE, 50);
        assertEquals(49, guaranteedTarget.stats.get(StatType.Defence).currentLevel);
    }

    @Test public void pvpHasNoHiddenAccuracyBoostAndDaggerCostIsCurrent() {
        assertEquals(1, Hit.PVP_MAGIC_ACCURACY_MODIFIER, 0);
        assertEquals(1, Hit.PVP_MELEE_ACCURACY_MODIFIER, 0);
        assertEquals(25, new io.ruin.model.combat.special.melee.AbyssalDagger().getDrainAmount());
    }

    @Test public void specialMultipliersStackAfterEquipmentAndFloorEachStep() {
        Fighter player = new Fighter(), target = new Fighter();
        player.equipment.bonuses[0] = 100;
        Hit hit = new Hit(player, AttackStyle.STAB, AttackType.ACCURATE)
                .randDamage(50).multiplyDamage(7.0 / 6).specialDamage(1.1).specialDamage(1.25)
                .multiplyAttack(7.0 / 6).specialAccuracy(1.15).ignoreDefence();
        assertEquals(24202, hit.getModifiedAttackBonus(), 0);
        hit.defend(target);
        assertEquals(78, hit.maxDamage);
        assertEquals(115, CombatUtils.floorScale(100, 1.15), 0);
    }

    @Test public void magicDamagePreservesFractionsAndAddsPrayerAndEliteVoid() {
        Fighter player = new Fighter();
        player.equipment.magicDamageBonus = .5;
        assertEquals(201, CombatUtils.getMagicMaxDamage(player, 200));
        player.prayer.magicDamageBoost = 4;
        assertEquals(209, CombatUtils.getMagicMaxDamage(player, 200));
        player.equipment.set(0, new Item(11663));
        player.equipment.set(4, new Item(13072));
        player.equipment.set(7, new Item(13073));
        player.equipment.set(9, new Item(8842));
        assertEquals(219, CombatUtils.getMagicMaxDamage(player, 200));
    }

    @Test public void drainedTridentUsesCurrentLevelAndFloorsMaxHit() {
        Fighter player = new Fighter();
        player.equipment.magicDamageBonus = 5;
        assertEquals(29, player.combat.getTridentMaxDamage(false));
        assertEquals(32, player.combat.getTridentMaxDamage(true));
        player.stats.get(StatType.Magic).currentLevel = 60;
        assertEquals(15, player.combat.getTridentMaxDamage(false));
        assertEquals(18, player.combat.getTridentMaxDamage(true));
    }

    @Test public void magicDefenceFloorsEachWeightedComponent() {
        Fighter player = new Fighter();
        assertEquals(106 * 64, CombatUtils.getDefenceBonus(player, AttackStyle.MAGIC), 0);
        player.stats.get(StatType.Magic).currentLevel = 90;
        player.stats.get(StatType.Defence).currentLevel = 90;
        assertEquals(98 * 64, CombatUtils.getDefenceBonus(player, AttackStyle.MAGIC), 0);
    }

    @Test public void twistedBowCanReduceDamageAndCapsEachMultiplier() {
        assertEquals(.40, CombatUtils.twistedBowMultiplier(1, true), .000001);
        assertEquals(.54, CombatUtils.twistedBowMultiplier(1, false), .000001);
        assertEquals(1.4, CombatUtils.twistedBowMultiplier(350, true), .000001);
        assertEquals(2.48, CombatUtils.twistedBowMultiplier(350, false), .000001);
    }

    @Test public void clawsDistributeAllFourAccuracyOutcomesAndRollWithoutBonus() {
        assertArrayEquals(new int[]{40, 20, 10, 11}, CombatUtils.splitClawDamage(80, 0));
        assertArrayEquals(new int[]{40, 20, 21, 0}, CombatUtils.splitClawDamage(80, 1));
        assertArrayEquals(new int[]{40, 41, 0, 0}, CombatUtils.splitClawDamage(80, 2));
        assertArrayEquals(new int[]{81, 0, 0, 0}, CombatUtils.splitClawDamage(80, 3));
        Fighter player = new Fighter(), target = new Fighter();
        for (int i = 0; i < 20; i++) {
            Hit hit = new Hit(player, AttackStyle.SLASH, AttackType.AGGRESSIVE).randDamage(50).clawAttack().ignoreDefence();
            hit.defend(target);
            assertTrue(hit.damage >= 25 && hit.damage <= 49);
            assertEquals(0, hit.getAttackBoost(), 0);
            assertEquals(hit.getClawDamage(2) + 1, hit.getClawDamage(3));
        }
    }

    @Test public void snapshotUsesOnlyVisibleLevelAndAmmo() {
        assertEquals(20, CombatUtils.getSnapshotMaxDamage(99, 55));
        Fighter player = new Fighter(), target = new Fighter();
        Hit hit = new Hit(player, AttackStyle.RANGED, AttackType.ACCURATE).randDamage(20).baseDamageOnly()
                .boostDamage(.5).multiplyDamage(1.15).ignoreDefence();
        hit.defend(target);
        assertEquals(20, hit.maxDamage);
    }

    @Test public void elysianRoundsTheReductionRatherThanRemainingDamage() {
        assertEquals(6, CombatUtils.getElysianDamage(7));
        assertEquals(0, CombatUtils.getElysianDamage(1));
        assertEquals(75, CombatUtils.getElysianDamage(100));
    }
}
