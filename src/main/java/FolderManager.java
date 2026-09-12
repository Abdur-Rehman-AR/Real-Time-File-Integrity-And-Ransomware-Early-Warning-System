import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// This class validates whether the required project directories exist on disk before your monitoring system runs.
public class FolderManager {

    // Step 1

    // Method no 1: Locates and verifies the Protected Folder directory. Stores the
    // path of the folder that WatchService needs to monitor for changes.
    public static Path selectFolderToProtect() {
        // Selecting the folder to protect
        Path path = Paths.get("Protected Folder");

        if (Files.exists(path) && Files.isDirectory(path))
            return path;
        else
            return null;
    }

    // Method no 2: Locates and verifies the Backup directory. Stores the path where
    // clean copy versions of your files are kept for synchronization.
    public static Path selectBackupFolder() {

        Path path = Paths.get("Backup");

        if (Files.exists(path) && Files.isDirectory(path))
            return path;
        else
            return null;
    }
}