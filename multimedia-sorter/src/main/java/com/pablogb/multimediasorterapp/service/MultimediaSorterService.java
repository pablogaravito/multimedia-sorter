package com.pablogb.multimediasorterapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablogb.multimediasorterapp.model.MultimediaInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
                    .sorted(Comparator.comparing(ImageInfo::getName))
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
    
}
