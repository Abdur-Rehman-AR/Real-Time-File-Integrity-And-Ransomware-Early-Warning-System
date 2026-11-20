import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

// This class displays unauthorized activity warnings in the console.
// Saving alerts to alerts.log.
public class AlertManager {

        // Location of the alert file.
        private static final Path alertFilePath = Paths.get("alerts.log").toAbsolutePath().normalize();

        // This method generates an alert for unauthorized activity.
        // synchronized means only 1 thread can execute this method at a time.
        public static synchronized void generateAlert(String eventType, Path filePath, String action) {

                // Stores the current date and time in timestamp variable
                String timestamp = LocalDateTime.now().toString();

                // Making of the alert message
                String alertMessage = "[" + timestamp + "] UNAUTHORIZED " + eventType
                                + " | File: " + filePath + " | Action: " + action;

                // Display warning in the console.
                System.out.println("ALERT: " + alertMessage);

                // Save the alert in alerts.log.
                try {
                        Files.writeString(
                                        alertFilePath,
                                        // Writes the alert message and then moves to a new line, so the next alert
                                        // appears on a separate line.
                                        alertMessage + System.lineSeparator(),
                                        StandardCharsets.UTF_8,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND);

                } catch (IOException e) {
                        System.out.println("Failed to save alert: " + e.getMessage());
                }
        }
}