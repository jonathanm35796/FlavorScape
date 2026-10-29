package io.ruin.model.combat;

import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.handlers.EquipmentStats;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.containers.Equipment;
import io.ruin.model.stat.StatList;
import io.ruin.model.stat.StatType;

public class CombatUtils {

    /** Avoid a binary floating-point representation turning an exact integer into n - 1. */
    public static double floorScale(double value, double multiplier) {
        return Math.floor(value * multiplier + 1e-9);
    }

    public static int getSnapshotMaxDamage(int rangedLevel, int ammoStrength) {
        return (int) Math.floor(0.5 + (rangedLevel + 10) * (ammoStrength + 64) / 640.0);
    }

    public static int getElysianDamage(int damage) {
        return Math.max(0, damage - Math.max(1, damage / 4));
    }

    public static int[] splitClawDamage(int total, int successfulRoll) {
        if (successfulRoll == 0)
            return new int[]{total / 2, total / 4, total / 8, total / 8 + 1};
        if (successfulRoll == 1)
            return new int[]{total / 2, total / 4, total / 4 + 1, 0};
        if (successfulRoll == 2)
            return new int[]{total / 2, total / 2 + 1, 0, 0};
        return new int[]{total + 1, 0, 0, 0};
    }

    /**
     * Magic calcs
     */

    /**
     * Attack calcs
     */

    private static double getEffectiveAttack(Entity entity, StatType statType, AttackType attackType) {
        double effectiveAttack = entity.getCombat().getLevel(statType);
        if(statType == StatType.Magic) {
            if(entity.player != null) {
                effectiveAttack *= (1D + entity.player.getPrayer().magicBoost);
            }
            effectiveAttack = Math.floor(effectiveAttack + 1e-9);
            effectiveAttack += entity.player == null || attackType != AttackType.ACCURATE ? 1 : 3;
        } else if(statType == StatType.Ranged) {
            if(entity.player != null)
                effectiveAttack *= (1D + entity.player.getPrayer().rangedAttackBoost);
            effectiveAttack = Math.floor(effectiveAttack + 1e-9);
            if(entity.player == null)
                effectiveAttack += 1;
            else if(attackType == AttackType.ACCURATE)
                effectiveAttack += 3;
        } else {
            if(entity.player != null)
                effectiveAttack *= (1D + entity.player.getPrayer().attackBoost);
            effectiveAttack = Math.floor(effectiveAttack + 1e-9);
            if(entity.player == null)
                effectiveAttack += 1;
            else if(attackType == AttackType.ACCURATE)
                effectiveAttack += 3;
            else if(attackType == AttackType.CONTROLLED)
                effectiveAttack += 1;
        }
        effectiveAttack += 8;
        if(entity.player != null) {
            if(statType == StatType.Magic && hasVoid(entity.player, 11663))
                effectiveAttack = floorScale(effectiveAttack, 1.45);
            else if((statType == StatType.Ranged && hasVoid(entity.player, 11664))
                    || (statType == StatType.Attack && hasVoid(entity.player, 11665)))
                effectiveAttack = floorScale(effectiveAttack, 1.1);
        }
        return effectiveAttack;
    }

    public static double getAttackBonus(Entity entity, AttackStyle attackStyle, AttackType attackType) {
        double effectiveAttack, bonus = 0;
        if(attackStyle == AttackStyle.MAGIC) {
            effectiveAttack = getEffectiveAttack(entity, StatType.Magic, attackType);
            bonus = entity.getCombat().getBonus(EquipmentStats.MAGIC_ATTACK);
        } else if(attackStyle == AttackStyle.RANGED || attackStyle == AttackStyle.MAGICAL_RANGED) {
            effectiveAttack = getEffectiveAttack(entity, StatType.Ranged, attackType);
            bonus = entity.getCombat().getBonus(EquipmentStats.RANGE_ATTACK);
        } else {
            effectiveAttack = getEffectiveAttack(entity, StatType.Attack, attackType);
            if(attackStyle == AttackStyle.STAB)
                bonus = entity.getCombat().getBonus(EquipmentStats.STAB_ATTACK);
            else if(attackStyle == AttackStyle.SLASH)
                bonus = entity.getCombat().getBonus(EquipmentStats.SLASH_ATTACK);
            else if(attackStyle == AttackStyle.CRUSH)
                bonus = entity.getCombat().getBonus(EquipmentStats.CRUSH_ATTACK);
            else if(attackStyle == AttackStyle.MAGICAL_MELEE)
                bonus = 0; // i know this was implied before but im leaving it explicit just to make it clear
        }
        return Math.max(0, effectiveAttack * (bonus + 64D));
    }

    /**
     * Defence calcs
     */

    private static double getEffectiveDefence(Entity entity) {
        AttackType type = entity.getCombat().getAttackType();
        double effectiveDefence = entity.getCombat().getLevel(StatType.Defence);
        if(entity.player != null)
            effectiveDefence *= (1D + entity.player.getPrayer().defenceBoost);
        effectiveDefence = Math.floor(effectiveDefence + 1e-9);
        if (entity.player != null) effectiveDefence = EquipmentCombatEffects.toragDefence(entity.player, effectiveDefence);
        if(entity.player == null)
            effectiveDefence += 1;
        else if(type != null) {
            if(type == AttackType.DEFENSIVE || type == AttackType.LONG_RANGED)
                effectiveDefence += 3;
            else if(type == AttackType.CONTROLLED)
                effectiveDefence += 1;
        }
        effectiveDefence += 8;
        return effectiveDefence;
    }

    private static double getEffectiveMagicDefence(Entity entity) {
        if(entity.player == null) {
            String name = entity.npc == null ? "" : entity.npc.getDef().name;
            StatType level = "Verzik Vitur".equalsIgnoreCase(name) || "Ice demon".equalsIgnoreCase(name)
                    ? StatType.Defence : StatType.Magic;
            return entity.getCombat().getLevel(level) + 9;
        }
        double magic = floorScale(entity.getCombat().getLevel(StatType.Magic), 1D + entity.player.getPrayer().magicBoost);
        double defence = floorScale(entity.getCombat().getLevel(StatType.Defence), 1D + entity.player.getPrayer().defenceBoost);
        defence = EquipmentCombatEffects.toragDefence(entity.player, defence);
        AttackType type = entity.getCombat().getAttackType();
        int style = type == AttackType.DEFENSIVE || type == AttackType.LONG_RANGED ? 3
                : type == AttackType.CONTROLLED ? 1 : 0;
        return Math.floor(magic * 7 / 10) + Math.floor(defence * 3 / 10) + style + 8;
    }

    public static double getDefenceBonus(Entity entity, AttackStyle attackStyle) {
        double effectiveDefence = getEffectiveDefence(entity);
        double bonus = 0;
        if(attackStyle == AttackStyle.MAGIC || attackStyle == AttackStyle.MAGICAL_RANGED || attackStyle == AttackStyle.MAGICAL_MELEE) {
            effectiveDefence = getEffectiveMagicDefence(entity);
            bonus = entity.getCombat().getBonus(EquipmentStats.MAGIC_DEFENCE);
        } else {
            if(attackStyle == AttackStyle.STAB)
                bonus = entity.getCombat().getBonus(EquipmentStats.STAB_DEFENCE);
            else if(attackStyle == AttackStyle.SLASH)
                bonus = entity.getCombat().getBonus(EquipmentStats.SLASH_DEFENCE);
            else if(attackStyle == AttackStyle.CRUSH)
                bonus = entity.getCombat().getBonus(EquipmentStats.CRUSH_DEFENCE);
            else if(attackStyle == AttackStyle.RANGED)
                bonus = entity.getCombat().getBonus(EquipmentStats.RANGE_DEFENCE);
        }
        return Math.max(0, effectiveDefence * (bonus + 64D));
    }

    /**
     * Max damage (Melee & Ranged only!)
     */

    private static double getEffectiveStrength(Entity entity, StatType statType, AttackType attackType) {
        double prayerBonus = 1, styleBonus = entity.player == null ? 1 : 0;
        double effectiveStrength = entity.getCombat().getLevel(statType);
        if(statType == StatType.Ranged) {
            if(entity.player != null)
                prayerBonus = (1D + entity.player.getPrayer().rangedStrengthBoost);
            if(attackType == AttackType.ACCURATE)
                styleBonus += 3;
        } else {
            if(entity.player != null) {
                prayerBonus = 1D + entity.player.getPrayer().strengthBoost;
                if (attackType == AttackType.AGGRESSIVE) {
                    styleBonus = 3;
                } else if (attackType == AttackType.CONTROLLED) {
                    styleBonus = 1;
                }
            }
        }
        effectiveStrength = (entity.player != null && effectiveStrength <= 20 && Math.abs(prayerBonus - 1.05) < 1e-9
                ? effectiveStrength + 1 : floorScale(effectiveStrength, prayerBonus)) + styleBonus + 8 + entity.getStrAdder();
        if(entity.player != null) {
            if(statType == StatType.Ranged && hasVoid(entity.player, 11664))
                effectiveStrength = floorScale(effectiveStrength, hasEliteVoidBody(entity.player) ? 1.125 : 1.1);
            else if(statType == StatType.Strength && hasVoid(entity.player, 11665))
                effectiveStrength = floorScale(effectiveStrength, 1.1);
        }
        return effectiveStrength;
    }

    public static int getMaxDamage(Entity entity, AttackStyle attackStyle, AttackType attackType) {
        double effectiveStrength, bonus;
        if(attackStyle == AttackStyle.RANGED) {
            effectiveStrength = getEffectiveStrength(entity, StatType.Ranged, attackType);
            bonus = entity.getCombat().getBonus(EquipmentStats.RANGED_STRENGTH);
        } else {
            effectiveStrength = getEffectiveStrength(entity, StatType.Strength, attackType);
            bonus = entity.getCombat().getBonus(EquipmentStats.MELEE_STRENGTH);
            if (entity.player != null) bonus += EquipmentCombatEffects.bulwarkStrength(entity.player);
        }
        return Math.max(0, (int) Math.floor(0.5 + effectiveStrength * (bonus + 64) / 640d));
    }

    private static boolean hasEliteVoidBody(Player player) {
        return player.getEquipment().getId(Equipment.SLOT_CHEST) == 13072
                && player.getEquipment().getId(Equipment.SLOT_LEGS) == 13073;
    }

    public static int getMagicMaxDamage(Entity entity, int baseHit) {
        double percent = entity.getCombat().getBonus(EquipmentStats.MAGIC_DAMAGE);
        if (entity.player != null) {
            percent += entity.player.getPrayer().magicDamageBoost;
            if (hasVoid(entity.player, 11663) && hasEliteVoidBody(entity.player))
                percent += 5;
        }
        return Math.max(0, (int) Math.floor(baseHit * (100 + percent) / 100));
    }

    public static double twistedBowMultiplier(double magic, boolean accuracy) {
        int offset = accuracy ? 10 : 14;
        int cap = accuracy ? 140 : 250;
        double linear = (int) ((3 * magic - offset) / 100);
        double squared = Math.floor(Math.pow(Math.floor(3 * magic / 10) - offset * 10, 2) / 100);
        return Math.max(0, Math.min(cap, cap + linear - squared)) / 100.0;
    }

    private static boolean hasVoid(Player player, int helm) {
        Equipment equipment = player.getEquipment();
        return equipment.getId(Equipment.SLOT_HAT) == helm
                && equipment.getId(Equipment.SLOT_HANDS) == 8842
                && (equipment.getId(Equipment.SLOT_CHEST) == 8839 || equipment.getId(Equipment.SLOT_CHEST) == 13072)
                && (equipment.getId(Equipment.SLOT_LEGS) == 8840 || equipment.getId(Equipment.SLOT_LEGS) == 13073);
    }

    /**
     * Experience
     */

    public static void addXp(Player player, Entity victim, AttackStyle attackStyle, AttackType attackType, int damageDealt) {
        boolean multiplier = victim.npc != null;
        double xp = damageDealt * 4D;
        double monsterMod = 1.0;
        if (multiplier && victim.npc.getId() == 2668)
            monsterMod = victim.npc.getCombat().getInfo().combat_xp_modifier;
        xp *= monsterMod;
        if (xp <= 0)
            return;
        if(attackStyle.isMagic()) {//this can pretty much only happen from trident!!
            xp /= 2;
            if(player.showHitAsExperience()) {
                player.getStats().addXp(StatType.Magic, damageDealt, false);
                return;
            }
            switch(attackType) {
                case ACCURATE:
                    player.getStats().addXp(StatType.Magic, xp, multiplier);
                    break;
                case DEFENSIVE:
                    xp /= 2;
                    player.getStats().addXp(StatType.Magic, xp, multiplier);
                    player.getStats().addXp(StatType.Defence, xp, multiplier);
                    break;
            }
        } else if(attackStyle.isRanged()) {
            if(player.showHitAsExperience()) {
                player.getStats().addXp(StatType.Ranged, damageDealt, false);
                return;
            }
            switch(attackType) {
                case LONG_RANGED:
                    xp /= 2;
                    player.getStats().addXp(StatType.Ranged, xp, multiplier);
                    player.getStats().addXp(StatType.Defence, xp, multiplier);
                    break;
                default:
                    player.getStats().addXp(StatType.Ranged, xp, multiplier);
                    break;
            }
        } else {
            switch(attackType) {
                case ACCURATE:
                    if(player.showHitAsExperience()) {
                        player.getStats().addXp(StatType.Attack, damageDealt, false);
                        return;
                    }
                    player.getStats().addXp(StatType.Attack, xp, multiplier);
                    break;
                case AGGRESSIVE:
                    if(player.showHitAsExperience()) {
                        player.getStats().addXp(StatType.Strength, damageDealt, false);
                        return;
                    }
                    player.getStats().addXp(StatType.Strength, xp, multiplier);
                    break;
                case CONTROLLED:
                    if(player.showHitAsExperience()) {
                        player.getStats().addXp(StatType.Attack, damageDealt, false);
                        return;
                    }
                    xp /= 3;
                    player.getStats().addXp(StatType.Attack, xp, multiplier);
                    player.getStats().addXp(StatType.Strength, xp, multiplier);
                    player.getStats().addXp(StatType.Defence, xp, multiplier);
                    break;
                case DEFENSIVE:
                    if(player.showHitAsExperience()) {
                        player.getStats().addXp(StatType.Defence, damageDealt, false);
                        return;
                    }
                    player.getStats().addXp(StatType.Defence, xp, multiplier);
                    break;
            }
        }
        player.getStats().addXp(StatType.Hitpoints, damageDealt * 1.33 * monsterMod, multiplier);
    }

    public static void addMagicXp(Player player, double baseXp, int damage, boolean multiplier) {
        double xp = baseXp + (damage * 2D);
        if(player.showHitAsExperience) {
            player.getStats().addXp(StatType.Magic, damage, false);
            return;
        }
        if(Config.DEFENSIVE_CAST.get(player) == 1) {
            xp /= 2;
            player.getStats().addXp(StatType.Defence, xp, multiplier);
            player.getStats().addXp(StatType.Magic, xp, multiplier);
        } else {
            player.getStats().addXp(StatType.Magic, xp, multiplier);
        }
        if(damage > 0)
            player.getStats().addXp(StatType.Hitpoints, damage * 1.33, multiplier);
    }

    /**
     * Combat level
     */

    public static int getCombatLevel(Player player) {
        StatList stats = player.getStats();
        double attack = stats.get(StatType.Attack).fixedLevel;
        double defence = stats.get(StatType.Defence).fixedLevel;
        double strength = stats.get(StatType.Strength).fixedLevel;
        double hitpoints = stats.get(StatType.Hitpoints).fixedLevel;
        double ranged = stats.get(StatType.Ranged).fixedLevel;
        double prayer = stats.get(StatType.Prayer).fixedLevel;
        double magic = stats.get(StatType.Magic).fixedLevel;
        double coreBase = (defence + hitpoints + (int) (prayer / 2D)) * 0.25;
        double meleeBase = (attack + strength) * 0.325;
        double rangedBase = ((int) (ranged / 2D) + ranged) * 0.325;
        double magicBase = ((int) (magic / 2D) + magic) * 0.325;
        return (int) (coreBase + Math.max(meleeBase, Math.max(rangedBase, magicBase)));
    }

}
