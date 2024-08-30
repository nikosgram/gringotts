package org.gestern.gringotts.event;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolder;

import java.util.Optional;

public class VaultCreator implements Listener {
    private final Accounting accounting = Gringotts.instance.getAccounting();

    /**
     * If the vault creation event was properly handled and an AccountHolder supplied, it will be created here.
     *
     * @param event event to handle
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void registerVault(PlayerVaultCreationEvent event) {
        // event has been marked invalid, ignore
        if (!event.isValid()) {
            return;
        }

        AccountHolder owner = event.getOwner();

        if (owner == null) {
            return;
        }

        GringottsAccount account = accounting.getAccount(owner);

        SignChangeEvent cause = event.getCause();
        Optional<Sign> optionalSign = Util.getBlockStateAs(
                cause.getBlock(),
                Sign.class
        );

        if (optionalSign.isEmpty()) {
            return;
        }

        // create account chest
        AccountChest accountChest = new AccountChest(optionalSign.get(), account, 0);

        // check for existence / add to tracking
        if (accounting.addChest(accountChest)) {
            String firstLine = cause.getLine(0);

            if (firstLine != null && firstLine.length() <= 16) {
                cause.setLine(0, " " + ChatColor.BOLD + firstLine + " ");
            }

            cause.setLine(2, owner.getName());
            cause.getPlayer().sendMessage(Language.LANG.vault_created);
        } else {
            cause.setCancelled(true);
            cause.getPlayer().sendMessage(Language.LANG.vault_error);
        }
    }
}
