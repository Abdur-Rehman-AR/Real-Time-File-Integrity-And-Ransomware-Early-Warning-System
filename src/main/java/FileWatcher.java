import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// This class FileWatcher continuously monitor protected folder in the background for 
// real-time file changes and handle authorization.
public class FileWatcher {

    // Making a class level list to store files that are authorized to be
    // changed. ConcurrentHashMap.newKeySet() provides the thread safety.
    public static Set<Path> approvedFiles = ConcurrentHashMap.newKeySet();

    // List of files that the system itself deleted intentionally, so when
    // WatchService reports their ENTRY_DELETE, we can ignore that event.
    public static Set<Path> systemDeletedFiles = ConcurrentHashMap.newKeySet();

    // List of files that system intentionally restored. It will tell the watcher
    // that this CREATE came from our restore, not from a user creating a new file.
    public static Set<Path> systemRestoredFiles = ConcurrentHashMap.newKeySet();

    // It keeps a single helper thread ready in the background. When you tell it to
    // run a task after a delay like 500 ms, that background thread waits for the
    // timer to finish, runs your task, and then goes back to waiting.
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
                                String status = "Unknown";
                                // Take the Backup folder path + take the name of the protected file and then
                                // combine them
                                Path backupFilePath = backupFolderPath.resolve(protectedFilePath.getFileName());

                                // Applying logic on the basis of event

                                // First of all what happens if we do creation of a file in the protected folder
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                                    if (systemRestoredFiles.contains(protectedFilePath)) {
                                        status = "System Restore";
                                        System.out.println(
                                                "Ignoring CREATE event caused by file restoration: " + fileName);
                                    } else if (approvedFiles.contains(protectedFilePath)) {
                                        status = "Authorized";
                                        System.out.println("SUCCESS: Authorized file created: " + fileName);

                                        // 1. Calculate hash for new file
                                        String fileHash = IntegrityChecker.calculateHash(protectedFilePath);

                                        // Adding the file creation log in log file.
                                        AuditLogger.logEvent(
                                                protectedFilePath.toString(),
                                                "CREATE",
                                                null,
                                                fileHash);

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
                                            // Adding the file in list, so that delete event can know it is intentional
                                            // deleteion
                                            systemDeletedFiles.add(protectedFilePath);
                                            // Delete the unauthorized file
                                            Files.deleteIfExists(protectedFilePath);
                                        } catch (IOException e) {
                                            systemDeletedFiles.remove(protectedFilePath);
                                            System.out.println("Failed to delete unauthorized file: " + e.getMessage());
                                        }
                                    }

                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                                    // It ignores the file-change event that our own program creates while restoring
                                    // a file.
                                    if (systemRestoredFiles.contains(protectedFilePath)) {

                                        status = "System Restore";
                                        System.out.println(
                                                "Ignoring MODIFY event caused by file restoration: " + fileName);
                                    }
                                    // If Authorized modification then:
                                    else if (approvedFiles.contains(protectedFilePath)) {

                                        status = "Authorized";

                                        // Calculate the new hash
                                        String newHash = IntegrityChecker.calculateHash(protectedFilePath);

                                        // Go through all the files stored in the baseline and find the record for the
                                        // file that was just changed.
                                        for (FileRecord record : baseLineRecords) {

                                            // It is checking does this baseline record belong to the file that
                                            // WatchService just detected.
                                            if (Paths.get(record.filePath).toAbsolutePath().normalize()
                                                    .equals(protectedFilePath)) {

                                                String oldHash = record.hash;

                                                // Check either file changed or not
                                                if (newHash.equals(oldHash)) {
                                                    status = "No Content Change";
                                                    System.out.println("File content is unchanged: " + fileName);

                                                } else if (approvedFiles.contains(protectedFilePath)) {

                                                    status = "Authorized";

                                                    // Adding the creation modification log in log file
                                                    AuditLogger.logEvent(
                                                            protectedFilePath.toString(),
                                                            "MODIFY",
                                                            oldHash,
                                                            newHash);

                                                    record.hash = newHash;
                                                    BaselineManager.putInfoInBaseline(baseLineRecords);
                                                    BackupManager.updateBackup(protectedFilePath, backupFilePath,
                                                            approvedFiles);
                                                } else {
                                                    status = "Unauthorized";
                                                    System.out.println("ALERT: Tampering detected on " + fileName
                                                            + ". Restoring from Backup...");
                                                    systemRestoredFiles.add(protectedFilePath);
                                                    BackupManager.restoreBackup(protectedFilePath, backupFilePath);

                                                    scheduler.schedule(
                                                            () -> systemRestoredFiles.remove(protectedFilePath),
                                                            2,
                                                            TimeUnit.SECONDS);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                                    if (systemDeletedFiles.remove(protectedFilePath)) {
                                        status = "System Cleanup";
                                        System.out
                                                .println("Ignoring DELETE event caused by system cleanup: " + fileName);
                                    }
                                    // Authorized deletion
                                    else if (approvedFiles.contains(protectedFilePath)) {

                                        status = "Authorized";

                                        System.out.println("SUCCESS: Authorized file deleted: " + fileName);

                                        String oldHash = null;

                                        for (FileRecord record : baseLineRecords) {
                                            if (Paths.get(record.filePath).toAbsolutePath().normalize()
                                                    .equals(protectedFilePath)) {
                                                oldHash = record.hash;
                                                break;
                                            }
                                        }

                                        // Creating a log in log file
                                        AuditLogger.logEvent(
                                                protectedFilePath.toString(),
                                                "DELETE",
                                                oldHash,
                                                null);

                                        // Remove the file from the baseline
                                        baseLineRecords.removeIf(record -> Paths.get(record.filePath)
                                                .toAbsolutePath().normalize().equals(protectedFilePath));

                                        // Save the updated baseline
                                        BaselineManager.putInfoInBaseline(baseLineRecords);

                                        // Delete the backup copy
                                        try {
                                            Files.deleteIfExists(backupFilePath);
                                            System.out.println("Backup copy deleted: " + fileName);
                                        } catch (IOException e) {
                                            System.out.println("Failed to delete backup: " + e.getMessage());
                                        }

                                        // Remove temporary approval
                                        approvedFiles.remove(protectedFilePath);

                                    } else {
                                        // Unauthorized deletion
                                        status = "Unauthorized";

                                        System.out.println("ALERT: Unauthorized deletion detected on " + fileName
                                                + ". Restoring from Backup...");
                                        // Adding file so that create event can see this creation is done by system
                                        // itself
                                        systemRestoredFiles.add(protectedFilePath);
                                        // Restore the deleted file from backup
                                        BackupManager.restoreBackup(protectedFilePath, backupFilePath);

                                        // It tells your background helper thread: "Wait 2 seconds and then remove this
                                        // file path from the systemRestoredFiles list.
                                        scheduler.schedule(
                                                () -> systemRestoredFiles.remove(protectedFilePath), 2,
                                                TimeUnit.SECONDS);
                                    }
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