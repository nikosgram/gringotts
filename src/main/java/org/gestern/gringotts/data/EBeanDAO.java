package org.gestern.gringotts.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.GringottsAccount;
import org.gestern.gringotts.Util;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.event.CalculateStartBalanceEvent;

import io.ebean.Database;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import io.ebean.SqlUpdate;

/**
 * The type E bean dao.
 */
public class EBeanDAO implements DAO {
    private static EBeanDAO dao;
    private final Database db = Gringotts.instance.getDatabase();
    private final Logger log = Gringotts.instance.getLogger();


    private List<AccountChest> allChests = new LinkedList<>();

    /**
     * Gets dao.
     *
     * @return the dao
     */
    public static EBeanDAO getDao() {
        if (dao != null) {
            return dao;
        }

        dao = new EBeanDAO();

        return dao;
    }

    /**
     * The classes comprising the DB model, required for the EBean DDL ("data description language").
     *
     * @return the database classes
     */
    public static List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(EBeanAccount.class, EBeanAccountChest.class, EBeanPendingOperation.class);
    }

    @Override
    public synchronized boolean storeAccountChest(AccountChest chest) {
        allChests.add(chest);

        SqlUpdate storeChest = db.sqlUpdate(
            "insert into gringotts_accountchest (world,x,y,z,account,total_value) " +
            "values (:world, :x, :y, :z, (select id from gringotts_account where owner=:owner and " +
            "type=:type), :total_value)"
        );

        Sign mark = chest.sign;
        storeChest.setParameter("world", mark.getWorld().getName());
        storeChest.setParameter("x", mark.getX());
        storeChest.setParameter("y", mark.getY());
        storeChest.setParameter("z", mark.getZ());
        storeChest.setParameter("owner", chest.account.owner.getId());
        storeChest.setParameter("type", chest.account.owner.getType());
        storeChest.setParameter("total_value", chest.getCachedBalance());

        return storeChest.execute() > 0;
    }

    @Override
    public synchronized boolean deleteAccountChest(AccountChest chest) {
        Sign mark = chest.sign;

        return deleteAccountChest(mark.getWorld().getName(), mark.getX(), mark.getY(), mark.getZ());
    }

    @Override
    public synchronized boolean storeAccount(GringottsAccount account) {
        AccountHolder owner = account.owner;

        if (hasAccount(owner)) {
            return false;
        }

        // If removed, it will break backwards compatibility :(
        if (Objects.equals(owner.getType(), "town") || Objects.equals(owner.getType(), "nation")) {
            if (hasAccount(new AccountHolder() {
                @Override
                public String getName() {
                    return owner.getName();
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

        EBeanAccount acc = new EBeanAccount();

        acc.setOwner(owner.getId());
        acc.setType(owner.getType());

        CalculateStartBalanceEvent startBalanceEvent = new CalculateStartBalanceEvent(account.owner);

        Bukkit.getPluginManager().callEvent(startBalanceEvent);

        if (startBalanceEvent.startValue > 0) account.add(startBalanceEvent.startValue);

        db.save(acc);

        return true;
    }

    @Override
    public synchronized boolean hasAccount(AccountHolder accountHolder) {
        return db
            .find(EBeanAccount.class).where()
            .ieq("type", accountHolder.getType())
            .ieq("owner", accountHolder.getId())
            .findOneOrEmpty().isPresent();
    }

    /**
     * Checks the balance of a given account chest and logs any discrepancies between the theoretical balance
     * and the real balance. If a discrepancy is found, the real balance is updated and logged.
     *
     * @param chest              the account chest to check
     * @param theoreticalBalance the expected balance of the account chest
     * @param ownerId            the owner ID of the account chest
     * @param worldName          the name of the world where the chest is located
     * @param x                  the x-coordinate of the chest's location
     * @param y                  the y-coordinate of the chest's location
     * @param z                  the z-coordinate of the chest's location
     */
    private void checkAndLogBalance(AccountChest chest, long theoreticalBalance, String ownerId, String worldName, int x, int y, int z) {
        long realBalance = chest.balance(true);
        if (theoreticalBalance != realBalance) {
            Gringotts.instance.getLogger().severe("Balance differs for account "
                    + ownerId + " at location " + worldName + " " + x + "," + y + "," + z
                    + ". Was supposed to be at " + theoreticalBalance + ", is at " + realBalance
            );

            chest.setCachedBalance(realBalance);
            updateChestBalance(chest, realBalance);
        }
    }

    @Override
    public synchronized Collection<AccountChest> retrieveChests() {
        if (!allChests.isEmpty()) return allChests;

        List<SqlRow> result = db.sqlQuery(
                "SELECT ac.world, ac.x, ac.y, ac.z, a.type, a.owner, ac.total_value FROM gringotts_accountchest ac JOIN gringotts_account a ON ac.account = a.id "
        ).findList();

        List<AccountChest> chests = new LinkedList<>();

        for (SqlRow c : result) {
            String worldName = c.getString("world");
            int x = c.getInteger("x");
            int y = c.getInteger("y");
            int z = c.getInteger("z");

            String type = c.getString("type");
            String ownerId = c.getString("owner");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue; // skip vaults in non-existing worlds
            }

            Block signBlock = world.getBlockAt(x, y, z);
            Optional<Sign> optionalSign = Util.getBlockStateAs(
                    signBlock,
                    Sign.class
            );

            if (optionalSign.isPresent()) {
                AccountHolder owner = Gringotts.instance.getAccountHolderFactory().get(type, ownerId);

                if (owner == null) {
                    log.info(String.format(
                            "AccountHolder %s:%s is not valid. Deleting associated account chest at %s",
                            type,
                            ownerId,
                            signBlock.getLocation()
                    ));

                    deleteAccountChest(
                            signBlock.getWorld().getName(),
                            signBlock.getX(),
                            signBlock.getY(),
                            signBlock.getZ()
                    );
                } else {
                    long theoreticalBalance = c.getLong("total_value");
                    GringottsAccount ownerAccount = new GringottsAccount(owner);
                    AccountChest chest = new AccountChest(optionalSign.get(), ownerAccount, theoreticalBalance);
                    chests.add(chest);

                    checkAndLogBalance(chest, theoreticalBalance, ownerId, worldName, x, y, z);
                }
            } else {
                // remove accountchest from storage if it is not a valid chest
                deleteAccountChest(worldName, x, y, z);
            }
        }

        allChests = chests;

        return chests;
    }

    @Override
    public boolean deleteAccountChest(String world, int x, int y, int z) {
        SqlUpdate deleteChest = db.sqlUpdate(
                "delete from gringotts_accountchest where world = :world and x = :x and y = :y and z = :z"
        );

        deleteChest.setParameter("world", world);
        deleteChest.setParameter("x", x);
        deleteChest.setParameter("y", y);
        deleteChest.setParameter("z", z);

        allChests.removeIf(chest -> {
            Location loc = chest.sign.getLocation();
            return loc.getWorld().getName().equals(world) && loc.getX() == x && loc.getY() == y && loc.getZ() == z;
        });

        return deleteChest.execute() > 0;
    }

    /**
     * Rename account boolean.
     *
     * @param type    the type
     * @param holder  the holder
     * @param newName the new name
     * @return the boolean
     */
    @Override
    public boolean renameAccount(String type, AccountHolder holder, String newName) {
        return renameAccount(type, holder.getId(), newName);
    }

    /**
     * Rename account boolean.
     *
     * @param type    the type
     * @param oldName the old name
     * @param newName the new name
     * @return the boolean
     */
    @Override
    public boolean renameAccount(String type, String oldName, String newName) {
        SqlUpdate renameAccount = db.sqlUpdate(
                "UPDATE gringotts_account SET owner = :newName WHERE owner = :oldName and type = :type"
        );

        renameAccount.setParameter("type", type);
        renameAccount.setParameter("oldName", oldName);
        renameAccount.setParameter("newName", newName);

        return renameAccount.execute() > 0;
    }

    @Override
    public synchronized List<AccountChest> retrieveChests(GringottsAccount account) {
        if (!allChests.isEmpty()) {
            return allChests.stream().filter(ac -> ac.account.owner.equals(account.owner)).collect(Collectors.toList());
        }
        SqlQuery getChests = db.sqlQuery("SELECT ac.world, ac.x, ac.y, ac.z, ac.total_value " +
                "FROM gringotts_accountchest ac JOIN gringotts_account a ON ac.account = a.id " +
                "WHERE a.owner = :owner and a.type = :type");

        getChests.setParameter("owner", account.owner.getId());
        getChests.setParameter("type", account.owner.getType());

        List<AccountChest> chests = new LinkedList<>();
        for (SqlRow result : getChests.findList()) {
            String worldName = result.getString("world");
            int x = result.getInteger("x");
            int y = result.getInteger("y");
            int z = result.getInteger("z");

            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                continue; // skip chest if it is in non-existent world
            }

            Optional<Sign> optionalSign = Util.getBlockStateAs(
                    world.getBlockAt(x, y, z),
                    Sign.class
            );

            if (optionalSign.isPresent()) {
                long theoreticalBalance = result.getLong("total_value");
                AccountChest chest = new AccountChest(optionalSign.get(), account, theoreticalBalance);
                chests.add(chest);

                checkAndLogBalance(chest, theoreticalBalance, account.owner.getId(), worldName, x, y, z);
            } else {
                // remove accountchest from storage if it is not a valid chest
                deleteAccountChest(worldName, x, y, z);
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
        SqlQuery getAccounts = db.sqlQuery("SELECT type, owner FROM gringotts_account");

        List<String> returned = new LinkedList<>();

        for (SqlRow result : getAccounts.findList()) {
            String type = result.getString("type");
            String owner = result.getString("owner");

            if (type != null && owner != null) {
                returned.add(type + ":" + owner);
            }
        }

        return returned;
    }

    /**
     * Gets accounts.
     *
     * @param type the type
     * @return the accounts
     */
    @Override
    public List<String> getAccounts(String type) {
        SqlQuery getAccounts = db.sqlQuery("SELECT owner FROM gringotts_account WHERE type = :type");

        getAccounts.setParameter("type", type);

        List<String> returned = new LinkedList<>();

        for (SqlRow result : getAccounts.findList()) {
            String owner = result.getString("owner");

            if (owner != null) {
                returned.add(type + ":" + owner);
            }
        }

        return returned;
    }

    @Override
    public synchronized boolean storeCents(GringottsAccount account, long amount) {
        SqlUpdate up = db.sqlUpdate("UPDATE gringotts_account SET cents = :cents " +
                "WHERE owner = :owner and type = :type");

        up.setParameter("cents", amount);
        up.setParameter("owner", account.owner.getId());
        up.setParameter("type", account.owner.getType());

        return up.execute() == 1;
    }

    @Override
    public synchronized long retrieveCents(GringottsAccount account) {
        Optional<EBeanAccount> result = db.find(EBeanAccount.class)
            .where()
            .ieq("type", account.owner.getType())
            .ieq("owner", account.owner.getId())
            .findOneOrEmpty();

        return result.map(eBeanAccount -> eBeanAccount.cents).orElse(0L);
    }

    @Override
    public synchronized boolean deleteAccount(GringottsAccount acc) {
        return deleteAccount(acc.owner.getType(), acc.owner.getId());
    }

    @Override
    public synchronized boolean deleteAccount(String type, String account) {
        SqlUpdate renameAccount = db.sqlUpdate(
                "DELETE FROM gringotts_account WHERE owner = :account and type = :type"
        );

        renameAccount.setParameter("type", type);
        renameAccount.setParameter("account", account);

        return renameAccount.execute() > 0;
    }

    @Override
    public synchronized boolean deleteAccountChests(GringottsAccount acc) {
        return deleteAccountChests(acc.owner.getId());
    }

    @Override
    public synchronized boolean deleteAccountChests(String account) {
        allChests.removeIf(chest -> chest.account.owner.getId().equals(account));
        SqlUpdate renameAccount = db.sqlUpdate(
                "DELETE FROM gringotts_accountchest WHERE account = :account"
        );

        renameAccount.setParameter("account", account);

        return renameAccount.execute() > 0;
    }

    @Override
    public synchronized void shutdown() {
        // probably handled by Bukkit?
    }

    @Override
    public boolean updateChestBalance(AccountChest chest, long balance) {
        SqlUpdate updateChest = db.sqlUpdate(
            "UPDATE gringotts_accountchest SET total_value = :total_value "
            + "WHERE world = :world and x = :x and y = :y and z = :z"
        );

        updateChest.setParameter("world", chest.sign.getWorld().getName());
        updateChest.setParameter("x", chest.sign.getX());
        updateChest.setParameter("y", chest.sign.getY());
        updateChest.setParameter("z", chest.sign.getZ());
        updateChest.setParameter("total_value", balance);
        return updateChest.execute() > 0;
    }
}
