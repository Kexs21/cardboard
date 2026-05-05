package org.cardboardpowered.mixin.world.entity;

import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobLeashInteractMixin {

    @Shadow
    public abstract Leashable.LeashData getLeashData();

    @Shadow
    public abstract void setLeashData(Leashable.LeashData leashData);

    @Shadow
    public abstract void onLeashRemoved();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void cardboard$fixLeashInteractions(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!mob.isAlive() || !mob.isLeashed()) {
            return;
        }

        Entity leashHolder = mob.getLeashHolder();
        if (leashHolder == null) {
            return;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean usingLead = held.is(Items.LEAD);
        boolean usingShears = held.is(Items.SHEARS);

        // 1) Игрок держит новый поводок и тыкает в моба,
        //    который уже привязан к другому держателю -> старый поводок должен выпасть в мир,
        //    затем ваниль продолжит и привяжет новым поводком к игроку.
        if (usingLead && leashHolder != player) {
            cardboard$dropLeadInWorld(mob);
            return; // НЕ cancel: дальше идёт обычная ванильная логика привязки новым поводком
        }

        // 2) Игрок сам держит моба на поводке и просто ПКМ по мобу -> поводок выпадает в мир
        // 3) Игрок режет поводок ножницами -> поводок выпадает в мир
        if (leashHolder == player || usingShears) {
            if (CraftEventFactory.callPlayerUnleashEntityEvent(mob, player).isCancelled()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetEntityLinkPacket(mob, leashHolder));
                }
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }

            cardboard$dropLeadInWorld(mob);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "leashTooFarBehaviour", at = @At("HEAD"), cancellable = true)
    private void cardboard$fixLeashTooFar(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!mob.isLeashed()) {
            return;
        }

        cardboard$dropLeadInWorld(mob);
        ci.cancel();
    }

    @Unique
    private void cardboard$dropLeadInWorld(Mob mob) {
        Entity entity = (Entity) (Object) this;
        Leashable.LeashData leashData = this.getLeashData();

        if (leashData == null || leashData.leashHolder == null) {
            return;
        }

        Entity oldHolder = leashData.leashHolder;

        this.setLeashData(null);
        this.onLeashRemoved();

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

            serverLevel.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, null));
            oldHolder.notifyLeasheeRemoved((Leashable) entity);
        }
    }
}