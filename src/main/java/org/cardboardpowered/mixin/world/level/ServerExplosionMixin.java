
package org.cardboardpowered.mixin.world.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;

import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.entity.EntityExplodeEvent;

import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {

    @ModifyExpressionValue(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"
            )
    )
    private List<BlockPos> cardboard$creeperExplosionEvent(
            List<BlockPos> positions
    ) {
        if (positions.isEmpty()) {
            return positions;
        }

        ServerExplosion explosion =
                (ServerExplosion) (Object) this;

        Entity source = explosion.getDirectSourceEntity();

        // Пока обрабатываем только криперов
        if (!(source instanceof Creeper)) {
            return positions;
        }

        World world = ((LevelBridge) (Object) explosion.level())
                .cardboard$getWorld();

        if (world == null) {
            return positions;
        }

        List<Block> blocks = new ArrayList<>();

        for (BlockPos pos : positions) {
            Block block = world.getBlockAt(
                    pos.getX(), pos.getY(), pos.getZ()
            );

            if (!block.getType().isAir()) {
                blocks.add(block);
            }
        }

        Explosion.BlockInteraction interaction =
                explosion.getBlockInteraction();

        ExplosionResult result = switch (interaction) {
            case DESTROY -> ExplosionResult.DESTROY;
            case DESTROY_WITH_DECAY ->
                    ExplosionResult.DESTROY_WITH_DECAY;
            case TRIGGER_BLOCK ->
                    ExplosionResult.TRIGGER_BLOCK;
            case KEEP -> ExplosionResult.KEEP;
        };

        float yield = interaction ==
                Explosion.BlockInteraction.DESTROY_WITH_DECAY
                ? 1.0F / Math.max(1.0F, explosion.radius())
                : 1.0F;

        Location location = new Location(
                world,
                explosion.center().x,
                explosion.center().y,
                explosion.center().z
        );

        EntityExplodeEvent event = new EntityExplodeEvent(
                ((EntityBridge) source).getBukkitEntity(),
                location,
                blocks,
                yield,
                result
        );

        CraftServer.INSTANCE.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return new ArrayList<>();
        }

        List<BlockPos> allowed = new ArrayList<>();

        for (Block block : event.blockList()) {
            if (!block.getWorld().equals(world)) {
                continue;
            }

            allowed.add(new BlockPos(
                    block.getX(),
                    block.getY(),
                    block.getZ()
            ));
        }

        return allowed;
    }
}
