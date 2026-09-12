import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileScanner {

    // Step 2

    // The purpose of scanProtectedFolder is to scan all files in your protected
    // folder and build an initial baseline snapshot.
    public static List<FileRecord> scanProtectedFolder(Path path) {

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
}