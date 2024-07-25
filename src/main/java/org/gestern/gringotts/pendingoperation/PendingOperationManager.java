package org.gestern.gringotts.pendingoperation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.data.EBeanPendingOperation;

public class PendingOperationManager {
    private List<EBeanPendingOperation> pendingOperations;
    private boolean ready = false;


    public void init() {
        this.pendingOperations = Gringotts.instance.getDatabase().find(EBeanPendingOperation.class).findList();
        this.ready = true;
    }

    public void registerOperation(EBeanPendingOperation operation) {
        this.pendingOperations.add(operation);
    }

    public void applyOperationsForChunk(Chunk chunk) {
        List<EBeanPendingOperation> operations = getOperationsForChunk(chunk);
        List<AccountChest> loadedChests = Gringotts.instance.getDao().retrieveChests()
            .stream().filter(chest -> chest.isChestLoaded()).collect(Collectors.toList());

        for (EBeanPendingOperation operation : operations) {
            Optional<AccountChest> accountChest = loadedChests.stream().filter(chest ->
                chest.sign.getX() == operation.getX() &&
                chest.sign.getY() == operation.getY() &&
                chest.sign.getZ() == operation.getZ() &&
                chest.sign.getWorld().getName().equals(operation.getWorld())
            ).findAny();

            if (accountChest.isEmpty()) continue;
            System.out.println("chest found");

            if (operation.getAmount() < 0) {
                accountChest.get().remove(-operation.getAmount());
            } else {
                accountChest.get().add(operation.getAmount());
            }

            pendingOperations.remove(operation);
            Gringotts.instance.getDatabase().delete(operation);
        }

    }

    public void registerNewOperation(EBeanPendingOperation op) {
        Gringotts.instance.getDatabase().save(op);
        pendingOperations.add(op);
    }

    private List<EBeanPendingOperation> getOperationsForChunk(Chunk chunk) {
        return pendingOperations.stream().filter(op -> chunk.getX() == op.getChunkX() && chunk.getZ() == op.getChunkZ())
            .collect(Collectors.toList());
    }

    public boolean isReady() {
        return this.ready;
    }
}
