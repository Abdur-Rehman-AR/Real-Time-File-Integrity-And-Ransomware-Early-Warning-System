import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class AlertManager {

    // Location of the alert file.
    private static final Path ALERT_FILE_PATH =
            Paths.get("alerts.log").toAbsolutePath().normalize();

    // Generate an alert for unauthorized activity.
    public static synchronized void generateAlert(
            String eventType, Path filePath, String action) {

        String timestamp = LocalDateTime.now().toString();

        String alertMessage =
                "[" + timestamp + "] UNAUTHORIZED " + eventType
                + " | File: " + filePath
                + " | Action: " + action;

        // Display warning in the console.
        System.out.println("ALERT: " + alertMessage);

        // Save the alert in alerts.log.
        try {
            Files.writeString(
                    ALERT_FILE_PATH,
                    alertMessage + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.out.println(
                    "Failed to save alert: " + e.getMessage());
        }
    }
}
