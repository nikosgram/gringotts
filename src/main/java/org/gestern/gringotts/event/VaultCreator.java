package org.gestern.gringotts.event;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolder;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

import java.util.Optional;

import static org.gestern.gringotts.Language.LANG;
import static org.gestern.gringotts.Configuration.CONF;

public class VaultCreator implements Listener {
    private final Accounting accounting = Gringotts.getInstance().getAccounting();

    /**
     * If the vault creation event was properly handled and an AccountHolder
     * supplied, it will be created here.
     *
     * @param event event to handle
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void registerVault(PlayerVaultCreationEvent event) {
        // event has been marked invalid, ignore
        if (!event.isValid()) {
            return;
        }

        AccountHolder owner = event.getOwner();

        if (owner == null) {
            return;
        }

        GringottsAccount account = accounting.getAccount(owner);

        SignChangeEvent cause = event.getCause();
        Optional<Sign> optionalSign = Util.getBlockStateAs(
                cause.getBlock(),
                Sign.class);

        if (!optionalSign.isPresent()) {
            return;
        }

        // Check if vault is in any town
        if (CONF.vaultsOnlyInTowns
                && Gringotts.getInstance().getDependencies().hasDependency("towny")
                && TownyAPI.getInstance().getTownBlock(event.getCause().getBlock().getLocation()) == null) {
            event.getCause().getPlayer().sendMessage(LANG.plugin_towny_vaultNotInTown);
            return;
        }

        // Check if town or nation has exceeded the max amount of vaults
        if (Gringotts.getInstance().getDependencies().hasDependency("towny")) {

            IntegerDataField vaultCountDataField = new IntegerDataField("vault_count", 1);

            switch (owner.getType()) {
                case "town":
                    Town town = TownyAPI.getInstance().getTown(owner.getName());
                    IntegerDataField townVaultCount = (IntegerDataField) town.getMetadata("vault_count");

                    if (!town.hasMeta(vaultCountDataField.getKey())) {
                        town.addMetaData(vaultCountDataField);
                        break;
                    }

                    if (CONF.maxTownVaults == -1) { // Keep track of vaults created & break because there is no limit for them
                        townVaultCount.setValue(townVaultCount.getValue() + 1);
                        town.addMetaData(townVaultCount); // Saves to disk
                        break;
                    }

                    if (townVaultCount.getValue() + 1 > CONF.maxTownVaults) {
                        event.getCause().getPlayer().sendMessage(LANG.plugin_towny_tooManyVaults
                                .replace("%max", String.valueOf(CONF.maxTownVaults))
                                .replace("%government", String.valueOf(owner.getType())));
                        return;
                    }

                    townVaultCount.setValue(townVaultCount.getValue() + 1); // Saves to memory
                    town.addMetaData(townVaultCount); // Saves to disk
                    break;

                case "nation":
                    Nation nation = TownyAPI.getInstance().getNation(owner.getName());
                    IntegerDataField nationVaultCount = (IntegerDataField) nation.getMetadata("vault_count");

                    if (!nation.hasMeta(vaultCountDataField.getKey())) {
                        nation.addMetaData(vaultCountDataField);
                        break;
                    }

                    if (CONF.maxNationVaults == -1) { // Keep track of vaults created & break because there is no limit for them
                        nationVaultCount.setValue(nationVaultCount.getValue() + 1);
                        nation.addMetaData(nationVaultCount); // Saves to disk
                        break;
                    }

                    if (nationVaultCount.getValue() + 1 > CONF.maxNationVaults) {
                        event.getCause().getPlayer().sendMessage(LANG.plugin_towny_tooManyVaults
                                .replace("%max", String.valueOf(CONF.maxNationVaults))
                                .replace("%government", String.valueOf(owner.getType())));
                        return;
                    }

                    nationVaultCount.setValue(nationVaultCount.getValue() + 1); // Saves to memory
                    nation.addMetaData(nationVaultCount); // Saves to disk
                    break;

                default:
                    break;
            }
        }

        // create account chest
        AccountChest accountChest = new AccountChest(optionalSign.get(), account);

        // check for existence / add to tracking
        if (accounting.addChest(accountChest)) {
            String firstLine = cause.getLine(0);

            if (firstLine != null && firstLine.length() <= 16) {
                cause.setLine(0, " " + ChatColor.BOLD + firstLine + " ");
            }

            cause.setLine(2, owner.getName());
            cause.getPlayer().sendMessage(LANG.vault_created);
        } else {
            cause.setCancelled(true);
            cause.getPlayer().sendMessage(LANG.vault_error);
        }
    }
}
