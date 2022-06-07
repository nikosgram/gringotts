package org.gestern.gringotts;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.BiFunction;

import static org.gestern.gringotts.Util.translateColors;

/**
 * Deals with all the language Strings.
 * <p>
 * In case Strings are not included in language.yml or there is no language.yml
 * there are default-values included here. If someone complains about a String
 * not translating,
 * check for the yml-nodes in readLanguage below. If the node does not match the
 * language file,
 * the default English message will be shown.
 *
 * @author Daenara (KanaYamamoto Q bukkit.org)
 */
public enum Language {
    LANG;

    // global
    public String noperm;
    public String playerOnly;
    public String balance;
    public String vault_balance;
    public String inv_balance;
    public String invalid_account;
    public String reload;
    // pay command
    public String pay_success_tax;
    public String pay_success_sender;
    public String pay_success_target;
    public String pay_insufficientFunds;
    public String pay_insS_sender;
    public String pay_insS_target;
    public String pay_error;
    // deposit command
    public String deposit_success;
    public String deposit_error;
    // withdraw command
    public String withdraw_success;
    public String withdraw_error;
    // moneyadmin command
    public String moneyadmin_b;
    public String moneyadmin_add_sender;
    public String moneyadmin_add_target;
    public String moneyadmin_add_error;
    public String moneyadmin_rm_sender;
    public String moneyadmin_rm_target;
    public String moneyadmin_rm_error;
    // gringotts vaults
    public String vault_created;
    public String vault_error;
    public String vault_noVaultPerm;
    // towny plugin
    public String plugin_towny_noTownVaultPerm;
    public String plugin_towny_noTownResident;
    public String plugin_towny_noNationVaultPerm;
    public String plugin_towny_notInNation;
    public String plugin_towny_vaultNotInTown;
    public String plugin_towny_tooManyVaults;
    // faction plugin
    public String plugin_faction_noVaultPerm;
    public String plugin_faction_notInFaction;
    // worldguard plugin
    public String plugin_worldguard_noVaultPerm;
    // vault plugin
    public String plugin_vault_insufficientFunds;
    public String plugin_vault_insufficientSpace;
    public String plugin_vault_error;
    public String plugin_vault_notImplemented;

    public void readLanguage(FileConfiguration savedLanguage) {
        BiFunction<String, String, String> translator = (path,
                def) -> translateColors(savedLanguage.getString(path, def));

        // global
        LANG.noperm = translator.apply(
                "noperm",
                "You do not have permission to transfer money.");
        LANG.playerOnly = translator.apply(
                "playeronly",
                "This command can only be run by a player.");
        LANG.balance = translator.apply(
                "balance",
                "Your current total balance: %balance");
        LANG.vault_balance = translator.apply(
                "vault_balance",
                "Vault balance: %balance");
        LANG.inv_balance = translator.apply(
                "inv_balance",
                "Inventory balance: %balance");
        LANG.invalid_account = translator.apply(
                "invalidaccount",
                "Invalid account: %player");
        LANG.reload = translator.apply(
                "reload",
                "Gringotts: Reloaded configuration!");

        // pay command
        LANG.pay_success_sender = translator.apply(
                "pay.success.sender",
                "Sent %value to %player. ");
        LANG.pay_success_tax = translator.apply(
                "pay.success.tax",
                "Received %value from %player.");
        LANG.pay_success_target = translator.apply(
                "pay.success.target",
                "Transaction tax deducted from your account: %value");
        LANG.pay_error = translator.apply(
                "pay.error",
                "Your attempt to send %value to %player failed for unknown reasons.");
        LANG.pay_insufficientFunds = translator.apply(
                "pay.insufficientFunds",
                "Your account has insufficient balance. Current balance: %balance. Required: %value");
        LANG.pay_insS_sender = translator.apply(
                "pay.insufficientSpace.sender",
                "%player has insufficient storage space for %value");
        LANG.pay_insS_target = translator.apply(
                "pay.insufficientSpace.target",
                "%player tried to send %value, but you don't have enough space for that amount.");

        // deposit command
        LANG.deposit_success = translator.apply(
                "deposit.success",
                "Deposited %value to your storage.");
        LANG.deposit_error = translator.apply(
                "deposit.error",
                "Unable to deposit %value to your storage.");

        // withdraw command
        LANG.withdraw_success = translator.apply(
                "withdraw.success",
                "Withdrew %value from your storage.");
        LANG.withdraw_error = translator.apply(
                "withdraw.error",
                "Unable to withdraw %value from your storage.");

        // moneyadmin command
        LANG.moneyadmin_b = translator.apply(
                "moneyadmin.b",
                "Balance of account %player: %balance");
        LANG.moneyadmin_add_sender = translator.apply(
                "moneyadmin.add.sender",
                "Added %value to account %player");
        LANG.moneyadmin_add_target = translator.apply(
                "moneyadmin.add.target",
                "Added to your account: %value");
        LANG.moneyadmin_add_error = translator.apply(
                "moneyadmin.add.error",
                "Could not add %value to account %player");
        LANG.moneyadmin_rm_sender = translator.apply(
                "moneyadmin.rm.sender",
                "Removed %value from account %player");
        LANG.moneyadmin_rm_target = translator.apply(
                "moneyadmin.rm.target",
                "Removed from your account: %value");
        LANG.moneyadmin_rm_error = translator.apply(
                "moneyadmin.rm.error",
                "Could not remove %value from account %player");

        // gringotts vaults
        LANG.vault_created = translator.apply(
                "vault.created",
                "Created vault successfully.");
        LANG.vault_noVaultPerm = translator.apply(
                "vault.noVaultPerm",
                "You do not have permission to create vaults here.");
        LANG.vault_error = translator.apply(
                "vault.error",
                "Failed to create vault.");

        // towny plugin
        LANG.plugin_towny_noTownVaultPerm = translator.apply(
                "plugins.towny.noTownPerm",
                "You do not have permission to create town vaults here.");
        LANG.plugin_towny_noTownResident = translator.apply(
                "plugins.towny.noTownResident",
                "Cannot create town vault: You are not resident of a town.");
        LANG.plugin_towny_noNationVaultPerm = translator.apply(
                "plugins.towny.NoNationVaultPerm",
                "You do not have permission to create nation vaults here.");
        LANG.plugin_towny_notInNation = translator.apply(
                "plugins.towny.notInNation",
                "Cannot create nation vault: You do not belong to a nation.");
        LANG.plugin_towny_vaultNotInTown = translator.apply(
                "plugins.towny.vaultNotInTown",
                "You cannot create vaults outside of towns.");
        LANG.plugin_towny_tooManyVaults = translator.apply(
                "plugins.towny.tooManyVaults",
                "You cannot create more vaults! Max: %max");

        // faction plugin
        LANG.plugin_faction_noVaultPerm = translator.apply(
                "plugins.faction.noFactionVaultPerm",
                "You do not have permission to create a faction vault here.");
        LANG.plugin_faction_notInFaction = translator.apply(
                "plugins.faction.notInFaction",
                "Cannot create faction vault: You are not in a faction.");

        // worldguard plugin
        LANG.plugin_worldguard_noVaultPerm = translator.apply(
                "plugins.worldguard.noFactionVaultPerm",
                "You do not have permission to create a region vault here.");

        // vault plugin
        LANG.plugin_vault_insufficientFunds = translator.apply(
                "plugins.vault.insufficientFunds",
                "Insufficient funds.");
        LANG.plugin_vault_insufficientSpace = translator.apply(
                "plugins.vault.insufficientSpace",
                "Insufficient space.");
        LANG.plugin_vault_error = translator.apply(
                "plugins.vault.unknownError",
                "Unknown failure.");
        LANG.plugin_vault_notImplemented = translator.apply(
                "plugins.vault.notImplemented",
                "Gringotts does not support banks");
    }
}
