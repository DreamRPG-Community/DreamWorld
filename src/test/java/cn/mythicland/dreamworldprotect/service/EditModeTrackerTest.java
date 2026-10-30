package cn.mythicland.dreamworldprotect.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditModeTrackerTest {

    @Test
    void togglesEachPlayerIndependently() {
        EditModeTracker tracker = new EditModeTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(tracker.toggle(first));
        assertTrue(tracker.isEnabled(first));
        assertFalse(tracker.isEnabled(second));
        assertFalse(tracker.toggle(first));
        assertFalse(tracker.isEnabled(first));
    }

    @Test
    void removesPlayerStateAndClearsAllState() {
        EditModeTracker tracker = new EditModeTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.toggle(first);
        tracker.toggle(second);

        tracker.remove(first);
        assertFalse(tracker.isEnabled(first));
        assertTrue(tracker.isEnabled(second));

        tracker.clear();
        assertFalse(tracker.isEnabled(second));
    }
}
