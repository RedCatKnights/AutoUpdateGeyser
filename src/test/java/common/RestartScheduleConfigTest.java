package common;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class RestartScheduleConfigTest {

    @Test
    public void testDefaultDelaysWhenNullOrEmpty() {
        RestartScheduleConfig configNull = RestartScheduleConfig.parse(null, null);
        assertFalse(configNull.isImmediate());
        assertEquals(RestartScheduleConfig.DEFAULT_DELAYS, configNull.getCountdownSeconds());
        assertEquals(120L, configNull.getMaxDelaySeconds());
        assertEquals(RestartScheduleConfig.DEFAULT_MESSAGE, configNull.getMessageTemplate());

        List<RestartScheduleConfig.BroadcastStep> steps = configNull.getBroadcastSteps();
        assertEquals(9, steps.size());
        assertEquals(0L, steps.get(0).getDelaySeconds());
        assertEquals(120, steps.get(0).getRemainingSeconds());
        assertEquals("&e[AutoUpdateGeyser] Server will restart in 120 seconds.", steps.get(0).getMessage());

        assertEquals(60L, steps.get(1).getDelaySeconds());
        assertEquals(60, steps.get(1).getRemainingSeconds());

        assertEquals(119L, steps.get(8).getDelaySeconds());
        assertEquals(1, steps.get(8).getRemainingSeconds());

        RestartScheduleConfig configEmpty = RestartScheduleConfig.parse(Collections.emptyList(), "");
        assertFalse(configEmpty.isImmediate());
        assertEquals(RestartScheduleConfig.DEFAULT_DELAYS, configEmpty.getCountdownSeconds());
    }

    @Test
    public void testImmediateRestartModeZero() {
        RestartScheduleConfig configZero = RestartScheduleConfig.parse(0, "&eRestarting in {seconds}s");
        assertTrue(configZero.isImmediate());
        assertEquals(0L, configZero.getMaxDelaySeconds());
        assertEquals(Collections.singletonList(0), configZero.getCountdownSeconds());
        assertEquals(1, configZero.getBroadcastSteps().size());
        assertEquals(0L, configZero.getBroadcastSteps().get(0).getDelaySeconds());
        assertEquals(0, configZero.getBroadcastSteps().get(0).getRemainingSeconds());
        assertEquals("&eRestarting in 0s", configZero.getBroadcastSteps().get(0).getMessage());

        RestartScheduleConfig configListZero = RestartScheduleConfig.parse(Collections.singletonList(0), null);
        assertTrue(configListZero.isImmediate());
        assertEquals(0L, configListZero.getMaxDelaySeconds());

        RestartScheduleConfig configNegative = RestartScheduleConfig.parse(Arrays.asList(-10, -5, 0), null);
        assertTrue(configNegative.isImmediate());
        assertEquals(0L, configNegative.getMaxDelaySeconds());
    }

    @Test
    public void testLegacySingleNumber() {
        RestartScheduleConfig config = RestartScheduleConfig.parse(60, "&aServer rebooting in {seconds} seconds.");
        assertFalse(config.isImmediate());
        assertEquals(60L, config.getMaxDelaySeconds());
        assertEquals(Collections.singletonList(60), config.getCountdownSeconds());
        assertEquals(1, config.getBroadcastSteps().size());
        assertEquals(0L, config.getBroadcastSteps().get(0).getDelaySeconds());
        assertEquals(60, config.getBroadcastSteps().get(0).getRemainingSeconds());
        assertEquals("&aServer rebooting in 60 seconds.", config.getBroadcastSteps().get(0).getMessage());
    }

    @Test
    public void testNormalizationDeduplicationAndSorting() {
        // [10, 60, 10, 0, -5, 30] -> [60, 30, 10]
        List<Integer> raw = Arrays.asList(10, 60, 10, 0, -5, 30);
        RestartScheduleConfig config = RestartScheduleConfig.parse(raw, "Restart in {seconds}");
        assertFalse(config.isImmediate());
        assertEquals(Arrays.asList(60, 30, 10), config.getCountdownSeconds());
        assertEquals(60L, config.getMaxDelaySeconds());

        List<RestartScheduleConfig.BroadcastStep> steps = config.getBroadcastSteps();
        assertEquals(3, steps.size());

        // s = 60 -> delay = 60 - 60 = 0
        assertEquals(0L, steps.get(0).getDelaySeconds());
        assertEquals(60, steps.get(0).getRemainingSeconds());
        assertEquals("Restart in 60", steps.get(0).getMessage());

        // s = 30 -> delay = 60 - 30 = 30
        assertEquals(30L, steps.get(1).getDelaySeconds());
        assertEquals(30, steps.get(1).getRemainingSeconds());
        assertEquals("Restart in 30", steps.get(1).getMessage());

        // s = 10 -> delay = 60 - 10 = 50
        assertEquals(50L, steps.get(2).getDelaySeconds());
        assertEquals(10, steps.get(2).getRemainingSeconds());
        assertEquals("Restart in 10", steps.get(2).getMessage());
    }

    @Test
    public void testStringParsing() {
        RestartScheduleConfig config = RestartScheduleConfig.parse("[120, 60, 10]", "Reboot {seconds}");
        assertFalse(config.isImmediate());
        assertEquals(Arrays.asList(120, 60, 10), config.getCountdownSeconds());
        assertEquals(120L, config.getMaxDelaySeconds());

        RestartScheduleConfig configZeroStr = RestartScheduleConfig.parse("0", "Reboot {seconds}");
        assertTrue(configZeroStr.isImmediate());
        assertEquals(0L, configZeroStr.getMaxDelaySeconds());
    }
}
