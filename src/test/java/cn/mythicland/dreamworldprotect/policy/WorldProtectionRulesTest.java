package cn.mythicland.dreamworldprotect.policy;

import cn.mythicland.framework.policy.ListMode;
import cn.mythicland.framework.policy.ListPolicy;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldProtectionRulesTest {

    @Test
    void commandMatchingIsCaseInsensitive() {
        WorldProtectionRules rules = rules(Set.of("Tell"));

        assertTrue(rules.blocksCommand("tell"));
        assertTrue(rules.blocksCommand("TELL"));
        assertFalse(rules.blocksCommand("say"));
    }

    @Test
    void allowedInteractionIdsRemainAvailableWhenInteractionProtectionIsEnabled() {
        WorldProtectionRules rules = rules(Set.of("54"));

        assertFalse(rules.blocksInteraction(54));
        assertTrue(rules.blocksInteraction(58));
    }

    @Test
    void naturalMobSpawningProtectionCanBeEnabledIndependently() {
        WorldProtectionRules rules = WorldProtectionRules.builder()
                .naturalMobSpawning(true)
                .build();

        assertTrue(rules.naturalMobSpawning());
    }

    private WorldProtectionRules rules(Set<String> blockedCommands) {
        return WorldProtectionRules.builder()
                .blockPlace(true)
                .blockBreak(true)
                .interactionPolicy(new ListPolicy<>(ListMode.WHITELIST, Set.of(54)))
                .bucket(true)
                .leafDecay(true)
                .blockFade(true)
                .blockIgnite(true)
                .fireSpread(true)
                .explosions(true)
                .fallDamage(true)
                .enderPearl(false)
                .commandPolicy(new ListPolicy<>(ListMode.BLACKLIST, blockedCommands))
                .build();
    }
}
