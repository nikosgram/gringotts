package org.gestern.gringotts.data;


import io.ebean.annotation.NotNull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gringotts_pending_operation")
public class EBeanPendingOperation {
    @Id
    int id;
    @NotNull
    String world;
    @NotNull
    int x;
    @NotNull
    int y;
    @NotNull
    int z;
    @NotNull
    long amount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "EBeanPendingOperation(" + amount + "," + world + ": " + x + "," + y + "," + z + ")";
    }
}