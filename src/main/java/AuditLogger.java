import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

// The purpose of this class is to record file changes in a log file
// and connect each record to the previous one using hashes.
public class AuditLogger {

    // Method to compute SHA-256 string hash
    private static String computeSha256(String input) {
        try {
            // Selects the SHA-256 hashing algorithm.
            MessageDigest hash = MessageDigest.getInstance("SHA-256");

            // Converts the String into bytes and calculates its SHA-256 hash.
            byte[] hashBytes = hash.digest(input.getBytes(StandardCharsets.UTF_8));

            // Creates an empty string that we will use to build the final hexadecimal hash.
            StringBuilder hexString = new StringBuilder();

            // '0xff & b' Gets the byte's unsigned value (0 – 255).
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    // SHA-256 produces 32 bytes and each byte must exactly 2 characters.
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // Stores the location of the audit log file.
    private static final Path LOG_FILE_PATH = Paths.get("audit_log.txt").toAbsolutePath().normalize();

    // When the first log entry is created, there is no previous entry. Therefore,
    // we use 64 zeros as the starting point of our hash chain.
    private static String lastEntryHash = "0000000000000000000000000000000000000000000000000000000000000000";

    // This method records a file-change event in our audit log file.
    // synchronized allows only 1 thread at a time to execute this method.
    public static synchronized void logEvent(String filePath, String eventType, String oldHash, String newHash) {

        // Gets the current date and exact time when the file-change event occurs.
        Instant now = Instant.now();

        // 1. Create record of log entry
        AuditLogRecord record = new AuditLogRecord(now, filePath, eventType, oldHash, newHash, lastEntryHash);

        // 2. Compute SHA-256 hash of this entry
        record.entryHash = computeSha256(record.toRawString());

        // 3. Stores that hash for the next log entry.
        lastEntryHash = record.entryHash;

        // 4. We convert our audit log entry into bytes and put those bytes into a
        // buffer because FileChannel needs a ByteBuffer to write data into a file.
        byte[] bytesToWrite = record.toLogLine().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytesToWrite);

        // 5. Open the audit log file, create it if necessary and prepare it to add new
        // entries at the end.
        try (FileChannel channel = FileChannel.open(LOG_FILE_PATH,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {

            // Lock the file, Check whether bytes are remaining, Write the bytes, Repeat
            // until all bytes are written and Release the lock then.
            try (FileLock lock = channel.lock()) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to audit log: " + e.getMessage());
        }
    }
}