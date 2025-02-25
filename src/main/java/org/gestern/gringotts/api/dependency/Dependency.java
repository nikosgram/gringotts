package org.gestern.gringotts.api.dependency;

import org.bukkit.plugin.Plugin;

/**
 * The interface Dependency.
 */
public interface Dependency {
    /**
     * Gets id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets name.
     *
     * @return the name
     */
    default String getName() {
        return this.getPlugin().getName();
    }

    /**
     * Gets version.
     *
     * @return the version
     */
    default String getVersion() {
        return this.getPlugin()
                .getPluginMeta()
                .getVersion();
    }

    /**
     * Gets plugin.
     *
     * @return the plugin
     */
    Plugin getPlugin();

    /**
     * On load.
     */
    default void onLoad() {}

    /**
     * On enable.
     */
    default void onEnable() {}

    /**
     * On disable.
     */
    default void onDisable() {}

    /**
     * Return whether the plugin handled by this handler is enabled.
     *
     * @return whether the plugin handled by this handler is enabled.
     */
    default boolean isEnabled() {
        return this.getPlugin().isEnabled();
    }
}
