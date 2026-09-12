import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

// The purpose of this class is to save your scanned file baseline records
// into a JSON file (baseline.json) on disk.
public class BaselineManager {

    // Step 3

    public static void putInfoInBaseline(List<FileRecord> records) {

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
}