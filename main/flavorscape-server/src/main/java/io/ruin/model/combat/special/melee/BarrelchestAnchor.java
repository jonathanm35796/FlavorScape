package io.ruin.model.combat.special.melee;

import io.ruin.api.utils.Random;
import io.ruin.cache.ItemDef;
import io.ruin.model.combat.AttackStyle;
import io.ruin.model.combat.AttackType;
import io.ruin.model.combat.Hit;
import io.ruin.model.combat.special.Special;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.stat.StatType;

//Sunder: Deal an attack with double accuracy and 10% higher max hit. Lowers your
//target's Attack, Defence, Ranged or Magic level by 10% of the damage dealt. (50%)
public class BarrelchestAnchor implements Special {

    private static final StatType[] DRAIN_STATS = {StatType.Defence, StatType.Attack, StatType.Ranged, StatType.Magic};

    @Override
    public boolean accept(ItemDef def, String name) {
        return def.id == 10887;
    }

    @Override
    public boolean handle(Player player, Entity target, AttackStyle attackStyle, AttackType attackType, int maxDamage) {
        player.animate(6147);
        player.graphics(1027);
        Hit hit = new Hit(player, attackStyle, attackType).defendAgainst(AttackStyle.CRUSH)
                .randDamage(maxDamage)
                .specialAccuracy(2)
                .specialDamage(1.1);
        hit.postDefend(t -> drain(t, hit.isBlocked() ? 0 : hit.damageRolled / 10));
        target.hit(hit);
        return true;
    }

    public static void drain(Entity target, int points) {
        if (points <= 0) return;
        for (StatType type : DRAIN_STATS) {
            io.ruin.model.stat.Stat stat = target.player != null ? target.player.getStats().get(type) : target.npc.getCombat().getStat(type);
            if (stat != null && stat.currentLevel > 1) {
                stat.drain(Math.min(points, stat.currentLevel - 1));
                return;
            }
        }
    }

    @Override
    public int getDrainAmount() {
        return 50;
    }
}
