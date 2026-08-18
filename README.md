Step 6 — Live Integrity Verification
Features:
Whenever WatchService detects a change:
•	Recalculate SHA-256 hash. 
•	Compare with baseline. 
•	Mark: 
o	Authorized Change 
o	Unauthorized Change 
Goal:
Verify every file change immediately.

    // 3. Remove from approvedFiles
    approvedFiles.remove(filepath);

} else {
    // UNAUTHORIZED: Restore tampered file from Backup
    Path backupFilePath = backupFolder.resolve(filepath.getFileName());
    try {
        approvedFiles.add(filepath); // Prevent infinite loop during rollback write
        Files.copy(backupFilePath, filepath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("ALERT: Unauthorized edit detected! Restored from Backup.");
    } catch (IOException e) {
        approvedFiles.remove(filepath);
        System.out.println("Failed to perform rollback: " + e.getMessage());
    }
}
________________________________________
Step 7 — Tamper-Evident Audit Log
Features:
Store:
•	Timestamp 
•	File path 
•	Event 
•	Old hash 
•	New hash 
Also:
Create hash chain between log entries.
Use:
•	FileChannel 
•	ByteBuffer 
•	Charset 
•	FileLock while writing 
Goal:
Prevent audit log corruption.
________________________________________
Step 8 — Alert Generation
Features:
Whenever unauthorized activity occurs:
•	Console warning 
•	alerts.log 
Future:
•	Email 
•	Desktop notification 
Goal:
Immediately inform administrators.
________________________________________
Step 9 — Ransomware Detection Engine
Features:
Detect:
•	Many files modified in a short time. 
•	Many files deleted quickly. 
•	Many files renamed quickly. 
•	Suspicious extensions. 
Goal:
Identify ransomware-like behavior early.
________________________________________
Step 10 — Backup and Auto-Response
First:
•	Automatically back up affected files. 
Then:
Possible responses:
•	Move files to quarantine. 
•	Remove write permission. 
•	Block further processing. 
Goal:
Protect files before more damage occurs.
________________________________________
Step 11 — Large File Support
Features:
For very large files:
Use:
•	MappedByteBuffer 
instead of normal reading.
Goal:
Process gigabyte-sized files efficiently with low memory usage.
________________________________________
Step 12 — Security Dashboard
Generate report:
•	Files monitored 
•	Files created 
•	Files modified 
•	Files deleted 
•	Unauthorized changes 
•	Suspicious activities 
•	Last scan 
•	Backup count 
Initially:
Console report.
Later:
Spring Boot dashboard (optional).
________________________________________
Step 13 — Configuration Management
Move all hardcoded values into configuration.
Examples:
•	Protected folder 
•	Backup folder 
•	Threshold values 
•	Whitelist 
Goal:
Make the system reusable.