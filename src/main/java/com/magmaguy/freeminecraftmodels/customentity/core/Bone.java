package com.magmaguy.freeminecraftmodels.customentity.core;

import com.magmaguy.freeminecraftmodels.MetadataHandler;
import com.magmaguy.freeminecraftmodels.config.DefaultConfig;
import com.magmaguy.freeminecraftmodels.dataconverter.BoneBlueprint;
import com.magmaguy.freeminecraftmodels.entities.MountArmorStand;
import com.magmaguy.freeminecraftmodels.packets.PacketArmorStand;
import com.magmaguy.freeminecraftmodels.thirdparty.Floodgate;
import com.magmaguy.magmacore.util.VersionChecker;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Bone {
    private final BoneBlueprint boneBlueprint;
    private final List<Bone> boneChildren = new ArrayList<>();
    private final Bone parent;
    private final Skeleton skeleton;
    private final BoneTransforms boneTransforms;
    private Vector3f animationTranslation = new Vector3f();
    private Vector3f animationRotation = new Vector3f();
    private float animationScale = -1;
    public PacketArmorStand nametag = null;
    public MountArmorStand mountArmorStand = null;
    public String displayName = "namePlaceholder";

    public Bone(BoneBlueprint boneBlueprint, Bone parent, Skeleton skeleton) {
        this.boneBlueprint = boneBlueprint;
        this.parent = parent;
        this.skeleton = skeleton;
        this.boneTransforms = new BoneTransforms(this, parent);
        for (BoneBlueprint child : boneBlueprint.getBoneBlueprintChildren())
            boneChildren.add(new Bone(child, this, skeleton));

        if(this.boneBlueprint.isNameTag()) {
            addNametag();
        }

        if(this.boneBlueprint.isMount()) {
            addMount();
        }
    }

    public void updateAnimationTranslation(float x, float y, float z) {
        animationTranslation = new Vector3f(x, y, z);
    }

    public void updateAnimationRotation(double x, double y, double z) {
        animationRotation = new Vector3f((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }

    public void updateAnimationScale(float animationScale) {
        this.animationScale = animationScale;
    }

    //Note that several optimizations might be possible here, but that syncing with a base entity is necessary.
    public void transform() {
        boneTransforms.transform();
        boneChildren.forEach(Bone::transform);
        skeleton.getSkeletonWatchers().sendPackets(this);
        if(this.boneBlueprint.isNameTag()) {
            if(nametag != null) {
                Location location = this.getSkeleton().getCurrentLocation();
                location.setY((location.getY() - boneBlueprint.getBlueprintModelPivot().y) - 1.3);
                nametag.updateLocation(location);
                nametag.setText(displayName);
            }
        }
        if(mountArmorStand != null) {
            Location location = this.getSkeleton().getCurrentLocation();
            location.setY((location.getY() - boneBlueprint.getBlueprintModelPivot().y));
            mountArmorStand.move(location);
        }
    }

    public void generateDisplay() {
        boneTransforms.generateDisplay();
        boneChildren.forEach(Bone::generateDisplay);
    }

    public void setName(String name) {
        boneChildren.forEach(child -> child.setName(name));
    }

    public void setNameVisible(boolean visible) {
        boneChildren.forEach(child -> child.setNameVisible(visible));
    }

    public void getNametags(List<ArmorStand> nametags) {
        boneChildren.forEach(child -> child.getNametags(nametags));
    }

    public void remove() {
        if (boneTransforms.getPacketArmorStandEntity() != null) boneTransforms.getPacketArmorStandEntity().remove();
        if (boneTransforms.getPacketDisplayEntity() != null) boneTransforms.getPacketDisplayEntity().remove();
        boneChildren.forEach(Bone::remove);
        if(this.boneBlueprint.isNameTag()) {
            if(nametag != null) {
                nametag.destroy();
            }
        }
        if(mountArmorStand != null) {
            mountArmorStand.destroy();
        }
    }

    protected void getAllChildren(HashMap<String, Bone> children) {
        boneChildren.forEach(child -> {
            children.put(child.getBoneBlueprint().getBoneName(), child);
            child.getAllChildren(children);
        });
    }

    public void sendUpdatePacket() {
        boneTransforms.sendUpdatePacket();
    }

    public void displayTo(Player player) {
        if (boneTransforms.getPacketArmorStandEntity() != null &&
                (!DefaultConfig.useDisplayEntitiesWhenPossible ||
                        Floodgate.isBedrock(player) ||
                        VersionChecker.serverVersionOlderThan(19, 4))) {
            boneTransforms.getPacketArmorStandEntity().displayTo(player.getUniqueId());
        }
        else if (boneTransforms.getPacketDisplayEntity() != null) {
            boneTransforms.getPacketDisplayEntity().displayTo(player.getUniqueId());
        }

        if(this.boneBlueprint.isNameTag()) {
            Bukkit.getScheduler().runTaskLater(MetadataHandler.PLUGIN, () -> {
                nametag.displayTo(player);
            }, 5L);
        }
    }

    public void hideFrom(UUID playerUUID) {
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().hideFrom(playerUUID);
        if (boneTransforms.getPacketDisplayEntity() != null)
            boneTransforms.getPacketDisplayEntity().hideFrom(playerUUID);
        if(this.boneBlueprint.isNameTag()) {
            nametag.hideFrom(Bukkit.getPlayer(playerUUID));
        }
    }

    public void setHorseLeatherArmorColor(Color color) {
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().setHorseLeatherArmorColor(color);
        if (boneTransforms.getPacketDisplayEntity() != null)
            boneTransforms.getPacketDisplayEntity().setHorseLeatherArmorColor(color);
    }

    public void teleport() {
        sendTeleportPacket();
        boneChildren.forEach(Bone::teleport);
    }

    private void sendTeleportPacket() {
        if (boneTransforms.getPacketArmorStandEntity() != null) {
            boneTransforms.getPacketArmorStandEntity().teleport(boneTransforms.getArmorStandTargetLocation());
        }
        if (boneTransforms.getPacketDisplayEntity() != null) {
            boneTransforms.getPacketDisplayEntity().teleport(boneTransforms.getDisplayEntityTargetLocation());
        }
    }

    private void addNametag() {
        if(this.boneBlueprint.isNameTag()) {
            nametag = new PacketArmorStand(displayName, this.getSkeleton().getCurrentLocation());
        }
    }

    private void addMount() {
        if(this.boneBlueprint.isMount()) {
            Bukkit.getScheduler().runTaskLater(MetadataHandler.PLUGIN, () -> {
                mountArmorStand = new MountArmorStand(this.getSkeleton());
            }, 3L);

        }
    }
}
