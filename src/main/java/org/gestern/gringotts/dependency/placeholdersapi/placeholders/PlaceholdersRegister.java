package org.gestern.gringotts.dependency.placeholdersapi.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.api.Account;
import org.gestern.gringotts.api.impl.GringottsEco;
import org.jetbrains.annotations.NotNull;

public class PlaceholdersRegister extends PlaceholderExpansion  {

    private final GringottsEco eco;

    public PlaceholdersRegister(Gringotts plugin) {
        eco = (GringottsEco) plugin.getEco();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gringotts";
    }

    @Override
    public @NotNull String getAuthor() {
        return "github.com/CamillePele";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String paramString) {
        String[] params = paramString.split("_");
        Account account = eco.player(player.getUniqueId());

        if(params[0].equalsIgnoreCase("balance") || params[0].equalsIgnoreCase("money")) {

            if (params.length == 2) {
                if (params[1].equalsIgnoreCase("vault")) {
                    return String.valueOf(account.vaultBalance());
                }
                if (params[1].equalsIgnoreCase("inventory")) {
                    return String.valueOf(account.invBalance());
                }
            }
            else if (params.length == 1) {
                return String.valueOf(account.balance());
            }
        }
        else if (params[0].equalsIgnoreCase("vault") && params.length >= 2) {

            if (params[1].equalsIgnoreCase("count")) {
                return String.valueOf(account.vaultCount());
            }
            else if (params.length == 3) {
                try {
                    int index = Integer.parseInt(params[1]);
                    if (index >= account.vaultCount()) {
                        return "out of bounds";
                    }


                    if (params[2].equalsIgnoreCase("location") || params[2].equalsIgnoreCase("position")) {
                        Location loc = account.vaultLocation(index);
                        if (loc == null) {
                            return null;
                        }
                        if (loc.getWorld() == null) {
                            return (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ();
                        }
                        return loc.getWorld().getName() + ", " + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ();
                    }

                    if (params[2].equalsIgnoreCase("balance")) {
                        return String.valueOf(account.vaultBalance(index));
                    }

                } catch (NumberFormatException e) {
                    return "invalid index";
                }
            }
        }

        return null; // Placeholder is unknown by the Expansion
    }
}
