package moe.minacle.minecraft.plugins.hameln;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

public final class Plugin extends JavaPlugin implements Listener {

    private Map<UUID, Set<Pair<UUID, EquipmentSlot>>> playerTridentSlotPairSetMap;
    private RandomGenerator randomGenerator;
    private File varFolder;

    private @Nullable Set<Pair<UUID, EquipmentSlot>> getTridentSlotPairSet(final @NotNull UUID playerUUID, final boolean createIfAbsent) {
        final Set<Pair<UUID, EquipmentSlot>> tridentSlotMap;
        if (!playerTridentSlotPairSetMap.containsKey(playerUUID)) {
            if (createIfAbsent)
                playerTridentSlotPairSetMap.put(playerUUID, tridentSlotMap = new HashSet<>());
            else
                return null;
        }
        else
            tridentSlotMap = playerTridentSlotPairSetMap.get(playerUUID);
        return tridentSlotMap;
    }

    private void sync(final @NotNull UUID playerUUID) {
        final File playerFile;
        final String playerUUIDString;
        Set<Pair<UUID, EquipmentSlot>> tridentSlotPairSet;
        if (varFolder == null)
            return;
        playerUUIDString = playerUUID.toString();
        playerFile = new File(new File(varFolder, playerUUIDString.substring(0, 2)), playerUUIDString);
        tridentSlotPairSet = getTridentSlotPairSet(playerUUID, false);
        if (tridentSlotPairSet == null) {
            Scanner scanner;
            if (!playerFile.isFile())
                return;
            try {
                scanner = new Scanner(playerFile);
            }
            catch (final FileNotFoundException exception) {
                return;
            }
            tridentSlotPairSet = new HashSet<>();
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final String[] parts;
                if (line.isEmpty())
                    continue;
                if ((parts = line.split(" ")).length < 2)
                    continue;
                try {
                    tridentSlotPairSet.add(Pair.of(UUID.fromString(parts[0]), EquipmentSlot.valueOf(parts[1])));
                }
                catch (final IllegalArgumentException exception) {
                    continue;
                }
            }
            scanner.close();
            playerTridentSlotPairSetMap.put(playerUUID, tridentSlotPairSet);
        }
        else {
            PrintWriter writer;
            if (!playerFile.exists()) {
                final File folder = playerFile.getParentFile();
                if (!folder.exists())
                    folder.mkdirs();
                try {
                    playerFile.createNewFile();
                }
                catch (final IOException exception) {
                    return;
                }
            }
            try {
                writer = new PrintWriter(playerFile);
            }
            catch (final FileNotFoundException exception) {
                return;
            }
            for (final Pair<UUID, EquipmentSlot> tridentSlotPair : tridentSlotPairSet)
                writer.println(tridentSlotPair.getLeft().toString() + " " + tridentSlotPair.getRight().toString());
            writer.close();
        }
    }

    private void syncAll() {
        if (varFolder == null)
            return;
        if (playerTridentSlotPairSetMap == null) {
            File[] directories = varFolder.listFiles();
            playerTridentSlotPairSetMap = new HashMap<>();
            if (directories == null)
                return;
            for (final File directory : directories) {
                if (!directory.isDirectory())
                    continue;
                for (final File playerFile : directory.listFiles()) {
                    if (!playerFile.isFile())
                        continue;
                    final String playerUUIDString = playerFile.getName();
                    final UUID playerUUID;
                    try {
                        playerUUID = UUID.fromString(playerUUIDString);
                    }
                    catch (final IllegalArgumentException exception) {
                        continue;
                    }
                    sync(playerUUID);
                }
            }
        }
        else
            for (final UUID playerUUID : playerTridentSlotPairSetMap.keySet())
                sync(playerUUID);
    }

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        randomGenerator = RandomGenerator.of("Random");
        varFolder = new File(getDataFolder(), "var");
        syncAll();
        if (playerTridentSlotPairSetMap == null)
            playerTridentSlotPairSetMap = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        syncAll();
        playerTridentSlotPairSetMap = null;
        randomGenerator = null;
        varFolder = null;
    }

    // MARK: Listener

    @EventHandler
    public void onEntityRemoveFromWorld(final @NotNull EntityRemoveFromWorldEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final Set<Pair<UUID, EquipmentSlot>> tridentSlotPairSet = getTridentSlotPairSet(player.getUniqueId(), false);
                final UUID tridentUUID;
                if (tridentSlotPairSet == null || tridentSlotPairSet.isEmpty())
                    return;
                tridentUUID = trident.getUniqueId();
                for (final Pair<UUID, EquipmentSlot> tridentSlotPair : tridentSlotPairSet)
                    if (tridentSlotPair.getLeft().equals(tridentUUID)) {
                        tridentSlotPairSet.remove(tridentSlotPair);
                        return;
                    }
            }
    }

    @EventHandler
    @SuppressWarnings("deprecation")  // org.bukkit.event.player.PlayerPickupArrowEvent#setCancelled is not deprecated actually
    public void onPlayerPickupArrow(final @NotNull PlayerPickupArrowEvent event) {
        final AbstractArrow arrow = event.getArrow();
        if (arrow instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final ItemStack tridentItem = trident.getItem();
                final PlayerInventory playerInventory = player.getInventory();
                final Set<Pair<UUID, EquipmentSlot>> tridentSlotPairSet = getTridentSlotPairSet(player.getUniqueId(), false);
                final UUID tridentUUID;
                if (tridentSlotPairSet == null || tridentSlotPairSet.isEmpty())
                    return;
                tridentUUID = trident.getUniqueId();
                for (final Pair<UUID, EquipmentSlot> tridentSlotPair : tridentSlotPairSet)
                    if (tridentSlotPair.getLeft().equals(tridentUUID)) {
                        final EquipmentSlot equipmentSlot = tridentSlotPair.getRight();
                        if (playerInventory.getItem(equipmentSlot).getType() == Material.AIR) {
                            final float itemPickupSoundPitch =
                                // stolen from net.minecraft.client.multiplayer.ClientPacketListener#handleTakeItemEntity
                                (randomGenerator.nextFloat() - randomGenerator.nextFloat()) * 1.4f + 2f;
                            final Location tridentLocation = trident.getLocation();
                            event.setCancelled(true);
                            playerInventory.setItem(equipmentSlot, tridentItem);
                            player.getWorld().playSound(tridentLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f, itemPickupSoundPitch);
                        }
                        trident.remove();
                        return;
                    }
            }
    }

    @EventHandler
    public void onProjectileLaunch(final @NotNull ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final PlayerInventory playerInventory = player.getInventory();
                final ItemStack tridentItem = trident.getItem();
                final Set<Pair<UUID, EquipmentSlot>> tridentSlotPairSet = getTridentSlotPairSet(player.getUniqueId(), true);
                if (tridentItem.equals(playerInventory.getItemInMainHand()))
                    tridentSlotPairSet.add(Pair.of(trident.getUniqueId(), EquipmentSlot.HAND));
                else if (tridentItem.equals(playerInventory.getItemInOffHand()))
                    tridentSlotPairSet.add(Pair.of(trident.getUniqueId(), EquipmentSlot.OFF_HAND));
            }
    }
}
