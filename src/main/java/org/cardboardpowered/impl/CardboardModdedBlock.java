package org.cardboardpowered.impl;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class CardboardModdedBlock implements CardboardModdedMaterial {

    private Block block;
    private String id;

    public CardboardModdedBlock(String id) {
        this.id = id;
        this.block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(id));
    }

    public CardboardModdedBlock(Block block) {
        this.block = block;
    }

    @Override
    public short getDamage() {
        return 0;
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public boolean isItem() {
        Identifier itemId = this.id != null
                ? Identifier.parse(this.id)
                : BuiltInRegistries.BLOCK.getKey(this.block);

        return BuiltInRegistries.ITEM.getOptional(itemId).isPresent();
    }

    @Override
    public boolean isEdible() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

}