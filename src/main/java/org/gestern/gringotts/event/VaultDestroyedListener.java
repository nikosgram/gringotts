package org.gestern.gringotts.event;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.accountholder.AccountHolder;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

public class VaultDestroyedListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockBreak(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        List<AccountChest> chests = Gringotts.getInstance().getDao().retrieveChests();

        // The block itself is actually a sign
        for (AccountChest chest : chests) {
            if (chest.sign.getBlock().getLocation().equals(blockLoc)
                    || chest.chestLocation().equals(blockLoc)) {

                AccountHolder owner = chest.getAccount().owner;

                switch (owner.getType()) {
                    case "town":
                        Town town = TownyAPI.getInstance().getTown(owner.getName());

                        IntegerDataField townVaultCount = (IntegerDataField) town.getMetadata("vault_count");
                        townVaultCount.setValue(townVaultCount.getValue() - 1);

                        break;

                    case "nation":
                        Nation nation = TownyAPI.getInstance().getNation(owner.getName());

                        IntegerDataField nationVaultCount = (IntegerDataField) nation.getMetadata("vault_count");
                        nationVaultCount.setValue(nationVaultCount.getValue() - 1);

                        break;

                    default:
                        break;
                }
            }
        }
    }
}
