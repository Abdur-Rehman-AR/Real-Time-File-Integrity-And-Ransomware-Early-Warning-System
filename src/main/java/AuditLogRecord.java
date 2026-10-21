import java.time.Instant;

// This class represents one log entry. Later, AuditLogger will save these
// entries to a file.
public class AuditLogRecord {

    // These variables store information about 1 file-change event.
    public Instant timestamp; // Instant is used to store an exact moment in time.
    public String filePath;
    public String eventType; // CREATE, MODIFY, DELETE
    public String oldHash;
    public String newHash;
    public String previousEntryHash; // Hash of the previous log
    public String entryHash; // Hash of this log

    // Constructor that will fill the details whenever a new AuditLogRecord object
    // is created.
    public AuditLogRecord(Instant timestamp, String filePath, String eventType,
            String oldHash, String newHash, String previousEntryHash) {
        this.timestamp = timestamp;
        this.filePath = filePath;
        this.eventType = eventType;
        this.oldHash = oldHash != null ? oldHash : "NONE"; // none is stored when the file is just created
        this.newHash = newHash != null ? newHash : "NONE"; // none is stored when the file is just deleted
        this.previousEntryHash = previousEntryHash != null ? previousEntryHash
                // We use 64 zeros instead of just 1 bcz SHA-256 hashes always contain 64
                // hexadecimal characters.
                : "0000000000000000000000000000000000000000000000000000000000000000";
    }

    // This function combines all log details into one string so we can calculate
    // its hash.
    public String calculateHash() {
        return timestamp.toString() + "|" + filePath + "|" + eventType + "|" + oldHash + "|" + newHash + "|"
                + previousEntryHash;
    }

    // This function prepares 1 complete audit record to save in the log file.
    public String toLogLine() {
        return calculateHash() + "|" + entryHash + "\n";
    }
}