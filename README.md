Step 8 — Alert Generation
Features:
Whenever unauthorized activity occurs: alerts.log 
Goal: Immediately inform administrators.
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