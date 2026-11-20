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

    // It creates an empty, thread-safe collection to store file modification
    // timestamps, so our ransomware detector can track how many files are modified
    // within 5 seconds.
    private static final Deque<Instant> modificationTimestamps = new ConcurrentLinkedDeque<>();

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

    // 2. This method detects whether too many files modifications happen within 5
    // seconds, which may indicate ransomware activity.

    public static boolean recordModification() {

        // records the current time
        Instant now = Instant.now();

        // Add the current modification time at the end of the Deque.
        modificationTimestamps.addLast(now);

        // Remove timestamps older than the 5-second window so that we can calculate
        // that how many modifications made in 5 sec.
        // between() calculates the time difference between 2 times.
        while (!modificationTimestamps.isEmpty()
                && Duration.between(modificationTimestamps.peekFirst(), now).compareTo(TIME_WINDOW) > 0) {

            // pollFirst() removes and returns the oldest item from the Deque.
            modificationTimestamps.pollFirst();
        }

        // Check if current window size exceeds threshold
        if (modificationTimestamps.size() > MODIFICATION_THRESHOLD) {
            System.err.println("RANSOMWARE ALERT [Rapid Modifications]: " + modificationTimestamps.size()
                    + " files modified within " + TIME_WINDOW.getSeconds() + " seconds!");
            return true;
        }
        return false;
    }
}