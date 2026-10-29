package io.ruin.model.combat.special.ranged.bolts;

import io.ruin.api.utils.Random;
import io.ruin.model.combat.Hit;
import io.ruin.model.entity.Entity;

import java.util.function.BiFunction;

public class DragonBoltEffect implements BiFunction<Entity, Hit, Boolean> {

    @Override
    public Boolean apply(Entity target, Hit hit) {
        if(!hit.rollBoltEffect(6))
            return false;
        if(target.getCombat().getDragonfireResistance() > 0.3)
            return false;
        if (target.npc != null && target.npc.getDef().dragon)
            return false;
        int extra = (int) (hit.attacker.getCombat().getLevel(io.ruin.model.stat.StatType.Ranged) / 5);
        hit.postDefend(t -> {
            if (!hit.isBlocked()) hit.damage += extra;
        });
        int damage = target.hit(hit);
        if(damage > 0)
            target.graphics(756);
        return true;
    }

}
