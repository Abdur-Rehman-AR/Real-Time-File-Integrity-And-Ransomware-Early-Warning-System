// Class that represents each file's information

public class FileRecord {

    // Variables that store the info about each file
    String filePath;
    String fileName;
    long fileSize;
    long fileLastModifiedDate;
    String hash;

    // Constructor that is used to inject values of each file
    public FileRecord(String filePath, String fileName, long fileSize, long fileLastModifiedDate, String hash) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileLastModifiedDate = fileLastModifiedDate;
        this.hash = hash;
    }

    // Getter methods used by Jackson to read value from
    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getFileLastModifiedDate() {
        return fileLastModifiedDate;
    }

    public String getHash() {
        return hash;
    }
}