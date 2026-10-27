package io.ruin.model.combat.special.ranged.bolts;

import io.ruin.api.utils.Random;
import io.ruin.model.combat.Hit;
import io.ruin.model.entity.Entity;

import java.util.function.BiFunction;

public class OnyxBoltEffect implements BiFunction<Entity, Hit, Boolean> {

    @Override
    public Boolean apply(Entity target, Hit hit) {
        if (target.npc != null && io.ruin.model.entity.player.PlayerCombat.UNDEAD_NPCS.stream().anyMatch(name ->
                target.npc.getDef().name.toLowerCase(java.util.Locale.ROOT).contains(name.toLowerCase(java.util.Locale.ROOT))))
            return false;
        if(!hit.rollBoltEffect(target.player != null ? 10 : 11))
            return false;
        int damage = target.hit(hit.multiplyDamage(1.20));
        int heal = hit.damageForHealing / 4;
        if(heal > 0) {
            target.graphics(753);
            hit.attacker.incrementHp(heal);
        }
        return true;
    }

}
