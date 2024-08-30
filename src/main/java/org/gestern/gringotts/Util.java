package org.gestern.gringotts;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The type Util.
 */
@SuppressWarnings({ "WeakerAccess" })
public final class Util {
    /**
     * Check whether a block is a sign or wall sign type.
     *
     * @param block block to check
     * @return true if the block is a sign or wall sign
     */
    public static boolean isSignBlock(Block block) {
        if (block == null) {
            return false;
        }

        BlockState blockState = PaperLib.getBlockState(block, true).getState();

        return blockState instanceof Sign;
    }

    /**
     * Gets an OfflinePlayer of the player currently known as the specified player name or UUID
     *
     * @param playerName the player name, or UUID, to look up the unique ID for
     * @return An OfflinePlayer, or null if that player name is not registered in server
     */
    public static @Nullable OfflinePlayer getOfflinePlayer(@NotNull String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);

        if (player != null) {
            return player;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (Objects.requireNonNull(offlinePlayer.getName()).equalsIgnoreCase(playerName)) {
                return offlinePlayer;
            }
        }

        try {
            UUID          targetUuid    = UUID.fromString(playerName);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);

            if (offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer;
            }
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }

    /**
     * Gets block state as T, if blockState
     * is not assignable from T class, is
     * going to return an empty {@link Optional}.
     *
     * @param <T>             the type parameter
     * @param block           the block
     * @param blockStateClass the block state class
     * @return the block state as
     */
    public static <T extends BlockState> Optional<T> getBlockStateAs(Block block, Class<T> blockStateClass) {
        if (block == null || blockStateClass == null) {
            return Optional.empty();
        }

        BlockState blockState = PaperLib.getBlockState(block, false).getState();

        if (blockState == null) {
            return Optional.empty();
        }

        if (blockStateClass.isInstance(blockState)) {
            // noinspection unchecked
            return (Optional<T>) Optional.of(blockState);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Compares whether a version string in standard format (dotted decimals) is
     * greater than another.
     *
     * @param version version string to check
     * @param atLeast minimum expected version
     * @return true if version is greater than greaterThanVersion, false otherwise
     */
    public static boolean versionAtLeast(String version, String atLeast) {
        int[] versionParts = versionParts(version);
        int[] atLeastParts = versionParts(atLeast);

        for (int i = 0; i < versionParts.length && i < atLeastParts.length; i++) {
            // if any more major version part is larger, our version is newer
            if (versionParts[i] > atLeastParts[i]) {
                return true;
            } else if (versionParts[i] < atLeastParts[i]) {
                return false;
            }
        }

        // the at least version has more digits
        return atLeastParts.length <= versionParts.length; // supposedly the versions are equal
    }

    /**
     * Break a version string into parts.
     *
     * @param version version string to handle
     * @return array with dotted decimal strings turned into int values
     */
    public static int[] versionParts(String version) {
        String[] strParts = version.split("\\.");
        int[] parts = new int[strParts.length];

        for (int i = 0; i < strParts.length; i++) {
            // just cut off any non-number part
            String number = strParts[i].replaceAll("(\\d+).*", "$1");
            int part = 0;

            try {
                part = Integer.parseInt(number);
            } catch (NumberFormatException ignored) {
            }

            parts[i] = part;
        }

        return parts;
    }

    /**
     * Find a valid container block for a given sign, if it exists.
     *
     * @param sign sign to check
     * @return container for the sign if available, null otherwise.
     */
    public static Block chestBlock(Sign sign) {
        if (sign == null) {
            return null;
        }

        // is sign attached to a valid vault container?
        Block signBlock = sign.getBlock();
        BlockData blockData = signBlock.getBlockData();

        if (!(blockData instanceof WallSign signData)) {
            return null;
        }

        BlockFace attached = signData.getFacing().getOppositeFace();

        // allow either the block sign is attached to or the block below the sign as
        // chest block. Prefer attached block.
        Block blockAttached = signBlock.getRelative(attached);
        Block blockBelow = signBlock.getRelative(BlockFace.DOWN);

        return isValidContainer(blockAttached.getType()) ? blockAttached
                : isValidContainer(blockBelow.getType()) ? blockBelow : null;
    }

    /**
     * Return whether the given material is a valid container type for Gringotts
     * vaults.
     *
     * @param material material to check
     * @return whether the given material is a valid container type for Gringotts
     *         vaults
     */
    public static boolean isValidContainer(Material material) {
        if (material == null) {
            return false;
        }

        return switch (material) {
            case CHEST, TRAPPED_CHEST, DISPENSER, FURNACE, HOPPER, DROPPER, BARREL -> true;
            default -> false;
        };
    }

    /**
     * Return whether the given inventory type for Gringotts
     *
     * @param inventory inventory to check
     * @return whether the given inventory type is valid for Gringotts
     */
    public static boolean isValidInventory(InventoryType inventory) {
        return switch (inventory) {
            case CHEST, DISPENSER, FURNACE, HOPPER, DROPPER, BARREL -> true;
            default -> false;
        };
    }

    /**
     * Alias for color code translation. Uses '&' as code prefix.
     *
     * @param s String to translate color codes.
     * @return the translated String
     */
    public static String translateColors(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Reform material name string.
     *
     * @param material the material
     * @return the string
     */
    public static String reformMaterialName(Material material) {
        String name = material.name();
        String[] words = name.split("_");

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }

        return String.join(" ", words);
    }
}
