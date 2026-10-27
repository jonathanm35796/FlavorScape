package io.ruin.model.combat;

import io.ruin.api.utils.Random;
import io.ruin.cache.ItemDef;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.item.containers.Equipment;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum SetEffect {
    VERAC((player, target, hit, roll) -> {
        if (roll < .25) {
            hit.ignorePrayer().ignoreDefence();
            if (target.npc != null) hit.addPostRollDamage(1);
            target.graphics(1041);
        }
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_HAT, 4753, 4976, 4977, 4978, 4979),
            new Piece(Equipment.SLOT_CHEST, 4757, 4988, 4989, 4990, 4991),
            new Piece(Equipment.SLOT_LEGS, 4759, 4994, 4995, 4996, 4997),
            new Piece(Equipment.SLOT_WEAPON, 4755, 4982, 4983, 4984, 4985)),

    DHAROK((player, target, hit, roll) -> {
        hit.scaleRolledDamage(1 + Math.max(0, (player.getMaxHp() - player.getHp()) * player.getMaxHp()) / 10000.0);
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_HAT, 4716, 4880, 4881, 4882, 4883),
            new Piece(Equipment.SLOT_CHEST, 4720, 4892, 4893, 4894, 4895),
            new Piece(Equipment.SLOT_LEGS, 4722, 4898, 4899, 4900, 4901),
            new Piece(Equipment.SLOT_WEAPON, 4718, 4886, 4887, 4888, 4889)),

    GUTHAN((player, target, hit, roll) -> {
        if (roll < .25) {
            target.graphics(398,0 ,0);
            int heal = hit.damageForHealing;
            if (EquipmentCombatEffects.hasDamned(player))
                player.setHp(Math.min(player.getMaxHp() + 10, player.getHp() + heal));
            else
                player.incrementHp(heal);
        }
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_HAT, 4724, 4904, 4905, 4906, 4907),
            new Piece(Equipment.SLOT_CHEST, 4728, 4916, 4917, 4918, 4919),
            new Piece(Equipment.SLOT_LEGS, 4730, 4922, 4923, 4924, 4925),
            new Piece(Equipment.SLOT_WEAPON, 4726, 4910, 4911, 4912, 4913)),

    AHRIM((player, target, hit, roll) -> {
        if (!hit.isBlocked() && roll < .25) {
            if (target.player != null) target.player.getStats().get(io.ruin.model.stat.StatType.Strength).drain(5);
            else target.npc.getCombat().getStat(io.ruin.model.stat.StatType.Strength).drain(5);
            target.graphics(400);
        }
    }, (player, hit) -> hit.attackStyle.isMagic(),
            barrowsPiece(0, "Ahrim's hood"), barrowsPiece(4, "Ahrim's robetop"),
            barrowsPiece(7, "Ahrim's robeskirt"), barrowsPiece(3, "Ahrim's staff")),

    KARIL((player, target, hit, roll) -> {
        if (!hit.equipmentFollowup && !hit.isBlocked() && roll < .25) {
            if (target.player != null) {
                io.ruin.model.stat.Stat agility = target.player.getStats().get(io.ruin.model.stat.StatType.Agility);
                agility.drain(agility.currentLevel / 5);
                target.graphics(401);
            }
            if (EquipmentCombatEffects.hasDamned(player)) {
                Hit extra = new Hit(player, AttackStyle.RANGED, hit.attackType).fixedDamage(hit.damageBeforeMitigation / 2).ignoreDefence();
                extra.equipmentFollowup = true;
                target.hit(extra);
            }
        }
    }, (player, hit) -> hit.attackStyle.isRanged(),
            barrowsPiece(0, "Karil's coif"), barrowsPiece(4, "Karil's leathertop"),
            barrowsPiece(7, "Karil's leatherskirt"), barrowsPiece(3, "Karil's crossbow")),

    TORAG((player, target, hit, roll) -> {
        if (target.player != null && !hit.isBlocked() && roll < .25) {
            target.player.getMovement().drainEnergyFraction(.20);
            target.graphics(399);
        }
    }, (player, hit) -> hit.attackStyle.isMelee(),
            barrowsPiece(0, "Torag's helm"), barrowsPiece(4, "Torag's platebody"),
            barrowsPiece(7, "Torag's platelegs"), barrowsPiece(3, "Torag's hammers")),

    VOID_MELEE((player, target, hit, roll) -> {
        // Applied to effective levels in CombatUtils, before calculating the roll/max hit.
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_CHEST, 8839),
            new Piece(Equipment.SLOT_LEGS, 8840),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11665)),

    VOID_RANGE((player, target, hit, roll) -> {
        // Applied to effective levels in CombatUtils.
    }, (player, hit) -> hit.attackStyle.isRanged(),
            new Piece(Equipment.SLOT_CHEST, 8839),
            new Piece(Equipment.SLOT_LEGS, 8840),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11664)),

    VOID_MAGE((player, target, hit, roll) -> {
        // Accuracy is applied to the effective level in CombatUtils.
    }, (player, hit) -> hit.attackStyle.isMagic(),
            new Piece(Equipment.SLOT_CHEST, 8839),
            new Piece(Equipment.SLOT_LEGS, 8840),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11663)),

    ELITE_VOID_MELEE((player, target, hit, roll) -> {
        // Applied to effective levels in CombatUtils.
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_CHEST, 13072),
            new Piece(Equipment.SLOT_LEGS, 13073),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11665)),

    ELITE_VOID_RANGE((player, target, hit, roll) -> {
        // Applied to effective levels in CombatUtils.
    }, (player, hit) -> hit.attackStyle.isRanged(),
            new Piece(Equipment.SLOT_CHEST, 13072),
            new Piece(Equipment.SLOT_LEGS, 13073),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11664)),

    ELITE_VOID_MAGE((player, target, hit, roll) -> {
        // Accuracy is applied to the effective level in CombatUtils.
        // The 5% magic damage bonus is included with equipment/prayers in CombatUtils.
    }, (player, hit) -> hit.attackStyle.isMagic(),
            new Piece(Equipment.SLOT_CHEST, 13072),
            new Piece(Equipment.SLOT_LEGS, 13073),
            new Piece(Equipment.SLOT_HANDS, 8842),
            new Piece(Equipment.SLOT_HAT, 11663)),

    BERSERKER_NECKLACE((player, target, hit, roll) -> {
        hit.scaleRolledDamage(1.20);
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_AMULET, 11128),
            new Piece(Equipment.SLOT_WEAPON, 6523, 6525, 6527, 6528)),

    OBSIDIAN_ARMOUR((player, target, hit, roll) -> {
        hit.boostAttack(0.10).boostDamage(0.10);
    }, (player, hit) -> hit.attackStyle.isMelee(),
            new Piece(Equipment.SLOT_CHEST, 21301),
            new Piece(Equipment.SLOT_LEGS, 21304),
            new Piece(Equipment.SLOT_HAT, 21298),
            new Piece(Equipment.SLOT_WEAPON, 6523, 6525, 6527, 6528)),

    GRACEFUL(new Piece(Equipment.SLOT_HAT, "graceful hood"),
            new Piece(Equipment.SLOT_CHEST, "graceful top"),
            new Piece(Equipment.SLOT_LEGS, "graceful legs"),
            new Piece(Equipment.SLOT_HANDS, "graceful gloves"),
            new Piece(Equipment.SLOT_FEET, "graceful boots"),
            new Piece(Equipment.SLOT_CAPE, def -> def.name.toLowerCase().contains("graceful cape") || def.name.toLowerCase().contains("agility cape") || def.name.toLowerCase().contains("agility master cape")|| def.name.toLowerCase().contains("max cape"))
            ),
    ;

    BiPredicate<Player, Hit> hitCondition; // must fulfill this condition to even check if the pieces are equipped
    private Effect hitEffect;

    SetEffect(Effect hitEffect, BiPredicate<Player, Hit> hitCondition, Piece... pieces) {
        this.hitCondition = hitCondition;
        this.hitEffect = hitEffect;
        this.pieces = pieces;
    }

    SetEffect(Piece... pieces) {
        this.pieces = pieces;
    }

    private Piece[] pieces;

    public boolean hasPieces(Player player) {
        for (Piece p : pieces)
            if (!p.hasPiece(player))
                return false;
        return true;
    }

    public boolean activate(Player player, Hit hit) {
        return hit.attackStyle != null && hitCondition != null && hitCondition.test(player, hit) && hasPieces(player);
    }

    public boolean checkAndApply(Player player, Entity target, Hit hit) {
        return checkAndApply(player, target, hit, Random.get());
    }

    public boolean checkAndApply(Player player, Entity target, Hit hit, double roll) {
        if (activate(player, hit)) {
            hitEffect.doEffect(player, target, hit, roll);
            return true;
        }
        return false;
    }

    private static Piece barrowsPiece(int slot, String name) {
        return new Piece(slot, def -> def.name != null && def.name.matches("(?i)" + java.util.regex.Pattern.quote(name) + "(?: (?:100|75|50|25))?"));
    }

    private static class Piece {
        public Piece(int slot, Integer... ids) {
            this.slot = slot;
            this.ids = Arrays.asList(ids);
        }

        public Piece(int slot, String name) {
            this(slot, def -> def.name.toLowerCase().contains(name));
        }

        public Piece(int slot, Predicate<ItemDef> predicate) {
            this.ids = ItemDef.cached.values().stream().filter(predicate).map(def -> def.id).collect(Collectors.toList());
            this.slot = slot;
        }

        int slot;

        List<Integer> ids;

        boolean hasPiece(Player player) { // can probably add something like ItemDef.setEffect[0
            int id = player.getEquipment().getId(slot);
            return id != -1 && ids.contains(id);
        }

    }

    @FunctionalInterface
    private interface Effect {
        void doEffect(Player player, Entity target, Hit hit, double roll);
    }

}
