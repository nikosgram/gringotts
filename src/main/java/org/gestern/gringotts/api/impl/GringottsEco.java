package org.gestern.gringotts.api.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.accountholder.AccountHolderFactory;
import org.gestern.gringotts.accountholder.PlayerAccountHolder;
import org.gestern.gringotts.api.*;
import org.gestern.gringotts.currency.GringottsCurrency;
import org.gestern.gringotts.data.DAO;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * The type Gringotts eco.
 */
public class GringottsEco implements Eco {
    private static final String               TAG_PLAYER    = "player";
    private final        AccountHolderFactory accountOwners = Gringotts.instance.getAccountHolderFactory();
    private final        DAO                  dao           = Gringotts.instance.getDao();

    /**
     * Account account.
     *
     * @param id the id
     * @return the account
     */
    @Override
    public Account account(String id) {
        AccountHolder owner = accountOwners.get(id);

        if (owner == null) {
            return new InvalidAccount("virtual", id);
        }

        GringottsAccount gAccount = Gringotts.instance.getAccounting().getAccount(owner);

        return new ValidAccount(gAccount);
    }

    /**
     * Player player account.
     *
     * @param id the id
     * @return the player account
     */
    @Override
    public PlayerAccount player(UUID id) {
        AccountHolder owner = accountOwners.get(TAG_PLAYER, id.toString());

        if (owner instanceof PlayerAccountHolder) {
            return new ValidPlayerAccount(Gringotts.instance.getAccounting().getAccount(owner));
        }

        return new InvalidAccount(TAG_PLAYER, id.toString());
    }

    /**
     * Bank bank account.
     *
     * @param name the name
     * @return the bank account
     */
    @Override
    public BankAccount bank(String name) {
        return new InvalidAccount("bank", name);
    }

    /**
     * Custom account.
     *
     * @param type the type
     * @param id   the id
     * @return the account
     */
    @Override
    public Account custom(String type, String id) {
        AccountHolder owner = accountOwners.get(type, id);

        if (owner == null) {
            return new InvalidAccount(type, id);
        }

        GringottsAccount acc = new GringottsAccount(owner);

        return new ValidAccount(acc);
    }

    /**
     * Currency currency.
     *
     * @return the currency
     */
    @Override
    public Currency currency() {
        return new Curr(Configuration.CONF.getCurrency());
    }

    /**
     * Supports banks boolean.
     *
     * @return the boolean
     */
    @Override
    public boolean supportsBanks() {
        return true;
    }

    /**
     * Gets banks.
     *
     * @return the banks
     */
    @Override
    public Set<String> getBanks() {
        // TODO implement banks
        return Collections.emptySet();
    }

    /**
     * Gets account.
     *
     * @param id the id
     * @return the account
     */
    @Override
    public Account getAccount(String id) {
        String[] parts = id.split(":");

        if (parts.length == 1) {
            OfflinePlayer player = Util.getOfflinePlayer(id);

            if (player != null) {
                return player(player.getUniqueId());
            }

            return account(id);
        }

        return custom(parts[0], parts[1]);
    }

    private static class InvalidAccount implements BankAccount, PlayerAccount {

        private final String type;
        private final String id;

        /**
         * Instantiates a new Invalid account.
         *
         * @param type the type
         * @param id   the id
         */
        InvalidAccount(String type, String id) {
            this.type = type;
            this.id = id;
        }

        /**
         * Exists boolean.
         *
         * @return the boolean
         */
        @Override
        public boolean exists() {
            return false;
        }

        /**
         * Create account.
         *
         * @return the account
         */
        @Override
        public Account create() {
            // TODO if account type allows virtual accounts, create it
            return this;
        }

        /**
         * Delete account.
         *
         * @return the account
         */
        @Override
        public Account delete() {
            return this; // delete invalid account is still invalid
        }

        /**
         * Balance double.
         *
         * @return the double
         */
        @Override
        public double balance() {
            return 0; // invalid account has 0 balance
        }

        /**
         * Vault balance double.
         *
         * @return the double
         */
        @Override
        public double vaultBalance() {
            return 0;
        }

        /**
         * Vault balance double.
         *
         * @return the double
         */
        @Override
        public double vaultBalance(int index) {
            return 0;
        }

        /**
         * Vault location.
         *
         * @return the location
         */
        @Override
        public Location vaultLocation(int index) {
            return null;
        }

        /**
         * Vault count.
         *
         * @return the int
         */
        @Override
        public int vaultCount() {
            return 0;
        }

        /**
         * Inv balance double.
         *
         * @return the double
         */
        @Override
        public double invBalance() {
            return 0;
        }

        @Override
        public double endBalance() {
            return 0;
        }

        /**
         * Has boolean.
         *
         * @param value the value
         * @return the boolean
         */
        @Override
        public boolean has(double value) {
            return false; // invalid account has nothing
        }

        /**
         * Sets balance.
         *
         * @param newBalance the new balance
         * @return the balance
         */
        @Override
        public TransactionResult setBalance(double newBalance) {
            return TransactionResult.ERROR;
        }

        /**
         * Add transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult add(double value) {
            return TransactionResult.ERROR;
        }

        /**
         * Remove transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult remove(double value) {
            return TransactionResult.ERROR;
        }

        /**
         * Send transaction.
         *
         * @param value the value
         * @return the transaction
         */
        @Override
        public Transaction send(double value) {
            return new GringottsTransaction(this, value);
        }

        /**
         * Type string.
         *
         * @return the string
         */
        @Override
        public String type() {
            return type;
        }

        /**
         * Id string.
         *
         * @return the string
         */
        @Override
        public String id() {
            return id;
        }

        /**
         * Message.
         *
         * @param message the message
         */
        @Override
        public void message(String message) {
            // do nothing - no owner on this
        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }

        @Override
        public Collection<AccountChest> getVaultChests() {
            return Collections.emptyList();
        }

        /**
         * Add owner bank account.
         *
         * @param player the player
         * @return the bank account
         */
        @Override
        public BankAccount addOwner(String player) {
            return this;
        }

        /**
         * Add member bank account.
         *
         * @param player the player
         * @return the bank account
         */
        @Override
        public BankAccount addMember(String player) {
            return this;
        }

        /**
         * Is owner boolean.
         *
         * @param player the player
         * @return the boolean
         */
        @Override
        public boolean isOwner(String player) {
            return false;
        }

        /**
         * Is member boolean.
         *
         * @param player the player
         * @return the boolean
         */
        @Override
        public boolean isMember(String player) {
            return false;
        }

        /**
         * Deposit transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult deposit(double value) {
            return TransactionResult.ERROR;
        }

        /**
         * Withdraw transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult withdraw(double value) {
            return TransactionResult.ERROR;
        }

        /**
         * Can add boolean.
         *
         * @param value the value
         * @return the boolean
         */
        @Override
        public boolean canAdd(double value) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private static class Curr implements Currency {

        /**
         * The Gcurr.
         */
        final GringottsCurrency gcurr;
        /**
         * The Format string.
         */
        final String formatString; // TODO this should be configurable

        /**
         * Instantiates a new Curr.
         *
         * @param curr the curr
         */
        Curr(GringottsCurrency curr) {
            this.gcurr = curr;
            formatString = "%." + curr.getDigits() + "f %s";
        }

        /**
         * Gets name.
         *
         * @return the name
         */
        @Override
        public String getName() {
            return gcurr.getName();
        }

        /**
         * Gets name plural.
         *
         * @return the name plural
         */
        @Override
        public String getNamePlural() {
            return gcurr.getNamePlural();
        }

        /**
         * Format string.
         *
         * @param value the value
         * @return the string
         */
        @Override
        public String format(double value) {
            return Configuration.CONF.getCurrency().format(formatString, value);
        }

        /**
         * Gets fractional digits.
         *
         * @return the fractional digits
         */
        @Override
        public int getFractionalDigits() {
            return gcurr.getDigits();
        }
    }

    private class ValidAccount implements Account {

        /**
         * The Acc.
         */
        protected final GringottsAccount acc;

        /**
         * Instantiates a new Valid account.
         *
         * @param acc the acc
         */
        public ValidAccount(GringottsAccount acc) {
            this.acc = acc;
        }

        /**
         * Exists boolean.
         *
         * @return the boolean
         */
        @Override
        public boolean exists() {
            // since this is a valid account, returns true
            return true;
        }

        /**
         * Create account.
         *
         * @return the account
         */
        @Override
        public Account create() {
            return this;
        }

        /**
         * Delete account.
         *
         * @return the account
         */
        @Override
        public Account delete() {
            dao.deleteAccount(acc);
            throw new RuntimeException("deleting accounts not supported by Gringotts");
        }

        /**
         * Balance double.
         *
         * @return the double
         */
        @Override
        public double balance() {
            return Configuration.CONF.getCurrency().getDisplayValue(acc.getBalance());
        }

        /**
         * Vault balance double.
         *
         * @return the double
         */
        @Override
        public double vaultBalance() {
            return Configuration.CONF.getCurrency().getDisplayValue(acc.getVaultBalance());
        }

        /**
         * Vault balance double.
         *
         * @return the double
         */
        @Override
        public double vaultBalance(int index) {
            return Configuration.CONF.getCurrency().getDisplayValue(acc.getVaultBalance(index));
        }

        /**
         * Vault location.
         *
         * @return the location
         */
        @Override
        public Location vaultLocation(int index) {
            return acc.getVaultLocation(index);
        }

        /**
         * Vault count.
         *
         * @return the int
         */
        @Override
        public int vaultCount() {
            return acc.getVaultCount();
        }

        /**
         * Inv balance double.
         *
         * @return the double
         */
        @Override
        public double invBalance() {
            return Configuration.CONF.getCurrency().getDisplayValue(acc.getInvBalance());
        }

        @Override
        public double endBalance() {
            return Configuration.CONF.getCurrency().getDisplayValue(acc.getEndBalance());
        }

        /**
         * Has boolean.
         *
         * @param value the value
         * @return the boolean
         */
        @Override
        public boolean has(double value) {
            return acc.getBalance() >= Configuration.CONF.getCurrency().getCentValue(value);
        }

        /**
         * Sets balance.
         *
         * @param newBalance the new balance
         * @return the balance
         */
        @Override
        public TransactionResult setBalance(double newBalance) {
            return add(balance() - newBalance);
        }

        /**
         * Add transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult add(double value) {
            if (value < 0) {
                return remove(-value);
            }

            return acc.add(Configuration.CONF.getCurrency().getCentValue(value));
        }

        /**
         * Remove transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult remove(double value) {
            if (value < 0) {
                return add(-value);
            }

            return acc.remove(Configuration.CONF.getCurrency().getCentValue(value));
        }

        /**
         * Send transaction.
         *
         * @param value the value
         * @return the transaction
         */
        @Override
        public Transaction send(double value) {
            return new GringottsTransaction(this, value);
        }

        /**
         * Type string.
         *
         * @return the string
         */
        @Override
        public String type() {
            return acc.owner.getType();
        }

        /**
         * Id string.
         *
         * @return the string
         */
        @Override
        public String id() {
            return acc.owner.getId();
        }

        /**
         * Message.
         *
         * @param message the message
         */
        @Override
        public void message(String message) {
            acc.owner.sendMessage(message);
        }

        @Override
        public boolean hasPermission(String permission) {
            return acc.owner.hasPermission(permission);
        }

        @Override
        public Collection<AccountChest> getVaultChests() {
            return acc.getVaultChests();
        }

        /**
         * Can add boolean.
         *
         * @param value the value
         * @return the boolean
         */
        @Override
        public boolean canAdd(double value) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private class ValidPlayerAccount extends ValidAccount implements PlayerAccount {

        /**
         * Instantiates a new Valid player account.
         *
         * @param acc the acc
         */
        public ValidPlayerAccount(GringottsAccount acc) {
            super(acc);
        }

        /**
         * Deposit transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult deposit(double value) {
            PlayerAccountHolder owner = (PlayerAccountHolder) acc.owner;
            Player player = Bukkit.getPlayer(owner.getUUID());

            if (player == null) {
                return TransactionResult.ERROR;
            }

            AccountInventory playerInventory = new AccountInventory(player.getInventory());
            long             centValue       = Configuration.CONF.getCurrency().getCentValue(value);
            long             toDeposit       = playerInventory.remove(centValue);

            if (toDeposit > centValue) {
                toDeposit -= playerInventory.add(toDeposit - centValue);
            }

            TransactionResult result = player(player.getUniqueId()).add(Configuration.CONF.getCurrency().getDisplayValue(toDeposit));

            if (result != TransactionResult.SUCCESS) {
                playerInventory.add(toDeposit);
            }

            return result;
        }

        /**
         * Withdraw transaction result.
         *
         * @param value the value
         * @return the transaction result
         */
        @Override
        public TransactionResult withdraw(double value) {
            PlayerAccountHolder owner = (PlayerAccountHolder) acc.owner;
            Player player = Bukkit.getPlayer(owner.getUUID());

            if (player == null) {
                return TransactionResult.ERROR;
            }

            AccountInventory  playerInventory = new AccountInventory(player.getInventory());
            long              centValue       = Configuration.CONF.getCurrency().getCentValue(value);
            TransactionResult remove          = acc.remove(centValue);

            if (remove == TransactionResult.SUCCESS) {
                long withdrawn = playerInventory.add(centValue);
                return acc.add(centValue - withdrawn); // add possible leftovers back to account
            }

            return remove;
        }

        /**
         * Can add boolean.
         *
         * @param value the value
         * @return the boolean
         */
        @Override
        public boolean canAdd(double value) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
