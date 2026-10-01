import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

// This class creates the backup folder files at the start of the application as
// copied from the protected folder
public class BackupManager {

    // Backup folder initialization

    public static void initializeBackupFolder(Path protectedFolder, Path backupFolder) {
        try {

            // Copy each file from Protected Folder to Backup Folder
            // DirectoryStream lets you go through the items inside a folder one by one.
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(protectedFolder)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {

                        // Creating the name of the path
                        Path newPath = backupFolder.resolve(file.getFileName());

                        // REPLACE_EXISTING means if destination already has the file, overwrite it.
                        Files.copy(file, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            System.out.println("Backup initialized successfully.");

        } catch (IOException e) {
            System.out.println("Error initializing backup: " + e.getMessage());
        }
    }

    // Method that will rewrite the protected folder file as copied from the backup
    // folder file when change is unauthorized
    public static void restoreBackup(Path protectedFilePath, Path backupFilePath, Set<Path> approvedFiles) {
        try {
            // Add to approvedFiles temporarily to prevent an infinite loop when rewriting
            // the clean file
            approvedFiles.add(protectedFilePath);
            Files.copy(backupFilePath, protectedFilePath,
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("SUCCESS: Restored " + protectedFilePath.getFileName() + " from Backup.");
        } catch (IOException e) {
            approvedFiles.remove(protectedFilePath);
            System.out.println("Failed to restore file: " + e.getMessage());
        }
    }

    // Method that will rewrite the backup folder file as copied from the protected
    // folder file when change is authorized
    public static void updateBackup(Path protectedFilePath, Path backupFilePath, Set<Path> approvedFiles) {
        try {
            // Update the Backup file content
            Files.copy(protectedFilePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup copy updated for: " + protectedFilePath.getFileName());
            approvedFiles.remove(protectedFilePath);
        } catch (IOException e) {
            approvedFiles.remove(protectedFilePath);
            System.out.println("Failed to update file: " + e.getMessage());
        }
    }
}