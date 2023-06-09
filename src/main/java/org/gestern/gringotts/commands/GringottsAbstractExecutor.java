package org.gestern.gringotts.commands;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolderProvider;
import org.gestern.gringotts.api.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GringottsAbstractExecutor implements TabExecutor {
    static final String TAG_BALANCE = "%balance";

    static final String TAG_PLAYER = "%player";

    static final String TAG_VALUE = "%value";

    final Eco eco = Gringotts.instance.getEco();

    static void sendInvalidAccountMessage(CommandSender sender, String accountName) {
        sender.sendMessage(Language.LANG.invalid_account.replace(TAG_PLAYER, accountName));
    }

    /**
     * Test permission.
     *
     * @param sender     the sender
     * @param command    the command
     * @param permission the permission
     */
    public static void testPermission(CommandSender sender,
                                      Command command,
                                      String permission) {
        if (!testPermission(sender, permission)) {
            if (command.getPermissionMessage() == null) {
                throw new CommandException("I'm sorry, but you do not have permission to perform this command. " +
                        "Please contact the server administrators if you believe that this is a mistake.");
            } else if (command.getPermissionMessage().length() != 0) {
                throw new CommandException(command.getPermissionMessage().replace("<permission>", permission));
            }
        }
    }

    /**
     * Test permission boolean.
     *
     * @param sender     the sender
     * @param permission the permission
     * @return the boolean
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean testPermission(CommandSender sender, String permission) {
        if ((permission == null) || (permission.length() == 0)) {
            return true;
        }

        return Arrays.stream(permission.split(";")).anyMatch(sender::hasPermission);
    }

    boolean pay(Player player, double value, String recipientName) {
        if (!Permissions.TRANSFER.isAllowed(player)) {
            player.sendMessage(Language.LANG.noperm);

            return true;
        }

        OfflinePlayer recipientPlayer = Util.getOfflinePlayer(recipientName);

        if (recipientPlayer == null) {
            player.spigot().sendMessage(
                    new ComponentBuilder(
                            "Player with name `" + recipientName + "` never played in this server before."
                    ).create()
            );

            return true;
        }

        PlayerAccount from = eco.player(player.getUniqueId());
        Account       to   = eco.account(recipientName);

        TaxedTransaction  transaction = from.send(value).withTaxes();
        TransactionResult result      = transaction.to(eco.player(recipientPlayer.getUniqueId()));

        double tax        = transaction.getTax();
        double valueAdded = value + tax;

        String formattedBalance      = eco.currency().format(from.balance());
        String formattedValue        = eco.currency().format(value);
        String formattedValuePlusTax = eco.currency().format(valueAdded);
        String formattedTax          = eco.currency().format(tax);

        switch (result) {
            case SUCCESS:
                String succTaxMessage = Language.LANG.pay_success_tax.replace(TAG_VALUE, formattedTax);
                String succSentMessage = Language.LANG.pay_success_sender.replace(TAG_VALUE, formattedValue).replace(TAG_PLAYER, recipientName);

                from.message(succSentMessage + (tax > 0 ? succTaxMessage : ""));

                String succReceivedMessage = Language.LANG.pay_success_target.replace(TAG_VALUE, formattedValue).replace(TAG_PLAYER, player.getName());

                to.message(succReceivedMessage);

                return true;
            case INSUFFICIENT_FUNDS:
                String insFMessage = Language.LANG.pay_insufficientFunds.replace(TAG_BALANCE, formattedBalance).replace(TAG_VALUE, formattedValuePlusTax);

                from.message(insFMessage);

                return true;
            case INSUFFICIENT_SPACE:
                String insSSentMessage = Language.LANG.pay_insS_sender.replace(TAG_PLAYER, recipientName).replace(TAG_VALUE, formattedValue);

                from.message(insSSentMessage);

                String insSReceiveMessage = Language.LANG.pay_insS_target.replace(TAG_PLAYER, from.id()).replace(TAG_VALUE, formattedValue);

                to.message(insSReceiveMessage);

                return true;
            default:
                String error = Language.LANG.pay_error.replace(TAG_VALUE, formattedValue).replace(TAG_PLAYER, recipientName);

                from.message(error);

                return true;
        }
    }

    void deposit(Player player, double value) {
        if (Permissions.COMMAND_DEPOSIT.isAllowed(player)) {
            TransactionResult result         = eco.player(player.getUniqueId()).deposit(value);
            String            formattedValue = eco.currency().format(value);

            if (result == TransactionResult.SUCCESS) {
                String success = Language.LANG.deposit_success.replace(TAG_VALUE, formattedValue);

                player.sendMessage(success);
            } else {
                String error = Language.LANG.deposit_error.replace(TAG_VALUE, formattedValue);

                player.sendMessage(error);
            }
        }
    }

    void withdraw(Player player, double value) {
        if (Permissions.COMMAND_WITHDRAW.isAllowed(player)) {
            TransactionResult result         = eco.player(player.getUniqueId()).withdraw(value);
            String            formattedValue = eco.currency().format(value);

            if (result == TransactionResult.SUCCESS) {
                String success = Language.LANG.withdraw_success.replace(TAG_VALUE, formattedValue);

                player.sendMessage(success);
            } else {
                String error = Language.LANG.withdraw_error.replace(TAG_VALUE, formattedValue);

                player.sendMessage(error);
            }
        }
    }

    void sendBalanceMessage(Account account) {
        account.message(Language.LANG.balance.replace(TAG_BALANCE, eco.currency().format(account.balance())));

        if (Configuration.CONF.balanceShowVault) {
            account.message(Language.LANG.vault_balance.replace(TAG_BALANCE, eco.currency().format(account.vaultBalance())));
        }

        if (Configuration.CONF.balanceShowInventory) {
            account.message(Language.LANG.inv_balance.replace(TAG_BALANCE, eco.currency().format(account.invBalance())));
        }

        if (Configuration.CONF.balanceShowEnderchest && account.hasPermission(Permissions.USE_VAULT_ENDERCHEST.node)) {
            account.message(Language.LANG.end_balance.replace(TAG_BALANCE, eco.currency().format(account.endBalance())));
        }
    }

    public List<String> suggestAccounts(String arg) {
        String[] steps = (arg + " ").split(":");

        if (steps.length == 1) {
            return Stream.of(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> startsWithIgnoreCase(name, arg))
                    .collect(Collectors.toList());
        }

        try {
            String type = steps[0].toUpperCase();

            Optional<AccountHolderProvider> providerOptional = Gringotts.instance.getAccountHolderFactory().getProvider(type);

            if (providerOptional.isPresent()) {
                return providerOptional.get().getAccountNames().stream()
                        .filter(Objects::nonNull)
                        .map(s -> type + ":" + s)
                        .filter(name -> startsWithIgnoreCase(name, arg))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }

        return Lists.newArrayList();
    }

    public boolean startsWithIgnoreCase(String source, String target) {
        return source.toLowerCase().startsWith(target.toLowerCase());
    }
}
