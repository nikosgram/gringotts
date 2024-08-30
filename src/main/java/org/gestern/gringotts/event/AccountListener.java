package org.gestern.gringotts.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.Util;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for chest creation, destruction and change events.
 *
 * @author jast
 */
public class AccountListener implements Listener {

    private final Pattern VAULT_PATTERN = Pattern.compile(Configuration.CONF.vaultPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Create an account chest by adding a sign marker over it.
     *
     * @param event Event data.
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0String = ChatColor.stripColor(event.getLine(0)).trim();

        if (line0String == null) {
            return;
        }

        Matcher match = VAULT_PATTERN.matcher(line0String);

        // consider only signs with proper formatting
        if (!match.matches()) {
            return;
        }

        String typeStr = match.group(1).toUpperCase();

        String type;

        // default vault is player
        if (typeStr.isEmpty()) {
            type = "player";
        } else {
            type = typeStr.toLowerCase();
        }

        Optional<Sign> optionalSign = Util.getBlockStateAs(
                event.getBlock(),
                Sign.class
        );

        if (optionalSign.isPresent() && Util.chestBlock(optionalSign.get()) != null) {
            // we made it this far, throw the event to manage vault creation
            final VaultCreationEvent creation = new PlayerVaultCreationEvent(type, event);

            Bukkit.getServer().getPluginManager().callEvent(creation);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignBreak(BlockDestroyEvent event) {
        if (Tag.SIGNS.isTagged(event.getBlock().getType())) {
            Gringotts.instance.getDao().deleteAccountChest(
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ()
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!Util.isValidInventory(event.getInventory().getType())) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;

        AccountChest chest = getAccountChestFromHolder(holder);
        if (chest == null) return;

        chest.setCachedBalance(chest.balance(true));
        Gringotts.instance.getDao().updateChestBalance(chest, chest.getCachedBalance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() != null) {
            AccountChest chest = getAccountChestFromHolder(event.getSource().getHolder());
            if (chest != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        chest.setCachedBalance(chest.balance(true));
                        Gringotts.instance.getDao().updateChestBalance(chest, chest.getCachedBalance());
                    }
                }.runTask(Gringotts.instance);
            }
        }
        if (event.getDestination() != null) {
            AccountChest chest = getAccountChestFromHolder(event.getDestination().getHolder());
            if (chest != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        chest.setCachedBalance(chest.balance(true));
                        Gringotts.instance.getDao().updateChestBalance(chest, chest.getCachedBalance());
                    }
                }.runTask(Gringotts.instance);
            }
        }
    }

    /**
     * Get the AccountChest associated with this {@link InventoryHolder}
     * @param holder
     * @return the {@link AccountChest} or null if none was found
     */
    private AccountChest getAccountChestFromHolder(InventoryHolder holder) {
        for (AccountChest chest : Gringotts.instance.getDao().retrieveChests()) {
            if (!chest.isChestLoaded()) continue; // For a chest to be open or interacted with, it needs to be loaded

            BlockState chestState = Util.chestBlock(chest.sign).getState();

            if (chestState.equals(holder)) return chest;

            if (holder instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) holder;
                if (doubleChest.getLeftSide().equals(chestState) || doubleChest.getRightSide().equals(chestState))
                    return chest;
            }
        }
        return null;
    }
}
