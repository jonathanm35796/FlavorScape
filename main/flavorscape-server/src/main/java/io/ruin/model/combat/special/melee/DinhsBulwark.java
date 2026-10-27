package io.ruin.model.combat.special.melee;

import io.ruin.cache.ItemDef;
import io.ruin.model.combat.*;
import io.ruin.model.combat.special.Special;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.stat.*;
import java.util.*;

public class DinhsBulwark implements Special {
    @Override public boolean accept(ItemDef def, String name) { return def.id == 21015; }
    @Override public int getDrainAmount() { return 50; }
    @Override public boolean handle(Player player, Entity target, AttackStyle style, AttackType type, int max) {
        if (player.getCombat().isBulwarkBlocking()) return false;
        player.animate(7511);
        List<Entity> targets = new ArrayList<>();
        targets.add(target);
        if (player.inMulti()) player.forLocalEntity(entity -> {
            if (entity == player || targets.contains(entity) || targets.size() >= 10 || !entity.inMulti()) return;
            if (Math.abs(entity.getAbsX() - player.getAbsX()) > 5 || Math.abs(entity.getAbsY() - player.getAbsY()) > 5) return;
            if (player.getCombat().canAttack(entity, false)) targets.add(entity);
        });
        for (Entity victim : targets) {
            Hit hit = new Hit(player, AttackStyle.CRUSH, type).randDamage(max).specialAccuracy(1.2);
            victim.hit(hit);
            if (victim.npc != null && !hit.isBlocked()) drainHighest(victim);
        }
        if (player.inMulti() && target.inMulti() && target.npc != null)
            target.hit(new Hit(player, AttackStyle.CRUSH, type).randDamage(max).specialAccuracy(1.2));
        return true;
    }
    private static void drainHighest(Entity target) {
        Stat attack = target.npc.getCombat().getStat(StatType.Attack);
        Stat strength = target.npc.getCombat().getStat(StatType.Strength);
        Stat ranged = target.npc.getCombat().getStat(StatType.Ranged);
        Stat magic = target.npc.getCombat().getStat(StatType.Magic);
        int melee = Math.max(attack.currentLevel, strength.currentLevel);
        if (melee >= ranged.currentLevel && melee >= magic.currentLevel) {
            attack.drain(attack.currentLevel / 20);
            strength.drain(strength.currentLevel / 20);
        } else {
            Stat stat = ranged.currentLevel >= magic.currentLevel ? ranged : magic;
            stat.drain(stat.currentLevel / 20);
        }
    }
}
