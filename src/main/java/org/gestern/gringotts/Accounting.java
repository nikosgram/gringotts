package org.gestern.gringotts;

import org.gestern.gringotts.accountholder.AccountHolder;

import java.util.Collection;

/**
 * Manages accounts.
 *
 * @author jast
 */
public class Accounting {
    /**
     * Get the account associated with an account holder.
     * If it was not yet stored in the data storage, it will be persisted.
     *
     * @param owner account holder
     * @return account associated with an account holder
     */
    public GringottsAccount getAccount(AccountHolder owner) {
        GringottsAccount account = new GringottsAccount(owner);

        Gringotts.instance.getDao().storeAccount(account);

        return account;
    }

    /**
     * Determine if a given AccountChest would be connected to an AccountChest already in storage.
     * Alas! need to call this every time we try to add an account chest, since chests can be added
     * without us noticing ...
     *
     * @param chest     chest to check for connectedness
     * @param allChests set of all chests that might be a candidate for connectedness
     * @return whether given chest is connected to any existing chest
     */
    // TODO perhaps this could be more elegantly done with block metadata
    private boolean chestConnected(AccountChest chest, Collection<AccountChest> allChests) {
        for (AccountChest ac : allChests) {
            if (ac.connected(chest)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Save an AccountChest to Account association.
     *
     * @param chest chest to add to the account
     * @return false if the specified AccountChest is already registered or would be connected to a registered chest. true if the association was successful.
     * @throws GringottsStorageException when saving of account chest failed
     */
    public boolean addChest(AccountChest chest) {
        // TODO refactor to do a more intelligent/quick query
        Collection<AccountChest> allChests = Gringotts.instance.getDao().retrieveChests();

        // if there is an invalid stored chest on location of new chest, remove it from storage.
        if (allChests.contains(chest)) {
            Gringotts.instance.getLogger().info("removing orphaned vault: " + chest);
            Gringotts.instance.getDao().deleteAccountChest(chest);
            allChests.remove(chest);
        }

        if (chestConnected(chest, allChests)) {
            return false;
        }

        if (!Gringotts.instance.getDao().storeAccountChest(chest)) {
            throw new GringottsStorageException("Could not save account chest: " + chest);
        }

        return true;
    }

}
