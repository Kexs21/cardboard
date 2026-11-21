package org.bukkit.craftbukkit.map;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.cardboardpowered.impl.map.MapViewImpl;

public class CraftMapView extends MapViewImpl {

    public final MapItemSavedData worldMap;

    public CraftMapView(MapItemSavedData worldMap) {
        super(worldMap);
        this.worldMap = worldMap;
    }

    @Override
    public RenderData render(CraftPlayer player) {
        org.cardboardpowered.impl.map.RenderData original =
                super.render(player);

        RenderData result = new RenderData();

        System.arraycopy(
                original.buffer, 0,
                result.buffer, 0,
                original.buffer.length
        );

        result.cursors.addAll(original.cursors);

        return result;
    }
}
