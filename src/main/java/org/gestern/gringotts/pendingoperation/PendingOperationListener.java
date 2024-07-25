package org.gestern.gringotts.pendingoperation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.gestern.gringotts.Gringotts;

public class PendingOperationListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Gringotts.instance.getPendingOperationManager().isReady()) return;
        Gringotts.instance.getPendingOperationManager().applyOperationsForChunk(event.getChunk());
    }
}
