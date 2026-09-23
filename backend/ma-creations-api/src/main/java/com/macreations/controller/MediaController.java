package com.macreations.controller;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.exception.NotFoundException;
import com.macreations.service.storage.LocalFileProductImageStorage;

/**
 * Serves locally stored product images until an external CDN/object store is confirmed.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final LocalFileProductImageStorage localFileProductImageStorage;

    public MediaController(LocalFileProductImageStorage localFileProductImageStorage) {
        this.localFileProductImageStorage = localFileProductImageStorage;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getMedia(@PathVariable String filename) {
        Path file = localFileProductImageStorage.resolveStoredFile(filename);
        if (file == null) {
            throw new NotFoundException("MEDIA_NOT_FOUND", "Media file not found");
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
