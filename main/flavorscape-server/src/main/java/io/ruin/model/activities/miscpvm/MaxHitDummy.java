package io.ruin.model.activities.miscpvm;

import io.ruin.model.combat.Hit;
import io.ruin.model.combat.HitType;
import io.ruin.model.entity.shared.listeners.HitListener;

public class MaxHitDummy extends PassiveCombat {

    @Override
    public void init() {
        npc.hitListener = new HitListener()
        .preDefend(h -> {
            if (h.type == HitType.DAMAGE || h.type == HitType.BLOCKED)
                h.maximumDamage();
        })
        .postDefend(h -> {
            if (h.attackStyle != null && h.attackStyle.isCannon())
                h.damage = 0;
        });
    }

    @Override
    public void startDeath(Hit killHit) {
        npc.setHp(npc.getMaxHp());
    }

}
