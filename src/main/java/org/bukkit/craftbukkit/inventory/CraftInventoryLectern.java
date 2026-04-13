package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.Container;
import org.bukkit.block.Lectern;
import org.bukkit.inventory.LecternInventory;

import org.cardboardpowered.bridge.world.ContainerBridge;

public class CraftInventoryLectern extends CraftInventory implements LecternInventory {

    public CraftInventoryLectern(Container inventory) {
        super(inventory);
    }

    @Override
    public org.bukkit.block.Lectern getHolder() {
        if (this.inventory instanceof org.cardboardpowered.bridge.world.ContainerBridge bridge) {
            Object owner = bridge.getOwner();
            if (owner instanceof org.bukkit.block.Lectern lectern) {
                return lectern;
            }
        }
        return null;
    }
}