package com.magmaguy.freeminecraftmodels.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.magmaguy.freeminecraftmodels.customentity.core.Skeleton;
import de.tr7zw.changeme.nbtapi.NBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

@Setter
@Getter
public class MountArmorStand {

    private Location location;
    private ArmorStand armorStand;
    private Skeleton skeleton;
    
    public MountArmorStand(Skeleton skeleton) {
        this.skeleton = skeleton;
        
        spawn();
    }
    
    private void spawn() {
        armorStand = (ArmorStand) skeleton.getCurrentLocation().getWorld().spawnEntity(skeleton.getCurrentLocation(), org.bukkit.entity.EntityType.ARMOR_STAND);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setPersistent(true);
        armorStand.setVisible(false);
        armorStand.setCustomNameVisible(false);
        armorStand.setSmall(true);
        armorStand.setInvisible(true);
        armorStand.setBasePlate(false);


        NBT.modifyPersistentData(armorStand, nbt -> {
            nbt.setUUID("LIVING_ENTITY_UUID", this.skeleton.getDynamicEntity().getLivingEntity().getUniqueId());
        });

    }

    public void move(Location newLocation) {
        int entityId = armorStand.getEntityId();

        // Update the stored location
        this.location = newLocation;

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityId,
                SpigotConversionUtil.fromBukkitLocation(newLocation),
                false
        );

        Bukkit.getOnlinePlayers().forEach(player -> {
           User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
           user.sendPacket(teleportPacket);
        });
    }

    public void destroy() {
        armorStand.remove();
    }
}
