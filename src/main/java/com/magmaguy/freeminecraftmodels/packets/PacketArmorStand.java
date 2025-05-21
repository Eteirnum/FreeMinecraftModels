package com.magmaguy.freeminecraftmodels.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
@Setter
public class PacketArmorStand {
    private String text;
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

    public void mount(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        int[] passengerList = {user.getEntityId()};
        WrapperPlayServerSetPassengers setPassengersPacket = new WrapperPlayServerSetPassengers(
                entityId,
                passengerList
        );

        user.sendPacket(setPassengersPacket);
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

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        // Send packets
        user.sendPacket(spawnPacket);

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

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityId,
                SpigotConversionUtil.fromBukkitLocation(newLocation),
                false
        );


        List<EntityData<?>> metadataList = new ArrayList<>();
        // No Gravity
        metadataList.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        // Custom Name
        metadataList.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(Component.text(text))));
        // Name Visible
        metadataList.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        // Is Small
        metadataList.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 0x01));
        // Is Invisible
        metadataList.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));

        // Set the metadata for the text
        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
                entityId,
                metadataList
        );

        // Send teleport packet to all viewers
        for (Player viewer : viewers) {
            User userViewer = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (userViewer == null) continue;
            userViewer.sendPacket(teleportPacket);
            userViewer.sendPacket(metadataPacket);
        }
    }

    public void destroy() {
        // Destroy the entity for all players
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
        for (Player viewer : viewers) {
            User userViewer = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (userViewer == null) continue;
            userViewer.sendPacket(destroyPacket);
        }
        viewers.clear();
    }
}

