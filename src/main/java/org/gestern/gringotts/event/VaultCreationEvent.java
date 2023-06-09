package org.gestern.gringotts.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.gestern.gringotts.Language;
import org.gestern.gringotts.accountholder.AccountHolder;

/**
 * Event that is thrown after Gringotts detects a vault creation.
 * When thrown, it includes the type of the vault, for example "player"
 * and is set to invalid with an empty message.
 * <p>
 * Listeners may set the event to valid, which will cause a vault of the given type to be
 * created by Gringotts. Optionally, a custom message will be sent to the owner of the account.
 *
 * @author jast
 */
public class VaultCreationEvent extends Event {

    /**
     * The constant handlers.
     */
    protected static final HandlerList handlers = new HandlerList();

    private final String type;
    private boolean isValid = false;
    private AccountHolder owner;

    /**
     * Create VaultCreationEvent for a given vault type.
     *
     * @param type Type of vault being created
     */
    public VaultCreationEvent(String type) {
        this.type = type;
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

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Return whether this is a valid vault type.
     * false by default. A Listener may set this to true.
     *
     * @return whether this is a valid vault.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Set valid status of vault being created. This is false by default.
     * A listener that sets this to true must also ensure that an owner is supplied.
     *
     * @param valid valid status to set
     */
    public void setValid(boolean valid) {
        this.isValid = valid;
    }

    /**
     * Get message sent to account owner on creation of this vault.
     *
     * @return message sent to account owner on creation of this vault.
     */
    public String getMessage() {
        return Language.LANG.vault_created;
    }

    /**
     * Get account holder supplied as owner for the vault being created.
     *
     * @return account holder supplied as owner for the vault being created.
     */
    public AccountHolder getOwner() {
        return owner;
    }

    /**
     * Set the account holder acting as the owner of the vault being created.
     * When the valid status is also true, this will enable the vault to be registered with Gringotts.
     *
     * @param owner owner of the vault being created
     */
    public void setOwner(AccountHolder owner) {
        this.owner = owner;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
