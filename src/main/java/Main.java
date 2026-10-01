import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.BorderLayout;

public class Main {

    // Step 6

    public static void ModifyFile(Path path, Path protectedFolder) {

        // Checking if user eneterd path is from protected folder or not
        if (!path.startsWith(protectedFolder)) {
            System.out.println("Protected Folder doesn't contain this file: " + path.getFileName());
            return;
        }

        try {

            // Create a window called "File Editor".
            JFrame frame = new JFrame("File Editor");

            // Read the file and put its content inside an editable text box.
            JTextArea textArea = new JTextArea(Files.readString(path));

            // Create a button labeled "Save".
            JButton saveButton = new JButton("Save");

            // Put the editable text area in the center with scrolling.
            frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

            // Adds the Save button to the bottom of the window.
            frame.add(saveButton, BorderLayout.SOUTH);

            // Sets the window size
            frame.setSize(600, 400);

            // Makes the window visible to the user
            frame.setVisible(true);

            // When the user clicks the Save button, execute this code.
            saveButton.addActionListener(e -> {
                try {
                    // Mark this file as authorized before modifying it
                    FileWatcher.approvedFiles.add(path);

                    // Save the edited content
                    Files.writeString(path, textArea.getText());

                    System.out.println("File saved successfully.");

                } catch (IOException ex) {
                    System.out.println("Error happened while saving the file.");
                    FileWatcher.approvedFiles.remove(path);
                }
            });

        } catch (IOException e) {
            System.out.println("Error happened while reading the file content.");
        }
    }

    // Step 7

    public static void createFile(Path protectedFolder) {

        // Create a file chooser that starts inside the Protected Folder.
        JFileChooser fileChooser = new JFileChooser(protectedFolder.toFile());

        // Set the window title to "Create New File".
        fileChooser.setDialogTitle("Create New File");

        // The result tells us whether the user clicked Save or Cancel.
        int result = fileChooser.showSaveDialog(null);

        // If the user selected a location and clicked Save, execute this code.
        if (result == JFileChooser.APPROVE_OPTION) {

            // Get the file that the user selected.
            File selectedFile = fileChooser.getSelectedFile();

            Path newFilePath = selectedFile.toPath().toAbsolutePath().normalize();

            // Checking either user is creating a file in protected folder or any other
            // folder
            Path protectedFolderPath = protectedFolder.toAbsolutePath().normalize();

            if (!newFilePath.startsWith(protectedFolderPath)) {
                System.out.println("You can only create files inside the Protected Folder.");
                return;
            }

            // Mark the file as authorized BEFORE creating it
            FileWatcher.approvedFiles.add(newFilePath);

            try {
                // Actually create the new file.
                Files.createFile(newFilePath);
                System.out.println("File created successfully: " + newFilePath.getFileName());
            } catch (IOException e) {

                FileWatcher.approvedFiles.remove(newFilePath);
                System.out.println("Error creating file: " + e.getMessage());
            }

        } else {
            System.out.println("File creation cancelled.");
        }
    }

    // Step 8

    public static void deleteFile(Path protectedFolder) {

        // Create a file chooser that starts inside the Protected Folder.
        JFileChooser fileChooser = new JFileChooser(protectedFolder.toFile());

        // Set the window title.
        fileChooser.setDialogTitle("Delete File");

        // Open the file chooser.
        int result = fileChooser.showOpenDialog(null);

        // If the user selected a file and clicked Open.
        if (result == JFileChooser.APPROVE_OPTION) {

            // Get the selected file.
            File selectedFile = fileChooser.getSelectedFile();

            Path filePath = selectedFile.toPath()
                    .toAbsolutePath()
                    .normalize();

            Path protectedFolderPath = protectedFolder
                    .toAbsolutePath()
                    .normalize();

            // Make sure the file is inside the Protected Folder.
            if (!filePath.startsWith(protectedFolderPath)) {
                System.out.println("You can only delete files inside the Protected Folder.");
                return;
            }

            // Mark the deletion as authorized BEFORE deleting.
            FileWatcher.approvedFiles.add(filePath);

            try {
                // Actually delete the file.
                Files.delete(filePath);
                System.out.println("File deleted successfully: " + filePath.getFileName());
            } catch (IOException e) {

                // Remove approval if deletion failed.
                FileWatcher.approvedFiles.remove(filePath);
                System.out.println("Error deleting file: " + e.getMessage());
            }
        } else {
            System.out.println("File deletion cancelled.");
        }
    }

    public static void main(String[] args) {

        System.out.println();
        System.out.println();
        System.out.println("********* Real-Time File Integrity and Ransomware Early Warning System *********");
        System.out.println();
        System.out.println();

        // 1. To get the protected Folder

        Path path = FolderManager.selectFolderToProtect();
        if (path == null) {
            System.out.println("Protected Folder does not exists.");
            System.out.println("Exiting ...");
            System.exit(1);
        } else {
            System.out.println("Protected Folder is Ready.");
        }
        System.out.println();

        // backup folder initialization

        Path backupFolderPath = FolderManager.selectBackupFolder();
        if (backupFolderPath != null)
            BackupManager.initializeBackupFolder(path, backupFolderPath);

        // 2. Scan all files and subfolders of protected folder and list each file's
        // information and store inside object

        List<FileRecord> records = FileScanner.scanProtectedFolder(path);
        System.out.println();

        // 3. Put all file's information inside one file of baseline

        BaselineManager.putInfoInBaseline(records);
        System.out.println();

        // 4. Manual Integrity Scan

        Path p = Paths.get("Baseline", "baseline.json");

        // Converting JSON file (baseline.json) back into java objects
        ObjectMapper objectMapper = new ObjectMapper();

        // A list that can store multiple FileRecord objects
        List<FileRecord> baseLineRecords = null;

        try {

            // readValue() reads JSON and converts it into Java objects.
            baseLineRecords = objectMapper.readValue(

                    // new TypeReference tells java which type of object to create
                    p.toFile(), new TypeReference<List<FileRecord>>() {
                    });

            IntegrityChecker.hasProtectedFolderChanged(records, baseLineRecords);
        } catch (IOException e) {
            System.out.println("Error happened while converting the json back to java objects.");
        }
        System.out.println();

        // 5. Keep watching the folder and immediately tell when something changes.

        FileWatcher.startWatching(path, baseLineRecords, backupFolderPath);
        System.out.println();

        // 6. Modify a file and check either that change is authorized or not

        // Let the user choose the file to be modified

        String filePath = null;

        // File Chooser object that opens the system's File Explorer.
        JFileChooser fileChooser = new JFileChooser();

        // showOpenDialog() Opens the File Explorer so the user can choose a file, and
        // null means no parent window. So the dialog opens independently.
        int result = fileChooser.showOpenDialog(null);

        // JFileChooser.APPROVE_OPTION is an int constant.
        if (result == JFileChooser.APPROVE_OPTION) {

            // getSelectedFile() gives the address (File object) of the selected file
            File selectedFile = fileChooser.getSelectedFile();

            filePath = selectedFile.getAbsolutePath();
            System.out.println("Selected File: " + selectedFile.getName());
            System.out.println("File Path: " + filePath);

        } else {
            System.out.println("No file selected.");
            System.out.println("Exiting ...");
            System.exit(1);
        }

        ModifyFile(Path.of(filePath).normalize(), path.normalize());
        System.out.println();

        // 7. New file creation logic

        createFile(path);

        // 8. Delete a File
        deleteFile(path);
    }
}