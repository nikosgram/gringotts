package org.gestern.gringotts.currency;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.gestern.gringotts.Configuration;

import java.util.HashMap;
import java.util.Objects;

/**
 * Hashable information to identify a denomination by it's ItemStack.
 */
public class DenominationKey {

    /**
     * Item type of this denomination.
     */
    public final ItemStack type;
    public final Material typeMaterial;
    public final Boolean hasCustomModelData;
    public final Integer typeCustomModelData;

    /**
     * Create a denomination key based on an item stack.
     *
     * @param type    item type of denominations, based on key-value relation creates the Material typeMaterial - Integer typeCustomModelData variables.
     *
     * @see #typeMaterial
     * @see #hasCustomModelData
     * @see #typeCustomModelData
     *
     */
    public DenominationKey(ItemStack type) {
        this.type = new ItemStack(type);
        this.typeMaterial = type.getType();
        this.hasCustomModelData = Objects.requireNonNull(type.getItemMeta()).hasCustomModelData();
        if (this.hasCustomModelData) this.typeCustomModelData = type.getItemMeta().getCustomModelData();
        else {
            this.typeCustomModelData = 0;
            type.getItemMeta().setCustomModelData(typeCustomModelData);
        }
        this.type.setAmount(1);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DenominationKey that = (DenominationKey) o;

        return type.equals(that.type);

    }

    /**
     * Checks if its typeMaterial - typeCustomModelData relation is equal with the key variables.
     *
     * @param o is the object to validate
     * @param onlyRelation defines if to check Material and CustomModelData only. If false equals(Object) is being used.
     *
     * @see #equals(Object) 
     * @see #type
     * @see #typeMaterial
     * @see #hasCustomModelData
     * @see #typeCustomModelData
     *
     * @author iomatix
     * 
     */
    public boolean equals(Object o, Boolean onlyRelation) {
        if (onlyRelation) {
            if (o == null || this.getClass() != o.getClass()) return false;

            DenominationKey that = (DenominationKey) o;
            if (that.typeMaterial.equals(this.typeMaterial) && that.typeCustomModelData.equals(this.typeCustomModelData)) return true;
        }

        return this.equals(o);

    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
