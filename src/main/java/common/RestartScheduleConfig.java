package common;

import java.lang.reflect.Array;
import java.util.*;

public final class RestartScheduleConfig {

    public static final List<Integer> DEFAULT_DELAYS = Collections.unmodifiableList(
            Arrays.asList(120, 60, 30, 10, 5, 4, 3, 2, 1)
    );

    public static final String DEFAULT_MESSAGE = "&e[AutoUpdateGeyser] Server will restart in {seconds} seconds.";

    public static final class BroadcastStep {
        private final long delaySeconds;
        private final int remainingSeconds;
        private final String message;

        public BroadcastStep(long delaySeconds, int remainingSeconds, String message) {
            this.delaySeconds = delaySeconds;
            this.remainingSeconds = remainingSeconds;
            this.message = message;
        }

        public long getDelaySeconds() {
            return delaySeconds;
        }

        public int getRemainingSeconds() {
            return remainingSeconds;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "BroadcastStep{delaySeconds=" + delaySeconds +
                    ", remainingSeconds=" + remainingSeconds +
                    ", message='" + message + "'}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BroadcastStep that = (BroadcastStep) o;
            return delaySeconds == that.delaySeconds &&
                    remainingSeconds == that.remainingSeconds &&
                    Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delaySeconds, remainingSeconds, message);
        }
    }

    private final boolean immediate;
    private final List<Integer> countdownSeconds;
    private final long maxDelaySeconds;
    private final String messageTemplate;
    private final List<BroadcastStep> broadcastSteps;

    public RestartScheduleConfig(boolean immediate,
                                 List<Integer> countdownSeconds,
                                 long maxDelaySeconds,
                                 String messageTemplate,
                                 List<BroadcastStep> broadcastSteps) {
        this.immediate = immediate;
        this.countdownSeconds = Collections.unmodifiableList(new ArrayList<>(countdownSeconds));
        this.maxDelaySeconds = maxDelaySeconds;
        this.messageTemplate = messageTemplate;
        this.broadcastSteps = Collections.unmodifiableList(new ArrayList<>(broadcastSteps));
    }

    public boolean isImmediate() {
        return immediate;
    }

    public List<Integer> getCountdownSeconds() {
        return countdownSeconds;
    }

    public long getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public List<BroadcastStep> getBroadcastSteps() {
        return broadcastSteps;
    }

    public static String formatMessage(String template, int seconds) {
        if (template == null || template.isEmpty()) {
            template = DEFAULT_MESSAGE;
        }
        return template.replace("{seconds}", String.valueOf(seconds));
    }

    /**
     * Parses and normalizes rawDelay and rawMessage according to specification.
     *
     * @param rawDelay   Object representing delay (Number, List/Collection, Array, String, or null)
     * @param rawMessage Message template containing {seconds}
     * @return RestartScheduleConfig
     */
    public static RestartScheduleConfig parse(Object rawDelay, String rawMessage) {
        String template = (rawMessage == null || rawMessage.trim().isEmpty()) ? DEFAULT_MESSAGE : rawMessage;

        List<Integer> rawNumbers = extractIntegers(rawDelay);

        // Fallback to default delays if null or empty
        if (rawNumbers == null || rawNumbers.isEmpty()) {
            return createCountdownConfig(DEFAULT_DELAYS, template);
        }

        // Check if there are any positive integers (> 0)
        boolean hasPositive = false;
        for (int val : rawNumbers) {
            if (val > 0) {
                hasPositive = true;
                break;
            }
        }

        if (!hasPositive) {
            // Immediate restart mode: value 0 (or only 0 / negative numbers)
            String msg = formatMessage(template, 0);
            BroadcastStep step = new BroadcastStep(0L, 0, msg);
            return new RestartScheduleConfig(
                    true,
                    Collections.singletonList(0),
                    0L,
                    template,
                    Collections.singletonList(step)
            );
        }

        // Filter out <= 0, deduplicate, and sort descending
        Set<Integer> uniquePositives = new HashSet<>();
        for (int val : rawNumbers) {
            if (val > 0) {
                uniquePositives.add(val);
            }
        }

        List<Integer> normalized = new ArrayList<>(uniquePositives);
        normalized.sort(Collections.reverseOrder());

        return createCountdownConfig(normalized, template);
    }

    private static RestartScheduleConfig createCountdownConfig(List<Integer> countdownSeconds, String template) {
        long tMax = countdownSeconds.get(0);
        List<BroadcastStep> steps = new ArrayList<>();
        for (int s : countdownSeconds) {
            long delay = tMax - s;
            String msg = formatMessage(template, s);
            steps.add(new BroadcastStep(delay, s, msg));
        }

        return new RestartScheduleConfig(
                false,
                countdownSeconds,
                tMax,
                template,
                steps
        );
    }

    private static List<Integer> extractIntegers(Object raw) {
        if (raw == null) {
            return null;
        }
        List<Integer> result = new ArrayList<>();
        if (raw instanceof Number) {
            result.add(((Number) raw).intValue());
            return result;
        }
        if (raw instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) raw) {
                Integer val = toInteger(item);
                if (val != null) {
                    result.add(val);
                }
            }
            return result;
        }
        if (raw.getClass().isArray()) {
            int len = Array.getLength(raw);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(raw, i);
                Integer val = toInteger(item);
                if (val != null) {
                    result.add(val);
                }
            }
            return result;
        }
        if (raw instanceof String) {
            String str = ((String) raw).trim();
            if (str.isEmpty()) {
                return null;
            }
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1).trim();
            }
            if (str.isEmpty()) {
                return Collections.emptyList();
            }
            String[] parts = str.split(",");
            for (String part : parts) {
                Integer val = toInteger(part.trim());
                if (val != null) {
                    result.add(val);
                }
            }
            return result;
        }
        return result;
    }

    private static Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
