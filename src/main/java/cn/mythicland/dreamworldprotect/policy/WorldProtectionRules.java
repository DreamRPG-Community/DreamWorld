package cn.mythicland.dreamworldprotect.policy;

import cn.mythicland.dreamlib.policy.ListMode;
import cn.mythicland.dreamlib.policy.ListPolicy;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable protection policy for one Bukkit world.
 */
public final class WorldProtectionRules {

    private final boolean blockPlace;
    private final boolean blockBreak;
    private final ListPolicy<Integer> interactionPolicy;
    private final boolean bucket;
    private final boolean leafDecay;
    private final boolean blockFade;
    private final boolean blockIgnite;
    private final boolean fireSpread;
    private final boolean explosions;
    private final boolean fallDamage;
    private final boolean enderPearl;
    private final boolean naturalMobSpawning;
    private final ListPolicy<String> commandPolicy;

    private WorldProtectionRules(Builder builder) {
        this.blockPlace = builder.blockPlace;
        this.blockBreak = builder.blockBreak;
        this.interactionPolicy = Objects.requireNonNull(builder.interactionPolicy, "interactionPolicy");
        this.bucket = builder.bucket;
        this.leafDecay = builder.leafDecay;
        this.blockFade = builder.blockFade;
        this.blockIgnite = builder.blockIgnite;
        this.fireSpread = builder.fireSpread;
        this.explosions = builder.explosions;
        this.fallDamage = builder.fallDamage;
        this.enderPearl = builder.enderPearl;
        this.naturalMobSpawning = builder.naturalMobSpawning;
        Set<String> normalizedCommands = builder.commandPolicy.entries().stream()
                .map(command -> command.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.commandPolicy = new ListPolicy<>(builder.commandPolicy.mode(), normalizedCommands);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean blockPlace() {
        return blockPlace;
    }

    public boolean blockBreak() {
        return blockBreak;
    }

    public boolean interact() {
        return interactionPolicy.mode() != ListMode.DISABLED;
    }

    public ListPolicy<Integer> interactionPolicy() {
        return interactionPolicy;
    }

    public boolean blocksInteraction(int blockId) {
        return interactionPolicy.blocks(blockId);
    }

    public boolean bucket() {
        return bucket;
    }

    public boolean leafDecay() {
        return leafDecay;
    }

    public boolean blockFade() {
        return blockFade;
    }

    public boolean blockIgnite() {
        return blockIgnite;
    }

    public boolean fireSpread() {
        return fireSpread;
    }

    public boolean explosions() {
        return explosions;
    }

    public boolean fallDamage() {
        return fallDamage;
    }

    public boolean enderPearl() {
        return enderPearl;
    }

    /**
     * Returns whether natural and chunk-generation creature spawning is protected.
     *
     * @return true when natural creature spawning should be blocked
     */
    public boolean naturalMobSpawning() {
        return naturalMobSpawning;
    }

    public boolean commandProtection() {
        return commandPolicy.mode() != ListMode.DISABLED;
    }

    public ListPolicy<String> commandPolicy() {
        return commandPolicy;
    }

    public boolean blocksCommand(String command) {
        if (command == null || command.isBlank()) return false;
        if (command.startsWith("/")) command = command.substring(1);
        return commandPolicy.blocks(command.toLowerCase(Locale.ROOT));
    }

    /**
     * Named construction API for the policy's many independent switches.
     */
    public static final class Builder {

        private boolean blockPlace;
        private boolean blockBreak;
        private ListPolicy<Integer> interactionPolicy = new ListPolicy<>(
                ListMode.WHITELIST,
                Set.of()
        );
        private boolean bucket;
        private boolean leafDecay;
        private boolean blockFade;
        private boolean blockIgnite;
        private boolean fireSpread;
        private boolean explosions;
        private boolean fallDamage;
        private boolean enderPearl;
        private boolean naturalMobSpawning;
        private ListPolicy<String> commandPolicy = new ListPolicy<>(
                ListMode.BLACKLIST,
                Set.of()
        );

        public Builder blockPlace(boolean value) {
            blockPlace = value;
            return this;
        }

        public Builder blockBreak(boolean value) {
            blockBreak = value;
            return this;
        }

        public Builder interactionPolicy(ListPolicy<Integer> value) {
            interactionPolicy = Objects.requireNonNull(value, "interactionPolicy");
            return this;
        }

        public Builder bucket(boolean value) {
            bucket = value;
            return this;
        }

        public Builder leafDecay(boolean value) {
            leafDecay = value;
            return this;
        }

        public Builder blockFade(boolean value) {
            blockFade = value;
            return this;
        }

        public Builder blockIgnite(boolean value) {
            blockIgnite = value;
            return this;
        }

        public Builder fireSpread(boolean value) {
            fireSpread = value;
            return this;
        }

        public Builder explosions(boolean value) {
            explosions = value;
            return this;
        }

        public Builder fallDamage(boolean value) {
            fallDamage = value;
            return this;
        }

        public Builder enderPearl(boolean value) {
            enderPearl = value;
            return this;
        }

        public Builder naturalMobSpawning(boolean value) {
            naturalMobSpawning = value;
            return this;
        }

        public Builder commandPolicy(ListPolicy<String> value) {
            commandPolicy = Objects.requireNonNull(value, "commandPolicy");
            return this;
        }

        public WorldProtectionRules build() {
            return new WorldProtectionRules(this);
        }
    }
}
