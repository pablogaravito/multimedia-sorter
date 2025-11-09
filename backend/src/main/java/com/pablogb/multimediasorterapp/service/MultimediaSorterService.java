package com.pablogb.multimediasorterapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablogb.multimediasorterapp.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MultimediaSorterService {

    //only images for now
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".svg"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MultimediaInfo> getImagesFromDirectory(String sourcePath) throws IOException {
        Path path = Paths.get(sourcePath);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IOException("Invalid directory path");
        }

        try (Stream<Path> paths = Files.walk(path, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .map(this::createMultimediaInfo)
                    .sorted(Comparator.comparing(MultimediaInfo::getName))
                    .collect(Collectors.toList());
        }
    }

    private boolean isImageFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private MultimediaInfo createMultimediaInfo(Path path) {
        try {
            return new MultimediaInfo(
                    path.getFileName().toString(),
                    path.toAbsolutePath().toString(),
                    Files.size(path)
            );
        } catch (IOException e) {
            return new MultimediaInfo(path.getFileName().toString(), path.toAbsolutePath().toString(), 0L);
        }
    }

    public SortResult sortImages(SortRequest request) throws Exception {
        int copied = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getClassifications().entrySet()) {
            String sourcePath = entry.getKey();
            String destFolder = entry.getValue();

            // Find destination path
            String destPath = request.getDestinations().stream()
                    .filter(d -> d.getName().equals(destFolder))
                    .map(Destination::getPath)
                    .findFirst()
                    .orElse(null);

            if (destPath == null) {
                errors.add("Destination folder not found: " + destFolder);
                failed++;
                continue;
            }

            try {
                Path source = Paths.get(sourcePath);
                Path destDir = Paths.get(destPath);

                // Create destination directory if it doesn't exist
                Files.createDirectories(destDir);

                Path destination = destDir.resolve(source.getFileName());

                // Check for duplicates
                if (Files.exists(destination)) {
                    String sourceHash = calculateFileHash(source);
                    String destHash = calculateFileHash(destination);

                    if (sourceHash.equals(destHash)) {
                        // Exact duplicate, just delete source
                        Files.delete(source);
                        skipped++;
                        continue;
                    } else {
                        // Different file with same name, rename
                        destination = findUniqueFileName(destDir, source.getFileName().toString());
                    }
                }

                // Copy file
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

                // Verify with hash
                String sourceHash = calculateFileHash(source);
                String destHash = calculateFileHash(destination);

                if (!sourceHash.equals(destHash)) {
                    Files.delete(destination);
                    errors.add("Hash verification failed for: " + source.getFileName());
                    failed++;
                    continue;
                }

                // Delete source after successful verification
                Files.delete(source);
                copied++;

            } catch (Exception e) {
                errors.add("Error processing " + sourcePath + ": " + e.getMessage());
                failed++;
            }
        }

        String message = String.format("Copied: %d, Skipped (duplicates): %d, Failed: %d",
                copied, skipped, failed);

        if (!errors.isEmpty()) {
            message += "\nErrors:\n" + String.join("\n", errors);
        }

        return new SortResult(failed == 0, message, copied, skipped, failed);
    }

    private String calculateFileHash(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] hashBytes = digest.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Path findUniqueFileName(Path directory, String originalFileName) {
        String nameWithoutExt = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String ext = originalFileName.substring(originalFileName.lastIndexOf('.'));

        int counter = 1;
        Path uniquePath;

        do {
            String newName = nameWithoutExt + "_" + counter + ext;
            uniquePath = directory.resolve(newName);
            counter++;
        } while (Files.exists(uniquePath));

        return uniquePath;
    }

    public SessionState loadSession(String sourcePath) throws IOException {
        Path sessionFile = getSessionFilePath(sourcePath);

        if (!Files.exists(sessionFile)) {
            return new SessionState();
        }

        return objectMapper.readValue(sessionFile.toFile(), SessionState.class);
    }

    public void saveSession(SessionState session) throws IOException {
        Path sessionFile = getSessionFilePath(session.getSourcePath());
        Files.createDirectories(sessionFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile.toFile(), session);
    }

    public void deleteSession(String sourcePath) throws IOException {
        Path sessionFile = getSessionFilePath(sourcePath);
        if (Files.exists(sessionFile)) {
            Files.delete(sessionFile);
        }
    }

    private Path getSessionFilePath(String sourcePath) {
        String userHome = System.getProperty("user.home");
        Path sessionDir = Paths.get(userHome, ".imagesorter", "sessions");

        // Create a safe filename from the source path
        String safeName = sourcePath.replaceAll("[^a-zA-Z0-9]", "_") + ".json";

        return sessionDir.resolve(safeName);
    }

    public List<String> listFolders(String path) throws IOException {
        Path dirPath = Paths.get(path);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return Collections.emptyList();
        }

        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(p -> p.toAbsolutePath().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public PathValidation validatePath(String path) {
        Path p = Paths.get(path);
        boolean exists = Files.exists(p);
        boolean isDirectory = exists && Files.isDirectory(p);
        boolean isReadable = exists && Files.isReadable(p);
        boolean isWritable = exists && Files.isWritable(p);

        return new PathValidation(exists, isDirectory, isReadable, isWritable);
    }

    public List<Destination> loadDestinations() throws IOException {
        Path configFile = getDestinationsConfigPath();

        if (!Files.exists(configFile)) {
            return Collections.emptyList();
        }

        return objectMapper.readValue(
                configFile.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Destination.class)
        );
    }

    public void saveDestinations(List<Destination> destinations) throws IOException {
        Path configFile = getDestinationsConfigPath();
        Files.createDirectories(configFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), destinations);
    }

    private Path getDestinationsConfigPath() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".imagesorter");
        return configDir.resolve("destinations.json");
    }
}
