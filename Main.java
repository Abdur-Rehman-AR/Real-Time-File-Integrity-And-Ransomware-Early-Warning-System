import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

public class Main {

    // Step 1

    public static Path selectFolderToProtect() {
        // Selecting the folder to protect
        Path path = Paths.get("Protected Folder");

        if (Files.exists(path) && Files.isDirectory(path))
            return path;
        else
            return null;
    }

    // Step 2

    public static void scanFolder(Path path) {

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
                            FileTime fileLastModifiedDate = Files.getLastModifiedTime(p);

                            // Calculating the 256-bit SHA-256 hash for each file

                            // First build a pipeline to the file
                            FileChannel pipeline = null;

                            try {

                                // Actually opening the pipeline till file and set mode on reading
                                pipeline = FileChannel.open(path, StandardOpenOption.READ);

                                // Reading through file and putting bytes into the buffer
                                ByteBuffer buffer = ByteBuffer.allocate(1024);

                                while(pipeline.read(buffer) > 0)
                                {
                                    
                                }

                            } catch (IOException e) {

                                System.out.println("An I/O error happened while opening the file.");
                                System.out.println("Exiting ...");
                                System.exit(1);
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
        }
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

        // 2. Scan all files and subfolders. i.e, Walk through all the files and list
        // each one's information

        scanFolder(path);

    }
}