package org.gestern.gringotts.data;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.gestern.gringotts.*;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.event.CalculateStartBalanceEvent;

import java.util.*;
import java.util.logging.Logger;

/**
 * The type E bean dao.
 */
public class EBeanDAO implements DAO {
    private static EBeanDAO    dao;
    private final  EbeanServer db  = Gringotts.instance.getDatabase();
    private final  Logger      log = Gringotts.instance.getLogger();

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
        return Arrays.asList(EBeanAccount.class, EBeanAccountChest.class);
    }

    @Override
    public synchronized boolean storeAccountChest(AccountChest chest) {
        SqlUpdate storeChest = db.createSqlUpdate(
                "insert into gringotts_accountchest (world,x,y,z,account) " +
                        "values (:world, :x, :y, :z, (select id from gringotts_account where owner=:owner and " +
                        "type=:type))");

        Sign mark = chest.sign;
        storeChest.setParameter("world", mark.getWorld().getName());
        storeChest.setParameter("x", mark.getX());
        storeChest.setParameter("y", mark.getY());
        storeChest.setParameter("z", mark.getZ());
        storeChest.setParameter("owner", chest.account.owner.getId());
        storeChest.setParameter("type", chest.account.owner.getType());

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
        int accCount = db
                .find(EBeanAccount.class)
                .where()
                .ieq("type", accountHolder.getType()).ieq("owner", accountHolder.getId())
                .findRowCount();

        return accCount == 1;
    }

    @Override
    public synchronized Collection<AccountChest> retrieveChests() {
        List<SqlRow> result = db.createSqlQuery(
                "SELECT ac.world, ac.x, ac.y, ac.z, a.type, a.owner FROM gringotts_accountchest ac JOIN gringotts_account a ON ac.account = a.id "
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
                    GringottsAccount ownerAccount = new GringottsAccount(owner);

                    chests.add(new AccountChest(optionalSign.get(), ownerAccount));
                }
            } else {
                // remove accountchest from storage if it is not a valid chest
                deleteAccountChest(worldName, x, y, z);
            }
        }

        return chests;
    }

    private boolean deleteAccountChest(String world, int x, int y, int z) {
        SqlUpdate deleteChest = db.createSqlUpdate(
                "delete from gringotts_accountchest where world = :world and x = :x and y = :y and z = :z"
        );

        deleteChest.setParameter("world", world);
        deleteChest.setParameter("x", x);
        deleteChest.setParameter("y", y);
        deleteChest.setParameter("z", z);

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
        SqlUpdate renameAccount = db.createSqlUpdate(
                "UPDATE gringotts_account SET owner = :newName WHERE owner = :oldName and type = :type"
        );

        renameAccount.setParameter("type", type);
        renameAccount.setParameter("oldName", oldName);
        renameAccount.setParameter("newName", newName);

        return renameAccount.execute() > 0;
    }

    @Override
    public synchronized List<AccountChest> retrieveChests(GringottsAccount account) {
        // TODO ensure world interaction is done in sync task
        SqlQuery getChests = db.createSqlQuery("SELECT ac.world, ac.x, ac.y, ac.z " +
                "FROM gringotts_accountchest ac JOIN gringotts_account a ON ac.account = a.id " +
                "WHERE a.owner = :owner and a.type = :type");

        getChests.setParameter("owner", account.owner.getId());
        getChests.setParameter("type", account.owner.getType());

        List<AccountChest> chests = new LinkedList<>();
        for (SqlRow result : getChests.findSet()) {
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
                chests.add(new AccountChest(optionalSign.get(), account));
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
        SqlQuery getAccounts = db.createSqlQuery("SELECT type, owner FROM gringotts_account");

        List<String> returned = new LinkedList<>();

        for (SqlRow result : getAccounts.findSet()) {
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
        SqlQuery getAccounts = db.createSqlQuery("SELECT owner FROM gringotts_account WHERE type = :type");

        getAccounts.setParameter("type", type);

        List<String> returned = new LinkedList<>();

        for (SqlRow result : getAccounts.findSet()) {
            String owner = result.getString("owner");

            if (owner != null) {
                returned.add(type + ":" + owner);
            }
        }

        return returned;
    }

    @Override
    public synchronized boolean storeCents(GringottsAccount account, long amount) {
        SqlUpdate up = db.createSqlUpdate("UPDATE gringotts_account SET cents = :cents " +
                "WHERE owner = :owner and type = :type");

        up.setParameter("cents", amount);
        up.setParameter("owner", account.owner.getId());
        up.setParameter("type", account.owner.getType());

        return up.execute() == 1;
    }

    @Override
    public synchronized long retrieveCents(GringottsAccount account) {
        // can this NPE? (probably doesn't)
        return db.find(EBeanAccount.class)
                .where()
                .ieq("type", account.owner.getType())
                .ieq("owner", account.owner.getId())
                .findUnique().cents;
    }

    @Override
    public synchronized boolean deleteAccount(GringottsAccount acc) {
        return deleteAccount(acc.owner.getType(), acc.owner.getId());
    }

    @Override
    public synchronized boolean deleteAccount(String type, String account) {
        SqlUpdate renameAccount = db.createSqlUpdate(
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
        SqlUpdate renameAccount = db.createSqlUpdate(
                "DELETE FROM gringotts_accountchest WHERE account = :account"
        );

        renameAccount.setParameter("account", account);

        return renameAccount.execute() > 0;
    }

    @Override
    public synchronized void shutdown() {
        // probably handled by Bukkit?
    }
}
