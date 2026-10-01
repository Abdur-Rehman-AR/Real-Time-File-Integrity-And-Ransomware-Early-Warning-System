import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// This class FileWatcher continuously monitor protected folder in the background for 
// real-time file changes and handle authorization.
public class FileWatcher {

    // Making a class level list to store files that are authorized to be
    // changed. ConcurrentHashMap.newKeySet() provides the thread safety.
    public static Set<Path> approvedFiles = ConcurrentHashMap.newKeySet();

    // If the OS sends multiple events very quickly for the same file, we can check
    // the time and ignore duplicate events occurring within 500 ms.

    // Map to track the timestamp of the last processed event for each file
    private static final Map<Path, Long> lastProcessedTime = new ConcurrentHashMap<>();

    // 0.5-second waiting period to ignore duplicate events happening very quickly
    private static final long DEBOUNCE_WINDOW_MS = 500;

    // Method that actually watches the protected folder for changes
    public static void startWatching(Path path, List<FileRecord> baseLineRecords, Path backupFolderPath) {

        // Creating a thread to run the code in background
        Thread thread = new Thread(

                // Thread constructor requires a Runnable object. When you call thread.start(),
                // Java looks inside that Runnable object and executes its .run() method.
                () -> {

                    // FileSystems gives you access to the computer's file system. getDefault() gets
                    // your OS's default file system.
                    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

                        // It tells Java to connect path to watcher and create alert whenever a file
                        // inside this path is created, modified, or deleted.
                        path.register(
                                watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_DELETE);

                        while (true) {

                            // take() pauses your program and waits for a file change in watched folder.
                            WatchKey key = watchService.take();

                            // WatchEvent<?> is a java data type for a single event.
                            WatchEvent.Kind<?> kind;
                            Path fileName;

                            // pollEvents() opens key variable and extracts all the individual file events
                            // stored inside it as a list (key).
                            for (WatchEvent<?> event : key.pollEvents()) {

                                kind = event.kind();

                                // context() gives the relative path/name of the file that caused the event.
                                fileName = (Path) event.context();

                                // Getting the full path of the file
                                Path protectedFilePath = path.resolve(fileName).toAbsolutePath().normalize();

                                // Asks Java for the exact current time in milliseconds right at this moment.
                                long currentTime = System.currentTimeMillis();
                                // Checks your lastProcessedTime Map to find out when this specific file was
                                // last processed.
                                long lastTime = lastProcessedTime.getOrDefault(protectedFilePath, 0L);

                                // If the event is a duplicate within 500ms, ignore it
                                if ((currentTime - lastTime) < DEBOUNCE_WINDOW_MS) {
                                    continue;
                                }

                                // Record the time of this processed event
                                lastProcessedTime.put(protectedFilePath, currentTime);

                                // variable to show change authorized or unauthorized
                                String status;
                                // Take the Backup folder path + take the name of the protected file and then
                                // combine them
                                Path backupFilePath = backupFolderPath.resolve(protectedFilePath.getFileName());

                                // Applying logic on the basis of event

                                // First of all what happens if we do creation of a file in the protected folder
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                                    if (approvedFiles.contains(protectedFilePath)) {
                                        status = "Authorized";
                                        System.out.println("SUCCESS: Authorized file created: " + fileName);

                                        // 1. Calculate hash for new file
                                        String fileHash = IntegrityChecker.calculateHash(protectedFilePath);

                                        // 2. Add new record to in-memory baseline
                                        baseLineRecords.add(new FileRecord(protectedFilePath.toString(),
                                                protectedFilePath.getFileName().toString(),
                                                Files.size(protectedFilePath),
                                                Files.getLastModifiedTime(protectedFilePath).toMillis(), fileHash));

                                        // 3. Save updated baseline to JSON
                                        BaselineManager.putInfoInBaseline(baseLineRecords);

                                        // 4. Create initial copy in Backup folder
                                        BackupManager.updateBackup(protectedFilePath, backupFilePath, approvedFiles);

                                    } else {
                                        status = "Unauthorized Creation";
                                        System.out.println("ALERT: Unauthorized file creation detected: " + fileName
                                                + ". Deleting file...");

                                        try {
                                            // Delete the unauthorized intruder file
                                            Files.deleteIfExists(protectedFilePath);
                                        } catch (IOException e) {
                                            System.out.println("Failed to delete unauthorized file: " + e.getMessage());
                                        }
                                    }

                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                                    // If Authorized modification then:
                                    if (approvedFiles.contains(protectedFilePath)) {

                                        status = "Authorized";

                                        // Calculate the new hash
                                        String newHash = IntegrityChecker.calculateHash(protectedFilePath);

                                        // Find the file in the baseline and update its hash
                                        for (FileRecord record : baseLineRecords) {

                                            if (Paths.get(record.filePath).toAbsolutePath().normalize()
                                                    .equals(protectedFilePath)) {
                                                record.hash = newHash;
                                                break;
                                            }
                                        }

                                        // Save the updated baseline to baseline.json
                                        BaselineManager.putInfoInBaseline(baseLineRecords);

                                        // Update the backup with the newly modified file
                                        BackupManager.updateBackup(
                                                protectedFilePath,
                                                backupFilePath,
                                                approvedFiles);
                                    } else {
                                        // Unauthorized modification
                                        status = "Unauthorized";

                                        System.out.println("ALERT: Tampering detected on " + fileName
                                                + ". Restoring from Backup...");

                                        // Restore the trusted backup copy
                                        BackupManager.restoreBackup(protectedFilePath, backupFilePath, approvedFiles);
                                    }

                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                                    // Optional: Handle deletion (restore file from backup if unauthorized)
                                }

                                // displaying the info
                                System.out.println("File name: " + fileName);
                                System.out.println("Event: " + kind);
                                System.out.println("Time: " + LocalDateTime.now());
                                System.out.println("Change: " + status);
                            }

                            // It tells java that i finished processing the events. You can watch for
                            // new events again.
                            key.reset();
                        }

                    } catch (InterruptedException e) {
                        // Mark the currently running thread as interrupted.
                        Thread.currentThread().interrupt();

                    } catch (IOException e) {
                        System.out.println("Error while monitoring folder: " + e.getMessage());
                    }
                });

        // It will start running the thread
        thread.start();
    }
}