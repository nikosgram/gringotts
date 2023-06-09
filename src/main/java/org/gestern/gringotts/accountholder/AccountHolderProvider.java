package org.gestern.gringotts.accountholder;


import org.bukkit.OfflinePlayer;
import org.gestern.gringotts.event.VaultCreationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Provides AccountHolder objects for a given id.
 * An AccountHolderProvider has its own internal mapping of ids to account holders.
 *
 * @author jast
 */
public interface AccountHolderProvider {

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param id id of account holder
     * @return account holder for id
     */
    @Nullable AccountHolder getAccountHolder(@NotNull String id);

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param uuid id of account holder
     * @return account holder for id
     */
    @Nullable AccountHolder getAccountHolder(@NotNull UUID uuid);

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param player the target player
     * @return account holder for id
     */
    @Nullable AccountHolder getAccountHolder(@NotNull OfflinePlayer player);

    /**
     * Gets type.
     *
     * @return the type
     */
    @NotNull String getType();

    /**
     * Gets account names.
     *
     * @return the account names
     */
    @NotNull Set<String> getAccountNames();
}
