package io.ruin.model.combat.special.ranged.bolts;

import io.ruin.api.utils.Random;
import io.ruin.model.combat.Hit;
import io.ruin.model.entity.Entity;
import io.ruin.model.item.containers.Equipment;
import io.ruin.model.stat.StatType;
import java.util.Locale;
import java.util.function.BiFunction;

public class BasicEnchantedBoltEffect implements BiFunction<Entity, Hit, Boolean> {
    public enum Type { OPAL, JADE, PEARL, TOPAZ }
    private final Type type;
    public BasicEnchantedBoltEffect(Type type) { this.type = type; }
    @Override public Boolean apply(Entity target, Hit hit) {
        if (type == Type.TOPAZ && target.player == null) return false;
        if (!hit.rollBoltEffect(type == Type.OPAL ? 5 : type == Type.TOPAZ ? 4 : 6)) return false;
        int ranged = (int) hit.attacker.getCombat().getLevel(StatType.Ranged);
        switch (type) {
            case OPAL:
                hit.postDefend(t -> hit.damage += ranged / 10);
                target.graphics(749);
                break;
            case PEARL:
                if (hasWaterStaff(target)) return false;
                int bonus = ranged / (isFiery(target) ? 15 : 20);
                hit.postDefend(t -> hit.damage += bonus);
                target.graphics(750);
                break;
            case TOPAZ:
                hit.postDefend(t -> {
                    if (!hit.isBlocked()) { target.player.getStats().get(StatType.Magic).drain(1); target.graphics(757); }
                });
                break;
            case JADE:
                hit.postDefend(t -> {
                    if (!hit.isBlocked() && (target.player == null || Random.get(99) >= target.getCombat().getLevel(StatType.Agility))) {
                        target.stun(6, false); target.graphics(755);
                    }
                });
                break;
        }
        target.hit(hit);
        return true;
    }
    private static String weapon(Entity target) {
        if (target.player == null) return "";
        io.ruin.cache.ItemDef def = target.player.getEquipment().getDef(Equipment.SLOT_WEAPON);
        return def == null ? "" : def.name.toLowerCase(Locale.ROOT);
    }
    private static boolean hasWaterStaff(Entity target) {
        String name = weapon(target);
        return name.contains("staff") && (name.contains("water") || name.contains("mist") || name.contains("mud") || name.contains("steam"));
    }
    private static boolean isFiery(Entity target) {
        if (target.npc != null) {
            String name = target.npc.getDef().name.toLowerCase(Locale.ROOT);
            return target.npc.getDef().dragon || name.contains("fire giant") || name.contains("pyrefiend") || name.contains("fire elemental");
        }
        String name = weapon(target);
        int cape = target.player.getEquipment().getId(Equipment.SLOT_CAPE);
        return cape == 6570 || cape == 13331 || (name.contains("staff") && (name.contains("fire") || name.contains("lava") || name.contains("steam")));
    }
}
