package io.ruin.model.combat.special.ranged;

import io.ruin.cache.ItemDef;
import io.ruin.model.combat.AttackStyle;
import io.ruin.model.combat.AttackType;
import io.ruin.model.combat.Hit;
import io.ruin.model.combat.special.Special;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.item.Item;
import io.ruin.model.map.Projectile;
import io.ruin.utility.Misc;

import java.util.LinkedList;
import java.util.List;

public class DragonCrossbow implements Special {

    private static final Projectile PROJECTILE = new Projectile(698, 38, 36, 41, 51, 5, 5, 11);

    @Override
    public boolean accept(ItemDef def, String name) {
        return def.id == 21902;
    }

    @Override
    public boolean handle(Player player, Entity victim, AttackStyle attackStyle, AttackType attackType, int maxDamage) {
        int delay = PROJECTILE.send(player, victim);
        Hit primary = new Hit(player, attackStyle, attackType).randDamage(1, maxDamage).scaleRolledDamage(1.2).clientDelay(delay);
        Item ammo = player.getEquipment().get(13);
        if (ammo != null) {
            primary.setRangedAmmo(ammo.getDef());
            player.getCombat().removeAmmo(ammo, primary);
        }
        victim.hit(primary);
        player.animate(4230);
        victim.graphics(1466, 80, delay);
        List<Entity> targets = new LinkedList<>();
        if (!player.inMulti() || !victim.inMulti() || primary.isBlocked()) return true;
        victim.localPlayers().forEach(p -> {
            if (targets.size() < 8 && p != victim && p != player && p.inMulti() && player.getCombat().canAttack(p, false) && Misc.getEffectiveDistance(victim, p) <= 1)
                targets.add(p);
        });
        victim.localNpcs().forEach(n -> {
            if (targets.size() < 8 && n != victim && n.inMulti() && player.getCombat().canAttack(n, false) && Misc.getEffectiveDistance(n, victim) <= 1)
                targets.add(n);
        });
        int damage = primary.damageBeforeMitigation;
        for (Entity target : targets) {
            damage = (int) io.ruin.model.combat.CombatUtils.floorScale(damage,.8);
            target.hit(new Hit(player, attackStyle, attackType).fixedDamage(damage).ignoreDefence().clientDelay(delay));
        }
        return true;
    }

    @Override
    public int getDrainAmount() {
        return 60;
    }
}
