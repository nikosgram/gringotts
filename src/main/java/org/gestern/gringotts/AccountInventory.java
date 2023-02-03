package org.gestern.gringotts;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.gestern.gringotts.currency.Denomination;
import org.gestern.gringotts.currency.GringottsCurrency;

import java.util.List;
import java.util.ListIterator;

/**
 * Account inventories define operations that can be used on all inventories belonging to an account.
 *
 * @author jast
 */
public class AccountInventory {
    private final Inventory inventory;

    public AccountInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Current balance of this inventory in cents (or rather atomic currency units).
     *
     * @return current balance of this inventory in cents
     */
    public long balance() {
        GringottsCurrency cur = Configuration.CONF.getCurrency();
        long count = 0;

        for (ItemStack stack : inventory) {
            count += cur.getValue(stack);
        }

        return count;
    }

    /**
     * Add items to this inventory corresponding to given value.
     * If the amount is larger than available space, the space is filled and the actually
     * added amount returned.
     *
     * @param value value to add to this inventory
     * @return amount actually added
     */
    public long add(long value) {
        long remaining = value;

        // try denominations from largest to smallest
        for (Denomination denomination : Configuration.CONF.getCurrency().getDenominations()) {
            if (denomination.getValue() <= remaining && denomination.getValue() > 0) {
                ItemStack stack = new ItemStack(denomination.getKey().type);
                int stackSize = stack.getMaxStackSize();
                long denItemCount = remaining / denomination.getValue();

                // add stacks in this denomination until stuff is returned
                while (denItemCount > 0) {
                    int remainderStackSize = denItemCount > stackSize ? stackSize : (int) denItemCount;
                    stack.setAmount(remainderStackSize);

                    int returned = 0;
                    for (ItemStack leftover : inventory.addItem(stack).values()) {
                        returned += leftover.getAmount();
                    }

                    // reduce remaining amount by whatever was deposited
                    long added = (long) remainderStackSize - returned;

                    denItemCount -= added;
                    remaining -= added * denomination.getValue();

                    // no more space for this denomination
                    if (returned > 0) {
                        break;
                    }
                }
            }
        }

        return value - remaining;
    }

    /**
     * Remove items from this inventory corresponding to given value.
     *
     * @param value amount to remove
     * @return value actually removed
     */
    public long remove(long value) {

        // avoid dealing with negatives
        if (value <= 0) {
            return 0;
        }

        GringottsCurrency cur = Configuration.CONF.getCurrency();
        long remaining = value;

        // try denominations from smallest to largest
        List<Denomination> denominations = cur.getDenominations();
        for (ListIterator<Denomination> it = denominations.listIterator(denominations.size()); it.hasPrevious(); ) {
            Denomination denomination = it.previous();
            ItemStack stack = new ItemStack(denomination.getKey().type);
            int stackSize = stack.getMaxStackSize();

            // take 1 more than necessary if it doesn't round. add the extra later
            long denItemCount = (long) Math.ceil((double) remaining / denomination.getValue());

            // add stacks in this denomination until stuff is returned or we are done
            while (denItemCount > 0) {
                int remainderStackSize = denItemCount > stackSize ? stackSize : (int) denItemCount;
                stack.setAmount(remainderStackSize);

                int returned = 0;

                for (ItemStack leftover : inventory.removeItem(stack).values()) {
                    returned += leftover.getAmount();
                }

                // reduce remaining amount by whatever was removed
                long removed = (long) remainderStackSize - returned;
                denItemCount -= removed;
                remaining -= removed * denomination.getValue();

                // stuff was returned, no more items of this type to take
                if (returned > 0) {
                    break;
                }
            }

        }
        return value - remaining;
    }
}
