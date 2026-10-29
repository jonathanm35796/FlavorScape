package io.ruin.model.combat;

import io.ruin.model.inter.handlers.EquipmentStats;
import io.ruin.model.item.Item;
import io.ruin.model.skills.magic.spells.ancient.IceBarrage;
import io.ruin.model.skills.prayer.Prayer;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MagicPvpAccuracyTest {
    @BeforeClass public static void data() throws Exception {
        CombatEquipmentInteractionTest.data();
    }

    private static CombatEquipmentInteractionTest.Fighter mage() {
        CombatEquipmentInteractionTest.Fighter p = new CombatEquipmentInteractionTest.Fighter();
        p.equipment.set(3, new Item(4675));
        p.equipment.set(4, new Item(4712));
        p.equipment.set(7, new Item(4714));
        p.equipment.set(2, new Item(12002));
        p.equipment.set(1, new Item(21795));
        p.equipment.recalculateBonuses();
        return p;
    }

    private static CombatEquipmentInteractionTest.Fighter ranger() {
        CombatEquipmentInteractionTest.Fighter p = new CombatEquipmentInteractionTest.Fighter();
        p.equipment.set(4, new Item(11828));
        p.equipment.set(7, new Item(11830));
        p.equipment.set(2, new Item(6585));
        p.equipment.set(5, new Item(30191));
        p.equipment.recalculateBonuses();
        return p;
    }

    @Test public void listedItemsSupplyTheirRealBonusesToCombat() {
        assertEquals(94, mage().equipment.bonuses[EquipmentStats.MAGIC_ATTACK]);
        assertEquals(127, ranger().equipment.bonuses[EquipmentStats.MAGIC_DEFENCE]);
    }

    private static void checkBarrage(CombatEquipmentInteractionTest.Fighter attacker,
                                      CombatEquipmentInteractionTest.Fighter defender,
                                      int attackRoll, int defenceRoll) {
        assertEquals(attackRoll, CombatUtils.getAttackBonus(attacker, AttackStyle.MAGIC, null), 0);
        assertEquals(defenceRoll, CombatUtils.getDefenceBonus(defender, AttackStyle.MAGIC), 0);
        double expected = attackRoll / (2.0 * (defenceRoll + 1));
        int landed = 0;
        IceBarrage spell = new IceBarrage();
        int casts = 30000;
        for (int i = 0; i < casts; i++) {
            Hit hit = new Hit(attacker, AttackStyle.MAGIC, null).randDamage(30).setAttackSpell(spell);
            hit.defend(defender);
            if (!hit.isBlocked()) landed++;
        }
        // Exercise production PvP listeners and accuracy rolls, including any hidden modifiers.
        assertEquals(expected, landed / (double) casts, .02);
        System.out.printf("Barrage: attack=%d defence=%d expected=%.4f observed=%.4f%n",
                attackRoll, defenceRoll, expected, landed / (double) casts);
    }

    @Test public void displayedBonusesGiveExpectedPvpBarrageAccuracy() {
        CombatEquipmentInteractionTest.Fighter attacker = mage(), defender = ranger();
        // Include the remaining equipment represented by the user's displayed totals.
        attacker.equipment.bonuses[EquipmentStats.MAGIC_ATTACK] = 105;
        defender.equipment.bonuses[EquipmentStats.MAGIC_DEFENCE] = 174;
        checkBarrage(attacker, defender, 18252, 25228);
        attacker.prayer.toggle(Prayer.AUGURY);
        checkBarrage(attacker, defender, 22308, 25228);
        defender.prayer.toggle(Prayer.AUGURY);
        checkBarrage(attacker, defender, 22308, 30940);
    }

    @Test public void switchingGearChangesMagicDefenceAndMeleeAccuracy() {
        CombatEquipmentInteractionTest.Fighter defender = ranger();
        double rangedMagicDefence = CombatUtils.getDefenceBonus(defender, AttackStyle.MAGIC);
        defender.equipment.set(4, new Item(1127));
        defender.equipment.set(7, new Item(1079));
        defender.equipment.recalculateBonuses();
        assertTrue(rangedMagicDefence > CombatUtils.getDefenceBonus(defender, AttackStyle.MAGIC));
        double plateSlashDefence = CombatUtils.getDefenceBonus(defender, AttackStyle.SLASH);
        defender.equipment.set(4, new Item(4712));
        defender.equipment.set(7, new Item(4714));
        defender.equipment.recalculateBonuses();
        assertTrue(plateSlashDefence > CombatUtils.getDefenceBonus(defender, AttackStyle.SLASH));
        CombatEquipmentInteractionTest.Fighter attacker = mage();
        attacker.equipment.set(3, new Item(11802));
        attacker.equipment.recalculateBonuses();
        double robeMeleeAccuracy = CombatUtils.getAttackBonus(attacker, AttackStyle.SLASH, AttackType.AGGRESSIVE);
        attacker.equipment.set(4, new Item(11828));
        attacker.equipment.set(7, new Item(11830));
        attacker.equipment.recalculateBonuses();
        assertTrue(robeMeleeAccuracy > CombatUtils.getAttackBonus(attacker, AttackStyle.SLASH, AttackType.AGGRESSIVE));
    }
}
