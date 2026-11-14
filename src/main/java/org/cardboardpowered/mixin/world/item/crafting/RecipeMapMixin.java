package org.cardboardpowered.mixin.world.item.crafting;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.*;
import org.cardboardpowered.bridge.world.item.crafting.RecipeMapBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.google.common.collect.LinkedHashMultimap;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.LinkedHashMap;

import java.util.Collection;
import java.util.Map;

@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin implements RecipeMapBridge {
    @Shadow
    @Final
    public Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow
    @Final
    public Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    @Shadow
    public abstract <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> recipeType);

    @ModifyArg(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeMap;<init>(Lcom/google/common/collect/Multimap;Ljava/util/Map;)V"
            ),
            index = 0
    )
    private static Multimap<RecipeType<?>, RecipeHolder<?>> cardboard$makeByTypeMutable(
            Multimap<RecipeType<?>, RecipeHolder<?>> recipes) {
        return LinkedHashMultimap.create(recipes);
    }

    @ModifyArg(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeMap;<init>(Lcom/google/common/collect/Multimap;Ljava/util/Map;)V"
            ),
            index = 1
    )
    private static Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> cardboard$makeByKeyMutable(
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes) {
        return new LinkedHashMap<>(recipes);
    }

    @Override
    public void cardboard$addRecipe(RecipeHolder<?> holder) {
        Collection<RecipeHolder<?>> recipes = this.byType.get(holder.value().getType());

        if (this.byKey.containsKey(holder.id())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + holder.id());
        } else {
            recipes.add(holder);
            this.byKey.put(holder.id(), holder);
        }
    }
    // CraftBukkit end

    // Paper start - replace removeRecipe implementation
    @Override
    public <T extends RecipeInput> boolean cardboard$removeRecipe(ResourceKey<Recipe<T>> mcKey) {
        //noinspection unchecked
        final RecipeHolder<Recipe<T>> remove = (RecipeHolder<Recipe<T>>) this.byKey.remove(mcKey);
        if (remove == null) {
            return false;
        }
        final Collection<? extends RecipeHolder<? extends Recipe<T>>> recipes = this.byType(remove.value().getType());
        return recipes.remove(remove);
        // Paper end - why are you using a loop???
    }
    // Paper end - replace removeRecipe implementation
}
