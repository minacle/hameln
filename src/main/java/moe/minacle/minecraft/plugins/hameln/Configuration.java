package moe.minacle.minecraft.plugins.hameln;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Configuration {

    private static final @NotNull String COLLIDE_BELOW_WORLD_BOUNDARY_KEY = "collide-below-world-boundary";

    private static final @NotNull String END_GATEWAY_KEY = "end-gateway";

    private static final @NotNull String END_PORTAL_KEY = "end-portal";

    private static final @NotNull String LOYALTY_KEY = "loyalty";

    private static final @NotNull String NETHER_PORTAL_KEY = "nether-portal";

    private static final @NotNull String PICKUP_KEY = "pickup";

    private static final @NotNull String PORTAL_KEY = "portal";

    private static final @NotNull String TO_END_KEY = "to-end";

    private static final @NotNull String TO_MAIN_HAND_KEY = "to-main-hand";

    private static final @NotNull String TO_NETHER_KEY = "to-nether";

    private static final @NotNull String TO_OFF_HAND_KEY = "to-off-hand";

    private static final @NotNull String TO_OVERWORLD_KEY = "to-overworld";

    public static enum TeleportSpecification {

        DEFAULT("default"),  // null
        IGNORE("ignore");  // false

        public static @Nullable TeleportSpecification value(final @Nullable Object object) {
            if (object == null)
                return DEFAULT;
            else if (object instanceof final String stringObject)
                for (final TeleportSpecification value : values()) {
                    if (value.keyword.equals(stringObject))
                        return value;
                }
            else if (object instanceof final Boolean booleanObject)
                return booleanObject.booleanValue() ? null : IGNORE;
            return null;
        }

        private final @NotNull String keyword;

        private TeleportSpecification(final @NotNull String keyword) {
            this.keyword = keyword;
        }
    }

    public abstract class Section {

        private final @NotNull String path;

        private final @NotNull String sectionKey;

        private final @Nullable Section supersection;

        protected Section(final @NotNull String sectionKey, final @Nullable Section supersection) {
            if (supersection == null)
                path = sectionKey;
            else
                path = String.format("%s.%s", supersection.getPath(), sectionKey);
            this.sectionKey = sectionKey;
            this.supersection = supersection;
        }

        protected @NotNull String getPath() {
            return path;
        }

        protected @NotNull String getPathForKey(final @NotNull String key) {
            return String.format("%s.%s", getPath(), key);
        }

        protected @NotNull String getSectionKey() {
            return sectionKey;
        }

        protected @Nullable Section getSupersection() {
            return supersection;
        }
    }

    public final class PickupSection extends Section {

        private @Nullable Boolean toMainHand;

        private @Nullable Boolean toOffHand;

        PickupSection() {
            super(PICKUP_KEY, null);
        }

        public boolean getToMainHand() {
            if (toMainHand == null)
                toMainHand = getFileConfiguration().getBoolean(getPathForKey(TO_MAIN_HAND_KEY));
            return toMainHand;
        }

        public boolean getToOffHand() {
            if (toOffHand == null)
                toOffHand = getFileConfiguration().getBoolean(getPathForKey(TO_OFF_HAND_KEY));
            return toOffHand;
        }
    }

    public final class LoyaltySection extends Section {

        private @Nullable Boolean toMainHand;

        private @Nullable Boolean toOffHand;

        private @Nullable Boolean collideBelowWorldBoundary;

        LoyaltySection() {
            super(LOYALTY_KEY, null);
        }

        public boolean getToMainHand() {
            if (toMainHand == null)
                toMainHand = getFileConfiguration().getBoolean(getPathForKey(TO_MAIN_HAND_KEY));
            return toMainHand;
        }

        public boolean getToOffHand() {
            if (toOffHand == null)
                toOffHand = getFileConfiguration().getBoolean(getPathForKey(TO_OFF_HAND_KEY));
            return toOffHand;
        }

        public boolean getCollideBelowWorldBoundary() {
            if (collideBelowWorldBoundary == null)
                collideBelowWorldBoundary = getFileConfiguration().getBoolean(getPathForKey(COLLIDE_BELOW_WORLD_BOUNDARY_KEY));
            return collideBelowWorldBoundary;
        }
    }

    public final class PortalSection extends Section {

        public final class NetherPortalSection extends Section {

            private @Nullable TeleportSpecification toNether;

            private @Nullable TeleportSpecification toOverworld;

            NetherPortalSection(final @Nullable Section supersection) {
                super(NETHER_PORTAL_KEY, supersection);
            }

            public @NotNull TeleportSpecification getToNether() {
                if (toNether == null) {
                    toNether = TeleportSpecification.value(getFileConfiguration().get(getPathForKey(TO_NETHER_KEY)));
                    if (toNether == null)
                        toNether = TeleportSpecification.DEFAULT;
                }
                return toNether;
            }

            public @NotNull TeleportSpecification getToOverworld() {
                if (toOverworld == null) {
                    toOverworld = TeleportSpecification.value(getFileConfiguration().get(getPathForKey(TO_OVERWORLD_KEY)));
                    if (toOverworld == null)
                        toOverworld = TeleportSpecification.DEFAULT;
                }
                return toOverworld;
            }
        }

        public final class EndPortalSection extends Section {

            private @Nullable TeleportSpecification toEnd;

            private @Nullable TeleportSpecification toOverworld;

            EndPortalSection(final @Nullable Section supersection) {
                super(END_PORTAL_KEY, supersection);
            }

            public @NotNull TeleportSpecification getToEnd() {
                if (toEnd == null) {
                    toEnd = TeleportSpecification.value(getFileConfiguration().get(getPathForKey(TO_END_KEY)));
                    if (toEnd == null)
                        toEnd = TeleportSpecification.DEFAULT;
                }
                return toEnd;
            }

            public @NotNull TeleportSpecification getToOverworld() {
                if (toOverworld == null) {
                    toOverworld = TeleportSpecification.value(getFileConfiguration().get(getPathForKey(TO_OVERWORLD_KEY)));
                    if (toOverworld == null)
                        toOverworld = TeleportSpecification.DEFAULT;
                }
                return toOverworld;
            }
        }

        private @NotNull NetherPortalSection netherPortal;

        private @NotNull EndPortalSection endPortal;

        private @Nullable TeleportSpecification endGateway;

        PortalSection() {
            super(PORTAL_KEY, null);
            netherPortal = new NetherPortalSection(this);
            endPortal = new EndPortalSection(this);
        }

        public @NotNull NetherPortalSection getNetherPortal() {
            return netherPortal;
        }

        public @NotNull EndPortalSection getEndPortal() {
            return endPortal;
        }

        public @NotNull TeleportSpecification getEndGateway() {
            if (endGateway == null) {
                endGateway = TeleportSpecification.value(getFileConfiguration().get(getPathForKey(END_GATEWAY_KEY)));
                if (endGateway == null)
                    endGateway = TeleportSpecification.DEFAULT;
            }
            return endGateway;
        }
    }

    private @NotNull FileConfiguration fileConfiguration;

    private @NotNull PickupSection pickup;

    private @NotNull LoyaltySection loyalty;

    private @NotNull PortalSection portal;

    public Configuration(final @NotNull Plugin plugin) {
        fileConfiguration = plugin.getConfig();
        pickup = new PickupSection();
        loyalty = new LoyaltySection();
        portal = new PortalSection();
    }

    public @NotNull PickupSection getPickup() {
        return pickup;
    }

    public @NotNull LoyaltySection getLoyalty() {
        return loyalty;
    }

    public @NotNull PortalSection getPortal() {
        return portal;
    }

    private @NotNull FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }
}
