package cn.mythicland.dreamworldprotect.api;

import cn.mythicland.framework.policy.ListPolicy;

import java.util.Set;

/**
 * Immutable, read-only protection policy for one loaded Bukkit world.
 *
 * <p>The methods are consumed by external plugins through {@code DreamWorldProtectionApi}, so IntelliJ
 * cannot always see their call sites inside this project.</p>
 */
@SuppressWarnings("unused")
public interface WorldProtectionPolicy {

    /**
     * Returns the user-facing logical name used for this world configuration.
     *
     * @return the logical world name, or the Bukkit world name when no external world manager
     * supplied a logical name
     */
    String logicalName();

    /**
     * @return whether block placement is protected
     */
    boolean blockPlace();

    /**
     * @return whether block breaking is protected
     */
    boolean blockBreak();

    /**
     * @return whether block interaction protection is enabled
     */
    boolean interact();

    /**
     * Returns the reusable interaction block policy.
     *
     * @return the immutable blacklist or whitelist policy for legacy numeric block IDs
     */
    ListPolicy<Integer> interactionPolicy();

    /**
     * Checks whether interaction with a legacy numeric block ID is blocked.
     *
     * @param blockId the legacy numeric block ID
     * @return true when interaction should be blocked
     */
    default boolean blocksInteraction(int blockId) {
        return interact() && interactionPolicy().blocks(blockId);
    }

    /**
     * @return an immutable set of block IDs used by the configured policy
     * @deprecated Use {@link #interactionPolicy()} to inspect the selected mode and entries. The
     * returned set contains the configured block IDs.
     */
    @Deprecated
    default Set<Integer> allowedInteractBlockIds() {
        return interactionPolicy().entries();
    }

    /**
     * @return whether bucket filling and emptying are protected
     */
    boolean bucket();

    /**
     * @return whether leaf decay is protected
     */
    boolean leafDecay();

    /**
     * @return whether block fading is protected
     */
    boolean blockFade();

    /**
     * @return whether ordinary block ignition is protected
     */
    boolean blockIgnite();

    /**
     * @return whether fire spread is protected
     */
    boolean fireSpread();

    /**
     * @return whether explosions are prevented from damaging blocks
     */
    boolean explosions();

    /**
     * @return whether player fall damage is protected
     */
    boolean fallDamage();

    /**
     * @return whether ender pearl launches are protected
     */
    boolean enderPearl();

    /**
     * @return whether creature spawning from natural spawning and chunk generation is protected
     */
    boolean naturalMobSpawning();

    /**
     * @return whether configured commands are intercepted
     */
    boolean commandProtection();

    /**
     * Returns the reusable command policy.
     *
     * @return the immutable blacklist or whitelist policy for command names
     */
    ListPolicy<String> commandPolicy();

    /**
     * @return an immutable, lower-case set of command names without a leading slash
     * @deprecated Use {@link #commandPolicy()} to inspect the selected mode and entries. The
     * returned set contains the configured command names.
     */
    @Deprecated
    default Set<String> blockedCommands() {
        return commandPolicy().entries();
    }

    /**
     * Checks whether a command is included in the configured interception set.
     *
     * @param command the command name, with or without a leading slash
     * @return {@code true} when the command is configured for interception; {@code false} for
     * null, blank, or unconfigured command names
     */
    boolean blocksCommand(String command);
}
