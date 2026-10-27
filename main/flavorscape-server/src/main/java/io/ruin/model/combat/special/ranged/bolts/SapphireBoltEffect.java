package io.ruin.model.combat.special.ranged.bolts;

import io.ruin.api.utils.Random;
import io.ruin.model.combat.Hit;
import io.ruin.model.entity.Entity;
import io.ruin.model.stat.Stat;
import io.ruin.model.stat.StatType;

import java.util.function.BiFunction;

public class SapphireBoltEffect implements BiFunction<Entity, Hit, Boolean> {

    @Override
    public Boolean apply(Entity target, Hit hit) {
        if(!hit.rollBoltEffect(target.player == null ? 25 : 5))
            return false;
        Stat prayer = hit.attacker.player.getStats().get(StatType.Prayer);
        target.hit(hit);
        if (!hit.isBlocked()) {
            int drain = (int) (target.player == null ? target.getCombat().getLevel(StatType.Ranged) / 10
                    : hit.attacker.getCombat().getLevel(StatType.Ranged) / 20);
            if (target.player != null) {
                drain = Math.min(drain, target.getHp());
                target.player.getPrayer().drain(drain);
            }
            prayer.restore(drain / (target.player == null ? 8 : 2));
            target.graphics(751);
        }
        return true;
    }

}
