import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    // Step 1

    private static Path selectFolderToProtect() {
        // Selecting the folder to protect
        Path path = Paths.get("Protected Folder");

        if (Files.exists(path) && Files.isDirectory(path))
            return path;
        else
            return null;
    }

    // Step 2

    private static List<FileRecord> scanProtectedFolder(Path path) {

        // Creating the array list that will store the each file record object
        List<FileRecord> records = new ArrayList<>();

        // Going through all the files and subfolders of the current folder
        try (Stream<Path> stream = Files.walk(path)) {

            // Scanning the folder to get only files
            stream.filter(p -> Files.isRegularFile(p))

                    // For each file, get its information
                    .forEach(p -> {

                        try {

                            // Information of each file will be stored in these variables
                            Path filePath = p;
                            String fileName = p.getFileName().toString();
                            long fileSize = Files.size(p);
                            long fileLastModifiedDate = Files.getLastModifiedTime(p).toMillis();

                            // Calculating the 256-bit SHA-256 hash for each file

                            // First build a pipeline to the file
                            // Opening the pipeline till file and set mode on reading
                            try (FileChannel pipeline = FileChannel.open(p, StandardOpenOption.READ);) {

                                // Reading through file and putting bytes into the buffer
                                ByteBuffer buffer = ByteBuffer.allocate(1024);

                                // MessageDigest is a java class that performs hashing and
                                // MessageDigest.getInstance() asks java to create a MessageDigest object using
                                // a specific hashing algorithm.

                                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

                                while (pipeline.read(buffer) > 0) {

                                    // Converting the buffer into reading mode
                                    buffer.flip();

                                    // Give the bytes currently inside buffer to the SHA-256 algorithm.
                                    // messageDigest keeps the internal calculation state
                                    messageDigest.update(buffer);

                                    // Clearing the buffer
                                    buffer.clear();
                                }

                                // digest() produces the final hash in the form of byte array with 32 bytes.
                                byte[] hash = messageDigest.digest();

                                // Convert the 32 hash bytes into a readable hexadecimal String.
                                // As hexadecimal is easy to read + StringBuilder() does not create every time
                                // like that of String
                                StringBuilder stringBuilder = new StringBuilder();
                                for (byte b : hash) {
                                    stringBuilder.append(String.format("%02x", b));
                                }

                                FileRecord fileRecord = new FileRecord(filePath.toString(), fileName, fileSize,
                                        fileLastModifiedDate, stringBuilder.toString());
                                records.add(fileRecord);

                            } catch (IOException e) {

                                System.out.println("An I/O error happened while opening the file.");
                                System.out.println("Exiting ...");
                                System.exit(1);
                            } catch (NoSuchAlgorithmException e) {
                                System.out.println("An Error happened while converting the data into a hash value.");
                            }

                        } catch (IOException e) {

                            System.out.println("An I/O error happened while reading file information.");
                            System.out.println("Exiting ...");
                            System.exit(1);
                        }
                    });

        } catch (IOException e) {

            System.out.println("An error happened while accessing the files.");
            System.out.println("Exiting ...");
            System.exit(1);
        }
        return records;
    }

    // Step 3

    private static void putInfoInBaseline(List<FileRecord> records) {

        // Creating the path object of that file
        Path path = Paths.get("Baseline", "baseline.json");

        if (!Files.exists(path)) {

            // Creates a Jackson object that knows how to convert Java objects into JSON.
            ObjectMapper objectMapper = new ObjectMapper();

            try {

                // Create the Baseline folder if it doesn't exist
                Files.createDirectories(path.getParent());

                // writeValue() convert the java data into JSON and write it somewhere.
                // writerWithDefaultPrettyPrinter() to format with line breaks and spaces

                objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), records);
                System.out.println("Baseline successfully saved to " + path.toAbsolutePath());
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("An error happened while accessing the file of BaseLine.");
                System.out.println("Exiting ...");
                System.exit(1);
            }
        }
    }

    // Step 4

    public static void hasProtectedFolderChanged(List<FileRecord> records, List<FileRecord> baselineRecords) {
        // hashmaps that will contain file's path as key and File's object as value

        Map<String, FileRecord> protectedFolderMap = new HashMap<>();
        Map<String, FileRecord> baseLineMap = new HashMap<>();

        // loop through each list of File's object to store key-value pair

        for (FileRecord record : records) {
            protectedFolderMap.put(record.filePath, record);
        }

        for (FileRecord record : baselineRecords) {
            baseLineMap.put(record.filePath, record);
        }

        System.out.println();

        // a. functionality to check either any file is new in Protected folder

        for (String key : protectedFolderMap.keySet()) {

            // File is new if not present in baseline but present in protected folder
            if (!baseLineMap.containsKey(key)) {
                System.out.println("New File: " + key);
            }
        }

        System.out.println();

        // b. functionality to check either any file is modified in Protected folder

        for (String key : protectedFolderMap.keySet()) {

            // file is modified if present in both folder and hash value is change in both
            if (baseLineMap.containsKey(key)) {
                if (!protectedFolderMap.get(key).hash.equals(baseLineMap.get(key).hash)) {
                    System.out.println("Modified File: " + key);
                }
            }
        }

        System.out.println();

        // c. functionality to check either any file is deleted from Protected folder

        for (String key : baseLineMap.keySet()) {

            // file is deleted if present in baseline but not in protected folder
            if (!protectedFolderMap.containsKey(key)) {
                System.out.println("Deleted File: " + key);
            }
        }
    }

    // Step 5

    public static void startWatching(Path path) {

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

                                // displaying the info
                                System.out.println("File name: " + fileName);
                                System.out.println("Event: " + kind);
                                System.out.println("Time: " + LocalDateTime.now());
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

    public static void main(String[] args) {

        System.out.println();
        System.out.println();
        System.out.println("********* Real-Time File Integrity and Ransomware Early Warning System *********");
        System.out.println();
        System.out.println();

        // 1. To get the protected Folder

        Path path = selectFolderToProtect();
        if (path == null) {
            System.out.println("Protected Folder does not exists.");
            System.out.println("Exiting ...");
            System.exit(1);
        } else {
            System.out.println("Protected Folder is Ready.");
        }

        // 2. Scan all files and subfolders of protected folder and list each file's
        // information and store inside object

        List<FileRecord> records = scanProtectedFolder(path);

        // 3. Put all file's information inside one file of baseline

        putInfoInBaseline(records);

        // 4. Manual Integrity Scan

        Path p = Paths.get("Baseline", "baseline.json");

        // Converting JSON file (baseline.json) back into java objects
        ObjectMapper objectMapper = new ObjectMapper();

        // readValue() method reads JSON and converts it into Java objects.
        // new TypeReference tells java which type of object to create

        try {
            List<FileRecord> baseLineRecords = objectMapper.readValue(p.toFile(),
                    new TypeReference<List<FileRecord>>() {
                    });
            hasProtectedFolderChanged(records, baseLineRecords);
        } catch (IOException e) {
            System.out.println("Error happened while converting the json back to java objects.");
        }

        // 5. Keep watching the folder and immediately tell when something changes.

        // path here refers to the protected folder's path.
        startWatching(path);
    }
}