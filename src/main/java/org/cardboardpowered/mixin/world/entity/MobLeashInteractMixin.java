package org.cardboardpowered.mixin.world.entity;

import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobLeashInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void cardboard$fixShiftUnleash(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!mob.isAlive()) {
            return;
        }

        if (!player.isSecondaryUseActive()) {
            return;
        }

        if (!mob.isLeashed()) {
            return;
        }

        if (mob.getLeashHolder() != player) {
            return;
        }

        if (CraftEventFactory.callPlayerUnleashEntityEvent(mob, player).isCancelled()) {
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(mob, mob.getLeashHolder()));
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        mob.removeLeash();

        if (mob.level() instanceof ServerLevel serverLevel) {
            ItemEntity leadDrop = new ItemEntity(
                    serverLevel,
                    mob.getX(),
                    mob.getY() + 0.5,
                    mob.getZ(),
                    new ItemStack(Items.LEAD)
            );
            leadDrop.setDeltaMovement(0.0, 0.2, 0.0);
            leadDrop.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(leadDrop);
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}