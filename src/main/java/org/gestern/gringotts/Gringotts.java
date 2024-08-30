package org.gestern.gringotts;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.gestern.gringotts.accountholder.AccountHolderFactory;
import org.gestern.gringotts.accountholder.AccountHolderProvider;
import org.gestern.gringotts.api.Eco;
import org.gestern.gringotts.api.dependency.Dependency;
import org.gestern.gringotts.api.dependency.DependencyProvider;
import org.gestern.gringotts.api.impl.GringottsEco;
import org.gestern.gringotts.api.impl.VaultConnector;
import org.gestern.gringotts.commands.GringottsExecutor;
import org.gestern.gringotts.commands.MoneyAdminExecutor;
import org.gestern.gringotts.commands.MoneyExecutor;
import org.gestern.gringotts.commands.VaultCommand;
import org.gestern.gringotts.currency.Denomination;
import org.gestern.gringotts.data.DAO;
import org.gestern.gringotts.data.EBeanDAO;
import org.gestern.gringotts.dependency.DependencyProviderImpl;
import org.gestern.gringotts.dependency.GenericDependency;
import org.gestern.gringotts.dependency.placeholdersapi.PlaceholderAPIDependency;
import org.gestern.gringotts.event.AccountListener;
import org.gestern.gringotts.event.PlayerVaultListener;
import org.gestern.gringotts.event.VaultCreator;
import org.gestern.gringotts.pendingoperation.PendingOperationListener;
import org.gestern.gringotts.pendingoperation.PendingOperationManager;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.Transaction;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import net.milkbowl.vault.economy.Economy;

/**
 * The type Gringotts.
 */
public class Gringotts extends JavaPlugin {
    // Make Gringotts instance publicly available, no need to be private!
    public static Gringotts instance;

    private static final String MESSAGES_YML = "messages.yml";

    private final AccountHolderFactory accountHolderFactory = new AccountHolderFactory();
    private final DependencyProvider dependencies = new DependencyProviderImpl(this);
    private final Database ebean;
    private final PendingOperationManager pendingOperationManager = new PendingOperationManager();
    private Accounting accounting;
    private DAO dao;
    private Eco eco;

    /**
     * Instantiates a new Gringotts.
     */
    public Gringotts() {
        instance = this;
        getDataFolder().mkdirs();

        DatabaseConfig cfg = new DatabaseConfig();
        Properties properties = new Properties();

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUsername("bukkit");
        dataSourceConfig.setPassword("walrus");
        dataSourceConfig.setUrl(replaceDatabaseString("jdbc:sqlite:{DIR}{NAME}.db"));
        dataSourceConfig.setDriver("org.sqlite.JDBC");
        dataSourceConfig.setIsolationLevel(Transaction.SERIALIZABLE);

        cfg.setDataSourceConfig(dataSourceConfig);
        cfg.setDdlGenerate(true);
        cfg.setDdlRun(true);
        cfg.setRunMigration(true);
        cfg.setClasses(EBeanDAO.getDatabaseClasses());

        cfg.loadFromProperties(properties);
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ebean = DatabaseFactory.create(cfg);
        Thread.currentThread().setContextClassLoader(previousCL);
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

    /**
     * On enable.
     */
    @Override
    public void onEnable() {
        try {
            // just call DAO once to ensure it's loaded before startup is complete
            dao = getDAO();

            new BukkitRunnable() {
                // Run once worlds are loaded
                @Override
                public void run() {
                    dao.retrieveChests();
                    pendingOperationManager.init();
                }
            }.runTask(instance);

            // load and init configuration
            saveDefaultConfig(); // saves default configuration if no config.yml exists yet
            reloadConfig();

            accounting = new Accounting();
            eco        = new GringottsEco();

            if (!(this.dependencies.hasDependency("vault"))) {
                Bukkit.getPluginManager().disablePlugin(this);

                getLogger().warning(
                        "Vault was not found. Other plugins may not be able to access Gringotts accounts."
                );

                return;
            }

            this.dependencies.onEnable();

            registerCommands();
            registerEvents();

            if (this.dependencies.hasDependency("vault")) {
                getServer().getServicesManager().register(
                        Economy.class,
                        new VaultConnector(),
                        this,
                        ServicePriority.Highest
                );

                getLogger().info("Registered Vault interface.");
            }

            registerMetrics();
        } catch (GringottsStorageException | GringottsConfigurationException e) {
            getLogger().severe(e.getMessage());
            this.disable();
        } catch (RuntimeException e) {
            this.disable();
            throw e;
        }

        getLogger().fine("enabled");
    }

    private void disable() {
        Bukkit.getPluginManager().disablePlugin(this);
        getLogger().warning("Gringotts disabled due to startup errors.");
    }

    @Override
    public void onLoad() {
        try {
            Plugin plugin = this.dependencies.hookPlugin(
                    "PlaceholderAPI",
                    "me.clip.placeholderapi.PlaceholderAPIPlugin",
                    "2.11.0"
            );

            if (plugin != null) {
                if (!this.dependencies.registerDependency(new PlaceholderAPIDependency(plugin))) {
                    getLogger().warning("PlaceholderAPI plugin is already assigned into the dependencies.");
                }
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning(
                    "Looks like PlaceholderAPI plugin is not compatible with Gringotts"
            );
        }

        this.registerGenericDependency(
                "vault",
                "Vault",
                "net.milkbowl.vault.Vault",
                "1.7"
        );

        this.dependencies.onLoad();
    }

    /**
     * Register generic dependency.
     *
     * @param id         the id
     * @param name       the name
     * @param classPath  the class path
     * @param minVersion the min version
     */
    private void registerGenericDependency(String id,
                                           String name,
                                           String classPath,
                                           String minVersion) {
        try {
            if (!this.dependencies.registerDependency(new GenericDependency(
                    this.dependencies.hookPlugin(
                            name,
                            classPath,
                            minVersion
                    ),
                    id
            ))) {
                getLogger().warning(name + " plugin is already assigned into the dependencies.");
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning(String.format("Looks like %1$s plugin is not compatible with Gringotts's code.", name));
        } catch (NullPointerException ignored) {}
    }

    /**
     * Gets dependencies.
     *
     * @return the dependencies
     */
    public DependencyProvider getDependencies() {
        return dependencies;
    }

    /**
     * On disable.
     */
    @Override
    public void onDisable() {
        this.dependencies.onDisable();

        // shut down db connection
        try {
            if (dao != null) {
                dao.shutdown();
            }
        } catch (GringottsStorageException e) {
            getLogger().severe(e.toString());
        }

        getLogger().info("disabled");
    }

    private void registerMetrics() {
        // Setup Metrics support.
        Metrics metrics = new Metrics(this, 4998);

        // Tracking the exists denominations.
        metrics.addCustomChart(new AdvancedPie("denominationsChart", () -> {
            Map<String, Integer> returned = new HashMap<>();

            for (Denomination denomination : Configuration.CONF.getCurrency().getDenominations()) {
                String name = denomination.getKey().type.getType().name();

                if (!returned.containsKey(name)) {
                    returned.put(name, 0);
                }

                returned.put(name, returned.get(name) + 1);
            }

            return returned;
        }));

        metrics.addCustomChart(new DrilldownPie("dependencies", () -> {
            Map<String, Map<String, Integer>> returned = new HashMap<>();

            for (Dependency dependency : this.dependencies) {
                if (dependency.isEnabled()) {
                    String name    = dependency.getName();
                    String version = dependency.getVersion();

                    if (name != null && version != null) {
                        returned.put(name, new HashMap<String, Integer>() {{
                            put(version, 1);
                        }});
                    }
                }
            }

            return returned;
        }));
    }

    private void registerCommands() {
        registerCommand("vault", new VaultCommand());
        registerCommand(new String[]{"balance", "money"}, new MoneyExecutor());
        registerCommand("moneyadmin", new MoneyAdminExecutor());
        registerCommand("gringotts", new GringottsExecutor(this));
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean registerCommand(String[] names, TabExecutor executor) {
        boolean returned = true;

        for (String name : names) {
            if (!registerCommand(name, executor)) {
                returned = false;
            }
        }

        return returned;
    }

    private boolean registerCommand(String name, TabExecutor executor) {
        PluginCommand pluginCommand = getCommand(name);

        if (pluginCommand == null) {
            getLogger().warning(String.format(
                    "Looks like the command '%1$s' is not available. Please be sure that Gringotts is the only plugin using it.",
                    name
            ));

            return false;
        }

        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);

        return true;
    }

    private void registerEvents() {
        PluginManager manager = getServer().getPluginManager();

        manager.registerEvents(new AccountListener(), this);
        manager.registerEvents(new PlayerVaultListener(), this);
        manager.registerEvents(new VaultCreator(), this);
        manager.registerEvents(new PendingOperationListener(), this);

        // listeners for other account types are loaded with dependencies
    }

    /**
     * Register an accountholder provider with Gringotts.
     * This is necessary for Gringotts to find and create
     * account holders of any non-player type. Registering
     * a provider for the same type twice will overwrite
     * the previously registered provider.
     *
     * @param type     type id for an account type
     * @param provider provider for the account type
     */
    public void registerAccountHolderProvider(String type, AccountHolderProvider provider) {
        accountHolderFactory.registerAccountHolderProvider(type, provider);
    }

    /**
     * Get the configured player interaction messages.
     *
     * @return the configured player interaction messages
     */
    public FileConfiguration getMessages() {
        String langPath = String.format("i18n/messages_%s.yml", Configuration.CONF.language);

        // try configured language first
        InputStream       langStream = getResource(langPath);
        FileConfiguration conf;

        if (langStream != null) {
            Reader langReader = new InputStreamReader(langStream, StandardCharsets.UTF_8);
            conf = YamlConfiguration.loadConfiguration(langReader);
        } else {
            // use custom/default
            File langFile = new File(getDataFolder(), MESSAGES_YML);
            conf = YamlConfiguration.loadConfiguration(langFile);
        }

        return conf;
    }

    /**
     * Reload config.
     * <p>
     * override to handle custom config logic and language loading
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        Configuration.CONF.readConfig(getConfig());
        Language.LANG.readLanguage(getMessages());
    }

    /**
     * Save default config.
     */
    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        File defaultMessages = new File(getDataFolder(), MESSAGES_YML);

        if (!defaultMessages.exists()) {
            saveResource(MESSAGES_YML, false);
        }
    }

    private DAO getDAO() {
        return EBeanDAO.getDao();
    }

    /**
     * Gets database classes.
     *
     * @return the database classes
     */
    public List<Class<?>> getDatabaseClasses() {
        return EBeanDAO.getDatabaseClasses();
    }

    /**
     * Gets the {@link Database} tied to this plugin.
     * <p>
     * <i>For more information on the use of <a href="http://www.avaje.org/">
     * Avaje Ebeans ORM</a>, see <a href="http://www.avaje.org/ebean/documentation.html">
     * Avaje Ebeans Documentation
     * </a></i>
     * <p>
     * <i>For an example using Ebeans ORM, see <a
     * href="https://github.com/Bukkit/HomeBukkit">Bukkit's Homebukkit Plugin
     * </a></i>
     *
     * @return ebean server instance or null if not enabled all EBean related methods has been removed with Minecraft 1.12 - see <a href="https://www.spigotmc.org/threads/194144/">...</a>
     */
    public Database getDatabase() {
        return ebean;
    }

    private String replaceDatabaseString(String input) {
        input = input.replaceAll(
                "\\{DIR}",
                getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
        input = input.replaceAll(
                "\\{NAME}",
                getName().replaceAll("[^\\w_-]", ""));

        return input;
    }

    /**
     * Gets dao.
     *
     * @return the dao
     */
    public DAO getDao() {
        return dao;
    }

    /**
     * The account holder factory is the place to go if you need an AccountHolder instance for an id.
     *
     * @return the account holder factory
     */
    public AccountHolderFactory getAccountHolderFactory() {
        return accountHolderFactory;
    }

    /**
     * Manages accounts.
     *
     * @return the accounting
     */
    public Accounting getAccounting() {
        return accounting;
    }

    /**
     * Gets eco.
     *
     * @return the eco
     */
    public Eco getEco() {
        return eco;
    }

    public PendingOperationManager getPendingOperationManager() {
        return pendingOperationManager;
    }
}
