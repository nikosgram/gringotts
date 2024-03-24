package org.gestern.gringotts.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for chest creation and destruction events.
 *
 * @author jast
 */
public class AccountListener implements Listener {

    private final Pattern VAULT_PATTERN = Pattern.compile(Configuration.CONF.vaultPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Create an account chest by adding a sign marker over it.
     *
     * @param event Event data.
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0String = ChatColor.stripColor(event.getLine(0)).trim();

        if (line0String == null) {
            return;
        }

        Matcher match = VAULT_PATTERN.matcher(line0String);

        // consider only signs with proper formatting
        if (!match.matches()) {
            return;
        }

        String typeStr = match.group(1).toUpperCase();

        String type;

        // default vault is player
        if (typeStr.isEmpty()) {
            type = "player";
        } else {
            type = typeStr.toLowerCase();
        }

        Optional<Sign> optionalSign = Util.getBlockStateAs(
                event.getBlock(),
                Sign.class
        );

        if (optionalSign.isPresent() && Util.chestBlock(optionalSign.get()) != null) {
            // we made it this far, throw the event to manage vault creation
            final VaultCreationEvent creation = new PlayerVaultCreationEvent(type, event);

            Bukkit.getServer().getPluginManager().callEvent(creation);
        }
    }
}
