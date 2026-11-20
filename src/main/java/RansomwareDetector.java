import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

// This class detects the ransomware attack.
public class RansomwareDetector {

    /**
     * This line creates a fixed set of suspicious file extensions that our
     * ransomware detection engine will check.
     * Set.of() creates a fixed and unmodifiable set of extensions.
     */
    private static final Set<String> suspiciousExtentions = Set.of(".locked", ".crypt", ".crypto", ".enc", ".encrypted",
            ".wannacry", ".wcry", ".ryk", ".lockbit", ".cerber", ".zepto");

    // --- Configuration Thresholds ---
    // 5-second sliding window
    private static final Duration TIME_WINDOW = Duration.ofSeconds(5);
    // Max allowed modifications in 5s
    private static final int MODIFICATION_THRESHOLD = 10;
    // Max allowed deletions in 5s
    private static final int DELETION_THRESHOLD = 10;

    // It creates an empty, thread-safe collection to store file modification
    // timestamps, so our ransomware detector can track how many files are modified
    // within 5 seconds.
    private static final Deque<Instant> modificationTimestamps = new ConcurrentLinkedDeque<>();

    // Stores timestamps of recent file deletions to detect rapid mass deletion
    private static final Deque<Instant> deletionTimestamps = new ConcurrentLinkedDeque<>();

    // 1. This method checks either the file has a suspicious extention or not
    public static boolean hasSuspiciousExtension(Path filePath) {

        // checks if file does not exists
        if (filePath == null || filePath.getFileName() == null) {
            return false;
        }

        // Convert filename to lowercase so ".LOCKED" and ".locked" both match
        String fileName = filePath.getFileName().toString().toLowerCase();

        // loop that iterate over each suspiciousExtentions to match it with selected
        // file extension
        for (String ext : suspiciousExtentions) {
            if (fileName.endsWith(ext)) {
                System.err.println("RANSOMWARE ALERT [Suspicious Extension]: " + fileName + " matched '" + ext + "'");
                return true;
            }
        }
        return false;
    }

    // Shared helper method used by all type of records so we don't repeat the
    // 5-second window logic
    private static boolean checkRateLimit(Deque<Instant> timestamps, int threshold, String alertType) {

        // records the current time
        Instant now = Instant.now();

        // Add the current modification time at the end of the Deque.
        timestamps.addLast(now);

        // Remove timestamps older than the 5-second window so that we can calculate
        // that how many modifications made in 5 sec.
        // between() calculates the time difference between 2 times.
        while (!timestamps.isEmpty()
                && Duration.between(timestamps.peekFirst(), now).compareTo(TIME_WINDOW) > 0) {

            // pollFirst() removes and returns the oldest item from the Deque.
            timestamps.pollFirst();
        }

        // Check if current window size exceeds threshold
        if (timestamps.size() > threshold) {
            System.err.println("RANSOMWARE ALERT [" + alertType + "]: " + timestamps.size()
                    + " files within " + TIME_WINDOW.getSeconds() + " seconds!");
            return true;
        }
        return false;
    }

    // 2. This method detects whether too many files modifications happen within 5
    // seconds, which may indicate ransomware activity.
    public static boolean recordModification() {
        return checkRateLimit(modificationTimestamps, MODIFICATION_THRESHOLD, "Rapid Modifications");
    }

    // 3. This method detects whether too many file deletions happen within 5
    // seconds, which may indicate ransomware wiping original files.
    public static boolean recordDeletion() {
        return checkRateLimit(deletionTimestamps, DELETION_THRESHOLD, "Rapid Deletions");
    }
}