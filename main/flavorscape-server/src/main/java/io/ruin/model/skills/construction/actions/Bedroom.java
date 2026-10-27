package io.ruin.model.skills.construction.actions;

import io.ruin.api.utils.NumberUtils;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.dialogue.ItemDialogue;
import io.ruin.model.inter.dialogue.MessageDialogue;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.map.object.actions.ObjectAction;
import io.ruin.model.skills.construction.Buildable;
import io.ruin.model.skills.construction.Construction;
import io.ruin.model.skills.construction.House;

import static io.ruin.cache.ItemID.BLOOD_MONEY;

public class Bedroom {

    static {
        ObjectAction.register(Buildable.SERVANTS_MONEYBAG.getBuiltObjects()[0], "use", Construction.forHouseOwnerOnly((player, house) -> {
            player.dialogue(new ItemDialogue().one(Buildable.SERVANTS_MONEYBAG.getItemId(), house.getMoneyInMoneybag() == 0 ? "The moneybag is empty." : "The servant's moneybag currently has " + NumberUtils.formatNumber(house.getMoneyInMoneybag()) + " Blood Money in it."),
                    new OptionsDialogue("Choose an option",
                            new Option("Deposit", () -> deposit(player, house)),
                            new Option("Withdraw", () -> withdraw(player, house)),
                            new Option("Cancel")
                    )
            );
        }));
    }

    private static void withdraw(Player player, House house) {
        if (house.getMoneyInMoneybag() <= 0) {
            player.dialogue(new MessageDialogue("The moneybag is empty."));
            return;
        }
        player.integerInput("Enter amount:", amount -> {
            amount = Math.min(amount, house.getMoneyInMoneybag());
            int added = player.getInventory().add(BLOOD_MONEY, amount);
            if (added > 0) {
                house.setMoneyInMoneybag(house.getMoneyInMoneybag() - amount);
                player.sendMessage("You withdraw " + NumberUtils.formatNumber(added) + " Blood Money from the moneybag.");
            } else {
                player.dialogue(new MessageDialogue("Not enough space in your inventory."));
            }
        });
    }

    private static void deposit(Player player, House house) {
        if (player.getInventory().getAmount(BLOOD_MONEY) == 0) {
            player.dialogue(new MessageDialogue("You don't have any Blood Money in your inventory."));
            return;
        }
        player.integerInput("Enter amount:", amount -> {
            amount = Math.min(player.getInventory().getAmount(BLOOD_MONEY), amount);
            if (amount <= 0)
                return;
            house.setMoneyInMoneybag((int) Math.min((long)(house.getMoneyInMoneybag() + amount), Integer.MAX_VALUE));
            player.getInventory().remove(BLOOD_MONEY, amount);
            player.sendMessage("You deposit " + NumberUtils.formatNumber(amount) + " Blood Money into the servant's moneybag.");
        });
    }

}
