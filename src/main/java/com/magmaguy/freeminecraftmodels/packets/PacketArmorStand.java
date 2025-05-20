package com.magmaguy.freeminecraftmodels.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class PacketArmorStand {
    private final String text;
    private Location location;
    private final int entityId;
    private final UUID uuid;
    private final Set<Player> viewers = new HashSet<>(); // Track viewers

    public PacketArmorStand(String text, Location location) {
        this.text = text;
        this.location = location;
        this.entityId = SpigotReflectionUtil.generateEntityId();
        this.uuid = UUID.randomUUID();
    }

    public void displayTo(Player player) {
        // Spawn the entity
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(


                entityId,
                uuid,
                EntityTypes.ARMOR_STAND,
                SpigotConversionUtil.fromBukkitLocation(location),
                location.getYaw(),
                0,
                null
        );

        List<EntityData<?>> metadataList = new ArrayList<>();

        // No Gravity
        metadataList.add(new EntityData<>(
                5,
                EntityDataTypes.BOOLEAN,
                true
        ));

        // Custom Name
        metadataList.add(new EntityData<>(
                2,
                EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                Optional.of(Component.text(text))
        ));

        // Name Visible
        metadataList.add(new EntityData<>(
                3,
                EntityDataTypes.BOOLEAN,
                true
        ));

        // Set the metadata for the text
        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
                entityId,
                metadataList
        );
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        // Send packets
        user.sendPacket(spawnPacket);
        user.sendPacket(metadataPacket);

        // Add player to viewers
        viewers.add(player);
    }

    public void hideFrom(Player player) {
        // Remove the entity from the player without destroying it
        viewers.remove(player);
    }

    public void updateLocation(Location newLocation) {

        // Update the stored location
        this.location = newLocation;

        // Send teleport packet to all viewers
        for (Player viewer : viewers) {
            WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                    entityId,
                    SpigotConversionUtil.fromBukkitLocation(newLocation),
                    false
            );
            User userViewer = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            userViewer.sendPacket(teleportPacket);
        }
    }

    public void destroy() {
        // Destroy the entity for all players
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
        for (Player viewer : viewers) {
            User userViewer = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            userViewer.sendPacket(destroyPacket);
        }
        viewers.clear();
    }
}

