package com.macreations.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Replaceable product image storage. Local filesystem is the default until
 * the client confirms disk vs object storage.
 */
public interface ProductImageStorage {

    /**
     * Persist the uploaded file and return a storage key/path stored in {@code product_image.storage_path}.
     */
    String store(MultipartFile file);

    /**
     * Convert a stored path/key into a URL the API can return to clients.
     */
    String toPublicUrl(String storagePath);

    /**
     * Best-effort removal of a stored file. Missing files should not fail the caller.
     */
    void delete(String storagePath);
}
