package com.macreations.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.macreations.exception.BadRequestException;

/**
 * Local-disk implementation. Swap this bean for an S3 (or other) implementation
 * without changing catalog services.
 */
@Service
public class LocalFileProductImageStorage implements ProductImageStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadRoot;
    private final String publicBasePath;

    public LocalFileProductImageStorage(
            @Value("${app.upload.dir:uploads/products}") String uploadDir,
            @Value("${app.upload.public-base-path:/api/media}") String publicBasePath) throws IOException {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBasePath = publicBasePath.endsWith("/")
                ? publicBasePath.substring(0, publicBasePath.length() - 1)
                : publicBasePath;
        Files.createDirectories(this.uploadRoot);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("VALIDATION_ERROR", "Product image is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("VALIDATION_ERROR", "Image must be JPEG, PNG, WEBP, or GIF");
        }

        String extension = resolveExtension(file);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadRoot.resolve(filename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new BadRequestException("VALIDATION_ERROR", "Invalid image path");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("IMAGE_STORE_FAILED", "Could not store product image");
        }

        return filename;
    }

    @Override
    public String toPublicUrl(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return null;
        }
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://") || storagePath.startsWith("/")) {
            return storagePath;
        }
        return publicBasePath + "/" + storagePath;
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://") || storagePath.startsWith("/")) {
            return;
        }
        Path resolved = uploadRoot.resolve(storagePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(resolved);
        } catch (IOException ignored) {
            // Best-effort cleanup — DB row removal remains authoritative
        }
    }

    public Path resolveStoredFile(String storagePath) {
        Path resolved = uploadRoot.resolve(storagePath).normalize();
        if (!resolved.startsWith(uploadRoot) || !Files.exists(resolved)) {
            return null;
        }
        return resolved;
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(original);
        if (extension == null || extension.isBlank()) {
            return ".bin";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
