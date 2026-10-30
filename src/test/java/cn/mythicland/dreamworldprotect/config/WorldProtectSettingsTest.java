package cn.mythicland.dreamworldprotect.config;

import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldProtectSettingsTest {

    @Test
    void builderUsesNamedFieldsAndExposesImmutableSettings() {
        WorldProtectionRules defaults = WorldProtectionRules.builder()
                .blockPlace(true)
                .build();
        WorldProtectSettings settings = WorldProtectSettings.builder()
                .worldConfigDirectory("worlds")
                .editPermission("dreamworld.protect.edit")
                .worldDefaults(defaults)
                .build();

        assertEquals("worlds", settings.worldConfigDirectory());
        assertEquals("dreamworld.protect.edit", settings.editPermission());
        assertEquals(defaults, settings.worldDefaults());
    }

    @Test
    void builderRejectsMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> WorldProtectSettings.builder().build());
    }
}
