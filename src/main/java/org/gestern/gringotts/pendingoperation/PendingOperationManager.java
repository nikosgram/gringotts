package org.gestern.gringotts.pendingoperation;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.data.EBeanPendingOperation;

public class PendingOperationManager {
    private List<EBeanPendingOperation> pendingOperations;


    public void init() {
        pendingOperations = Gringotts.instance.getDatabase().find(EBeanPendingOperation.class).findList();
    }

    public void registerOperation(EBeanPendingOperation operation) {
        pendingOperations.add(operation);
    }

    public void applyOperationsForChunk(Chunk chunk) {
        List<EBeanPendingOperation> operations = getOperationsForChunk(chunk);

    }

    public void registerNewOperation(EBeanPendingOperation op) {
        Gringotts.instance.getDatabase().save(op);
        pendingOperations.add(op);
    }

    private List<EBeanPendingOperation> getOperationsForChunk(Chunk chunk) {
        return pendingOperations.stream().filter(op -> chunk.getX() == op.getChunkX() && chunk.getZ() == op.getChunkZ())
            .collect(Collectors.toList());
    }

}
