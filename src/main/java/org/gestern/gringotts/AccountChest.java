package org.gestern.gringotts;

import io.papermc.lib.PaperLib;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.gestern.gringotts.data.EBeanPendingOperation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a storage unit for an account.
 *
 * @author jast
 */
public class AccountChest {

    private final Pattern VAULT_PATTERN = Pattern.compile(Configuration.CONF.vaultPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public final String           id;

    /**
     * Sign marking the chest as an account chest.
     */
    public final Sign             sign;
    /**
     * Account this chest belongs to.
     */
    public final GringottsAccount account;

    private long cachedBalance;

    /**
     * Create Account chest based on a sign marking its position and belonging to an account.
     *
     * @param sign    the marker sign
     * @param account the account
     */
    public AccountChest(Sign sign, GringottsAccount account, long cachedBalance) {
        if (sign == null || account == null) {
            throw new IllegalArgumentException(String.format(
                    "null arguments to AccountChest() not allowed. args were: sign: %s, account: %s",
                    sign,
                    account
            ));
        }

        this.sign    = sign;
        this.account = account;
        this.id      = String.format("%s_%d_%d_%d", sign.getWorld().getUID(), sign.getX(), sign.getY(), sign.getZ());
        this.cachedBalance = cachedBalance;
    }

    /**
     * The actual "chest" containing this account chest's stuff.
     *
     * @return InventoryHolder for this account chest
     */
    public InventoryHolder chest() {
        Block block = Util.chestBlock(sign);

        if (block != null) {
            BlockState blockState = PaperLib.getBlockState(
                    block,
                    false
            ).getState();

            if (blockState instanceof InventoryHolder) {
                return (InventoryHolder) blockState;
            }
        }

        return null;
    }

    /**
     * Location of the storage block of this account chest.
     *
     * @return Location of the storage block of this account chest.
     */
    public Location chestLocation() {
        Block block = Util.chestBlock(sign);

        return block != null ? block.getLocation() : null;
    }

    /**
     * Get inventory of this account chest.
     *
     * @return inventory of this AccountChest, if any. otherwise null.
     */
    private Inventory inventory() {
        InventoryHolder inventoryHolder = chest();

        return (inventoryHolder != null) ? inventoryHolder.getInventory() : null;
    }

    /**
     * Get account inventory of this account chest, which is based on the container inventory.
     *
     * @return account inventory of this account chest
     */
    private AccountInventory accountInventory() {
        Inventory inventory = inventory();

        return inventory != null ? new AccountInventory(inventory) : null;
    }

    /**
     * Test if this chest is valid, and if not, removes it from storage.
     *
     * @return true if valid, false if not and was removed from storage.
     */
    private boolean updateInvalid() {
        if (notValid()) {
            Gringotts.instance.getLogger().info("Destroying orphaned vault: " + this);
            destroy();

            return true;
        }
        return false;
    }

    /**
     * Return balance of this chest.
     * Equivalent to AccountChest#balance(false)
     * @return balance of this chest or the cached balance if the chest is unloaded
     */
    public long balance() {
        return balance(false);
    }

    public long balance(boolean forceUpdate) {
        if (!forceUpdate && !isChestLoaded()) return cachedBalance;

        if (updateInvalid()) {
            return 0;
        }

        AccountInventory inv = accountInventory();
        if (inv == null) {
            return 0;
        }

        return inv.balance();
    }

    /**
     * Attempts to add given amount to this chest.
     * If the amount is larger than available space, the space is filled and the actually
     * added amount returned.
     *
     * @param value the value
     * @return amount actually added
     */
    public long add(long value) {
        setCachedBalance(cachedBalance + value);
        if (!isChestLoaded()) {
            emitPendingOperation(value);
            return value;
        }
        if (updateInvalid()) {
            return 0;
        }

        AccountInventory inv = accountInventory();

        return inv == null ? 0 : inv.add(value);
    }

    /**
     * Attempts to remove given amount from this chest.
     * If the amount is larger than available items, everything is removed and the number of
     * removed items returned.
     *
     * @param value amount to remove
     * @return amount actually removed from this chest
     */
    public long remove(long value) {
        long subtracted = Math.min(value, cachedBalance);
        setCachedBalance(cachedBalance - subtracted);
        if (!isChestLoaded()) {
            emitPendingOperation(-subtracted);
            return subtracted;
        }
        if (updateInvalid()) {
            return 0;
        }

        AccountInventory inv = accountInventory();

        return inv == null ? 0 : inv.remove(value);
    }

    /**
     * Checks whether this chest is currently a valid vault.
     * It is considered valid when the sign block contains [vault] or [(type) vault] on the first line,
     * a name on the third line and has a chest associated with it.
     *
     * @return false if the chest can be considered a valid vault
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean notValid() {
        // is it still a sign?
        if (!Util.isSignBlock(sign.getBlock())) {
            return true;
        }

        // Fetch the sign again to avoid strange bug where lines are blank
        String[] lines = Util.getBlockStateAs(sign.getBlock(), Sign.class).get().getLines();
        String line0 = ChatColor.stripColor(lines[0]).trim();

        Matcher match = VAULT_PATTERN.matcher(line0);

        if (!match.matches()) {
            return true;
        }

        if (lines[2] == null) {
            return true;
        }

        if (lines[2].isEmpty()) {
            return true;
        }

        return chest() == null;
    }

    /**
     * Triggered on destruction of physical chest or sign.
     */
    void destroy() {
        Gringotts.instance.getDao().deleteAccountChest(this);

        sign.getBlock().breakNaturally();
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        Location loc = sign.getLocation();

        return "[vault] "
            + loc.getBlockX() + ", "
            + loc.getBlockY() + ", "
            + loc.getBlockZ() + ", "
            + loc.getWorld().getName();
    }

    /**
     * Connected chests that comprise the inventory of this account chest.
     *
     * @return chest blocks connected to this chest, if any
     */
    private Chest[] connectedChests() {
        Inventory inventory = inventory();

        if (inventory == null) {
            return new Chest[0];
        }

        if (inventory instanceof DoubleChestInventory) {
            DoubleChestInventory chestInventory = (DoubleChestInventory) inventory;
            Chest                left           = (Chest) (chestInventory.getLeftSide().getHolder());
            Chest                right          = (Chest) (chestInventory.getRightSide().getHolder());

            return new Chest[]{left, right};
        } else {
            InventoryHolder inventoryHolder = inventory.getHolder();
            if (inventoryHolder instanceof Chest) {
                return new Chest[]{(Chest) (inventory.getHolder())};
            }
        }

        return new Chest[0];
    }


    /**
     * Hash code int.
     *
     * @return the int
     */
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + sign.getLocation().hashCode();

        return result;
    }

    /**
     * Equals boolean.
     *
     * @param obj the obj
     * @return the boolean
     */
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        AccountChest other = (AccountChest) obj;

        return this.sign.getLocation().equals(other.sign.getLocation());
    }

    /**
     * Determine whether the chest of another AccountChest would be connected to this chest.
     *
     * @param chest another AccountChest
     * @return whether the chest of another AccountChest would be connected to this chest
     */
    public boolean connected(AccountChest chest) {
        // no valid account chest anymore -> no connection
        if (updateInvalid()) {
            return false;
        }

        Location myLoc = chestLocation();

        if (myLoc == null) {
            return false;
        }

        if (myLoc.equals(chest.chestLocation())) {
            return true;
        }

        // no double chest -> no further connection possible
        if (!(inventory() instanceof DoubleChestInventory)) {
            return false;
        }

        for (Chest c : chest.connectedChests()) {
            if (c.getLocation().equals(myLoc)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets account.
     *
     * @return the account
     */
    public GringottsAccount getAccount() {
        return account;
    }

    public void updateSign() {
        this.sign.setLine(2, this.account.owner.getName());

        this.sign.update();
    }

    /**
     * Returns true if the chunk containing this account's chest is already loaded in memory
     * (in fact this checks if vault's sign is loaded but we don't care)
     * @return
     */
    public boolean isChestLoaded() {
        return sign.getWorld().isChunkLoaded(sign.getX()/16, sign.getZ()/16);
    }

    public void setCachedBalance(long amount) {
        cachedBalance = amount;
        Gringotts.instance.getDao().updateChestBalance(this, cachedBalance);
    }

    public long getCachedBalance() {
        return cachedBalance;
    }

    private void emitPendingOperation(long amount) {
        Gringotts.instance.getPendingOperationManager()
            .registerNewOperation(new EBeanPendingOperation(this, amount));
    }
}
