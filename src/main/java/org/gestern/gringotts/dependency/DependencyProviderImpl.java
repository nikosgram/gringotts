package org.gestern.gringotts.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.Util;
import org.gestern.gringotts.api.dependency.Dependency;
import org.gestern.gringotts.api.dependency.DependencyProvider;

/**
 * The type Dependency provider.
 */
public class DependencyProviderImpl implements DependencyProvider {
    private final Map<String, Dependency> dependencies = new HashMap<>();
    private final Gringotts gringotts;

    /**
     * Instantiates a new Dependency provider.
     *
     * @param gringotts the gringotts
     */
    public DependencyProviderImpl(Gringotts gringotts) {
        this.gringotts = gringotts;
    }

    /**
     * Attempt to hook a plugin dependency.
     *
     * @param name       Name of the plugin.
     * @param classpath  classpath to check for
     * @param minVersion minimum version of the plugin. The plugin will still be hooked if this version is not
     *                   satisfied,
     *                   but a warning will be emitted.
     * @return the plugin object when hooked successfully, or null if not.
     */
    @Override
    public Plugin hookPlugin(String name, String classpath, String minVersion) {
        if (DependencyProvider.packagesExists(classpath)) {
            Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(name);

            if (plugin == null) {
                return null;
            }

            this.gringotts.getLogger().info(String.format(
                    "Plugin %s hooked.", name
            ));

            String version = plugin.getPluginMeta().getVersion();

            if (!Util.versionAtLeast(version, minVersion)) {
                this.gringotts.getLogger().warning(String.format(
                        "Plugin dependency %1$s is version %2$s. Expected at least %3$s -- Errors may occur.",
                        name,
                        version,
                        minVersion
                ));

                return null;
            }

            return plugin;
        } else {
            return null;
        }
    }

    /**
     * Register dependency boolean.
     *
     * @param dependency the dependency
     * @return the boolean
     */
    @Override
    public boolean registerDependency(Dependency dependency) {
        if (this.dependencies.containsKey(dependency.getId())) {
            return false;
        }

        this.dependencies.put(dependency.getId(), dependency);

        return true;
    }

    /**
     * Gets dependency.
     *
     * @param id the id
     * @return the dependency
     */
    @Override
    public Optional<Dependency> getDependency(String id) {
        return Optional.ofNullable(this.dependencies.getOrDefault(id, null));
    }

    @Override
    public boolean hasDependency(String id) {
        return this.dependencies.containsKey(id);
    }

    /**
     * Gets dependencies.
     *
     * @return the dependencies
     */
    @Override
    public Collection<Dependency> getDependencies() {
        return this.dependencies.values();
    }

    /**
     * On load.
     */
    @Override
    public void onLoad() {
        for (Dependency dependency : this) {
            this.gringotts.getLogger().info(
                    "Loading dependency " + dependency.getName()
            );

            dependency.onLoad();
        }
    }

    /**
     * On enable.
     */
    @Override
    public void onEnable() {
        for (Dependency dependency : this) {
            this.gringotts.getLogger().info(
                    "Enabling dependency " + dependency.getName()
            );

            dependency.onEnable();
        }
    }

    /**
     * On disable.
     */
    @Override
    public void onDisable() {
        for (Dependency dependency : this) {
            this.gringotts.getLogger().info(
                    "Disabling dependency " + dependency.getName()
            );

            dependency.onDisable();
        }
    }
}
