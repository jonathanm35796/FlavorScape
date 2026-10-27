package io.ruin.model.combat.special.melee;

import io.ruin.cache.ItemDef;
import io.ruin.model.combat.*;
import io.ruin.model.combat.special.Special;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;

public class DragonClaws implements Special {
    @Override public boolean accept(ItemDef def, String name) { return name.contains("dragon claws"); }

    @Override public boolean handle(Player player, Entity target, AttackStyle style, AttackType type, int maxDamage) {
        player.animate(7514, 10);
        player.graphics(1171, 0, 10);
        player.publicSound(2537);
        // One set of equipment modifiers, then up to four accuracy rolls against slash defence.
        Hit first = new Hit(player, style, type).defendAgainst(AttackStyle.SLASH).randDamage(maxDamage).clawAttack();
        target.hit(first);
        for (int index = 1; index < 4; index++)
            target.hit(new Hit(player, style, type).fixedDamage(first.getClawDamage(index)).ignoreDefence().delay(index < 2 ? 1 : 2));
        return true;
    }

    @Override public int getDrainAmount() { return 50; }
}
