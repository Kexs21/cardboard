/**
 * CardboardPowered - Bukkit/Spigot for Fabric
 * Copyright (C) CardboardPowered.org and contributors
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.cardboardpowered.mixin.world.level.saveddata.maps;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.cardboardpowered.impl.map.MapViewImpl;
import org.cardboardpowered.bridge.world.level.saveddata.maps.MapItemSavedDataBridge;
import org.bukkit.craftbukkit.map.CraftMapView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapId;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.cardboardpowered.impl.map.RenderData;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapItemSavedData.class)
public class MapItemSavedDataMixin implements MapItemSavedDataBridge {

    public MapViewImpl mapView;

    //@Inject(at = @At("TAIL"), method="<init>*")
    //public void setMapView(String s, CallbackInfo ci) {
    //    mapView = new MapViewImpl((MapState)(Object)this);
    //}

    @Inject(at = @At("TAIL"), method="<init>*")
    public void setMapView(int a, int b, byte c, boolean d, boolean e, boolean f, ResourceKey key, CallbackInfo ci) {
        mapView = new CraftMapView((MapItemSavedData)(Object)this);
    }


    @Override
    public MapViewImpl getMapViewBF() {
        return mapView;
    }

    @Unique
    private final Map<UUID, Long> cardboard$lastMapRender =
            new HashMap<>();

    @Inject(
            method = "getUpdatePacket",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cardboard$renderBukkitMap(
            MapId mapId,
            Player player,
            CallbackInfoReturnable<Packet<?>> cir
    ) {
        // Обычные карты Minecraft не затрагиваем.
        if (mapView == null || !mapView.isVirtual()) {
            return;
        }

        org.bukkit.entity.Player bukkitPlayer =
                Bukkit.getPlayer(player.getUUID());

        if (!(bukkitPlayer instanceof CraftPlayer craftPlayer)) {
            return;
        }

        UUID uuid = player.getUUID();
        long now = System.nanoTime();

        // Для теста обновляем изображение не чаще раза в секунду.
        Long previous = cardboard$lastMapRender.get(uuid);

        if (previous != null && now - previous < 1_000_000_000L) {
            cir.setReturnValue(null);
            return;
        }

        RenderData data = mapView.render(craftPlayer);

        if (data.buffer == null || data.buffer.length != 128 * 128) {
            return;
        }

        MapItemSavedData savedData =
                (MapItemSavedData) (Object) this;

        ClientboundMapItemDataPacket packet =
                new ClientboundMapItemDataPacket(
                        mapId,
                        savedData.scale,
                        savedData.locked,
                        Optional.empty(),
                        Optional.of(
                                new MapItemSavedData.MapPatch(
                                        0, 0, 128, 128,
                                        data.buffer.clone()
                                )
                        )
                );

        cardboard$lastMapRender.put(uuid, now);
        cir.setReturnValue(packet);
    }
}