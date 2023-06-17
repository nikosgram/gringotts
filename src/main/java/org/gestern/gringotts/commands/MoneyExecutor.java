package org.gestern.gringotts.commands;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gestern.gringotts.Language;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Player commands.
 */
public class MoneyExecutor extends GringottsAbstractExecutor {
    private static final List<String> commands = Arrays.asList("", "withdraw", "deposit", "send");

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender       Source of the command
     * @param cmd          Command which was executed
     * @param commandLabel Alias of the command which was used
     * @param args         Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String commandLabel,
                             String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.LANG.playerOnly);
            return false;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // same as balance
            sendBalanceMessage(eco.player(player.getUniqueId()));

            return true;
        }

        if (args.length < 2) return false;

        String command = args[0];

        switch (command.toLowerCase()) {
            case "withdraw": {
                try {
                    double value = Double.parseDouble(args[1]);

                    withdraw(player, value);

                    return true;
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Language.LANG.invalid_number.replace("%value", args[1]));
                    return true;
                }
            }
            case "deposit": {
                try {
                    double value = Double.parseDouble(args[1]);

                    deposit(player, value);

                    return true;
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Language.LANG.invalid_number.replace("%value", args[1]));
                    return true;
                }
            }
            case "send": {
                if (args.length < 3) return false;
                try {
                    double value = Double.parseDouble(args[2]);

                    // money send <player> <amount>
                    return pay(player, value, args[1]);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Language.LANG.invalid_number.replace("%value", args[2]));
                    return true;
                }
            }
            case "pay": {
                if (args.length < 3) return false;
                try {
                    double value = Double.parseDouble(args[1]);

                    // money pay <amount> <player>
                    return pay(player, value, args[2]);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Language.LANG.invalid_number.replace("%value", args[1]));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside of a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param alias   The alias used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed and command label
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        String cmd = args[0].toLowerCase();

        switch (args.length) {
            case 1: {
                return commands.stream()
                        .filter(com -> startsWithIgnoreCase(com, args[0]))
                        .collect(Collectors.toList());
            }
            case 2: {
                if ("send".equals(cmd)) {
                    return suggestAccounts(args[1]);
                }
                break;
            }
            case 3: {
                if ("pay".equals(cmd)) {
                    return suggestAccounts(args[2]);
                }
                break;
            }
        }

        return Lists.newArrayList();
    }
}
