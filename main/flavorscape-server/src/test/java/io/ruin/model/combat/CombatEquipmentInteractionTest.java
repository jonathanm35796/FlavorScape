package io.ruin.model.combat;

import io.ruin.cache.ItemDef;
import io.ruin.data.impl.items.*;
import io.ruin.data.impl.npcs.npc_combat;
import io.ruin.model.activities.miscpvm.BasicCombat;
import io.ruin.model.entity.npc.*;
import io.ruin.model.entity.player.*;
import io.ruin.model.entity.shared.masks.Appearance;
import io.ruin.model.item.Item;
import io.ruin.model.skills.prayer.Prayer;
import io.ruin.model.stat.StatType;
import java.nio.file.*;
import org.junit.*;
import static org.junit.Assert.*;

public class CombatEquipmentInteractionTest {
    @BeforeClass public static void data() throws Exception {
        io.ruin.tools.ItemCacheFixture.load();
        new weapon_types().fromJson("weapons", Files.readString(Path.of("data/items/weapon_types.json")));
        new shield_types().fromJson("shields", Files.readString(Path.of("data/items/shield_types.json")));
        new item_info().fromJson("items", Files.readString(Path.of("data/items/item_info.json")));
        Class.forName("io.ruin.model.item.actions.impl.chargable.Arclight");
    }
    static class Fighter extends CombatCalculationTest.Fighter {
        final Appearance appearance = new Appearance();
        final PlayerMovement movement = new PlayerMovement(this);
        final io.ruin.network.PacketSender sender = new io.ruin.network.PacketSender(this, null) {
            @Override public void write(io.ruin.api.buffer.OutBuffer packet) {}
        };
        Fighter() {
            equipment.init(this,14,-1,-1,-1,false);
            prayer.init(this);
            tempUseRaidPrayers = true;
            combat.init(this);
        }
        @Override public void updateVarp(int id) {}
        @Override public Appearance getAppearance() { return appearance; }
        @Override public PlayerMovement getMovement() { return movement; }
        @Override public io.ruin.network.PacketSender getPacketSender() { return sender; }
        @Override public int setHp(int hp) { return stats.get(StatType.Hitpoints).alter(hp); }
        @Override public void privateSound(int id, int type, int delay) {}
    }
    static class MonsterCombat extends BasicCombat {
        MonsterCombat(NPC npc) {
            npc_combat.Info info = new npc_combat.Info();
            info.attack = info.strength = info.ranged = info.magic = info.defence = 99;
            info.hitpoints = 1000;
            init(npc, info);
        }
    }
    static class Monster extends NPC {
        final MonsterCombat combat = new MonsterCombat(this);
        Monster() { super(1); }
        @Override public NPCCombat getCombat() { return combat; }
        @Override public int getHp() { return 1000; }
        @Override public int hit(Hit... hits) { int sum = 0; for(Hit hit : hits) { hit.defend(this); sum += hit.damage; } return sum; }
        @Override public void graphics(int id, int height, int delay) {}
    }
    private static void gear(Fighter player, int head, int body, int legs, int weapon) {
        player.equipment.set(0, new Item(head)); player.equipment.set(4, new Item(body));
        player.equipment.set(7, new Item(legs)); player.equipment.set(3, new Item(weapon));
    }

    @Test public void realPrayerActivationChangesAccuracyStrengthAndDefenceWithoutStacking() {
        Fighter player = new Fighter();
        double base = CombatUtils.getAttackBonus(player, AttackStyle.SLASH, AttackType.ACCURATE);
        player.prayer.toggle(Prayer.PIETY);
        assertTrue(CombatUtils.getAttackBonus(player, AttackStyle.SLASH, AttackType.ACCURATE) > base);
        assertEquals(.20, player.prayer.attackBoost, .000001);
        assertEquals(.23, player.prayer.strengthBoost, .000001);
        assertEquals(.25, player.prayer.defenceBoost, .000001);
        player.prayer.toggle(Prayer.AUGURY);
        assertFalse(player.prayer.isActive(Prayer.PIETY));
        assertEquals(0, player.prayer.strengthBoost, .000001);
        assertEquals(.25, player.prayer.magicBoost, .000001);
        assertEquals(4, player.prayer.magicDamageBoost, .000001);
        player.prayer.deactivateProtectionPrayer();
        assertEquals(.25, player.prayer.defenceBoost, .000001);
        player.prayer.deactivateAll();
        assertEquals(0, player.prayer.magicBoost, .000001);
    }
    @Test public void protectionPrayersReducePvpAndBlockOrdinaryPvm() {
        Fighter defender = new Fighter();
        defender.prayer.toggle(Prayer.PROTECT_FROM_MELEE);
        Hit pvp = new Hit(new Fighter(), AttackStyle.SLASH, AttackType.AGGRESSIVE).fixedDamage(50).ignoreDefence();
        defender.hit(pvp);
        assertEquals(30, pvp.damage);
        Hit pvm = new Hit(new Monster(), AttackStyle.SLASH).fixedDamage(50).ignoreDefence();
        defender.hit(pvm);
        assertEquals(0, pvm.damage);
        Hit wrongPrayer = new Hit(new Fighter(), AttackStyle.MAGIC).fixedDamage(50).ignoreDefence();
        defender.hit(wrongPrayer);
        assertEquals(50, wrongPrayer.damage);
    }
    @Test public void spectralHalvesNpcAttackDrainButNotPvpOrOrdinaryPrayerUse() {
        Fighter player = new Fighter(); player.equipment.set(5, new Item(12821));
        assertEquals(15, EquipmentCombatEffects.prayerDrain(player, 30, new Monster()));
        assertEquals(30, EquipmentCombatEffects.prayerDrain(player, 30, new Fighter()));
        assertEquals(30, EquipmentCombatEffects.prayerDrain(player, 30, null));
        assertEquals(15, player.prayer.drainFromAttack(30, new Monster()));
        assertEquals(84, player.stats.get(StatType.Prayer).currentLevel);
        assertEquals(30, player.prayer.drain(30));
        assertEquals(54, player.stats.get(StatType.Prayer).currentLevel);
        assertEquals(0, player.prayer.drain(-10));
        assertEquals(54, player.stats.get(StatType.Prayer).currentLevel);
    }
    @Test public void elysianRollHasCorrectBoundaryAndIgnoresPoisonAndDragonfire() {
        Fighter player = new Fighter(); player.equipment.set(5, new Item(12817));
        Hit hit = new Hit(new Fighter(), AttackStyle.CRUSH).fixedDamage(100); hit.damage = 100;
        EquipmentCombatEffects.defend(player, hit, () -> .69999);
        assertEquals(75, hit.damage);
        hit.damage = 100; EquipmentCombatEffects.defend(player, hit, () -> .70);
        assertEquals(100, hit.damage);
        hit.attackStyle = AttackStyle.DRAGONFIRE; EquipmentCombatEffects.defend(player, hit, () -> 0);
        assertEquals(100, hit.damage);
        hit.attackStyle = AttackStyle.CRUSH; hit.type = HitType.POISON;
        EquipmentCombatEffects.defend(player, hit, () -> 0);
        assertEquals(100, hit.damage);
    }
    @Test public void justiciarStacksAdditivelyWithElysianAndIsDisabledInPvp() {
        Fighter player = new Fighter(); gear(player,22326,22327,22328,4151);
        player.equipment.set(5, new Item(12817)); player.equipment.bonuses[7] = 450;
        Hit pvm = new Hit(new Monster(), AttackStyle.CRUSH); pvm.damage = 100;
        EquipmentCombatEffects.defend(player, pvm, () -> 0);
        assertEquals(60, pvm.damage);
        Hit pvp = new Hit(new Fighter(), AttackStyle.CRUSH); pvp.damage = 100;
        EquipmentCombatEffects.defend(player, pvp, () -> .9);
        assertEquals(100, pvp.damage);
        player.equipment.set(5,null); pvm.damage = 1;
        EquipmentCombatEffects.defend(player, pvm, () -> .9);
        assertEquals(0, pvm.damage);
    }
    @Test public void allBarrowsSetsRequireTheirWeaponAndWorkingPieces() {
        Fighter player = new Fighter(), target = new Fighter();
        gear(player,4708,4712,4714,4710);
        Hit magic = new Hit(player,AttackStyle.MAGIC);
        assertTrue(SetEffect.AHRIM.checkAndApply(player,target,magic,0));
        assertEquals(94,target.stats.get(StatType.Strength).currentLevel);
        magic.block(); SetEffect.AHRIM.checkAndApply(player,target,magic,0);
        assertEquals(94,target.stats.get(StatType.Strength).currentLevel);
        gear(player,4732,4736,4738,4734);
        assertTrue(SetEffect.KARIL.checkAndApply(player,target,new Hit(player,AttackStyle.RANGED),0));
        assertEquals(80,target.stats.get(StatType.Agility).currentLevel);
        gear(player,4745,4749,4751,4747); target.energyUnits = 5000;
        assertTrue(SetEffect.TORAG.checkAndApply(player,target,new Hit(player,AttackStyle.CRUSH),0));
        assertEquals(4000,target.energyUnits,0);
        player.equipment.set(3,new Item(4151));
        assertFalse(SetEffect.TORAG.checkAndApply(player,target,new Hit(player,AttackStyle.CRUSH),0));
    }
    @Test public void bulwarkBlockIsPvmOnlyAndStrengthScalesFromPhysicalDefence() {
        Fighter player = new Fighter(); player.equipment.set(3,new Item(21015));
        player.stance = AttackType.DEFENSIVE;
        Hit hit = new Hit(new Monster(),AttackStyle.CRUSH); hit.damage = 100;
        EquipmentCombatEffects.defend(player,hit,()->.9); assertEquals(80,hit.damage);
        hit.attacker = new Fighter(); hit.damage = 100;
        EquipmentCombatEffects.defend(player,hit,()->.9); assertEquals(100,hit.damage);
        player.stance = AttackType.ACCURATE;
        for(int index : new int[]{5,6,7,9}) player.equipment.bonuses[index]=400;
        assertEquals(28,EquipmentCombatEffects.bulwarkStrength(player));
    }
    @Test public void sanguinestiUsesBoostedMagicEquipmentAndPrayerRatherThanFixed34() {
        Fighter player = new Fighter();
        assertEquals(32,EquipmentCombatEffects.sanguinestiMaxHit(player));
        player.stats.get(StatType.Magic).currentLevel = 120;
        player.equipment.magicDamageBonus=20; player.prayer.magicDamageBoost=4;
        assertEquals(48,EquipmentCombatEffects.sanguinestiMaxHit(player));
    }

    @Test public void actualArmourBonusesFeedMagicAccuracyAndPhysicalDefence() {
        Fighter player = new Fighter();
        gear(player,4708,4712,4714,4710); player.equipment.set(3,null);
        player.equipment.recalculateBonuses();
        assertEquals(58,player.equipment.bonuses[3]);
        assertEquals(13176,CombatUtils.getAttackBonus(player,AttackStyle.MAGIC,null),0);
        double magicRobesDefence = CombatUtils.getDefenceBonus(player,AttackStyle.SLASH);
        gear(player,4745,4749,4751,4747); player.equipment.set(3,null);
        player.equipment.recalculateBonuses();
        assertEquals(-57,player.equipment.bonuses[3]);
        assertEquals(756,CombatUtils.getAttackBonus(player,AttackStyle.MAGIC,null),0);
        assertTrue(CombatUtils.getDefenceBonus(player,AttackStyle.SLASH)>magicRobesDefence);
        player.equipment.set(5,new Item(1187)); player.equipment.recalculateBonuses();
        assertEquals(108,CombatUtils.getAttackBonus(player,AttackStyle.MAGIC,null),0);
        player.equipment.set(10,new Item(4131)); player.equipment.recalculateBonuses();
        assertEquals(0,CombatUtils.getAttackBonus(player,AttackStyle.MAGIC,null),0);
    }
    @Test public void prayerBonusMakesActivePrayerLastLonger() {
        Fighter normal = new Fighter(), boosted = new Fighter();
        boosted.equipment.bonuses[13]=15;
        normal.prayer.toggle(Prayer.PROTECT_FROM_MELEE); boosted.prayer.toggle(Prayer.PROTECT_FROM_MELEE);
        for(int tick=0;tick<60;tick++){normal.prayer.process();boosted.prayer.process();}
        assertEquals(87,normal.stats.get(StatType.Prayer).currentLevel);
        assertEquals(91,boosted.stats.get(StatType.Prayer).currentLevel);
    }
    @Test public void arclightConsumesSuccessesButNotMissesOrSpecialAttacks() {
        Fighter player=new Fighter(); Monster monster=new Monster();
        Item weapon=new Item(19675); player.equipment.set(3,weapon);
        io.ruin.model.item.attributes.AttributeExtensions.setCharges(weapon,3);
        Hit success=new Hit(player,AttackStyle.SLASH,AttackType.AGGRESSIVE).randDamage(20).ignoreDefence().setAttackWeapon(weapon.getDef());
        monster.hit(success);
        assertEquals(2,io.ruin.model.item.attributes.AttributeExtensions.getCharges(weapon));
        Hit miss=new Hit(player,AttackStyle.SLASH,AttackType.AGGRESSIVE).randDamage(20).setAttackWeapon(weapon.getDef()); miss.block(); monster.hit(miss);
        assertEquals(2,io.ruin.model.item.attributes.AttributeExtensions.getCharges(weapon));
        monster.hit(new Hit(player,AttackStyle.SLASH,AttackType.AGGRESSIVE).randDamage(20).ignoreDefence().specialAttack());
        assertEquals(2,io.ruin.model.item.attributes.AttributeExtensions.getCharges(weapon));
    }
    @Test public void damnedEnablesToragDefenceGuthanOverhealAndVeracPrayer() {
        Fighter player=new Fighter(),target=new Fighter();player.equipment.set(2,new Item(12851));
        gear(player,4745,4749,4751,4747);player.stats.get(StatType.Hitpoints).currentLevel=1;
        assertEquals(197,EquipmentCombatEffects.toragDefence(player,99),0);
        gear(player,4724,4728,4730,4726);player.stats.get(StatType.Hitpoints).currentLevel=99;
        Hit heal=new Hit(player,AttackStyle.STAB);heal.damageForHealing=30;
        SetEffect.GUTHAN.checkAndApply(player,target,heal,0);
        assertEquals(109,player.getHp());
        gear(player,4753,4757,4759,4755);player.equipment.recalculateBonuses();
        assertEquals(28,player.equipment.bonuses[13]);
        assertEquals(2,EquipmentCombatEffects.recoilDamage(10));
        assertEquals(1,EquipmentCombatEffects.recoilDamage(1));
    }
    @Test public void chaosGauntletsAndChargeModifySpellBaseBeforeEquipment() {
        Fighter player=new Fighter();player.equipment.set(9,new Item(777));
        io.ruin.model.skills.magic.spells.modern.FireBolt bolt=new io.ruin.model.skills.magic.spells.modern.FireBolt();
        assertEquals(15,bolt.getBaseMaxDamage(player));
        player.combat.chargeTicks=100;player.equipment.set(1,new Item(2413));
        io.ruin.model.skills.magic.spells.modern.ClawsOfGuthix claws=new io.ruin.model.skills.magic.spells.modern.ClawsOfGuthix();
        assertEquals(30,claws.getBaseMaxDamage(player));
        player.equipment.magicDamageBonus=15;
        assertEquals(34,CombatUtils.getMagicMaxDamage(player,claws.getBaseMaxDamage(player)));
        Hit hit=new Hit(player,AttackStyle.MAGIC);hit.damage=1;claws.afterHit(hit,player);
        assertEquals(94,player.stats.get(StatType.Defence).currentLevel);
        assertEquals(99,player.stats.get(StatType.Magic).currentLevel);
    }
    @Test public void allEnchantedBoltFamiliesHaveHandlersAndUnenchantedBoltsCannotProc() {
        for(RangedAmmo ammo:new RangedAmmo[]{RangedAmmo.OPAL_BOLTS,RangedAmmo.JADE_BOLTS,RangedAmmo.PEARL_BOLTS,RangedAmmo.TOPAZ_BOLTS,
                RangedAmmo.SAPPHIRE_BOLTS,RangedAmmo.EMERALD_BOLTS,RangedAmmo.RUBY_BOLTS,RangedAmmo.DIAMOND_BOLTS,RangedAmmo.DRAGONSTONE_BOLTS,RangedAmmo.ONYX_BOLTS,
                RangedAmmo.DRAGON_OPAL_BOLTS,RangedAmmo.DRAGON_JADE_BOLTS,RangedAmmo.DRAGON_PEARL_BOLTS,RangedAmmo.DRAGON_TOPAZ_BOLTS}) assertNotNull(ammo.effect);
        Hit unenchanted=new Hit(new Fighter(),AttackStyle.RANGED).setRangedAmmo(ItemDef.get(879));
        assertFalse(unenchanted.rollBoltEffect(100));
        Hit enchanted=new Hit(new Fighter(),AttackStyle.RANGED).setRangedAmmo(ItemDef.get(9236));
        assertTrue(enchanted.rollBoltEffect(100));
    }

    @Test public void dragonfireShieldDefenceStaysFullRegardlessOfStoredCharges() {
        Fighter player = new Fighter();
        for (int id : new int[]{11283,21633,22002}) {
            Item shield = new Item(id); player.equipment.set(5,shield);
            io.ruin.model.item.attributes.AttributeExtensions.setCharges(shield,50);
            player.equipment.recalculateBonuses();
            int[] full = player.equipment.bonuses.clone();
            for (int charges : new int[]{1,0}) {
                io.ruin.model.item.attributes.AttributeExtensions.setCharges(shield,charges);
                player.equipment.recalculateBonuses();
                assertArrayEquals(full,player.equipment.bonuses);
            }
        }
    }
    @Test public void venomAndPoisonRespectAttackTypeSuccessAndPvpSerpentineRules() {
        Fighter player = new Fighter(), opponent = new Fighter(); Monster monster = new Monster();
        player.equipment.set(0,new Item(12931)); player.equipment.set(3,new Item(12899));
        Hit melee = new Hit(player,AttackStyle.SLASH); melee.damage=10;
        assertEquals(1.0/6,EquipmentCombatEffects.venomChance(player,monster,melee),0);
        assertEquals(0,EquipmentCombatEffects.venomChance(player,opponent,melee),0);
        Hit magic = new Hit(player,AttackStyle.MAGIC); magic.damage=10;
        assertEquals(1,EquipmentCombatEffects.venomChance(player,monster,magic),0);
        assertEquals(.25,EquipmentCombatEffects.venomChance(player,opponent,magic),0);
        magic.block(); assertEquals(0,EquipmentCombatEffects.venomChance(player,monster,magic),0);
        player.equipment.set(3,new Item(5698)); // dragon dagger(p++)
        assertEquals(6,EquipmentCombatEffects.poisonDamage(player,melee));
        Hit ranged = new Hit(player,AttackStyle.RANGED).setRangedAmmo(ItemDef.get(892)); ranged.damage=10;
        assertEquals(0,EquipmentCombatEffects.poisonDamage(player,ranged));
        melee.block(); assertEquals(0,EquipmentCombatEffects.poisonDamage(player,melee));
    }
    @Test public void actualWeaponsStrengthBonusesAndPrayerFeedMeleeAndRangedCalculations() {
        Fighter player = new Fighter(); player.stance=AttackType.ACCURATE;
        player.equipment.set(3,new Item(4151)); player.equipment.recalculateBonuses();
        double whipAccuracy=CombatUtils.getAttackBonus(player,AttackStyle.SLASH,AttackType.ACCURATE);
        int whipMax=CombatUtils.getMaxDamage(player,AttackStyle.SLASH,AttackType.ACCURATE);
        player.equipment.set(2,new Item(6585)); player.equipment.recalculateBonuses();
        assertTrue(CombatUtils.getAttackBonus(player,AttackStyle.SLASH,AttackType.ACCURATE)>whipAccuracy);
        assertTrue(CombatUtils.getMaxDamage(player,AttackStyle.SLASH,AttackType.ACCURATE)>whipMax);
        player.prayer.toggle(Prayer.PIETY);
        assertTrue(CombatUtils.getMaxDamage(player,AttackStyle.SLASH,AttackType.ACCURATE)>whipMax);
        player.equipment.set(3,new Item(9185)); player.equipment.set(13,new Item(9140)); player.equipment.recalculateBonuses();
        int bolts=CombatUtils.getMaxDamage(player,AttackStyle.RANGED,AttackType.ACCURATE);
        player.equipment.set(13,new Item(9244)); player.equipment.recalculateBonuses();
        assertTrue(CombatUtils.getMaxDamage(player,AttackStyle.RANGED,AttackType.ACCURATE)>bolts);
        player.prayer.toggle(Prayer.RIGOUR);
        assertTrue(CombatUtils.getMaxDamage(player,AttackStyle.RANGED,AttackType.ACCURATE)>bolts);
    }
    @Test public void anchorDrainsDefenceFirstThenAttackAndSupportsNpcs() {
        Fighter player = new Fighter(); Monster monster = new Monster();
        io.ruin.model.combat.special.melee.BarrelchestAnchor.drain(player,6);
        assertEquals(93,player.stats.get(StatType.Defence).currentLevel);
        assertEquals(99,player.stats.get(StatType.Attack).currentLevel);
        player.stats.get(StatType.Defence).currentLevel=1;
        io.ruin.model.combat.special.melee.BarrelchestAnchor.drain(player,6);
        assertEquals(93,player.stats.get(StatType.Attack).currentLevel);
        io.ruin.model.combat.special.melee.BarrelchestAnchor.drain(monster,6);
        assertEquals(93,monster.combat.getStat(StatType.Defence).currentLevel);
        io.ruin.model.combat.special.melee.BarrelchestAnchor.drain(monster,0);
        assertEquals(93,monster.combat.getStat(StatType.Defence).currentLevel);
    }
}
