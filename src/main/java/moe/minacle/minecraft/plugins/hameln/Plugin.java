package moe.minacle.minecraft.plugins.hameln;

import java.util.random.RandomGenerator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType.PrimitivePersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class Plugin extends JavaPlugin implements Listener {

    private static enum Hand {

        MAIN_HAND((byte)0, EquipmentSlot.HAND),
        OFF_HAND((byte)1, EquipmentSlot.OFF_HAND);

        private static @Nullable NamespacedKey key;

        static @NotNull NamespacedKey getKey() {
            if (key == null)
                key = NamespacedKey.fromString("hand", getSharedPlugin());
            return key;
        }

        static @Nullable Hand of(final byte value) {
            switch (value) {
                case 0:
                    return MAIN_HAND;
                case 1:
                    return OFF_HAND;
                default:
                    return null;
            }
        }

        private final EquipmentSlot equipmentSlot;

        private final byte value;

        private Hand(final Byte value, final EquipmentSlot equipmentSlot) {
            this.value = value;
            this.equipmentSlot = equipmentSlot;
        }

        @NotNull EquipmentSlot getEquipmentSlot() {
            return equipmentSlot;
        }

        byte getValue() {
            return value;
        }
    }

    private static @Nullable Plugin sharedPlugin;

    public static @UnknownNullability Plugin getSharedPlugin() {
        return sharedPlugin;
    }

    private @Nullable RandomGenerator randomGenerator;

    @EventHandler
    @SuppressWarnings("deprecation")  // org.bukkit.event.player.PlayerPickupArrowEvent#setCancelled is not deprecated actually
    private void onPlayerPickupArrow(final @NotNull PlayerPickupArrowEvent event) {
        if (event.getArrow() instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final PersistentDataContainer tridentPersistentDataContainer;
                final Hand hand;
                final EquipmentSlot equipmentSlot;
                final PlayerInventory playerInventory;
                if (trident.getLoyaltyLevel() <= 0)
                    return;
                if (!player.equals(event.getPlayer()))
                    return;
                tridentPersistentDataContainer = trident.getPersistentDataContainer();
                if (!tridentPersistentDataContainer.has(Hand.getKey(), PrimitivePersistentDataType.BYTE))
                    return;
                hand = Hand.of(tridentPersistentDataContainer.get(Hand.getKey(), PrimitivePersistentDataType.BYTE));
                if (hand == null)
                    return;
                equipmentSlot = hand.getEquipmentSlot();
                playerInventory = player.getInventory();
                if (playerInventory.getItem(equipmentSlot).getType() == Material.AIR) {
                    final float itemPickupSoundPitch =
                        // stolen from net.minecraft.client.multiplayer.ClientPacketListener#handleTakeItemEntity
                        (randomGenerator.nextFloat() - randomGenerator.nextFloat()) * 1.4f + 2f;
                    final ItemStack tridentItem = trident.getItem();
                    final Location tridentLocation = trident.getLocation();
                    event.setCancelled(true);
                    playerInventory.setItem(equipmentSlot, tridentItem);
                    trident.remove();
                    player.getWorld().playSound(tridentLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f, itemPickupSoundPitch);
                }
            }
    }

    @EventHandler
    private void onProjectileLaunch(final @NotNull ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final PlayerInventory playerInventory = player.getInventory();
                final ItemStack tridentItem = trident.getItem();
                if (tridentItem.equals(playerInventory.getItemInMainHand()))
                    trident.getPersistentDataContainer().set(Hand.getKey(), PrimitivePersistentDataType.BYTE, Hand.MAIN_HAND.getValue());
                else if (tridentItem.equals(playerInventory.getItemInOffHand()))
                    trident.getPersistentDataContainer().set(Hand.getKey(), PrimitivePersistentDataType.BYTE, Hand.OFF_HAND.getValue());
                else
                    return;
            }
    }

    // MARK: JavaPlugin

    @Override
    public void onLoad() {
        super.onLoad();
        sharedPlugin = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        randomGenerator = RandomGenerator.of("Random");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        randomGenerator = null;
    }
}
