package org.gestern.gringotts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.accountholder.PlayerAccountHolder;
import org.gestern.gringotts.api.TransactionResult;
import org.gestern.gringotts.currency.Denomination;
import org.gestern.gringotts.data.DAO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Implementation of inventory-based accounts with a virtual overflow capacity.
 * Has support for player accounts specifically and works with any other container storage.
 *
 * @author jast
 */
public class GringottsAccount {
    public final  AccountHolder owner;
    private final DAO           dao = Gringotts.instance.getDao();

    public GringottsAccount(AccountHolder owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Account owner cannot be null");
        }

        this.owner = owner;
    }

    /**
     * Call a function in the main thread. The returned CompletionStage will be completed after the function is called.
     *
     * @param callMe function to call
     * @return will be completed after function is called
     */
    private static <V> CompletableFuture<V> callSync(Callable<V> callMe) {
        final CompletableFuture<V> f = new CompletableFuture<>();

        Runnable runMe = () -> {
            try {
                f.complete(callMe.call());
            } catch (Exception e) {
                f.completeExceptionally(e);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            runMe.run();
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Gringotts.instance, runMe);
        }

        return f;
    }

    /**
     * Current balance of this account in cents
     *
     * @return current balance of this account in cents
     */
    public long getBalance() {
        CompletableFuture<Long> cents            = getCents();
        CompletableFuture<Long> playerInv        = countPlayerInventory();
        CompletableFuture<Long> playerEnderchest = countPlayerEnderchest();
        CompletableFuture<Long> chestInv         = countChestInventories();

        // order of combination is important, because chestInv/playerInv/enderchest might have to run on main thread
        CompletableFuture<Long> f = chestInv
                .thenCombine(playerInv, Long::sum)
                .thenCombine(playerEnderchest, Long::sum)
                .thenCombine(cents, Long::sum);

        return getTimeout(f);
    }

    /**
     * Current balance this account has in chest(s) in cents
     *
     * @return current balance this account has in chest(s) in cents
     */
    public long getVaultBalance() {
        return getTimeout(countChestInventories());
    }

    /**
     * Current balance this account has in chest(s) in cents
     *
     * @return current balance this account has in chest(s) in cents
     */
    public long getVaultBalance(int index) {
        return getTimeout(countChestInventory(index));
    }

    /**
     * Current balance this account has in chest(s) in cents
     *
     * @return current balance this account has in chest(s) in cents
     */
    public Location getVaultLocation(int index) {
        return getTimeout(countChestLocation(index));
    }


    public Collection<AccountChest> getVaultChests() {
        return getTimeout(getChests());
    }

    public int getVaultCount() {
        return getVaultChests().size();
    }

    /**
     * Current balance this account has in inventory in cents
     *
     * @return current balance this account has in inventory in cents
     */
    public long getInvBalance() {
        CompletableFuture<Long> cents     = getCents();
        CompletableFuture<Long> playerInv = countPlayerInventory();
        CompletableFuture<Long> f         = cents.thenCombine(playerInv, Long::sum);

        return getTimeout(f);
    }

    public long getEndBalance() {
        return getTimeout(countPlayerEnderchest());
    }

    /**
     * Add an amount in cents to this account if able to.
     *
     * @param amount amount in cents to add
     * @return Whether amount successfully added
     */
    public TransactionResult add(long amount) {
        Callable<TransactionResult> callMe = () -> {
            // Cannot add negative amount
            if (amount < 0) {
                return TransactionResult.ERROR;
            }

            long centsStored = dao.retrieveCents(this);

            long remaining = amount + centsStored;

            // add currency to account's vaults
            if (Configuration.CONF.useVaultContainer) {
                for (AccountChest chest : dao.retrieveChests(this)) {
                    remaining -= chest.add(remaining);

                    if (remaining <= 0) {
                        break;
                    }

                    if (Configuration.CONF.includeShulkerBoxes) {
                        remaining = addToShulkerBox(remaining, chest.chest().getInventory());
                    }
                }
            }

            // add stuff to player's inventory and enderchest too, when they are online
            Optional<Player> playerOpt = playerOwner();

            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();

                if (remaining > 0 && Permissions.USE_VAULT_INVENTORY.isAllowed(player)) {
                    remaining -= new AccountInventory(player.getInventory()).add(remaining);

                    if (Configuration.CONF.includeShulkerBoxes && remaining > 0) {
                        remaining = addToShulkerBox(remaining, player.getInventory());
                    }
                }
                if (remaining > 0 && Configuration.CONF.useVaultEnderChest && Permissions.USE_VAULT_ENDERCHEST.isAllowed(player)) {
                    remaining -= new AccountInventory(player.getEnderChest()).add(remaining);

                    if (Configuration.CONF.includeShulkerBoxes && remaining > 0) {
                        remaining = addToShulkerBox(remaining, player.getEnderChest());
                    }
                }
            }

            // allow smallest denom value as threshold for available space
            // TODO make maximum virtual amount configurable
            // this is under the assumption that there is always at least 1 denomination
            List<Denomination> denoms             = Configuration.CONF.getCurrency().getDenominations();
            long               smallestDenomValue = denoms.get(denoms.size() - 1).getValue();

            if (remaining < smallestDenomValue) {
                dao.storeCents(this, remaining);
                remaining = 0;
            }

            if (remaining == 0) {
                return TransactionResult.SUCCESS;
            }

            if (Configuration.CONF.dropOverflowingItem && playerOpt.isPresent()) {
                for (Denomination denomination : Configuration.CONF.getCurrency().getDenominations()) {
                    if (denomination.getValue() > remaining) {
                        continue;
                    }

                    // noinspection ConstantValue
                    if (denomination.getKey().type == null) {
                        Gringotts.instance.getLogger().warning("Denomination " + denomination.getUnitName() + " has no item type set!");

                        continue;
                    }

                    ItemStack stack        = new ItemStack(denomination.getKey().type);
                    int       stackSize    = stack.getMaxStackSize();
                    long      denItemCount = denomination.getValue() > 0 ? remaining / denomination.getValue() : 0;

                    while (denItemCount > 0) {
                        int remainderStackSize;

                        if (denItemCount > stackSize) {
                            remainderStackSize = stackSize;
                        } else {
                            remainderStackSize = (int) denItemCount;
                        }

                        stack.setAmount(remainderStackSize);

                        denItemCount -= remainderStackSize;
                        remaining -= remainderStackSize * denomination.getValue();

                        playerOpt.get().getWorld().dropItem(playerOpt.get().getLocation(), stack);
                    }
                }
            }

            return TransactionResult.INSUFFICIENT_SPACE;
        };

        return getTimeout(callSync(callMe));
    }

    /**
     * Attempt to remove an amount in cents from this account.
     * If the account contains less than the specified amount, returns false
     *
     * @param amount amount in cents to remove
     * @return amount actually removed.
     */
    public TransactionResult remove(long amount) {
        Callable<TransactionResult> callMe = () -> {
            // Cannot remove negative amount
            if (amount < 0) {
                return TransactionResult.ERROR;
            }

            // Make sure we have enough to remove
            if (getBalance() < amount) {
                return TransactionResult.INSUFFICIENT_FUNDS;
            }

            long remaining = amount;

            // Now remove the physical amount left
            if (Configuration.CONF.useVaultContainer) {
                for (AccountChest chest : dao.retrieveChests(this)) {
                    remaining -= chest.remove(remaining);

                    if (remaining <= 0) {
                        break;
                    }

                    if (Configuration.CONF.includeShulkerBoxes) {
                        remaining = removeFromShulkerBox(remaining, chest.chest().getInventory());
                    }
                }
            }

            Optional<Player> playerOpt = playerOwner();

            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();

                if (Permissions.USE_VAULT_INVENTORY.isAllowed(player) && remaining > 0) {
                    remaining -= new AccountInventory(player.getInventory()).remove(remaining);

                    if (Configuration.CONF.includeShulkerBoxes && remaining > 0) {
                        remaining = removeFromShulkerBox(remaining, player.getInventory());
                    }
                }
                if (Configuration.CONF.useVaultEnderChest && remaining > 0) {
                    remaining -= new AccountInventory(player.getEnderChest()).remove(remaining);

                    if (Configuration.CONF.includeShulkerBoxes && remaining > 0) {
                        remaining = removeFromShulkerBox(remaining, player.getEnderChest());
                    }
                }
            }

            if (remaining < 0)
                // took too much, pay back the extra
                return add(-remaining);

            if (remaining > 0) {
                // cannot represent the leftover in our denominations, take them from the virtual reserve
                long cents = dao.retrieveCents(this);
                dao.storeCents(this, cents - remaining);
            }

            return TransactionResult.SUCCESS;
        };

        return getTimeout(callSync(callMe));
    }

    public long addToShulkerBox(long remaining, Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && Tag.SHULKER_BOXES.isTagged(itemStack.getType()) && itemStack.getItemMeta() instanceof BlockStateMeta blockState) {
                if (blockState.getBlockState() instanceof ShulkerBox shulkerBox) {
                    remaining -= new AccountInventory(shulkerBox.getInventory()).add(remaining);

                    shulkerBox.update();
                    blockState.setBlockState(shulkerBox);
                    itemStack.setItemMeta(blockState);

                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }

        return remaining;
    }

    public long removeFromShulkerBox(long remaining, Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && Tag.SHULKER_BOXES.isTagged(itemStack.getType()) && itemStack.getItemMeta() instanceof BlockStateMeta blockState) {
                if (blockState.getBlockState() instanceof ShulkerBox shulkerBox) {
                    remaining -= new AccountInventory(shulkerBox.getInventory()).remove(remaining);

                    shulkerBox.update();
                    blockState.setBlockState(shulkerBox);
                    itemStack.setItemMeta(blockState);

                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }

        return remaining;
    }

    @Override
    public String toString() {
        return String.format("Account (%s)", owner);
    }

    /**
     * Returns the player owning this account, if the owner is actually a player and online.
     *
     * @return {@link Optional} of the player owning this account, if the owner is actually a player and online, otherwise
     * empty.
     */
    private Optional<Player> playerOwner() {
        if (owner instanceof PlayerAccountHolder) {
            OfflinePlayer player = ((PlayerAccountHolder) owner).accountHolder;

            return Optional.ofNullable(player.getPlayer());
        }

        return Optional.empty();
    }

    private CompletableFuture<Long> countChestInventories() {
        Callable<Long> callMe = () -> {
            Collection<AccountChest> chests  = dao.retrieveChests(this);
            long                     balance = 0;

            if (Configuration.CONF.useVaultContainer) {
                for (AccountChest chest : chests) {
                    balance += chest.balance();
                }
            }

            return balance;
        };

        return callSync(callMe);
    }

    private CompletableFuture<Long> countChestInventory(int index) {
        Callable<Long> callMe = () -> {
            List<AccountChest> chests = new ArrayList<>(dao.retrieveChests(this));

            if (Configuration.CONF.useVaultContainer && index < chests.size() && index >= 0) {
                return chests.get(index).balance();
            }

            Optional<Player> playerOpt = playerOwner();
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();

                if (Configuration.CONF.useVaultEnderChest && Permissions.USE_VAULT_ENDERCHEST.isAllowed(player) && index == -1) {
                    return new AccountInventory(player.getEnderChest()).balance();
                }
            }
            return -1L;
        };

        return callSync(callMe);
    }

    private CompletableFuture<Location> countChestLocation(int index) {
        Callable<Location> callMe = () -> {
            List<AccountChest> chests = new ArrayList<>(dao.retrieveChests(this));

            if (Configuration.CONF.useVaultContainer && index < chests.size() && index >= 0) {
                return chests.get(index).chestLocation();
            }
            return null;
        };

        return callSync(callMe);
    }

    private CompletableFuture<Collection<AccountChest>> getChests() {
        return callSync(() -> dao.retrieveChests(this));
    }

    private CompletableFuture<Long> countPlayerInventory() {
        Callable<Long> callMe = () -> {
            long balance = 0;

            Optional<Player> playerOpt = playerOwner();
            if (playerOpt.isPresent() && Permissions.USE_VAULT_INVENTORY.isAllowed(playerOpt.get())) {
                Player player = playerOpt.get();

                balance += new AccountInventory(player.getInventory()).balance();
            }
            return balance;
        };

        return callSync(callMe);
    }

    private CompletableFuture<Long> countPlayerEnderchest() {
        Callable<Long> callMe = () -> {
            long balance = 0;

            Optional<Player> playerOpt = playerOwner();
            if (playerOpt.isPresent() && Permissions.USE_VAULT_ENDERCHEST.isAllowed(playerOpt.get())) {
                Player player = playerOpt.get();

                balance += new AccountInventory(player.getEnderChest()).balance();
            }
            return balance;
        };

        return callSync(callMe);
    }

    private CompletableFuture<Long> getCents() {
        return CompletableFuture.supplyAsync(() -> dao.retrieveCents(this));
    }

    private <V> V getTimeout(CompletableFuture<V> f) {
        try {
            return f.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new GringottsException(e);
        }
    }

}
