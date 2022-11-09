package org.gestern.gringotts.data;

import org.bukkit.Bukkit;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.GringottsAccount;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.event.CalculateStartBalanceEvent;

import java.util.*;

public class CacheDAO implements DAO {
    public static  Map<String, AccountChest>     Chests   = new HashMap<>();
    public static  Map<String, GringottsAccount> Accounts = new HashMap<>();
    private static CacheDAO                      dao;

    public static CacheDAO getDao() {
        if (dao != null) {
            return dao;
        }

        dao = new CacheDAO();

        return dao;
    }

    @Override
    public boolean storeAccountChest(AccountChest chest) {
        Chests.put(chest.id, chest);

        return true;
    }

    @Override
    public boolean deleteAccountChest(AccountChest chest) {
        Chests.remove(chest.id);

        return true;
    }

    @Override
    public boolean storeAccount(GringottsAccount account) {
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
            })) {
                renameAccount(owner.getType(), owner.getType() + "-" + owner.getName(), owner.getId());

                return false;
            }
        }

        CalculateStartBalanceEvent startBalanceEvent = new CalculateStartBalanceEvent(account.owner);

        Bukkit.getPluginManager().callEvent(startBalanceEvent);

        account.add(startBalanceEvent.startValue);
        Accounts.put(account.owner.getId(), account);

        return true;
    }

    @Override
    public boolean hasAccount(AccountHolder accountHolder) {
        return Accounts.containsKey(accountHolder.getId());
    }

    @Override
    public boolean renameAccount(String type, AccountHolder holder, String newName) {
        return renameAccount(type, holder.getId(), newName);
    }

    @Override
    public boolean renameAccount(String type, String oldName, String newName) {
        return false;
    }

    @Override
    public Collection<AccountChest> retrieveChests() {
        return Chests.values();
    }

    @Override
    public Collection<AccountChest> retrieveChests(GringottsAccount account) {
        return null;
    }

    @Override
    public Collection<String> getAccounts() {
        return Accounts.keySet();
    }

    @Override
    public Collection<String> getAccounts(String type) {
        return null;
    }

    @Override
    public boolean storeCents(GringottsAccount account, long amount) {
        return false;
    }

    @Override
    public long retrieveCents(GringottsAccount account) {
        return 0;
    }

    @Override
    public boolean deleteAccount(GringottsAccount acc) {
        return false;
    }

    @Override
    public boolean deleteAccount(String type, String account) {
        return false;
    }

    @Override
    public boolean deleteAccountChests(GringottsAccount acc) {
        return false;
    }

    @Override
    public boolean deleteAccountChests(String account) {
        return false;
    }

    @Override
    public void shutdown() {

    }
}
