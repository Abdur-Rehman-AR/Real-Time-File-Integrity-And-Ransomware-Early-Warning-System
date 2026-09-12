import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegrityChecker {

    // Step 4

    // Method that will used to sacn the protected folder to check either any file
    // changed or not
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

    // Method that will calculate the SHA-256 for the files

    public static String calculateHash(Path path) {

        // Creating a pipeline till file that we need to read
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

            // Buffer that will use to contain the bytes of the file
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // MessageDigest class calculates hash values, this MessageDigest object uses
            // the SHA-256 algorithm.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // read the bytes from file and put them into the buffer
            while (channel.read(buffer) > 0) {
                buffer.flip();

                // Give the bytes currently inside buffer to the SHA-256 calculator.
                digest.update(buffer);
                buffer.clear();
            }

            // hash came out in the form of bytes
            byte[] hash = digest.digest();

            // using StringBuilder bcz it updates itself every time as modifies when
            StringBuilder result = new StringBuilder();

            // Converting into hexadecimal
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}