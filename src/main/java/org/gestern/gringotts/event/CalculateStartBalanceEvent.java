package org.gestern.gringotts.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.jetbrains.annotations.NotNull;

/**
 * The type Calculate start balaance event.
 */
public class CalculateStartBalanceEvent extends Event {
    /**
     * The constant handlers.
     */
    public static final HandlerList   handlers   = new HandlerList();
    /**
     * The Holder.
     */
    public final        AccountHolder holder;
    /**
     * The Start value.
     */
    public              long          startValue = 0;

    /**
     * Instantiates a new Calculate start balance event.
     *
     * @param holder the holder
     */
    public CalculateStartBalanceEvent(AccountHolder holder) {
        super(!Bukkit.getServer().isPrimaryThread());

        this.holder = holder;
    }

    /**
     * Gets handler list.
     *
     * @return the handler list
     */
    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
