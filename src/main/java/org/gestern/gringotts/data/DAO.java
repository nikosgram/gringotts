package org.gestern.gringotts.data;

import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.GringottsAccount;
import org.gestern.gringotts.GringottsStorageException;
import org.gestern.gringotts.accountholder.AccountHolder;

import java.util.Collection;

/**
 * The interface Dao.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface DAO {
    /**
     * Store a new account chest to database.
     *
     * @param chest chest to store
     * @return true if chest was stored, false otherwise
     * @throws GringottsStorageException when storage failed
     */
    boolean storeAccountChest(AccountChest chest);

    /**
     * Update a chest's balance oin the database
     * @param chest chest to update
     * @param balance chest's new balance
     * @return true if update was successful, false otherwise
     */
    boolean updateChestBalance(AccountChest chest, long balance);

    /**
     * Deletes an account chest from the datastore.
     *
     * @param chest chest to delete
     * @return true if the chest was deleted, false if no chest was deleted.
     */
    boolean deleteAccountChest(AccountChest chest);

    /**
     * Store the given Account to DB.
     *
     * @param account account to store
     * @return true if an account was stored, false if it already existed
     */
    boolean storeAccount(GringottsAccount account);

    /**
     * Return whether a given account owner has an account.
     *
     * @param accountHolder account holder to check
     * @return true if the account holder has an account associated with them
     */
    boolean hasAccount(AccountHolder accountHolder);

    /**
     * Rename account boolean.
     *
     * @param type    the type
     * @param holder  the holder
     * @param newName the new name
     * @return the boolean
     */
    boolean renameAccount(String type, AccountHolder holder, String newName);

    /**
     * Rename account boolean.
     *
     * @param type    the type
     * @param oldName the old name
     * @param newName the new name
     * @return the boolean
     */
    boolean renameAccount(String type, String oldName, String newName);

    /**
     * Get set of all chests registered with Gringotts.
     * If a stored chest turns out to be invalid, that chest may be removed from storage.
     *
     * @return set of all chests registered with Gringotts
     */
    Collection<AccountChest> retrieveChests();

    /**
     * Get all chests belonging to the given account.
     * If a stored chest turns out to be invalid, that chest is removed from storage.
     *
     * @param account account to fetch chests for.
     * @return account to get chests for
     */
    Collection<AccountChest> retrieveChests(GringottsAccount account);

    /**
     * Gets accounts.
     *
     * @return the accounts
     */
    Collection<String> getAccounts();

    /**
     * Gets accounts.
     *
     * @param type the type
     * @return the accounts
     */
    Collection<String> getAccounts(String type);

    /**
     * Store an amount of cents to a given account.
     *
     * @param account account to store amount to
     * @param amount  amount to store to account
     * @return true if storing was successful, false otherwise.
     */
    boolean storeCents(GringottsAccount account, long amount);

    /**
     * Get the cents stored for a given account.
     *
     * @param account account to query
     * @return amount of cents stored in the account, 0 if the account is not stored
     */
    long retrieveCents(GringottsAccount account);

    /**
     * Delete an account and associated data from the storage.
     *
     * @param acc account to delete
     * @return the boolean
     */
    boolean deleteAccount(GringottsAccount acc);

    /**
     * Delete account boolean.
     *
     * @param type    the type
     * @param account the account
     * @return the boolean
     */
    boolean deleteAccount(String type, String account);

    /**
     * Delete account chests boolean.
     *
     * @param acc the acc
     * @return the boolean
     */
    boolean deleteAccountChests(GringottsAccount acc);

    /**
     * Delete account chests boolean.
     *
     * @param account the account
     * @return the boolean
     */
    boolean deleteAccountChests(String account);

    /**
     * Delete account chests
     * @param world
     * @param x
     * @param y
     * @param z
     * @return boolean indicating success or failure
     */
    boolean deleteAccountChest(String world, int x, int y, int z);

    /**
     * Shutdown the database connection.
     */
    void shutdown();
}
