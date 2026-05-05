package org.cardboardpowered.mixin.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobHarnessInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void cardboard$dropMobEquipmentWithShears(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!mob.isAlive()) {
            return;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.SHEARS)) {
            return;
        }

        boolean changed = false;

        changed |= cardboard$dropSlotItem(mob, EquipmentSlot.BODY);
        changed |= cardboard$dropSlotItem(mob, EquipmentSlot.SADDLE);

        if (changed) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    private boolean cardboard$dropSlotItem(Mob mob, EquipmentSlot slot) {
        ItemStack stack = mob.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack drop = stack.copy();
        mob.setItemSlot(slot, ItemStack.EMPTY);

        if (mob.level() instanceof ServerLevel serverLevel) {
            ItemEntity itemDrop = new ItemEntity(
                    serverLevel,
                    mob.getX(),
                    mob.getY() + 0.5,
                    mob.getZ(),
                    drop
            );
            itemDrop.setDeltaMovement(0.0, 0.2, 0.0);
            itemDrop.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemDrop);
        }

        return true;
    }
}