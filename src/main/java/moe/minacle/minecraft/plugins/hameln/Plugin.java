package moe.minacle.minecraft.plugins.hameln;

import java.util.Locale;
import java.util.random.RandomGenerator;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.PortalType;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
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

import com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent;

public final class Plugin extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 20342;

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

    private @Nullable Configuration configuration;

    private @Nullable RandomGenerator randomGenerator;

    private void addCustomChartsToMetrics(final @NotNull Metrics metrics) {
        final Configuration.PickupSection pickup = configuration.getPickup();
        final Configuration.LoyaltySection loyalty = configuration.getLoyalty();
        final Configuration.PortalSection portal = configuration.getPortal();
        final String pickupToMainHand = pickup.getToMainHand() ? "true" : "false";
        final String pickupToOffHand = pickup.getToOffHand() ? "true" : "false";
        final String loyaltyToMainHand = loyalty.getToMainHand() ? "true" : "false";
        final String loyaltyToOffHand = loyalty.getToOffHand() ? "true" : "false";
        final String loyaltyCollideBelowWorldBoundary = loyalty.getCollideBelowWorldBoundary() ? "true" : "false";
        final String portalNetherPortalToNether = portal.getNetherPortal().getToNether().toString().toLowerCase(Locale.ROOT);
        final String portalNetherPortalToOverworld = portal.getNetherPortal().getToOverworld().toString().toLowerCase(Locale.ROOT);
        final String portalEndPortalToEnd = portal.getEndPortal().getToEnd().toString().toLowerCase(Locale.ROOT);
        final String portalEndPortalToOverworld = portal.getEndPortal().getToOverworld().toString().toLowerCase(Locale.ROOT);
        final String portalEndGateway = portal.getEndGateway().toString().toLowerCase(Locale.ROOT);
        metrics.addCustomChart(new SimplePie("pickup.toMainHand", () -> pickupToMainHand));
        metrics.addCustomChart(new SimplePie("pickup.toOffHand", () -> pickupToOffHand));
        metrics.addCustomChart(new SimplePie("loyalty.toMainHand", () -> loyaltyToMainHand));
        metrics.addCustomChart(new SimplePie("loyalty.toOffHand", () -> loyaltyToOffHand));
        metrics.addCustomChart(new SimplePie("loyalty.collideBelowWorldBoundary", () -> loyaltyCollideBelowWorldBoundary));
        metrics.addCustomChart(new SimplePie("portal.netherPortal.toNether", () -> portalNetherPortalToNether));
        metrics.addCustomChart(new SimplePie("portal.netherPortal.toOverworld", () -> portalNetherPortalToOverworld));
        metrics.addCustomChart(new SimplePie("portal.endPortal.toEnd", () -> portalEndPortalToEnd));
        metrics.addCustomChart(new SimplePie("portal.endPortal.toOverworld", () -> portalEndPortalToOverworld));
        metrics.addCustomChart(new SimplePie("portal.endGateway", () -> portalEndGateway));
    }

    @EventHandler(ignoreCancelled = true)
    private void onEntityPortal(final @NotNull EntityPortalEvent event) {
        if (event.getEntity() instanceof final Trident trident) {
            final Environment fromEnvironment = event.getFrom().getWorld().getEnvironment();
            final Environment toEnvironment = event.getTo().getWorld().getEnvironment();
            final Configuration.TeleportSpecification teleportSpecification;
            if (event.getPortalType() == PortalType.NETHER) {
                if (fromEnvironment == Environment.NORMAL && toEnvironment == Environment.NETHER)
                    teleportSpecification = configuration.getPortal().getNetherPortal().getToNether();
                else if (fromEnvironment == Environment.NETHER && toEnvironment == Environment.NORMAL)
                    teleportSpecification = configuration.getPortal().getNetherPortal().getToOverworld();
                else
                    return;
            }
            else if (event.getPortalType() == PortalType.ENDER) {
                if (fromEnvironment == Environment.NORMAL && toEnvironment == Environment.THE_END)
                    teleportSpecification = configuration.getPortal().getEndPortal().getToEnd();
                else if (fromEnvironment == Environment.THE_END && toEnvironment == Environment.NORMAL)
                    teleportSpecification = configuration.getPortal().getEndPortal().getToOverworld();
                else
                    return;
            }
            else
                return;
            if (teleportSpecification == Configuration.TeleportSpecification.IGNORE)
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onEntityTeleportEndGateway(final @NotNull EntityTeleportEndGatewayEvent event) {
        if (event.getEntity() instanceof final Trident trident) {
            final Configuration.TeleportSpecification teleportSpecification = configuration.getPortal().getEndGateway();
            if (teleportSpecification == Configuration.TeleportSpecification.IGNORE)
                event.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")  // org.bukkit.event.player.PlayerPickupArrowEvent#setCancelled is not deprecated actually
    private void onPlayerPickupArrow(final @NotNull PlayerPickupArrowEvent event) {
        if (event.getArrow() instanceof final Trident trident)
            if (trident.getShooter() instanceof final Player player) {
                final PersistentDataContainer tridentPersistentDataContainer;
                final NamespacedKey handKey;
                final Hand hand;
                final EquipmentSlot equipmentSlot;
                final PlayerInventory playerInventory;
                if (!player.equals(event.getPlayer()))
                    return;
                tridentPersistentDataContainer = trident.getPersistentDataContainer();
                handKey = Hand.getKey();
                if (!tridentPersistentDataContainer.has(handKey, PrimitivePersistentDataType.BYTE))
                    return;
                hand = Hand.of(tridentPersistentDataContainer.get(handKey, PrimitivePersistentDataType.BYTE));
                if (hand == null)
                    return;
                if (trident.getLoyaltyLevel() > 0) {
                    if (hand == Hand.MAIN_HAND && !configuration.getLoyalty().getToMainHand())
                        return;
                    else if (hand == Hand.OFF_HAND && !configuration.getLoyalty().getToOffHand())
                        return;
                }
                else {
                    if (hand == Hand.MAIN_HAND && !configuration.getPickup().getToMainHand())
                        return;
                    else if (hand == Hand.OFF_HAND && !configuration.getPickup().getToOffHand())
                        return;
                }
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
                final double belowWorldHeight;
                if (tridentItem.equals(playerInventory.getItemInMainHand()))
                    trident.getPersistentDataContainer().set(Hand.getKey(), PrimitivePersistentDataType.BYTE, Hand.MAIN_HAND.getValue());
                else if (tridentItem.equals(playerInventory.getItemInOffHand()))
                    trident.getPersistentDataContainer().set(Hand.getKey(), PrimitivePersistentDataType.BYTE, Hand.OFF_HAND.getValue());
                else
                    return;
                if (trident.getLoyaltyLevel() <= 0 || !configuration.getLoyalty().getCollideBelowWorldBoundary())
                    return;
                belowWorldHeight = trident.getWorld().getMinHeight() - 64;
                trident.getScheduler().runAtFixedRate(
                    this,
                    (task) -> {
                        if (trident.getY() + trident.getVelocity().getY() <= belowWorldHeight) {
                            trident.setHasDealtDamage(true);
                            task.cancel();
                        }
                    },
                    null,
                    1,
                    1
                );
            }
    }

    // MARK: JavaPlugin

    @Override
    public void onLoad() {
        super.onLoad();
        sharedPlugin = this;
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        final Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        configuration = new Configuration(this);
        randomGenerator = RandomGenerator.of("Random");
        getServer().getPluginManager().registerEvents(this, this);
        addCustomChartsToMetrics(metrics);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        configuration = null;
        randomGenerator = null;
    }
}
