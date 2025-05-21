package com.magmaguy.freeminecraftmodels.entities;

import com.magmaguy.freeminecraftmodels.customentity.core.Skeleton;
import de.tr7zw.changeme.nbtapi.NBT;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;
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
        // Update the stored location
        this.location = newLocation;


        CraftEntity craftEntity = (CraftEntity) armorStand;
        craftEntity.getHandle().a(newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.getYaw(), newLocation.getPitch());

    }

    public void destroy() {
        armorStand.remove();
    }
}
