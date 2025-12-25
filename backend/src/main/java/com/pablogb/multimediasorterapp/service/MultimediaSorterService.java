package com.pablogb.multimediasorterapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.pablogb.multimediasorterapp.model.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultimediaSorterService {

    //only images for now
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".svg"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".avi", ".mov", ".webm", ".mkv", ".flv", ".wmv", ".m4v"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MultimediaInfo> getMultimediaFilesFromDirectory(String sourcePath) throws IOException {
        Path path = Paths.get(sourcePath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IOException("Invalid directory path");
        }

        try (java.util.stream.Stream<Path> paths = Files.walk(path, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isMediaFile)  // Changed from isImageFile
                    .map(this::createMultimediaInfo)
                    .sorted(Comparator.comparing(MultimediaInfo::getName))
                    .collect(Collectors.toList());
        }
    }

    private boolean isMediaFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith) ||
                VIDEO_EXTENSIONS.stream().anyMatch(fileName::endsWith);
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

    public MultimediaMetadata getMediaMetadata(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found");
        }

        long size = Files.size(path);
        String fileName = path.getFileName().toString().toLowerCase();

        // Check if it's a video
        if (VIDEO_EXTENSIONS.stream().anyMatch(fileName::endsWith)) {
            return getVideoMetadata(path, size);
        } else {
            return getImageMetadata(path, size);
        }
    }

    private MultimediaMetadata getImageMetadata(Path path, long size) {
        Integer width = null;
        Integer height = null;

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception e) {
            // If we can't read dimensions, just return what we have
        }

        return new MultimediaMetadata(size, width, height, null, "image");
    }

    private MultimediaMetadata getVideoMetadata(Path path, long size) {
        Integer width = null;
        Integer height = null;
        Float duration = null;

        try {
            System.out.println("Attempting to read video metadata for: " + path);

            FFprobeResult result = FFprobe.atPath()
                    .setShowStreams(true)
                    .setShowFormat(true)  // IMPORTANT: Need to explicitly enable format info
                    .setInput(path)
                    .execute();

            System.out.println("FFprobe execution completed");
            System.out.println("Number of streams: " + (result.getStreams() != null ? result.getStreams().size() : 0));

            // Get video stream information
            if (result.getStreams() != null) {
                for (Stream stream : result.getStreams()) {
                    System.out.println("Stream codec type: " + stream.getCodecType());
                    if (stream.getCodecType() == StreamType.VIDEO) {  // Use enum comparison
                        width = stream.getWidth();
                        height = stream.getHeight();
                        System.out.println("Found video stream: " + width + "x" + height);
                        break;
                    }
                }
            }

            // Get duration from format
            if (result.getFormat() != null) {
                System.out.println("Format info available");
                if (result.getFormat().getDuration() != null) {
                    duration = result.getFormat().getDuration();
                    System.out.println("Duration: " + duration + " seconds");
                } else {
                    System.out.println("Duration is null in format");
                }
            } else {
                System.out.println("Format is null");
            }

            System.out.println("Final metadata - Width: " + width + ", Height: " + height + ", Duration: " + duration);

        } catch (Exception e) {
            System.err.println("Error reading video metadata: " + e.getMessage());
            e.printStackTrace();
        }

        return new MultimediaMetadata(size, width, height, duration, "video");
    }

    public SortResult sortMedia(SortRequest request) throws Exception {
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

        try (java.util.stream.Stream<Path> paths = Files.list(dirPath)) {
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

    public Map<String, List<Destination>> loadDestinationLists() throws IOException {
        Path configFile = getDestinationListsConfigPath();
        if (!Files.exists(configFile)) {
            return new HashMap<>();
        }

        TypeFactory typeFactory = objectMapper.getTypeFactory();
        CollectionType listType = typeFactory.constructCollectionType(List.class, Destination.class);
        MapType mapType = typeFactory.constructMapType(HashMap.class, typeFactory.constructType(String.class), listType);

        return objectMapper.readValue(configFile.toFile(), mapType);
    }

    public void saveDestinationLists(Map<String, List<Destination>> lists) throws IOException {
        Path configFile = getDestinationListsConfigPath();
        Files.createDirectories(configFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), lists);
    }

    private Path getDestinationListsConfigPath() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".imagesorter");
        return configDir.resolve("destination-lists.json");
    }

    private Path getDestinationsConfigPath() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".imagesorter");
        return configDir.resolve("destinations.json");
    }

    public void openFileInDefaultApp(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found");
        }

        // Force non-headless mode
        System.setProperty("java.awt.headless", "false");

        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop operations not supported on this system");
        }

        Desktop.getDesktop().open(path.toFile());
    }

    public void openFileInExplorer(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found");
        }

        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Windows: Use explorer with /select flag to highlight the file
                Runtime.getRuntime().exec(new String[]{
                        "explorer.exe", "/select,", path.toAbsolutePath().toString()
                });
            } else if (os.contains("mac")) {
                // macOS: Use open with -R flag to reveal in Finder
                Runtime.getRuntime().exec(new String[]{
                        "open", "-R", path.toAbsolutePath().toString()
                });
            } else {
                // Linux: Open the parent directory (most file managers don't support highlighting)
                Desktop.getDesktop().open(path.getParent().toFile());
            }
        } catch (Exception e) {
            throw new IOException("Could not open explorer: " + e.getMessage());
        }
    }
}
