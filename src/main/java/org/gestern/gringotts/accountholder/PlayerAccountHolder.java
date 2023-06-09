package org.gestern.gringotts.accountholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerAccountHolder implements AccountHolder {

    /**
     * Actual player owning the account.
     */
    public final OfflinePlayer accountHolder;

    public PlayerAccountHolder(OfflinePlayer player) {
        if (player != null)
            this.accountHolder = player;
        else throw new IllegalArgumentException("Attempted to create account holder with null player.");
    }

    @Override
    public String getName() {
        return accountHolder.getName();
    }

    @Override
    public void sendMessage(String message) {
        if (accountHolder.isOnline()) {
            Player player = accountHolder.getPlayer();

            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getUUID().hashCode();
    }

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

        PlayerAccountHolder other = (PlayerAccountHolder) obj;

        return getUUID().equals(other.getUUID());
    }

    @Override
    public String getType() {
        return "player";
    }

    @Override
    public String toString() {
        return "PlayerAccountHolder(" + getName() + ")";
    }

    @Override
    public String getId() {
        return accountHolder.getUniqueId().toString();
    }

    public UUID getUUID() {
        return accountHolder.getUniqueId();
    }

    @Override
    public boolean hasPermission(String permission) {
        if (accountHolder.isOnline()) {
            Player player = accountHolder.getPlayer();

            if (player != null) {
                return player.hasPermission(permission);
            }
        }

        return false;
    }
}
