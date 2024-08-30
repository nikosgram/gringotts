package org.gestern.gringotts;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.gestern.gringotts.currency.GringottsCurrency;

import java.util.*;
import java.util.logging.Logger;

/**
 * Singleton for global configuration information.
 * Values are initialized when the plugin is enabled.
 *
 * @author jast
 */
public enum Configuration {

    /**
     * Central configuration instance.
     */
    CONF;

    private final Logger log = Gringotts.instance.getLogger();

    /**
     * Regular expression defining what patterns on a sign will create a valid vault.
     * Subpattern 1 denotes the type of the vault.
     */
    public String vaultPattern        = "[^\\[]*\\[(\\w*) ?vault\\]";
    /**
     * Language to be used for messages. Should be an ISO 639-1 (alpha-2) code.
     * If a language is not supported by Gringotts, use user-configured or default (English) messages.
     */
    public String language            = "custom";

    public boolean dropOverflowingItem = false;

    /**
     *
     * Check only CustomModelData for compatibility with different plugins.
     *
     */
    public boolean custommodeldataOnly = false;

    /**
     * Flat tax on every player-to-player transaction. This is a value in currency units.
     */
    public double transactionTaxFlat = 0;
    /**
     * Rate tax on every player-to-player transaction. This is a fraction, e.g. 0.1 means 10% tax.
     */
    public double transactionTaxRate = 0;
    /**
     * Amount of non-physical money to give to new players
     */
    // An alternative to flooding new players' inventories with currency items
    public long   startBalancePlayer = 0;
    /**
     * Use container vaults (chest, dispenser, furnace).
     */
    public boolean useVaultContainer = true;
    /**
     * Use ender chests as player vaults.
     */
    public boolean useVaultEnderChest = true;
    /**
     * Balance command shows vault balance.
     */
    public boolean balanceShowVault = true;
    /**
     * Balance command shows inventory balance.
     */
    public boolean balanceShowInventory = true;
    /**
     * Balance command shows enderchest balance
     */
    public boolean balanceShowEnderchest = true;
    /**
     * if true, the denomination finding process will include shulker boxes
     */
    public boolean includeShulkerBoxes = true;
    /**
     * Currency configuration.
     */
    private GringottsCurrency currency;

    /**
     * Attempt to identify an item by name. Prefers to use built-in Minecraft names,
     * and uses Vault identification if available.
     *
     * @param name name of the item type.
     * @return the identified item, if successful, or null otherwise.
     */
    private static ItemStack itemByName(String name) {
        // matchMaterial also works for item ids
        Material material = Material.matchMaterial(name);

        // TODO check for Vault dependency
        if (material != null) {
            return new ItemStack(material);
        }

        throw new GringottsConfigurationException("Unable to identify denomination item by name or id: " + name);
    }

    /**
     * Derived name for this denomination.
     */
    private static String unitName(ItemStack type) {
        if (type.hasItemMeta()) {
            ItemMeta meta = type.getItemMeta();

            if (meta != null) {
                if (meta.hasDisplayName()) {
                    return meta.getDisplayName();
                } else if (meta.hasLocalizedName()) {
                    return meta.getLocalizedName();
                }
            }
        }

        return Util.reformMaterialName(type.getType());
    }

    /**
     * Set configuration from values in a file configuration.
     *
     * @param savedConfig config to read and set values with
     */
    public void readConfig(FileConfiguration savedConfig) {
        String version = Bukkit.getBukkitVersion();

        if (Util.versionAtLeast(version, "1.3.1")) {
            log.info("Found Bukkit version: " + version + ". All features enabled.");

            CONF.useVaultEnderChest = savedConfig.getBoolean("usevault.enderchest", true);

        } else {
            log.info("Found Bukkit version: " + version + ". Disabling 1.3+ features.");

            CONF.useVaultEnderChest = false;
        }

        // legacy parameter sets digits to 0 (false) or 2 (true)
        int digits = savedConfig.getBoolean("currency.fractional", true) ? 2 : 0;
        // digits param overrides fractional if available
        digits = savedConfig.getInt("currency.digits", digits);

        boolean namedDenominations = savedConfig.getBoolean("currency.named-denominations", false);

        String currencyNameSingular, currencyNamePlural;
        currencyNameSingular = Util.translateColors(savedConfig.getString("currency.name.singular", "Emerald"));
        currencyNamePlural   = Util.translateColors(savedConfig.getString("currency.name.plural", currencyNameSingular + "s"));
        currency             = new GringottsCurrency(currencyNameSingular, currencyNamePlural, digits, namedDenominations);

        // regular currency configuration (multi-denomination)
        ConfigurationSection denomSection = savedConfig.getConfigurationSection("currency.denominations");
        parseCurrency(denomSection, savedConfig);

        CONF.dropOverflowingItem = savedConfig.getBoolean("drop-overflowing-item", false);

        CONF.custommodeldataOnly = savedConfig.getBoolean("custommodeldata-only", false);

        CONF.transactionTaxFlat = savedConfig.getDouble("transactiontax.flat", 0);
        CONF.transactionTaxRate = savedConfig.getDouble("transactiontax.rate", 0);

        CONF.startBalancePlayer  = savedConfig.getLong("startingbalance.player", 0);

        CONF.useVaultContainer   = savedConfig.getBoolean("usevault.container", true);
        CONF.includeShulkerBoxes = savedConfig.getBoolean("usevault.include-shulker-boxes", true);

        CONF.balanceShowInventory  = savedConfig.getBoolean("balance.show-inventory", true);
        CONF.balanceShowVault      = savedConfig.getBoolean("balance.show-vault", true);
        CONF.balanceShowEnderchest = savedConfig.getBoolean("balance.show-enderchest", true);

        CONF.language = savedConfig.getString("language", "custom");

        CONF.vaultPattern        = savedConfig.getString("vault_pattern", "[^\\[]*\\[(\\w*) ?vault\\]");
    }

    /**
     * Parse currency list from configuration, if present.
     * A currency definition consists of a map of denominations to value.
     * A denomination type is defined either as the item id,
     * or a semicolon-separated string of item id; damage value; data value
     *
     * @param denomSection config section containing denomination definition
     * @param savedConfig  the entire config for if the denom section is "null"
     */
    private void parseCurrency(ConfigurationSection denomSection, FileConfiguration savedConfig) {
        // if the denom section is null, it means it doesn't have a dictionary
        // thus we'll read it in the new list format
        if (denomSection == null && savedConfig.isList("currency.denominations")) {
            for (Map<?, ?> denomEntry : savedConfig.getMapList("currency.denominations")) {

                try {
                    MemoryConfiguration denomConf = new MemoryConfiguration();
                    //noinspection unchecked
                    denomConf.addDefaults((Map<String, Object>) denomEntry);

                    String materialName = denomConf.getString("material");

                    ItemStack denomType = itemByName(materialName);

                    // Allow loading a serialized meta block and overriding it with the other fields
                    ItemMeta meta = denomConf.getSerializable("meta", ItemMeta.class);
                    // Default to the item's default meta if the one in the config doesn't exist
                    if (meta == null) {
                        meta = denomType.getItemMeta();
                    }

                    if (denomConf.contains("damage")) {
                        short damage = (short) denomConf.getInt("damage"); // returns 0 when path is unset
                        if (meta != null) {
                            ((Damageable) meta).setDamage(damage);
                            denomType.setItemMeta(meta);
                        }
                    }

                    if (meta == null) {
                        continue;
                    }

                    String name = denomConf.getString("displayname");

                    if (name != null && !name.isEmpty()) {
                        meta.setDisplayName(Util.translateColors(name));
                    }

                    List<String> lore = denomConf.isString("lore") ?
                            // allow users to configure a single lore string
                            Collections.singletonList(denomConf.getString("lore")) :
                            denomConf.getStringList("lore");

                    if (!lore.isEmpty()) {
                        List<String> loreTranslated = new ArrayList<>(lore.size());

                        for (String l : lore) {
                            loreTranslated.add(Util.translateColors(l));
                        }

                        meta.setLore(loreTranslated);
                    }

                    if (denomConf.contains("custom_model_data") && denomConf.isInt("custom_model_data")) {
                        meta.setCustomModelData(denomConf.getInt("custom_model_data"));
                    }

                    denomType.setItemMeta(meta);

                    double value = denomConf.getDouble("value");

                    String unitName = denomConf.contains("unit-name") ?
                            denomConf.getString("unit-name") :
                            unitName(denomType);

                    String unitNamePlural = denomConf.contains("unit-name-plural") ?
                            denomConf.getString("unit-name-plural") :
                            unitName + "s";

                    currency.addDenomination(denomType, value, Util.translateColors(unitName), Util.translateColors(unitNamePlural));

                } catch (GringottsConfigurationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new GringottsConfigurationException("Encountered an error parsing currency. Please check " +
                            "your Gringotts configuration. Error was: " + e.getMessage(), e);
                }
            }
        } else if (denomSection != null) {
            parseLegacyCurrency(denomSection);
        } else {
            throw new GringottsConfigurationException("Denom section is null.");
        }
    }

    /**
     * Parse a multi-denomination currency from configuration.
     * A currency definition consists of a map of denominations to value.
     * A denomination type is defined either as the item id, item name,
     * or a semicolon-separated string of item id; damage value; data value
     *
     * @param denomSection config section containing denomination definition
     */
    private void parseLegacyCurrency(ConfigurationSection denomSection) {
        Set<String> denoms = denomSection.getKeys(false);

        if (denoms.isEmpty()) {
            throw new GringottsConfigurationException(
                    "No denominations configured. Please check your Gringotts configuration.");
        }

        for (String denomStr : denoms) {
            String[] keyParts = denomStr.split(";");
            String[] valueParts = denomSection.getString(denomStr, "").split(";");

            String name = "";

            try {
                // a denomination needs at least a valid item type
                ItemStack denomType = itemByName(keyParts[0]);

                if (keyParts.length >= 2) {
                    short dmg = Short.parseShort(keyParts[1]);
                    ItemMeta meta = denomType.getItemMeta();
                    if (meta != null) {
                        ((Damageable) meta).setDamage(dmg);
                        denomType.setItemMeta(meta);
                    }
                }

                if (valueParts.length >= 2) {
                    name = valueParts[1];
                }

                if (!name.isEmpty()) {
                    ItemMeta meta = denomType.getItemMeta();

                    if (meta == null) {
                        continue;
                    }

                    meta.setDisplayName(name);
                    denomType.setItemMeta(meta);
                }

                double value = Double.parseDouble(valueParts[0]);

                String unitName = unitName(denomType);
                String unitNamePlural = unitName + "s";

                currency.addDenomination(
                        denomType,
                        value,
                        Util.translateColors(unitName),
                        Util.translateColors(unitNamePlural)
                );

            } catch (Exception e) {
                throw new GringottsConfigurationException(
                        "Encountered an error parsing currency. Please check your Gringotts configuration. Error was: "
                                + e.getMessage(),
                        e
                );
            }
        }
    }

    /**
     * Currency configuration.
     */
    public GringottsCurrency getCurrency() {
        return currency;
    }
}
