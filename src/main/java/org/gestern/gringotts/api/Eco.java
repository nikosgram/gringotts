package org.gestern.gringotts.api;

import java.util.Set;
import java.util.UUID;

/**
 * The interface Eco.
 */
@SuppressWarnings("unused")
public interface Eco {

    /**
     * Access a generic account for the given id. The type of the account is defined by the economy plugin.
     * The recommended behavior is to return a player account if a player of the name exists, and return any other
     * type of account
     * if no player of that name is known to the economy.
     *
     * @param id id of the account
     * @return The account representation.
     */
    Account account(String id);

    /**
     * Access a player account for the player with given uuid. If a player can have multiple accounts,
     * the account returned is defined by the economy plugin. For example, if the economy supports one account per
     * world,
     * return the account associated with the world the player is currently in.
     * The representation of the money of a player account may be in-game items or virtual.
     *
     * @param id The unique id of the player owning the account.
     * @return The player account representation
     */
    PlayerAccount player(UUID id);

    /**
     * Access a bank account with given name.
     * The representation of the money in a bank account may be in-game items or virtual.
     * Support for this method is optional.
     *
     * @param name The name or id of the bank.
     * @return The bank account representation.
     */
    BankAccount bank(String name);

    /**
     * Access custom account type. Implementors must support this method, but may choose to implement via another
     * account type internally.
     *
     * @param type type of account
     * @param id   account id
     * @return an account of the given type and id
     */
    Account custom(String type, String id);

    /**
     * The currency for this Economy.
     *
     * @return The currency for this Economy.
     */
    Currency currency();

    /**
     * Return whether this economy supports banks.
     *
     * @return true if this economy has bank support, false otherwise.
     */
    boolean supportsBanks();

    /**
     * Get a set of all banks currently registered by the economy.
     *
     * @return a set of the names of all banks currently registered by the economy.
     */
    Set<String> getBanks();

    /**
     * Gets account.
     *
     * @param id the id
     * @return the account
     */
    Account getAccount(String id);
}
