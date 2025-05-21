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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SteerVehiclePacketListener implements PacketListener, Listener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();

        if (event.getPacketType() != PacketType.Play.Client.STEER_VEHICLE) return;

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

            // Movement vectors
            Vector direction = mountLoc.getDirection().setY(0).normalize(); // Forward direction
            Vector side = new Vector(-direction.getZ(), 0, direction.getX()); // Right direction

            Vector velocity = direction.multiply(forward).add(side.multiply(-sideways)).multiply(0.4); // Speed tweak

            // Adjust mount height to ground level
            Location adjustedLoc = adjustMountHeightToGround(mount.getLocation(), 10);

            // Adjust mount height for stepping up obstacles
            adjustedLoc = adjustMountHeightForStepUp(adjustedLoc, velocity);

            if (!adjustedLoc.equals(mount.getLocation())) {
                mount.teleport(adjustedLoc);
            }

            // Apply velocity to mount
            mount.setVelocity(velocity);
        });
    }

    /**
     * Checks blocks below the given location up to maxDepth blocks down.
     * If no solid block is found directly below, lowers the location to stand on the first solid block.
     *
     * @param loc The starting location to check from
     * @param maxDepth The maximum number of blocks to check downward
     * @return A Location adjusted downwards if needed, or the original location if no adjustment was needed
     */
    private Location adjustMountHeightToGround(Location loc, int maxDepth) {
        World world = loc.getWorld();
        int checkDepth = 0;

        while (checkDepth < maxDepth) {
            Location checkLoc = loc.clone().subtract(0, checkDepth + 1, 0);
            Block block = world.getBlockAt(checkLoc);
            if (!block.isPassable() && block.getType() != Material.AIR) {
                return loc.clone().subtract(0, checkDepth, 0);
            }
            checkDepth++;
        }

        // No block found below within maxDepth, return original location
        return loc;
    }

    /**
     * Given current location and velocity direction, check blocks at the destination point ahead.
     * If there is a 1-block-high obstacle in front (a block at Y or Y+1), adjust the Y to step up.
     * Returns adjusted location.
     *
     * @param loc Current location of the mount
     * @param velocity Velocity vector representing movement direction and speed
     * @return Location adjusted for stepping up if needed, or original location if no step needed
     */
    private Location adjustMountHeightForStepUp(Location loc, Vector velocity) {
        World world = loc.getWorld();

        // Calculate next position based on velocity direction, ignoring speed magnitude (normalize direction)
        Vector direction = velocity.clone().setY(0).normalize();
        if (direction.lengthSquared() == 0) return loc; // no movement

        // The next horizontal position we want to check (1 block ahead)
        Location nextPos = loc.clone().add(direction);

        // Floor coordinates for block checking
        int x = nextPos.getBlockX();
        int y = nextPos.getBlockY();
        int z = nextPos.getBlockZ();

        // Blocks to check:
        Block blockAtFeet = world.getBlockAt(x, y, z);
        Block blockOneAbove = world.getBlockAt(x, y + 1, z);

        // If block at feet level is solid but block one above is air/passable, we can step up
        if ((!blockAtFeet.isPassable() && blockAtFeet.getType() != Material.AIR)
                && (blockOneAbove.isPassable() || blockOneAbove.getType() == Material.AIR)) {

            // Increase Y by 1 block to step up
            return loc.clone().add(0, 1, 0);
        }

        // If no step up needed, return original location
        return loc;
    }

}
