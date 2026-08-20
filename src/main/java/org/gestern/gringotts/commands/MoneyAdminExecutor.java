package org.gestern.gringotts.commands;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Language;
import org.gestern.gringotts.api.Account;
import org.gestern.gringotts.api.TransactionResult;
import org.gestern.gringotts.currency.Denomination;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin commands for managing ingame aspects.
 */
public class MoneyAdminExecutor extends GringottsAbstractExecutor {
    private static final List<String> commands = Arrays.asList("balance", "add", "remove");

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
        testPermission(sender, cmd, "gringotts.admin");

        if (args.length < 2) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "balance":
            case "bal":
            case "b": {
                if (args.length > 2) {
                    return false;
                }

                String targetAccount = args[1];

                Account target = eco.getAccount(targetAccount);

                if (!target.exists()) {
                    sendInvalidAccountMessage(sender, targetAccount);

                    return false;
                }

                String formattedBalance = eco.currency().format(target.balance());
                String senderMessage = Language.LANG.moneyadmin_b.replace(TAG_BALANCE, formattedBalance).replace(TAG_PLAYER, targetAccount);

                sender.sendMessage(senderMessage);

                return true;
            }
            case "add": {
                if (args.length != 3) {
                    return false;
                }

                String targetAccount = args[1];

                Account target = eco.getAccount(targetAccount);

                if (!target.exists()) {
                    sendInvalidAccountMessage(sender, targetAccount);

                    return false;
                }

                double amount;

                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    return false;
                }

                String formattedAmount = eco.currency().format(amount);
                double beforeBalance = target.balance();
                TransactionResult added = target.add(amount);

                if (added == TransactionResult.INSUFFICIENT_SPACE) {
                    double afterBalance = target.balance();
                    long amountCents = Configuration.CONF.getCurrency().getCentValue(amount);
                    long beforeCents = Configuration.CONF.getCurrency().getCentValue(beforeBalance);
                    long afterCents = Configuration.CONF.getCurrency().getCentValue(afterBalance);
                    long remaining = amountCents - Math.max(0L, afterCents - beforeCents);

                    if (remaining > 0 && dropRemainingAtPlayer(targetAccount, target, remaining)) {
                        added = TransactionResult.SUCCESS;
                    }
                }

                if (added == TransactionResult.SUCCESS) {
                    String senderMessage = Language.LANG.moneyadmin_add_sender.replace(TAG_VALUE, formattedAmount).replace(TAG_PLAYER, targetAccount);

                    sender.sendMessage(senderMessage);

                    String targetMessage = Language.LANG.moneyadmin_add_target.replace(TAG_VALUE, formattedAmount);

                    target.message(targetMessage);
                } else {
                    String errorMessage = Language.LANG.moneyadmin_add_error.replace(TAG_VALUE, targetAccount).replace(TAG_PLAYER, targetAccount);

                    sender.sendMessage(errorMessage);
                }

                return true;
            }
            case "remove":
            case "rm": {
                if (args.length != 3) {
                    return false;
                }

                String targetAccount = args[1];

                Account target = eco.getAccount(targetAccount);

                if (!target.exists()) {
                    sendInvalidAccountMessage(sender, targetAccount);

                    return false;
                }

                double amount;

                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    return false;
                }

                String            formattedAmount = eco.currency().format(amount);
                TransactionResult removed         = target.remove(amount);

                if (removed == TransactionResult.SUCCESS) {
                    String senderMessage = Language.LANG.moneyadmin_rm_sender.replace(TAG_VALUE, formattedAmount).replace(TAG_PLAYER, targetAccount);

                    sender.sendMessage(senderMessage);

                    String targetMessage = Language.LANG.moneyadmin_rm_target.replace(TAG_VALUE, formattedAmount);

                    target.message(targetMessage);
                } else {
                    String errorMessage = Language.LANG.moneyadmin_rm_error.replace(TAG_VALUE, formattedAmount).replace(TAG_PLAYER, targetAccount);

                    sender.sendMessage(errorMessage);
                }

                return true;
            }
        }

        return false;
    }

    private boolean dropRemainingAtPlayer(String targetAccount, Account target, long remainingCents) {
        Player player = Bukkit.getPlayerExact(targetAccount);
        if (player == null) {
            player = Bukkit.getPlayer(targetAccount);
        }
        if (player == null) {
            try {
                player = Bukkit.getPlayer(UUID.fromString(target.id()));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        if (player == null) {
            return false;
        }

        long remaining = remainingCents;
        for (Denomination denomination : Configuration.CONF.getCurrency().getDenominations()) {
            long value = denomination.getValue();
            if (value <= 0 || value > remaining || denomination.getKey().type == null) {
                continue;
            }

            ItemStack stack = new ItemStack(denomination.getKey().type);
            int maxStackSize = stack.getMaxStackSize();
            long itemCount = remaining / value;
            while (itemCount > 0) {
                int stackAmount = (int) Math.min(itemCount, (long) maxStackSize);
                stack.setAmount(stackAmount);
                player.getWorld().dropItem(player.getLocation(), stack.clone());
                itemCount -= stackAmount;
                remaining -= (long) stackAmount * value;
            }
        }

        return remaining == 0;
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
        if (!testPermission(sender, "gringotts.admin")) {
            return Lists.newArrayList();
        }

        String cmd = args[0].toLowerCase();

        switch (args.length) {
            case 1: {
                return commands.stream()
                        .filter(com -> startsWithIgnoreCase(com, args[0]))
                        .collect(Collectors.toList());
            }
            case 2: {
                switch (cmd) {
                    case "b":
                    case "bal":
                    case "balance":
                    case "add":
                    case "remove":
                    case "rm": {
                        return suggestAccounts(args[1]);
                    }
                }
                break;
            }
        }

        return Lists.newArrayList();
    }
}
