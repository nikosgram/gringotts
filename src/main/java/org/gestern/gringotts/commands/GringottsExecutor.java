package org.gestern.gringotts.commands;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.Language;
import org.gestern.gringotts.api.dependency.Dependency;
import org.gestern.gringotts.currency.Denomination;
import org.gestern.gringotts.currency.DenominationKey;
import org.gestern.gringotts.currency.GringottsCurrency;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Administrative commands not related to ingame money.
 */
public class GringottsExecutor extends GringottsAbstractExecutor {
    private static final List<String> commands = Arrays.asList("reload", "dependencies", "denominations", "adddenomination");
    private final Gringotts gringotts;

    private static final String TAG_VALUE = "%value";
    private static final String TAG_NAME = "%name";

    /**
     * Instantiates a new Gringotts executor.
     *
     * @param gringotts the gringotts
     */
    public GringottsExecutor(Gringotts gringotts) {
        this.gringotts = gringotts;
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        testPermission(sender, command, "gringotts.admin");

        if (args.length < 1) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "reload": {
                Gringotts.instance.reloadConfig();
                sender.sendMessage(Language.LANG.reload);

                return true;
            }
            case "deps":
            case "dependencies": {
                if (sender instanceof Player) {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

                    BookMeta meta = (BookMeta) book.getItemMeta();

                    //noinspection ConstantConditions
                    meta.setAuthor("Gringotts");
                    meta.setTitle("Gringotts Dependencies");

                    ComponentBuilder builder = new ComponentBuilder()
                            .append("Gringotts Dependencies")
                            .bold(true)
                            .reset()
                            .append("\n\n");

                    for (Dependency dependency : this.gringotts.getDependencies()) {
                        BaseComponent[] identificationComponent = new ComponentBuilder(
                                "Identification: "
                        ).append(
                                dependency.getId()
                        ).bold(true).create();
                        BaseComponent[] versionComponent = new ComponentBuilder(
                                "Version: "
                        ).append(
                                dependency.getVersion()
                        ).bold(true).create();

                        builder.reset().append(
                                new ComponentBuilder(" - ").append(
                                        new ComponentBuilder(
                                                dependency.getName()
                                        ).color(
                                                dependency.isEnabled() ? ChatColor.DARK_GREEN : ChatColor.RED
                                        ).underlined(true).event(
                                                new HoverEvent(
                                                        HoverEvent.Action.SHOW_TEXT,
                                                        new Text(
                                                                new ComponentBuilder()
                                                                        .append(identificationComponent)
                                                                        .append("\n")
                                                                        .reset()
                                                                        .append(versionComponent)
                                                                        .create()
                                                        )
                                                )
                                        ).create()
                                ).append("\n").create()
                        );
                    }

                    meta.spigot().addPage(builder.create());

                    book.setItemMeta(meta);

                    ((Player) sender).openBook(book);
                }
                break;
            }
            case "addden":
            case "adddenomination":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem == null || heldItem.getType() == Material.AIR || heldItem.getAmount() == 0) {
                        player.sendMessage(Language.LANG.hold_item);
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(Language.LANG.missing_value.replace(TAG_NAME, Language.LANG.denomination_value));
                        return true;
                    }
                    int value;
                    try {
                        value = Integer.valueOf(args[1]);
                    }
                    catch (NumberFormatException ex) {
                        player.sendMessage(Language.LANG.missing_value.replace(TAG_VALUE, args[1]).replace(TAG_NAME, Language.LANG.denomination_value));
                        return true;
                    }
                    if (args.length < 3) {
                        player.sendMessage(Language.LANG.missing_value.replace(TAG_NAME, Language.LANG.denomination_name));
                        return true;
                    }
                    String name = args[2];
                    String pluralName = name;
                    if (args.length > 3) {
                        pluralName = args[3];
                    }

                    // Configuration update hack
                    FileConfiguration configuration = Gringotts.instance.getConfig();
                    List list = configuration.getList("currency.denominations");
                    Map<String, Object> newDenomination = new LinkedHashMap<>();
                    newDenomination.put("material", heldItem.getType().name());
                    newDenomination.put("value", value);
                    newDenomination.put("unit-name", name);
                    newDenomination.put("unit-name-plural", pluralName);
                    newDenomination.put("meta", heldItem.getItemMeta());
                    list.add(newDenomination);
                    configuration.set("currency.denominations", list);
                    try {
                        configuration.save(new File(Gringotts.instance.getDataFolder(), "config.yml"));
                        Gringotts.instance.reloadConfig();
                        player.sendMessage(Language.LANG.added_denomination.replace(TAG_NAME, pluralName));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                break;
            case "den":
            case "denominations": {
                if (sender instanceof Player) {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

                    BookMeta meta = (BookMeta) book.getItemMeta();

                    //noinspection ConstantConditions
                    meta.setAuthor("Gringotts");
                    meta.setTitle("Gringotts Denominations");

                    GringottsCurrency currency = Configuration.CONF.getCurrency();

                    meta.spigot().addPage(
                            new ComponentBuilder()
                                    .append("Gringotts Denominations")
                                    .bold(true)
                                    .reset()
                                    .append("\n\n")
                                    .append("Singular Name: ")
                                    .append(currency.getName())
                                    .bold(true)
                                    .reset()
                                    .append("\n\n")
                                    .reset()
                                    .append("Plural Name: ")
                                    .append(currency.getNamePlural())
                                    .bold(true)
                                    .reset()
                                    .append("\n\n")
                                    .reset()
                                    .append("Digits: ")
                                    .append(String.valueOf(currency.getDigits()))
                                    .bold(true)
                                    .reset()
                                    .append("\n\n")
                                    .create()
                    );

                    ComponentBuilder builder = new ComponentBuilder()
                            .append("Denominations: ")
                            .append("\n");

                    for (Denomination denomination : currency.getDenominations()) {
                        BaseComponent[] valueComponent = new ComponentBuilder(
                                "Value: "
                        ).append(
                                String.valueOf(
                                        currency.getDisplayValue(
                                                denomination.getValue()
                                        )
                                )
                        ).bold(true).create();
                        BaseComponent[] materialComponent = new ComponentBuilder(
                                "Material: "
                        ).append(
                                denomination.getKey()
                                        .type
                                        .getType()
                                        .getKey()
                                        .toString()
                        ).bold(true).create();

                        builder.reset().append(
                                new ComponentBuilder(" - ").append(
                                        new ComponentBuilder(
                                                denomination.getUnitName()
                                        ).underlined(true).event(
                                                new HoverEvent(
                                                        HoverEvent.Action.SHOW_TEXT,
                                                        new Text(
                                                                new ComponentBuilder()
                                                                        .append(valueComponent)
                                                                        .append("\n")
                                                                        .reset()
                                                                        .append(materialComponent)
                                                                        .create()
                                                        )
                                                )
                                        ).create()
                                ).append("\n").create()
                        );
                    }

                    meta.spigot().addPage(builder.create());

                    book.setItemMeta(meta);

                    ((Player) sender).openBook(book);
                }
                break;
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
        if (!testPermission(sender, "gringotts.admin")) {
            return Lists.newArrayList();
        }

        if (args.length == 1) {
            return commands.stream()
                    .filter(com -> startsWithIgnoreCase(com, args[0]))
                    .collect(Collectors.toList());
        }

        return Lists.newArrayList();
    }
}
