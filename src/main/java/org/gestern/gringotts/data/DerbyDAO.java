package org.gestern.gringotts.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.event.CalculateStartBalanceEvent;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The Data Access Object provides access to the datastore.
 * This implementation uses the Apache Derby embedded DB.
 *
 * @author jast
 */
public class DerbyDAO implements DAO {

    private static final String            DB_NAME = "GringottsDB";
    /**
     * Singleton DAO instance.
     */
    private static       DerbyDAO          dao;
    private final        Logger            log     = Gringotts.instance.getLogger();
    private final        Driver            driver;
    /**
     * Full connection string for database, without connect options.
     */
    private final        String            dbString;
    private              Connection        connection;
    private              PreparedStatement storeAccountChest, deleteAccountChest, storeAccount, retrieveAccount, retrieveChests, retrieveChestsForAccount, retrieveCents, renameAccount, deleteAccount, deleteAccountChests, storeCents;

    private DerbyDAO() {

        String dbPath = Gringotts.instance.getDataFolder().getAbsolutePath();
        dbString = "jdbc:derby:" + dbPath + "/" + DB_NAME;
        String connectString = dbString + ";create=true";

        try {
            driver     = DriverManager.getDriver(connectString);
            connection = driver.connect(connectString, null);

            checkConnection();
            setupDB(connection);
            prepareStatements();

            log.fine("DAO setup successfully.");
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to initialize database connection.", e);
        }

    }

    /**
     * Get a DAO instance.
     *
     * @return the DAO instance
     */
    public synchronized static DerbyDAO getDao() {

        if (dao != null) {
            return dao;
        }

        // load derby embedded driver
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ignored) {
            return null;
        }

        dao = new DerbyDAO();

        return dao;
    }

    /**
     * Configure DB for use with gringotts, if it isn't already.
     *
     * @param connection Connection to the db
     */
    private void setupDB(Connection connection) throws SQLException {

        // create tables only if they don't already exist. use metadata to determine this.
        DatabaseMetaData dbmd = connection.getMetaData();

        ResultSet rs1 = dbmd.getTables(null, null, "ACCOUNT", null);
        if (!rs1.next()) {
            String createAccount =
                    "create table account (" +
                            "id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                            "type varchar(64), owner varchar(64), cents int not null, " +
                            "primary key (id), constraint unique_type_owner unique(type, owner))";

            Statement stmt = connection.createStatement();
            int updated = stmt.executeUpdate(createAccount);

            if (updated > 0) {
                log.info("created table ACCOUNT");
            }

            stmt.close();
        }

        ResultSet rs2 = dbmd.getTables(null, null, "ACCOUNTCHEST", null);
        if (!rs2.next()) {
            String createAccountChest =
                    "create table accountchest (" +
                            "id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                            "world varchar(64), x integer, y integer, z integer, account integer not null, " +
                            "primary key(id), constraint unique_location unique(world,x,y,z), constraint fk_account " +
                            "foreign key(account) references account(id))";

            Statement stmt = connection.createStatement();
            int updated = stmt.executeUpdate(createAccountChest);

            if (updated > 0) {
                log.info("created table ACCOUNTCHEST");
            }

            stmt.close();
        }
    }

    /**
     * Prepare sql statements for use in DAO.
     *
     */
    private void prepareStatements() throws SQLException {

        storeAccountChest = connection.prepareStatement(
                "insert into accountchest (world,x,y,z,account) values (?, ?, ?, ?, (select id from account where " +
                        "owner=? and type=?))");
        deleteAccountChest = connection.prepareStatement(
                "delete from accountchest where world = ? and x = ? and y = ? and z = ?");
        storeAccount = connection.prepareStatement(
                "insert into account (type, owner, cents) values (?,?,?)");
        retrieveAccount = connection.prepareStatement(
                "select * from account where owner = ? and type = ?");
        renameAccount = connection.prepareStatement(
                "UPDATE account set owner = ? where owner = ? and type = ?"
        );
        deleteAccount = connection.prepareStatement(
                "DELETE FROM account WHERE owner = ? AND type = ?"
        );
        deleteAccountChests = connection.prepareStatement(
                "DELETE FROM accountchest WHERE account = ?"
        );
        retrieveChests = connection.prepareStatement(
                "SELECT ac.world, ac.x, ac.y, ac.z, a.type, a.owner " +
                        "FROM accountchest ac JOIN account a ON ac.account = a.id ");
        retrieveChestsForAccount = connection.prepareStatement(
                "SELECT ac.world, ac.x, ac.y, ac.z " +
                        "FROM accountchest ac JOIN account a ON ac.account = a.id " +
                        "WHERE a.owner = ? and a.type = ?");
        retrieveCents = connection.prepareStatement(
                "SELECT cents FROM account WHERE owner = ? and type = ?");
        storeCents = connection.prepareStatement(
                "UPDATE account SET cents = ? WHERE owner = ? and type = ?");
    }

    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = driver.connect(dbString, null);

            prepareStatements();

            log.warning("Database connection lost. Reinitialized DB.");
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#storeAccountChest(org.gestern.gringotts.AccountChest)
     */
    @Override
    public synchronized boolean storeAccountChest(AccountChest chest) {
        GringottsAccount account = chest.getAccount();
        Location loc = chest.sign.getLocation();

        log.info("storing account chest: " + chest + " for account: " + account);

        try {
            checkConnection();

            storeAccountChest.setString(1, loc.getWorld().getName());
            storeAccountChest.setInt(2, loc.getBlockX());
            storeAccountChest.setInt(3, loc.getBlockY());
            storeAccountChest.setInt(4, loc.getBlockZ());
            storeAccountChest.setString(5, account.owner.getId());
            storeAccountChest.setString(6, account.owner.getType());

            int updated = storeAccountChest.executeUpdate();

            return updated > 0;
        } catch (SQLException e) {
            // unique constraint failed: chest already exists
            if (e.getErrorCode() == 23505) {
                log.warning("Unable to store account chest: " + e.getMessage());
                return false;
            }

            // other more serious error probably
            throw new GringottsStorageException("Failed to store account chest: " + chest, e);
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#destroyAccountChest(org.gestern.gringotts.AccountChest)
     */
    @Override
    public synchronized boolean deleteAccountChest(AccountChest chest) {
        Location loc = chest.sign.getLocation();

        try {
            checkConnection();

            return deleteAccountChest(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to delete account chest: " + chest, e);
        }
    }

    public synchronized boolean renameAccount(String type,
                                              AccountHolder holder,
                                              String newName) {
        return renameAccount(type, holder.getId(), newName);
    }

    public synchronized boolean renameAccount(String type,
                                              String oldName,
                                              String newName) {
        try {
            renameAccount.setString(1, newName);
            renameAccount.setString(2, type);
            renameAccount.setString(3, oldName);

            int updated = renameAccount.executeUpdate();

            return updated > 0;
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to rename account: " + oldName, e);
        }
    }

    private boolean deleteAccountChest(String world, int x, int y, int z) throws SQLException {
        deleteAccountChest.setString(1, world);
        deleteAccountChest.setInt(2, x);
        deleteAccountChest.setInt(3, y);
        deleteAccountChest.setInt(4, z);

        int updated = deleteAccountChest.executeUpdate();

        return updated > 0;
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#storeAccount(org.gestern.gringotts.GringottsAccount)
     */
    @Override
    public synchronized boolean storeAccount(GringottsAccount account) {
        AccountHolder owner = account.owner;

        // don't store/overwrite if it's already there
        if (hasAccount(owner)) {
            return false;
        }

        if (Objects.equals(owner.getType(), "town") || Objects.equals(owner.getType(), "nation")) {
            if (hasAccount(new AccountHolder() {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public void sendMessage(String message) {

                }

                @Override
                public String getType() {
                    return owner.getType();
                }

                @Override
                public String getId() {
                    return owner.getType() + "-" + owner.getName();
                }

                @Override
                public boolean hasPermission(String permission) {
                    return false;
                }
            })) {
                renameAccount(
                        owner.getType(),
                        owner.getType() + "-" + owner.getName(),
                        owner.getId()
                );

                return false;
            }
        }

        try {
            checkConnection();

            storeAccount.setString(1, owner.getType());
            storeAccount.setString(2, owner.getId());

            CalculateStartBalanceEvent startBalanceEvent = new CalculateStartBalanceEvent(account.owner);

            Bukkit.getPluginManager().callEvent(startBalanceEvent);

            storeAccount.setLong(3, startBalanceEvent.startValue);

            int updated = storeAccount.executeUpdate();

            return updated > 0;
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to store account: " + account, e);
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#getAccount(org.gestern.gringotts.accountholder.AccountHolder)
     */
    @Override
    public synchronized boolean hasAccount(AccountHolder accountHolder) {
        ResultSet result = null;

        try {
            checkConnection();

            retrieveAccount.setString(1, accountHolder.getId());
            retrieveAccount.setString(2, accountHolder.getType());

            result = retrieveAccount.executeQuery();

            return result.next();
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get account for owner: " + accountHolder, e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Migration method: Get all account raw data.
     *
     * @return list of {{DerbyAccount}} describing the accounts
     */
    public synchronized List<DerbyAccount> getAccountsRaw() {

        List<DerbyAccount> accounts = new LinkedList<>();
        Statement stmt = null;
        ResultSet result = null;
        try {
            checkConnection();
            stmt = connection.createStatement();
            result = stmt.executeQuery("select * from account");

            while (result.next()) {
                int id = result.getInt("id");
                String type = result.getString("type");
                String owner = result.getString("owner");
                long cents = result.getLong("cents");
                accounts.add(new DerbyAccount(id, type, owner, cents));
            }

        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get set of accounts", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ignored) {
            }
        }

        return accounts;
    }

    /**
     * Migration method: Get all accountchest raw data.
     *
     * @return ...
     */
    public synchronized List<DerbyAccountChest> getChestsRaw() {
        List<DerbyAccountChest> chests = new LinkedList<>();
        Statement stmt = null;
        ResultSet result = null;
        try {
            checkConnection();
            stmt = connection.createStatement();
            result = stmt.executeQuery("select * from accountchest");

            while (result.next()) {
                int id = result.getInt("id");
                String world = result.getString("world");
                int x = result.getInt("x");
                int y = result.getInt("y");
                int z = result.getInt("z");
                int account = result.getInt("account");

                chests.add(new DerbyAccountChest(id, world, x, y, z, account));
            }

        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get set of accounts", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ignored) {
            }
        }

        return chests;

    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#getChests()
     */
    @Override
    public synchronized List<AccountChest> retrieveChests() {
        List<AccountChest> chests = new LinkedList<>();
        ResultSet result = null;
        try {
            checkConnection();

            result = retrieveChests.executeQuery();

            while (result.next()) {
                String worldName = result.getString("world");
                int x = result.getInt("x");
                int y = result.getInt("y");
                int z = result.getInt("z");

                String type = result.getString("type");
                String ownerId = result.getString("owner");

                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    Gringotts.instance.getLogger().warning("Vault " + type + ":" + ownerId + " located on a non-existent world. Skipping.");

                    continue;
                }

                Block signBlock = world.getBlockAt(x, y, z);
                Optional<Sign> optionalSign = Util.getBlockStateAs(
                        signBlock,
                        Sign.class
                );

                if (optionalSign.isPresent()) {
                    AccountHolder owner = Gringotts.instance.getAccountHolderFactory().get(type, ownerId);

                    if (owner == null) {
                        // FIXME this logic really doesn't belong in DAO, I think?
                        log.info("AccountHolder " + type + ":" + ownerId + " is not valid. " +
                                "Deleting associated account chest at " + signBlock.getLocation());

                        deleteAccountChest(
                                signBlock.getWorld().getName(),
                                signBlock.getX(),
                                signBlock.getY(),
                                signBlock.getZ());
                    } else {
                        GringottsAccount ownerAccount = new GringottsAccount(owner);

                        chests.add(new AccountChest(optionalSign.get(), ownerAccount));
                    }
                } else {
                    // remove accountchest from storage if it is not a valid chest
                    deleteAccountChest(
                            signBlock.getWorld().getName(),
                            signBlock.getX(),
                            signBlock.getY(),
                            signBlock.getZ());
                }
            }
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get list of all chests", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
        }

        return chests;
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#getChests(org.gestern.gringotts.GringottsAccount)
     */
    @Override
    public synchronized List<AccountChest> retrieveChests(GringottsAccount account) {
        AccountHolder owner = account.owner;
        List<AccountChest> chests = new LinkedList<>();
        ResultSet result = null;

        try {
            checkConnection();

            retrieveChestsForAccount.setString(1, owner.getId());
            retrieveChestsForAccount.setString(2, owner.getType());
            result = retrieveChestsForAccount.executeQuery();

            while (result.next()) {
                String worldName = result.getString("world");
                int x = result.getInt("x");
                int y = result.getInt("y");
                int z = result.getInt("z");

                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    deleteAccountChest(worldName, x, y, x); // FIXME: Isn't actually removing the non-existent vaults..

                    Gringotts.instance.getLogger().severe(String.format("Vault of %s located on a non-existent world. Deleting Vault on world %s", owner.getName(), worldName));

                    continue;
                }

                Block signBlock = world.getBlockAt(x, y, z);
                Optional<Sign> optionalSign = Util.getBlockStateAs(
                        signBlock,
                        Sign.class
                );

                if (optionalSign.isPresent()) {
                    chests.add(new AccountChest(optionalSign.get(), account));
                } else {
                    // remove accountchest from storage if it is not a valid chest
                    deleteAccountChest(
                            signBlock.getWorld().toString(),
                            signBlock.getX(),
                            signBlock.getY(),
                            signBlock.getZ());
                }
            }
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get list of all chests", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
        }

        return chests;
    }

    /**
     * Gets accounts.
     *
     * @return the accounts
     */
    @Override
    public List<String> getAccounts() {
        return null;
    }

    /**
     * Gets accounts.
     *
     * @param type the type
     * @return the accounts
     */
    @Override
    public List<String> getAccounts(String type) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#storeCents(org.gestern.gringotts.GringottsAccount, long)
     */
    @Override
    public synchronized boolean storeCents(GringottsAccount account, long amount) {
        try {
            checkConnection();

            storeCents.setLong(1, amount);
            storeCents.setString(2, account.owner.getId());
            storeCents.setString(3, account.owner.getType());

            int updated = storeCents.executeUpdate();

            return updated > 0;
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get cents for account: " + account, e);
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#getCents(org.gestern.gringotts.GringottsAccount)
     */
    @Override
    public synchronized long retrieveCents(GringottsAccount account) {
        ResultSet result = null;

        try {
            checkConnection();

            retrieveCents.setString(1, account.owner.getId());
            retrieveCents.setString(2, account.owner.getType());

            result = retrieveCents.executeQuery();

            if (result.next()) {
                return result.getLong("cents");
            }

            return 0;

        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to get stored cents for account: " + account, e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        try {
            log.info("shutting down database connection");
            // disconnect from derby completely
            String disconnectString = "jdbc:derby:;shutdown=true";
            DriverManager.getConnection(disconnectString);
        } catch (SQLException e) {
            // yes, derby actually throws an exception as a shutdown message ...
            log.info("Derby shutdown: " + e.getSQLState() + ": " + e.getMessage());
            System.gc();
        }
    }

    /* (non-Javadoc)
     * @see org.gestern.gringotts.data.DAO#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        shutdown();
    }

    @Override
    public synchronized boolean deleteAccount(GringottsAccount acc) {
        return deleteAccount(acc.owner.getType(), acc.owner.getId());
    }

    /**
     * Delete account boolean.
     *
     * @param type    the type
     * @param account the account
     * @return the boolean
     */
    @Override
    public boolean deleteAccount(String type, String account) {
        try {
            deleteAccount.setString(1, account);
            deleteAccount.setString(2, type);

            return deleteAccount.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to delete account: " + account, e);
        }
    }

    /**
     * Delete account chests boolean.
     *
     * @param acc the acc
     * @return the boolean
     */
    @Override
    public boolean deleteAccountChests(GringottsAccount acc) {
        return deleteAccountChests(acc.owner.getId());
    }

    /**
     * Delete account chests boolean.
     *
     * @param account the account
     * @return the boolean
     */
    @Override
    public boolean deleteAccountChests(String account) {
        try {
            deleteAccountChests.setString(1, account);

            return deleteAccountChests.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new GringottsStorageException("Failed to delete chests from account: " + account, e);
        }
    }

    /**
     * Utility class to support migration of Derby database.
     */
    public static class DerbyAccount {
        public final int id;
        public final String type;
        public final String owner;
        public final long cents;

        public DerbyAccount(int id, String type, String owner, long cents) {
            this.id = id;
            this.type = type;
            this.owner = owner;
            this.cents = cents;
        }
    }

    public static class DerbyAccountChest {
        public final int id;
        public final String world;
        public final int x, y, z;
        public final int account;

        public DerbyAccountChest(int id, String world, int x, int y, int z, int account) {
            this.id = id;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.account = account;
        }
    }
}
