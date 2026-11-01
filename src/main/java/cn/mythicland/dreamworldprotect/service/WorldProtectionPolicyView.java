package cn.mythicland.dreamworldprotect.service;

import cn.mythicland.framework.policy.ListPolicy;
import cn.mythicland.dreamworldprotect.api.WorldProtectionPolicy;
import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;

/**
 * Adds the logical world name to the internal immutable protection rules.
 */
public final class WorldProtectionPolicyView implements WorldProtectionPolicy {

    private final String logicalName;
    private final WorldProtectionRules rules;

    public WorldProtectionPolicyView(String logicalName, WorldProtectionRules rules) {
        this.logicalName = logicalName;
        this.rules = rules;
    }

    @Override
    public String logicalName() {
        return logicalName;
    }

    @Override
    public boolean blockPlace() {
        return rules.blockPlace();
    }

    @Override
    public boolean blockBreak() {
        return rules.blockBreak();
    }

    @Override
    public boolean interact() {
        return rules.interact();
    }

    @Override
    public ListPolicy<Integer> interactionPolicy() {
        return rules.interactionPolicy();
    }

    @Override
    public boolean bucket() {
        return rules.bucket();
    }

    @Override
    public boolean leafDecay() {
        return rules.leafDecay();
    }

    @Override
    public boolean blockFade() {
        return rules.blockFade();
    }

    @Override
    public boolean blockIgnite() {
        return rules.blockIgnite();
    }

    @Override
    public boolean fireSpread() {
        return rules.fireSpread();
    }

    @Override
    public boolean explosions() {
        return rules.explosions();
    }

    @Override
    public boolean fallDamage() {
        return rules.fallDamage();
    }

    @Override
    public boolean enderPearl() {
        return rules.enderPearl();
    }

    @Override
    public boolean naturalMobSpawning() {
        return rules.naturalMobSpawning();
    }

    @Override
    public boolean commandProtection() {
        return rules.commandProtection();
    }

    @Override
    public ListPolicy<String> commandPolicy() {
        return rules.commandPolicy();
    }

    @Override
    public boolean blocksCommand(String command) {
        return rules.blocksCommand(command);
    }
}
