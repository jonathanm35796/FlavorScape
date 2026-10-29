package io.ruin.model.combat;

import io.ruin.api.utils.Random;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.handlers.EquipmentStats;
import io.ruin.model.item.containers.Equipment;
import java.util.function.DoubleSupplier;

/** Native equipment effects shared by ordinary PvP and PvM combat. */
public final class EquipmentCombatEffects {
    private EquipmentCombatEffects() {}
    public static boolean hasDamned(Player player) {
        int amulet = player.getEquipment().getId(Equipment.SLOT_AMULET);
        return amulet == 12851 || amulet == 12853;
    }
    public static double toragDefence(Player player, double level) {
        if (!hasDamned(player) || !SetEffect.TORAG.hasPieces(player)) return level;
        return CombatUtils.floorScale(level, 1 + Math.max(0, player.getMaxHp() - player.getHp()) / (double) player.getMaxHp());
    }
    public static int recoilDamage(int damage) { return damage > 0 ? 1 + damage / 10 : 0; }

    public static double venomChance(Player player, Entity target, Hit hit) {
        if (hit.isBlocked() || hit.damage <= 0 || hit.attackStyle == null) return 0;
        io.ruin.cache.ItemDef weapon = hit.attackWeapon != null ? hit.attackWeapon : player.getEquipment().getDef(3);
        if (weapon == null) return 0;
        boolean venomWeapon = weapon.id == 12926 && (hit.attackStyle.isRanged() || hit.attackStyle.isMagic())
                || weapon.id == 12899 && hit.attackStyle.isMagic()
                || weapon.id == 12904 && (hit.attackStyle.isMagic() || hit.attackStyle.isMelee());
        int helm = player.getEquipment().getId(0);
        boolean serpent = target.npc != null && (helm == 12931 || helm == 13197 || helm == 13199);
        if (venomWeapon) return serpent ? 1 : .25;
        if (!serpent || !hit.attackStyle.isMelee()) return 0;
        return weapon.name.toLowerCase(java.util.Locale.ROOT).contains("(p") ? .5 : 1.0 / 6;
    }

    public static int poisonDamage(Player player, Hit hit) {
        if (hit.attackStyle == null || hit.isBlocked() || hit.damage <= 0) return 0;
        io.ruin.cache.ItemDef source = hit.attackWeapon != null ? hit.attackWeapon : player.getEquipment().getDef(3);
        boolean ranged = hit.attackStyle.isRanged();
        if (ranged && hit.rangedAmmo != null) source = hit.rangedAmmo;
        if (source == null || (!ranged && !hit.attackStyle.isMelee())) return 0;
        if (source.name.toLowerCase(java.util.Locale.ROOT).contains("abyssal tentacle")) return hit.specialAttack ? 0 : 4;
        String name = source.name.toLowerCase(java.util.Locale.ROOT);
        int damage = name.contains("(p++)") || name.contains("(kp)") ? 6 : name.contains("(p+)") ? 5 : name.contains("(p)") ? 4 : 0;
        return damage == 0 ? 0 : damage - (ranged ? 2 : 0);
    }
    public static void reflectDamned(Player player, Hit hit, double roll) {
        if (hit.attacker != null && hit.attackStyle != null && hit.damage > 0 && hit.type == HitType.DAMAGE
                && roll < .25 && hasDamned(player) && SetEffect.DHAROK.hasPieces(player)) {
            int reflected = (int) CombatUtils.floorScale(hit.damage, .15);
            if (reflected > 0) hit.attacker.hit(new Hit(player).fixedDamage(reflected).setResetActions(false));
        }
    }
    public static boolean hasJusticiar(Player player) {
        Equipment gear = player.getEquipment();
        return gear.getId(0) == 22326 && gear.getId(4) == 22327 && gear.getId(7) == 22328;
    }
    public static int defenceIndex(AttackStyle style) {
        if (style == null) return -1;
        switch (style) {
            case STAB: return EquipmentStats.STAB_DEFENCE;
            case SLASH: return EquipmentStats.SLASH_DEFENCE;
            case CRUSH: return EquipmentStats.CRUSH_DEFENCE;
            case RANGED: return EquipmentStats.RANGE_DEFENCE;
            case MAGIC: case MAGICAL_RANGED: case MAGICAL_MELEE: return EquipmentStats.MAGIC_DEFENCE;
            default: return -1;
        }
    }
    public static void defend(Player player, Hit hit) { defend(player, hit, Random::get); }
    static void defend(Player player, Hit hit, DoubleSupplier random) {
        int index = defenceIndex(hit.attackStyle);
        if (hit.attacker == null || hit.type != HitType.DAMAGE || hit.damage <= 0 || index < 0) return;
        boolean elysian = player.getEquipment().getId(Equipment.SLOT_SHIELD) == 12817 && random.getAsDouble() < .70;
        boolean justiciar = hit.attacker.player == null && hasJusticiar(player);
        int original = hit.damage;
        double reduction = elysian ? .25 : 0;
        if (justiciar) reduction += Math.max(0, player.getCombat().getBonus(index)) / 3000;
        if (elysian || justiciar) {
            hit.damage = Math.max(0, original - Math.max(1, (int) CombatUtils.floorScale(original, reduction)));
            if (elysian) player.graphics(321);
        }
        if (hit.attacker.player == null && player.getCombat().isBulwarkBlocking() && !player.getCombat().bulwarkDefenceDelay.isDelayed())
            hit.damage = (int) CombatUtils.floorScale(hit.damage, .8);
    }
    public static int prayerDrain(Player player, int points, Entity source) {
        if (points <= 0) return 0;
        return source != null && source.player == null && player.getEquipment().getId(Equipment.SLOT_SHIELD) == 12821
                ? (points + 1) / 2 : points;
    }
    public static void attack(Player player, Hit hit) {
        if (hit.attackStyle == AttackStyle.MAGIC && hasDamned(player) && SetEffect.AHRIM.hasPieces(player) && Random.get() < .25)
            hit.scaleRolledDamage(1.3);
        if (hit.attackStyle == AttackStyle.MAGIC && player.getEquipment().getId(Equipment.SLOT_RING) == 22975 && Random.get() < .25)
            hit.multiplyDefence(.9);
    }
    public static int sanguinestiMaxHit(Player player) {
        return CombatUtils.getMagicMaxDamage(player, Math.max(0, (int) player.getCombat().getLevel(io.ruin.model.stat.StatType.Magic) / 3 - 1));
    }

    public static int bulwarkStrength(Player player) {
        if (player.getEquipment().getId(3) != 21015 || player.getCombat().getAttackType() == AttackType.DEFENSIVE) return 0;
        int[] bonuses = player.getEquipment().bonuses;
        return Math.max(0, (bonuses[5] + bonuses[6] + bonuses[7] + bonuses[9] - 800) / 12 - 38);
    }
}
