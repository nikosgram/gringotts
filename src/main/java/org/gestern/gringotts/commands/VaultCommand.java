package org.gestern.gringotts.commands;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Language;
import org.gestern.gringotts.Util;
import org.gestern.gringotts.api.Account;
import org.gestern.gringotts.api.PlayerAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class VaultCommand extends GringottsAbstractExecutor {
    private static final List<String> commands = Collections.singletonList("list");

    private BaseComponent[] getVaultsComponent(CommandSender sender, Account account) {
        return getVaultsComponent(sender, account, "");
    }

    private BaseComponent[] getVaultsComponent(CommandSender sender, Account account, String titlePrefix) {
        Collection<AccountChest> chests = account.getVaultChests();

        ComponentBuilder builder = new ComponentBuilder().append(titlePrefix + "Vaults")
                .bold(true)
                .reset()
                .append("\n\n");

        Map<String, Collection<BaseComponent[]>> groupedValues = new HashMap<>();

        for (AccountChest chest : chests) {
            Location chestLocation = chest.chestLocation();

            if (chestLocation == null) {
                continue;
            }

            World chestWorld = chestLocation.getWorld();

            if (chestWorld == null) {
                continue;
            }

            String worldName = chestWorld.getName();

            BaseComponent[] balanceComponent = new ComponentBuilder("Balance: ").append(eco.currency()
                            .format(Configuration.CONF.getCurrency().getDisplayValue(chest.balance())))
                    .bold(true)
                    .create();

            BaseComponent[] actionExplaining = new ComponentBuilder("Click to copy the location\nof the vault").create();

            ClickEvent onClick = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.format("%s %d,%d,%d",
                    worldName,
                    chestLocation.getBlockX(),
                    chestLocation.getBlockY(),
                    chestLocation.getBlockZ()));

            if (sender.hasPermission("minecraft.command.tp")) {
                actionExplaining = new ComponentBuilder("Click to instantly teleport\nto the vault").create();

                onClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(
                        "/execute in %s run teleport %d %d %d",
                        chestWorld.getKey(),
                        chestLocation.getBlockX(),
                        chestLocation.getBlockY(),
                        chestLocation.getBlockZ()
                ));
            }

            if (!groupedValues.containsKey(worldName)) {
                groupedValues.put(worldName, new ArrayList<>());
            }

            groupedValues.get(worldName).add(
                    new ComponentBuilder(" ")
                            .reset()
                            .append(
                                    new ComponentBuilder(String.format("%d, %d, %d",
                                            chestLocation.getBlockX(),
                                            chestLocation.getBlockY(),
                                            chestLocation.getBlockZ()))
                                            .event(onClick)
                                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                    new Text(new ComponentBuilder()
                                                            .append(balanceComponent)
                                                            .reset()
                                                            .append("\n")
                                                            .append(actionExplaining)
                                                            .create())))
                                            .underlined(true)
                                            .color(ChatColor.DARK_GREEN)
                                            .create()
                            )
                            .append("\n")
                            .create()
            );
        }

        for (Map.Entry<String, Collection<BaseComponent[]>> entry : groupedValues.entrySet()) {
            builder.reset()
                    .append(entry.getKey())
                    .reset()
                    .append("\n");

            for (BaseComponent[] baseComponents : entry.getValue()) {
                builder.append(baseComponents)
                        .reset();
            }

            builder.reset().append("\n");
        }

        return builder.create();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String s,
                             @NotNull String[] args) {
        if (!sender.hasPermission("gringotts.command.vault")) {
            sender.sendMessage("I'm sorry, but you do not have permission to perform this command. " +
                    "Please contact the server administrators if you believe that this is a mistake.");

            return true;
        }

        if (args.length == 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("gringotts.command.vault.list")) {
                sender.sendMessage("I'm sorry, but you do not have permission to perform this command. " +
                        "Please contact the server administrators if you believe that this is a mistake.");

                return true;
            }

            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Language.LANG.playerOnly);

                    return false;
                }

                Player player = (Player) sender;

                PlayerAccount account = eco.player(player.getUniqueId());

                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

                BookMeta meta = (BookMeta) book.getItemMeta();

                //noinspection ConstantConditions
                meta.setAuthor("Gringotts");
                meta.setTitle("Vaults");

                meta.spigot().addPage(getVaultsComponent(player, account));

                book.setItemMeta(meta);

                player.openBook(book);

                return true;
            } else {
                if (!sender.hasPermission("gringotts.command.vault.list.others")) {
                    return false;
                }

                OfflinePlayer targetPlayer = Util.getOfflinePlayer(args[1]);

                if (targetPlayer == null) {
                    sender.spigot().sendMessage(
                            new ComponentBuilder(
                                    "Player with name `" + args[1] + "` never played in this server before."
                            ).create()
                    );

                    return true;
                }

                Account account = eco.player(targetPlayer.getUniqueId());

                if (!(sender instanceof Player)) {
                    sender.spigot().sendMessage(getVaultsComponent(sender, account, targetPlayer.getName() + "'s "));
                } else {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

                    BookMeta meta = (BookMeta) book.getItemMeta();

                    //noinspection ConstantConditions
                    meta.setAuthor("Gringotts");
                    meta.setTitle(targetPlayer.getName() + "'s Vaults");

                    meta.spigot().addPage(getVaultsComponent(sender, account, targetPlayer.getName() + "'s "));

                    book.setItemMeta(meta);

                    ((Player) sender).openBook(book);
                }

                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String s,
                                      @NotNull String[] args) {
        if (!testPermission(sender, "gringotts.command.vault")) {
            return Lists.newArrayList();
        }

        switch (args.length) {
            case 1: {
                return commands.stream()
                        .filter(com -> startsWithIgnoreCase(com, args[0]))
                        .filter(com -> sender.hasPermission("gringotts.command.vault." + com))
                        .collect(Collectors.toList());
            }
            case 2: {
                if ("list".equals(args[0])) {
                    if (!sender.hasPermission("gringotts.command.vault.list.others")) {
                        return Lists.newArrayList();
                    }

                    return suggestAccounts(args[1]);
                }
                break;
            }
        }

        return Lists.newArrayList();
    }
}
