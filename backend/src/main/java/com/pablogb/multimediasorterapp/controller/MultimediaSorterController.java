package com.pablogb.multimediasorterapp.controller;

import com.pablogb.multimediasorterapp.model.*;
import com.pablogb.multimediasorterapp.service.MultimediaSorterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MultimediaSorterController {

    @Autowired
    private MultimediaSorterService service;

    @GetMapping("/images")
    public ResponseEntity<List<MultimediaInfo>> getImages(@RequestParam String sourcePath) {
        try {
            List<MultimediaInfo> images = service.getMultimediaFilesFromDirectory(sourcePath);
            return ResponseEntity.ok(images);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/media")
    public ResponseEntity<Resource> getMedia(@RequestParam String path) {
        try {
            Path imagePath = Paths.get(path);
            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(imagePath);
            String contentType = Files.probeContentType(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/media-metadata")
    public ResponseEntity<MultimediaMetadata> getMediaMetadata(@RequestParam String path) {
        try {
            MultimediaMetadata metadata = service.getMediaMetadata(path);
            return ResponseEntity.ok(metadata);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/sort")
    public ResponseEntity<SortResult> sortMedia(@RequestBody SortRequest request) {
        try {
            SortResult result = service.sortMedia(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new SortResult(false, e.getMessage(), 0, 0, 0)
            );
        }
    }

    @GetMapping("/session")
    public ResponseEntity<SessionState> getSession(@RequestParam String sourcePath) {
        try {
            SessionState session = service.loadSession(sourcePath);
            return ResponseEntity.ok(session);
        } catch (IOException e) {
            return ResponseEntity.ok(new SessionState());
        }
    }

    @PostMapping("/session")
    public ResponseEntity<Void> saveSession(@RequestBody SessionState session) {
        try {
            service.saveSession(session);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> deleteSession(@RequestParam String sourcePath) {
        try {
            service.deleteSession(sourcePath);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<List<String>> getFolders(@RequestParam String path) {
        try {
            List<String> folders = service.listFolders(path);
            return ResponseEntity.ok(folders);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/validate-path")
    public ResponseEntity<PathValidation> validatePath(@RequestParam String path) {
        PathValidation validation = service.validatePath(path);
        return ResponseEntity.ok(validation);
    }

    @GetMapping("/destinations")
    public ResponseEntity<List<Destination>> getDestinations() {
        try {
            List<Destination> destinations = service.loadDestinations();
            return ResponseEntity.ok(destinations);
        } catch (IOException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping("/destinations")
    public ResponseEntity<Void> saveDestinations(@RequestBody List<Destination> destinations) {
        try {
            service.saveDestinations(destinations);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/destination-lists")
    public ResponseEntity<Map<String, List<Destination>>> getDestinationLists() {
        try {
            Map<String, List<Destination>> lists = service.loadDestinationLists();
            return ResponseEntity.ok(lists);
        } catch (IOException e) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }

    @PostMapping("/destination-lists")
    public ResponseEntity<Void> saveDestinationLists(@RequestBody Map<String, List<Destination>> lists) {
        try {
            service.saveDestinationLists(lists);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/open-file")
    public ResponseEntity<String> openFile(@RequestParam String path) {
        try {
            service.openFileInDefaultApp(path);
            return ResponseEntity.ok("File opened successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/open-explorer")
    public ResponseEntity<String> openExplorer(@RequestParam String path) {
        try {
            service.openFileInExplorer(path);
            return ResponseEntity.ok("Explorer opened successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
