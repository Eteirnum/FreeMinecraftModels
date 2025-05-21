package com.magmaguy.freeminecraftmodels.packets.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.magmaguy.freeminecraftmodels.MetadataHandler;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SteerVehiclePacketListener implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();

        if(event.getPacketType() != PacketType.Play.Client.STEER_VEHICLE) {
            return;
        }

        WrapperPlayClientSteerVehicle steerVehicle = new WrapperPlayClientSteerVehicle(event);
        float sideways = steerVehicle.getSideways();
        float forward = steerVehicle.getForward();

        Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> {
            Player player = Bukkit.getPlayer(user.getUUID());
            if (player == null) return;
            if (player.getVehicle() == null) return;
            if (!(player.getVehicle() instanceof ArmorStand armorStand)) return;

            UUID uuid = NBT.getPersistentData(armorStand, nbt -> nbt.getUUID("LIVING_ENTITY_UUID"));
            if (uuid == null) return;

            LivingEntity mount = (LivingEntity) Bukkit.getEntity(uuid);
            if (mount == null) return;

            // Update mount rotation to match player
            Location mountLoc = mount.getLocation();
            mountLoc.setYaw(player.getLocation().getYaw());
            mountLoc.setPitch(player.getLocation().getPitch());
            mount.teleport(mountLoc);

            // Movement logic
            Location loc = mount.getLocation();
            Vector direction = loc.getDirection().setY(0).normalize(); // Forward
            Vector side = new Vector(-direction.getZ(), 0, direction.getX()); // Right

            Vector velocity = direction.multiply(forward).add(side.multiply(-sideways)).multiply(0.4); // Tweak speed
            mount.setVelocity(velocity);
        });

    }
}
