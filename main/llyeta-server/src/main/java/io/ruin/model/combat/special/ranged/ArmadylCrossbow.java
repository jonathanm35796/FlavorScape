package io.ruin.model.combat.special.ranged;

import io.ruin.cache.ItemDef;
import io.ruin.model.combat.AttackStyle;
import io.ruin.model.combat.AttackType;
import io.ruin.model.combat.Hit;
import io.ruin.model.combat.special.Special;
import io.ruin.model.entity.Entity;
import io.ruin.model.entity.player.Player;
import io.ruin.model.map.Projectile;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Equipment;
import io.ruin.model.combat.RangedAmmo;

// Armadyl Eye: Double accuracy and enchanted bolt activation chance. (50%)
public class ArmadylCrossbow implements Special {

    private static final Projectile PROJECTILE = new Projectile(301, 38, 36, 41, 51, 5, 5, 11);

    @Override
    public boolean accept(ItemDef def, String name) {
        return name.contains("armadyl crossbow");
    }

    @Override
    public boolean handle(Player player, Entity victim, AttackStyle attackStyle, AttackType attackType, int maxDamage) {
        player.animate(4230);
        int delay = PROJECTILE.send(player, victim);
        Item ammo = player.getEquipment().get(Equipment.SLOT_AMMO);
        if (ammo == null) return false;
        RangedAmmo data = ammo.getDef().rangedAmmo;
        Hit hit = new Hit(player, attackStyle, attackType).randDamage(maxDamage).specialAccuracy(2)
                .boltChance(2).clientDelay(delay).setRangedAmmo(ammo.getDef());
        player.getCombat().removeAmmo(ammo, hit);
        if (data == null || data.effect == null || !data.effect.apply(victim, hit))
            victim.hit(hit);
        return true;
    }

    @Override
    public int getDrainAmount() {
        return 50;
    }

}
