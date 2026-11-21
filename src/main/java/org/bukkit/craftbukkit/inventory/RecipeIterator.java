package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.inventory.Recipe;

import org.cardboardpowered.bridge.server.players.PlayerListBridge;
import org.cardboardpowered.bridge.world.item.crafting.RecipeHolderBridge;
import org.cardboardpowered.bridge.world.item.crafting.RecipeManagerBridge;

public class RecipeIterator implements Iterator<Recipe> {

    private final Iterator<Map.Entry<RecipeType<?>, RecipeHolder<?>>> recipes;

    private RecipeHolder<?> currentRecipe;
    private RecipeHolder<?> nextHolder;
    private Recipe nextRecipe;
    private boolean canRemove = false;

    public RecipeIterator() {
        this.recipes = new ArrayList<>(
                CraftServer.INSTANCE.getServer()
                        .getRecipeManager()
                        .recipes.byType.entries()
        ).iterator();
    }

    @Override
    public boolean hasNext() {
        if (nextRecipe != null) {
            return true;
        }

        while (recipes.hasNext()) {
            RecipeHolder<?> holder = recipes.next().getValue();

            try {
                Recipe bukkitRecipe = ((RecipeHolderBridge) (Object) holder).toBukkitRecipe();

                if (bukkitRecipe != null) {
                    nextHolder = holder;
                    nextRecipe = bukkitRecipe;
                    return true;
                }

            } catch (IllegalArgumentException e) {
                String message = e.getMessage();

                if (message == null ||
                        !message.startsWith("Invalid recipe type:")) {
                    throw e;
                }
            }
        }

        return false;
    }

    @Override
    public Recipe next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        currentRecipe = nextHolder;
        Recipe result = nextRecipe;

        nextHolder = null;
        nextRecipe = null;
        canRemove = true;

        return result;
    }

    @Override
    public void remove() {
        if (!canRemove) {
            throw new IllegalStateException();
        }

        var manager = CraftServer.INSTANCE.getServer().getRecipeManager();
        manager.recipes.byKey.remove(currentRecipe.id());
        manager.recipes.byType.remove(
                currentRecipe.value().getType(),
                currentRecipe
        );

        ((RecipeManagerBridge) manager).cardboard$finalizeRecipeLoading();
        ((PlayerListBridge) CraftServer.INSTANCE.getServer().getPlayerList()).cardboard$reloadRecipes();
        canRemove = false;
        currentRecipe = null;
    }
}
